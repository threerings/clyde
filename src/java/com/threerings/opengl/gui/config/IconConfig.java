//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.gui.icon.BlankIcon;
import com.threerings.opengl.gui.icon.Icon;
import com.threerings.opengl.gui.icon.ImageIcon;
import com.threerings.opengl.util.GlContext;

/**
 * Contains an icon configuration.
 */
@EditorTypes({ IconConfig.Image.class, IconConfig.Blank.class })
public abstract class IconConfig extends DeepObject
    implements Exportable
{
    /**
     * An image icon.
     */
    public static class Image extends IconConfig
    {
        /** The background image. */
        @Editable(editor="resource", nullable=true)
        @FileConstraints(
            description="m.image_files_desc",
            extensions={".png", ".jpg"},
            directory="image_dir")
        public String file;

        @Override // documentation inherited
        public void getUpdateResources (HashSet<String> paths)
        {
            if (file != null) {
                paths.add(file);
            }
        }

        @Override // documentation inherited
        protected Icon createIcon (GlContext ctx)
        {
            return new ImageIcon(ctx.getImageCache().getImage(file));
        }
    }

    /**
     * A blank icon.
     */
    public static class Blank extends IconConfig
    {
        /** The dimensions of the icon. */
        @Editable(hgroup="d")
        public int width, height;

        @Override // documentation inherited
        protected Icon createIcon (GlContext ctx)
        {
            return new BlankIcon(width, height);
        }
    }

    /**
     * Adds the icon's update resources to the provided set.
     */
    public void getUpdateResources (HashSet<String> paths)
    {
        // nothing by default
    }

    /**
     * Returns the icon corresponding to this config.
     */
    public Icon getIcon (GlContext ctx)
    {
        Icon icon = (_icon == null) ? null : _icon.get();
        if (icon == null) {
            _icon = new SoftReference<Icon>(icon = createIcon(ctx));
        }
        return icon;
    }

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        _icon = null;
    }

    /**
     * Creates the icon corresponding to this config.
     */
    protected abstract Icon createIcon (GlContext ctx);

    /** The cached icon. */
    protected transient SoftReference<Icon> _icon;
}
