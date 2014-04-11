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

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;

import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.Scripted;
import com.threerings.opengl.model.config.InfluenceFlagConfig;
import com.threerings.opengl.scene.SceneElement.TickPolicy;
import com.threerings.opengl.util.GlContext;

/**
 * A scripted model implementation.
 */
public class ScriptedConfig extends ModelConfig.Implementation
{
    /**
     * An action to perform after a specific time interval.
     */
    public static class TimeAction extends DeepObject
        implements Exportable
    {
        /** The time at which to perform the action. */
        @Editable(min=0, step=0.01)
        public float time;

        /** The expected duration of the action (or 0 for 'unknown'). */
        @Editable(min=0, step=0.01)
        public float duration;

        /** The action to perform. */
        @Editable
        public ActionConfig action = new ActionConfig.CallFunction();
    }

    /** The loop duration, or zero for unlooped. */
    @Editable(min=0.0, step=0.01, hgroup="l")
    public float loopDuration;

    /** A fixed amount by which to expand the bounds. */
    @Editable(min=0.0, step=0.01, hgroup="l")
    public float boundsExpansion;

    /** The model's tick policy. */
    @Editable
    public TickPolicy tickPolicy = TickPolicy.DEFAULT;

    /** The influences allowed to affect this model. */
    @Editable
    public InfluenceFlagConfig influences = new InfluenceFlagConfig(false);

    /** The actions to perform. */
    @Editable
    public TimeAction[] actions = new TimeAction[0];

    @Override
    public void preload (GlContext ctx)
    {
        for (TimeAction action : actions) {
            action.action.preload(ctx);
        }
    }

    @Override
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        if (impl instanceof Scripted) {
            ((Scripted)impl).setConfig(ctx, this);
        } else {
            impl = new Scripted(ctx, scope, this);
        }
        return impl;
    }
}
