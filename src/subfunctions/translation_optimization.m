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




function [Y1, X1, Y2, X2, CC1, CC2] = translation_optimization(source_directory, img_name_grid, Y1, X1, Y2, X2, CC1, CC2, max_repeatability, percent_overlap_error, estimated_overlap_x, estimated_overlap_y, log_file_path)
startTime = tic;
% Get dimensions of an image
[nb_vertical_tiles, nb_horizontal_tiles] = size(img_name_grid);
stats = [];
for i = 1:numel(img_name_grid)
  if ~isempty(img_name_grid{i}) && exist([source_directory img_name_grid{i}],'file')
    stats = imfinfo([source_directory img_name_grid{i}]);
    break;
  end
end
if isempty(stats)
  error('No images found on disk that matched the source directory and image names specified.');
end
size_I = [stats.Height stats.Width];

% correct the North translations
print_to_command('Correcting North Translations', log_file_path);
try
  [X1, Y1, ConfidenceIndex1, repeatability1] = correct_translation_model(X1, Y1, CC1, source_directory, img_name_grid, size_I, max_repeatability, percent_overlap_error, estimated_overlap_y, StitchingConstants.NORTH, log_file_path);
catch err
	warning(err.getReport());
  warning('Translation Model Correction Failed... attempting to recover.');
  if isnan(max_repeatability) || isnan(estimated_overlap_y)
    error('Unable to stitch. Set an estimated vertical overlap, estimated horizontal overlap, and repeatabilty to try again.');
  end
  repeatability1 = max_repeatability;
  ConfidenceIndex1 = zeros(size(X1));
  % replace translations with basic estimates and let hill climbing attempt to find a solution
  y = round((1-estimated_overlap_y/100)*size_I(1));
  X1 = zeros(size(X1));
  Y1 = y.*ones(size(Y1));
  Y1(1,:) = NaN;
  X1(1,:) = NaN;
end

% correct the West translations
print_to_command('Correcting West Translations', log_file_path);
try
  [X2, Y2, ConfidenceIndex2, repeatability2] = correct_translation_model(X2, Y2, CC2, source_directory, img_name_grid, size_I, max_repeatability, percent_overlap_error, estimated_overlap_x, StitchingConstants.WEST, log_file_path);
catch err
	warning(err.getReport());
  warning('Translation Model Correction Failed... attempting to recover.');
  if isnan(max_repeatability) || isnan(estimated_overlap_x)
    error('Unable to stitch. Set an estimated vertical overlap, estimated horizontal overlap, and repeatabilty to try again.');
  end
  repeatability2 = max_repeatability;
  ConfidenceIndex2 = zeros(size(X2));
  % replace translations with basic estimates and let hill climbing attempt to find a solution
  x = round((1-estimated_overlap_x/100)*size_I(2));
  Y2 = zeros(size(Y2));
  X2 = x.*ones(size(X2));
  X2(:,1) = NaN;
  Y2(:,1) = NaN;
end


print_to_command(['Valid Vertical translation found: ' num2str(nnz(ConfidenceIndex1>=StitchingConstants.VALID_TRANSLATION_CC_OFFSET)) '/' num2str(numel(ConfidenceIndex1))], log_file_path);
print_to_command(['Valid Horizontal translation found: ' num2str(nnz(ConfidenceIndex2>=StitchingConstants.VALID_TRANSLATION_CC_OFFSET)) '/' num2str(numel(ConfidenceIndex2))], log_file_path);
print_to_command(['Vertical translation Repeatability: ' num2str(repeatability1)], log_file_path);
print_to_command(['Horizontal translation Repeatability: ' num2str(repeatability2)], log_file_path);



% repeatability search range is 2r +1 (to encompase +-r)
r = max(repeatability1, repeatability2);
r = 2*max(r, 1) + 1;
print_to_command(['Overall translation Repeatability: ' num2str(r)], log_file_path);
	
% build the cross correlation search bounds and perform the search
for j = 1:nb_horizontal_tiles
	print_to_command(['  ' num2str(j) '/' num2str(nb_horizontal_tiles)], log_file_path);
	% loop over the rows correcting invalid correlation values
	parfor i = 1:nb_vertical_tiles
		
		% if not the first column, and both images exist
		if j ~= 1 && ~isempty(img_name_grid{i,j-1}) && ~isempty(img_name_grid{i,j})
			bounds = [Y2(i,j)-r, Y2(i,j)+r, X2(i,j)-r, X2(i,j)+r];
			[Y2(i,j), X2(i,j), CC2(i,j)] = cross_correlation_hill_climb(source_directory, img_name_grid{i,j-1}, img_name_grid{i,j}, bounds, X2(i,j), Y2(i,j));
		end
		
		% if not the first row, and both images exist
		if i ~= 1 && ~isempty(img_name_grid{i-1,j}) && ~isempty(img_name_grid{i,j})
			bounds = [Y1(i,j)-r, Y1(i,j)+r, X1(i,j)-r, X1(i,j)+r];
			[Y1(i,j), X1(i,j), CC1(i,j)] = cross_correlation_hill_climb(source_directory, img_name_grid{i-1,j}, img_name_grid{i,j}, bounds, X1(i,j), Y1(i,j));
		end
	end
  
end



% % adjust the correlation value to reflect the confidence index, if it was a valid t (CI >= 4), give it a
% higher weight than the other ts that had a cross correlation search performed
ConfidenceIndex1(ConfidenceIndex1 < StitchingConstants.VALID_TRANSLATION_CC_OFFSET) = 0;
ConfidenceIndex2(ConfidenceIndex2 < StitchingConstants.VALID_TRANSLATION_CC_OFFSET) = 0;
CC1 = CC1 + ConfidenceIndex1;
CC2 = CC2 + ConfidenceIndex2;

s = StitchingStatistics.getInstance;
s.global_optimization_time = toc(startTime);
end



