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


function cr = cross_correlation_matrix(I1, I2)

% Transform inputs into one column vectors
I1 = I1(:);     I2 = I2(:);
nb_elem = size(I1,1);

% center vectors to the mean
mv = sum(I1)/nb_elem;
I1 = I1 - mv;
mv = sum(I2)/nb_elem;
I2 = I2 - mv;
% I1 = I1-mean(I1);       I2 = I2-mean(I2);

% Compute the Numerator and Denominator of the cross-correlation formula
N = I1'*I2;
D = sqrt(I1'*I1) * sqrt(I2'*I2);

% Compute the cross-correlation factor
cr = N/D;

if ~isfinite(cr)
  cr = -1;
end

end