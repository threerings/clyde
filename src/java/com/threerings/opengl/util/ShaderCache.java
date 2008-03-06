//
// $Id$

package com.threerings.opengl.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBVertexShader;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.SoftCache;

import com.threerings.opengl.renderer.Program;
import com.threerings.opengl.renderer.Shader;

import static com.threerings.opengl.Log.*;

/**
 * Caches loaded shaders and shader programs.
 */
public class ShaderCache
{
    /**
     * Creates a new shader cache.
     */
    public ShaderCache (GlContext ctx)
    {
        _ctx = ctx;
    }

    /**
     * Loads and returns the shader at the specified path.
     */
    public Shader getShader (String path)
    {
        return getShader(path, null, null);
    }

    /**
     * Loads and returns the shader at the specified path with the supplied preprocessor
     * definitions.
     */
    public Shader getShader (String path, String... defs)
    {
        return getShader(path, defs, null);
    }

    /**
     * Loads and returns the shader at the specified path with the supplied preprocessor
     * definitions as well as a set of derived definitions (not used to find the shader,
     * but included if the shader is compiled).
     */
    public Shader getShader (String path, String[] defs, String[] ddefs)
    {
        ShaderKey key = new ShaderKey(path, defs);
        Shader shader = _shaders.get(key);
        if (shader == null) {
            String ext = path.substring(path.lastIndexOf('.') + 1);
            Integer type = _types.get(ext);
            if (type == null) {
                log.warning("Unknown shader extension [path=" + path + "].");
                return null;
            }
            String source = readSource(path, safeConcatenate(defs, ddefs));
            if (source == null) {
                return null;
            }
            shader = new Shader(_ctx.getRenderer(), type.intValue());
            if (!shader.setSource(source)) {
                log.warning("Error compiling shader [path=" + path + ", log=" +
                    shader.getInfoLog() + "].");
                return null;
            }
            _shaders.put(key, shader);
        }
        return shader;
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
                log.warning("Error linking shader program [vertex=" + vertex + ", fragment=" +
                    fragment + ", log=" + program.getInfoLog() + "].");
                return null;
            }
            _programs.put(key, program);
        }
        return program;
    }

    /**
     * Reads the shader at the specified path, prepending the supplied preprocessor
     * definitions.
     */
    protected String readSource (String path, String[] defs)
    {
        StringBuffer buf = new StringBuffer();
        if (defs != null) {
            for (String def : defs) {
                buf.append("#define ").append(def).append('\n');
            }
        }
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(_ctx.getResourceManager().getResource("shader/" + path)));
            String line;
            while ((line = reader.readLine()) != null) {
                buf.append(line).append('\n');
            }
            reader.close();
        } catch (IOException e) {
            log.warning("Error reading shader [path=" + path + ", error=" + e + "].");
            return null;
        }
        return buf.toString();
    }

    /**
     * Concatenates two arrays, either or both of which may be <code>null</code>.
     */
    protected static String[] safeConcatenate (String[] a1, String[] a2)
    {
        return (a1 == null) ? a2 : (a2 == null ? a1 : ArrayUtil.concatenate(a1, a2));
    }

    /**
     * Identifies a loaded shader.
     */
    protected static class ShaderKey
    {
        /** The path of the shader. */
        public String path;

        /** The set of preprocessor definitions. */
        public HashSet<String> defs = new HashSet<String>();

        public ShaderKey (String path, String[] defs)
        {
            this.path = path;
            if (defs != null) {
                Collections.addAll(this.defs, defs);
            }
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return path.hashCode() ^ defs.hashCode();
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            ShaderKey okey = (ShaderKey)other;
            return path.equals(okey.path) && defs.equals(okey.defs);
        }
    }

    /**
     * Identifies a linked shader program.
     */
    protected static class ProgramKey
    {
        /** The vertex and fragment shaders. */
        public Shader vertex, fragment;

        public ProgramKey (Shader vertex, Shader fragment)
        {
            this.vertex = vertex;
            this.fragment = fragment;
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return (vertex == null ? 0 : vertex.hashCode()) ^
                (fragment == null ? 0 : fragment.hashCode());
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            ProgramKey okey = (ProgramKey)other;
            return ObjectUtil.equals(vertex, okey.vertex) &&
                ObjectUtil.equals(fragment, okey.fragment);
        }
    }

    /** The renderer context. */
    protected GlContext _ctx;

    /** The set of compiled shaders. */
    protected SoftCache<ShaderKey, Shader> _shaders = new SoftCache<ShaderKey, Shader>();

    /** The set of linked shader programs. */
    protected SoftCache<ProgramKey, Program> _programs = new SoftCache<ProgramKey, Program>();

    /** Maps file extensions to shader types. */
    protected static final HashMap<String, Integer> _types = new HashMap<String, Integer>();
    static {
        _types.put("vert", ARBVertexShader.GL_VERTEX_SHADER_ARB);
        _types.put("frag", ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);
    }
}
