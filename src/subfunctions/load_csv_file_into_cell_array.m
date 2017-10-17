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




function cell_array = load_csv_file_into_cell_array(filepath) 

cell_array = cell(0);

if ~strcmpi(filepath(end-3:end), '.csv')
  return;
end
if ~exist(filepath, 'file')
  return;
end

fh = fopen(filepath, 'r');
i = 1;
while ~feof(fh)
  line = fgetl(fh);
  vals = strsplit(line, ',','CollapseDelimiters',false);
  for j = 1:numel(vals)
    cell_array{i,j} = vals{j};
  end
  i = i + 1;
end
fclose(fh);


  
  
  
