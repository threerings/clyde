//
// $Id$

package com.threerings.opengl.compositor;

/**
 * A render scheme.  At the moment, render schemes are represented solely by their configurations,
 * and this class exists only to provide a home for related constants.
 */
public class RenderScheme
{
    /** The name of the translucent scheme. */
    public static final String TRANSLUCENT = "Translucent";

    /** The name of the perspective projection scheme. */
    public static final String PERSPECTIVE_PROJECTION = "Projection (Persp)";

    /** The name of the orthographic projection scheme. */
    public static final String ORTHOGRAPHIC_PROJECTION = "Projection (Ortho)";
}
