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



function MIST(varargin)


% add the folder 'Sub_Functions' to the Matlab search path so it finds any
% functions placed in that subfolder
if ~isdeployed
  addpath([pwd filesep 'subfunctions']);
  addpath([pwd filesep 'doc']);
  addpath([pwd filesep 'imgs']);
end

% ----------------------------------------------------------------------
% Shared Variables (with default values)
% ----------------------------------------------------------------------
fp = fileparts(pwd);
source_directory = [fp filesep 'test' filesep]; % String containing the filepath of the source images
target_directory = [fp filesep 'test' filesep 'stitched' filesep]; % String containing the filepath to the target directory
to_stitch_time_slices = 'all'; % string controling which of the available time slices to stitch
nb_horizontal_tiles = 5; % number of image tiles horizontally in the stitched mosaic
nb_vertical_tiles = 5; % number of image tiles vertical in the stitched mosaic
source_name = 'ImageName_t0{t}_p{pp}.tif'; % the search string for source names Ex 'KB_ & c01', multiple channels are seperated by a comma
output_prefix = 'img-'; 


generate_stitched_image_flag = true; % flag controllling if to generate the stitched image rather than just the metadata
assemble_zero_percent_overlap_flag = false; % flag to create a zero percent overlap image

assemble_from_metadata_flag = false; % flag to attempt to generate the stitched image from metadata
alpha = 1.5; % controls the linear blending, higher alpha will blend the edges more, alpha of 0 turns the linear blending into average blending
blend_method_options = {'Overlay','Average','Linear','Max','Min'};
blend_method = blend_method_options{1};

filename_pattern_options = {'Sequential','Row-Column'};
starting_point_options = {'Upper left','Upper right','Lower right','Lower left'};
starting_point = starting_point_options{2};
first_direction_options = {'Vertical', 'Horizontal'};
first_direction = first_direction_options{1};
tiling_technique_options = {'Combing', 'Continuous'};
tiling_technique = tiling_technique_options{1};
img_name_grid = [];

% Advanced Tab
repeatability = NaN;
overlap_error = NaN;
estimated_overlap_x = NaN;
estimated_overlap_y = NaN;
start_location_x = 1;
start_location_y = 1;
extent_width = nb_horizontal_tiles;
extent_height = nb_vertical_tiles;

% if the GUI is already open, don't open another copy
open_fig_handle = findobj('type','figure','name','MIST');
if ~isempty(open_fig_handle)
  figure(open_fig_handle); % brings the figure to front
  return;
end

dark_gray = [0.7,0.7,0.7];
background_color = [0.86,0.86,0.86];
highlight_text_color = [0.0,0.3,0.4];
green_blue = [0.0,0.3,0.4];

% Cell String of Tab Labels
TabLabels = {'Aquisition Technique'; 'Set Parameters'};
% Number of tabs to be generated
NumberOfTabs = length(TabLabels);

% Get user screen size
SC = get(0, 'ScreenSize');
MaxMonitorX = SC(3);
MaxMonitorY = SC(4);

main_tabFigScale = 0.5;
gui_ratio = 0.6; % height/width
MaxWindowX = round(MaxMonitorX*main_tabFigScale);
MaxWindowY = MaxWindowX*gui_ratio;

% Set the figure window size values and calculate center of the screen position for the lower left corner
gui_width = round(MaxMonitorX/2); % Gui is half the screen wide
gui_height = round(gui_width*gui_ratio); % fix the ratio of the GUI height to width
% if the users screen resolution is not the same as their screen size the GUI will be placed off center
% of possibly out of view
offset = 0;
if (SC(2) ~= 1)
  offset = abs(SC(2));
end
XPos = (MaxMonitorX-MaxWindowX)/2 - offset;
YPos = (MaxMonitorY-MaxWindowY)/2 + offset;

% create main gui figure
hctfig = figure('Name','MIST',...
  'Position',[XPos, YPos, gui_width, gui_height],...
  'units', 'pixels', 'NumberTitle','off', 'Menubar','none', 'Toolbar','none', 'Resize', 'on');


% create the tabs
h_tabpanel = zeros(NumberOfTabs,1);

% create a UIPanel
h_tabpanel(1) = uipanel('Position', [0,0,1,1], 'Parent', hctfig,...
  'Units', 'normalized', 'Visible', 'off', ...
  'Backgroundcolor', background_color, 'BorderWidth',1);

% the NIST logo into the bottom right corner
try
  axes('Parent', h_tabpanel(1), 'Units', 'normalized', 'position', [0.8125 -0.035 0.16 0.2]);
  imshow(imread('NIST_Logo.png'), []);
catch err
  warning('NIST logo failed to load and display on the GUI');
  % logo failed to load
end

%---------------------------------------------------------------------------------------------
%  Aquisition Technique
%---------------------------------------------------------------------------------------------
% create NIST Stitching GUI text box
label(h_tabpanel(1), [.275 .9 .45 .07], 'MIST', 'center', green_blue, background_color, .8, 'serif', 'bold');


input_panel = sub_panel(h_tabpanel(1), [.05 .14 .9 .7], 'Input', 'lefttop', highlight_text_color, background_color, 14, 'serif');
output_panel = sub_panel(h_tabpanel(1), [.05 .14 .9 .7], 'Output', 'lefttop', highlight_text_color, background_color, 14, 'serif');
advanced_panel = sub_panel(h_tabpanel(1), [.05 .14 .9 .7], 'Advanced', 'lefttop', highlight_text_color, background_color, 14, 'serif');

