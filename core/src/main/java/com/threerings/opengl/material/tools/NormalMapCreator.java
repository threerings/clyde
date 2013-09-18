//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
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

package com.threerings.opengl.material.tools;

import java.awt.image.BufferedImage;

import java.io.File;

import javax.imageio.ImageIO;

/**
 * A simple tool to create normal maps (optionally with the original height in the alpha channel)
 * from bump maps.
 */
public class NormalMapCreator
{
    /**
     * Program entry point.
     */
    public static void main (String[] args)
        throws Exception
    {
        if (args.length < 4) {
            System.err.println("Usage: NormalMapCreator <input bump map> <output normal map> " +
                "<scale> <include depth>");
            return;
        }
        BufferedImage in = ImageIO.read(new File(args[0]));
        int width = in.getWidth(), height = in.getHeight();
        float scale = Float.parseFloat(args[2]);
        boolean depth = Boolean.parseBoolean(args[3]);

        int[] inRGB = in.getRGB(0, 0, width, height, null, 0, width);
        int[] outRGB = new int[width * height];
        for (int yy = 0; yy < height; yy++) {
            for (int xx = 0; xx < width; xx++) {
                // we determine the normal by taking the cross product of the vector from left to
                // right and the vector from bottom to top
                int top = inRGB[Math.max(0, yy-1)*width + xx];
                int bottom = inRGB[Math.min(height-1, yy+1)*width + xx];
                int left = inRGB[yy*width + Math.max(0, xx-1)];
                int right = inRGB[yy*width + Math.min(width-1, xx+1)];

                // extract, compute normal, repack
                double nx = ((left & 0xFF) - (right & 0xFF)) * scale / 255.0;
                double ny = ((bottom & 0xFF) - (top & 0xFF)) * scale / 255.0;
                double nz = 1.0;
                double rlen = 1.0 / Math.sqrt(nx*nx + ny*ny + nz*nz);
                int r = (int)((nx*rlen + 1.0) * 255.0 / 2.0);
                int g = (int)((ny*rlen + 1.0) * 255.0 / 2.0);
                int b = (int)((nz*rlen + 1.0) * 255.0 / 2.0);
                int a = inRGB[yy*width + xx] & 0xFF;
                outRGB[yy*width + xx] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }

        BufferedImage out = new BufferedImage(width, height, depth ?
            BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR);
        out.setRGB(0, 0, width, height, outRGB, 0, width);
        ImageIO.write(out, "png", new File(args[1]));
    }
}
