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




function [tiling_indicator, tiling_coeff, global_y_img_pos, global_x_img_pos] = minimum_spanning_tree(Y1, X1, Y2, X2, CC1, CC2)

[nb_vertical_tiles, nb_horizontal_tiles] = size(Y1);
% Initialize the starting point of each image. These starting points are computed relative to Matlab image coordinate system ==>
% x is left to right and y is up to down. global_y_img_pos is vertical or y and global_x_img_pos is horizontal or x
global_y_img_pos = zeros(nb_vertical_tiles,nb_horizontal_tiles);
global_x_img_pos = zeros(nb_vertical_tiles,nb_horizontal_tiles);

% Initialize the tiling indicator matrix that gives us the direction by which images were stitched 
% in the vertical direction up 11, down 12
% in the horizontal direction right 21, left 22
% This means that if an element of tiling indicator (i,j) has 11 in it, that means that this tile was stitched to the one above it (i-1,j) and
% if an element has 21 in it that means that this tile was stitched to the one on its right (i,j+1) in the global image 
tiling_indicator = zeros(nb_vertical_tiles,nb_horizontal_tiles);

[val1, indx1] = max(CC1(:));
[val2, indx2] = max(CC2(:));
if val1 > val2
    [ii, jj] = ind2sub([nb_vertical_tiles, nb_horizontal_tiles], indx1);
else
    [ii, jj] = ind2sub([nb_vertical_tiles, nb_horizontal_tiles], indx2);
end
tiling_indicator(ii,jj) = StitchingConstants.MST_START_TILE;

% Compute tiles positions
% correlations are inverted because for this application we actually want a maximum spanning tree
[tiling_indicator, global_y_img_pos, global_x_img_pos, tiling_coeff] = minimum_spanning_tree_worker(tiling_indicator, global_y_img_pos, global_x_img_pos, ...
    Y1, X1, Y2, X2, -CC1, -CC2); 

tiling_coeff = -tiling_coeff;
tiling_coeff(ii,jj) = 5; % the starting point

tiling_coeff(~tiling_indicator) = NaN;
global_x_img_pos(~tiling_indicator) = NaN;
global_y_img_pos(~tiling_indicator) = NaN;
tiling_indicator(~tiling_indicator) = NaN;

% translate the positions to (1,1)
global_y_img_pos = global_y_img_pos - min(global_y_img_pos(:)) + 1;
global_x_img_pos = global_x_img_pos - min(global_x_img_pos(:)) + 1;



end


function [tiling_indicator, global_y_img_pos, global_x_img_pos, tiling_coeff] = minimum_spanning_tree_worker(tiling_indicator, global_y_img_pos, global_x_img_pos, Y1, X1, Y2, X2, CC1, CC2)

% Initialize the minimum spanning tree value and set the first value in tiling_coeff to the highest = -1.
[tile_y, tile_x] = size(Y1);
mst_value = 0;
tiling_coeff = zeros(tile_y, tile_x);
tiling_coeff(tiling_indicator > 0) = -1;

