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



classdef (Sealed) StitchingStatistics < handle
  methods (Access = private)
    function obj = StitchingStatistics
    end
  end
  methods (Static)
    function singleObj = getInstance
      persistent localObj
      if isempty(localObj) || ~isvalid(localObj)
        localObj = StitchingStatistics;
      end
      singleObj = localObj;
    end
  end
  methods
    function obj = print_stats_to(obj, stats_file)
      fh = fopen(stats_file,'w');
      fprintf(fh, 'Execution timing (ms) and general information:\n');
      fprintf(fh, '\n');
      fprintf(fh, 'Total time for experiment: %d\n', round(1000*obj.total_experiment_time));
      fprintf(fh, 'Total Stitching Time: %d\n', round(1000*obj.total_stitching_time));
      fprintf(fh, 'Relative Displacement Time: %d\n', round(1000*obj.relative_displacement_time));
      fprintf(fh, 'Global Optimization Time: %d\n', round(1000*obj.global_optimization_time));
      
      fprintf(fh, '\n');
      fprintf(fh, 'North repeatability: %d\n', obj.north_repeatability);
      fprintf(fh, 'North overlap: %g\n', obj.north_overlap);
      fprintf(fh, 'North valid tiles after filter: %d out of %d\n', obj.north_nb_valid_tiles, obj.north_nb_tiles);
      fprintf(fh, 'North min filter threshold: %g\n', obj.north_min_range_filter);
      fprintf(fh, 'North max filter threshold: %g\n', obj.north_max_range_filter);
      fprintf(fh, 'North missing row/col: [%s]\n', obj.north_missing_row_col);
      
      fprintf(fh, '\n');
      fprintf(fh, 'West repeatability: %d\n', obj.west_repeatability);
      fprintf(fh, 'West overlap: %g\n', obj.west_overlap);
      fprintf(fh, 'West valid tiles after filter: %d out of %d\n', obj.west_nb_valid_tiles, obj.west_nb_tiles);
      fprintf(fh, 'West min filter threshold: %g\n', obj.west_min_range_filter);
      fprintf(fh, 'West max filter threshold: %g\n', obj.west_max_range_filter);
      fprintf(fh, 'West missing row/col: [%s]\n', obj.west_missing_row_col);
      fprintf(fh,'\n');
      fclose(fh);
    end
    function obj = reset(obj)
      obj.total_experiment_time = [];
      obj.total_stitching_time = [];
      obj.relative_displacement_time = [];
      obj.global_optimization_time = [];

      obj.north_repeatability = [];
      obj.north_overlap = [];
      obj.north_nb_tiles = [];
      obj.north_nb_valid_tiles = [];
      obj.north_min_range_filter = [];
      obj.north_max_range_filter = [];
      obj.north_missing_row_col = [];

      obj.west_repeatability = [];
      obj.west_overlap = [];
      obj.west_nb_tiles = [];
      obj.west_nb_valid_tiles = [];
      obj.west_min_range_filter = [];
      obj.west_max_range_filter = [];
      obj.west_missing_row_col = [];
    end
  end
  
  properties
    total_experiment_time;
    total_stitching_time;
    relative_displacement_time;
    global_optimization_time;
    
    north_repeatability;
    north_overlap;
    north_nb_tiles;
    north_nb_valid_tiles;
    north_min_range_filter;
    north_max_range_filter;
    north_missing_row_col;
    
    west_repeatability;
    west_overlap;
    west_nb_tiles;
    west_nb_valid_tiles;
    west_min_range_filter;
    west_max_range_filter;
    west_missing_row_col;
  end
end