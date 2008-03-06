//
// $Id$

package com.threerings.opengl.gui.text;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.gui.util.Dimension;

/**
 * Contains a "run" of text.  Specializations of this class render text in different ways, for
 * example using JME's internal bitmapped font support or by using the AWT to render the run of
 * text to an image and texturing a quad with that entire image.
 */
public abstract class Text
{
    /**
     * Returns the length in characters of this text.
     */
    public abstract int getLength ();

    /**
     * Returns the screen dimensions of this text.
     */
    public abstract Dimension getSize ();

    /**
     * Returns the character position to which the cursor should be moved given that the user
     * clicked the specified coordinate (relative to the text's bounds).
     */
    public abstract int getHitPos (int x, int y);

    /**
     * Returns the x position for the cursor at the specified character index. Note that the
     * position should be "before" that character.
     */
    public abstract int getCursorPos (int index);

    /**
     * Renders this text to the display.
     */
    public abstract void render (Renderer renderer, int x, int y, float alpha);

    /**
     * Optional rendering this text scaled to a certain height/width.
     */
    public void render (Renderer renderer, int x, int y, int w, int h, float alpha)
    {
        render(renderer, x, y, alpha);
    }
}
