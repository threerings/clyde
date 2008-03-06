//
// $Id$

package com.threerings.opengl.gui.icon;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.gui.Image;

/**
 * Displays an image as an icon.
 */
public class ImageIcon extends Icon
{
    /**
     * Creates an icon from the supplied source image.
     */
    public ImageIcon (Image image)
    {
        _image = image;
    }

    /**
     * Converts the supplied AWT icon into a BUI icon.
     */
    public ImageIcon (javax.swing.Icon icon)
    {
        BufferedImage cached = new BufferedImage(
            icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D gfx = cached.createGraphics();
        try {
            icon.paintIcon(null, gfx, 0, 0);
            _image = new Image(cached);
        } finally {
            gfx.dispose();
        }
    }

    // documentation inherited
    public int getWidth ()
    {
        return _image.getWidth();
    }

    // documentation inherited
    public int getHeight ()
    {
        return _image.getHeight();
    }

    // documentation inherited
    public void render (Renderer renderer, int x, int y, float alpha)
    {
        super.render(renderer, x, y, alpha);
        _image.render(renderer, x, y, alpha);
    }

    protected Image _image;
}
