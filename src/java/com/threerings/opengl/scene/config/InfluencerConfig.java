//
// $Id$

package com.threerings.opengl.scene.config;

import com.threerings.editor.Editable;
import com.threerings.expr.Scope;

import com.threerings.opengl.mod.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.scene.Influencer;
import com.threerings.opengl.util.GlContext;

/**
 * An influencer implementation.
 */
public class InfluencerConfig extends ModelConfig.Implementation
{
    /** The influence that this influencer exerts. */
    @Editable
    public SceneInfluenceConfig influence = new SceneInfluenceConfig.AmbientLight();

    @Override // documentation inherited
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        if (impl instanceof Influencer) {
            ((Influencer)impl).setConfig(this);
        } else {
            if (impl != null) {
                impl.dispose();
            }
            impl = new Influencer(ctx, scope, this);
        }
        return impl;
    }
}