input_button = push_button(h_tabpanel(1), [.15 .823 .1 .05], 'Input', 'center', 'k', dark_gray, .6, 'serif', 'bold', 'on', {@input_callback});
output_button = push_button(h_tabpanel(1), [.25 .823 .1 .05], 'Output', 'center', 'k', dark_gray, .6, 'serif', 'bold', 'on', {@output_callback});
advanced_button = push_button(h_tabpanel(1), [.35 .823 .1 .05], 'Advanced', 'center', 'k', dark_gray, .6, 'serif', 'bold', 'on', {@advanced_callback});

  function input_callback(varargin)
    set(input_button, 'background', dark_gray);
    set(output_button, 'background', background_color);
    set(advanced_button, 'background', background_color);
    set(input_panel, 'Visible', 'on');
    set(output_panel, 'Visible', 'off');
    set(advanced_panel, 'Visible', 'off');
  end

  function output_callback(varargin)
    set(output_button, 'background', dark_gray);
    set(input_button, 'background', background_color);
    set(advanced_button, 'background', background_color);
    set(output_panel, 'Visible', 'on');
    set(input_panel, 'Visible', 'off');
    set(advanced_panel, 'Visible', 'off');
  end

  function advanced_callback(varargin)
    set(advanced_button, 'background', dark_gray);
    set(input_button, 'background', background_color);
    set(output_button, 'background', background_color);
    set(advanced_panel, 'Visible', 'on');
    set(input_panel, 'Visible', 'off');
    set(output_panel, 'Visible', 'off');
  end


input_callback();

% ****************************************************************************
% Build the input panel
component_height = 0.07;
gap = 0.01;
y_val = 0.9;
label(input_panel, [.01 y_val .21 component_height], 'Source Directory', 'right', 'k', background_color, .6, 'sans serif', 'normal');
y_val = y_val - component_height - gap;
source_directory_edit = editbox(input_panel, [.01 y_val .8 component_height], source_directory, 'left', 'k', 'w', .6, 'normal');
push_button(input_panel, [.85 y_val .1 component_height], 'Open', 'center', 'k', 'default', .6, 'sans serif', 'bold', 'on', {@pb_choose_source_directory});

y_val = y_val - 4*gap;
y_val = y_val - component_height - gap;
% create common name of images text box
label(input_panel, [.01 y_val .21 component_height], 'File Name Pattern', 'right', 'k', background_color, .6, 'sans serif', 'normal');
source_name_edit = editbox(input_panel, [.23 y_val .5 component_height], source_name, 'left', 'k', 'w', .6, 'normal');
fn_pattern_dropdown = popmenu(input_panel, [.75 y_val .22 component_height], filename_pattern_options, 'k', 'w', .7, 'normal', @display_callback);
set(fn_pattern_dropdown, 'Value', 1);

y_val = y_val - component_height - gap;
% create Stitching Sequence text box
label(input_panel, [.01 y_val .21 component_height], 'Slices to Stitch', 'right', 'k', background_color, .6, 'sans serif', 'normal');
to_stitch_time_slices_edit = editbox(input_panel, [.23 y_val .5 component_height], to_stitch_time_slices, 'left', 'k', 'w', .6, 'normal');

y_val = y_val - 4*gap;
y_val = y_val - component_height - gap;
% create tile x text box
label(input_panel, [.01 y_val .25 component_height], 'Plate Size (W x H)', 'right', 'k', background_color, .65, 'sans serif', 'normal');
nb_horizontal_tiles_edit = editbox_check(input_panel, [.3 y_val .1 component_height], nb_horizontal_tiles, 'center', 'k', 'w', .6, 'normal',{@subgrid});
label(input_panel, [.4 y_val .05 component_height], 'x', 'center', 'k', background_color, .7, 'sans serif', 'normal');
nb_vertical_tiles_edit = editbox_check(input_panel, [.45 y_val .1 component_height], nb_vertical_tiles, 'center', 'k', 'w', .6, 'normal',{@subgrid});

  function subgrid(varargin)
    extent_width = str2double(get(nb_horizontal_tiles_edit, 'string'));
    extent_height = str2double(get(nb_vertical_tiles_edit, 'string'));
    
    set(extent_width_edit, 'string', num2str(extent_width));
    set(extent_height_edit, 'string', num2str(extent_height));
  end


% create starting point text box
y_val = y_val - 4*gap;
y_val = y_val - component_height - gap;
label(input_panel, [.01 y_val .25 component_height], 'Starting Point', 'right', 'k', background_color, .65, 'sans serif', 'normal');
starting_point_dropdown = popmenu(input_panel, [.3 y_val .25 component_height], starting_point_options, 'k', 'w', .7, 'normal', @display_callback);
set(starting_point_dropdown, 'Value', 1);

y_val = y_val - component_height - gap;
% create first direction text box
label(input_panel, [.01 y_val .25 component_height], 'First Direction', 'right', 'k', background_color, .65, 'sans serif', 'normal');
first_direction_dropdown = popmenu(input_panel, [.3 y_val .25 component_height], first_direction_options, 'k', 'w', .7, 'normal', @display_callback);
set(first_direction_dropdown, 'Value', 1);

y_val = y_val - component_height - gap;
% create tiling technique text box
label(input_panel, [.01 y_val .25 component_height], 'Tiling Technique', 'right', 'k', background_color, .65, 'sans serif', 'normal');
tiling_technique_dropdown = popmenu(input_panel, [.3 y_val .25 component_height], tiling_technique_options, 'k', 'w', .7, 'normal', @display_callback);
set(tiling_technique_dropdown, 'Value', 1);


