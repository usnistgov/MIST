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




function [y, x, max_peak] = cross_correlation_hill_climb(images_path, I1_name, I2_name, bounds, x, y)
% bounds = [y_min, y_max, x_min, x_max];

I1 = read_img(images_path, I1_name);
I2 = read_img(images_path, I2_name);
I1 = double(I1); I2 = double(I2);

max_peak = -1;
if isempty(I1) || isempty(I2)
  return;
end

if StitchingConstants.USE_EXHAUSTIVE_CORRELAION_SEARCH
  
  for i = bounds(1):bounds(2)
    for j = bounds(3):bounds(4)
      val = find_ncc(I1, I2, bounds, j, i);
      if val > max_peak
        x = j;
        y = i;
        max_peak = val;
      end
    end
  end
  
  
else
  if StitchingConstants.NUM_NCC_HILL_CLIMB_SEARCH_POINTS > 1
		% perform the hill climbing from the computed translation
    % the starting point of the hill climb search is (x,y)
    [x,y,max_peak] = perform_hill_climb(I1, I2, bounds, x, y);
		
		
		% perform NUM_NCC_HILL_CLIMB_SEARCH_POINTS hill climbings with random starting points
		for k = 1:StitchingConstants.NUM_NCC_HILL_CLIMB_SEARCH_POINTS
			% create a new random starting point within bounds
			x1 = (bounds(4) - bounds(3))*rand() + bounds(3);
			y1 = (bounds(2) - bounds(1))*rand() + bounds(1);
			
			[x1,y1,max_peak1] = perform_hill_climb(I1, I2, bounds, x1, y1);
			if max_peak1 > max_peak
				x = x1;
				y = y1;
				max_peak = max_peak1;
			end
		end
		
  else
    % the starting point of the hill climb search is (x,y)
    [x,y,max_peak] = perform_hill_climb(I1, I2, bounds, x, y);
  end

end

% to avoid propagating an inf value back as NCC
if isinf(max_peak), max_peak = -1; end
end


function [x,y,max_peak] = perform_hill_climb(I1, I2, bounds, x, y)

max_peak = -Inf;

% start the search at the middle point in bounds
done = false;
% init the matrix to hold computed ncc values to avoid recomputing
ncc_values = NaN(3,3);

% north, south, east, west
dx_vals = [0;0;1;-1];
dy_vals = [-1;1;0;0];

% comptue the current peak
ncc_values(2,2) = find_ncc(I1, I2, bounds, x, y);

while ~done
  
  % comptue the 4 connected peaks to the current locations
  for k = 1:numel(dx_vals)
    delta_x = dx_vals(k);
    delta_y = dy_vals(k);
    if isnan(ncc_values(2+delta_y,2+delta_x)) % compute the NCC value if not already computed
      ncc_values(2+delta_y,2+delta_x) = find_ncc(I1, I2, bounds, x+delta_x, y+delta_y);
    end
  end
  
  
  [local_max_peak,idx] = max(ncc_values(:));
  if isnan(local_max_peak)
    break;
  end
  [delta_y,delta_x] = ind2sub(size(ncc_values), idx);
  
  % make a translation instead of a location
  delta_y = delta_y - 2;
  delta_x = delta_x - 2;
  
  % adjust the translation value
  y = y + delta_y;
  x = x + delta_x;
  max_peak = local_max_peak;
  
  % update the elements in the ncc_values to reflect the new translation
  ncc_values = translate_mat_elements(ncc_values,delta_y,delta_x);
  % remove the 8 connected values
  ncc_values(1,1) = NaN;
  ncc_values(1,end) = NaN;
  ncc_values(end,1) = NaN;
  ncc_values(end,end) = NaN;
  
  if delta_y == 0 && delta_x == 0
    done = true;
  end
end

end



function peak = find_ncc(I1, I2, bounds, x, y)
peak = NaN;

% ensure the current location is valid
if y < bounds(1) || y > bounds(2)
  return;
end
if x < bounds(3) || x > bounds(4)
  return;
end

peak = cross_correlation_matrix(extract_subregion(I1, x, y), extract_subregion(I2, -x, -y));
end


function mat = translate_mat_elements(mat,di,dj)
if di == 0 && dj == 0
  return;
end

[m,n] = size(mat);

temp = NaN(m,n);
for j = 1:n
  for i = 1:m
    newi = i-di;
    newj = j-dj;
    if isfinite(mat(i,j)) && newi >= 1 && newi <= m && newj >= 1 && newj <= n
      temp(newi,newj) = mat(i,j);  
    end
  end
end
mat = temp;



end


