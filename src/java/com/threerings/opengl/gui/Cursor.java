//
// $Id$

package com.threerings.opengl.gui;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/**
 * Contains a cursor.
 */
public class Cursor
{
    /**
     * Create a cursor from a BufferedImage.
     */
    public static org.lwjgl.input.Cursor createCursor (BufferedImage image, int hx, int hy)
    {
        int ww = image.getWidth();
        int hh = image.getHeight();
        IntBuffer data = ByteBuffer.allocateDirect(ww*hh*4).asIntBuffer();
        for (int yy = hh - 1; yy >= 0; yy--) {
            for (int xx = 0; xx < ww; xx++) {
                data.put(image.getRGB(xx, yy));
            }
        }
        data.flip();
        try {
            return new org.lwjgl.input.Cursor(ww, hh, hx, hh - hy - 1, 1, data, null);
        } catch (LWJGLException e) {
            System.err.println("Unable to create cursor: " + e);
            return null;
        }
    }

    public Cursor (org.lwjgl.input.Cursor cursor)
    {
        setCursor(cursor);
    }

    public Cursor (BufferedImage image, int hx, int hy)
    {
        setCursor(image, hx, hy);
    }

    /**
     * Set the cursor to the specified value.
     */
    public void setCursor (org.lwjgl.input.Cursor cursor)
    {
        _cursor = cursor;
        _image = null;
    }

    /**
     * Create a cursor from the image with the supplied hotspot and use it.
     */
    public void setCursor (BufferedImage image, int hx, int hy)
    {
        if (Mouse.isCreated()) {
            setCursor(createCursor(image, hx, hy));
        } else {
            _image = image;
            _hx = hx;
            _hy = hy;
        }
    }

    /**
     * Retrieve the cursor.
     */
    public org.lwjgl.input.Cursor getCursor ()
    {
        return _cursor;
    }

    /**
     * Display this cursor.
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
                Log.log.log(Level.WARNING, "Problem creating mouse", t);
                return;
            }
        }
        if (_image != null) {
            setCursor(_image, _hx, _hy);
        }
        if (Mouse.getNativeCursor() != _cursor) {
            try {
                Mouse.setNativeCursor(_cursor);
            } catch (Throwable t) {
                Log.log.log(Level.WARNING, "Problem updating mouse cursor: ", t);
            }
        }
    }

    protected org.lwjgl.input.Cursor _cursor;

    protected BufferedImage _image;
    protected int _hx, _hy;
}
