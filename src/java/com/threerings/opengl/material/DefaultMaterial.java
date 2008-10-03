//
// $Id$

package com.threerings.opengl.material;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.threerings.opengl.effect.ParticleGeometry;
import com.threerings.opengl.geometry.Geometry;
import com.threerings.opengl.model.SkinMesh;
import com.threerings.opengl.renderer.Program;
import com.threerings.opengl.renderer.Shader;
import com.threerings.opengl.renderer.Texture;
import com.threerings.opengl.renderer.TextureUnit;
import com.threerings.opengl.renderer.state.AlphaState;
import com.threerings.opengl.renderer.state.ColorMaskState;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.state.ShaderState;
import com.threerings.opengl.renderer.state.TextureState;
import com.threerings.opengl.util.GlUtil;

import static com.threerings.opengl.Log.*;

/**
 * The default material class.
 */
public class DefaultMaterial extends Material
{
    @Override // documentation inherited
    public void didInit ()
    {
        // create the shared states
        _sstates = RenderState.createEmptySet();
        _sstates[RenderState.ALPHA_STATE] = AlphaState.OPAQUE;
        _sstates[RenderState.COLOR_MASK_STATE] = ColorMaskState.ALL;
        _sstates[RenderState.DEPTH_STATE] = DepthState.TEST_WRITE;
        _sstates[RenderState.FOG_STATE] = FogState.DISABLED;
        _sstates[RenderState.LIGHT_STATE] = LightState.DISABLED;
        _sstates[RenderState.SHADER_STATE] = ShaderState.DISABLED;

        // load the diffuse texture
        TextureUnit[] units = null;
        String diffuse = _props.getProperty("diffuse");
        if (diffuse != null) {
            int minFilter = parseMinFilter(
                _props.getProperty("min_filter", "linear_mipmap_linear"));
            int magFilter = parseMagFilter(
                _props.getProperty("mag_filter", "linear"));
            Texture dtex = _ctx.getTextureCache().getTexture(
                diffuse, null, true, isMipmappedFilter(minFilter),
                Boolean.parseBoolean(_props.getProperty("compress", "true")));
            if (dtex != null) {
                dtex.setFilters(minFilter, magFilter);
                units = new TextureUnit[] { new TextureUnit(dtex) };
                if (dtex.hasAlpha()) {
                    _alphaThreshold =
                        Float.parseFloat(_props.getProperty("alpha_threshold", "0.5"));
                    _sstates[RenderState.ALPHA_STATE] =
                        AlphaState.getTestInstance(_alphaThreshold);
                }
                if (Boolean.parseBoolean(_props.getProperty("sphere_map"))) {
                    units[0].genModeS = units[0].genModeT = GL11.GL_SPHERE_MAP;
                }
            }
        }
        _sstates[RenderState.TEXTURE_STATE] = new TextureState(units);

        // note emissivity
        _emissive = Boolean.parseBoolean(_props.getProperty("emissive"));

        // perhaps load the shader program
        if (GLContext.getCapabilities().GL_ARB_vertex_shader &&
            _skinProgram == null && !_disableShaders) {
            Shader vert = _ctx.getShaderCache().getShader(
                "shader/vertex/skin_old.vert", "MAX_BONE_COUNT " + SkinMesh.MAX_BONE_COUNT);
            if (vert == null) { // compilation error
                _disableShaders = true;
                return;
            }

            Program program = _ctx.getShaderCache().getProgram(vert, null);
            if (program == null) { // linkage error
                _disableShaders = true;
                return;
            }
            _skinProgram = program;
            _skinProgram.setAttribLocation("boneIndices", SkinMesh.BONE_INDEX_ATTRIB);
            _skinProgram.setAttribLocation("boneWeights", SkinMesh.BONE_WEIGHT_ATTRIB);
            _skinProgram.relink();

            vert = _ctx.getShaderCache().getShader(
                "shader/vertex/skin_old.vert", "MAX_BONE_COUNT " + SkinMesh.MAX_BONE_COUNT,
                "SPHERE_MAP");
            program = _ctx.getShaderCache().getProgram(vert, null);
            _skinProgramSphere = program;
            _skinProgramSphere.setAttribLocation("boneIndices", SkinMesh.BONE_INDEX_ATTRIB);
            _skinProgramSphere.setAttribLocation("boneWeights", SkinMesh.BONE_WEIGHT_ATTRIB);
            _skinProgramSphere.relink();
        }
    }

