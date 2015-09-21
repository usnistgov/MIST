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




function image_name = generate_image_name(source_name,t, p)
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
[matchstart,matchend,~,~,~,~,~] = regexpi(image_name, '{p+}');
if ~isempty(matchstart)
    if isnan(p)
        error('generate_image_name:argChk','Position number is NaN but position iterator found in source name.');
    end
    iterator_length = find_length_iterator_string(image_name, 'p');
    image_name = [image_name(1:matchstart-1) sprintf(['%0' num2str(iterator_length) 'd'], p) image_name(matchend+1:end)];
end

if ~strcmpi(image_name(end-3:end), '.tif')
    image_name = [image_name '.tif'];
end

if ~isempty(strfind(image_name, '{')) || ~isempty(strfind(image_name, '}'))
    error('generate_image_name:argChk','Invalid iterator found.');
end
end