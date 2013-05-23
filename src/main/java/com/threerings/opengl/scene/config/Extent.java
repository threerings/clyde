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

        @Override
        public void transformBounds (Transform3D transform, Box result)
        {
            float hx = sizeX * 0.5f, hy = sizeY * 0.5f, hz = sizeZ * 0.5f;
            result.getMinimumExtent().set(-hx, -hy, -hz);
            result.getMaximumExtent().set(+hx, +hy, +hz);
            result.transformLocal(transform);
        }
    }

    /**
     * Unlimited extent.
     */
    public static class Unlimited extends Extent
    {
        @Override
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
