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

 

function I = assemble_stitched_image(source_directory, img_name_grid, global_y_img_pos, global_x_img_pos, tile_weights, fusion_method, alpha)

if ~exist('alpha', 'var')
    alpha = 1.5;
end
if ~exist('fusion_method','var')
    fusion_method = 'overlay';
end
if ~exist('tile_weights','var')
  tile_weights = ones(size(global_x_img_pos));
end

% get the size of a single image
tempI = [];
for i = 1:numel(img_name_grid)
    if ~isempty(img_name_grid{i}) && exist([source_directory img_name_grid{i}],'file')
        tempI = imread([source_directory img_name_grid{i}]);
        break;
    end
end
if isempty(tempI)
  error('assemble_stitched_image:InvalidImageNames','No images found on disk that matched the source directory and image names specified.');
end
[img_height, img_width] = size(tempI);
class_str = class(tempI);
% check the valid image types
switch class_str
    case 'single'
    case 'int8'
    case 'uint8'
    case 'int16'
    case 'uint16'
    case 'int32'
    case 'uint32'
    otherwise
        error('assemble_stitched_image:InvalidImageType','Invalid Image Type.');
end

% translate the positions to (1,1)
global_y_img_pos = round(global_y_img_pos - min(global_y_img_pos(:)) + 1);
global_x_img_pos = round(global_x_img_pos - min(global_x_img_pos(:)) + 1);

% determine how big to make the image
stitched_img_height = max(global_y_img_pos(:)) + img_height + 1;
stitched_img_width = max(global_x_img_pos(:)) + img_width + 1;
% initialize image

nb_img_tiles = numel(img_name_grid);
% create the ordering vector so that images with lower ccf values are placed before other images
% the result is the images with higher ccf values overwrite those with lower values
[~,assemble_ordering] = sort(tile_weights(:), 'ascend');
fusion_method = lower(fusion_method);

switch fusion_method
    case 'overlay'
        I = zeros(stitched_img_height, stitched_img_width, class_str);
        % Assemble images so that the lower the image numbers get priority over higher image numbers
        % the earlier images aquired are layered upon the later images
        for k = 1:nb_img_tiles
            img_idx = assemble_ordering(k);
            if ~isempty(img_name_grid{img_idx})
                % Read the current image
                current_image = read_img(source_directory, img_name_grid{img_idx});
                if ~isempty(current_image)
                  % Assemble the image to the global one
                  x_st = global_x_img_pos(img_idx);
                  x_end = global_x_img_pos(img_idx)+img_width-1;
                  y_st = global_y_img_pos(img_idx);
                  y_end = global_y_img_pos(img_idx)+img_height-1;
                  I(y_st:y_end,x_st:x_end) = current_image;
                end
            end
        end
        
    case 'average'
        I = zeros(stitched_img_height, stitched_img_width, 'single');
        countsI = zeros(stitched_img_height, stitched_img_width, 'single');
        % Assemble images
        for k = 1:nb_img_tiles
            % Read the current image
            img_idx = assemble_ordering(k);
            if ~isempty(img_name_grid{img_idx})
                current_image = single(read_img(source_directory, img_name_grid{img_idx}));
                if ~isempty(current_image)
                  % Assemble the image to the global one
                  x_st = global_x_img_pos(img_idx);
                  x_end = global_x_img_pos(img_idx)+img_width-1;
                  y_st = global_y_img_pos(img_idx);
                  y_end = global_y_img_pos(img_idx)+img_height-1;
                  I(y_st:y_end,x_st:x_end) = I(y_st:y_end,x_st:x_end) + current_image;
                  countsI(y_st:y_end,x_st:x_end) = countsI(y_st:y_end,x_st:x_end) + 1;
                end
            end
        end
        I = I./countsI;
        I = cast(I, class_str);
        
    case 'linear'
        I = zeros(stitched_img_height, stitched_img_width, 'single');
        % generate the pixel weights matrix (its the same size as the images)
        w_mat = single(compute_linear_blend_pixel_weights([img_height, img_width], alpha));
        countsI = zeros(stitched_img_height, stitched_img_width, 'single');
        % Assemble images
        for k = 1:nb_img_tiles
            % Read the current image
            img_idx = assemble_ordering(k);
            if ~isempty(img_name_grid{img_idx})
                current_image = single(read_img(source_directory, img_name_grid{img_idx}));
                if ~isempty(current_image)
                  current_image = current_image.*w_mat;
                  % Assemble the image to the global one
                  x_st = global_x_img_pos(img_idx);
                  x_end = global_x_img_pos(img_idx)+img_width-1;
                  y_st = global_y_img_pos(img_idx);
                  y_end = global_y_img_pos(img_idx)+img_height-1;
                  I(y_st:y_end,x_st:x_end) = I(y_st:y_end,x_st:x_end) + current_image;
                  countsI(y_st:y_end,x_st:x_end) = countsI(y_st:y_end,x_st:x_end) + w_mat;
                end
            end
        end
        I = I./countsI;
        I = cast(I,class_str);
        
    case 'min'
        I = zeros(stitched_img_height, stitched_img_width, class_str);
        if strcmpi(class_str, 'single') || strcmpi(class_str, 'double')
            maxval = realmax(class_str);
        else
            maxval = intmax(class_str);
        end
        % Assemble images so that the lower the image numbers get priority over higher image numbers
        % the earlier images aquired are layered upon the later images
        for k = 1:nb_img_tiles
            img_idx = assemble_ordering(k);
            if ~isempty(img_name_grid{img_idx})
                % Read the current image
                current_image = read_img(source_directory, img_name_grid{img_idx});
                if ~isempty(current_image)
                  % Assemble the image to the global one
                  x_st = global_x_img_pos(img_idx);
                  x_end = global_x_img_pos(img_idx)+img_width-1;
                  y_st = global_y_img_pos(img_idx);
                  y_end = global_y_img_pos(img_idx)+img_height-1;
                  temp = I(y_st:y_end,x_st:x_end);
                  temp(temp == 0) = maxval; % set the zeros to max value to avoid those being used
                  I(y_st:y_end,x_st:x_end) = min(current_image, temp);
                end
            end
        end
        
    case 'max'
        I = zeros(stitched_img_height, stitched_img_width, class_str);
        % Assemble images so that the lower the image numbers get priority over higher image numbers
        % the earlier images aquired are layered upon the later images
        for k = 1:nb_img_tiles
            img_idx = assemble_ordering(k);
            if ~isempty(img_name_grid{img_idx})
                % Read the current image
                current_image = read_img(source_directory, img_name_grid{img_idx});
                if ~isempty(current_image)
                  % Assemble the image to the global one
                  x_st = global_x_img_pos(img_idx);
                  x_end = global_x_img_pos(img_idx)+img_width-1;
                  y_st = global_y_img_pos(img_idx);
                  y_end = global_y_img_pos(img_idx)+img_height-1;
                  I(y_st:y_end,x_st:x_end) = max(current_image, I(y_st:y_end,x_st:x_end));
                end
            end
        end
        
    otherwise
    	% the fusion method was not valid
    	error('assemble_stitched_image:InvalidFusionMethod', ['Invalid fusion method: <' fusion_method '>']);
end




end


