package com.keypoint;

/**
 * PngEncoderB takes a Java BufferedImage object and creates a byte string which can be saved as a PNG file.
 * The encoder will accept BufferedImages with eight-bit samples
 * or 4-byte ARGB samples. 
 * 
 * There is also code to handle 4-byte samples returned as
 * one int per pixel, but that has not been tested.
 *
 * Thanks to Jay Denny at KeyPoint Software
 *    http://www.keypoint.com/
 * who let me develop this code on company time.
 *
 * You may contact me with (probably very-much-needed) improvements,
 * comments, and bug fixes at:
 *
 *   david@catcode.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * A copy of the GNU LGPL may be found at
 * http://www.gnu.org/copyleft/lesser.html,
 *
 * @author J. David Eisenberg
 * @version 1.4, 31 March 2000
 */

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;

public class PngEncoderB extends PngEncoder 
{
    protected BufferedImage image;
    protected WritableRaster wRaster;
    protected int tType;

    /**
     * Class constructor
     *
     */
    public PngEncoderB()
    {
        this( null, false, FILTER_NONE, 0 );
    }

    /**
     * Class constructor specifying BufferedImage to encode, with no alpha channel encoding.
     *
     * @param image A Java BufferedImage object
     */
    public PngEncoderB( BufferedImage image )
    {
        this(image, false, FILTER_NONE, 0);
    }

    /**
     * Class constructor specifying BufferedImage to encode, and whether to encode alpha.
     *
     * @param image A Java BufferedImage object
     * @param encodeAlpha Encode the alpha channel? false=no; true=yes
     */
    public PngEncoderB( BufferedImage image, boolean encodeAlpha )
    {
        this( image, encodeAlpha, FILTER_NONE, 0 );
    }

    /**
     * Class constructor specifying BufferedImage to encode, whether to encode alpha, and filter to use.
     *
     * @param image A Java BufferedImage object
     * @param encodeAlpha Encode the alpha channel? false=no; true=yes
     * @param whichFilter 0=none, 1=sub, 2=up
     */
    public PngEncoderB( BufferedImage image, boolean encodeAlpha,
       int whichFilter )
    {
        this( image, encodeAlpha, whichFilter, 0 );
    }

    /**
     * Class constructor specifying BufferedImage source to encode, whether to encode alpha, filter to use, and compression level
     *
     * @param image A Java BufferedImage object
     * @param encodeAlpha Encode the alpha channel? false=no; true=yes
     * @param whichFilter 0=none, 1=sub, 2=up
     * @param compLevel 0..9
     */
    public PngEncoderB( BufferedImage image, boolean encodeAlpha,
      int whichFilter, int compLevel )
    {
        this.image = image;
        this.encodeAlpha = encodeAlpha;
        setFilter( whichFilter );
        if (compLevel >=0 && compLevel <=9)
        {
            this.compressionLevel = compLevel;
        }
    }

    /**
     * Set the BufferedImage to be encoded
     *
     * @param BufferedImage A Java BufferedImage object
     */
    public void setImage( BufferedImage image )
    {
        this.image = image;
        pngBytes = null;
    }

    /**
     * Creates an array of bytes that is the PNG equivalent of the current image, specifying whether to encode alpha or not.
     *
     * @param encodeAlpha boolean false=no alpha, true=encode alpha
     * @return an array of bytes, or null if there was a problem
     */
    public byte[] pngEncode( boolean encodeAlpha )
    {
        byte[]  pngIdBytes = { -119, 80, 78, 71, 13, 10, 26, 10 };
        int     i;

        if (image == null)
        {
            return null;
        }
        width = image.getWidth( null );
        height = image.getHeight( null );
        this.image = image;

        if (!establishStorageInfo())
        {
            return null;
        }
        
        /*
         * start with an array that is big enough to hold all the pixels
         * (plus filter bytes), and an extra 200 bytes for header info
         */
        pngBytes = new byte[((width+1) * height * 3) + 200];

        /*
         * keep track of largest byte written to the array
         */
        maxPos = 0;

        bytePos = writeBytes( pngIdBytes, 0 );
        hdrPos = bytePos;
        writeHeader();
        dataPos = bytePos;
        if (writeImageData())
        {
            writeEnd();
            pngBytes = resizeByteArray( pngBytes, maxPos );
        }
        else
        {
            pngBytes = null;
        }
        return pngBytes;
    }

