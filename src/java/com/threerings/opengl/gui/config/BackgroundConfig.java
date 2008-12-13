//
// $Id$

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
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.util.GlContext;

/**
 * Contains a background configuration.
 */
@EditorTypes({
    BackgroundConfig.Solid.class, BackgroundConfig.Image.class, BackgroundConfig.Blank.class })
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

        @Override // documentation inherited
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
            FRAME_Y(ImageBackground.FRAME_Y, true);

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
            protected int _constant;

            /** Whether this is one of the frame modes. */
            protected boolean _frame;
        }

        /** The background image. */
        @Editable(editor="resource", nullable=true, hgroup="f")
        @FileConstraints(
            description="m.image_files_desc",
            extensions={".png", ".jpg"},
            directory="image_dir")
        public String file;

        /** The image mode. */
        @Editable(hgroup="f")
        public Mode mode = Mode.SCALE_XY;

        /** The image frame. */
        @Editable(nullable=true)
        public InsetsConfig frame;

        @Override // documentation inherited
        public void getUpdateResources (HashSet<String> paths)
        {
            if (file != null) {
                paths.add(file);
            }
        }

        @Override // documentation inherited
        protected Background createBackground (GlContext ctx)
        {
            return new ImageBackground(mode.getConstant(), ctx.getImageCache().getImage(file),
                (frame != null && mode.isFrame()) ? frame.createInsets() : null);
        }
    }

    /**
     * A blank background.
     */
    public static class Blank extends BackgroundConfig
    {
        @Override // documentation inherited
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
