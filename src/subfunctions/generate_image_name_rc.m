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



function image_name = generate_image_name_rc(source_name,t, r, c)
image_name = source_name;

% timeslice
[matchstart,matchend,~,~,~,~,~] = regexpi(image_name, '{t+}');
if ~isempty(matchstart)
    if isnan(t)
        error('generate_image_name:argChk','Time slice number is NaN but time slice iterator found in source name.');
    end
    iterator_length = find_length_iterator_string(image_name, 't');
    image_name = [image_name(1:matchstart-1) sprintf(['%0' num2str(iterator_length) 'd'], t) image_name(matchend+1:end)];
end

% position
[matchstart,matchend,~,~,~,~,~] = regexpi(image_name, '{r+}');
if ~isempty(matchstart)
    if isnan(r)
        error('generate_image_name:argChk','Row number is NaN but position iterator found in source name.');
    end
    iterator_length = find_length_iterator_string(image_name, 'r');
    image_name = [image_name(1:matchstart-1) sprintf(['%0' num2str(iterator_length) 'd'], r) image_name(matchend+1:end)];
end
[matchstart,matchend,~,~,~,~,~] = regexpi(image_name, '{c+}');
if ~isempty(matchstart)
    if isnan(c)
        error('generate_image_name:argChk','Column number is NaN but position iterator found in source name.');
    end
    iterator_length = find_length_iterator_string(image_name, 'c');
    image_name = [image_name(1:matchstart-1) sprintf(['%0' num2str(iterator_length) 'd'], c) image_name(matchend+1:end)];
end

if ~strcmpi(image_name(end-3:end), '.tif')
    image_name = [image_name '.tif'];
end

if ~isempty(strfind(image_name, '{')) || ~isempty(strfind(image_name, '}'))
    error('generate_image_name:argChk','Invalid iterator found.');
end
end