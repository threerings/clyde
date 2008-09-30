//
// $Id$

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
        @Override // documentation inherited
        public boolean isCompatibleWith (RenderSchemeConfig other)
        {
            return other == null || other.implementation instanceof Normal;
        }

        @Override // documentation inherited
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

        @Override // documentation inherited
        public boolean isCompatibleWith (RenderSchemeConfig other)
        {
            return false;
        }

        @Override // documentation inherited
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
