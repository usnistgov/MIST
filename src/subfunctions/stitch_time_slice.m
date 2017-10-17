% Disclaimer: IMPORTANT: This software was developed at the National
% Institute of Standards and Technology by employees of the Federal
% Government in the course of their official duties. Pursuant to
% title 17 Section 105 of the United States Code this software is not
% subject to copyright protection and is in the public domain. This
% is an experimental system. NIST assumes no responsibility
% whatsoever for its use by other parties, and makes no guarantees,
% expressed or implied, about its quality, reliability, or any other
% characteristic. We would appreciate acknowledgement if the software
% is used. This software can be redistributed and/or modified freely
% provided that any derivative works bear some notice that they are
% derived from it, and any modified versions bear some notice that
% they have been modified.


function stitch_time_slice(input_directory, img_name_grid, output_directory, output_prefix, time_slice, repeatability, ...
percent_overlap_error, blend_method, alpha, save_stitched_image, assemble_from_metadata, log_file_path, estimated_overlap_x,...
estimated_overlap_y)

expStartTime = tic;
if input_directory(end) ~= filesep
  input_directory = [input_directory filesep];
end
if output_directory(end) ~= filesep
  output_directory = [output_directory filesep];
end

if ~isfinite(time_slice), time_slice = 0; end
if ~isfinite(repeatability) || repeatability == 0, repeatability = NaN; end
if ~isfinite(alpha), alpha = 1.5; end
if ~isfinite(estimated_overlap_x), estimated_overlap_x = NaN; end
if ~isfinite(estimated_overlap_y), estimated_overlap_y = NaN; end
if ~isfinite(percent_overlap_error) || percent_overlap_error == 0, percent_overlap_error = 5; end


temp = strsplit(input_directory, filesep);
if isempty(temp{end}), temp = temp(1:end-1); end
fh = fopen(log_file_path, 'w');
dv = round(clock());
fprintf(fh,'<%02d-%02d-%02dT%02d:%02d:%02d>\n',dv(1),dv(2),dv(3),dv(4),dv(5),dv(6));
fclose(fh);
print_to_command(['**** Stitching: ' temp{end-1} filesep temp{end} sprintf('-%d',time_slice) ' ****'], log_file_path);
print_to_command(sprintf('PCIAM Computation'), log_file_path);
print_to_command(sprintf('Source Directory: %s', input_directory),log_file_path);

% set the statistics class in blank to overwrite any previously saved information
StitchingStatistics.getInstance.reset;


compute_translations = true;
compute_global_positions = true;
if assemble_from_metadata
  try
    if exist([output_directory output_prefix sprintf('metadata-%d.mat',time_slice)], 'file')
      % save backup copies of the variables that needs to be preserved
      current_target_name = output_prefix;
      current_source_directory = input_directory;
      current_target_directory = output_directory;
      current_assemble_img_flag = save_stitched_image;
      cur_blend_method = blend_method;
      
      % load the metadata mat file
      print_to_command('Loading Translations', log_file_path);
      load([output_directory output_prefix sprintf('metadata-%d.mat',time_slice)]);
      
      if exist('X1','var') && exist('Y1','var') && exist('CC1','var') && exist('X2','var') && exist('Y2','var') && exist('CC2','var')
        print_to_command('Loaded values contain relative translations', log_file_path);
        compute_translations = false;
      end
      
      if exist('global_y_img_pos','var') && exist('global_x_img_pos','var')
        print_to_command('Loaded values contain global image positions', log_file_path);
        compute_global_positions = false;
      end
      
      % restore the variables
      output_prefix = current_target_name;
      input_directory = current_source_directory;
      output_directory = current_target_directory;
      save_stitched_image = current_assemble_img_flag;
      blend_method = cur_blend_method;
    end
  catch err
    print_to_command(['Failed to load translations! Computing Translations by Default\n' getReport(err)]);
    compute_translations = true;
    compute_global_positions = true;
  end
end



% Translation Computation and Correction
if compute_translations
  print_to_command('Computing PCIAM', log_file_path);
  [Y1, X1, Y2, X2, CC1, CC2] = compute_pciam(input_directory, img_name_grid, log_file_path);
  write_translations_to_csv(img_name_grid, X1,Y1,CC1,X2,Y2,CC2, [output_directory output_prefix sprintf('relative-positions-no-optimization-%d.txt',time_slice)]);
  save([output_directory output_prefix sprintf('relative-positions-no-optimization-%d.mat',time_slice)], 'img_name_grid','X1','Y1','CC1','X2','Y2','CC2');
  
  print_to_command('Correcting Translations', log_file_path);
  [Y1, X1, Y2, X2, CC1, CC2] = translation_optimization(input_directory, img_name_grid, Y1, X1, Y2, X2, CC1, CC2, repeatability, percent_overlap_error, estimated_overlap_x, estimated_overlap_y, log_file_path);
  write_translations_to_csv(img_name_grid, X1,Y1,CC1,X2,Y2,CC2, [output_directory output_prefix sprintf('relative-positions-%d.txt',time_slice)]);
end


% Create global image positions
if compute_global_positions
  startTime = tic;
  print_to_command('Creating global image tile locations using Minimum Spanning Tree', log_file_path);
  % assemble global positions using minimum spanning tree
  [tiling_indicator, tile_weights, global_y_img_pos, global_x_img_pos] = minimum_spanning_tree(Y1, X1, Y2, X2, CC1, CC2);
  write_global_positions_to_csv(input_directory, img_name_grid, global_y_img_pos, global_x_img_pos,CC1,CC2, [output_directory output_prefix sprintf('global-positions-%d.txt',time_slice)]);
  s = StitchingStatistics.getInstance;
  s.total_stitching_time = toc(startTime) + s.relative_displacement_time + s.global_optimization_time;
end

% Assemble the mosaic image
if save_stitched_image
  print_to_command('Assembling output stitched image', log_file_path);
  I = assemble_stitched_image(input_directory, img_name_grid, global_y_img_pos, global_x_img_pos, tile_weights, blend_method, alpha);
  img_name = strcat(output_prefix,sprintf('stitched-%d.tif',time_slice));
  print_to_command(['Saving output image: ' img_name], log_file_path);
  if isa(I, 'single') || isa(I, 'double')
    % save as a 32bit floating point TIFF
    save_32_tiff(I, [output_directory img_name]);
  else
    % save as a 8 or 16 bit grayscale TIFF
    imwrite(I, [output_directory img_name]);
  end
  clear I;
end


s = StitchingStatistics.getInstance;
s.total_experiment_time = toc(expStartTime);
s.print_stats_to(strcat(output_directory, output_prefix, sprintf('statistics-%d.txt',time_slice)));

if compute_translations || compute_global_positions
  % save stitching metadata using the output prefix
  print_to_command(['Saving metadata: ' output_prefix sprintf('metadata-%d.mat',time_slice)], log_file_path);
  save([output_directory output_prefix sprintf('metadata-%d.mat',time_slice)]);
end
end