% display tiling technique image demo
Haxes1 = axes('Parent', input_panel, 'Units', 'normalized', 'position', [0.6 0.1 0.45 0.45]);
display_callback();


% create set parameters pushbutton
uicontrol('style','push',...
  'Parent',input_panel,...
  'unit','normalized',...
  'position',[.1 .1 .25 component_height],...
  'string','Load CSV Image Grid',...
  'fontweight','bold',...
  'FontUnits', 'normalized',...
  'fontsize',.7,...
  'callback',@load_csv_image_grid_callback);

  function load_csv_image_grid_callback(varargin)
    
    [filename, pathname, ~] = uigetfile( ...
      {  '*.csv','CSV-files (*.csv)'; ...
      '*.xls','Excel 95-files (*.xls)'; ...
      '*.xlsx','Excel-files (*.xlsx)'; ...
      '*.*',  'All Files (*.*)'}, ...
      'Select Image Grid File(s)', ...
      'MultiSelect', 'on');
    if ~iscell(filename) && all(filename == 0)
      % the user clicked cancel
      return;
    end
    
    if ~iscell(filename)
      filename = {filename};
    end
    for i = 1:numel(filename)
      filename{i} = [pathname filename{i}];
    end
    % build the image name grid using the csv file
    img_name_grid = build_img_name_grid_from_csv(filename);
    
    % build the common name regex
    idx = ~cellfun(@isempty, img_name_grid);
    tmp = img_name_grid(idx);
    cn = double(tmp{1});
    for i = 1:numel(tmp)
      invalid = cn ~= double(tmp{i});
      cn(invalid) = NaN;
    end
    cn(isnan(cn)) = double('*');
    source_name = char(cn);
    
    % determine other information from the image name grid
    nb_vertical_tiles = size(img_name_grid,1);
    nb_horizontal_tiles = size(img_name_grid,2);
    start_location_y = 1;
    extent_height = nb_vertical_tiles;
    start_location_x = 1;
    extent_width = nb_horizontal_tiles;
    
    % disable the gui patameters that are determined by the csv file so that the user cannot overwrite them
    set(starting_point_dropdown,'enable','off');
    set(first_direction_dropdown,'enable','off');
    set(tiling_technique_dropdown,'enable','off');
    set(to_stitch_time_slices_edit,'enable','off');
    set(source_name_edit,'enable','off');
    
    % update the GUI with the new parameters that were determined from the csv file
    to_stitch_time_slices = 'all';
    % copy defaults back to the gui
    set(to_stitch_time_slices_edit,'string',to_stitch_time_slices);
    set(source_name_edit,'string',source_name);
    
    
    set(nb_horizontal_tiles_edit,'string',num2str(nb_horizontal_tiles));
    set(nb_vertical_tiles_edit,'string',num2str(nb_vertical_tiles));
    set(nb_horizontal_tiles_edit,'enable','off');
    set(nb_vertical_tiles_edit,'enable','off');
    
    set(start_location_x_edit,'string',num2str(start_location_x));
    set(start_location_y_edit,'string',num2str(start_location_y));
    set(extent_width_edit,'string',num2str(extent_width));
    set(extent_height_edit,'string',num2str(extent_height));
    
    
    % run validators
    if ~validate_inputs_panel()
      input_callback(); % display the inputs panel if validation failed
      return;
    end
    if ~validate_advanced_panel()
      advanced_callback(); % display the advanced panel if validation failed
      return;
    end
    
  end

uicontrol('style','push',...
  'Parent',input_panel,...
  'unit','normalized',...
  'position',[.4 .1 .25 component_height],...
  'string','Reset',...
  'fontweight','bold',...
  'FontUnits', 'normalized',...
  'fontsize',.7,...
  'callback',@reset_img_name_grid_callback);

  function reset_img_name_grid_callback(varargin)
    img_name_grid = [];
    
    % re-enable the parameters edit boxes that were invalidated by loading a csv grid
    set(starting_point_dropdown,'enable','on');
    set(first_direction_dropdown,'enable','on');
    set(tiling_technique_dropdown,'enable','on');
    set(to_stitch_time_slices_edit,'enable','on');
    set(source_name_edit,'enable','on');
    
    push_parameters_to_gui();
  end

  function push_parameters_to_gui()
    % copy defaults back to the gui
    set(source_directory_edit,'string',source_directory);
    set(target_directory_edit,'string',target_directory);
    set(to_stitch_time_slices_edit,'enable','on');
    set(to_stitch_time_slices_edit,'string',to_stitch_time_slices);
    set(nb_horizontal_tiles_edit,'enable','on');
    set(nb_vertical_tiles_edit,'enable','on');
    set(nb_horizontal_tiles_edit,'string',num2str(nb_horizontal_tiles));
    set(nb_vertical_tiles_edit,'string',num2str(nb_vertical_tiles));
    set(source_name_edit, 'enable','on');
    set(source_name_edit,'string',source_name);
    set(output_prefix_edit,'string',output_prefix);
    set(gen_stitched_image_flag_checkbox,'value',generate_stitched_image_flag);
    set(gen_zero_overlap_flag_checkbox,'value',assemble_zero_percent_overlap_flag);
    set(assemble_from_metadata_flag_checkbox,'value',assemble_from_metadata_flag);
    
    
    idx = 1;
    for i = 1:numel(starting_point_options)
      if strcmpi(starting_point, starting_point_options{i})
        idx = i;
      end
    end
    set(starting_point_dropdown,'value',idx);
    idx = 1;
    for i = 1:numel(first_direction_options)
      if strcmpi(first_direction, first_direction_options{i})
        idx = i;
      end
    end
    set(first_direction_dropdown,'value',idx);
    idx = 1;
    for i = 1:numel(tiling_technique_options)
      if strcmpi(tiling_technique, tiling_technique_options{i})
        idx = i;
      end
    end
    set(tiling_technique_dropdown,'value',idx);
    idx = 1;
    for i = 1:numel(blend_method_options)
      if strcmpi(blend_method, blend_method_options{i})
        idx = i;
      end
    end
    set(blending_method_drowdown,'value',idx);
    
    set(starting_point_dropdown,'enable','on');
    set(first_direction_dropdown,'enable','on');
    set(tiling_technique_dropdown,'enable','on');
    set(repeatability_edit,'string',num2str(repeatability));
    set(estimated_overlap_x_edit,'string','NaN');
    set(estimated_overlap_y_edit,'string','NaN');
    set(start_location_x_edit,'string',num2str(start_location_x));
    set(start_location_y_edit,'string',num2str(start_location_y));
    set(extent_width_edit,'string',num2str(extent_width));
    set(extent_height_edit,'string',num2str(extent_height));
  end

