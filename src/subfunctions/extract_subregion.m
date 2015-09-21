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



function [sub_I] = extract_subregion(I, x, y)
[h, w] = size(I);
if abs(x) >= w || abs(y) >= h
  sub_I = [];
  return;
end

x_st = 1 + x;
x_end = x_st + w - 1;
y_st = 1 + y;
y_end = y_st + h - 1;
% constrain to valid coords
x_st = max(1, min(x_st, w));
x_end = max(1, min(x_end, w));
y_st = max(1, min(y_st, h));
y_end = max(1, min(y_end, h));

sub_I = I(y_st:y_end, x_st:x_end);
end