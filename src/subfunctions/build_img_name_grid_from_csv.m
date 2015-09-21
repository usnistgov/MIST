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



function img_name_grid = build_img_name_grid_from_csv(filename)
if ~iscell(filename)
  filename = {filename};
end

holder_F_images = cell(numel(filename),1);
for fnn = 1:numel(filename)
  if filename{fnn} == 0
    continue;
  end
  
  if strcmpi(filename{fnn}(end-3:end), '.csv')
    F_images = load_csv_file_into_cell_array(filename{fnn});
  else
    F_images = importdata([pathname filename{fnn}]);
  end
  
  holder_F_images{fnn} = F_images;
end


% check that the dimensions are the same across all the time sequences
for i = 2:numel(holder_F_images)
  assert(size(holder_F_images{fnn},1) == size(holder_F_images{fnn-1},1),'Dimensions must match between time slices');
  assert(size(holder_F_images{fnn},2) == size(holder_F_images{fnn-1},2),'Dimensions must match between time slices');
end

% build F_images into a (x,y,timeslice) cell array
img_name_grid = cell(size(holder_F_images{1},1),size(holder_F_images{1},2),numel(holder_F_images));
for i = 1:numel(holder_F_images)
  img_name_grid(:,:,i) = holder_F_images{i};
end


for i = 1:numel(img_name_grid)
  img_name_grid{i} = regexprep(img_name_grid{i},'\s','');
end

end