% ************************************************************************************************************************************************************************************************************************
% create stitch images pushbutton
uicontrol('style','push',...
  'Parent',h_tabpanel(1),...
  'unit','normalized',...
  'position',[.5 .005 .25 .05],...
  'string','Load Default Params',...
  'FontUnits', 'normalized',...
  'fontsize',.7,...
  'fontweight','bold',...
  'callback',@reset_defaults_callback);

  function reset_defaults_callback(varargin)
    fp = fileparts(pwd);
    source_directory = [fp filesep 'test' filesep]; % String containing the filepath of the source images
    target_directory = [fp filesep 'test' filesep 'stitched' filesep]; % String containing the filepath to the target directory
    to_stitch_time_slices = 'all'; % string controling which of the available time slices to stitch
    nb_horizontal_tiles = 5; % number of image tiles horizontally in the stitched mosaic
    nb_vertical_tiles = 5; % number of image tiles vertical in the stitched mosaic
    source_name = 'ImageName_t0{t}_p{pp}.tif';
    output_prefix = 'img-'; % the target names for the output stitched image and metadata, multiple channels are seperated by a comma
    
    generate_stitched_image_flag = true; % flag controllling if to generate the stitched image rather than just the metadata
    assemble_zero_percent_overlap_flag = false; % flag to create a zero percent overlap image
    
    assemble_from_metadata_flag = false; % flag to attempt to generate the stitched image from metadata
    alpha = 1.5; % controls the linear blending, higher alpha will blend the edges more, alpha of 0 turns the linear blending into average blending
    blend_method = blend_method_options{1};
    starting_point = starting_point_options{2};
    first_direction = first_direction_options{1};
    tiling_technique = tiling_technique_options{1};
    
    
    % Advanced Tab
    repeatability = NaN;
    estimated_overlap_x = NaN;
    estimated_overlap_y = NaN;
    start_location_x = 1;
    start_location_y = 1;
    extent_width = nb_horizontal_tiles;
    extent_height = nb_vertical_tiles;
    
    img_name_grid = [];
    push_parameters_to_gui();
    
  end


% **************************************************************
% Setup the output panel
component_height = 0.07;
gap = 0.01;
y_val = 0.9;
label(output_panel, [.01 y_val .21 component_height], 'Output Directory', 'right', 'k', background_color, .6, 'sans serif', 'normal');
target_directory_edit = editbox(output_panel, [.23 y_val .6 component_height], target_directory, 'left', 'k', 'w', .6, 'normal');
push_button(output_panel, [.85 y_val .1 component_height], 'Open', 'center', 'k', 'default', .6, 'sans serif', 'bold', 'on', {@pb_choose_target_directory});

y_val = y_val - component_height - gap;
% create common name of images text box
label(output_panel, [.01 y_val .21 component_height], 'Output Prefix', 'right', 'k', background_color, .6, 'sans serif', 'normal');
output_prefix_edit = editbox(output_panel, [.23 y_val .4 component_height], output_prefix, 'left', 'k', 'w', .6, 'normal');

y_val = y_val - 4*gap;
y_val = y_val - component_height - gap;
label(output_panel, [.01 y_val .21 component_height], 'Blending Method', 'right', 'k', background_color, .6, 'sans serif', 'normal');
blending_method_drowdown = popmenu(output_panel, [.23 y_val .25 component_height], blend_method_options, 'k', 'w', .6, 'normal', @blend_method_callback);
  function blend_method_callback(varargin)
    temp = get(blending_method_drowdown,'value');
    blend_method = blend_method_options{temp};
  end
set(blending_method_drowdown, 'Value', 1);


y_val = y_val - 4*gap;
y_val = y_val - component_height - gap;
gen_stitched_image_flag_checkbox = checkbox(output_panel, [.1 y_val .5 component_height], 'Create Stitched Image','left', 'k', background_color, .6, 'sans serif', 'normal', {@gen_stitched_image_checkbox_callback});
set(gen_stitched_image_flag_checkbox, 'value', generate_stitched_image_flag);

