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




function nb_fnd = find_length_iterator_string(src_str, search_char)
regex_search_str = ['{' search_char '+}'];
match_str = regexpi(src_str, regex_search_str, 'match');
if numel(match_str) > 1
	error('Invalid Source Name: multiple iterator indicies');
end
if ~isempty(match_str)
    nb_fnd = length(match_str{1}) - 2;
else
    nb_fnd = 0;
end
end