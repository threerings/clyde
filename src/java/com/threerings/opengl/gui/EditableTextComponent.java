//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

package com.threerings.opengl.gui;

import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.config.StyleConfig;

/**
 * Extends TextComponent with mechanisms shared by editable text Components.
 */
public abstract class EditableTextComponent extends TextComponent
{
    /**
     * Set the cursor to use in an editable text component to hint that the text is editable.
     */
    public static void setTextCursor (Cursor textCursor)
    {
        _textCursor = textCursor;
    }

    /**
     * For subclasses.
     */
    protected EditableTextComponent (GlContext ctx)
    {
        super(ctx);
    }

    @Override
    protected void updateFromStyleConfig (int state, StyleConfig.Original config)
    {
        super.updateFromStyleConfig(state, config);

        // utilize the text cursor if none other defined
        if ((state == DEFAULT) && (_cursor == null)) {
            _cursor = _textCursor;
        }
    }

    /** A Cursor to use in any editable text component unless otherwise overridden. */
    protected static Cursor _textCursor;
}
