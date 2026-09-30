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

package com.threerings.opengl.renderer.config;

import java.util.List;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLCapabilities;

import com.google.common.collect.Lists;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.RunAnywhere;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.expr.Updater;
import com.threerings.util.DeepObject;

import com.threerings.opengl.geometry.config.PassDescriptor;
import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.Program;
import com.threerings.opengl.renderer.Program.Uniform;
import com.threerings.opengl.renderer.Shader;
import com.threerings.opengl.renderer.config.ShaderConfig.UniformConfig;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.renderer.state.MaterialState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.state.ShaderState;
import com.threerings.opengl.renderer.util.SnippetUtil;
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
    @Override
    public boolean isSupported (GlContext ctx, boolean fallback)
    {
      return true;
    }

    @Override
    public ShaderState getState (
      GlContext ctx, Scope scope, RenderState[] states, List<Updater> updaters)
    {
      // Apple's fixed-function lighting turns every vertex behind a spot light black (NaN),
      // so there we light with a vertex shader that follows the fixed-function equations
      return (RunAnywhere.isMacOS() && hasSpotLight(states)) ?
        getFixedFunctionEmulation(ctx, states) : ShaderState.DISABLED;
    }

    /**
     * Checks whether the states light with a spot light.
     */
    protected boolean hasSpotLight (RenderState[] states)
    {
      LightState lstate = (LightState)states[RenderState.LIGHT_STATE];
      Light[] lights = (lstate == null) ? null : lstate.getLights();
      if (lights != null) {
        for (Light light : lights) {
          if (light != null && light.getType() == Light.Type.SPOT) {
            return true;
          }
        }
      }
      return false;
    }

    /**
     * Returns a state whose vertex shader does the fixed-function vertex processing for the
     * supplied states, or the disabled state if we can't make one.
     */
    protected ShaderState getFixedFunctionEmulation (GlContext ctx, RenderState[] states)
    {
      if (!GL.getCapabilities().GL_ARB_vertex_shader || ctx.getApp().getCompatibilityMode()) {
        return ShaderState.DISABLED;
      }
      String source = SnippetUtil.getFixedFunctionVertexShader(states);
      Shader shader = (source == null) ?
        null : ctx.getShaderCache().getGeneratedShader(GL20.GL_VERTEX_SHADER, source);
      Program program = (shader == null) ? null : ctx.getShaderCache().getProgram(shader, null);
      if (program == null) {
        return ShaderState.DISABLED;
      }
      // a source means lighting is enabled with a material state
      MaterialState mstate = (MaterialState)states[RenderState.MATERIAL_STATE];
      return new ShaderState(program, null, mstate.getTwoSide(), true);
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

    /** Whether or not to enable two-sided vertex program mode. */
    @Editable
    public boolean vertexProgramTwoSide;

    @Override
    public boolean isSupported (GlContext ctx, boolean fallback)
    {
      return (vertex == null || GL.getCapabilities().GL_ARB_vertex_shader) &&
        (fragment == null || GL.getCapabilities().GL_ARB_fragment_shader) &&
          !ctx.getApp().getCompatibilityMode();
    }

    @Override
    public void populateDescriptor (GlContext ctx, PassDescriptor desc)
    {
      ShaderConfig vconfig = getShaderConfig(ctx, vertex);
      if (vconfig != null) {
        vconfig.populateDescriptor(ctx, desc);
      } else {
        super.populateDescriptor(ctx, desc);
      }
    }

    @Override
    public ShaderState getState (
      GlContext ctx, Scope scope, RenderState[] states, List<Updater> updaters)
    {
      ShaderConfig vconfig = getShaderConfig(ctx, vertex);
      ShaderConfig fconfig = getShaderConfig(ctx, fragment);
      Shader vshader = (vconfig == null) ?
        null : vconfig.getShader(ctx, scope, states, vertexProgramTwoSide);
      Shader fshader = (fconfig == null) ?
        null : fconfig.getShader(ctx, scope, states, vertexProgramTwoSide);
      if (vshader == null && fshader == null) {
        return ShaderState.DISABLED;
      }
      Program program = ctx.getShaderCache().getProgram(vshader, fshader);
      if (program == null) {
        return ShaderState.DISABLED;
      }
      List<Uniform> uniforms = Lists.newArrayList();
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
      return new ShaderState(
        program, uniforms.toArray(new Uniform[uniforms.size()]), vertexProgramTwoSide, false);
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
  public abstract boolean isSupported (GlContext ctx, boolean fallback);

  /**
   * Populates the relevant portion of the supplied descriptor.
   */
  public void populateDescriptor (GlContext ctx, PassDescriptor desc)
  {
    desc.coordSpace = CoordSpace.OBJECT;
    desc.hints = ArrayUtil.EMPTY_STRING;
    desc.vertexAttribs = ArrayUtil.EMPTY_STRING;
  }

  /**
   * Returns the corresponding shader state.
   */
  public abstract ShaderState getState (
    GlContext ctx, Scope scope, RenderState[] states, List<Updater> updaters);
}
