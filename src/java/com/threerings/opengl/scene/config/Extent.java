//
// $Id$

package com.threerings.opengl.scene.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.math.Box;
import com.threerings.math.Transform3D;
import com.threerings.util.DeepObject;

/**
 * Represents the extent of an influence or effect.
 */
@EditorTypes({ Extent.Limited.class, Extent.Unlimited.class })
public abstract class Extent extends DeepObject
    implements Exportable
{
    /**
     * Limited extent.
     */
    public static class Limited extends Extent
    {
        /** The size in the x direction. */
        @Editable(min=0, step=0.01, hgroup="s")
        public float sizeX = 1f;

        /** The size in the y direction. */
        @Editable(min=0, step=0.01, hgroup="s")
        public float sizeY = 1f;

        /** The size in the z direction. */
        @Editable(min=0, step=0.01, hgroup="s")
        public float sizeZ = 1f;

        @Override // documentation inherited
        public void transformBounds (Transform3D transform, Box result)
        {
            result.getMinimumExtent().set(-sizeX, -sizeY, -sizeZ);
            result.getMaximumExtent().set(+sizeX, +sizeY, +sizeZ);
            result.transformLocal(transform);
        }
    }

    /**
     * Unlimited extent.
     */
    public static class Unlimited extends Extent
    {
        @Override // documentation inherited
        public void transformBounds (Transform3D transform, Box result)
        {
            result.set(Box.MAX_VALUE);
        }
    }

    /**
     * Retrieves the bounds of the extent under the specified transform.
     */
    public abstract void transformBounds (Transform3D transform, Box result);
}