    /**
     * Creates an array of bytes that is the PNG equivalent of the current image.
     * Alpha encoding is determined by its setting in the constructor.
     *
     * @return an array of bytes, or null if there was a problem
     */
    public byte[] pngEncode()
    {
        return pngEncode( encodeAlpha );
    }

    /**
     * 
     * Get and set variables that determine how picture is stored.
     *
     * Retrieves the writable raster of the buffered image,
     * as well its transfer type.
     *
     * Sets number of output bytes per pixel, and, if only
     * eight-bit bytes, turns off alpha encoding.
     * @return true if 1-byte or 4-byte data, false otherwise
     */
    protected boolean establishStorageInfo()
    {
        int dataBytes;
    
        wRaster = image.getRaster();
        dataBytes = wRaster.getNumDataElements();
        tType = wRaster.getTransferType();

        if (((tType == DataBuffer.TYPE_BYTE) && (dataBytes == 4)) ||
            ((tType == DataBuffer.TYPE_INT) && (dataBytes == 1)) )
        {
            bytesPerPixel = (encodeAlpha) ? 4 : 3;
        }
        else if ((tType == DataBuffer.TYPE_BYTE) && (dataBytes == 1))
        {
            bytesPerPixel = 1;
            encodeAlpha = false;    // one-byte samples
        }
        else
        {
            return false;
        }
        return true;
    }

    /**
     * Write a PNG "IHDR" chunk into the pngBytes array.
     */
    protected void writeHeader()
    {
        int startPos;

        startPos = bytePos = writeInt4( 13, bytePos );
        bytePos = writeString( "IHDR", bytePos );
        width = image.getWidth( null );
        height = image.getHeight( null );
        bytePos = writeInt4( width, bytePos );
        bytePos = writeInt4( height, bytePos );
        bytePos = writeByte( 8, bytePos ); // bit depth
        if (bytesPerPixel != 1)
        {
            bytePos = writeByte( (encodeAlpha) ? 6 : 2, bytePos ); // direct model
        }
        else
        {
            bytePos = writeByte( 3, bytePos ); // indexed
        }
        bytePos = writeByte( 0, bytePos ); // compression method
        bytePos = writeByte( 0, bytePos ); // filter method
        bytePos = writeByte( 0, bytePos ); // no interlace
        crc.reset();
        crc.update( pngBytes, startPos, bytePos-startPos );
        crcValue = crc.getValue();
        bytePos = writeInt4( (int) crcValue, bytePos );
    }

    protected void writePalette( IndexColorModel icm )
    {
        byte[] redPal = new byte[256];
        byte[] greenPal = new byte[256];
        byte[] bluePal = new byte[256];
        byte[] allPal = new byte[768];
        int i;

        icm.getReds( redPal );
        icm.getGreens( greenPal );
        icm.getBlues( bluePal );
        for (i=0; i<256; i++)
        {
            allPal[i*3  ] = redPal[i];
            allPal[i*3+1] = greenPal[i];
            allPal[i*3+2] = bluePal[i];
        }
        bytePos = writeInt4( 768, bytePos );
        bytePos = writeString( "PLTE", bytePos );
        crc.reset();
        crc.update("PLTE".getBytes());
        bytePos = writeBytes( allPal, bytePos );
        crc.update( allPal );
        crcValue = crc.getValue();
        bytePos = writeInt4( (int) crcValue, bytePos );
    }

