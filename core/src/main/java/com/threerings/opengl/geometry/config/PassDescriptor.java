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
