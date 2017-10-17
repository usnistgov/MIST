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



function [img_name_grid, to_stitch_time_slices] = build_img_name_grid(source_img_dir, source_img_name_pattern, nb_horz_tiles, nb_vert_tiles, varargin)
img_name_grid = cell(0,0,0);
validTT = {'combing','continuous'};
validFD = {'horizontal','vertical'};
validSP = {'upperleft','upperright','lowerleft','lowerright'};

p = inputParser;

% setup required parameters
p.addRequired('source_img_dir', @(x)validateFolderExists(x));
p.addRequired('source_img_name_pattern', @(x)validateFilenamePattern(x));
p.addRequired('nb_horz_tiles', @(x)validateNumberGtZ(x));
p.addRequired('nb_vert_tiles', @(x)validateNumberGtZ(x));

% setup (K,V) pair optional parameters
p.addParamValue('time_slices_to_stitch', '', @(x)validateTimeslices(x));
p.addParamValue('tiling_technique', 'combing', @(x) any(validatestring(x,validTT)));
p.addParamValue('starting_point', 'upperleft', @(x) any(validatestring(x,validSP)));
p.addParamValue('first_direction', 'vertical', @(x) any(validatestring(x,validFD)));

p.parse(source_img_dir, source_img_name_pattern, nb_horz_tiles, nb_vert_tiles, varargin{:});
inputs = p.Results;

% append file seperator to filepath
if inputs.source_img_dir(end) ~= filesep
  inputs.source_img_dir = [inputs.source_img_dir filesep];
end

if strcmpi(inputs.time_slices_to_stitch, 'all')
  inputs.time_slices_to_stitch = '';
end
if any(inputs.time_slices_to_stitch==',' | inputs.time_slices_to_stitch=='-')
  groups = strsplit(inputs.time_slices_to_stitch,',');
  timeslice_numbers = [];
  for g = 1:numel(groups)
    if ~isnan(str2double(groups{g}))
      timeslice_numbers = [timeslice_numbers, str2double(groups{g})];
    else
      if any(groups{g}=='-')
        toks = strsplit(groups{g},'-');
        if numel(toks) > 1
          lower = str2double(toks{1});
          upper = str2double(toks{2});
          vals = lower:upper;
          vals(isnan(vals)) = [];
          timeslice_numbers = [timeslice_numbers, vals];
        end
      end
    end
  end
  inputs.time_slices_to_stitch = num2str(timeslice_numbers);
end

inputs.time_slices_to_stitch = str2num(inputs.time_slices_to_stitch);  %#ok<ST2NM>

row_col_img_name_pattern_flag = validateIterators(inputs.source_img_name_pattern, 'r') && validateIterators(inputs.source_img_name_pattern, 'c');

filepatern_has_timeslices = validateIterators(inputs.source_img_name_pattern, 't');
if filepatern_has_timeslices
  % perform time slice discovery
  timeslice_iterator_length = find_length_iterator_string(inputs.source_img_name_pattern, 't');
  max_timeslice_nb = find_max_nb_from_padding(timeslice_iterator_length);

  % if the user defined a time slice iterator
  if max_timeslice_nb > 0
    % test to see if the user specified the time slices to stitch
    if isempty(inputs.time_slices_to_stitch)
      % determine which time slices exist to be stitched
      fnd_timeslices = [];
      i = 0; % the timeslice to test for
      while true
        if row_col_img_name_pattern_flag
          img_path_str = [inputs.source_img_dir generate_image_name_rc(inputs.source_img_name_pattern, i, 1, 1)];
        else
          img_path_str = [inputs.source_img_dir generate_image_name(inputs.source_img_name_pattern, i, 1)];
        end
        
        if validate_image_exists(img_path_str) % if there is an image with the current settings
          fnd_timeslices = [fnd_timeslices i];
        else
          % if we have tested t = 0 and 1, and an invalid timeslice is found, break the search off
          if i > 0, break; end
        end
        i = i+1;
      end
      % update the time slices to stitch with the valid time slices found
      inputs.time_slices_to_stitch = fnd_timeslices;
    else
      % the user selected time slices to stitch so loop over those time slices
    end
  else
    % there was no timeslice iterator, so there is only one time slice and it is not numbered
    inputs.time_slices_to_stitch = NaN;
  end
  
  
end


if isempty(inputs.time_slices_to_stitch)
  inputs.time_slices_to_stitch = 1;
end

