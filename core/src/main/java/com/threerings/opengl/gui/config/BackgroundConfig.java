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

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.gui.background.Background;
import com.threerings.opengl.gui.background.BlankBackground;
import com.threerings.opengl.gui.background.ImageBackground;
import com.threerings.opengl.gui.background.TintedBackground;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.config.ColorizationConfig;
import com.threerings.opengl.util.GlContext;

/**
 * Contains a background configuration.
 */
@EditorTypes({
    BackgroundConfig.Solid.class, BackgroundConfig.Image.class,
    BackgroundConfig.ColorizedImage.class, BackgroundConfig.Blank.class })
public abstract class BackgroundConfig extends DeepObject
    implements Exportable
{
    /**
     * A solid background.
     */
    public static class Solid extends BackgroundConfig
    {
        /** The color of the background. */
        @Editable(mode="alpha")
        public Color4f color = new Color4f(Color4f.BLACK);

        @Override
        protected Background createBackground (GlContext ctx)
        {
            return new TintedBackground(color);
        }
    }

    /**
     * An image background.
     */
    public static class Image extends BackgroundConfig
    {
        /** The various image modes. */
        public enum Mode
        {
            CENTER_XY(ImageBackground.CENTER_XY),
            CENTER_X(ImageBackground.CENTER_X),
            CENTER_Y(ImageBackground.CENTER_Y),

            SCALE_XY(ImageBackground.SCALE_XY),
            SCALE_X(ImageBackground.SCALE_X),
            SCALE_Y(ImageBackground.SCALE_Y),

            TILE_XY(ImageBackground.TILE_XY),
            TILE_X(ImageBackground.TILE_X),
            TILE_Y(ImageBackground.TILE_Y),

            FRAME_XY(ImageBackground.FRAME_XY, true),
            FRAME_X(ImageBackground.FRAME_X, true),
            FRAME_Y(ImageBackground.FRAME_Y, true),

            ASPECT_INNER(ImageBackground.ASPECT_INNER),
            ASPECT_OUTER(ImageBackground.ASPECT_OUTER);

            /**
             * Returns the corresponding {@link ImageBackground} constant.
             */
            public int getConstant ()
            {
                return _constant;
            }

            /**
             * Determines whether this is one of the framing modes.
             */
            public boolean isFrame ()
            {
                return _frame;
            }

            Mode (int constant)
            {
                this(constant, false);
            }

            Mode (int constant, boolean frame)
            {
                _constant = constant;
                _frame = frame;
            }

            /** The corresponding {@link ImageBackground} constant. */
            protected final int _constant;

            /** Whether this is one of the frame modes. */
            protected final boolean _frame;
        }

        /** The various tile anchors. */
        public enum Anchor {
            LOWER_LEFT(ImageBackground.ANCHOR_LL),
            LOWER_RIGHT(ImageBackground.ANCHOR_LR),
            UPPER_LEFT(ImageBackground.ANCHOR_UL),
            UPPER_RIGHT(ImageBackground.ANCHOR_UR);

            /**
             * Returns the corresponding {@link ImageBackground} constant.
             */
            public int getConstant ()
            {
                return _constant;
            }

            Anchor (int constant)
            {
                _constant = constant;
            }

            /** The corresponding {@link ImageBackground} constant. */
            protected final int _constant;
        }

        /** The background image. */
        @Editable(editor="resource", nullable=true, hgroup="f")
        @FileConstraints(
            description="m.image_files_desc",
            extensions={".png", ".jpg", ".dds"},
            directory="image_dir")
        public String file;

        /** The image mode. */
        @Editable(hgroup="m")
        public Mode mode = Mode.SCALE_XY;

        /** The image anchor. */
        @Editable(hgroup="m")
        public Anchor anchor = Anchor.LOWER_LEFT;

        /** The image frame. */
        @Editable(nullable=true)
        public InsetsConfig frame;

        @Override
        public void getUpdateResources (HashSet<String> paths)
        {
            if (file != null) {
                paths.add(file);
            }
        }

        @Override
        protected Background createBackground (GlContext ctx)
        {
            return (file == null) ? new BlankBackground() :
                new ImageBackground(mode.getConstant(), getImage(ctx),
                    (frame != null && mode.isFrame()) ? frame.createInsets() : null,
                    anchor.getConstant());
        }

        /**
         * Retrieves the image for the background.
         */
        protected com.threerings.opengl.gui.Image getImage (GlContext ctx)
        {
            return ctx.getImageCache().getImage(file);
        }
    }

    /**
     * A colorized image.
     */
    public static class ColorizedImage extends Image
    {
        /** The colorizations to apply to the image. */
        @Editable
        public ColorizationConfig[] colorizations = new ColorizationConfig[0];

        @Override
        protected com.threerings.opengl.gui.Image getImage (GlContext ctx)
        {
            return IconConfig.getImage(ctx, file, colorizations);
        }
    }

    /**
     * A blank background.
     */
    public static class Blank extends BackgroundConfig
    {
        @Override
        protected Background createBackground (GlContext ctx)
        {
            return new BlankBackground();
        }
    }

    /**
     * Adds the background's update resources to the provided set.
     */
    public void getUpdateResources (HashSet<String> paths)
    {
        // nothing by default
    }

    /**
     * Returns the background corresponding to this config.
     */
    public Background getBackground (GlContext ctx)
    {
        Background background = (_background == null) ? null : _background.get();
        if (background == null) {
            _background = new SoftReference<Background>(background = createBackground(ctx));
        }
        return background;
    }

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        _background = null;
    }

    /**
     * Creates the background corresponding to this config.
     */
    protected abstract Background createBackground (GlContext ctx);

    /** The cached background. */
    @DeepOmit
    protected transient SoftReference<Background> _background;
}
