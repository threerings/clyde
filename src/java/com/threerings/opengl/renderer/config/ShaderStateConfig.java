//
// $Id$

package com.threerings.opengl.renderer.config;

import java.util.ArrayList;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.Program;
import com.threerings.opengl.renderer.Program.Uniform;
import com.threerings.opengl.renderer.Shader;
import com.threerings.opengl.renderer.config.ShaderConfig.Variable;
import com.threerings.opengl.renderer.state.ShaderState;
import com.threerings.opengl.util.GlContext;

/**
 * Configurable shader state.
 */
public abstract class ShaderStateConfig extends DeepObject
    implements Exportable
{
    /**
     * Disables the shader.
     */
    public static class Disabled extends ShaderStateConfig
    {
        @Override // documentation inherited
        public ShaderState getState (GlContext ctx)
        {
            return ShaderState.DISABLED;
        }
    }

    /**
     * Enables the shader.
     */
    public static class Enabled extends ShaderStateConfig
    {
        /** The vertex shader to use. */
        @Editable
        public ConfigReference<ShaderConfig> vertex;

        /** The fragment shader to use. */
        @Editable
        public ConfigReference<ShaderConfig> fragment;

        @Override // documentation inherited
        public ShaderState getState (GlContext ctx)
        {
            ShaderConfig vconfig = getShaderConfig(ctx, vertex);
            ShaderConfig fconfig = getShaderConfig(ctx, fragment);
            Shader vshader = (vconfig == null) ? null : vconfig.getShader(ctx);
            Shader fshader = (fconfig == null) ? null : fconfig.getShader(ctx);
            if (vshader == null && fshader == null) {
                return ShaderState.DISABLED;
            }
            Program program = ctx.getShaderCache().getProgram(vshader, fshader);
            if (program == null) {
                return ShaderState.DISABLED;
            }
            ArrayList<Uniform> uniforms = new ArrayList<Uniform>();
            if (vshader != null) {
                addUniforms(program, uniforms, vconfig.getUniforms(ctx));
            }
            if (fshader != null) {
                addUniforms(program, uniforms, fconfig.getUniforms(ctx));
            }
            return new ShaderState(program, uniforms.toArray(new Uniform[uniforms.size()]));
        }

        /**
         * Attempts to resolve and return the provided config reference.
         */
        protected ShaderConfig getShaderConfig (GlContext ctx, ConfigReference<ShaderConfig> ref)
        {
            return (ref == null) ?
                null : ctx.getConfigManager().getConfig(ShaderConfig.class, ref);
        }

        /**
         * Adds the uniforms corresponding to the supplied variables to the list.
         */
        protected void addUniforms (Program program, ArrayList<Uniform> uniforms, Variable[] vars)
        {
            for (Variable var : vars) {
                int location = program.getUniformLocation(var.name);
                if (location != -1) {
                    uniforms.add(var.createUniform(location));
                }
            }
        }
    }

    /**
     * Returns the subclasses available for selection in the editor.
     */
    public static Class[] getEditorTypes ()
    {
        return new Class[] { Disabled.class, Enabled.class };
    }

    /**
     * Returns the corresponding shader state.
     */
    public abstract ShaderState getState (GlContext ctx);
}
