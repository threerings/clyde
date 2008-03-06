//
// $Id$

package com.threerings.opengl.gui.text;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.gui.UIConstants;

/**
 * Creates instances of {@link Text} using a particular technology and a particular font
 * configuration.
 */
public abstract class TextFactory
    implements UIConstants
{
    /**
     * Returns the height of our text.
     */
    public abstract int getHeight ();

    /**
     * Creates a text instance using our the font configuration associated with this text factory
     * and the foreground color specified.
     */
    public Text createText (String text, Color4f color)
    {
        return createText(text, color, NORMAL, DEFAULT_SIZE, null, false);
    }

    /**
     * Creates a text instance using our the font configuration associated with this text factory
     * and the foreground color, text effect and text effect color specified.
     *
     * @param useAdvance if true, the advance to the next insertion point will be included in the
     * bounds of the created text (this is needed by editable text displays).
     */
    public abstract Text createText (String text, Color4f color, int effect, int effectSize,
                                     Color4f effectColor, boolean useAdvance);

    /**
     * Wraps a string into a set of text objects that do not exceed the specified width.
     */
    public abstract Text[] wrapText (String text, Color4f color, int effect, int effectSize,
                                     Color4f effectColor, int maxWidth);
}
