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

package com.threerings.opengl.renderer.util;

import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import com.samskivert.util.ArrayUtil;

import com.threerings.util.ArrayKey;
import com.threerings.util.CacheUtil;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.TextureUnit;
import com.threerings.opengl.renderer.state.CullState;
import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.renderer.state.MaterialState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.state.ShaderState;
import com.threerings.opengl.renderer.state.TextureState;

/**
 * Contains methods to create snippets of GLSL shader code.
 */
public class SnippetUtil
{
  /**
   * Creates a fog parameter snippet.
   */
  public static void getFogParam (
    String name, String eyeVertex, RenderState[] states, List<String> defs)
  {
    FogState state = (FogState)states[RenderState.FOG_STATE];
    int mode = (state == null) ? -1 : state.getFogMode();
    if (mode == -1) {
      defs.add("SET_" + name);
      return;
    }
    ArrayKey key = new ArrayKey(name, eyeVertex, mode);
    String def = _fogParams.get(key);
    if (def == null) {
      _fogParams.put(key, def = createFogParamDef(name, eyeVertex, mode));
    }
    defs.add(def);
  }

  /**
   * Creates a fog blend snippet.
   */
  public static void getFogBlend (String name, RenderState[] states, List<String> defs)
  {
    FogState state = (FogState)states[RenderState.FOG_STATE];
    int mode = (state == null) ? -1 : state.getFogMode();
    if (mode == -1) {
      defs.add("BLEND_" + name);
      return;
    }
    defs.add("BLEND_" + name + " gl_FragColor.rgb = mix(gl_Fog.color.rgb, gl_FragColor.rgb, " +
      "gl_FogFragCoord);");
  }

  /**
   * Retrieves a tex coord snippet.
   */
  public static void getTexCoord (
    String name, String eyeVertex, String eyeNormal, RenderState[] states, List<String> defs)
  {
    TextureState state = (TextureState)states[RenderState.TEXTURE_STATE];
    TextureUnit[] units = (state == null) ? null : state.getUnits();
    ArrayKey key = createTexCoordKey(name, eyeVertex, eyeNormal, units);
    String def = _texCoords.get(key);
    if (def == null) {
      _texCoords.put(key, def = createTexCoordDef(name, eyeVertex, eyeNormal, units));
    }
    defs.add(def);
  }

  /**
   * Creates a vertex lighting snippet.
   */
  public static void getVertexLighting (
    String name, String eyeVertex, String eyeNormal, RenderState[] states,
    boolean vertexProgramTwoSide, List<String> defs)
  {
    CullState cstate = (CullState)states[RenderState.CULL_STATE];
    LightState lstate = (LightState)states[RenderState.LIGHT_STATE];
    int cullFace = (cstate == null) ? -1 : cstate.getCullFace();
    Light.Type[] lights = (lstate == null) ? null : getLightTypes(lstate.getLights());
    ArrayKey key = new ArrayKey(
      name, eyeVertex, eyeNormal, cullFace, vertexProgramTwoSide, lights);
    String def = _vertexLighting.get(key);
    if (def == null) {
      _vertexLighting.put(key, def = createVertexLightingDef(
        name, eyeVertex, eyeNormal, cullFace, vertexProgramTwoSide, lights));
    }
    defs.add(def);
  }

  /**
   * Creates a fragment lighting snippet.
   */
  public static void getFragmentLighting (
    String name, String eyeVertex, String eyeNormal, RenderState[] states, List<String> defs)
  {
    LightState lstate = (LightState)states[RenderState.LIGHT_STATE];
    Light.Type[] lights = (lstate == null) ? null : getLightTypes(lstate.getLights());
    ArrayKey key = new ArrayKey(name, eyeVertex, eyeNormal, lights);
    String def = _fragmentLighting.get(key);
    if (def == null) {
      _fragmentLighting.put(key, def = createFragmentLightingDef(
        name, eyeVertex, eyeNormal, lights));
    }
    defs.add(def);
  }

