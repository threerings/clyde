//
// $Id$

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
    public static final ShaderState DISABLED = new ShaderState(null, null);

    /**
     * Creates a new shader state.
     */
    public ShaderState (Program program, Uniform[] uniforms)
    {
        _program = program;
        _uniforms = uniforms;
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

    @Override // documentation inherited
    public int getType ()
    {
        return SHADER_STATE;
    }

    @Override // documentation inherited
    public void apply (Renderer renderer)
    {
        renderer.setShaderState(_program);
        if (_program != null && _uniforms != null) {
            _program.setUniforms(_uniforms);
        }
    }

    /** The shader program. */
    protected Program _program;

    /** The shader uniforms. */
    protected Uniform[] _uniforms;
}
