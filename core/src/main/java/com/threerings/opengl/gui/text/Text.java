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
