//
// $Id$

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

import static com.threerings.opengl.gui.Log.*;

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