y_val = y_val - component_height;
gen_zero_overlap_flag_checkbox = checkbox(output_panel, [.13 y_val .47 component_height], 'Create 0% Overlap Mosaic','left', 'k', background_color, .6, 'sans serif', 'normal', {@gen_zero_overlap_checkbox_callback});
set(gen_zero_overlap_flag_checkbox, 'value', assemble_zero_percent_overlap_flag);



  function gen_stitched_image_checkbox_callback(varargin)
    generate_stitched_image_flag = get(gen_stitched_image_flag_checkbox, 'value');
    if generate_stitched_image_flag
      set(gen_zero_overlap_flag_checkbox, 'enable', 'on');
    else
      set(gen_zero_overlap_flag_checkbox, 'enable', 'off');
      set(gen_zero_overlap_flag_checkbox, 'value', 0);
    end
  end
  function gen_zero_overlap_checkbox_callback(varargin)
    assemble_zero_percent_overlap_flag = get(gen_zero_overlap_flag_checkbox, 'value');
  end

y_val = y_val - component_height;
assemble_from_metadata_flag_checkbox = checkbox(output_panel, [.1 y_val .47 component_height], 'Assemble from metadata','left', 'k', background_color, .6, 'sans serif', 'normal', {@assemble_from_metadata_checkbox_callback});
set(assemble_from_metadata_flag_checkbox, 'value', assemble_from_metadata_flag);
  function assemble_from_metadata_checkbox_callback(varargin)
    assemble_from_metadata_flag = get(assemble_from_metadata_flag_checkbox, 'value');
  end



% ***************************************************************
% Setup the advanced panel
component_height = 0.07;
gap = 0.01;
y_val = 0.9;

% Max Repeatability
label(advanced_panel, [0.01 y_val .3 component_height], 'Stage Repeatability', 'right', 'k', background_color, .6, 'sans serif', 'normal');
repeatability_edit = editbox(advanced_panel, [.32 y_val .15 component_height], repeatability, 'center', 'k', 'w', .6, 'normal');
label(advanced_panel, [0.5 y_val .1 component_height], 'pixels', 'left', 'k', background_color, .6, 'sans serif', 'normal');

% Percent Overlap Error
y_val = y_val - component_height - gap;
label(advanced_panel, [0.01 y_val .3 component_height], 'Image Overlap Uncertainty', 'right', 'k', background_color, .6, 'sans serif', 'normal');
overlap_error_edit = editbox(advanced_panel, [.32 y_val .15 component_height], overlap_error, 'center', 'k', 'w', .6, 'normal');
label(advanced_panel, [0.5 y_val .1 component_height], 'percent', 'left', 'k', background_color, .6, 'sans serif', 'normal');

% Estimated Overlap
y_val = y_val - component_height - gap;
label(advanced_panel, [.01 y_val .3 component_height], 'Estimated Overlap (Horz,Vert)', 'right', 'k', background_color, .6, 'sans serif', 'normal');

estimated_overlap_x_edit = editbox(advanced_panel, [.32 y_val .15 component_height], estimated_overlap_x, 'center', 'k', 'w', .6, 'normal');
label(advanced_panel, [.478 y_val-.01 .03 component_height], ',', 'center', 'k', background_color, .8, 'sans serif', 'normal');
estimated_overlap_y_edit = editbox(advanced_panel, [.52 y_val .15 component_height], estimated_overlap_y, 'center', 'k', 'w', .6, 'normal');



y_val = y_val - 8*gap;
y_val = y_val - component_height - gap;
% Subgrid
label(advanced_panel, [.01 y_val .3 component_height], 'Subgrid', 'right', 'k', background_color, .6, 'sans serif', 'bold');

% Start Location
y_val = y_val - component_height - gap;
label(advanced_panel, [.01 y_val .3 component_height], 'Start Location (X,Y)', 'right', 'k', background_color, .6, 'sans serif', 'normal');
start_location_x_edit = editbox(advanced_panel, [.32 y_val .15 component_height], start_location_x, 'center', 'k', 'w', .6, 'normal');
label(advanced_panel, [.478 y_val-.01 .03 component_height], ',', 'center', 'k', background_color, .8, 'sans serif', 'normal');
start_location_y_edit = editbox(advanced_panel, [.52 y_val .15 component_height], start_location_y, 'center', 'k', 'w', .6, 'normal');


% Extent Area
y_val = y_val - component_height - 2*gap;
label(advanced_panel, [.01 y_val .3 component_height], 'Extent Area (W x H)', 'right', 'k', background_color, .6, 'sans serif', 'normal');
extent_width_edit = editbox(advanced_panel, [.32 y_val .15 component_height], extent_width, 'center', 'k', 'w', .6, 'normal');
label(advanced_panel, [.478 y_val .03 component_height], 'x', 'center', 'k', background_color, .8, 'sans serif', 'normal');
extent_height_edit = editbox(advanced_panel, [.52 y_val .15 component_height], extent_height, 'center', 'k', 'w', .6, 'normal');




%  create save pushbutton
push_button(h_tabpanel(1), [.8475 .905 .125 .05], 'Help', 'center', 'k', dark_gray, .6, 'serif', 'bold', 'on', {@Open_Help_PDF_callback});


% create stitch images pushbutton
uicontrol('style','push',...
  'Parent',h_tabpanel(1),...
  'unit','normalized',...
  'position',[.27 .005 .2 .05],...
  'string','Stitch Images',...
  'FontUnits', 'normalized',...
  'fontsize',.7,...
  'fontweight','bold',...
  'callback',{@stitch_images});

%-----------------------------------------------------------------------------------------
% / END Aquisition technique
%-----------------------------------------------------------------------------------------
%---------------------------------------------------------------------------------------------
% /End Set Parameters
%---------------------------------------------------------------------------------------------

