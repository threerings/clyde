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

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.expr.BooleanExpression;
import com.threerings.expr.Scope;
import com.threerings.math.Transform3D;
import com.threerings.util.DeepObject;

import com.threerings.opengl.model.Conditional;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.scene.SceneElement.TickPolicy;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Preloadable;

/**
 * A conditional model implementation.
 */
public class ConditionalConfig extends ModelConfig.Implementation
{
    /**
     * Represents one of the cases that makes up the conditional.
     */
    public static class Case extends DeepObject
        implements Exportable
    {
        /** The condition for the case. */
        @Editable
        public BooleanExpression condition = new BooleanExpression.Constant(true);

        /** The model reference. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        /** The model transform. */
        @Editable(step=0.01)
        public Transform3D transform = new Transform3D();
    }

    /** The model's tick policy. */
    @Editable
    public TickPolicy tickPolicy = TickPolicy.DEFAULT;

    /** The influences allowed to affect this model. */
    @Editable
    public InfluenceFlagConfig influences = new InfluenceFlagConfig(true);

    /** The condition cases. */
    @Editable
    public Case[] cases = new Case[0];

    /** The default model reference. */
    @Editable(nullable=true)
    public ConfigReference<ModelConfig> defaultModel;

    /** The default model transform. */
    @Editable(step=0.01)
    public Transform3D defaultTransform = new Transform3D();

    @Override
    public void preload (GlContext ctx)
    {
        new Preloadable.Model(defaultModel).preload(ctx);
        for (Case condition : cases) {
            new Preloadable.Model(condition.model).preload(ctx);
        }
    }

    @Override
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        if (impl instanceof Conditional) {
            ((Conditional)impl).setConfig(ctx, this);
        } else {
            impl = new Conditional(ctx, scope, this);
        }
        return impl;
    }

    @Override
    public void invalidate ()
    {
        for (Case caze : cases) {
            caze.condition.invalidate();
        }
    }
}
