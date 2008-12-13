//
// $Id$

package com.threerings.opengl.gui.config;

import java.lang.ref.SoftReference;

import java.util.HashSet;

import com.threerings.config.ManagedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.FileConstraints;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.gui.Cursor;
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
                ctx.getImageCache().getBufferedImage(image), hotSpotX, hotSpotY));
        }
        return cursor;
    }

    @Override // documentation inherited
    protected void fireConfigUpdated ()
    {
        // invalidate
        _cursor = null;
        super.fireConfigUpdated();
    }

    @Override // documentation inherited
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
