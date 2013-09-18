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

package com.threerings.opengl.material.config;

import com.samskivert.util.ArrayUtil;

import com.threerings.config.ConfigManager;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.ExpressionBinding;
import com.threerings.expr.Scope;
import com.threerings.expr.Updater;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Vector4f;
import com.threerings.util.DeepObject;

import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.renderer.config.AlphaStateConfig;
import com.threerings.opengl.renderer.config.AlphaStateConfig.DestBlendFactor;
import com.threerings.opengl.renderer.config.DepthStateConfig;
import com.threerings.opengl.renderer.config.TextureCoordGenConfig;
import com.threerings.opengl.renderer.config.TextureStateConfig;
import com.threerings.opengl.renderer.config.TextureUnitConfig;
import com.threerings.opengl.renderer.TextureUnit;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.state.TextureState;

/**
 * Used to transform material techniques.
 */
@EditorTypes({
    MaterialRewriter.DepthOnly.class, MaterialRewriter.Translucent.class,
    MaterialRewriter.Projection.class })
public abstract class MaterialRewriter extends DeepObject
    implements Exportable
{
    /**
     * Creates depth-only versions of input techniques (for shadow maps, e.g.)
     */
    public static class DepthOnly extends MaterialRewriter
    {
    }

    /**
     * Creates translucent versions of input techniques.
     */
    public static class Translucent extends MaterialRewriter
    {
        @Override
        protected TechniqueConfig.Enqueuer rewrite (TechniqueConfig.NormalEnqueuer enqueuer)
        {
            enqueuer = (TechniqueConfig.NormalEnqueuer)super.rewrite(enqueuer);
            enqueuer.queue = rewriteQueue(enqueuer.queue);
            return enqueuer;
        }

        @Override
        protected TechniqueConfig.Enqueuer rewrite (TechniqueConfig.GroupedEnqueuer enqueuer)
        {
            enqueuer = (TechniqueConfig.GroupedEnqueuer)super.rewrite(enqueuer);
            enqueuer.queue = rewriteQueue(enqueuer.queue);
            return enqueuer;
        }

        @Override
        protected AlphaStateConfig rewrite (AlphaStateConfig alphaState)
        {
            alphaState.testFunc = AlphaStateConfig.TestFunc.ALWAYS;
            if (alphaState.destBlendFactor == DestBlendFactor.ZERO) {
                alphaState.destBlendFactor = DestBlendFactor.ONE_MINUS_SRC_ALPHA;
            }
            return alphaState;
        }

        @Override
        protected DepthStateConfig rewrite (DepthStateConfig depthState)
        {
            depthState.mask = false;
            return depthState;
        }

        /**
         * Rewrites the specified queue reference.
         */
        protected String rewriteQueue (String queue)
        {
            return RenderQueue.OPAQUE.equals(queue) ? RenderQueue.TRANSPARENT : queue;
        }
    }

    /**
     * Creates projected versions of input techniques.
     */
    public static class Projection extends MaterialRewriter
    {
        /** Whether or not to enable generation for each texture coordinate. */
        @Editable(hgroup="t")
        public boolean s, t, r, q;

        @Override
        protected PassConfig rewrite (PassConfig pass)
        {
            pass = super.rewrite(pass);
            int nunits = pass.textureState.units.length;
            if (nunits == 0 || !(s || t || r || q)) {
                return pass;
            }
            // static binding sets coordinate vector references
            ExpressionBinding sbinding = new ExpressionBinding() {
                public Updater createUpdater (ConfigManager cfgmgr, Scope scope, Object object) {
                    final TextureState tstate =
                        (TextureState)((TechniqueConfig.StateContainer)object).states[
                            RenderState.TEXTURE_STATE];
                    final Vector4f genPlaneS =
                        s ? ScopeUtil.resolve(scope, "genPlaneS", null, Vector4f.class) : null;
                    final Vector4f genPlaneT =
                        t ? ScopeUtil.resolve(scope, "genPlaneT", null, Vector4f.class) : null;
                    final Vector4f genPlaneR =
                        r ? ScopeUtil.resolve(scope, "genPlaneR", null, Vector4f.class) : null;
                    final Vector4f genPlaneQ =
                        q ? ScopeUtil.resolve(scope, "genPlaneQ", null, Vector4f.class) : null;
                    return new Updater() {
                        public void update () {
                            for (TextureUnit unit : tstate.getUnits()) {
                                unit.genPlaneS = (genPlaneS == null) ? unit.genPlaneS : genPlaneS;
                                unit.genPlaneT = (genPlaneT == null) ? unit.genPlaneT : genPlaneT;
                                unit.genPlaneR = (genPlaneR == null) ? unit.genPlaneR : genPlaneR;
                                unit.genPlaneQ = (genPlaneQ == null) ? unit.genPlaneQ : genPlaneQ;
                            }
                        }
                    };
                }
            };
            pass.staticBindings = ArrayUtil.append(pass.staticBindings, sbinding);

            // dynamic binding sets dirty flags
            ExpressionBinding dbinding = new ExpressionBinding() {
                public Updater createUpdater (ConfigManager cfgmgr, Scope scope, Object object) {
                    final TextureState tstate =
                        (TextureState)((TechniqueConfig.StateContainer)object).states[
                            RenderState.TEXTURE_STATE];
                    return new Updater() {
                        public void update () {
                            for (TextureUnit unit : tstate.getUnits()) {
                                unit.dirty = true;
                            }
                            tstate.setDirty(true);
                        }
                    };
                }
            };
            pass.dynamicBindings = ArrayUtil.append(pass.dynamicBindings, dbinding);
            return pass;
        }

        @Override
        protected TextureStateConfig rewrite (TextureStateConfig textureState)
        {
            textureState = super.rewrite(textureState);
            textureState.uniqueInstance = true;
            return textureState;
        }

        @Override
        protected TextureUnitConfig rewrite (TextureUnitConfig textureUnit)
        {
            textureUnit.coordGenS =
                s ? new TextureCoordGenConfig.EyeLinear() : textureUnit.coordGenS;
            textureUnit.coordGenT =
                t ? new TextureCoordGenConfig.EyeLinear() : textureUnit.coordGenT;
            textureUnit.coordGenR =
                r ? new TextureCoordGenConfig.EyeLinear() : textureUnit.coordGenR;
            textureUnit.coordGenQ =
                q ? new TextureCoordGenConfig.EyeLinear() : textureUnit.coordGenQ;
            return textureUnit;
        }
    }

    /**
     * Returns a new technique that is the rewritten version of the technique supplied.
     */
    public TechniqueConfig rewrite (TechniqueConfig technique)
    {
        TechniqueConfig rewritten = (TechniqueConfig)technique.clone();
        rewritten.enqueuer = rewritten.enqueuer.rewrite(this);
        return rewritten;
    }

    /**
     * Rewrites the specified enqueuer.
     */
    protected TechniqueConfig.Enqueuer rewrite (TechniqueConfig.NormalEnqueuer enqueuer)
    {
        PassConfig[] passes = enqueuer.passes;
        for (int ii = 0; ii < passes.length; ii++) {
            passes[ii] = rewrite(passes[ii]);
        }
        return enqueuer;
    }

    /**
     * Rewrites the specified enqueuer.
     */
    protected TechniqueConfig.Enqueuer rewrite (TechniqueConfig.CompoundEnqueuer enqueuer)
    {
        TechniqueConfig.Enqueuer[] enqueuers = enqueuer.enqueuers;
        for (int ii = 0; ii < enqueuers.length; ii++) {
            enqueuers[ii] = enqueuers[ii].rewrite(this);
        }
        return enqueuer;
    }

    /**
     * Rewrites the specified enqueuer.
     */
    protected TechniqueConfig.Enqueuer rewrite (TechniqueConfig.GroupedEnqueuer enqueuer)
    {
        return rewrite((TechniqueConfig.CompoundEnqueuer)enqueuer);
    }

    /**
     * Rewrites the specified pass.
     */
    protected PassConfig rewrite (PassConfig pass)
    {
        pass.alphaState = rewrite(pass.alphaState);
        pass.depthState = rewrite(pass.depthState);
        pass.textureState = rewrite(pass.textureState);
        return pass;
    }

    /**
     * Rewrites the specified state.
     */
    protected AlphaStateConfig rewrite (AlphaStateConfig alphaState)
    {
        return alphaState;
    }

    /**
     * Rewrites the specified state.
     */
    protected DepthStateConfig rewrite (DepthStateConfig depthState)
    {
        return depthState;
    }

    /**
     * Rewrites the specified state.
     */
    protected TextureStateConfig rewrite (TextureStateConfig textureState)
    {
        TextureUnitConfig[] units = textureState.units;
        for (int ii = 0; ii < units.length; ii++) {
            units[ii] = rewrite(units[ii]);
        }
        return textureState;
    }

    /**
     * Rewrites the specified texture unit.
     */
    protected TextureUnitConfig rewrite (TextureUnitConfig textureUnit)
    {
        return textureUnit;
    }
}