  /**
   * Returns the source of a vertex shader that does the fixed-function vertex processing for
   * the supplied states: lighting, texture coordinate generation, and the fog and clip
   * coordinates.  Link it without a fragment shader so the fixed-function fragment stage does
   * the rest, and render it with a {@link ShaderState#isFixedFunctionEmulation} state so that
   * stage's fog and color sum stay on.
   *
   * <p>Unlike the snippets above, this follows the fixed-function equations exactly (color
   * material, specular, the texture matrix applied after generation).
   *
   * @return the source, or null if lighting is enabled without a material state, which would
   * leave the lighting to whatever material the previous pass set.
   */
  public static String getFixedFunctionVertexShader (RenderState[] states)
  {
    LightState lstate = (LightState)states[RenderState.LIGHT_STATE];
    Light.Type[] lights = (lstate == null) ? null : getLightTypes(lstate.getLights());
    MaterialState mstate = (MaterialState)states[RenderState.MATERIAL_STATE];
    if (lights != null && mstate == null) {
      return null;
    }
    LightingModel model = (lights == null) ? null : new LightingModel(mstate);
    TextureState tstate = (TextureState)states[RenderState.TEXTURE_STATE];
    int[][] genModes = getGenModes((tstate == null) ? null : tstate.getUnits());
    ArrayKey key = new ArrayKey(lights, model, genModes);
    String source = _fixedFunctionVertexShaders.get(key);
    if (source == null) {
      _fixedFunctionVertexShaders.put(
        key, source = createFixedFunctionVertexShader(lights, model, genModes));
    }
    return source;
  }

  /**
   * Creates and returns the definition for the supplied fog parameters.
   */
  protected static String createFogParamDef (String name, String eyeVertex, int mode)
  {
    StringBuilder buf = new StringBuilder();
    switch(mode) {
      case GL11.GL_LINEAR:
        buf.append("gl_FogFragCoord = clamp((gl_Fog.end + " + eyeVertex + ".z) * gl_Fog.scale");
        break;
      case GL11.GL_EXP:
        buf.append("gl_FogFragCoord = clamp(exp(gl_Fog.density * " + eyeVertex + ".z)");
        break;
      case GL11.GL_EXP2:
        buf.append("float f = gl_Fog.density * " + eyeVertex + ".z; ");
        buf.append("gl_FogFragCoord = clamp(exp(-f*f)");
        break;
    }
    buf.append(", 0.0, 1.0); ");
    return "SET_" + name + " { " + buf + "}";
  }

  /**
   * Creates and returns a key for the supplied tex coord parameters.
   */
  protected static ArrayKey createTexCoordKey (
    String name, String eyeVertex, String eyeNormal, TextureUnit[] units)
  {
    return new ArrayKey(name, eyeVertex, eyeNormal, getGenModes(units));
  }

  /**
   * Returns the s, t, r, and q generation modes of each unit (null for null units).
   */
  protected static int[][] getGenModes (TextureUnit[] units)
  {
    int[][] genModes = new int[units == null ? 0 : units.length][];
    for (int ii = 0; ii < genModes.length; ii++) {
      TextureUnit unit = units[ii];
      genModes[ii] = (unit == null) ? null :
        new int[] { unit.genModeS, unit.genModeT, unit.genModeR, unit.genModeQ };
    }
    return genModes;
  }

  /**
   * Creates and returns the definition for the supplied tex coord parameters.
   */
  protected static String createTexCoordDef (
    String name, String eyeVertex, String eyeNormal, TextureUnit[] units)
  {
    StringBuilder buf = new StringBuilder();
    if (units != null) {
      if (anySphereMapped(units)) {
        buf.append("vec3 f = reflect(normalize(" + eyeVertex + ".xyz), " +
          eyeNormal + ".xyz); ");
        buf.append("float z1 = f.z + 1.0; ");
        buf.append("float rm = 0.5 / sqrt(f.x*f.x + f.y*f.y + (z1*z1)); ");
        buf.append("vec4 sphereTexCoord = vec4(f.x*rm + 0.5, f.y*rm + 0.5, 0.0, 1.0); ");
      }
      for (int ii = 0; ii < units.length; ii++) {
        createTexCoordUnit(ii, units[ii], eyeVertex, buf);
      }
    }
    return name + " { " + buf + "}";
  }

