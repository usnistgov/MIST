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



% The phase correlation image alignment method from image I1 to image I2
% (I1, I2) <==> (left, right) if left right pair I1 should be left of I2
% (I1, I2) <==> (up, down) if up down pair, I1 should be above I2
function [y, x, v] = pciam(I1, F1, I2, F2, direction, nb_FFT_peaks, min_Dist_Between_Peaks, fh)
if isempty(I1) || isempty(I2) || isempty(F1) || isempty(F2)
  y = NaN;
  x = NaN;
  v = NaN;
  return;
end

% Compute peak correlation matrix between I1 and I2 in the frequency domain. The max
% will be the actual vertical (y) and horizontal (x) translation between I1 and I2
start_time = tic;
pcm = peak_correlation_matrix(F1, F2);

% Get nb_FFT_peaks peaks and compute their respective normalized cross correlation values
peak_matrix = multi_peak_correlation_matrix(pcm, nb_FFT_peaks, min_Dist_Between_Peaks);

PCC = zeros(nb_FFT_peaks,3);
for i = 1:nb_FFT_peaks
  [PCC(i,1), PCC(i,2), PCC(i,3)] = get_peak_cross_correlation(I1, I2, peak_matrix(i,3), peak_matrix(i,2), direction);
end

[~,ind] = max(PCC(:,3));
y = PCC(ind,1);
x = PCC(ind,2);
v = PCC(ind,3);

if fh > 0 % print debug info
  fprintf(fh, '(%.2fms)\n', toc(start_time)*1000);
  for i = 1:size(peak_matrix,1)
    if i == ind
      fprintf(fh, '  peak %d: x: %f y: %f, ncc: %f  <--\n', i, PCC(i,2), PCC(i,1), PCC(i,3));
    else
      fprintf(fh, '  peak %d: x: %f y: %f, ncc: %f\n', i, PCC(i,2), PCC(i,1), PCC(i,3));
    end
  end
end

end


% Compute correlation matrix between I1 and I2 in the frequency domain
function pcm = peak_correlation_matrix(f1, f2)
% Perform phase correlation (amplitude is normalized)
fc = f1 .* conj(f2);
fc(fc == 0) = eps;
fcn = fc ./(abs(fc));

% Inverse fourier of peak correlation matrix
pcm = ifft2(fcn);
end


% Compute correlation matrix between I1 and I2 in the frequency domain
function peak_matrix = multi_peak_correlation_matrix(pcm, nb_FFT_peaks, min_Dist_Between_Peaks)

% precompute the squared distance to prevent needing a sqrt later
dist = min_Dist_Between_Peaks*min_Dist_Between_Peaks;

% Sort values in descending order
[r,c] = size(pcm);
[pcm,Ind] = sort(pcm(:), 'descend');

% Compute locations of each peak
[y, x] = ind2sub([r,c], Ind);

% Initialize first 5 peak matrix
peak_matrix = -Inf(nb_FFT_peaks,3);
n = 1;
m = 1;
max_ind = length(y);
while n <= nb_FFT_peaks && m <= max_ind
  % Compute distance between existing peaks and the current one
  D = (peak_matrix(:,2)-y(m)).^2 + (peak_matrix(:,3)-x(m)).^2;
  
  % if current peak is within the surrounding neighborhood of existing peaks, skip it
  indx = find(D<dist,1);
  if ~isempty(indx), m = m+1; continue, end
  
  % Otherwise memorise value and location
  peak_matrix(n,1) = pcm(m);
  peak_matrix(n,2) = y(m);
  peak_matrix(n,3) = x(m);
  n = n+1;
  m = m+1;
end

% Take care of the fact that Matlab starts at 1
peak_matrix(:,2:3) = peak_matrix(:,2:3) - 1;
end


function [y,x,v] = get_peak_cross_correlation(I1, I2, x, y, direction)
% Get image dimensions
[h, w] = size(I1);

% Compute the real translation between the images from the possible four combinations:
% 1) (x,y);     2) (w-x,y);       3) (x, h-y);      4) (w-x, h-y);

% Create row and column vector of the posible four combinations
m = [y, y, h-y, h-y];
n = [x, w-x, x, w-x];

% check all possible combinations of image locations
if direction == StitchingConstants.NORTH
  m = [m, m];
  n = [n, -n];
else
  m = [m, -m];
  n = [n, n];
end

% Initialize the value of peaks at each combination
peaks = zeros(numel(m),1);
% Compute the cross correlation index for each combination. The correct one will correspond to the most correlated value
for i = 1:numel(m)
  x = n(i);
  y = m(i);
  
  % the translations from I1 to I2 are the inverse of the translation from I2 to I1
  peaks(i) = cross_correlation_matrix(extract_subregion(I1, x, y), extract_subregion(I2, -x, -y));
end


% The real translation values correspond to the maximum correlation between overlaping regions
% [v, idx] = max(peaks); % tests for >= as opposed to required >
idx = 1;
for i = 2:numel(peaks)
  if peaks(i) > peaks(idx), idx = i; end
end
% assign the right output
v = peaks(idx);
y = m(idx);
x = n(idx);
end




