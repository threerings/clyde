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

package com.threerings.media.image;

import java.util.Arrays;
import java.util.Iterator;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import com.samskivert.util.Logger;

import com.samskivert.swing.Label;

/**
 * Image related utility functions.
 */
public class ImageUtil
{
    public static interface ImageCreator
    {
        /** Used by routines that need to create new images to allow the caller to dictate the
         * format (which may mean using createCompatibleImage). */
        public BufferedImage createImage (int width, int height, int transparency);
    }

    /**
     * Creates a new buffered image with the same sample model and color model as the source image
     * but with the new width and height. */
    public static BufferedImage createCompatibleImage (BufferedImage source, int width, int height)
    {
        WritableRaster raster = source.getRaster().createCompatibleWritableRaster(width, height);
        return new BufferedImage(source.getColorModel(), raster, false, null);
    }

    /**
     * Creates an image with the word "Error" written in it.
     */
    public static BufferedImage createErrorImage (int width, int height)
    {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED);
        Graphics2D g = (Graphics2D)img.getGraphics();
        g.setColor(Color.red);
        Label l = new Label("Error");
        l.layout(g);
        Dimension d = l.getSize();
        // fill that sucker with errors
        for (int yy = 0; yy < height; yy += d.height) {
            for (int xx = 0; xx < width; xx += (d.width+5)) {
                l.render(g, xx, yy);
            }
        }
        g.dispose();
        return img;
    }

    /**
     * Used to recolor images by shifting bands of color (in HSV color space) to a new hue. The
     * source images must be 8-bit color mapped images, as the recoloring process works by
     * analysing the color map and modifying it.
     */
    public static BufferedImage recolorImage (
        BufferedImage image, Color rootColor, float[] dists, float[] offsets)
    {
        return recolorImage(image, new Colorization[] {
            new Colorization(-1, rootColor, dists, offsets) });
    }

    /**
     * Recolors the supplied image as in
     * {@link #recolorImage(BufferedImage,Color,float[],float[])} obtaining the recoloring
     * parameters from the supplied {@link Colorization} instance.
     */
    public static BufferedImage recolorImage (BufferedImage image, Colorization cz)
    {
        return recolorImage(image, new Colorization[] { cz });
    }

    /**
     * Recolors the supplied image using the supplied colorizations.
     */
    public static BufferedImage recolorImage (BufferedImage image, Colorization[] zations)
    {
        ColorModel cm = image.getColorModel();
        if (!(cm instanceof IndexColorModel)) {
            throw new RuntimeException(Logger.format(
                "Unable to recolor images with non-index color model", "cm", cm.getClass()));
        }

        // now process the image
        IndexColorModel icm = (IndexColorModel)cm;
        int size = icm.getMapSize();
        int zcount = zations.length;
        int[] rgbs = new int[size];

        // fetch the color data
        icm.getRGBs(rgbs);

        // convert the colors to HSV
        float[] hsv = new float[3];
        int[] fhsv = new int[3];
        for (int ii = 0; ii < size; ii++) {
            int value = rgbs[ii];

            // don't fiddle with alpha pixels
            if ((value & 0xFF000000) == 0) {
                continue;
            }

            // convert the color to HSV
            int red = (value >> 16) & 0xFF;
            int green = (value >> 8) & 0xFF;
            int blue = (value >> 0) & 0xFF;
            Color.RGBtoHSB(red, green, blue, hsv);
            Colorization.toFixedHSV(hsv, fhsv);

            // see if this color matches and of our colorizations and recolor it if it does
            for (int z = 0; z < zcount; z++) {
                Colorization cz = zations[z];
                if (cz != null && cz.matches(hsv, fhsv)) {
                    // massage the HSV bands and update the RGBs array
                    rgbs[ii] = cz.recolorColor(hsv);
                    break;
                }
            }
        }

        // create a new image with the adjusted color palette
        IndexColorModel nicm = new IndexColorModel(
            icm.getPixelSize(), size, rgbs, 0, icm.hasAlpha(),
            icm.getTransparentPixel(), icm.getTransferType());
        return new BufferedImage(nicm, image.getRaster(), false, null);
    }

    /**
     * Paints multiple copies of the supplied image using the supplied graphics context such that
     * the requested area is filled with the image.
     */
    public static void tileImage (Graphics2D gfx, Mirage image, int x, int y, int width, int height)
    {
        int iwidth = image.getWidth(), iheight = image.getHeight();
        int xnum = width / iwidth, xplus = width % iwidth;
        int ynum = height / iheight, yplus = height % iheight;
        Shape oclip = gfx.getClip();

        for (int ii=0; ii < ynum; ii++) {
            // draw the full copies of the image across
            int xx = x;
            for (int jj=0; jj < xnum; jj++) {
                image.paint(gfx, xx, y);
                xx += iwidth;
            }

            if (xplus > 0) {
                gfx.clipRect(xx, y, xplus, iheight);
                image.paint(gfx, xx, y);
                gfx.setClip(oclip);
            }

            y += iheight;
        }

        if (yplus > 0) {
            int xx = x;
            for (int jj=0; jj < xnum; jj++) {
                gfx.clipRect(xx, y, iwidth, yplus);
                image.paint(gfx, xx, y);
                gfx.setClip(oclip);
                xx += iwidth;
            }

            if (xplus > 0) {
                gfx.clipRect(xx, y, xplus, yplus);
                image.paint(gfx, xx, y);
                gfx.setClip(oclip);
            }
        }
    }

    /**
     * Paints multiple copies of the supplied image using the supplied graphics context such that
     * the requested width is filled with the image.
     */
    public static void tileImageAcross (Graphics2D gfx, Mirage image, int x, int y, int width)
    {
        tileImage(gfx, image, x, y, width, image.getHeight());
    }

    /**
     * Paints multiple copies of the supplied image using the supplied graphics context such that
     * the requested height is filled with the image.
     */
    public static void tileImageDown (Graphics2D gfx, Mirage image, int x, int y, int height)
    {
        tileImage(gfx, image, x, y, image.getWidth(), height);
    }

    // Not fully added because we're not using it anywhere, plus it's probably a little sketchy
    // to create Area objects with all this pixely data.
    // Also, the Area was getting zeroed out when it was translated. Something to look into someday
    // if anyone wants to use this method.
