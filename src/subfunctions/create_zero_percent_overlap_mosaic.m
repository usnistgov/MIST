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



function create_zero_percent_overlap_mosaic(input_directory, img_name_grid, output_directory, output_prefix, blend_method, alpha, time_slice)
if input_directory(end) ~= filesep
  input_directory = [input_directory filesep];
end
if output_directory(end) ~= filesep
  output_directory = [output_directory filesep];
end

[nb_vertical_tiles, nb_horizontal_tiles] = size(img_name_grid);

% Initialize the starting point of each image. These starting points are computed relative to Matlab image coordinate system ==>
% x is left to right and y is up to down. global_y_img_pos is vertical or y and global_x_img_pos is horizontal or x
global_y_img_pos = zeros(nb_vertical_tiles,1);
global_x_img_pos = zeros(1,nb_horizontal_tiles);

stats = [];
for i = 1:numel(img_name_grid)
    if ~isempty(img_name_grid{i})
        stats = imfinfo([input_directory img_name_grid{i}]);
        break;
    end
end
if isempty(stats)
  error('create_zero_percent_mosaic:InvalidImageNames','No images found on disk that matched the source directory and image names specified.');
end


% create global_y_img_pos and global_x_img_pos so that each image is placed with no overlaps
for i = 2:nb_vertical_tiles
	global_y_img_pos(i) = global_y_img_pos(i-1) + stats.Height;
end
global_y_img_pos = repmat(global_y_img_pos, 1, nb_horizontal_tiles);
for i = 2:nb_horizontal_tiles
	global_x_img_pos(i) = global_x_img_pos(i-1) + stats.Width;
end
global_x_img_pos = repmat(global_x_img_pos, nb_vertical_tiles, 1);

% adjust to 1 based indexing
global_y_img_pos = global_y_img_pos + 1;
global_x_img_pos = global_x_img_pos + 1;

tile_weights = ones(nb_vertical_tiles, nb_horizontal_tiles);



print_to_command('Assembling output stitched image');
I = assemble_stitched_image(input_directory, img_name_grid, global_y_img_pos, global_x_img_pos, tile_weights, blend_method, alpha);
img_name = strcat(output_prefix,sprintf('stitched-%d.tif',time_slice));
print_to_command('Saving output stitched image');
if isa(I, 'single') || isa(I, 'double')
    save_32_tiff(I, [output_directory img_name]);
else
    imwrite(I, [output_directory img_name]);
end

end
