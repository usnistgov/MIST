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




function write_global_positions_to_csv(input_directory, img_name_grid, global_y_img_pos, global_x_img_pos,CC1,CC2, output_file)

fh = fopen(output_file, 'w');

[r,c] = size(img_name_grid);

for i = 1:r
  for j = 1:c
    if ~isempty(img_name_grid{i,j}) && exist([input_directory img_name_grid{i,j}],'file')
      cc = max(CC1(i,j), CC2(i,j));
      if isnan(cc)
        cc = -1;
      end
      fprintf(fh, 'file: %s; corr: %1.10f; position: (%d, %d); grid: (%d, %d);\n', img_name_grid{i,j}, cc, global_x_img_pos(i,j)-1, global_y_img_pos(i,j)-1, j-1, i-1);
    end
  end
end

fclose(fh);
