//
// $Id$

package com.threerings.opengl.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.lang.ref.WeakReference;

import java.util.Arrays;
import java.util.HashMap;

import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBVertexShader;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.SoftCache;

import com.threerings.opengl.renderer.Program;
import com.threerings.opengl.renderer.Shader;

import static com.threerings.opengl.Log.*;

/**
 * Caches loaded shaders and shader programs.
 */
public class ShaderCache extends ResourceCache
{
    /**
     * Creates a new shader cache.
     *
     * @param checkTimestamps if true, check the last-modified timestamp of each resource file
     * when we retrieve it from the cache, reloading the resource if the file has been modified
     * externally.
     */
    public ShaderCache (GlContext ctx, boolean checkTimestamps)
    {
        super(ctx, checkTimestamps);
    }

    /**
     * Loads and returns the shader at the specified path with the supplied preprocessor
     * definitions.
     */
    public Shader getShader (String path, String... defs)
    {
        return getShader(path, defs, new String[0]);
    }

    /**
     * Loads and returns the shader at the specified path with the supplied preprocessor
     * definitions as well as a set of derived definitions (not used to find the shader,
     * but included if the shader is compiled).
     */
    public Shader getShader (String path, String[] defs, String[] ddefs)
    {
        return _shaders.getResource(new ShaderKey(path, defs, ddefs));
    }

    /**
     * Retrieves the shader source at the specified path.
     */
    public String getSource (String path)
    {
        return _source.getResource(path);
    }

    /**
     * Returns an instance of a program with the supplied vertex and fragment shaders.
     */
    public Program getProgram (Shader vertex, Shader fragment)
    {
        ProgramKey key = new ProgramKey(vertex, fragment);
        Program program = _programs.get(key);
        if (program == null) {
            program = new Program(_ctx.getRenderer());
            if (!program.setShaders(vertex, fragment)) {
                log.warning(
                    "Error linking shader program.", "vertex", vertex,
                    "fragment", fragment, "log", program.getInfoLog());
                return null;
            }
            _programs.put(key, program);
        }
        return program;
    }

    /**
     * Identifies a loaded shader.
     */
    protected static class ShaderKey
    {
        /** The path of the shader. */
        public String path;

        /** The definitions and derived definitions. */
        public String[] defs, ddefs;

        public ShaderKey (String path, String[] defs, String[] ddefs)
        {
            this.path = path;
            this.defs = defs;
            this.ddefs = ddefs;
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return path.hashCode() ^ Arrays.hashCode(defs);
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            ShaderKey okey = (ShaderKey)other;
            return path.equals(okey.path) && Arrays.equals(defs, okey.defs);
        }
    }

    /**
     * Identifies a linked shader program.
     */
    protected static class ProgramKey
    {
        public ProgramKey (Shader vertex, Shader fragment)
        {
            _vertex = new WeakReference<Shader>(vertex);
            _fragment = new WeakReference<Shader>(fragment);
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return System.identityHashCode(_vertex.get()) ^
                System.identityHashCode(_fragment.get());
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            ProgramKey okey = (ProgramKey)other;
            return _vertex.get() == okey._vertex.get() && _fragment.get() == okey._fragment.get();
        }

        /** The vertex and fragment shaders. */
        protected WeakReference<Shader> _vertex, _fragment;
    }

    /** The shader cache. */
    protected Subcache<ShaderKey, Shader> _shaders = new Subcache<ShaderKey, Shader>() {
        protected Shader loadResource (ShaderKey key) {
            String path = key.path;
            String ext = path.substring(path.lastIndexOf('.') + 1);
            Integer type = _types.get(ext);
            if (type == null) {
                log.warning("Unknown shader extension.", "path", path);
                return null;
            }
            StringBuilder buf = new StringBuilder();
            appendDefs(buf, key.defs);
            appendDefs(buf, key.ddefs);
            buf.append(getSource(key.path));
            Shader shader = new Shader(_ctx.getRenderer(), type.intValue());
            if (!shader.setSource(buf.toString())) {
                log.warning(
                    "Error compiling shader.", "defs", key.defs, "ddefs",
                    key.ddefs, "path", path, "log", shader.getInfoLog());
                return null;
            }
            return shader;
        }
        protected String getResourcePath (ShaderKey key) {
            return key.path;
        }
        protected void appendDefs (StringBuilder buf, String[] defs) {
            for (String def : defs) {
                buf.append("#define ").append(def).append('\n');
            }
        }
    };

    /** The source file cache. */
    protected Subcache<String, String> _source = new Subcache<String, String>() {
        protected String loadResource (String path) {
            StringBuilder buf = new StringBuilder();
            try {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(_ctx.getResourceManager().getResource(path)));
                String line;
                while ((line = reader.readLine()) != null) {
                    buf.append(line).append('\n');
                }
                reader.close();
            } catch (IOException e) {
                log.warning("Failed to read shader source.", "path", path, e);
            }
            return buf.toString();
        }
        protected String getResourcePath (String path) {
            return path;
        }
    };

    /** The set of linked shader programs. */
    protected SoftCache<ProgramKey, Program> _programs = new SoftCache<ProgramKey, Program>();

    /** Maps file extensions to shader types. */
    protected static final HashMap<String, Integer> _types = new HashMap<String, Integer>();
    static {
        _types.put("vert", ARBVertexShader.GL_VERTEX_SHADER_ARB);
        _types.put("frag", ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);
    }
}
