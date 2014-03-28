//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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

    @Override
    public Object getValue ()
    {
        try {
            return Integer.valueOf(getText());

        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // documentation inherited
    protected boolean validateEdit (String oldText, String newText)
    {
        // some special cases
        if (newText.isEmpty()) {
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
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** If true, only positive values are allowed. */
    protected boolean _positive;
}
