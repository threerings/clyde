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

package com.threerings.opengl.model.config;

import java.lang.ref.SoftReference;

import java.util.TreeSet;

import com.samskivert.util.ComparableTuple;

import com.threerings.expr.Scope;
import com.threerings.math.Box;
import com.threerings.util.DeepOmit;
import com.threerings.util.Shallow;

import com.threerings.opengl.material.config.GeometryMaterial;
import com.threerings.opengl.model.CollisionMesh;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.Static;
import com.threerings.opengl.model.config.ModelConfig.MeshSet;
import com.threerings.opengl.model.config.ModelConfig.VisibleMesh;
import com.threerings.opengl.model.tools.ModelDef;
import com.threerings.opengl.util.GlContext;

/**
 * An original static implementation.
 */
public class StaticConfig extends ModelConfig.Imported
{
    /**
     * Contains the resolved derived config bits.
     */
    public static class Resolved
    {
        /** The merged bounds. */
        public final Box bounds;

        /** The merged collision mesh. */
        public final CollisionMesh collision;

        /** The merged geometry/material pairs. */
        public final GeometryMaterial[] gmats;

        /** The merged influence flags. */
        public final int influenceFlags;

        public Resolved (
            Box bounds, CollisionMesh collision, GeometryMaterial[] gmats, int influenceFlags)
        {
            this.bounds = bounds;
            this.collision = collision;
            this.gmats = gmats;
            this.influenceFlags = influenceFlags;
        }
    }

    /** The meshes comprising this model. */
    @Shallow
    public MeshSet meshes;

    @Override
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        if (meshes == null) {
            return null;
        }
        Resolved resolved = (_resolved == null) ? null : _resolved.get();
        if (resolved == null) {
            _resolved = new SoftReference<Resolved>(resolved = new Resolved(
                meshes.bounds, meshes.collision,
                getGeometryMaterials(ctx, meshes.visible, materialMappings),
                influences.getFlags()));
        }
        if (impl instanceof Static) {
            ((Static)impl).setConfig(ctx, resolved);
        } else {
            impl = new Static(ctx, scope, resolved);
        }
        return impl;
    }

    @Override
    public void invalidate ()
    {
        _resolved = null;
    }

    @Override
    protected VisibleMesh getParticleMesh ()
    {
        return (meshes == null || meshes.visible.length == 0) ? null : meshes.visible[0];
    }

    @Override
    protected void updateFromSource (ModelDef def)
    {
        if (def == null) {
            meshes = null;
        } else {
            def.update(this);
        }
    }

    @Override
    protected void getTextures (TreeSet<String> textures)
    {
        if (meshes != null) {
            meshes.getTextures(textures);
        }
    }

    @Override
    protected void getTextureTagPairs (TreeSet<ComparableTuple<String, String>> pairs)
    {
        if (meshes != null) {
            meshes.getTextureTagPairs(pairs);
        }
    }

    /** The cached resolved config bits. */
    @DeepOmit
    protected transient SoftReference<Resolved> _resolved;
}
