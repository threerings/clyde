//
// $Id$

package com.threerings.opengl.model.config;

import java.util.TreeSet;

import com.samskivert.util.ComparableTuple;

import com.threerings.expr.Scope;
import com.threerings.util.Shallow;

import com.threerings.opengl.mod.Model;
import com.threerings.opengl.mod.Static;
import com.threerings.opengl.model.config.ModelConfig.MeshSet;
import com.threerings.opengl.model.tools.ModelDef;
import com.threerings.opengl.util.GlContext;

/**
 * An original static implementation.
 */
public class StaticConfig extends ModelConfig.Imported
{
    /** The meshes comprising this model. */
    @Shallow
    public MeshSet meshes;

    @Override // documentation inherited
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        if (meshes == null) {
            return null;
        }
        if (impl instanceof Static) {
            ((Static)impl).setConfig(meshes, materialMappings);
        } else {
            if (impl != null) {
                impl.dispose();
            }
            impl = new Static(ctx, scope, meshes, materialMappings);
        }
        return impl;
    }

    @Override // documentation inherited
    protected void updateFromSource (ModelDef def)
    {
        if (def == null) {
            meshes = null;
        } else {
            def.update(this);
        }
    }

    @Override // documentation inherited
    protected void getTextures (TreeSet<String> textures)
    {
        if (meshes != null) {
            meshes.getTextures(textures);
        }
    }

    @Override // documentation inherited
    protected void getTextureTagPairs (TreeSet<ComparableTuple<String, String>> pairs)
    {
        if (meshes != null) {
            meshes.getTextureTagPairs(pairs);
        }
    }
}
