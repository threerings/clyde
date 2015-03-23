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

package com.threerings.opengl.compositor.config;

import java.util.ArrayList;

import com.google.common.base.Objects;

import com.threerings.config.BoundConfig;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.Reference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.util.GlContext;

/**
 * Describes a render effect.
 */
public class RenderEffectConfig extends BoundConfig
{
    /**
     * Contains the actual implementation of the effect.
     */
    @EditorTypes({ Original.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        @Deprecated
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            // nothing. Deprecated.
        }

        /**
         * Gets the priority of the effect.
         */
        public abstract int getPriority (GlContext ctx);

        /**
         * Returns a technique to use to render this effect.
         */
        public abstract Technique getTechnique (GlContext ctx, String scheme);

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            // nothing by default
        }
    }

    /**
     * An original implementation.
     */
    public static class Original extends Implementation
    {
        /** The effect priority. */
        @Editable
        public int priority;

        /** The techniques available to render the effect. */
        @Editable
        public Technique[] techniques = new Technique[0];

        @Override
        public int getPriority (GlContext ctx)
        {
            return priority;
        }

        @Override
        public Technique getTechnique (GlContext ctx, String scheme)
        {
            // first look for an exact match for the scheme
            Technique[] processed = getProcessedTechniques(ctx);
            for (Technique technique : processed) {
                if (Objects.equal(technique.scheme, scheme)) {
                    return technique;
                }
            }

            // then look for a compatible match
            RenderSchemeConfig sconfig = (scheme == null) ?
                null : ctx.getConfigManager().getConfig(RenderSchemeConfig.class, scheme);
            for (Technique technique : processed) {
                RenderSchemeConfig tconfig = technique.getSchemeConfig(ctx);
                if ((sconfig == null) ? (tconfig == null || tconfig.isCompatibleWith(sconfig)) :
                        sconfig.isCompatibleWith(tconfig)) {
                    return technique;
                }
            }
            return null;
        }

        @Override
        public void invalidate ()
        {
            _processedTechniques = null;
            for (Technique technique : techniques) {
                technique.invalidate();
            }
        }

        /**
         * Returns the lazily-constructed list of processed techniques.
         */
        protected Technique[] getProcessedTechniques (GlContext ctx)
        {
            if (_processedTechniques == null) {
                ArrayList<Technique> list = new ArrayList<Technique>(techniques.length);
                ArrayList<Technique> fallbacks = new ArrayList<Technique>(0);
                for (Technique technique : techniques) {
                    // first try without fallbacks, then with
                    Technique processed = technique.process(ctx, false);
                    if (processed == null) {
                        Technique fallback = technique.process(ctx, true);
                        if (fallback != null) {
                            fallbacks.add(fallback);
                        }
                    } else {
                        list.add(processed);
                    }
                }
                list.addAll(fallbacks);
                _processedTechniques = list.toArray(new Technique[list.size()]);
            }
            return _processedTechniques;
        }

        /** The cached list of supported techniques. */
        @DeepOmit
        protected transient Technique[] _processedTechniques;
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The effect reference. */
        @Editable(nullable=true)
        public ConfigReference<RenderEffectConfig> renderEffect;

        @Override
        public int getPriority (GlContext ctx)
        {
            RenderEffectConfig config = ctx.getConfigManager().getConfig(
                RenderEffectConfig.class, renderEffect);
            return (config == null) ? 0 : config.getPriority(ctx);
        }

        @Override
        public Technique getTechnique (GlContext ctx, String scheme)
        {
            RenderEffectConfig config = ctx.getConfigManager().getConfig(
                RenderEffectConfig.class, renderEffect);
            return (config == null) ? null : config.getTechnique(ctx, scheme);
        }
    }

    /**
     * A technique available to render the effect.
     */
    public static class Technique extends DeepObject
        implements Exportable
    {
        /** The render scheme with which this technique is associated. */
        @Editable(nullable=true)
        @Reference(RenderSchemeConfig.class)
        public String scheme;

        /** The intermediate targets. */
        @Editable
        public TargetConfig[] targets = new TargetConfig[0];

        /** The final output target. */
        @Editable
        public TargetConfig.Output output = new TargetConfig.Output();

        @Deprecated
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
        }

        /**
         * Processes this technique to accommodate the features of the hardware.
         *
         * @return the processed technique, or <code>null</code> if the technique is not supported.
         */
        public Technique process (GlContext ctx, boolean fallback)
        {
            // for now, we don't do any actual processing; we just check for support
            return isSupported(ctx, fallback) ? this : null;
        }

        /**
         * Determines whether this technique is supported.
         */
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            for (TargetConfig target : targets) {
                if (!target.isSupported(ctx, fallback)) {
                    return false;
                }
            }
            return output.isSupported(ctx, fallback);
        }

        /**
         * Returns the cached configuration for the technique's render scheme.
         */
        public RenderSchemeConfig getSchemeConfig (GlContext ctx)
        {
            if (_schemeConfig == RenderSchemeConfig.INVALID) {
                return (scheme == null) ? null :
                    ctx.getConfigManager().getConfig(RenderSchemeConfig.class, scheme);
            }
            return _schemeConfig;
        }

        /**
         * Invalidates any cached state for this technique.
         */
        public void invalidate ()
        {
            _schemeConfig = RenderSchemeConfig.INVALID;
        }

        /** The cached scheme config. */
        @DeepOmit
        protected transient RenderSchemeConfig _schemeConfig = RenderSchemeConfig.INVALID;
    }

    /** The actual effect implementation. */
    @Editable
    public Implementation implementation = new Original();

    /**
     * Gets the priority of the effect.
     */
    public int getPriority (GlContext ctx)
    {
        return implementation.getPriority(ctx);
    }

    /**
     * Finds a technique to render this effect.
     *
     * @param scheme the preferred render scheme to use.
     */
    public Technique getTechnique (GlContext ctx, String scheme)
    {
        return implementation.getTechnique(ctx, scheme);
    }

    @Override
    public void wasUpdated ()
    {
        // invalidate the implementation
        implementation.invalidate();
        super.wasUpdated();
    }
}
