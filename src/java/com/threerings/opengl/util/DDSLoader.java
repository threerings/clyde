//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.opengl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import org.lwjgl.opengl.EXTBgra;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.threerings.opengl.renderer.Texture1D;
import com.threerings.opengl.renderer.Texture2D;
import com.threerings.opengl.renderer.Texture3D;
import com.threerings.opengl.renderer.TextureCubeMap;

/**
 * Provides a means of loading texture data from DirectDraw Surface (DDS) files.
 */
public class DDSLoader
{
    /**
     * Loads a 1D texture from the supplied file.
     */
    public static void load (File file, Texture1D texture, boolean border)
        throws IOException
    {
        ByteBuffer buf = load(file);
        Header header = new Header(buf);
        if ((header.caps2 & (DDSCAPS2_VOLUME | DDSCAPS2_CUBEMAP)) != 0 || header.height != 1) {
            throw new IOException("Not a 1D texture: " + file);
        }

    }

    /**
     * Loads a 2D texture from the supplied file.
     */
    public static void load (File file, Texture2D texture, boolean border)
        throws IOException
    {
        ByteBuffer buf = load(file);
        Header header = new Header(buf);
        if ((header.caps2 & (DDSCAPS2_VOLUME | DDSCAPS2_CUBEMAP)) != 0) {
            throw new IOException("Not a 2D texture: " + file);
        }
        if (header.fourCC == null) {
            int width = header.width;
            int height = header.height;
            int format = ((header.pixelFormatFlags & DDPF_ALPHAPIXELS) == 0) ?
                GL11.GL_RGB : GL11.GL_RGBA;
            int dformat = header.getUncompressedFormat();
            if (dformat == -1) {
                throw new IOException("Unknown format");
            }
            for (int ii = 0, nn = header.getLevels(); ii < nn; ii++) {
                int pitch = width * (format == GL11.GL_RGB ? 3 : 4);
                buf.limit(buf.position() + height*pitch);
                texture.setImage(ii, format, width, height, border, dformat, GL11.GL_UNSIGNED_BYTE, buf);
                width = Math.max(width/2, 1);
                height = Math.max(height/2, 1);
                buf.position(buf.limit());
            }
        } else {
            int width = header.width;
            int height = header.height;
            int format = header.getCompressedFormat();
            if (format == -1) {
                throw new IOException("Unknown format: " + header.fourCC);
            }
            for (int ii = 0, nn = header.getLevels(); ii < nn; ii++) {
                int size = Math.max(width/4, 1) * Math.max(height/4, 1) *
                    (format == EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT ? 8 : 16);
                buf.limit(buf.position() + size);
                texture.setCompressedImage(ii, format, width, height, border, buf);
                width = Math.max(width/2, 1);
                height = Math.max(height/2, 1);
                buf.position(buf.limit());
            }
        }
    }

    /**
     * Loads a 3D texture from the supplied file.
     */
    public static void load (File file, Texture3D texture, boolean border)
        throws IOException
    {
        ByteBuffer buf = load(file);
        Header header = new Header(buf);
        if ((header.caps2 & DDSCAPS2_VOLUME) == 0) {
            throw new IOException("Not a volume texture: " + file);
        }

    }

    /**
     * Loads a cube map texture from the supplied file.
     */
    public static void load (File file, TextureCubeMap texture, boolean border)
        throws IOException
    {
        ByteBuffer buf = load(file);
        Header header = new Header(buf);
        if ((header.caps2 & DDSCAPS2_CUBEMAP) == 0) {
            throw new IOException("Not a cube map texture: " + file);
        }
    }

    /**
     * Loads the entire file into a (direct, little-endian) byte buffer.
     */
    protected static ByteBuffer load (File file)
        throws IOException
    {
        FileInputStream in = new FileInputStream(file);
        try {
            FileChannel channel = in.getChannel();
            ByteBuffer buf =
                ByteBuffer.allocateDirect((int)channel.size()).order(ByteOrder.LITTLE_ENDIAN);
            channel.read(buf);
            buf.rewind();
            return buf;

        } finally {
            in.close();
        }
    }

    /**
     * The DDS file header.
     */
    protected static class Header
    {
        /** The image flags. */
        public int flags;

        /** The base image height. */
        public int height;

        /** The base image width. */
        public int width;

        /** The pitch or linear size. */
        public int pitchOrLinearSize;

        /** The depth. */
        public int depth;

        /** The number of mipmaps. */
        public int mipMapCount;

        /** The pixel format flags. */
        public int pixelFormatFlags;

        /** The four CC code, if any. */
        public String fourCC;

        /** The RGB bit count. */
        public int rgbBitCount;

        /** The red, green, and blue bit masks. */
        public int rBitMask, gBitMask, bBitMask;

        /** The alpha bit mask. */
        public int alphaBitMask;

        /** The first set of capability bits. */
        public int caps1;

        /** The second set of capability bits. */
        public int caps2;

