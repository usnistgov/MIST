% Disclaimer:  IMPORTANT:  This software was developed at the National Institute of Standards
% and Technology by employees of the Federal Government in the course of their official duties.
% Pursuant to title 17 Section 105 of the United States Code this software is not subject to
% copyright protection and is in the public domain. This is an experimental system. NIST
% assumes no responsibility whatsoever for its use by other parties, and makes no guarantees,
% expressed or implied, about its quality, reliability, or any other characteristic. We would
% appreciate acknowledgment if the software is used. This software can be redistributed and/or
% modified freely provided that any derivative works bear some notice that they are derived
% from it, and any modified versions bear some notice that they have been modified.



function [t_Y1, t_X1, t_Y2, t_X2, CC1, CC2] = compute_pciam(source_directory, img_name_grid, log_file_path)
startTime = tic;
assert(nargin >= 2, 'Missing input arguments, image directory path and image names cell array');
if ~exist('log_file_path', 'var')
  log_file_path = [];
end

assert(isa(img_name_grid,'cell'), 'Grid of Image file names must be a cell array of strings');

% ensure non string elements within F_images are set to empty cells so that they can be ignored later
for i = 1:numel(img_name_grid)
  if ~ischar(img_name_grid{i}) || isempty(img_name_grid{i})
    img_name_grid{i} = [];
  end
end

% check to make sure all of the images exist on disk
missing_img = false;
for i = 1:numel(img_name_grid)
  if ~isempty(img_name_grid{i}) && ~exist([source_directory img_name_grid{i}], 'file')
    missing_img = true;
    break;
  end
end
assert(~missing_img, 'Images(s) from the image name grid could not be found on disk');


[nb_vertical_tiles, nb_horizontal_tiles] = size(img_name_grid);

% initialize the translation matricies
t_Y1 = NaN(nb_vertical_tiles,nb_horizontal_tiles);        t_Y2 = NaN(nb_vertical_tiles,nb_horizontal_tiles);
t_X1 = NaN(nb_vertical_tiles,nb_horizontal_tiles);        t_X2 = NaN(nb_vertical_tiles,nb_horizontal_tiles);
CC1 = NaN(nb_vertical_tiles,nb_horizontal_tiles);         CC2 = NaN(nb_vertical_tiles,nb_horizontal_tiles);

% if the log file has been specified, setup logging
if ~isempty(log_file_path)
  debug_fh = fopen(log_file_path, 'a');
  fprintf(debug_fh, '--->Compute Translations<---\n');
else
  debug_fh = 0;
end

% init the cell arrays to hold a single column of image and FFT data
image_col = cell(nb_vertical_tiles, 1);
fft_col = cell(nb_vertical_tiles, 1);

% loop over the image tiles computing translations
for j = 1:nb_horizontal_tiles
  print_to_command(['  col: ' num2str(j) '/' num2str(nb_horizontal_tiles)]);
  for i = 1:nb_vertical_tiles
    % read image from disk
    I1 = read_img(source_directory, img_name_grid{i,j});
    % compute FFT
    FFT1 = fft2(I1);
    
    if i > 1
      % compute pciam north
      if debug_fh > 0
        fprintf(debug_fh, 'N: %s -> %s ', img_name_grid{i,j}, img_name_grid{i-1,j});
      end
      [t_Y1(i,j), t_X1(i,j), CC1(i,j)] = pciam(image_col{i-1}, fft_col{i-1}, I1, FFT1, StitchingConstants.NORTH, StitchingConstants.NB_FFT_PEAKS, StitchingConstants.MIN_DIST_BETWEEN_PEAKS, debug_fh);
    end
    if j > 1
      % perform pciam west
      if debug_fh > 0
        fprintf(debug_fh, 'W: %s -> %s ', img_name_grid{i,j}, img_name_grid{i,j-1});
      end
      [t_Y2(i,j), t_X2(i,j), CC2(i,j)] = pciam(image_col{i}, fft_col{i}, I1, FFT1, StitchingConstants.WEST, StitchingConstants.NB_FFT_PEAKS, StitchingConstants.MIN_DIST_BETWEEN_PEAKS, debug_fh);
    end
    
    % overwrite the image and fft data in cell array
    image_col{i} = I1;
    fft_col{i} = FFT1;
  end
end
if ~isempty(log_file_path)
  fclose(debug_fh);
end

s = StitchingStatistics.getInstance;
s.relative_displacement_time = toc(startTime);

end

function img = read_img(srcDir, imgName)
if ~isempty(imgName)
  img = double(imread([srcDir imgName]));
  assert(numel(size(img)) == 2, sprintf('Input images must contain a 2D raster of pixels, RGB images not allows.\nInvalid image: %s', [srcDir imgName]));
else
  img = [];
end
end