//    /**
//     * Creates a mask that is opaque in the non-transparent areas of the source image.
//     */
//    public static Area createImageMask (BufferedImage src)
//    {
//        Raster srcdata = src.getData();
//        int wid = src.getWidth(), hei = src.getHeight();
//        Log.info("creating area of (" + wid + ", " + hei + ")");
//        Area a = new Area(new Rectangle(wid, hei));
//        Rectangle r = new Rectangle(1, 1);
//
//        for (int yy=0; yy < hei; yy++) {
//            for (int xx=0; xx < wid; xx++) {
//                if (srcdata.getSample(xx, yy, 0) == 0) {
//                    r.setLocation(xx, yy);
//                    a.subtract(new Area(r));
//                }
//            }
//        }
//
//        return a;
//    }

    /**
     * Creates and returns a new image consisting of the supplied image traced with the given
     * color and thickness.
     */
    public static BufferedImage createTracedImage (
        ImageCreator isrc, BufferedImage src, Color tcolor, int thickness)
    {
        return createTracedImage(isrc, src, tcolor, thickness, 1.0f, 1.0f);
    }

    /**
     * Creates and returns a new image consisting of the supplied image traced with the given
     * color, thickness and alpha transparency.
     */
    public static BufferedImage createTracedImage (
        ImageCreator isrc, BufferedImage src, Color tcolor, int thickness,
        float startAlpha, float endAlpha)
    {
        // create the destination image
        int wid = src.getWidth(), hei = src.getHeight();
        BufferedImage dest = isrc.createImage(wid, hei, Transparency.TRANSLUCENT);
        return createTracedImage(src, dest, tcolor, thickness, startAlpha, endAlpha);
    }

    /**
     * Creates and returns a new image consisting of the supplied image traced with the given
     * color, thickness and alpha transparency.
     */
    public static BufferedImage createTracedImage (
        BufferedImage src, BufferedImage dest, Color tcolor, int thickness,
        float startAlpha, float endAlpha)
    {
        // prepare various bits of working data
        int wid = src.getWidth(), hei = src.getHeight();
        int spixel = (tcolor.getRGB() & RGB_MASK);
        int salpha = (int)(startAlpha * 255);
        int tpixel = (spixel | (salpha << 24));
        boolean[] traced = new boolean[wid * hei];
        int stepAlpha = (thickness <= 1) ? 0 :
            (int)(((startAlpha - endAlpha) * 255) / (thickness - 1));

        // TODO: this could be made more efficient, e.g., if we made four passes through the image
        // in a vertical scan, horizontal scan, and opposing diagonal scans, making sure each
        // non-transparent pixel found during each scan is traced on both sides of the respective
        // scan direction. For now, we just naively check all eight pixels surrounding each pixel
        // in the image and fill the center pixel with the tracing color if it's transparent but
        // has a non-transparent pixel around it.
        for (int tt = 0; tt < thickness; tt++) {
            if (tt > 0) {
                // clear out the array of pixels traced this go-around
                Arrays.fill(traced, false);
                // use the destination image as our new source
                src = dest;
                // decrement the trace pixel alpha-level
                salpha -= Math.max(0, stepAlpha);
                tpixel = (spixel | (salpha << 24));
            }

            for (int yy = 0; yy < hei; yy++) {
                for (int xx = 0; xx < wid; xx++) {
                    // get the pixel we're checking
                    int argb = src.getRGB(xx, yy);

                    if ((argb & TRANS_MASK) != 0) {
                        // copy any pixel that isn't transparent
                        dest.setRGB(xx, yy, argb);

                    } else if (bordersNonTransparentPixel(src, wid, hei, traced, xx, yy)) {
                        dest.setRGB(xx, yy, tpixel);
                        // note that we traced this pixel this pass so
                        // that it doesn't impact other-pixel borderedness
                        traced[(yy*wid)+xx] = true;
                    }
                }
            }
        }

        return dest;
    }

    /**
     * Returns whether the given pixel is bordered by any non-transparent pixel.
     */
    protected static boolean bordersNonTransparentPixel (
        BufferedImage data, int wid, int hei, boolean[] traced, int x, int y)
    {
        // check the three-pixel row above the pixel
        if (y > 0) {
            for (int rxx = x - 1; rxx <= x + 1; rxx++) {
                if (rxx < 0 || rxx >= wid || traced[((y-1)*wid)+rxx]) {
                    continue;
                }

                if ((data.getRGB(rxx, y - 1) & TRANS_MASK) != 0) {
                    return true;
                }
            }
        }

        // check the pixel to the left
        if (x > 0 && !traced[(y*wid)+(x-1)]) {
            if ((data.getRGB(x - 1, y) & TRANS_MASK) != 0) {
                return true;
            }
        }

        // check the pixel to the right
        if (x < wid - 1 && !traced[(y*wid)+(x+1)]) {
            if ((data.getRGB(x + 1, y) & TRANS_MASK) != 0) {
                return true;
            }
        }

        // check the three-pixel row below the pixel
        if (y < hei - 1) {
            for (int rxx = x - 1; rxx <= x + 1; rxx++) {
                if (rxx < 0 || rxx >= wid || traced[((y+1)*wid)+rxx]) {
                    continue;
                }

                if ((data.getRGB(rxx, y + 1) & TRANS_MASK) != 0) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Create an image using the alpha channel from the first and the RGB values from the second.
     */
    public static BufferedImage composeMaskedImage (
        ImageCreator isrc, BufferedImage mask, BufferedImage base)
    {
        int wid = base.getWidth();
        int hei = base.getHeight();

        Raster maskdata = mask.getData();
        Raster basedata = base.getData();

        // create a new image using the rasters if possible
        if (maskdata.getNumBands() == 4 && basedata.getNumBands() >= 3) {
            WritableRaster target = basedata.createCompatibleWritableRaster(wid, hei);

            // copy the alpha from the mask image
            int[] adata = maskdata.getSamples(0, 0, wid, hei, 3, (int[]) null);
            target.setSamples(0, 0, wid, hei, 3, adata);

            // copy the RGB from the base image
            for (int ii=0; ii < 3; ii++) {
                int[] cdata = basedata.getSamples(0, 0, wid, hei, ii, (int[]) null);
                target.setSamples(0, 0, wid, hei, ii, cdata);
            }

            return new BufferedImage(mask.getColorModel(), target, true, null);

        } else {
            // otherwise composite them by rendering them with an alpha
            // rule
            BufferedImage target = isrc.createImage(wid, hei, Transparency.TRANSLUCENT);
            Graphics2D g2 = target.createGraphics();
            try {
                g2.drawImage(mask, 0, 0, null);
                g2.setComposite(AlphaComposite.SrcIn);
                g2.drawImage(base, 0, 0, null);
            } finally {
                g2.dispose();
            }
            return target;
        }
    }

    /**
     * Create a new image using the supplied shape as a mask from which to cut out pixels from the
     * supplied image. Pixels inside the shape will be added to the final image, pixels outside
     * the shape will be clear.
     */
    public static BufferedImage composeMaskedImage (
        ImageCreator isrc, Shape mask, BufferedImage base)
    {
        int wid = base.getWidth();
        int hei = base.getHeight();

        // alternate method for composition:
        // 1. create WriteableRaster with base data
        // 2. test each pixel with mask.contains() and set the alpha channel to fully-alpha if false
        // 3. create buffered image from raster
        // (I didn't use this method because it depends on the colormodel of the source image, and
        // was booching when the souce image was a cut-up from a tileset, and it seems like it
        // would take longer than the method we are using. But it's something to consider)

        // composite them by rendering them with an alpha rule
        BufferedImage target = isrc.createImage(wid, hei, Transparency.TRANSLUCENT);
        Graphics2D g2 = target.createGraphics();
        try {
            g2.setColor(Color.BLACK); // whatever, really
            g2.fill(mask);
            g2.setComposite(AlphaComposite.SrcIn);
            g2.drawImage(base, 0, 0, null);
        } finally {
            g2.dispose();
        }
        return target;
    }

    /**
     * Returns true if the supplied image contains a non-transparent pixel at the specified
     * coordinates, false otherwise.
     */
    public static boolean hitTest (BufferedImage image, int x, int y)
    {
        // it's only a hit if the pixel is non-transparent
        int argb = image.getRGB(x, y);
        return (argb >> 24) != 0;
    }

    /**
     * Computes the bounds of the smallest rectangle that contains all non-transparent pixels of
     * this image. This isn't extremely efficient, so you shouldn't be doing this anywhere
     * exciting.
     */
    public static void computeTrimmedBounds (BufferedImage image, Rectangle tbounds)
    {
        // this could be more efficient, but it's run as a batch process and doesn't really take
        // that long anyway
        int width = image.getWidth(), height = image.getHeight();

        int firstrow = -1, lastrow = -1, minx = width, maxx = 0;
        for (int yy = 0; yy < height; yy++) {

            int firstidx = -1, lastidx = -1;
            for (int xx = 0; xx < width; xx++) {
                // if this pixel is transparent, do nothing
                int argb = image.getRGB(xx, yy);
                if ((argb >> 24) == 0) {
                    continue;
                }

                // otherwise, if we've not seen a non-transparent pixel, make a note that this is
                // the first non-transparent pixel in the row
                if (firstidx == -1) {
                    firstidx = xx;
                }
                // keep track of the last non-transparent pixel we saw
                lastidx = xx;
            }

            // if we saw no pixels on this row, we can bail now
            if (firstidx == -1) {
                continue;
            }

            // update our min and maxx
            minx = Math.min(firstidx, minx);
            maxx = Math.max(lastidx, maxx);

            // otherwise keep track of the first row on which we see pixels and the last row on
            // which we see pixels
            if (firstrow == -1) {
                firstrow = yy;
            }
            lastrow = yy;
        }

        // fill in the dimensions
        if (firstrow != -1) {
            tbounds.x = minx;
            tbounds.y = firstrow;
            tbounds.width = maxx - minx + 1;
            tbounds.height = lastrow - firstrow + 1;
        } else {
            // Entirely blank image.  Return 1x1 blank image.
            tbounds.x = 0;
            tbounds.y = 0;
            tbounds.width = 1;
            tbounds.height = 1;
        }
    }

    /**
     * Returns the estimated memory usage in bytes for the specified image.
     */
    public static long getEstimatedMemoryUsage (BufferedImage image)
    {
        if (image != null) {
            return getEstimatedMemoryUsage(image.getRaster());
        } else {
            return 0;
        }
    }

    /**
     * Returns the estimated memory usage in bytes for the specified raster.
     */
    public static long getEstimatedMemoryUsage (Raster raster)
    {
        // we assume that the data buffer stores each element in a byte-rounded memory element;
        // maybe the buffer is smarter about things than this, but we're better to err on the safe
        // side
        DataBuffer db = raster.getDataBuffer();
        int bpe = (int)Math.ceil(DataBuffer.getDataTypeSize(db.getDataType()) / 8f);
        return bpe * db.getSize();
    }

    /**
     * Returns the estimated memory usage in bytes for all buffered images in the supplied
     * iterator.
     */
    public static long getEstimatedMemoryUsage (Iterator<BufferedImage> iter)
    {
        long size = 0;
        while (iter.hasNext()) {
            BufferedImage image = iter.next();
            size += getEstimatedMemoryUsage(image);
        }
        return size;
    }

    /**
     * Obtains the default graphics configuration for this VM. If the JVM is in headless mode,
     * this method will return null.
     */
    protected static GraphicsConfiguration getDefGC ()
    {
        if (_gc == null) {
            // obtain information on our graphics environment
            try {
                GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice gd = env.getDefaultScreenDevice();
                _gc = gd.getDefaultConfiguration();
            } catch (HeadlessException e) {
                // no problem, just return null
            }
        }
        return _gc;
    }

    /** The graphics configuration for the default screen device. */
    protected static GraphicsConfiguration _gc;

    /** Used when seeking fully transparent pixels for outlining. */
    protected static final int TRANS_MASK = (0xFF << 24);

    /** Used when outlining. */
    protected static final int RGB_MASK = 0x00FFFFFF;
}
