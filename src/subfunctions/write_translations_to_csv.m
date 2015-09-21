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




function write_translations_to_csv(img_name_grid, X1,Y1,CC1,X2,Y2,CC2, output_file)

fh = fopen(output_file, 'w');

[r,c] = size(img_name_grid);

for i = 1:r
    for j = 1:c
      if ~isempty(img_name_grid{i,j})
        if j > 1  && ~isempty(img_name_grid{i,j-1})
            % display west
            fprintf(fh, 'west, %s, %s, %1.10f, %d, %d\n', img_name_grid{i,j}, img_name_grid{i,j-1}, CC2(i,j), X2(i,j), Y2(i,j));
        end
        if i > 1 && ~isempty(img_name_grid{i-1,j})
            % display north
            fprintf(fh, 'north, %s, %s, %1.10f, %d, %d\n', img_name_grid{i,j}, img_name_grid{i-1,j}, CC1(i,j), X1(i,j), Y1(i,j));
        end
      end
    end
end

fclose(fh);
