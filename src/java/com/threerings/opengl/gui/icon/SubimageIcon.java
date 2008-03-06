//
// $Id$

package com.threerings.opengl.gui.icon;

import com.threerings.opengl.renderer.Renderer;

import com.threerings.opengl.gui.Image;
import com.threerings.opengl.gui.util.Rectangle;

/**
 * Displays a region of an image as an icon.
 */
public class SubimageIcon extends Icon
{
    /**
     * Creates an icon that will display the specified region of the supplied
     * image.
     */
    public SubimageIcon (Image image, int x, int y, int width, int height)
    {
        _region = new Rectangle(x, y, width, height);
        _image = image;
    }

    // documentation inherited
    public int getWidth ()
    {
        return _region.width;
    }

    // documentation inherited
    public int getHeight ()
    {
        return _region.height;
    }

    // documentation inherited
    public void render (Renderer renderer, int x, int y, float alpha)
    {
        super.render(renderer, x, y, alpha);
        _image.render(renderer, _region.x, _region.y,
                      _region.width, _region.height, x, y, alpha);
    }

    protected Image _image;
    protected Rectangle _region;
}
