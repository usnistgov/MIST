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




function overlap = compute_image_overlap(X, Y, CC, source_directory, img_name_grid, direction)

% sort the correlation values so that high quality values are used to compute overlap
[ncc_vals, idx] = sort(CC(:));
% idx contains linear index locations of the top translations in CC1

% remove the NaN translations from the idx vector
idx(isnan(ncc_vals)) = [];
% keep only the top half of the values
idx = idx(floor(size(idx,1)/2):end,:);

% determine how many translations are left to compute the overlap estimate
nb_to_check = min(StitchingConstants.MIN_NB_TRANSLATION_FOR_OVERLAP_COMPUTATION, size(idx,1));

% extract the top nb_to_check translations
% get their std of the overlap regions
% remove the bottom half of the std values from contention
% compute overlap 
top_trans_idx = idx(end-(nb_to_check-1):end);
std_val = NaN(size(top_trans_idx));
img_stats = [];
for k = 1:numel(top_trans_idx)
    [i,j] = ind2sub(size(X), top_trans_idx(k));
    x = X(i,j);
    y = Y(i,j);
    if isempty(img_stats)
      img_stats = imfinfo([source_directory img_name_grid{i,j}]);
    end
    img_file_path2 = [source_directory img_name_grid{i,j}];
    
    if direction == StitchingConstants.NORTH
      img_file_path1 = [source_directory img_name_grid{i-1,j}]; % I1 is above I2

      if x>=0 % For positive translation on the x axis
        img_sub_region1 = {[y+1, img_stats.Height],[x+1,img_stats.Width]};
        img_sub_region2 = {[1, img_stats.Height-y],[1,img_stats.Width-x]};
      else % For negative translation on the x axis
        img_sub_region1 = {[y+1,img_stats.Height],[1,img_stats.Width+x]};
        img_sub_region2 = {[1,img_stats.Height-y],[-x+1,img_stats.Width]};
      end
    else
      img_file_path1 = [source_directory img_name_grid{i,j-1}]; % I1 is left I2

      if y>=0 % For positive translation on the y axis
        img_sub_region1 = {[1,img_stats.Height-y],[x+1,img_stats.Width]};
        img_sub_region2 = {[y+1,img_stats.Height],[1,img_stats.Width-x]};
      else % For negative translation on the y axis
        img_sub_region1 = {[-y+1, img_stats.Height],[x+1,img_stats.Width]};
        img_sub_region2 = {[1,img_stats.Height+y],[1,img_stats.Width-x]};
      end
    end
    
    
    std_down = std2(double(imread(img_file_path1, 'PixelRegion', img_sub_region1)));
    std_up = std2(double(imread(img_file_path2, 'PixelRegion', img_sub_region2)));
    
    std_val(k) = min(std_down, std_up);
end

std_val(isnan(std_val)) = [];
std_threshold = median(std_val);
valid_idx = std_val >= std_threshold;
if direction == StitchingConstants.NORTH
  med_translation = median(Y(top_trans_idx(valid_idx)));
  overlap = round(100*(1 - med_translation/img_stats.Height));  
else
  med_translation = median(X(top_trans_idx(valid_idx)));
  overlap = round(100*(1 - med_translation/img_stats.Width));
end

assert(~isnan(overlap), 'Estimated Overlap could not be computed; stitch it using another method');


end

