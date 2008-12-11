//
// $Id$

package com.threerings.opengl.gui;

import com.threerings.opengl.util.GlContext;

/**
 * A derivation of {@link TextField} that does not display the actual
 * text, but asterisks instead.
 */
public class PasswordField extends TextField
{
    public PasswordField (GlContext ctx)
    {
        super(ctx);
    }

    public PasswordField (GlContext ctx, int maxLength)
    {
        super(ctx, maxLength);
    }

    public PasswordField (GlContext ctx, String text)
    {
        super(ctx, text);
    }

    public PasswordField (GlContext ctx, String text, int maxLength)
    {
        super(ctx, text, maxLength);
    }

    // documentation inherited
    protected String getDisplayText ()
    {
        String text = super.getDisplayText();
        if (text == null) {
            return null;
        } else if (_stars == null || _stars.length() != text.length()) {
            StringBuffer stars = new StringBuffer();
            for (int ii = 0; ii < text.length(); ii++) {
                stars.append("*");
            }
            _stars = stars.toString();
        }
        return _stars;
    }

    protected String _stars;
}
