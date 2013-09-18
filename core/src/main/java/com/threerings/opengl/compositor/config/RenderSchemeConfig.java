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

package com.threerings.opengl.compositor.config;

import com.threerings.config.ManagedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.material.config.MaterialRewriter;

/**
 * The configuration of a render scheme.
 */
public class RenderSchemeConfig extends ManagedConfig
{
    /** Used to indicate that a config reference is invalid. */
    public static final RenderSchemeConfig INVALID = new RenderSchemeConfig();

    /**
     * The actual implementation of the scheme.
     */
    @EditorTypes({ Normal.class, Special.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Checks this scheme for compatibility with another.
         */
        public abstract boolean isCompatibleWith (RenderSchemeConfig other);

        /**
         * Returns the material rewriter for this scheme, if any.
         */
        public abstract MaterialRewriter getMaterialRewriter ();
    }

    /**
     * A "normal" implementation.  Normal schemes are compatible with all other normal schemes and
     * the null (default) scheme.
     */
    public static class Normal extends Implementation
    {
        @Override
        public boolean isCompatibleWith (RenderSchemeConfig other)
        {
            return other == null || other.implementation instanceof Normal;
        }

        @Override
        public MaterialRewriter getMaterialRewriter ()
        {
            return null;
        }
    }

    /**
     * A "special" implementation.  Special schemes are not compatible with any other schemes.
     */
    public static class Special extends Implementation
    {
        /** An optional "rewriter" that will transform normal material techniques into techniques for
         * this scheme in the absence of an explicitly defined technique. */
        @Editable(nullable=true)
        public MaterialRewriter materialRewriter;

        @Override
        public boolean isCompatibleWith (RenderSchemeConfig other)
        {
            return false;
        }

        @Override
        public MaterialRewriter getMaterialRewriter ()
        {
            return materialRewriter;
        }
    }

    /** The actual implementation of the scheme. */
    @Editable
    public Implementation implementation = new Normal();

    /**
     * Checks this scheme for compatibility with another.
     */
    public boolean isCompatibleWith (RenderSchemeConfig other)
    {
        return implementation.isCompatibleWith(other);
    }

    /**
     * Returns the material rewriter for this scheme, if any.
     */
    public MaterialRewriter getMaterialRewriter ()
    {
        return implementation.getMaterialRewriter();
    }
}
