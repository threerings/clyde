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

package com.threerings.opengl.renderer.state;

import com.threerings.opengl.renderer.Program;
import com.threerings.opengl.renderer.Program.Uniform;
import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the GLSL shader state.
 */
public class ShaderState extends RenderState
{
    /** A state that disables shading. */
    public static final ShaderState DISABLED = new ShaderState(null, null, false);

    /**
     * Creates a new shader state.
     */
    public ShaderState (Program program, Uniform[] uniforms, boolean vertexProgramTwoSide)
    {
        _program = program;
        _uniforms = uniforms;
        _vertexProgramTwoSide = vertexProgramTwoSide;
    }

    /**
     * Returns a reference to the shader program.
     */
    public Program getProgram ()
    {
        return _program;
    }

    /**
     * Returns a reference to the array of shader uniform values.
     */
    public Uniform[] getUniforms ()
    {
        return _uniforms;
    }

    /**
     * Checks whether the state enables two-sided vertex program mode.
     */
    public boolean isVertexProgramTwoSide ()
    {
        return _vertexProgramTwoSide;
    }

    @Override
    public int getType ()
    {
        return SHADER_STATE;
    }

    @Override
    public void apply (Renderer renderer)
    {
        renderer.setShaderState(_program, _vertexProgramTwoSide);
        if (_program != null && _uniforms != null) {
            _program.setUniforms(_uniforms);
        }
    }

    /** The shader program. */
    protected Program _program;

    /** The shader uniforms. */
    protected Uniform[] _uniforms;

    /** Whether or not to enable two-sided vertex program mode. */
    protected boolean _vertexProgramTwoSide;
}
