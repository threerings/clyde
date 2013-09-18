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
