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

    /**
     * Creates a new summary for the described passes.
     */
    public PassSummary (PassDescriptor... passes)
    {
        for (PassDescriptor pass : passes) {
            Collections.addAll(vertexAttribs, pass.vertexAttribs);
            texCoordSets.add(pass.texCoordSets);
            colors |= pass.colors;
            normals |= pass.normals;
        }
    }

    @Override
    public int hashCode ()
    {
        int hash = 1;
        hash = 31*hash + vertexAttribs.hashCode();
        hash = 31*hash + texCoordSets.hashCode();
        hash = 31*hash + (colors ? 1231 : 1237);
        hash = 31*hash + (normals ? 1231 : 1237);
        return hash;
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof PassSummary)) {
            return false;
        }
        PassSummary osummary = (PassSummary)other;
        return vertexAttribs.equals(osummary.vertexAttribs) &&
            texCoordSets.equals(osummary.texCoordSets) &&
            colors == osummary.colors && normals == osummary.normals;
    }
}