if row_col_img_name_pattern_flag
  for t = 1:numel(inputs.time_slices_to_stitch)
    time_slice = inputs.time_slices_to_stitch(t);
   
    r_starts_at_0 = false;
    c_starts_at_0 = false;
    image_name = generate_image_name_rc(inputs.source_img_name_pattern, time_slice, 1, 0);
    info = dir([inputs.source_img_dir image_name]);
    if ~isempty(info) 
      r_starts_at_0 = true;
    end
    image_name = generate_image_name_rc(inputs.source_img_name_pattern, time_slice, 0,1);
    info = dir([inputs.source_img_dir image_name]);
    if ~isempty(info) 
      c_starts_at_0 = true;
    end
    
    
    if r_starts_at_0
      r = 0;
    else
      r = 1;
    end
    
    % generate and save into F_images the name of every image to be stitched
    F_images = cell(inputs.nb_vert_tiles,inputs.nb_horz_tiles);
    for i = 1:inputs.nb_vert_tiles
      if c_starts_at_0
        c = 0;
      else
        c = 1;
      end
      for j = 1:inputs.nb_horz_tiles
        F_images{i,j} = generate_image_name_rc(inputs.source_img_name_pattern, time_slice, r, c);
        c = c + 1;
      end
      r = r + 1;
    end
    
    switch inputs.starting_point
      case 'upperleft'
        % do nothing
      case 'upperright'
        F_images = fliplr(F_images);
      case 'lowerleft'
        F_images = flipud(F_images);
      case 'lowerright'
        F_images = flipud(fliplr(F_images)); %#ok<FLUDLR>
    end
    
    
    

    img_name_grid(:,:,t) = F_images;
  end
  
else
  for t = 1:numel(inputs.time_slices_to_stitch)
    time_slice = inputs.time_slices_to_stitch(t);

    index_matrix = gen_tiling_index_matrix(inputs.tiling_technique, inputs.starting_point, inputs.first_direction, inputs.nb_vert_tiles, inputs.nb_horz_tiles);
    image_name = generate_image_name(inputs.source_img_name_pattern, time_slice, 0);
    info = dir([inputs.source_img_dir image_name]);
    if ~isempty(info)
      % decrement all elements of the index matrix to account for the fact the images start a 0
      % index matrix initially is generated assuming starting image numbering at 1
      index_matrix = index_matrix - 1;
    end


    % generate and save into F_images the name of every image to be stitched
    F_images = cell(inputs.nb_vert_tiles,inputs.nb_horz_tiles);
    for j = 1:inputs.nb_horz_tiles
      for i = 1:inputs.nb_vert_tiles
        F_images{i,j} = generate_image_name(inputs.source_img_name_pattern, time_slice, index_matrix(i,j));
      end
    end

    img_name_grid(:,:,t) = F_images;
  end
end




to_stitch_time_slices = inputs.time_slices_to_stitch;


end

function max_nb = find_max_nb_from_padding(length_of_padding)
max_nb = 0;
for i = 1:length_of_padding
	max_nb = max_nb * 10;
	max_nb = max_nb + 9;
end
end

function bool = validate_image_exists(img_path) 
bool = false;
if ischar(img_path) && exist(img_path, 'file')
  bool = true;
end
end


function bool = validateNumberGtZ(x)
bool = false;
if isempty(x)
  return;
end
bool = isnumeric(x) &&  x > 0;
end

function bool = validateIterators(x, itrChar) 
bool = false;
linear_regex = ['^(.*)(\{' itrChar '+\})(.*)$'];
groups = regexp(x,linear_regex, 'tokenExtents');
if isempty(groups)
  return;
end
groups = groups{1};
if size(groups,1) == 3
  bool = true;
end
end

function bool = validateFolderExists(x)
bool = false;
if exist(x, 'dir')==7
  bool = true;
end
end

function bool = validateFilenamePattern(x)
bool = false;
bool_l = validateIterators(x, 'p');
bool_r = validateIterators(x, 'r');
bool_c = validateIterators(x, 'c');
% validate that its either row/col, or linear indexing
if bool_l
  if bool_r || bool_c
    return;
  end
  bool = true;
end
if bool_r && bool_c
  bool = true;
end
end


function bool = validateTimeslices(x)
bool = false;
if ~ischar(x)
  return;
end

if strcmpi(x, 'all')
  bool = true;
  return;
end
x = str2num(x); %#ok<ST2NM>
if any(isnan(x))
  return;
end
bool = true;
end