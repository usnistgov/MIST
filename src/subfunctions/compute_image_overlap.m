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

overlap = NaN;

if StitchingConstants.USE_MLE_TO_COMPUTE_OVERLAP
  
  % get the size of an image
  idx = 1;
  for i = 1:numel(img_name_grid)
    if ~isempty(img_name_grid{i})
      idx = i;
      break;
    end
  end
  info = imfinfo([source_directory img_name_grid{idx}]);
  
  
  % get the relevant translations
  if direction == StitchingConstants.NORTH
    T = Y(:);
    T(isnan(T)) = [];
    T = double(T);
    
    range = info.Height;
  else
    T = X(:);
    T(isnan(T)) = [];
    T = double(T);
    
    range = info.Width;
  end
  
  bestPoint = struct('m',0,'s',0,'p',0,'l',-inf);
  
  factor = double(range)/100;
  likelihoodValues = NaN(100,100,25);
  
  pVals = 1:100;
  mVals = 1:100;
  sVals = 1:25;
  
  pSkip = (round(numel(pVals)/StitchingConstants.MLE_GRID_SEARCH_SIZE_PER_SIDE));
  mSkip = (round(numel(mVals)/StitchingConstants.MLE_GRID_SEARCH_SIZE_PER_SIDE));
  sSkip = (round(numel(sVals)/StitchingConstants.MLE_GRID_SEARCH_SIZE_PER_SIDE));
  
  deltaP = floor(pSkip/2);
  deltaM = floor(mSkip/2);
  deltaS = floor(sSkip/2);
  
  for p = deltaP:pSkip:numel(pVals)
    for m = deltaM:mSkip:numel(mVals)
      for s = deltaS:sSkip:numel(sVals)
        pt = struct('m',mVals(m),'s',sVals(s),'p',pVals(p),'l',-inf);
        tmp = pt;
        
        done = false;
        while ~done
          % setup search bounds
          pmin = max(1,tmp.p-1);
          pmax = min(numel(pVals), tmp.p+1);
          mmin = max(1, tmp.m-1);
          mmax = min(numel(mVals), tmp.m+1);
          smin = max(1, tmp.s-1);
          smax = min(numel(sVals),tmp.s+1);
          
          % loop over the local neighborhood
          for hcP = pmin:pmax
            for hcM = mmin:mmax
              for hcS = smin:smax
                l = likelihoodValues(hcP,hcM,hcS);
                if isnan(l)
                  l = compute_likelihood(T, pVals(hcP), factor*mVals(hcM), factor*sVals(hcS), range);
                  likelihoodValues(hcP,hcM,hcS) = l;
                end
                
                if l > tmp.l
                  tmp.p = hcP;
                  tmp.m = hcM;
                  tmp.s = hcS;
                  tmp.l = l;
                end
                
              end
            end
          end
          
          % if the best local neighborhood point is better than the current point
          if tmp.l > pt.l
            pt = tmp;
          else
            % otherwise hill climbing is done
            done = true;
          end
        end
        % end hill climbing
        
        % keep track of the most likely point across all hill climbings
        if pt.l > bestPoint.l
          bestPoint = pt;
        end
      end
    end
  end
  
  % convert from percent of range to the actual translation values
  bestPoint.p = pVals(bestPoint.p);
  bestPoint.m = round(factor*mVals(bestPoint.m));
  bestPoint.s = round(factor*sVals(bestPoint.s));
  
  
  % perform refinement hill climbing using pixel level mu and sigma resolution
  tmp = bestPoint;
  done = false;
  while ~done
    pmin = max(1,tmp.p-1);
    pmax = min(100,tmp.p+1);
    mmin = max(1, tmp.m-1);
    mmax = min(range,tmp.m+1);
    smin = max(1, tmp.s-1);
    smax = min(range/4, tmp.s+1);
    
    % loop over the local neighborhood
    for hcP = pmin:pmax
      for hcM = mmin:mmax
        for hcS = smin:smax
          l = compute_likelihood(T, hcP, hcM, hcS, range);
          
          if l > tmp.l
            tmp.p = hcP;
            tmp.m = hcM;
            tmp.s = hcS;
            tmp.l = l;
          end
        end
      end
    end
    
    % if the best local neighborhood point is better
    if tmp.l > bestPoint.l
      bestPoint = tmp;
    else
      done = true;
    end
  end
  
  overlap = 100 * (1.0- (bestPoint.m/range));
  

else
  
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

end

assert(~isnan(overlap), 'Estimated Overlap could not be computed; stitch it using another method');


end



function likelihood = compute_likelihood(T, piUni, mu, sigma, range)
piUni = piUni/100;

T = double(T);

T = (T - mu)./sigma;
T = exp(-0.5.*T.*T);
T = T./(sqrt(2*pi)*sigma);
T = (piUni/range) + (1 - piUni).*T;
T = abs(T);
T = log(T);
likelihood = sum(T);

end
