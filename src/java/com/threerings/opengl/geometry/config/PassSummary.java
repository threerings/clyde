//
// $Id$

package com.threerings.opengl.geometry.config;

import java.util.Collections;
import java.util.HashSet;

import com.samskivert.util.ArrayIntSet;

import com.threerings.util.DeepObject;

/**
 * Summarizes the attributes used by a set of passes.
 */
public class PassSummary extends DeepObject
{
    /** The names of all vertex attributes used by the passes. */
    public HashSet<String> vertexAttribs = new HashSet<String>();

    /** All of the texture coordinate sets used by the passes. */
    public ArrayIntSet texCoordSets = new ArrayIntSet();

    /** Whether or not any of the passes use vertex colors. */
    public boolean colors;

    /** Whether or not any of the passes use vertex normals. */
    public boolean normals;

    public PassSummary (PassDescriptor... passes)
    {
        for (PassDescriptor pass : passes) {
            Collections.addAll(vertexAttribs, pass.vertexAttribs);
            texCoordSets.add(pass.texCoordSets);
            colors |= pass.colors;
            normals |= pass.normals;
        }
    }
}
