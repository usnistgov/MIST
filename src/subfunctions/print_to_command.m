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


function log_file_path = print_to_command(value, tgt_dir)
 
switch nargin
    case 1
        tgt_dir = '';
        use_log_file_flag = false;
    case 2
        if ~ischar(tgt_dir)
            use_log_file_flag = false;
        else
        	use_log_file_flag = true;
        end
        % this is the correct number of arguments
    otherwise
        return;
end
 
if ~isa(value,'numeric') && ~isa(value,'char')
    error('print_to_command:argChk','Able to print numeric and char types');
end

newline_char = '\n';
 
date_vector = round(clock());
time_vector = date_vector(4:end);
if use_log_file_flag
    [pathstr,name,ext] = fileparts(tgt_dir);
    if ~isempty(ext)
        log_file_path = tgt_dir;
    else
        log_file_path = [tgt_dir filesep getenv('username') '_log' sprintf('_%04d%02d%02d',date_vector(1),date_vector(2),date_vector(3)) '.log'];
    end
    log_fileID = fopen(log_file_path,'a');
end
output_padding = '          ';
 
output_string = sprintf('<%02d:%02d:%02d>',time_vector(1),time_vector(2),time_vector(3));

if isa(value, 'char') && isempty(value)
    % simply print a clear clean line
    fprintf(1, newline_char);
    if use_log_file_flag
        fprintf(log_fileID, newline_char);
    end
else
    if ~isempty(value)
        if ~isa(value, 'char')
            value = num2str(value);
        end
        [m,~] = size(value);
        fprintf(1, [output_string ' %s' newline_char], value(1,:));
        if use_log_file_flag
            fprintf(log_fileID, [output_string ' %s' newline_char], value(1,:));
        end
        for i = 2:m
            fprintf(1, [output_padding ' %s' newline_char], value(i,:));
            if use_log_file_flag
                fprintf(log_fileID, [output_padding ' %s' newline_char], value(i,:));
            end
        end
    end
end
 
if use_log_file_flag
    fclose(log_fileID);
end
 
end



function [cur_path] = validate_filepath(cur_path)
if nargin ~= 1, return, end
if ~isa(cur_path, 'char')
    error('validate_filepath:argChk','invalid input type');
end

[status,message] = fileattrib(cur_path);
if status == 0
    error('validate_filepath:notFoundInPath',...
            'No such file or directory: \"%s\"',cur_path);
else
    cur_path = message.Name;
    if message.directory == 0
        % the path represents a file so this is valid
        % So do nothing
    else
        % the path represents a directory
        if cur_path(end) ~= filesep
            cur_path = [cur_path filesep];
        end
    end
end

end