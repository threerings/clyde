//
// $Id$

package com.threerings.opengl.material;

import com.threerings.math.Matrix4f;

/**
 * Extends {@link SurfaceHost} to provide access to bone parameters.
 */
public interface SkinHost extends SurfaceHost
{
    /**
     * Returns a reference to the transform matrix of the named bone.
     */
    public Matrix4f getBoneMatrix (String bone);
}
