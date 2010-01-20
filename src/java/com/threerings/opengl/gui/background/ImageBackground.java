//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

package com.threerings.opengl.gui.background;

import com.threerings.opengl.renderer.Renderer;

import com.threerings.opengl.gui.Image;
import com.threerings.opengl.gui.util.Insets;

/**
 * Supports image backgrounds in a variety of ways. Specifically:
 *
 * <ul>
 * <li> Centering the image either horizontally, vertically or both.
 * <li> Scaling the image either horizontally, vertically or both.
 * <li> Tiling the image either horizontally, vertically or both.
 * <li> Framing the image in a fancy way: the background image is divided into
 * nine sections (three across and three down), the corners are rendered
 * unscaled, the central edges are scaled in one direction and the center
 * section is scaled in both directions.
 *
 * <pre>
 * +----------+----------------+----------+
 * | unscaled |  <- scaled ->  | unscaled |
 * +----------+----------------+----------+
 * |    ^     |       ^        |    ^     |
 * |  scaled  |  <- scaled ->  |  scaled  |
 * |    v     |       v        |    v     |
 * +----------+----------------+----------+
 * | unscaled |  <- scaled ->  | unscaled |
 * +----------+----------------+----------+
 * </pre>
 * </ul>
 */
public class ImageBackground extends Background
{
    public static final int CENTER_XY = 0;
    public static final int CENTER_X = 1;
    public static final int CENTER_Y = 2;

    public static final int SCALE_XY = 3;
    public static final int SCALE_X = 4;
    public static final int SCALE_Y = 5;

    public static final int TILE_XY = 6;
    public static final int TILE_X = 7;
    public static final int TILE_Y = 8;

    public static final int FRAME_XY = 9;
    public static final int FRAME_X = 10;
    public static final int FRAME_Y = 11;

    /**
     * Creates an image background in the specified mode using the supplied image.
     */
    public ImageBackground (int mode, Image image)
    {
        this(mode, image, null);
    }

    /**
     * Creates an image background in the specified mode using the supplied image and the special
     * frame. This should only be used if one of the framing modes is being used and the supplied
     * frame will be used instead of the default frame which divides the image in thirds.
     */
    public ImageBackground (int mode, Image image, Insets frame)
    {
        _mode = mode;
        _image = image;
        _frame = frame;

        // compute the frame for our framed mode if one was not specially provided
        if (_frame == null && (_mode == FRAME_X || _mode == FRAME_Y || _mode == FRAME_XY)) {
            int twidth = _image.getWidth(), theight = _image.getHeight();
            _frame = new Insets();
            _frame.left = twidth/3;
            _frame.right = twidth/3;
            _frame.top = theight/3;
            _frame.bottom = theight/3;
        }
    }

    // documentation inherited
    public int getMinimumWidth ()
    {
        return (_mode == FRAME_XY || _mode == FRAME_X) ?
            (_frame.left + _frame.right) : _image.getWidth();
    }

    /**
     * Returns the minimum height allowed by this background.
     */
    public int getMinimumHeight ()
    {
        return (_mode == FRAME_XY || _mode == FRAME_Y) ?
            _frame.top + _frame.bottom : _image.getHeight();
    }

    // documentation inherited
    public void render (Renderer renderer, int x, int y, int width, int height, float alpha)
    {
        super.render(renderer, x, y, width, height, alpha);

        switch (_mode/3) {
        case CENTER:
            renderCentered(renderer, x, y, width, height, alpha);
            break;

        case SCALE:
            renderScaled(renderer, x, y, width, height, alpha);
            break;

        case TILE:
            renderTiled(renderer, x, y, width, height, alpha);
            break;

        case FRAME:
            renderFramed(renderer, x, y, width, height, alpha);
            break;
        }
    }

    protected void renderCentered (
        Renderer renderer, int x, int y, int width, int height, float alpha)
    {
        if (_mode == CENTER_X || _mode == CENTER_XY) {
            x += (width-_image.getWidth())/2;
        }
        if (_mode == CENTER_Y || _mode == CENTER_XY) {
            y += (height-_image.getHeight())/2;
        }
        _image.render(renderer, x, y, alpha);
    }