%---------------------------------------------------------------------------------------------
% Validators
%---------------------------------------------------------------------------------------------

  function bool = validate_inputs_panel()
    bool = false;
    
    source_name = get(source_name_edit,'String');
    source_directory = get(source_directory_edit, 'string');
    [source_directory, dir_flag] = validate_directory(source_directory, 0);
    if ~dir_flag
      errordlg('Invalid Source Directory');
      return;
    end
    if ~strcmpi(source_directory(end), filesep)
      source_directory = [source_directory filesep];
    end
    
    
    % validate the time slices to stitch
    to_stitch_time_slices = get(to_stitch_time_slices_edit,'String');
    
    nb_horizontal_tiles = str2double(get(nb_horizontal_tiles_edit,'String'));
    if isnan(nb_horizontal_tiles)
      errordlg('Invalid number of horizontal tiles');
      return;
    end
    nb_vertical_tiles = str2double(get(nb_vertical_tiles_edit,'String'));
    if isnan(nb_vertical_tiles)
      errordlg('Invalid number of vertical tiles');
      return;
    end
    
    
    temp = get(tiling_technique_dropdown,'Value');
    tiling_technique = lower(strrep(tiling_technique_options{temp},' ',''));
    temp = get(starting_point_dropdown,'Value');
    starting_point = lower(strrep(starting_point_options{temp},' ',''));
    temp = get(first_direction_dropdown,'Value');
    first_direction = lower(strrep(first_direction_options{temp},' ',''));
    
    bool = true;
  end

  function bool = validate_outputs_panel()
    bool = false;
    
    output_prefix = get(output_prefix_edit,'String');
    target_directory = get(target_directory_edit,'String');
    
    % validate the target directory
    if isempty(target_directory)
      target_directory = [source_directory 'stitched' filesep];
    end
    [target_directory, dir_flag] = validate_directory(target_directory, 1);
    if ~dir_flag
      errordlg('Invalid Target Directory');
      return;
    end
    if ~strcmpi(target_directory(end), filesep)
      target_directory = [target_directory filesep];
    end
    
    % validate the target name string
    if isempty(output_prefix)
      output_prefix = 'img-';
    end
    
    % update the gui fields
    set(output_prefix_edit, 'string', output_prefix);
    set(target_directory_edit,'string', target_directory);
    
    temp = get(blending_method_drowdown,'value');
    blend_method = blend_method_options{temp};
    
    
    generate_stitched_image_flag = get(gen_stitched_image_flag_checkbox, 'value');
    assemble_zero_percent_overlap_flag = get(gen_zero_overlap_flag_checkbox, 'value');
    
    if ~generate_stitched_image_flag
      assemble_zero_percent_overlap_flag = false;
    end
    
    bool = true;
  end

  function bool = validate_advanced_panel()
    bool = false;
    
    repeatability = str2double(get(repeatability_edit, 'string'));
    set(repeatability_edit, 'string', num2str(repeatability));
    overlap_error = str2double(get(overlap_error_edit, 'string'));
    set(overlap_error_edit, 'string', num2str(overlap_error));
    estimated_overlap_x = str2double(get(estimated_overlap_x_edit, 'string'));
    set(estimated_overlap_x_edit, 'string', num2str(estimated_overlap_x));
    estimated_overlap_y = str2double(get(estimated_overlap_y_edit, 'string'));
    set(estimated_overlap_y_edit, 'string', num2str(estimated_overlap_y));
    
    start_location_x = str2double(get(start_location_x_edit,'string'));
    start_location_y = str2double(get(start_location_y_edit,'string'));
    extent_width = str2double(get(extent_width_edit,'string'));
    extent_height = str2double(get(extent_height_edit,'string'));
    
    if isnan(start_location_x) || start_location_x < 1 || isnan(start_location_y) || start_location_y < 1
      errordlg('Invalid Subgrid Dimensions');
      return;
    end
    
    if isnan(extent_width) || extent_width < 1 || isnan(extent_height) || extent_height < 1
      errordlg('Invalid Subgrid Dimensions');
      return;
    end
    
    % check to make sure subgrid bounds are within the plate bounds
    if start_location_y > nb_vertical_tiles
      errordlg('Start location Y is too large.');
      return;
    end
    
    if  start_location_y + extent_height - 1 > nb_vertical_tiles
      errordlg('Extent area height is too large.');
      return;
    end
    
    if  start_location_x > nb_horizontal_tiles
      errordlg('Start location X is too large.');
      return;
    end
    
    if start_location_x + extent_width - 1 > nb_horizontal_tiles
      errordlg('Extent area width is too large.');
      return;
    end
    
    bool = true;
  end

  function bool = validate_parameters()
    bool = false;
    if ~validate_inputs_panel()
      input_callback(); % display the invalid panel in the GUI
      return;
    end
    if ~validate_outputs_panel()
      output_callback(); % display the invalid panel in the GUI
      return;
    end
    if ~validate_advanced_panel()
      advanced_callback(); % display the invalid panel in the GUI
      return;
    end
    
    if strcmpi(blend_method, 'linear')
      % if the fusion method is linear query the user for the alpha value
      alpha = get_alpha_value_from_user();
    end
    if assemble_from_metadata_flag
      choice = questdlg('Aquisition Parameters will be overwritten by any loaded metadata! Directory and File name patterns will persist.','Load Params','Accept','Cancel','Accept');
      if ~strcmpi(choice, 'accept')
        return;
      end
    end
    
    bool = true;
  end
%---------------------------------------------------------------------------------------------
% /Validators
%---------------------------------------------------------------------------------------------



