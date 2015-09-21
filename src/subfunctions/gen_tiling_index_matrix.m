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


% This function outputs the index_matrix of the image in at location (i,j) in the reconstructed image using the following four inputs:
%  
% tiling_technique: (1) Combing, (2) Continous
% starting_point: (1) upper left, (2) upper right, (3) lower right, (4) lower left
% first_direction: (1) vertical, (2) Horizontal --> How did the microscope started tiling
% nb_horizontal_tiles and nb_vertical_tiles are the number of tiles in both direction
% 

function index_matrix = gen_tiling_index_matrix(tiling_technique, starting_point, first_direction, nb_vert_tiles, nb_horz_tiles)

% if acquisition_mode is combing
% Create numbers
index_matrix = 1:nb_vert_tiles*nb_horz_tiles;

% if First_direction = 2, flip dimensions
if strcmpi(first_direction, 'horizontal')
  t = nb_vert_tiles; 
  nb_vert_tiles = nb_horz_tiles; 
  nb_horz_tiles = t; 
end

% Reshape matrix
index_matrix = reshape(index_matrix, nb_vert_tiles, nb_horz_tiles);

% if acquisition_mode is continous, circule every other column in the matrix
if strcmpi(tiling_technique, 'continuous')
    % Get every other column
    temp_matrix = index_matrix(:,2:2:end);
    % Circle every other column
    index_matrix(:,2:2:end) = temp_matrix(sort(1:size(temp_matrix,1), 'descend'),:);
end

% if numbering is horizontal, transpose the matrix
if strcmpi(first_direction, 'horizontal')
  index_matrix = index_matrix'; 
end

% TAKE Starting_point INTO ACCOUNT
% if Starting_point is upper left, return
if strcmpi(starting_point, 'upperleft')
  return;
end

% shift numbering to upper right
if strcmpi(starting_point, 'upperright')
  index_matrix = index_matrix(:,sort(1:size(index_matrix,2), 'descend'));
  return;
end

% shift numbering to lower right
if strcmpi(starting_point, 'lowerright')
    index_matrix = index_matrix(:,sort(1:size(index_matrix,2), 'descend'));
    index_matrix = index_matrix(sort(1:size(index_matrix,1), 'descend'),:);
    return
end

% shift numbering to lower left
if strcmpi(starting_point, 'lowerleft')
  index_matrix = index_matrix(sort(1:size(index_matrix,1), 'descend'),:); 
  return;
end

end


