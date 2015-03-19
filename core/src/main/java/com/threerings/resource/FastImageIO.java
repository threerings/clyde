//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// https://github.com/threerings/nenya
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.resource;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.WritableRaster;

import com.samskivert.io.StreamUtil;

/**
 * Provides routines for writing and reading uncompressed 8-bit color
 * mapped images in a manner that is extremely fast and generates a
 * minimal amount of garbage during the loading process.
 */
public class FastImageIO
{
    /**
     * A suffix for use when storing raw images in bundles or on the file system.
     */
    public static final String FILE_SUFFIX = ".raw";

    /**
     * Returns true if the supplied image is of a format that is supported by the fast image I/O
     * services, false if not.
     */
    public static boolean canWrite (BufferedImage image)
    {
        return (image.getColorModel() instanceof IndexColorModel) &&
            (image.getRaster().getDataBuffer() instanceof DataBufferByte);
    }

    /**
     * Writes the supplied image to the supplied output stream.
     *
     * @exception IOException thrown if an error occurs writing to the output stream.
     */
    public static void write (BufferedImage image, OutputStream out)
        throws IOException
    {
        DataOutputStream dout = new DataOutputStream(out);

        // write the image dimensions
        int width = image.getWidth(), height = image.getHeight();
        dout.writeInt(width);
        dout.writeInt(height);

        // write the color model information
        IndexColorModel cmodel = (IndexColorModel)image.getColorModel();
        int tpixel = cmodel.getTransparentPixel();
        dout.writeInt(tpixel);
        int msize = cmodel.getMapSize();
        int[] map = new int[msize];
        cmodel.getRGBs(map);
        dout.writeInt(msize);
        for (int element : map) {
            dout.writeInt(element);
        }

        // write the raster data
        DataBufferByte dbuf = (DataBufferByte)image.getRaster().getDataBuffer();
        byte[] data = dbuf.getData();
        if (data.length != width * height) {
            String errmsg = "Raster data not same size as image! [" +
                width + "x" + height + " != " + data.length + "]";
            throw new IllegalStateException(errmsg);
        }
        dout.write(data);
        dout.flush();
    }

    /**
     * Reads an image from the supplied file (which must contain an image previously written via a
     * call to {@link #write}).
     *
     * @exception IOException thrown if an error occurs reading from the file.
     */
    public static BufferedImage read (File file)
        throws IOException
    {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel fchan = raf.getChannel();

        try {
            MappedByteBuffer mbuf = fchan.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            return read(mbuf);
        } finally {
            fchan.close();
            raf.close();
        }
    }

    /**
     * Reads an image from the supplied input stream (which must return the image format previously
     * written via a call to {@link #write}).
     *
     * @exception IOException thrown if an error occurs reading from the file.
     */
    public static BufferedImage read (InputStream in)
        throws IOException
    {
        return read(ByteBuffer.wrap(StreamUtil.toByteArray(in)));
    }

    /**
     * Reads an image from the supplied byte buffer (which must return the image format previously
     * written via a call to {@link #write}).
     *
     * @exception IOException thrown if an error occurs reading from the file.
     */
    public static BufferedImage read (ByteBuffer byteBuffer)
        throws IOException
    {
        // read in our integer fields
        IntBuffer ibuf = byteBuffer.asIntBuffer();
        int width = ibuf.get();
        int height = ibuf.get();
        /* int tpixel = */ ibuf.get();
        int msize = ibuf.get();

        if (width > Short.MAX_VALUE || width < 0 || height > Short.MAX_VALUE || height < 0) {
            throw new IOException("Bogus image size " + width + "x" + height);
        }

        IndexColorModel cmodel;
        synchronized (_origin) { // any old object will do
            // make sure our colormap array is big enough
            if  (_cmap == null || _cmap.length < msize) {
                _cmap = new int[msize];
            }
            // read in the data and create our colormap
            ibuf.get(_cmap, 0, msize);
            cmodel = new IndexColorModel(
                8, msize, _cmap, 0, DataBuffer.TYPE_BYTE, null);
        }

        // advance the byte buffer accordingly
        byteBuffer.position(ibuf.position() * 4);

        // read in the image data itself
        byte[] data = new byte[width*height];
        byteBuffer.get(data);

        // create the image from our component parts
        DataBuffer dbuf = new DataBufferByte(data, data.length, 0);
        int[] offsets = new int[] { 0 };
        PixelInterleavedSampleModel smodel =
            new PixelInterleavedSampleModel(
                DataBuffer.TYPE_BYTE, width, height, 1, width, offsets);
        WritableRaster raster = WritableRaster.createWritableRaster(smodel, dbuf, _origin);
        return new BufferedImage(cmodel, raster, false, null);
    }

    /** Used when loading our color map. */
    protected static int[] _cmap;

    /** Used when creating our writable raster. */
    protected static Point _origin = new Point(0, 0);
}