%---------------------------------------------------------------------------------------------
% Stitch Images
%---------------------------------------------------------------------------------------------
  function stitch_images(varargin)
    
    if ~validate_parameters()
      return;
    end
    
    if isdeployed
      % if this is being run from a compiled .exe, this will display a working message box to let the user know that
      % something is being done
      h = msgbox('Working...');
    end
    
    % img_name_grid will be non empty if the user has loaded a csv grid of images
    if isempty(img_name_grid)
      % build the appropriate image name grid based on the users parameters
      [img_name_grid, to_stitch_time_slice_nbs] = build_img_name_grid(source_directory, source_name, nb_horizontal_tiles, nb_vertical_tiles,...
        'time_slices_to_stitch',to_stitch_time_slices,...
        'tiling_technique',tiling_technique,...
        'starting_point',starting_point,...
        'first_direction',first_direction);
    else
      to_stitch_time_slice_nbs = 1:size(img_name_grid,3);
    end
    img_name_grid = img_name_grid(start_location_y:(start_location_y + extent_height - 1),start_location_x:(start_location_x + extent_width - 1),:);
    
    try
      
      for t = 1:size(img_name_grid,3)
        time_slice = to_stitch_time_slice_nbs(t);
        
        log_file_path = [target_directory output_prefix 'debug.log'];
        temp_img_name_grid = img_name_grid(:,:,t);
        if assemble_zero_percent_overlap_flag
          create_zero_percent_overlap_mosaic(source_directory, temp_img_name_grid, target_directory, output_prefix, blend_method, alpha, time_slice);
        else
          stitch_time_slice(source_directory, temp_img_name_grid, target_directory, output_prefix, time_slice, repeatability, ...
            overlap_error, blend_method, alpha, generate_stitched_image_flag, assemble_from_metadata_flag, log_file_path, estimated_overlap_x,...
            estimated_overlap_y);
        end

      end
      print_to_command('Done stitching');
      
      reset_img_name_grid_callback();
      if isdeployed
        if ishandle(h), close(h); end
        msgbox('Done Stitching');
      end
      
    catch err
      % perform some cleanup in case of error
      reset_img_name_grid_callback();
      if isdeployed
        if ishandle(h), close(h); end
        msgbox('Done Stitching');
      end
      rethrow(err);
    end
    
    
  end


%--------------------------------------------------------------------------------------------------
% function to validate a directory
%--------------------------------------------------------------------------------------------------
  function [cur_dir, bool] = validate_directory(cur_dir, make_dir_flag)
    try % validate the target directory
      cur_dir = validate_filepath(cur_dir);
      bool = true;
    catch err
      switch err.identifier
        case 'validate_filepath:notFoundInPath'
          if make_dir_flag
            mkdir(cur_dir);
            bool = true;
          else
            bool = false;
          end
        otherwise
          rethrow(err);
      end
    end
  end

  function [cur_path] = validate_filepath(cur_path)
    % check the number of inputs
    if nargin ~= 1, return, end
    % check that the cur_path variable is a char string
    if ~isa(cur_path, 'char')
      error('validate_filepath:argChk','invalid input type');
    end
    
    % get the file attributes
    [status,message] = fileattrib(cur_path);
    % if status is 0 then the file path was invalid
    if status == 0
      error('validate_filepath:notFoundInPath', 'No such file or directory: \"%s\"',cur_path);
    else
      % cur_path held a valid file path to either a directory or a file
      cur_path = message.Name;
      % determine if cur_path is a file or a folder
      if message.directory == 0
        % the path represents a file
        % do nothing
      else
        % the path represents a directory
        if cur_path(end) ~= filesep
          cur_path = [cur_path filesep];
        end
      end
    end
    
  end
%--------------------------------------------------------------------------------------------------
% function controlling the display of the example image on the first tab
%--------------------------------------------------------------------------------------------------
% function that gets user input of collection parameters and generates and example image.
  function display_callback(varargin)
    tt = get(tiling_technique_dropdown,'Value');
    sp = get(starting_point_dropdown,'Value');
    fd = get(first_direction_dropdown,'Value');
    display = [num2str(tt) ',' '1,' num2str(sp) ',' num2str(fd) ];
    
    fp = get(fn_pattern_dropdown, 'value');
    if fp == 1
      set(tiling_technique_dropdown, 'enable','on');
      set(first_direction_dropdown, 'enable','on');
      if strcmpi(get(source_name_edit, 'string'), 'ImageName_t0{t}_r{rr}_c{cc}.tif') || strcmpi(get(source_name_edit, 'string'), 'ImageName_t0{t}_p{pp}.tif')
        set(source_name_edit, 'string', 'ImageName_t0{t}_p{pp}.tif');
      end
    else
      set(tiling_technique_dropdown,'Value', 1);
      set(first_direction_dropdown,'Value', 1);
      set(tiling_technique_dropdown, 'enable','off');
      set(first_direction_dropdown, 'enable','off');
      if strcmpi(get(source_name_edit, 'string'), 'ImageName_t0{t}_r{rr}_c{cc}.tif') || strcmpi(get(source_name_edit, 'string'), 'ImageName_t0{t}_p{pp}.tif')
        set(source_name_edit, 'string', 'ImageName_t0{t}_r{rr}_c{cc}.tif');
      end
    end
    
    I1 = imread([display '.png']);
    delete(get(Haxes1, 'Children'))
    imshow(I1, [], 'Parent', Haxes1);
  end

%--------------------------------------------------------------------------------------------------
% Open/Select source directory
%--------------------------------------------------------------------------------------------------
  function pb_choose_source_directory(varargin)
    source_directory = num2str(uigetdir(pwd,'Select Directory with images to stitch together'));
    % if the source directory is invalid, reset it and return
    if source_directory == '0'
      source_directory = [];
      return;
    end
    
    [source_directory, bool] = validate_directory(source_directory, 0);
    if ~bool
      errordlg('Invalid Source Directory');
      return;
    end
    
    target_directory = [source_directory 'stitched' filesep];
    set(target_directory_edit,'String', target_directory);
    
    % display the selected directory
    set(source_directory_edit,'String', source_directory);
  end