    @Override // documentation inherited
    public Surface createSurface (Geometry geom)
    {
        if (geom instanceof ParticleGeometry) {
            return new ParticleSurface(_ctx, this, (ParticleGeometry)geom);
        } else if (geom instanceof SkinMesh) {
            return new SkinSurface(_ctx, this, (SkinMesh)geom);
        } else {
            return new DefaultSurface(_ctx, this, geom);
        }
    }

    /**
     * Returns the set of states shared by all surfaces created by this material.
     */
    public RenderState[] getSharedStates ()
    {
        return _sstates;
    }

    /**
     * Returns the configured alpha threshold.
     */
    public float getAlphaThreshold ()
    {
        return _alphaThreshold;
    }

    /**
     * Checks whether the material is fully emissive.
     */
    public boolean isEmissive ()
    {
        return _emissive;
    }

    /**
     * Determines whether the skin programs have successfully loaded.
     */
    public boolean hasSkinPrograms ()
    {
        return _skinProgram != null && _skinProgramSphere != null;
    }

    /**
     * Returns a reference to the shader program to use for skinned meshes (if any).
     */
    public Program getSkinProgram (boolean sphereMap)
    {
        return sphereMap ? _skinProgramSphere : _skinProgram;
    }

    /**
     * Parses and returns a minification filter constant.
     */
    protected static int parseMinFilter (String filter)
    {
        if (filter.equals("nearest")) {
            return GL11.GL_NEAREST;
        } else if (filter.equals("linear")) {
            return GL11.GL_LINEAR;
        } else if (filter.equals("nearest_mipmap_nearest")) {
            return GL11.GL_NEAREST_MIPMAP_NEAREST;
        } else if (filter.equals("linear_mipmap_nearest")) {
            return GL11.GL_LINEAR_MIPMAP_NEAREST;
        } else if (filter.equals("nearest_mipmap_linear")) {
            return GL11.GL_NEAREST_MIPMAP_LINEAR;
        } else if (filter.equals("linear_mipmap_linear")) {
            return GL11.GL_LINEAR_MIPMAP_LINEAR;
        } else {
            log.warning("Invalid minification filter [name=" + filter + "].");
            return GL11.GL_NEAREST_MIPMAP_LINEAR;
        }
    }

    /**
     * Determines whether the identified minification filter requires mipmaps.
     */
    protected static boolean isMipmappedFilter (int minFilter)
    {
        return minFilter != GL11.GL_NEAREST && minFilter != GL11.GL_LINEAR;
    }

    /**
     * Parses and returns a magnification filter constant.
     */
    protected static int parseMagFilter (String filter)
    {
        if (filter.equals("nearest")) {
            return GL11.GL_NEAREST;
        } else if (filter.equals("linear")) {
            return GL11.GL_LINEAR;
        } else {
            log.warning("Invalid magnification filter [name=" + filter + "].");
            return GL11.GL_LINEAR;
        }
    }

    /** The states shared between all surfaces. */
    protected RenderState[] _sstates;

    /** The configured alpha threshold. */
    protected float _alphaThreshold = 1f;

    /** If true, the material is fully emissive. */
    protected boolean _emissive;

    /** The skin program. */
    protected static Program _skinProgram;

    /** The skin program with sphere mapping enabled. */
    protected static Program _skinProgramSphere;

    /** If true, shaders are disabled because of a compilation/linkage error. */
    protected static boolean _disableShaders;
}
