//
// $Id$

package com.threerings.opengl.gui.config;

import java.util.HashSet;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.gui.UIConstants;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.util.GlContext;

/**
 * Describes a user interface style.
 */
public class StyleConfig extends ParameterizedConfig
{
    /** Used when we can't resolve the tile's underlying original implementation. */
    public static final Original NULL_ORIGINAL = new Original();

    /** Text alignment modes. */
    public enum TextAlignment
    {
        LEFT(UIConstants.LEFT),
        CENTER(UIConstants.CENTER),
        RIGHT(UIConstants.RIGHT);

        /**
         * Returns the corresponding UI constant.
         */
        public int getConstant ()
        {
            return _constant;
        }

        TextAlignment (int constant)
        {
            _constant = constant;
        }

        /** The UI constant. */
        protected int _constant;
    }

    /** Vertical alignment modes. */
    public enum VerticalAlignment
    {
        TOP(UIConstants.TOP),
        CENTER(UIConstants.CENTER),
        BOTTOM(UIConstants.BOTTOM);

        /**
         * Returns the corresponding UI constant.
         */
        public int getConstant ()
        {
            return _constant;
        }

        VerticalAlignment (int constant)
        {
            _constant = constant;
        }

        /** The UI constant. */
        protected int _constant;
    }

    /** Text effects. */
    public enum TextEffect
    {
        NONE(UIConstants.NORMAL),
        OUTLINE(UIConstants.OUTLINE),
        SHADOW(UIConstants.SHADOW),
        PLAIN(UIConstants.PLAIN),
        GLOW(UIConstants.GLOW);

        /**
         * Returns the corresponding UI constant.
         */
        public int getConstant ()
        {
            return _constant;
        }

        TextEffect (int constant)
        {
            _constant = constant;
        }

        /** The UI constant. */
        protected int _constant;
    }

    /**
     * Contains the actual implementation of the style.
     */
    @EditorTypes({ Original.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Adds the implementation's update references to the provided set.
         */
        public abstract void getUpdateReferences (ConfigReferenceSet refs);

        /**
         * Adds the implementation's update resources to the provided set.
         */
        public void getUpdateResources (HashSet<String> paths)
        {
            // nothing by default
        }

        /**
         * Returns a reference to the config's underlying original implementation.
         */
        public abstract Original getOriginal (GlContext ctx);

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            // nothing by default
        }
    }

    /**
     * An original implementation.
     */
    public static class Original extends Implementation
    {
        /** The foreground color. */
        @Editable(mode="alpha", hgroup="c")
        public Color4f color = new Color4f();

        /** The cursor. */
        @Editable(editor="config", mode="cursor", nullable=true, hgroup="c")
        public String cursor;

        /** The font. */
        @Editable(editor="config", mode="font", nullable=true, hgroup="f")
        public String font;

        /** The font style. */
        @Editable(hgroup="f")
        public FontConfig.Style fontStyle = FontConfig.Style.PLAIN;

        /** The font size. */
        @Editable(min=1, hgroup="f")
        public int fontSize = 12;

        /** The text effect. */
        @Editable(hgroup="e")
        public TextEffect textEffect = TextEffect.NONE;

        /** The effect size. */
        @Editable(hgroup="e")
        public int effectSize = UIConstants.DEFAULT_SIZE;

        /** The effect color. */
        @Editable(mode="alpha", hgroup="e")
        public Color4f effectColor = new Color4f();

        /** The text alignment. */
        @Editable(hgroup="a")
        public TextAlignment textAlignment = TextAlignment.LEFT;

        /** The vertical alignment .*/
        @Editable(hgroup="a")
        public VerticalAlignment verticalAlignment = VerticalAlignment.CENTER;

        /** The line spacing. */
        @Editable(hgroup="a")
        public int lineSpacing = UIConstants.DEFAULT_SPACING;

        /** The padding. */
        @Editable
        public InsetsConfig padding = new InsetsConfig();

        /** The background configuration. */
        @Editable(nullable=true)
        public BackgroundConfig background;

        /** The icon configuration. */
        @Editable(nullable=true)
        public IconConfig icon;

        /** The border configuration. */
        @Editable(nullable=true)
        public BorderConfig border;

        /** The preferred size. */
        @Editable(nullable=true)
        public DimensionConfig size;

        /** The tooltip style. */
        @Editable(nullable=true)
        public ConfigReference<StyleConfig> tooltipStyle;

        /** The selection background. */
        @Editable(nullable=true)
        public BackgroundConfig selectionBackground;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            if (cursor != null) {
                refs.add(CursorConfig.class, cursor);
            }
            if (font != null) {
                refs.add(FontConfig.class, font);
            }
        }

        @Override // documentation inherited
        public void getUpdateResources (HashSet<String> paths)
        {
            if (background != null) {
                background.getUpdateResources(paths);
            }
            if (icon != null) {
                icon.getUpdateResources(paths);
            }
            if (selectionBackground != null) {
                selectionBackground.getUpdateResources(paths);
            }
        }

        @Override // documentation inherited
        public Original getOriginal (GlContext ctx)
        {
            return this;
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            if (background != null) {
                background.invalidate();
            }
            if (icon != null) {
                icon.invalidate();
            }
            if (border != null) {
                border.invalidate();
            }
            if (selectionBackground != null) {
                selectionBackground.invalidate();
            }
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The style reference. */
        @Editable(nullable=true)
        public ConfigReference<StyleConfig> style;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(StyleConfig.class, style);
        }

        @Override // documentation inherited
        public Original getOriginal (GlContext ctx)
        {
            StyleConfig config = ctx.getConfigManager().getConfig(StyleConfig.class, style);
            return (config == null) ? null : config.getOriginal(ctx);
        }
    }

    /** The actual style implementation. */
    @Editable
    public Implementation implementation = new Original();

    /**
     * Returns a reference to the config's underlying original implementation.
     */
    public Original getOriginal (GlContext ctx)
    {
        return implementation.getOriginal(ctx);
    }

    @Override // documentation inherited
    protected void fireConfigUpdated ()
    {
        // invalidate the implementation
        implementation.invalidate();
        super.fireConfigUpdated();
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }

    @Override // documentation inherited
    protected void getUpdateResources (HashSet<String> paths)
    {
        implementation.getUpdateResources(paths);
    }
}