        /**
         * Reads the header from the specified buffer.
         */
        public Header (ByteBuffer buf)
            throws IOException
        {
            // verify the magic number
            byte[] bytes = new byte[4];
            buf.get(bytes);
            String magic = new String(bytes, "US-ASCII");
            if (!magic.equals("DDS ")) {
                throw new IOException("Bad magic: " + magic);
            }

            // verify the header size
            int size = buf.getInt();
            if (size != 124) {
                throw new IOException("Wrong header size: " + size);
            }

            flags = buf.getInt();
            height = buf.getInt();
            width = buf.getInt();
            pitchOrLinearSize = buf.getInt();
            depth = buf.getInt();
            mipMapCount = buf.getInt();
            buf.position(buf.position() + 11*4); // advance past first reserved

            // read the pixel format structure
            size = buf.getInt();
            if (size != 32) {
                throw new IOException("Wrong pixel format size: " + size);
            }
            pixelFormatFlags = buf.getInt();
            buf.get(bytes);
            fourCC = ((pixelFormatFlags & DDPF_FOURCC) == 0) ?
                null : new String(bytes, "US-ASCII");
            rgbBitCount = buf.getInt();
            rBitMask = buf.getInt();
            gBitMask = buf.getInt();
            bBitMask = buf.getInt();
            alphaBitMask = buf.getInt();

            // read the caps structure
            caps1 = buf.getInt();
            caps2 = buf.getInt();
            buf.position(buf.position() + 2*4); // advance past caps reserved

            buf.position(buf.position() + 4); // advance past second reserved
        }

        /**
         * Returns the number of stored mipmap levels.
         */
        public int getLevels ()
        {
            return ((flags & DDSD_MIPMAPCOUNT) == 0) ? 1 : mipMapCount;
        }

        /**
         * Returns the OpenGL uncompressed format corresponding to the texture, or -1 if
         * unknown.
         */
        public int getUncompressedFormat ()
        {
            // this is a little confusing because the masks are in little-endian order, so they're
            // reversed with respect to the OpenGL constants
            if (rBitMask == 0xFF && gBitMask == 0xFF00 && bBitMask == 0xFF0000) {
                if ((pixelFormatFlags & DDPF_ALPHAPIXELS) == 0) {
                    return GL11.GL_RGB;
                } else if (alphaBitMask == 0xFF000000) {
                    return GL11.GL_RGBA;
                }
            } else if (rBitMask == 0xFF0000 && gBitMask == 0xFF00 && bBitMask == 0xFF) {
                if ((pixelFormatFlags & DDPF_ALPHAPIXELS) == 0) {
                    return EXTBgra.GL_BGR_EXT;
                } else if (alphaBitMask == 0xFF000000) {
                    return EXTBgra.GL_BGRA_EXT;
                }
            }
            return -1;
        }

        /**
         * Returns the OpenGL compressed format corresponding to the texture, or -1 if
         * unknown.
         */
        public int getCompressedFormat ()
        {
            if ("DXT1".equals(fourCC)) {
                return EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
            } else if ("DXT3".equals(fourCC)) {
                return EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
            } else if ("DXT5".equals(fourCC)) {
                return EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
            } else {
                return -1;
            }
        }
    }

    /** Flag indicating that the header contains the bytes per scan line (uncompressed texture). */
    protected static final int DDSD_PITCH = 0x00000008;

    /** Flag indicating that the header contains the total number of bytes in the top-level texture
     * (compressed texture). */
    protected static final int DDSD_LINEARSIZE = 0x00080000;

    /** Flag indicating that the header contains a mipmap count. */
    protected static final int DDSD_MIPMAPCOUNT = 0x00020000;

    /** Flag indicating that the header contains a depth value for volume textures. */
    protected static final int DDSD_DEPTH = 0x00800000;

    /** Pixel format flag indicating that the texture contains uncompressed RGB data. */
    protected static final int DDPF_RGB = 0x00000040;

    /** Pixel format flag indicating that the texture contains compressed RGB data. */
    protected static final int DDPF_FOURCC = 0x00000004;

    /** Pixel format flag indicating that the texture contains alpha data. */
    protected static final int DDPF_ALPHAPIXELS = 0x00000001;

    /** Caps flag indicating that the texture contains mipmaps. */
    protected static final int DDSCAPS_MIPMAP = 0x00400000;

    /** Caps flag indicating that the texture contains a cube map. */
    protected static final int DDSCAPS2_CUBEMAP = 0x00000200;

    /** Caps flag indicating the presence of the positive x cube map face. */
    protected static final int DDSCAPS2_CUBEMAP_POSITIVEX = 0x00000400;

    /** Caps flag indicating the presence of the negative x cube map face. */
    protected static final int DDSCAPS2_CUBEMAP_NEGATIVEX = 0x00000800;

    /** Caps flag indicating the presence of the positive y cube map face. */
    protected static final int DDSCAPS2_CUBEMAP_POSITIVEY = 0x00001000;

    /** Caps flag indicating the presence of the negative y cube map face. */
    protected static final int DDSCAPS2_CUBEMAP_NEGATIVEY = 0x00002000;

    /** Caps flag indicating the presence of the positive z cube map face. */
    protected static final int DDSCAPS2_CUBEMAP_POSITIVEZ = 0x00004000;

    /** Caps flag indicating the presence of the negative z cube map face. */
    protected static final int DDSCAPS2_CUBEMAP_NEGATIVEZ = 0x00008000;

    /** Caps flag indicating that the texture is a volume texture. */
    protected static final int DDSCAPS2_VOLUME = 0x00200000;
}
