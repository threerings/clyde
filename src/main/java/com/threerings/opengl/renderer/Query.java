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

package com.threerings.opengl.renderer;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBOcclusionQuery;
import org.lwjgl.opengl.GL11;

/**
 * An OpenGL occlusion query object.
 */
public abstract class Query
{
    /**
     * Queries the number of samples passed.
     */
    public static class SamplesPassed extends Query
    {
        /**
         * Creates a new samples-passed query for the specified renderer.
         */
        public SamplesPassed (Renderer renderer)
        {
            super(renderer, ARBOcclusionQuery.GL_SAMPLES_PASSED_ARB);
        }
    }

    /**
     * Creates a new query for the specified renderer.
     */
    public Query (Renderer renderer, int target)
    {
        _renderer = renderer;
        IntBuffer idbuf = BufferUtils.createIntBuffer(1);
        ARBOcclusionQuery.glGenQueriesARB(idbuf);
        _id = idbuf.get(0);
        _target = target;
    }

    /**
     * Returns this query's OpenGL identifier.
     */
    public final int getId ()
    {
        return _id;
    }

    /**
     * Returns the query's target.
     */
    public final int getTarget ()
    {
        return _target;
    }

    /**
     * Determines whether the result of the query would be available without blocking.
     */
    public boolean isResultAvailable ()
    {
        ARBOcclusionQuery.glGetQueryObjectARB(
            _id, ARBOcclusionQuery.GL_QUERY_RESULT_AVAILABLE_ARB, _result);
        return _result.get(0) == GL11.GL_TRUE;
    }

    /**
     * Retrieves and returns the result of this query.
     */
    public int getResult ()
    {
        ARBOcclusionQuery.glGetQueryObjectARB(
            _id, ARBOcclusionQuery.GL_QUERY_RESULT_ARB, _result);
        return _result.get(0);
    }

    @Override
    protected void finalize ()
        throws Throwable
    {
        super.finalize();
        if (_id > 0) {
            _renderer.queryFinalized(_id);
        }
    }

    /** The renderer responsible for this query. */
    protected Renderer _renderer;

    /** The OpenGL identifier for this query. */
    protected int _id;

    /** The query target. */
    protected int _target;

    /** Stores results. */
    protected IntBuffer _result = BufferUtils.createIntBuffer(1);
}