%--------------------------------------------------------------------------------------------------
% Open/Select target directory
%--------------------------------------------------------------------------------------------------
  function pb_choose_target_directory(varargin)
    target_directory = num2str(uigetdir(pwd,'Select Directory to save images'));
    % if the target directory is not valid, reset it and return
    if target_directory == '0'
      target_directory = [];
      return;
    end
    
    [target_directory, bool] = validate_directory(target_directory, 1);
    if ~bool
      errordlg('Invalid Target Directory');
      return;
    end
    % display the selected directory
    set(target_directory_edit,'String', target_directory);
  end

  function val = get_alpha_value_from_user()
    % if the fusion method is linear query the user for the alpha value
    found_valid_alpha = false;
    while ~found_valid_alpha
      prompt = {'Enter linear blending method alpha value (>0):'};
      dlg_title = 'Enter Alpha Value';
      num_lines = 1;
      def = {num2str(val)};
      answer = inputdlg(prompt,dlg_title,num_lines,def);
      temp = str2double(answer);
      if isempty(temp)
        return;
      end
      if ~isnan(temp) && temp >= 0
        found_valid_alpha = true;
        val = temp;
      end
    end
  end


  


% ---------------------------------------------------------------------
% Help Dialog callbacks
% ---------------------------------------------------------------------
  function Open_Help_PDF_callback(varargin)
    winopen('User-Guide.html');
  end


set(h_tabpanel(1), 'Visible', 'on');

end

% UI Control Wrappers
function edit_return = editbox(parent_handle, position, string, horz_align, color, bgcolor, fontsize, fontweight, varargin)
edit_return = uicontrol('style','edit',...
  'parent',parent_handle,...
  'unit','normalized',...
  'fontunits', 'normalized',...
  'position',position,...
  'horizontalalignment',horz_align,...
  'string',string,...
  'foregroundcolor',color,...
  'backgroundcolor',bgcolor,...
  'fontsize',fontsize,...
  'fontweight',fontweight);
end

function edit_return = editbox_check(parent_handle, position, string, horz_align, color, bgcolor, fontsize, fontweight, callback, varargin)
edit_return = uicontrol('style','edit',...
  'parent',parent_handle,...
  'unit','normalized',...
  'fontunits', 'normalized',...
  'position',position,...
  'horizontalalignment',horz_align,...
  'string',string,...
  'foregroundcolor',color,...
  'backgroundcolor',bgcolor,...
  'fontsize',fontsize,...
  'fontweight',fontweight,...
  'callback', callback);
end

function label_return = label(parent_handle, position, string, horz_align, color, bgcolor, fontsize, fontname, fontweight, varargin)
label_return = uicontrol('style','text',...
  'parent',parent_handle,...
  'unit','normalized',...
  'fontunits','normalized',...
  'position',position,...
  'horizontalalignment',horz_align,...
  'string',string,...
  'foregroundcolor',color,...
  'backgroundcolor',bgcolor,...
  'fontsize',fontsize,...
  'fontname', fontname,...
  'fontweight',fontweight);
end


function button_return = push_button(parent_handle, position, string, horz_align, color, bgcolor, fontsize, fontname, fontweight, on_off, callback, varargin)
button_return = uicontrol('style','pushbutton',...
  'parent',parent_handle,...
  'unit','normalized',...
  'fontunits','normalized',...
  'position',position,...
  'horizontalalignment',horz_align,...
  'foregroundcolor',color,...
  'backgroundcolor',bgcolor,...
  'string',string,...
  'fontsize',fontsize,...
  'fontname', fontname,...
  'fontweight',fontweight,...
  'enable', on_off,...
  'callback',callback);
end


function check_return = checkbox(parent_handle, position, string, horz_align, color, bgcolor, fontsize, fontname, fontweight, callback, varargin)
check_return = uicontrol('style','checkbox',...
  'Parent',parent_handle,...
  'unit','normalized',...
  'fontunits', 'normalized',...
  'position',position,...
  'horizontalalignment',horz_align,...
  'string',string,...
  'foregroundcolor',color,...
  'backgroundcolor',bgcolor,...
  'fontsize', fontsize,...
  'fontname', fontname,...
  'fontweight',fontweight,...
  'callback', callback);
end

function pop_return = popmenu(parent_handle, position, options, color, bgcolor, fontsize, fontweight, callback, varargin)
pop_return = uicontrol('style','popupmenu',...
  'Parent',parent_handle,...
  'String',options,...
  'unit','normalized',...
  'fontunits', 'normalized',...
  'position',position,...
  'foregroundcolor',color,...
  'backgroundcolor',bgcolor,...
  'fontsize', fontsize,...
  'fontweight',fontweight,...
  'callback',callback);
end



% UI Panels
function panel_return = sub_panel(parent_handle, position, title, title_align, color, bgcolor, fontsize, fontname, varargin)
panel_return = uipanel('parent', parent_handle,...
  'units', 'normalized',...
  'position',position,...
  'title',title,...
  'titleposition',title_align,...
  'foregroundcolor',color,...
  'backgroundcolor',bgcolor,...
  'fontname', fontname,...
  'fontsize',fontsize,...
  'fontweight', 'bold',...
  'visible', 'on',...
  'borderwidth',1);
end