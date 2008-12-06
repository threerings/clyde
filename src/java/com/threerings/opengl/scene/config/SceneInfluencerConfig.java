//
// $Id$

package com.threerings.opengl.scene.config;

import com.threerings.editor.Editable;
import com.threerings.expr.Scope;

import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.scene.SceneInfluencer;
import com.threerings.opengl.util.GlContext;

/**
 * A scene influencer implementation.
 */
public class SceneInfluencerConfig extends ModelConfig.Implementation
{
    /** The influence that this influencer exerts. */
    @Editable
    public SceneInfluenceConfig influence = new SceneInfluenceConfig.AmbientLight();

    /** The extent of the influence. */
    @Editable
    public Extent extent = new Extent.Limited();

    @Override // documentation inherited
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        if (impl instanceof SceneInfluencer) {
            ((SceneInfluencer)impl).setConfig(this);
        } else {
            impl = new SceneInfluencer(ctx, scope, this);
        }
        return impl;
    }
}