  /**
   * Determines whether any of the specified texture units use sphere-map texture coordinate
   * generation.
   */
  protected static boolean anySphereMapped (TextureUnit[] units)
  {
    for (TextureUnit unit : units) {
      if (unit != null && unit.anyGenModesEqual(GL11.GL_SPHERE_MAP)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Appends the code for a single texture coordinate unit.
   */
  protected static void createTexCoordUnit (
    int idx, TextureUnit unit, String eyeVertex, StringBuilder buf)
  {
    if (unit == null) {
      return;
    }
    if (unit.genModeS == GL11.GL_SPHERE_MAP && unit.genModeT == GL11.GL_SPHERE_MAP) {
      buf.append("gl_TexCoord[" + idx + "] = sphereTexCoord; ");
    } else if (unit.allGenModesEqual(-1)) {
      buf.append("gl_TexCoord[" + idx + "] = gl_TextureMatrix[" +
        idx + "] * gl_MultiTexCoord" + idx + "; ");
    } else {
      if (unit.anyGenModesEqual(-1)) {
        buf.append("vec4 texCoord" + idx + " = gl_TextureMatrix[" +
          idx + "] * gl_MultiTexCoord" + idx + "; ");
      }
      buf.append("gl_TexCoord[" + idx + "] = vec4(");
      buf.append(createTexCoordElement(idx, 's', unit.genModeS, eyeVertex) + ", ");
      buf.append(createTexCoordElement(idx, 't', unit.genModeT, eyeVertex) + ", ");
      buf.append(createTexCoordElement(idx, 'r', unit.genModeR, eyeVertex) + ", ");
      buf.append(createTexCoordElement(idx, 'q', unit.genModeQ, eyeVertex) + "); ");
    }
  }

  /**
   * Returns the code for a single texture coordinate element.
   */
  protected static String createTexCoordElement (
    int idx, char element, int mode, String eyeVertex)
  {
    switch (mode) {
      case GL11.GL_SPHERE_MAP:
        return "sphereTexCoord." + element;
      case GL11.GL_OBJECT_LINEAR:
        return "dot(gl_ObjectPlane" + Character.toUpperCase(element) +
          "[" + idx + "], gl_Vertex)";
      case GL11.GL_EYE_LINEAR:
        return "dot(gl_EyePlane" + Character.toUpperCase(element) +
          "[" + idx + "], " + eyeVertex + ")";
      default:
        return "(gl_TextureMatrix[" + idx + "] * gl_MultiTexCoord" + idx + ")." +
          getSwizzle(element);
    }
  }

  /**
   * Returns the GLSL swizzle for the named texture coordinate element.
   */
  protected static char getSwizzle (char element)
  {
    // GLSL spells the r coordinate p, since r is already the first (red) component
    return (element == 'r') ? 'p' : element;
  }

  /**
   * Returns an array of types corresponding to each light.
   */
  protected static Light.Type[] getLightTypes (Light[] lights)
  {
    if (lights == null) {
      return null;
    }
    Light.Type[] types = new Light.Type[lights.length];
    for (int ii = 0; ii < types.length; ii++) {
      Light light = lights[ii];
      types[ii] = (light == null) ? null : light.getType();
    }
    return types;
  }

  /**
   * Creates and returns the definition for the supplied vertex lighting parameters.
   */
  protected static String createVertexLightingDef (
    String name, String eyeVertex, String eyeNormal, int cullFace,
    boolean vertexProgramTwoSide, Light.Type[] lights)
  {
    StringBuilder buf = new StringBuilder();
    buf.append(createLightingSide("Front", "Front", eyeVertex, eyeNormal, lights));
    if (vertexProgramTwoSide) {
      buf.append("vec4 rnormal = -" + eyeNormal + "; ");
      buf.append(createLightingSide("Back", "Back", eyeVertex, "rnormal", lights));
    }
    return name + " { " + buf + "}";
  }

  /**
   * Creates and returns the definition for the supplied fragment lighting parameters.
   */
  protected static String createFragmentLightingDef (
    String name, String eyeVertex, String eyeNormal, Light.Type[] lights)
  {
    StringBuilder buf = new StringBuilder();
    buf.append(createLightingSide("Frag", "Front", eyeVertex, eyeNormal, lights));
    return name + " { " + buf + "}";
  }

  /**
   * Creates and returns the expression for a single lit side.
   */
  protected static String createLightingSide (
    String dest, String side, String eyeVertex, String eyeNormal, Light.Type[] lights)
  {
    String variable = "gl_" + dest + "Color";
    if (lights == null) {
      return variable + " = gl_Color; ";
    }
    StringBuilder buf = new StringBuilder();
    buf.append(variable + " = gl_" + side + "LightModelProduct.sceneColor; ");
    for (int ii = 0; ii < lights.length; ii++) {
      Light.Type light = lights[ii];
      if (light == null) {
        continue;
      }
      switch (light) {
        case DIRECTIONAL:
          addDirectionalLight(ii, dest, side, eyeNormal, buf);
          break;
        case POINT:
          addPointLight(ii, dest, side, eyeVertex, eyeNormal, buf);
          break;
        case SPOT:
          addSpotLight(ii, dest, side, eyeVertex, eyeNormal, buf);
          break;
      }
    }
    return buf.toString();
  }

  /**
   * Adds the influence of a directional light.
   */
  protected static void addDirectionalLight (
    int idx, String dest, String side, String eyeNormal, StringBuilder buf)
  {
    String lightProduct = "gl_" + side + "LightProduct[" + idx + "]";
    buf.append("gl_" + dest + "Color += " + lightProduct + ".ambient + " +
      lightProduct + ".diffuse * max(dot(" + eyeNormal +
      ", gl_LightSource[" + idx + "].position), 0.0); ");
  }

  /**
   * Adds the influence of a point light.
   */
  protected static void addPointLight (
    int idx, String dest, String side, String eyeVertex, String eyeNormal, StringBuilder buf)
  {
    String lightSource = "gl_LightSource[" + idx + "]";
    String lightProduct = "gl_" + side + "LightProduct[" + idx + "]";
    buf.append("{ vec4 lvec = " + lightSource + ".position - " + eyeVertex + "; ");
    buf.append("float d = length(lvec); ");
    buf.append("gl_" + dest + "Color += (" + lightProduct + ".ambient + " + lightProduct +
      ".diffuse * max(dot(" + eyeNormal + ", lvec/d), 0.0)) / (" + lightSource +
      ".constantAttenuation + d*(" + lightSource + ".linearAttenuation + d*" + lightSource +
      ".quadraticAttenuation)); } ");
  }

  /**
   * Adds the influence of a spot light.
   */
  protected static void addSpotLight (
    int idx, String dest, String side, String eyeVertex, String eyeNormal, StringBuilder buf)
  {
    String lightSource = "gl_LightSource[" + idx + "]";
    String lightProduct = "gl_" + side + "LightProduct[" + idx + "]";
    buf.append("{ vec4 lvec = " + lightSource + ".position - " + eyeVertex + "; ");
    buf.append("float d = length(lvec); ");
    buf.append("vec4 nvec = lvec/d; ");
    buf.append("float cosa = -dot(nvec.xyz, " + lightSource + ".spotDirection); ");
    buf.append("gl_" + dest + "Color += " + getSpotFactor(lightSource, "cosa") + " * (" +
      lightProduct + ".ambient + " + lightProduct + ".diffuse * max(dot(" + eyeNormal +
      ", nvec), 0.0)) / (" + lightSource + ".constantAttenuation + d*(" + lightSource +
      ".linearAttenuation + d*" + lightSource + ".quadraticAttenuation)); } ");
  }

  /**
   * Returns the expression for a spot light's falloff, given the cosine of the angle between
   * the spot direction and the vertex.
   */
  protected static String getSpotFactor (String lightSource, String cosa)
  {
    return "(" + cosa + " < " + lightSource + ".spotCosCutoff ? 0.0 : " +
      getLightingPow(cosa, lightSource + ".spotExponent") + ")";
  }

  /**
   * Returns an expression raising the base, clamped at zero, to the exponent the way the
   * lighting equations do, where 0^0 = 1.
   */
  protected static String getLightingPow (String base, String exponent)
  {
    // pow is undefined for a negative base and for 0^0; Apple's GL makes those NaN, and NaN
    // draws black, so the base bottoms out at a tiny positive value rather than zero
    return "pow(max(" + base + ", 1e-30), " + exponent + ")";
  }

  /**
   * Creates and returns the source of a fixed-function vertex shader.
   *
   * @param model the lighting model, or null if lighting is disabled.
   */
  protected static String createFixedFunctionVertexShader (
    Light.Type[] lights, LightingModel model, int[][] genModes)
  {
    StringBuilder buf = new StringBuilder("void main ()\n{\n");

    // ftransform() is invariant with the fixed-function transform, so these passes still
    // depth test equal against fixed-function passes over the same geometry
    buf.append("  gl_Position = ftransform();\n");
    buf.append("  vec4 eyeVertex = gl_ModelViewMatrix * gl_Vertex;\n");
    buf.append("  gl_ClipVertex = eyeVertex;\n");

    // a zero normal stays zero (lit only by ambient), where normalize() would make it NaN
    buf.append("  vec3 normal = gl_NormalMatrix * gl_Normal;\n");
    buf.append("  float normalLength2 = dot(normal, normal);\n");
    buf.append("  vec3 eyeNormal = (normalLength2 > 0.0) ? " +
      "normal * inversesqrt(normalLength2) : normal;\n");

    if (lights == null) {
      buf.append("  gl_FrontColor = gl_Color;\n");
    } else {
      appendFixedFunctionLighting(lights, model, buf);
    }
    appendFixedFunctionTexCoords(genModes, buf);

    // the fixed-function fragment stage computes the fog factor from this distance
    buf.append("  gl_FogFragCoord = abs(eyeVertex.z);\n");
    return buf.append("}\n").toString();
  }

  /**
   * Appends the fixed-function lighting equations for the supplied lights.
   */
  protected static void appendFixedFunctionLighting (
    Light.Type[] lights, LightingModel model, StringBuilder buf)
  {
    appendMaterialColors("front", "Front", GL11.GL_FRONT, model, buf);
    if (model.twoSide()) {
      appendMaterialColors("back", "Back", GL11.GL_BACK, model, buf);
    }
    if (model.specular()) {
      buf.append("  vec3 eyeDirection = " + (model.localViewer() ?
        "-normalize(eyeVertex.xyz)" : "vec3(0.0, 0.0, 1.0)") + ";\n");
    }
    for (int ii = 0; ii < lights.length; ii++) {
      Light.Type type = lights[ii];
      if (type == null) {
        continue;
      }
      String light = "gl_LightSource[" + ii + "]";
      buf.append("  {\n");
      if (type == Light.Type.DIRECTIONAL) {
        buf.append("    vec3 lightDirection = normalize(" + light + ".position.xyz);\n");
        buf.append("    float attenuation = 1.0;\n");
      } else {
        buf.append("    vec3 lightDirection = " + light + ".position.xyz - eyeVertex.xyz;\n");
        buf.append("    float lightDistance = length(lightDirection);\n");
        buf.append("    lightDirection /= lightDistance;\n");
        buf.append("    float attenuation = 1.0 / (" + light + ".constantAttenuation + " +
          "lightDistance*(" + light + ".linearAttenuation + lightDistance*" + light +
          ".quadraticAttenuation));\n");
        if (type == Light.Type.SPOT) {
          buf.append("    float spotCosine = dot(-lightDirection, normalize(" + light +
            ".spotDirection));\n");
          buf.append("    attenuation *= " + getSpotFactor(light, "spotCosine") + ";\n");
        }
      }
      if (model.specular()) {
        buf.append("    vec3 halfVector = normalize(lightDirection + eyeDirection);\n");
      }
      appendLightContribution("front", "Front", "eyeNormal", light, model, buf);
      if (model.twoSide()) {
        appendLightContribution("back", "Back", "-eyeNormal", light, model, buf);
      }
      buf.append("  }\n");
    }
    appendLitColors("front", "Front", model, buf);
    if (model.twoSide()) {
      appendLitColors("back", "Back", model, buf);
    }
  }

  /**
   * Appends the declarations of one side's material colors, which color material takes from
   * the vertex color, and starts its lit color at its emission plus the global ambient light.
   *
   * @param glSide the side as it appears in the GLSL built-ins ("Front" or "Back").
   * @param face the side's GL face constant.
   */
  protected static void appendMaterialColors (
    String side, String glSide, int face, LightingModel model, StringBuilder buf)
  {
    String material = "gl_" + glSide + "Material";
    buf.append("  vec4 " + side + "Ambient = " +
      (model.tracks(GL11.GL_AMBIENT, face) ? "gl_Color" : material + ".ambient") + ";\n");
    buf.append("  vec4 " + side + "Diffuse = " +
      (model.tracks(GL11.GL_DIFFUSE, face) ? "gl_Color" : material + ".diffuse") + ";\n");
    if (model.specular()) {
      buf.append("  vec4 " + side + "Specular = " +
        (model.tracks(GL11.GL_SPECULAR, face) ? "gl_Color" : material + ".specular") + ";\n");
      buf.append("  vec4 " + side + "Highlights = vec4(0.0);\n");
    }
    buf.append("  vec4 " + side + "Color = " +
      (model.tracks(GL11.GL_EMISSION, face) ? "gl_Color" : material + ".emission") +
      " + " + side + "Ambient * gl_LightModel.ambient;\n");
  }

  /**
   * Appends one light's contribution to one side, given that side's normal.
   */
  protected static void appendLightContribution (
    String side, String glSide, String normal, String light, LightingModel model,
    StringBuilder buf)
  {
    buf.append("    float " + side + "Dot = max(dot(" + normal + ", lightDirection), 0.0);\n");
    buf.append("    " + side + "Color += attenuation * (" + side + "Ambient * " + light +
      ".ambient + " + side + "Dot * " + side + "Diffuse * " + light + ".diffuse);\n");
    if (model.specular()) {
      // highlights only where the light reaches the surface
      buf.append("    " + side + "Highlights += (" + side + "Dot > 0.0 ? attenuation * " +
        getLightingPow("dot(" + normal + ", halfVector)", "gl_" + glSide + "Material.shininess") +
        " : 0.0) * " + side + "Specular * " + light + ".specular;\n");
    }
  }

  /**
   * Appends the assignment of one side's lit colors.
   */
  protected static void appendLitColors (
    String side, String glSide, LightingModel model, StringBuilder buf)
  {
    String highlights = model.specular() ? side + "Highlights.rgb" : "vec3(0.0)";
    String rgb = side + "Color.rgb";
    if (model.specular() && !model.separateSpecular()) {
      rgb += " + " + highlights;
    }
    // the lit alpha is the diffuse alpha
    buf.append("  gl_" + glSide + "Color = vec4(" + rgb + ", " + side + "Diffuse.a);\n");
    if (model.separateSpecular()) {
      buf.append("  gl_" + glSide + "SecondaryColor = vec4(" + highlights + ", 0.0);\n");
    }
  }

  /**
   * Appends the fixed-function texture coordinates for units with the supplied generation
   * modes.
   */
  protected static void appendFixedFunctionTexCoords (int[][] genModes, StringBuilder buf)
  {
    boolean sphereMapped = anyGenModesEqual(genModes, GL11.GL_SPHERE_MAP);
    if (sphereMapped || anyGenModesEqual(genModes, GL13.GL_REFLECTION_MAP)) {
      buf.append("  vec3 reflection = reflect(normalize(eyeVertex.xyz), eyeNormal);\n");
    }
    if (sphereMapped) {
      buf.append("  vec2 sphereMap = reflection.xy / " +
        "(2.0 * length(reflection + vec3(0.0, 0.0, 1.0))) + 0.5;\n");
    }
    for (int ii = 0; ii < genModes.length; ii++) {
      int[] modes = genModes[ii];
      if (modes == null) {
        continue;
      }
      String coords = "gl_MultiTexCoord" + ii;
      if (modes[0] != -1 || modes[1] != -1 || modes[2] != -1 || modes[3] != -1) {
        coords = "vec4(" + getFixedFunctionTexCoordElement(ii, 0, modes[0]) + ", " +
          getFixedFunctionTexCoordElement(ii, 1, modes[1]) + ", " +
          getFixedFunctionTexCoordElement(ii, 2, modes[2]) + ", " +
          getFixedFunctionTexCoordElement(ii, 3, modes[3]) + ")";
      }
      // generated coordinates go through the texture matrix too
      buf.append("  gl_TexCoord[" + ii + "] = gl_TextureMatrix[" + ii + "] * " + coords + ";\n");
    }
  }

  /**
   * Returns the expression for one element (0-3 for s-q) of a unit's texture coordinates.
   */
  protected static String getFixedFunctionTexCoordElement (int unit, int element, int mode)
  {
    char swizzle = "xyzw".charAt(element);
    switch (mode) {
      case GL11.GL_OBJECT_LINEAR:
        return "dot(gl_ObjectPlane" + "STRQ".charAt(element) + "[" + unit + "], gl_Vertex)";
      case GL11.GL_EYE_LINEAR:
        return "dot(gl_EyePlane" + "STRQ".charAt(element) + "[" + unit + "], eyeVertex)";
      case GL11.GL_SPHERE_MAP:
        return "sphereMap." + swizzle;
      case GL13.GL_NORMAL_MAP:
        return "eyeNormal." + swizzle;
      case GL13.GL_REFLECTION_MAP:
        return "reflection." + swizzle;
      default:
        return "gl_MultiTexCoord" + unit + "." + swizzle;
    }
  }

  /**
   * Checks whether any of the supplied units' generation modes equal the given mode.
   */
  protected static boolean anyGenModesEqual (int[][] genModes, int mode)
  {
    for (int[] modes : genModes) {
      if (modes != null && ArrayUtil.indexOf(modes, mode) != -1) {
        return true;
      }
    }
    return false;
  }

  /**
   * The parts of a material state that shape the fixed-function lighting equations (the
   * colors themselves come from the GL state).
   */
  protected record LightingModel (
    int colorMaterialMode, int colorMaterialFace, boolean twoSide, boolean localViewer,
    boolean separateSpecular, boolean specular)
  {
    public LightingModel (MaterialState state)
    {
      this(state.getColorMaterialMode(), state.getColorMaterialFace(), state.getTwoSide(),
        state.getLocalViewer(), state.getSeparateSpecular(), hasSpecular(state));
    }

    /**
     * Checks whether color material replaces the given property of the given face with the
     * vertex color.
     */
    public boolean tracks (int property, int face)
    {
      boolean tracked = (colorMaterialMode == property) ||
        (colorMaterialMode == GL11.GL_AMBIENT_AND_DIFFUSE &&
          (property == GL11.GL_AMBIENT || property == GL11.GL_DIFFUSE));
      return tracked &&
        (colorMaterialFace == face || colorMaterialFace == GL11.GL_FRONT_AND_BACK);
    }

    /**
     * Checks whether the state can produce specular highlights.
     */
    protected static boolean hasSpecular (MaterialState state)
    {
      return state.getColorMaterialMode() == GL11.GL_SPECULAR ||
        !isBlack(state.getFrontSpecular()) ||
        (state.getTwoSide() && !isBlack(state.getBackSpecular()));
    }

    /**
     * Checks whether the color has no red, green, or blue.
     */
    protected static boolean isBlack (Color4f color)
    {
      return color.r == 0f && color.g == 0f && color.b == 0f;
    }
  }

  /** Cached fog param snippets. */
  protected static Map<ArrayKey, String> _fogParams = CacheUtil.softValues();

  /** Cached tex coord snippets. */
  protected static Map<ArrayKey, String> _texCoords = CacheUtil.softValues();

  /** Cached vertex lighting snippets. */
  protected static Map<ArrayKey, String> _vertexLighting = CacheUtil.softValues();

  /** Cached fragment lighting snippets. */
  protected static Map<ArrayKey, String> _fragmentLighting = CacheUtil.softValues();

  /** Cached fixed-function vertex shader sources. */
  protected static Map<ArrayKey, String> _fixedFunctionVertexShaders = CacheUtil.softValues();
}
