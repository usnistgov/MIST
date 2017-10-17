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




function overlap = compute_image_overlap(X, Y, source_directory, img_name_grid, direction)

% get the size of an image
idx = 1;
for i = 1:numel(img_name_grid)
  if ~isempty(img_name_grid{i}) && exist([source_directory img_name_grid{i}],'file')
    idx = i;
    break;
  end
end
info = imfinfo([source_directory img_name_grid{idx}]);


% get the relevant translations
if direction == StitchingConstants.NORTH
  range = info.Height;
  T = Y(:);
else
  range = info.Width;
  T = X(:);
end

T = double(T);
T(isnan(T)) = [];
min_translation_value = 1;
max_translation_value = range-1;
% remove translations that are out of range, or consist of a single pixel
T(T <= min_translation_value) = [];
T(T >= max_translation_value) = [];

% convert translations into [0,100]
T = 100*T/range;

bestPoint = struct('p',0,'m',0,'s',0,'l',-inf);
likelihoodValues = NaN(100,100,100);

hc_results = {};

stableCount = 0;
% while stableCount < StitchingConstants.MLE_OPTIMIZATION_STALL_COUNT
while stableCount < 100
	% generate a new random starting point
  p = round(100*rand());
  m = round(100*rand());
  s = round(100*rand());
  
	% convert it to a MlePoint struct
  point = struct('p',p,'m',m,'s',s,'l',-inf);
	% perform hill climbing to optimize the starting point
  [point,likelihoodValues] = perform_mle_hill_climb(T,point,likelihoodValues);
  hc_results = vertcat(hc_results, point);
  
	% if the new starting point is better than the previous best
  if point.l > bestPoint.l
    bestPoint = point;
    stableCount = 0;
  else
		% this hill climb failed to find a better solution, increment the stall count
    stableCount = stableCount + 1;
  end
end

% determine how many converged
numConverged = 0;
for i = 1:numel(hc_results)
  if hc_results{i}.m == bestPoint.m && hc_results{i}.s == bestPoint.s
    numConverged = numConverged + 1;
  end
end

% convert the model mu to an overlap estimate
overlap = 100 - bestPoint.m;

end

function [pt,likelihoodValues] = perform_mle_hill_climb(T,pt,likelihoodValues)

done = false;
while ~done
	tmp = pt;
  
	% setup the search neighborhood (deltas from the current point)
	% [PIuniform, mu, sigma]
  search_bounds = [1, 0, 0;
    -1, 0, 0;
    0, 1, 0;
    0, -1, 0;
    0, 0, 1;
    0, 0, -1];
  
  % loop over the local search neighborhood
  for k = 1:size(search_bounds,1)
    hcP = pt.p + search_bounds(k,1);
    hcM = pt.m + search_bounds(k,2);
    hcS = pt.s + search_bounds(k,3);
    
    % check whether this is within bounds
    if hcP > 0 && hcP < 100 && hcM > 0 && hcM < 100 && hcS > 0 && hcS < 100
      l = likelihoodValues(hcP,hcM,hcS);
      if isnan(l)
        l = compute_likelihood(T, hcP, hcM, hcS);
        likelihoodValues(hcP,hcM,hcS) = l;
      end
      
      if isnan(tmp.l) || l > tmp.l
        tmp.p = hcP;
        tmp.m = hcM;
        tmp.s = hcS;
        tmp.l = l;
      end
    end
  end
  
  % if the best local neighborhood point is better than the current point
  if isnan(pt.l) || tmp.l > pt.l
    pt = tmp;
  else
    % otherwise hill climbing is done
    done = true;
  end
end
% end hill climbing

end



function likelihood = compute_likelihood(T, piUni, mu, sigma)
piUni = piUni/100;

T = double(T);
range = 100;

T = (T - mu)./sigma;
T = exp(-0.5.*T.*T);
T = T./(sqrt(2*pi)*sigma);
T = (piUni/range) + (1 - piUni).*T;
T = abs(T);
T = log(T);
likelihood = sum(T);

end
