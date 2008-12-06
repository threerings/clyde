//
// $Id$

package com.threerings.opengl.scene.config;

import com.threerings.editor.Editable;
import com.threerings.expr.Scope;

import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.scene.ViewerAffecter;
import com.threerings.opengl.util.GlContext;

/**
 * A viewer affecter implementation.
 */
public class ViewerAffecterConfig extends ModelConfig.Implementation
{
    /** The effect that this affecter exerts. */
    @Editable
    public ViewerEffectConfig effect = new ViewerEffectConfig.Sound();

    /** The extent of the effect. */
    @Editable
    public Extent extent = new Extent.Limited();

    @Override // documentation inherited
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        if (impl instanceof ViewerAffecter) {
            ((ViewerAffecter)impl).setConfig(this);
        } else {
            impl = new ViewerAffecter(ctx, scope, this);
        }
        return impl;
    }
}
