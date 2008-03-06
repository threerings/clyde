//
// $Id$

package com.threerings.opengl.gui;

/**
 * A derivation of {@link TextField} that does not display the actual
 * text, but asterisks instead.
 */
public class PasswordField extends TextField
{
    public PasswordField ()
    {
    }

    public PasswordField (int maxLength)
    {
        super(maxLength);
    }

    public PasswordField (String text)
    {
        super(text);
    }

    public PasswordField (String text, int maxLength)
    {
        super(text, maxLength);
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
