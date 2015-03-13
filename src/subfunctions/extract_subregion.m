
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