    /**
     * Write the image data into the pngBytes array.
     * This will write one or more PNG "IDAT" chunks. In order
     * to conserve memory, this method grabs as many rows as will
     * fit into 32K bytes, or the whole image; whichever is less.
     *
     *
     * @return true if no errors; false if error grabbing pixels
     */
    protected boolean writeImageData()
    {
        int rowsLeft = height;  // number of rows remaining to write
        int startRow = 0;       // starting row to process this time through
        int nRows;              // how many rows to grab at a time

        byte[] scanLines;       // the scan lines to be compressed
        int scanPos;            // where we are in the scan lines
        int startPos;           // where this line's actual pixels start (used for filtering)
        int readPos;            // position from which source pixels are read

        byte[] compressedLines; // the resultant compressed lines
        int nCompressed;        // how big is the compressed area?

        byte[] pixels;          // storage area for byte-sized pixels
        int[] iPixels;          // storage area for int-sized pixels

        Deflater scrunch = new Deflater( compressionLevel );
        ByteArrayOutputStream outBytes = 
            new ByteArrayOutputStream(1024);
            
        DeflaterOutputStream compBytes =
            new DeflaterOutputStream( outBytes, scrunch );

        if (bytesPerPixel == 1)
        {
            writePalette( (IndexColorModel) image.getColorModel() );
        }

        try
        {
            while (rowsLeft > 0)
            {
                nRows = Math.min( 32767 / (width*(bytesPerPixel+1)), rowsLeft );
                // nRows = rowsLeft;

                /*
                 * Create a data chunk. scanLines adds "nRows" for
                 * the filter bytes.
                 */
                scanLines = new byte[width * nRows * bytesPerPixel +  nRows];

                if (filter == FILTER_SUB)
                {
                    leftBytes = new byte[16];
                }
                if (filter == FILTER_UP)
                {
                    priorRow = new byte[width*bytesPerPixel];
                }

                if (tType == DataBuffer.TYPE_BYTE)
                {
                    pixels = (byte[]) wRaster.getDataElements(
                        0, startRow, width, nRows, null );
                    iPixels = null;
                }
                else
                {
                    iPixels = (int[]) wRaster.getDataElements(
                        0, startRow, width, nRows, null );
                    pixels = null;
                }

                scanPos = 0;
                readPos = 0;
                startPos = 1;
                for (int i=0; i<width*nRows; i++)
                {
                    if (i % width == 0)
                    {
                        scanLines[scanPos++] = (byte) filter; 
                        startPos = scanPos;
                    }

                    if (bytesPerPixel == 1)
                    {
                        scanLines[scanPos++] = pixels[readPos++];
                    }
                    else if (tType == DataBuffer.TYPE_BYTE)
                    {
                        scanLines[scanPos++] = pixels[readPos++];
                        scanLines[scanPos++] = pixels[readPos++];
                        scanLines[scanPos++] = pixels[readPos++];
                        if (encodeAlpha)
                        {
                            scanLines[scanPos++] = pixels[readPos++];
                        }
                        else
                        {
                            readPos++;
                        }
                    }
                    else
                    {
                        scanLines[scanPos++] = (byte) ((iPixels[readPos] >> 16) & 0xff);
                        scanLines[scanPos++] = (byte) ((iPixels[readPos] >>  8) & 0xff);
                        scanLines[scanPos++] = (byte) ((iPixels[readPos]      ) & 0xff);
                        if (encodeAlpha)
						{
                            scanLines[scanPos++] = (byte) ((iPixels[readPos] >> 24) & 0xff );
                        }
                        readPos++;
                    }
                    if ((i % width == width-1) && (filter != FILTER_NONE))
                    {
                        if (filter == FILTER_SUB)
                        {
                            filterSub( scanLines, startPos, width );
                        }
                        if (filter == FILTER_UP)
                        {
                            filterUp( scanLines, startPos, width );
                        }
                    }
                }

                /*
                 * Write these lines to the output area
                 */
                compBytes.write( scanLines, 0, scanPos );

                startRow += nRows;
                rowsLeft -= nRows;
            }
            compBytes.close();

            /*
             * Write the compressed bytes
             */
            compressedLines = outBytes.toByteArray();
            nCompressed = compressedLines.length;

            crc.reset();
            bytePos = writeInt4( nCompressed, bytePos );
            bytePos = writeString("IDAT", bytePos );
            crc.update("IDAT".getBytes());
            bytePos = writeBytes( compressedLines, nCompressed, bytePos );
            crc.update( compressedLines, 0, nCompressed );

            crcValue = crc.getValue();
            bytePos = writeInt4( (int) crcValue, bytePos );
            scrunch.finish();
            return true;
        }
        catch (IOException e)
        {
            System.err.println( e.toString());
            return false;
        }
    }

}

