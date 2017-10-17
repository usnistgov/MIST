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
function [y, x, v] = pciam(I1, I2, direction, nb_FFT_peaks, fh)
if isempty(I1) || isempty(I2)
  y = NaN;
  x = NaN;
  v = NaN;
  return;
end


% Compute peak correlation matrix between I1 and I2 in the frequency domain. The max
% will be the actual vertical (y) and horizontal (x) translation between I1 and I2
start_time = tic;

% Perform phase correlation (amplitude is normalized)
if StitchingConstants.USE_GPU
  fc = fft2(gpuArray(I1)) .* conj(fft2(gpuArray(I2)));
else
  fc = fft2(I1) .* conj(fft2(I2));
end
fc(fc == 0) = eps('double');
fcn = fc ./(abs(fc));
pcm = real(ifft2(fcn)); % ignore the non real component of the inverse fft


% Get nb_FFT_peaks peaks and compute their respective normalized cross correlation values
% Sort values in descending order
[~,idx] = sort(pcm(:), 'descend');
% limit to nb_FFT_peaks
idx = idx(1:nb_FFT_peaks);
idx = gather(idx);

[r,c] = size(I1);

% Compute locations of each peak
[y, x] = ind2sub([r,c], idx);
% Take care of the fact that Matlab starts at 1
y = y - 1; 
x = x - 1;

PCC = zeros(nb_FFT_peaks,3);
for i = 1:nb_FFT_peaks
  [PCC(i,1), PCC(i,2), PCC(i,3)] = get_peak_cross_correlation(I1, I2, x(i), y(i), direction);
end

PCC = gather(PCC);
[~,ind] = max(PCC(:,3));
y = PCC(ind,1);
x = PCC(ind,2);
v = PCC(ind,3);

if fh > 0 % print debug info
  fprintf(fh, '(%.2fms)\n', toc(start_time)*1000);
  for i = 1:size(PCC,1)
    if i == ind
      fprintf(fh, '  peak %d: x: %f y: %f, ncc: %f  <--\n', i, PCC(i,2), PCC(i,1), PCC(i,3));
    else
      fprintf(fh, '  peak %d: x: %f y: %f, ncc: %f\n', i, PCC(i,2), PCC(i,1), PCC(i,3));
    end
  end
end

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




