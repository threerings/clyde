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

package com.threerings.opengl.model.config;

import com.threerings.io.Streamable;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.model.Model;

/**
 * Contains a set of flags for different kinds of influences.
 */
public class InfluenceFlagConfig extends DeepObject
    implements Exportable, Streamable
{
    /** Whether or not to enable fog influences. */
    @Editable(hgroup="i")
    public boolean fog;

    /** Whether or not to enable light influences. */
    @Editable(hgroup="i")
    public boolean lights;

    /** Whether or not to enable projection influences. */
    @Editable(hgroup="i")
    public boolean projections;

    /** Whether or not to enable definition influences. */
    @Editable(hgroup="i")
    public boolean definitions;

    /**
     * Default constructor.
     */
    public InfluenceFlagConfig ()
    {
        this(true);
    }

    /**
     * Creates a new config.
     */
    public InfluenceFlagConfig (boolean value)
    {
        this(value, value, value, value);
    }

    /**
     * Creates a new config.
     */
    public InfluenceFlagConfig (
        boolean fog, boolean lights, boolean projections, boolean definitions)
    {
        this.fog = fog;
        this.lights = lights;
        this.projections = projections;
        this.definitions = definitions;
    }

    /**
     * Returns the set of influence flags corresponding to this config.
     */
    public int getFlags ()
    {
        return (fog ? Model.FOG_INFLUENCE : 0) |
            (lights ? Model.LIGHT_INFLUENCE : 0) |
            (projections ? Model.PROJECTION_INFLUENCE : 0) |
            (definitions ? Model.DEFINITION_INFLUENCE : 0);
    }
}
