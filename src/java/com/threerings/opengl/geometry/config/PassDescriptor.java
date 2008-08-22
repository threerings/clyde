//
// $Id$

package com.threerings.opengl.geometry.config;

import com.samskivert.util.ListUtil;

import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.config.CoordSpace;

/**
 * Describes the elements of state that will be used in a pass for the purpose of configuring
 * the geometry instance.
 */
public class PassDescriptor extends DeepObject
{
    /** The set of hints specified in the vertex shader config. */
    public String[] hints;

    /** The coordinate space in which the vertex shader works. */
    public CoordSpace coordSpace;

    /** The index of the first vertex attribute used in the pass. */
    public int firstVertexAttribIndex;

    /** The vertex attributes used by the pass. */
    public String[] vertexAttribs;

    /** The texture coordinate sets used by each unit. */
    public int[] texCoordSets;

    /** Whether or not the pass will use vertex colors. */
    public boolean colors;

    /** Whether or not the pass will use vertex normals. */
    public boolean normals;

    /**
     * Determines whether this descriptor contains the specified hint.
     */
    public boolean containsHint (String hint)
    {
        return ListUtil.contains(hints, hint);
    }
}
