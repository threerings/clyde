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

package com.threerings.opengl.gui;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import static com.threerings.opengl.gui.Log.log;

/**
 * Contains a cursor.
 */
public class Cursor
{
    /**
     * Creates a new cursor.
     */
    public Cursor (BufferedImage image, int hx, int hy)
    {
        _image = image;
        _hx = hx;
        _hy = hy;
    }

    /**
     * Retrieve the lazily-initialized LWJGL cursor.
     */
    public org.lwjgl.input.Cursor getLWJGLCursor ()
    {
        if (_lwjglCursor == null) {
            _lwjglCursor = createLWJGLCursor();
        }
        return _lwjglCursor;
    }

    /**
     * Retrieves the lazily-initialized AWT cursor using the supplied toolkit.
     */
    public java.awt.Cursor getAWTCursor (Toolkit toolkit)
    {
        if (_awtCursor == null) {
            _awtCursor = createAWTCursor(toolkit);
        }
        return _awtCursor;
    }

    /**
     * Display the LWJGL cursor.
     */
    public void show ()
    {
        if (!Display.isCreated()) {
            return;
        }
        if (!Mouse.isCreated()) {
            try {
                Mouse.create();
            } catch (Throwable t) {
                log.warning("Problem creating mouse.", t);
                return;
            }
        }
        org.lwjgl.input.Cursor cursor = getLWJGLCursor();
        if (Mouse.getNativeCursor() != cursor) {
            try {
                Mouse.setNativeCursor(cursor);
            } catch (Throwable t) {
                log.warning("Problem updating mouse cursor.", t);
            }
        }
    }

    /**
     * Creates an LWJGL cursor from the configured image and hotspot.
     */
    protected org.lwjgl.input.Cursor createLWJGLCursor ()
    {
        int ww = _image.getWidth();
        int hh = _image.getHeight();
        IntBuffer data = ByteBuffer.allocateDirect(ww*hh*4).asIntBuffer();
        for (int yy = hh - 1; yy >= 0; yy--) {
            for (int xx = 0; xx < ww; xx++) {
                data.put(_image.getRGB(xx, yy));
            }
        }
        data.flip();
        try {
            return new org.lwjgl.input.Cursor(ww, hh, _hx, hh - _hy - 1, 1, data, null);
        } catch (LWJGLException e) {
            System.err.println("Unable to create cursor: " + e);
            return null;
        }
    }

    /**
     * Creates an AWT cursor from the configured image and hotspot.
     */
    protected java.awt.Cursor createAWTCursor (Toolkit toolkit)
    {
        int width = _image.getWidth(), height = _image.getHeight();
        Dimension size = toolkit.getBestCursorSize(width, height);
        Image image = _image;
        int hx = _hx, hy = _hy;
        if (size.width != width || size.height != height) {
            // resize the image and adjust the hotspot
            image = _image.getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH);
            hx = (_hx * size.width) / width;
            hy = (_hy * size.height) / height;
        }
        return toolkit.createCustomCursor(image, new Point(hx, hy), "cursor");
    }

    protected BufferedImage _image;
    protected int _hx, _hy;

    protected org.lwjgl.input.Cursor _lwjglCursor;
    protected java.awt.Cursor _awtCursor;
}
