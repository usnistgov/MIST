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




function [X, Y, ConfidenceIndex, repeatability] = correct_translation_model(X, Y, CC, source_directory, img_name_grid, size_I, max_repeatability, percent_overlap_error, overlap, direction, log_file_path)

nb_rows = size_I(1);
nb_cols = size_I(2);

% compute the estimated overlap
if isnan(overlap)
  overlap = compute_image_overlap(X, Y, source_directory, img_name_grid, direction);
end

% bound the computed image overlap (0,100)
if overlap >= 100-percent_overlap_error
  overlap = 100-percent_overlap_error;
end
if overlap <= percent_overlap_error
  overlap = percent_overlap_error;
end
print_to_command(sprintf('Overlap estimate: %g', overlap), log_file_path);


% compute range bounds
if direction == StitchingConstants.NORTH
  ty_min = nb_rows - (overlap + percent_overlap_error)*nb_rows/100;
  ty_max = nb_rows - (overlap - percent_overlap_error)*nb_rows/100;
  
  % the valid translations are within the range bounds
  valid_translations_index = Y>=ty_min & Y<=ty_max;
  s = StitchingStatistics.getInstance;
  s.north_overlap = overlap;
  s.north_min_range_filter = ty_min;
  s.north_max_range_filter = ty_max;
  
  print_to_command(sprintf('Translation range filer: min=%g, max=%g', ty_min, ty_max), log_file_path);
  
else
  tx_min = nb_cols - (overlap + percent_overlap_error)*nb_cols/100;
  tx_max = nb_cols - (overlap - percent_overlap_error)*nb_cols/100;
  
  % the valid translations are within the range bounds
  valid_translations_index = X>=tx_min & X<=tx_max;
  s = StitchingStatistics.getInstance;
  s.west_overlap = overlap;
  s.west_min_range_filter = tx_min;
  s.west_max_range_filter = tx_max;
  
  print_to_command(sprintf('Translation range filer: min=%g, max=%g', tx_min, tx_max), log_file_path);
end


% valid translations must have a cc of >= 0.5
valid_translations_index(CC < StitchingConstants.VALID_TRANSLATION_HEURISIC_CC_THRESHOLD) = 0;

% test for existance of valid translations
if nnz(valid_translations_index) == 0
  ConfidenceIndex = zeros(size(img_name_grid));
  if direction == StitchingConstants.WEST
    print_to_command('Waring: no good translations found for WEST direction. Estimated translations generated from overlap.');
    est_translation = round(nb_cols*(1- overlap/100));
    Y(~isnan(Y)) = 0;
    X(~isnan(X)) = est_translation;
  else
    print_to_command('Waring: no good translations found for NORTH direction. Estimated translations generated from overlap.');
    est_translation = round(nb_rows*(1- overlap/100));
    X(~isnan(X)) = 0;
    Y(~isnan(Y)) = est_translation;
  end
  
  repeatability = 0;
  if ~isnan(max_repeatability)
    repeatability = max_repeatability;
  end
  s = StitchingStatistics.getInstance;
  s.north_repeatability = repeatability;
  return;
end


% filter out translation outliers
w = 1.5; % default outlier threshold is w = 1.5

% filter translations using outlier
if direction == StitchingConstants.NORTH
  
  % filter Y components of the translations
  T = Y(valid_translations_index);
  % only filter if there are more than 3 translations
  if numel(T) > 3
    q1 = median(T(T<median(T(:))));
    q3 = median(T(T>median(T(:))));
    iqd = abs(q3-q1);
    
    valid_translations_index(Y < (q1 - w*iqd)) = 0;
    valid_translations_index((q3 + w*iqd) < Y) = 0;
  end
  
  % filter X components of the translations
  T = X(valid_translations_index);
  % only filter if there are more than 3 translations
  if numel(T) > 3
    q1 = median(T(T<median(T(:))));
    q3 = median(T(T>median(T(:))));
    iqd = abs(q3-q1);
    
    valid_translations_index(X < (q1 - w*iqd)) = 0;
    valid_translations_index((q3 + w*iqd) < X) = 0;
  end
