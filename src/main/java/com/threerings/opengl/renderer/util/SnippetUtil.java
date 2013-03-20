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

import com.threerings.util.ArrayKey;
import com.threerings.util.CacheUtil;

import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.TextureUnit;
import com.threerings.opengl.renderer.state.CullState;
import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.renderer.state.RenderState;
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
        int[][] genModes = new int[units == null ? 0 : units.length][];
        for (int ii = 0; ii < genModes.length; ii++) {
            TextureUnit unit = units[ii];
            genModes[ii] = (unit == null) ? null :
                new int[] { unit.genModeS, unit.genModeT, unit.genModeR, unit.genModeQ };
        }
        return new ArrayKey(name, eyeVertex, eyeNormal, genModes);
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
                return "(gl_TextureMatrix[" + idx + "] * gl_MultiTexCoord" + idx + ")." + element;
        }
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
        buf.append("gl_" + dest + "Color += step(" + lightSource +
            ".spotCosCutoff, cosa) * pow(cosa, " + lightSource + ".spotExponent) * (" +
            lightProduct + ".ambient + " + lightProduct + ".diffuse * max(dot(" + eyeNormal +
            ", nvec), 0.0)) / (" + lightSource + ".constantAttenuation + d*(" + lightSource +
            ".linearAttenuation + d*" + lightSource + ".quadraticAttenuation)); } ");
    }

    /** Cached fog param snippets. */
    protected static Map<ArrayKey, String> _fogParams = CacheUtil.softValues();

    /** Cached tex coord snippets. */
    protected static Map<ArrayKey, String> _texCoords = CacheUtil.softValues();

    /** Cached vertex lighting snippets. */
    protected static Map<ArrayKey, String> _vertexLighting = CacheUtil.softValues();

    /** Cached fragment lighting snippets. */
    protected static Map<ArrayKey, String> _fragmentLighting = CacheUtil.softValues();
}
