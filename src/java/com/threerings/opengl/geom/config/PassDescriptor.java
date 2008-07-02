//
// $Id$

package com.threerings.opengl.geom.config;

/**
 * Describes the elements of state that will be used in a pass for the purpose of configuring
 * the geometry instance.
 */
public class PassDescriptor
{
    /** The set of hints specified in the vertex shader config. */
    public String[] hints;

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
}
