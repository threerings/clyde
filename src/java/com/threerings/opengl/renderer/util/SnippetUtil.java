//
// $Id$

package com.threerings.opengl.renderer.util;

import java.util.ArrayList;
import java.util.Collections;

import org.lwjgl.opengl.GL11;

import com.samskivert.util.SoftCache;

import com.threerings.util.ArrayKey;

import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.TextureUnit;
import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.renderer.state.MaterialState;
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
    public static void getFogParam (String name, RenderState[] states, ArrayList<String> defs)
    {
        FogState state = (FogState)states[RenderState.FOG_STATE];

    }

    /**
     * Retrieves a tex coord snippet.
     */
    public static void getTexCoord (
        String name, String eyeVertex, String eyeNormal, RenderState[] states,
        ArrayList<String> defs)
    {
        TextureState state = (TextureState)states[RenderState.TEXTURE_STATE];
        TextureUnit[] units = (state == null) ? null : state.getUnits();
        ArrayKey key = createTexCoordKey(name, eyeVertex, eyeNormal, units);
        String def = _texCoords.get(key);
        if (def == null) {
            _texCoords.put(key, def = createTexCoordDef(name, eyeVertex, eyeNormal, units));
            System.out.println(def);
        }
        defs.add(def);
    }

    /**
     * Creates a vertex lighting snippet.
     */
    public static void getVertexLighting (
        String name, RenderState[] states, ArrayList<String> defs)
    {
        LightState lstate = (LightState)states[RenderState.LIGHT_STATE];

    }

    /**
     * Creates and returns a key for the supplied tex coord parameters.
     */
    protected static ArrayKey createTexCoordKey (
        String name, String eyeVertex, String eyeNormal, TextureUnit[] units)
    {
        ArrayList<Object> list = new ArrayList<Object>();
        Collections.addAll(list, name, eyeVertex, eyeNormal);
        if (units != null) {
            for (TextureUnit unit : units) {
                list.add(unit == null ? null : new int[] {
                    unit.genModeS, unit.genModeT, unit.genModeR, unit.genModeQ });
            }
        }
        return new ArrayKey(list.toArray(new Object[list.size()]));
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
                buf.append("vec3 f = reflect(normalize(" + eyeVertex + ".xyz), normalize(" +
                    eyeNormal + ".xyz)); ");
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

    /** Cached tex coord snippets. */
    protected static SoftCache<ArrayKey, String> _texCoords = new SoftCache<ArrayKey, String>();
}