% Keep on finding the next vertice in the tree until all is found. The first vertice is always the position of the first image 
% defined in global_y_img_pos(1,1) and global_x_img_pos(1,1)
for j = 2:numel(tiling_indicator)
    
    % Check the vertices that are already connected to the tree
    [I, J] = find(tiling_indicator);
    
    % Initialize the minimum coefficient value
    mst_min = Inf;
    
    % Scan all the unconnected neighbors of the connected vertices and add the one with the lowest correlation coefficient to the tree
    for i = 1:length(I)
        % Check the neighbor below
        % Check if it is valid and isn't already stitched and that the correlation coefficient is minimal
        if I(i)<tile_y && tiling_indicator(I(i)+1,J(i)) == 0 && CC1(I(i)+1,J(i)) < mst_min
            % update the minimum coefficient value
            mst_min = CC1(I(i)+1,J(i));
            stitching_index = StitchingConstants.MST_CONNECTED_NORTH; % index that indicates the stitching direction of the minimal coefficient
            mst_i = I(i);
            mst_j = J(i);
        end
        
        % Check the neighbor above
        % Check if it is valid and isn't already stitched and that the correlation coefficient is minimal
        if I(i)>1 && tiling_indicator(I(i)-1,J(i)) == 0 && CC1(I(i),J(i)) < mst_min
            % update the minimum coefficient value
            mst_min = CC1(I(i),J(i));
            stitching_index = StitchingConstants.MST_CONNECTED_SOUTH; % index that indicates the stitching direction of the minimal coefficient
            mst_i = I(i);
            mst_j = J(i);
        end
        
        % Check the neighbor to the right
        % Check if it is valid and isn't already stitched and that the correlation coefficient is minimal
        if J(i)<tile_x && tiling_indicator(I(i),J(i)+1) == 0 && CC2(I(i),J(i)+1) < mst_min
            % update the minimum coefficient value
            mst_min = CC2(I(i),J(i)+1);
            stitching_index = StitchingConstants.MST_CONNECTED_LEFT; % index that indicates the stitching direction of the minimal coefficient
            mst_i = I(i);
            mst_j = J(i);
        end
        
        % Check the neighbor to the left
        % Check if it is valid and isn't already stitched and that the correlation coefficient is minimal
        if J(i)>1 && tiling_indicator(I(i),J(i)-1) == 0 && CC2(I(i),J(i)) < mst_min
            % update the minimum coefficient value
            mst_min = CC2(I(i),J(i));
            stitching_index = StitchingConstants.MST_CONNECTED_RIGHT; % index that indicates the stitching direction of the minimal coefficient
            mst_i = I(i);
            mst_j = J(i);
        end
    end
    
    % update the minimum spanning tree value and the tiling coefficient
    mst_value = mst_value + mst_min;
    
    % Compute the starting position of the chosen tile
    % Check the neighbor below
    if stitching_index == StitchingConstants.MST_CONNECTED_NORTH
        global_y_img_pos(mst_i+1,mst_j) = global_y_img_pos(mst_i,mst_j) + Y1(mst_i+1,mst_j);
        global_x_img_pos(mst_i+1,mst_j) = global_x_img_pos(mst_i,mst_j) + X1(mst_i+1,mst_j);
        
        % update tiling indicator
        tiling_indicator(mst_i+1,mst_j) = StitchingConstants.MST_CONNECTED_NORTH;
        tiling_coeff(mst_i+1,mst_j) = mst_min;
    end
    
    % Check the neighbor above
    if stitching_index == StitchingConstants.MST_CONNECTED_SOUTH
        global_y_img_pos(mst_i-1,mst_j) = global_y_img_pos(mst_i,mst_j) - Y1(mst_i,mst_j);
        global_x_img_pos(mst_i-1,mst_j) = global_x_img_pos(mst_i,mst_j) - X1(mst_i,mst_j);
        
        % update tiling indicator
        tiling_indicator(mst_i-1,mst_j) = StitchingConstants.MST_CONNECTED_SOUTH;
        tiling_coeff(mst_i-1,mst_j) = mst_min;
    end
    
    % Check the neighbor to the right
    if stitching_index == StitchingConstants.MST_CONNECTED_LEFT
        global_y_img_pos(mst_i,mst_j+1) = global_y_img_pos(mst_i,mst_j) + Y2(mst_i,mst_j+1);
        global_x_img_pos(mst_i,mst_j+1) = global_x_img_pos(mst_i,mst_j) + X2(mst_i,mst_j+1);
        
        % update tiling indicator
        tiling_indicator(mst_i,mst_j+1) = StitchingConstants.MST_CONNECTED_LEFT;
        tiling_coeff(mst_i,mst_j+1) = mst_min;
    end
    
    % Check the neighbor to the left
    if stitching_index == StitchingConstants.MST_CONNECTED_RIGHT
        global_y_img_pos(mst_i,mst_j-1) = global_y_img_pos(mst_i,mst_j) - Y2(mst_i,mst_j);
        global_x_img_pos(mst_i,mst_j-1) = global_x_img_pos(mst_i,mst_j) - X2(mst_i,mst_j);
        
        % update tiling indicator
        tiling_indicator(mst_i,mst_j-1) = StitchingConstants.MST_CONNECTED_RIGHT;
        tiling_coeff(mst_i,mst_j-1) = mst_min;
    end
end

end




