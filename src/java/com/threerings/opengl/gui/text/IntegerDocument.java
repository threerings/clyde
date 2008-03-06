//
// $Id$

package com.threerings.opengl.gui.text;

import com.threerings.opengl.gui.TextField;

/**
 * A document for use with a {@link TextField} that allows only integer
 * numeric input. <em>Note:</em> to allow fully valid values to be entered one
 * character at a time, partially valid values (like the string '-' at position
 * zero) must be allowed. Thus one cannot rely on the integer document only
 * ever containing valid integers.
 */
public class IntegerDocument extends Document
{
    /**
     * Creates a new document that allows any integer value.
     */
    public IntegerDocument ()
    {
        this(false);
    }

    /**
     * Creates a new integer document.
     *
     * @param positive if true, only accept positive values
     */
    public IntegerDocument (boolean positive)
    {
        _positive = positive;
    }

    // documentation inherited
    protected boolean validateEdit (String oldText, String newText)
    {
        // some special cases
        if (newText.length() == 0) {
            return true;
        }
        if (newText.startsWith("-") && _positive) {
            return false;
        }
        if (newText.equals("-")) {
            return true;
        }
        try {
            Integer.parseInt(newText);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** If true, only positive values are allowed. */
    protected boolean _positive;
}
