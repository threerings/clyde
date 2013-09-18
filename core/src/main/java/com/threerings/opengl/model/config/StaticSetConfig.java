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

import java.util.TreeMap;
import java.util.TreeSet;

import proguard.annotation.Keep;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ComparableTuple;

import com.threerings.editor.Editable;
import com.threerings.expr.Scope;
import com.threerings.util.DeepOmit;
import com.threerings.util.Shallow;

import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.Static;
import com.threerings.opengl.model.config.ModelConfig.MeshSet;
import com.threerings.opengl.model.config.ModelConfig.VisibleMesh;
import com.threerings.opengl.model.config.StaticConfig.Resolved;
import com.threerings.opengl.model.tools.ModelDef;
import com.threerings.opengl.util.GlContext;

/**
 * An original static set implementation.
 */
public class StaticSetConfig extends ModelConfig.Imported
{
    /** The selected model. */
    @Editable(editor="choice", depends={"source"})
    public String model;

    /** Maps top-level node names to meshes. */
    @Shallow
    public TreeMap<String, MeshSet> meshes;

    /**
     * Returns the options for the model field.
     */
    @Keep
    public String[] getModelOptions ()
    {
        return ((meshes == null) || meshes.isEmpty()) ?
            ArrayUtil.EMPTY_STRING : meshes.keySet().toArray(new String[meshes.size()]);
    }

    @Override
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        Resolved resolved = (_resolved == null) ? null : _resolved.get();
        if (resolved == null) {
            MeshSet mset = (model == null || meshes == null) ? null : meshes.get(model);
            if (mset == null) {
                return null;
            }
            _resolved = new SoftReference<Resolved>(resolved = new Resolved(
                mset.bounds, mset.collision,
                getGeometryMaterials(ctx, mset.visible, materialMappings),
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
        MeshSet mset = (model == null || meshes == null) ? null : meshes.get(model);
        return (mset == null || mset.visible.length == 0) ? null : mset.visible[0];
    }

    @Override
    protected void updateFromSource (ModelDef def)
    {
        if (def == null) {
            model = null;
            meshes = null;
        } else {
            def.update(this);
        }
    }

    @Override
    protected void getTextures (TreeSet<String> textures)
    {
        if (meshes != null) {
            for (MeshSet set : meshes.values()) {
                set.getTextures(textures);
            }
        }
    }

    @Override
    protected void getTextureTagPairs (TreeSet<ComparableTuple<String, String>> pairs)
    {
        if (meshes != null) {
            for (MeshSet set : meshes.values()) {
                set.getTextureTagPairs(pairs);
            }
        }
    }

    /** The cached resolved config bits. */
    @DeepOmit
    protected transient SoftReference<Resolved> _resolved;
}