    protected void renderScaled (
        Renderer renderer, int x, int y, int width, int height, float alpha)
    {
        switch (_mode) {
        case SCALE_X:
            y = (height-_image.getHeight())/2;
            height = _image.getHeight();
            break;
        case SCALE_Y:
            x = (width-_image.getWidth())/2;
            width = _image.getWidth();
            break;
        }
        _image.render(renderer, x, y, width, height, alpha);
    }

    protected void renderTiled (
        Renderer renderer, int x, int y, int width, int height, float alpha)
    {
        int iwidth = _image.getWidth(), iheight = _image.getHeight();
        if (_mode == TILE_X) {
            renderRow(renderer, x, y, width, Math.min(height, iheight), alpha);

        } else if (_mode == TILE_Y) {
            int up = height / iheight;
            iwidth = Math.min(width, iwidth);
            for (int yy = 0; yy < up; yy++) {
                _image.render(renderer, 0, 0, iwidth, iheight,
                              x, y + yy*iheight, iwidth, iheight, alpha);
            }
            int remain = height % iheight;
            if (remain > 0) {
                _image.render(renderer, 0, 0, iwidth, remain,
                              x, y + up*iheight, iwidth, remain, alpha);
            }

        } else if (_mode == TILE_XY) {
            int up = height / iheight;
            for (int yy = 0; yy < up; yy++) {
                renderRow(renderer, x, y + yy*iheight, width, iheight, alpha);
            }
            int remain = height % iheight;
            if (remain > 0) {
                renderRow(renderer, x, y + up*iheight, width, remain, alpha);
            }
        }
    }

    protected void renderRow (
        Renderer renderer, int x, int y, int width, int iheight, float alpha)
    {
        int iwidth = _image.getWidth();
        int across = width / iwidth;
        for (int xx = 0; xx < across; xx++) {
            _image.render(renderer, 0, 0, iwidth, iheight,
                          x + xx*iwidth, y, iwidth, iheight, alpha);
        }
        int remain = width % iwidth;
        if (remain > 0) {
            _image.render(renderer, 0, 0, remain, iheight,
                          x + across*iwidth, y, remain, iheight, alpha);
        }
    }

    protected void renderFramed (
        Renderer renderer, int x, int y, int width, int height, float alpha)
    {
        // render each of our image sections appropriately
        int twidth = _image.getWidth(), theight = _image.getHeight();

        // draw the corners
        _image.render(renderer, 0, 0, _frame.left, _frame.bottom, x, y, alpha);
        _image.render(renderer, twidth-_frame.right, 0, _frame.right, _frame.bottom,
                      x+width-_frame.right, y, alpha);
        _image.render(renderer, 0, theight-_frame.top, _frame.left, _frame.top,
                      x, y+height-_frame.top, alpha);
        _image.render(renderer, twidth-_frame.right, theight-_frame.top, _frame.right, _frame.top,
                      x+width-_frame.right, y+height-_frame.top, alpha);

        // draw the "gaps"
        int wmiddle = twidth - _frame.getHorizontal(), hmiddle = theight - _frame.getVertical();
        int gwmiddle = width - _frame.getHorizontal(), ghmiddle = height - _frame.getVertical();
        _image.render(renderer, _frame.left, 0, wmiddle, _frame.bottom,
                      x+_frame.left, y, gwmiddle, _frame.bottom, alpha);
        _image.render(renderer, _frame.left, theight-_frame.top, wmiddle, _frame.top, x+_frame.left,
                      y+height-_frame.top, gwmiddle, _frame.top, alpha);
        _image.render(renderer, 0, _frame.bottom, _frame.left, hmiddle, x, y+_frame.bottom,
                      _frame.left, ghmiddle, alpha);
        _image.render(renderer, twidth-_frame.right, _frame.bottom, _frame.right, hmiddle,
                      x+width-_frame.right, y+_frame.bottom, _frame.right, ghmiddle, alpha);

        // draw the center
        _image.render(renderer, _frame.left, _frame.bottom, wmiddle, hmiddle,
                      x+_frame.left, y+_frame.bottom, gwmiddle, ghmiddle, alpha);
    }

    protected int _mode;
    protected Image _image;
    protected Insets _frame;

    protected static final int CENTER = 0;
    protected static final int SCALE = 1;
    protected static final int TILE = 2;
    protected static final int FRAME = 3;
}
