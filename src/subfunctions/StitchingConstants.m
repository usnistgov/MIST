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



classdef StitchingConstants
  properties (Constant)
    % Control options
    USE_GPU = false;
    NUM_NCC_HILL_CLIMB_SEARCH_POINTS = 0; % for performing random search with hill climbing on ncc optimization
    USE_EXHAUSTIVE_CORRELAION_SEARCH = false;
    
    VALID_TRANSLATION_CC_OFFSET = 3;
    NB_FFT_PEAKS = 2;
    VALID_TRANSLATION_HEURISIC_CC_THRESHOLD = 0.5;
    
    % Minimum Spanning Tree Constants
    MST_START_TILE = 10;
    MST_CONNECTED_NORTH = 11;
    MST_CONNECTED_SOUTH = 12;
    MST_CONNECTED_LEFT = 22;
    MST_CONNECTED_RIGHT = 21;
    
    MST_NORTH = 11;
    MST_SOUTH = 12;
    MST_WEST = 22;
    MST_EAST = 21;
    
    % Overlap Computation Constants
    MLE_OPTIMIZATION_STALL_COUNT = 20;
  end
  
  % direction constants
  enumeration
    NORTH, SOUTH, EAST, WEST
  end
end