//
// $Id$

package com.threerings.opengl.gui.config;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.util.GlContext;

/**
 * Describes a user interface style.
 */
public class StyleConfig extends ParameterizedConfig
{
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
    }

    /**
     * An original implementation.
     */
    public static class Original extends Implementation
    {
        /** The foreground color. */
        @Editable(mode="alpha")
        public Color4f color = new Color4f();

        /** The background configuration. */
        @Editable(nullable=true)
        public BackgroundConfig background;

        /** The cursor. */
        @Editable(editor="config", mode="cursor", nullable=true)
        public String cursor;

        /** The font. */
        @Editable(editor="config", mode="font", nullable=true)
        public String font;

        /** The border configuration. */
        @Editable(nullable=true)
        public BorderConfig border;

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
    }

    /** The actual style implementation. */
    @Editable
    public Implementation implementation = new Original();

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }
}
