//
// $Id$

package com.threerings.opengl.gui.text;

import com.threerings.opengl.gui.TextField;

/**
 * A document for use with a {@link TextField} that limits the input to a
 * maximum length.
 */
public class LengthLimitedDocument extends Document
{
    /**
     * Creates a document that will limit its maximum length to the specified
     * value.
     */
    public LengthLimitedDocument (int maxLength)
    {
        _maxLength = maxLength;
    }

    // documentation inherited
    protected boolean validateEdit (String oldText, String newText)
    {
        return newText.length() <= _maxLength;
    }

    protected int _maxLength;
}
