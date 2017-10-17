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




function save_32_tiff(I1, name)

% Create new TIFF file
t = Tiff(name,'w');

% create tags for 32bit image
tagstruct.ImageLength = size(I1,1);
tagstruct.ImageWidth = size(I1,2);

% tagstruct.TileWidth = 256;
% tagstruct.TileLength = 256;

tagstruct.Photometric = 1;
tagstruct.SampleFormat = 3;
tagstruct.PlanarConfiguration = 1;
tagstruct.BitsPerSample = 32;
tagstruct.Software = 'MATLAB';
t.setTag(tagstruct)

% write image and close it
t.write(single(I1));
t.close();


