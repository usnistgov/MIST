package gov.nist.isg.mist.lib.export;

import loci.formats.out.OMETiffWriter;

public enum CompressionMode {
    UNCOMPRESSED(OMETiffWriter.COMPRESSION_UNCOMPRESSED),
    LZW(OMETiffWriter.COMPRESSION_LZW),
    JPEG_2000_LOSSY(OMETiffWriter.COMPRESSION_J2K_LOSSY),
    JPEG(OMETiffWriter.COMPRESSION_JPEG),
    ZLIB(OMETiffWriter.COMPRESSION_ZLIB)
    ;
    private CompressionMode(String compressionName) {
        this.compressionName = compressionName;
    }

    public String getCompressionName() {
        return this.compressionName;
    }

    /**
     * Gets the tooltip text
     *
     * @return the tooltip text
     */
    public String getToolTipText() {
        return this.toolTipText;
    }

    private String compressionName;
    private String toolTipText;


}