else
  
  % filter X components of the translations
  T = X(valid_translations_index);
  % only filter if there are more than 3 translations
  if numel(T) > 3
    q1 = median(T(T<median(T(:))));
    q3 = median(T(T>median(T(:))));
    iqd = abs(q3-q1);
    
    valid_translations_index(X < (q1 - w*iqd)) = 0;
    valid_translations_index((q3 + w*iqd) < X) = 0;
  end
  
  % filter Y components of the translations
  T = Y(valid_translations_index);
  % only filter if there are more than 3 translations
  if numel(T) > 3
    q1 = median(T(T<median(T(:))));
    q3 = median(T(T>median(T(:))));
    iqd = abs(q3-q1);
    
    valid_translations_index(Y < (q1 - w*iqd)) = 0;
    valid_translations_index((q3 + w*iqd) < Y) = 0;
  end
end



% test for existance of valid translations
if nnz(valid_translations_index) == 0
  ConfidenceIndex = zeros(size(img_name_grid));
  if direction == StitchingConstants.WEST
    print_to_command('Waring: no good translations found for WEST direction. Estimated translations generated from overlap.');
    est_translation = round(nb_cols*(1- overlap/100));
    Y(~isnan(Y)) = 0;
    X(~isnan(X)) = est_translation;
  else
    print_to_command('Waring: no good translations found for NORTH direction. Estimated translations generated from overlap.');
    est_translation = round(nb_rows*(1- overlap/100));
    X(~isnan(X)) = 0;
    Y(~isnan(Y)) = est_translation;
  end
  
  repeatability = 0;
  if ~isnan(max_repeatability)
    repeatability = max_repeatability;
  end
  return;
end

% compute repeatability
if direction == StitchingConstants.NORTH
  rx = ceil((max(X(valid_translations_index)) - min(X(valid_translations_index)))/2);
  tY = Y; % temporarily remove non valid translatons to compute Y range
  tY(~valid_translations_index) = NaN;
  ry = max(ceil((max(tY,[],2) - min(tY,[],2))/2));
  repeatability = max(rx,ry);
  
  s = StitchingStatistics.getInstance;
  s.north_repeatability = repeatability;
else
  ry = ceil((max(Y(valid_translations_index)) - min(Y(valid_translations_index)))/2);
  tX = X; % temporarily remove non valid translatons to compute X range
  tX(~valid_translations_index) = NaN;
  rx = max(ceil((max(tX,[],1) - min(tX,[],1))/2));
  repeatability = max(rx,ry);
  
  s = StitchingStatistics.getInstance;
  s.west_repeatability = repeatability;
end
print_to_command(sprintf('Computed repeatability: %g', repeatability), log_file_path);

% if the user defined a repeatabilty, use that one
if ~isnan(max_repeatability)
  repeatability = max_repeatability;
end
assert(~isnan(repeatability),'Repeatability could not be found; stitch using another method');


% Filter translations to ensure all are within median+-r
if direction == StitchingConstants.NORTH
  for i = 1:size(valid_translations_index,1)
    valid_translations_index(i,:) = range_filter_vec(X(i,:),Y(i,:),CC(i,:),repeatability,valid_translations_index(i,:));
  end
else
  for j = 1:size(valid_translations_index,2)
    valid_translations_index(:,j) = range_filter_vec(X(:,j),Y(:,j),CC(:,j),repeatability,valid_translations_index(:,j));
  end
end

% remove invalid translations
X(~valid_translations_index) = NaN;
Y(~valid_translations_index) = NaN;


