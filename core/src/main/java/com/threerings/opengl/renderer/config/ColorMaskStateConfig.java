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

package com.threerings.opengl.renderer.config;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.state.ColorMaskState;

/**
 * Configurable color mask state.
 */
public class ColorMaskStateConfig extends DeepObject
    implements Exportable
{
    /** Whether to write to the red channel. */
    @Editable(hgroup="m")
    public boolean red = true;

    /** Whether to write to the green channel. */
    @Editable(hgroup="m")
    public boolean green = true;

    /** Whether to write to the blue channel. */
    @Editable(hgroup="m")
    public boolean blue = true;

    /** Whether to write to the alpha channel. */
    @Editable(hgroup="m")
    public boolean alpha = true;

    /** If true, do not use a shared instance. */
    @Editable
    public boolean uniqueInstance;

    /**
     * Returns the corresponding color mask state.
     */
    public ColorMaskState getState ()
    {
        return uniqueInstance ?
            new ColorMaskState(red, green, blue, alpha) :
            ColorMaskState.getInstance(red, green, blue, alpha);
    }
}
