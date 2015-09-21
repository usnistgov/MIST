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




function w_mat = compute_linear_blend_pixel_weights(size_I, alpha)
d_min_mat_i = zeros(size_I(1), 1);
d_min_mat_j = zeros(1, size_I(1));
for i = 1:size_I(1)
    d_min_mat_i(i,1) = min(i, size_I(1) - i + 1);
end
for j = 1:size_I(2)
    d_min_mat_j(1,j) = min(j, size_I(2) - j + 1);
end

w_mat = d_min_mat_i*d_min_mat_j;
w_mat = w_mat.^alpha;

end