% find the rows/columns that have no valid translations
missing_index = false(size(valid_translations_index));
if direction == StitchingConstants.NORTH
  % Find the rows that are missing all their values, these will later be replaced by the global median
  idxX = sum(isnan(X),2) == size(X,2);
  idxX(1) = 0; % remove the first row
  missing_index(idxX, :) = 1;

  idxY = sum(isnan(Y),2) == size(Y,2);
  idxY(1) = 0; % remove the first column
  missing_index(idxY, :) = 1;
else
  % Find the columns that are missing all their values, these will later be replaced by the global median
  idxX = sum(isnan(X),1) == size(X,1);
  idxX(1) = 0; % remove the first val
  missing_index(:,idxX) = 1;

  idxY = sum(isnan(Y),1) == size(Y,1);
  idxY(1) = 0; % remove the first val
  missing_index(:,idxY) = 1;
end

if direction == StitchingConstants.NORTH
  % fill in any invalid translations with the row wise median
  for i = 1:size(X,1)
    X(i,:) = replace_NaN_with_median(X(i,:));
    Y(i,:) = replace_NaN_with_median(Y(i,:));
  end
else
  % fill in any invalid translations with the column wise median
  for j = 1:size(X,2)
    X(:,j) = replace_NaN_with_median(X(:,j));
    Y(:,j) = replace_NaN_with_median(Y(:,j));
  end
end



% replace any completly missed rows by searching within the backlash using cross correlation
if any(missing_index(:))
  X(missing_index) = round(median(X(valid_translations_index)));
  Y(missing_index) = round(median(Y(valid_translations_index)));
end

if direction == StitchingConstants.NORTH    
  s = StitchingStatistics.getInstance;
  s.north_nb_tiles = nnz(~isnan(CC));
  s.north_nb_valid_tiles = nnz(valid_translations_index);
else
  s = StitchingStatistics.getInstance;
  s.west_nb_tiles = nnz(~isnan(CC));
  s.west_nb_valid_tiles = nnz(valid_translations_index);
end
ConfidenceIndex = zeros(size(valid_translations_index));
ConfidenceIndex(valid_translations_index) = StitchingConstants.VALID_TRANSLATION_CC_OFFSET;

% reassert the missing tiles, to avoid propagating estimated translations for tiles that do not exist
for j = 1:size(img_name_grid,2)
  for i = 1:size(img_name_grid,1)
    if isempty(img_name_grid{i,j})
      X(i,j) = NaN;
      Y(i,j) = NaN;
      if direction == StitchingConstants.NORTH
        if i ~= size(img_name_grid,1)
          % remove the translation to the tile that would be below
          X(i+1,j) = NaN;
          Y(i+1,j) = NaN;
        end
      else
        if j ~= size(img_name_grid,2)
          % remove the translation to the tile that would be to the right
          X(i,j+1) = NaN;
          Y(i,j+1) = NaN;
        end
      end
    end
  end
end



end



function V = range_filter_vec(X,Y,CC,r,V)
V = logical(V);
% if the entire vector is invalid, skip it
if ~any(V), return; end

% compute the median row values
median_x = median(X(V));
median_y = median(Y(V));

% find the translations within r of the median
valid_x_row = (X >= (median_x-r)) & (X <= (median_x+r));
valid_y_row = (Y >= (median_y-r)) & (Y <= (median_y+r));
% remove any translation with a CCF < 0.5
valid_x_row = valid_x_row & CC >= StitchingConstants.VALID_TRANSLATION_HEURISIC_CC_THRESHOLD;
valid_y_row = valid_y_row & CC >= StitchingConstants.VALID_TRANSLATION_HEURISIC_CC_THRESHOLD;

V = valid_x_row & valid_y_row;

end


function A = replace_NaN_with_median(A)
    idx = isnan(A);
    if any(idx) && nnz(idx) ~= numel(idx)
      % this implements round towards infinity, as opposed to matlabs round away from 0
      A(idx) = floor(median(A(~idx)) + 0.5);
    end
end
  



