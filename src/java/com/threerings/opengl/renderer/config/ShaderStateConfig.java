//
// $Id$

package com.threerings.opengl.renderer.config;

import org.lwjgl.opengl.GLContext;

import java.util.ArrayList;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.expr.Updater;
import com.threerings.util.DeepObject;

import com.threerings.opengl.geom.config.PassDescriptor;
import com.threerings.opengl.renderer.Program;
import com.threerings.opengl.renderer.Program.Uniform;
import com.threerings.opengl.renderer.Shader;
import com.threerings.opengl.renderer.config.ShaderConfig.UniformConfig;
import com.threerings.opengl.renderer.state.ShaderState;
import com.threerings.opengl.util.GlContext;

/**
 * Configurable shader state.
 */
@EditorTypes({ ShaderStateConfig.Disabled.class, ShaderStateConfig.Enabled.class })
public abstract class ShaderStateConfig extends DeepObject
    implements Exportable
{
    /**
     * Disables the shader.
     */
    public static class Disabled extends ShaderStateConfig
    {
        @Override // documentation inherited
        public boolean isSupported (GlContext ctx)
        {
            return true;
        }

        @Override // documentation inherited
        public ShaderState getState (GlContext ctx, Scope scope, ArrayList<Updater> updaters)
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
        @Editable(nullable=true)
        public ConfigReference<ShaderConfig> vertex;

        /** The fragment shader to use. */
        @Editable(nullable=true)
        public ConfigReference<ShaderConfig> fragment;

        @Override // documentation inherited
        public boolean isSupported (GlContext ctx)
        {
            return (vertex == null || GLContext.getCapabilities().GL_ARB_vertex_shader) &&
                (fragment == null || GLContext.getCapabilities().GL_ARB_fragment_shader);
        }

        @Override // documentation inherited
        public void populateDescriptor (GlContext ctx, PassDescriptor desc)
        {
            ShaderConfig vconfig = getShaderConfig(ctx, vertex);
            if (vconfig != null) {
                vconfig.populateDescriptor(ctx, desc);
            } else {
                super.populateDescriptor(ctx, desc);
            }
        }

        @Override // documentation inherited
        public ShaderState getState (GlContext ctx, Scope scope, ArrayList<Updater> updaters)
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
                PassDescriptor desc = new PassDescriptor();
                vconfig.populateDescriptor(ctx, desc);
                boolean relink = false;
                for (int ii = 0; ii < desc.vertexAttribs.length; ii++) {
                    String attrib = desc.vertexAttribs[ii];
                    int oloc = program.getAttribLocation(attrib);
                    int nloc = desc.firstVertexAttribIndex + ii;
                    if (oloc != nloc) {
                        program.setAttribLocation(attrib, nloc);
                        relink = true;
                    }
                }
                if (relink) {
                    program.relink();
                }
                for (UniformConfig config : vconfig.getUniforms(ctx)) {
                    config.createUniforms(scope, program, uniforms, updaters);
                }
            }
            if (fshader != null) {
                for (UniformConfig config : fconfig.getUniforms(ctx)) {
                    config.createUniforms(scope, program, uniforms, updaters);
                }
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
    }

    /**
     * Determines whether this state is supported by the hardware.
     */
    public abstract boolean isSupported (GlContext ctx);

    /**
     * Populates the relevant portion of the supplied descriptor.
     */
    public void populateDescriptor (GlContext ctx, PassDescriptor desc)
    {
        desc.coordSpace = CoordSpace.OBJECT;
        desc.hints = new String[0];
        desc.vertexAttribs = new String[0];
    }

    /**
     * Returns the corresponding shader state.
     */
    public abstract ShaderState getState (GlContext ctx, Scope scope, ArrayList<Updater> updaters);
}
