//
// $Id$

package com.threerings.opengl.model.config;

import java.util.TreeMap;
import java.util.TreeSet;

import com.samskivert.util.ComparableTuple;

import com.threerings.editor.Editable;
import com.threerings.expr.Scope;
import com.threerings.util.Shallow;

import com.threerings.opengl.mod.Model;
import com.threerings.opengl.mod.Static;
import com.threerings.opengl.model.config.ModelConfig.MeshSet;
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
    public String[] getModelOptions ()
    {
        return (meshes == null) ?
            new String[0] : meshes.keySet().toArray(new String[meshes.size()]);
    }

    @Override // documentation inherited
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        MeshSet mset = (meshes == null) ? null : meshes.get(model);
        if (mset == null) {
            return null;
        }
        if (impl instanceof Static) {
            ((Static)impl).setConfig(mset, materialMappings);
        } else {
            impl = new Static(ctx, scope, mset, materialMappings);
        }
        return impl;
    }

    @Override // documentation inherited
    protected void updateFromSource (ModelDef def)
    {
        if (def == null) {
            model = null;
            meshes = null;
        } else {
            def.update(this);
        }
    }

    @Override // documentation inherited
    protected void getTextures (TreeSet<String> textures)
    {
        if (meshes != null) {
            for (MeshSet set : meshes.values()) {
                set.getTextures(textures);
            }
        }
    }

    @Override // documentation inherited
    protected void getTextureTagPairs (TreeSet<ComparableTuple<String, String>> pairs)
    {
        if (meshes != null) {
            for (MeshSet set : meshes.values()) {
                set.getTextureTagPairs(pairs);
            }
        }
    }
}
