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

package com.threerings.opengl.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.lang.ref.WeakReference;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBVertexShader;

import com.samskivert.util.ArrayUtil;

import com.threerings.util.CacheUtil;

import com.threerings.opengl.renderer.Program;
import com.threerings.opengl.renderer.Shader;
import com.threerings.opengl.renderer.ShaderObject;

import static com.threerings.opengl.Log.log;

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
        return getShader(path, defs, ArrayUtil.EMPTY_STRING);
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
            maybeCheckLog(program, "vertex", vertex, "fragment", fragment);
            _programs.put(key, program);
        }
        return program;
    }

    /**
     * If so configured, checks the specified log for the dreaded "software" keyword.
     */
    protected static void maybeCheckLog (ShaderObject object, Object... args)
    {
        if (!CHECK_LOGS) {
            return;
        }
        String infolog = object.getInfoLog();
        if (infolog.length() > 255 || infolog.toLowerCase().contains("software")) {
            log.warning("Possibly handling shader in software.",
                ArrayUtil.concatenate(args, new Object[] { "log", infolog }));
        }
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

        @Override
        public int hashCode ()
        {
            return path.hashCode() ^ Arrays.hashCode(defs);
        }

        @Override
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

        @Override
        public int hashCode ()
        {
            return System.identityHashCode(_vertex.get()) ^
                System.identityHashCode(_fragment.get());
        }

        @Override
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
            Integer type = TYPES.get(ext);
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
            maybeCheckLog(shader, "defs", key.defs, "ddefs", key.ddefs, "path", path);
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
    protected Map<ProgramKey, Program> _programs = CacheUtil.softValues();

    /** Maps file extensions to shader types. */
    protected static final Map<String, Integer> TYPES = ImmutableMap.of(
        "vert", ARBVertexShader.GL_VERTEX_SHADER_ARB,
        "frag", ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);

    /** Whether or not we should check the logs even if the shader compiles/links successfully. */
    protected static final boolean CHECK_LOGS = true;
}
