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

package com.threerings.opengl.gui.config;

import com.threerings.config.Reference;
import com.threerings.editor.Editable;
import com.threerings.expr.Scope;

import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.compositor.config.RenderQueueConfig;
import com.threerings.opengl.gui.ComponentBillboard;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ArticulatedConfig.BillboardRotationX;
import com.threerings.opengl.model.config.ArticulatedConfig.BillboardRotationY;
import com.threerings.opengl.model.config.InfluenceFlagConfig;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.renderer.config.AlphaStateConfig;
import com.threerings.opengl.renderer.config.DepthStateConfig;
import com.threerings.opengl.util.GlContext;

/**
 * A component billboard model implementation.
 */
public class ComponentBillboardConfig extends ModelConfig.Implementation
{
    /** The message bundle to use for translations (or the empty string for the default). */
    @Editable(hgroup="b")
    public String bundle = "";

    /** A uniform scale to apply. */
    @Editable(min=0.0, step=0.001, hgroup="b")
    public float scale = 0.01f;

    /** The root component. */
    @Editable
    public ComponentConfig root = new ComponentConfig.Spacer();

    /** Whether or not rotation is enabled. */
    @Editable
    public boolean rotationEnabled = true;

    /** The x rotation mode. */
    @Editable(hgroup="r")
    public BillboardRotationX rotationX = BillboardRotationX.ALIGN_TO_VIEW;

    /** The y rotation mode. */
    @Editable(hgroup="r")
    public BillboardRotationY rotationY = BillboardRotationY.ALIGN_TO_VIEW;

    /** The queue into which we render. */
    @Editable(nullable=true, hgroup="q")
    @Reference(RenderQueueConfig.class)
    public String queue = RenderQueue.TRANSPARENT;

    /** The priority at which the batch is enqueued. */
    @Editable(hgroup="q")
    public int priority;

    /** The alpha state to use in this pass. */
    @Editable
    public AlphaStateConfig alphaState = new AlphaStateConfig();

    /** The depth state to use. */
    @Editable
    public DepthStateConfig depthState = new DepthStateConfig();

    /** The influences allowed to affect this model. */
    @Editable
    public InfluenceFlagConfig influences = new InfluenceFlagConfig(false);

    /**
     * Default constructor.
     */
    public ComponentBillboardConfig ()
    {
        // set up the default states
        alphaState.destBlendFactor = AlphaStateConfig.DestBlendFactor.ONE_MINUS_SRC_ALPHA;
        depthState.mask = false;
    }

    @Override
    public void preload (GlContext ctx)
    {
        // Do nothing
    }

    @Override
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        if (impl instanceof ComponentBillboard) {
            ((ComponentBillboard)impl).setConfig(ctx, this);
        } else {
            impl = new ComponentBillboard(ctx, scope, this);
        }
        return impl;
    }
}
