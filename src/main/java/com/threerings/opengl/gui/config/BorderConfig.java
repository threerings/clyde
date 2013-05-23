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

package com.threerings.opengl.gui.config;

import java.lang.ref.SoftReference;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.gui.border.Border;
import com.threerings.opengl.gui.border.EmptyBorder;
import com.threerings.opengl.gui.border.LineBorder;
import com.threerings.opengl.renderer.Color4f;

/**
 * Contains a border configuration.
 */
@EditorTypes({ BorderConfig.Solid.class, BorderConfig.Blank.class })
public abstract class BorderConfig extends DeepObject
    implements Exportable
{
    /**
     * A solid border.
     */
    public static class Solid extends BorderConfig
    {
        /** The color of the border. */
        @Editable(mode="alpha", hgroup="t")
        public Color4f color = new Color4f();

        @Override
        protected Border createBorder ()
        {
            return new LineBorder(color, thickness);
        }
    }

    /**
     * A blank border.
     */
    public static class Blank extends BorderConfig
    {
        @Override
        protected Border createBorder ()
        {
            return new EmptyBorder(thickness, thickness, thickness, thickness);
        }
    }

    /** The thickness of the border. */
    @Editable(hgroup="t")
    public int thickness = 1;

    /**
     * Returns the border corresponding to this config.
     */
    public Border getBorder ()
    {
        Border border = (_border == null) ? null : _border.get();
        if (border == null) {
            _border = new SoftReference<Border>(border = createBorder());
        }
        return border;
    }

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        _border = null;
    }

    /**
     * Creates the border corresponding to this config.
     */
    protected abstract Border createBorder ();

    /** The cached border. */
    @DeepOmit
    protected transient SoftReference<Border> _border;
}
