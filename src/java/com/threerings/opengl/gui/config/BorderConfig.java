//
// $Id$

package com.threerings.opengl.gui.config;

import java.lang.ref.SoftReference;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

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

        @Override // documentation inherited
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
        @Override // documentation inherited
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
    protected SoftReference<Border> _border;
}
