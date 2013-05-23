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

import java.util.HashSet;

import com.threerings.config.ManagedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.FileConstraints;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.gui.Cursor;
import com.threerings.opengl.renderer.config.ColorizationConfig;
import com.threerings.opengl.renderer.config.TextureConfig;
import com.threerings.opengl.util.GlContext;

/**
 * Describes a cursor.
 */
public class CursorConfig extends ManagedConfig
{
    /** The cursor image. */
    @Editable(editor="resource", nullable=true)
    @FileConstraints(
        description="m.image_files_desc",
        extensions={".png", ".jpg"},
        directory="image_dir")
    public String image;

    /** Colorizations to apply to the cursor. */
    @Editable
    public ColorizationConfig[] colorizations = new ColorizationConfig[0];

    /** The hot spot x coordinate. */
    @Editable(min=0, hgroup="h")
    public int hotSpotX;

    /** The hot spot y coordinate. */
    @Editable(min=0, hgroup="h")
    public int hotSpotY;

    /**
     * Returns the cursor corresponding to this config.
     */
    public Cursor getCursor (GlContext ctx)
    {
        Cursor cursor = (_cursor == null) ? null : _cursor.get();
        if (cursor == null) {
            _cursor = new SoftReference<Cursor>(cursor = new Cursor(
                TextureConfig.getImage(ctx, image, colorizations), hotSpotX, hotSpotY));
        }
        return cursor;
    }

    @Override
    protected void fireConfigUpdated ()
    {
        // invalidate
        _cursor = null;
        super.fireConfigUpdated();
    }

    @Override
    protected void getUpdateResources (HashSet<String> paths)
    {
        if (image != null) {
            paths.add(image);
        }
    }

    /** The cached cursor. */
    @DeepOmit
    protected transient SoftReference<Cursor> _cursor;
}
