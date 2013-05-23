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

import com.threerings.media.image.Colorization;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.gui.icon.BlankIcon;
import com.threerings.opengl.gui.icon.Icon;
import com.threerings.opengl.gui.icon.ImageIcon;
import com.threerings.opengl.gui.icon.RotatedIcon;
import com.threerings.opengl.renderer.config.ColorizationConfig;
import com.threerings.opengl.util.GlContext;

/**
 * Contains an icon configuration.
 */
@EditorTypes({
    IconConfig.Image.class, IconConfig.ColorizedImage.class,
    IconConfig.Rotated.class, IconConfig.Blank.class })
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
            extensions={".png", ".jpg", ".dds"},
            directory="image_dir")
        public String file;

        /**
         * Retrieves the image for the icon.
         */
        public com.threerings.opengl.gui.Image getImage (GlContext ctx)
        {
            return ctx.getImageCache().getImage(file);
        }

        @Override
        public void getUpdateResources (HashSet<String> paths)
        {
            if (file != null) {
                paths.add(file);
            }
        }

        @Override
        protected Icon createIcon (GlContext ctx)
        {
            return (file == null) ? new BlankIcon(1, 1) : new ImageIcon(getImage(ctx));
        }
    }

    /**
     * A colorized image icon.
     */
    public static class ColorizedImage extends Image
    {
        /** The colorizations to apply to the image. */
        @Editable
        public ColorizationConfig[] colorizations = new ColorizationConfig[0];

        @Override
        public com.threerings.opengl.gui.Image getImage (GlContext ctx)
        {
            return getImage(ctx, file, colorizations);
        }
    }

    /**
     * A rotated icon.
     */
    public static class Rotated extends IconConfig
    {
        /** The amount of rotation. */
        @Editable(min=-180, max=+180)
        public float rotation;

        /** The sub-icon to rotate. */
        @Editable(nullable=true)
        public IconConfig icon;

        @Override
        protected Icon createIcon (GlContext ctx)
        {
            return (icon == null) ? new BlankIcon(1, 1) :
                new RotatedIcon(icon.createIcon(ctx), rotation);
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

        @Override
        protected Icon createIcon (GlContext ctx)
        {
            return new BlankIcon(width, height);
        }
    }

    /**
     * Retrieves the specified image, applying the desired colorizations.
     */
    public static com.threerings.opengl.gui.Image getImage (
        GlContext ctx, String file, ColorizationConfig[] colorizations)
    {
        Colorization[] zations = new Colorization[colorizations.length];
        for (int ii = 0; ii < zations.length; ii++) {
            zations[ii] = colorizations[ii].getColorization(ctx);
        }
        return ctx.getImageCache().getImage(file, zations);
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
    @DeepOmit
    protected transient SoftReference<Icon> _icon;
}
