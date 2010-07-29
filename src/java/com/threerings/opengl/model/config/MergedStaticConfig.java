//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.editor.Editable;
import com.threerings.expr.Scope;
import com.threerings.math.Box;
import com.threerings.math.FloatMath;
import com.threerings.math.Ray3D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.geometry.config.GeometryConfig;
import com.threerings.opengl.geometry.config.TransformedGeometry;
import com.threerings.opengl.material.config.GeometryMaterial;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.model.Articulated;
import com.threerings.opengl.model.CollisionMesh;
import com.threerings.opengl.model.Compound;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.Static;
import com.threerings.opengl.model.config.CompoundConfig.ComponentModel;
import com.threerings.opengl.model.config.ModelConfig.Imported.MaterialMapping;
import com.threerings.opengl.model.config.ModelConfig.MeshSet;
import com.threerings.opengl.model.config.ModelConfig.VisibleMesh;
import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.Log.*;

/**
 * A merged static implementation.
 */
public class MergedStaticConfig extends ModelConfig.Implementation
{
    /** The influences allowed to affect this model. */
    @Editable
    public InfluenceFlagConfig influences = new InfluenceFlagConfig();

    /** The component models. */
    @Editable
    public ComponentModel[] models = new ComponentModel[0];

    @Override // documentation inherited
    public void getUpdateReferences (ConfigReferenceSet refs)
    {
        for (ComponentModel cmodel : models) {
            refs.add(ModelConfig.class, cmodel.model);
        }
    }

    @Override // documentation inherited
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        Cached cached = (_cached == null) ? null : _cached.get();
        if (cached == null) {
            _cached = new SoftReference<Cached>(cached = createCached(ctx));
        }
        if (impl instanceof Static) {
            ((Static)impl).setConfig(cached.bounds, cached.collision,
                cached.gmats, influences.getFlags());
        } else {
            impl = new Static(ctx, scope, cached.bounds, cached.collision,
                cached.gmats, influences.getFlags());
        }
        return impl;
    }

    @Override // documentation inherited
    public void invalidate ()
    {
        _cached = null;
    }

    /**
     * (Re)generates the cached data.
     */
    protected Cached createCached (GlContext ctx)
    {
        // process the component models, mapping geometry by material
        ConfigManager cfgmgr = ctx.getConfigManager();
        IdentityHashMap<MaterialConfig, List<TransformedGeometry>> glists =
            Maps.newIdentityHashMap();
        Map<String, MaterialConfig> mmap = Maps.newHashMap();
        List<TransformedCollision> cmeshes = Lists.newArrayList();
        final Box bounds = new Box();
        for (ComponentModel cmodel : models) {
            ModelConfig config = cfgmgr.getConfig(ModelConfig.class, cmodel.model);
            ModelConfig.Implementation original = (config == null) ? null : config.getOriginal();
            MeshSet mset = null;
            if (original instanceof StaticConfig) {
                mset = ((StaticConfig)original).meshes;

            } else if (original instanceof StaticSetConfig) {
                StaticSetConfig ssconfig = (StaticSetConfig)original;
                if (ssconfig.model != null && ssconfig.meshes != null) {
                    mset = ssconfig.meshes.get(ssconfig.model);
                }
            } else if (original != null) {
                log.warning("Merged model not of static type.", "model", cmodel.model,
                    "class", original.getClass());
            }
            if (mset == null) {
                continue;
            }
            bounds.addLocal(mset.bounds.transform(cmodel.transform));
            if (mset.collision != null) {
                cmeshes.add(new TransformedCollision(mset.collision, cmodel.transform));
            }
            ModelConfig.Imported imported = (ModelConfig.Imported)original;
            for (VisibleMesh mesh : mset.visible) {
                String key = mesh.texture + "|" + mesh.tag;
                MaterialConfig material = Model.getMaterialConfig(
                    ctx, mesh.texture, mesh.tag, imported.materialMappings, mmap);
                List<TransformedGeometry> glist = glists.get(material);
                if (glist == null) {
                    glists.put(material, glist = Lists.newArrayList());
                }
                glist.add(new TransformedGeometry(mesh.geometry, cmodel.transform));
            }
            mmap.clear();
        }

        // merge geometry of the same material
        List<GeometryMaterial> gmats = Lists.newArrayList();
        for (Map.Entry<MaterialConfig, List<TransformedGeometry>> entry : glists.entrySet()) {
            MaterialConfig material = entry.getKey();
            List<TransformedGeometry> glist = entry.getValue();
            while (!glist.isEmpty()) {
                GeometryConfig merged = glist.get(0).geometry.merge(glist);
                if (merged != null) {
                    gmats.add(new GeometryMaterial(merged, material));
                } else {
                    glist.remove(0);
                }
            }
        }

        // create the combined collision mesh
        final TransformedCollision[] tcollisions = cmeshes.toArray(
            new TransformedCollision[cmeshes.size()]);
        CollisionMesh collision = new CollisionMesh() {
            @Override public Box getBounds () {
                return bounds;
            }
            @Override public boolean getIntersection (Ray3D ray, Vector3f result) {
                // check the component meshes (transforming the ray into their space and back out
                // again if we detect a hit)
                Vector3f closest = result;
                for (TransformedCollision tcoll : tcollisions) {
                    if (tcoll.bounds.intersects(ray) && tcoll.collision.getIntersection(
                            ray.transform(tcoll.invTransform), result)) {
                        tcoll.transform.transformPointLocal(result);
                        result = FloatMath.updateClosest(ray.getOrigin(), result, closest);
                    }
                }
                // if we ever changed the result reference, that means we hit something
                return (result != closest);
            }
        };
        return new Cached(bounds, collision, gmats.toArray(new GeometryMaterial[gmats.size()]));
    }

    /**
     * Contains the cached derived config bits.
     */
    protected static class Cached
    {
        /** The merged bounds. */
        public final Box bounds;

        /** The merged collision mesh. */
        public final CollisionMesh collision;

        /** The merged geometry/material pairs. */
        public final GeometryMaterial[] gmats;

        public Cached (Box bounds, CollisionMesh collision, GeometryMaterial[] gmats)
        {
            this.bounds = bounds;
            this.collision = collision;
            this.gmats = gmats;
        }
    }

    /**
     * Holds a collision mesh and associated transform.
     */
    protected static class TransformedCollision
    {
        /** The collision mesh. */
        public final CollisionMesh collision;

        /** The transform to apply to the mesh. */
        public final Transform3D transform;

        /** The inverse of the mesh transform. */
        public final Transform3D invTransform;

        /** The transformed bounds. */
        public final Box bounds;

        public TransformedCollision (CollisionMesh collision, Transform3D transform)
        {
            this.collision = collision;
            this.transform = transform;
            invTransform = transform.invert();
            bounds = collision.getBounds().transform(transform);
        }
    }

    /** The cached derived config bits. */
    @DeepOmit
    protected transient SoftReference<Cached> _cached;
}
