//
// $Id$

package com.threerings.opengl.renderer.config;

/**
 * The different coordinate spaces in which geometry may be specified.
 */
public enum CoordSpace
{
    /** Object space: must be transformed by the modelview matrix. */
    OBJECT,

    /** World space: must be transformed by the view matrix. */
    WORLD,

    /** Eye space: must be transformed by the identity matrix. */
    EYE;
}
