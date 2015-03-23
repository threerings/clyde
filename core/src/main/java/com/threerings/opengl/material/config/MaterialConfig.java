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

import java.util.ArrayList;

import com.google.common.base.Objects;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.compositor.config.RenderSchemeConfig;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Preloadable;

/**
 * Describes a material.
 */
public class MaterialConfig extends ParameterizedConfig
    implements Preloadable.LoadableConfig
{
    /**
     * Contains the actual implementation of the material.
     */
    @EditorTypes({ Original.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable, Preloadable.LoadableConfig
    {
        @Deprecated
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
        }

        /**
         * Returns a technique to use to render this material.
         */
        public abstract TechniqueConfig getTechnique (GlContext ctx, String scheme);

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
        /** The techniques available to render the material. */
        @Editable
        public TechniqueConfig[] techniques = new TechniqueConfig[] { new TechniqueConfig() };

        @Override
        public void preload (GlContext ctx)
        {
            for (TechniqueConfig technique : techniques) {
                technique.preload(ctx);
            }
        }

        @Override
        public TechniqueConfig getTechnique (GlContext ctx, String scheme)
        {
            // first look for an exact match for the scheme
            ArrayList<TechniqueConfig> processed = getProcessedTechniques(ctx);
            for (int ii = 0, nn = processed.size(); ii < nn; ii++) {
                TechniqueConfig technique = processed.get(ii);
                if (Objects.equal(technique.scheme, scheme)) {
                    return technique;
                }
            }

            // then look for a compatible match
            RenderSchemeConfig sconfig = (scheme == null) ?
                null : ctx.getConfigManager().getConfig(RenderSchemeConfig.class, scheme);
            for (int ii = 0, nn = processed.size(); ii < nn; ii++) {
                TechniqueConfig technique = processed.get(ii);
                RenderSchemeConfig tconfig = technique.getSchemeConfig(ctx);
                if ((sconfig == null) ? (tconfig == null || tconfig.isCompatibleWith(sconfig)) :
                        sconfig.isCompatibleWith(tconfig)) {
                    return technique;
                }
            }

            // then try to rewrite an existing technique
            MaterialRewriter rewriter = (sconfig == null) ? null : sconfig.getMaterialRewriter();
            if (rewriter == null) {
                return null;
            }
            for (int ii = 0, nn = processed.size(); ii < nn; ii++) {
                TechniqueConfig technique = processed.get(ii);
                RenderSchemeConfig tconfig = technique.getSchemeConfig(ctx);
                if (tconfig == null || tconfig.isCompatibleWith(null)) {
                    TechniqueConfig rewritten = rewriter.rewrite(technique);
                    if (rewritten != null) {
                        rewritten.scheme = scheme;
                        processed.add(rewritten);
                        return rewritten;
                    }
                }
            }

            // finally, just give up
            return null;
        }

        @Override
        public void invalidate ()
        {
            _processedTechniques = null;
            for (TechniqueConfig technique : techniques) {
                technique.invalidate();
            }
        }

        /**
         * Returns the lazily-constructed list of processed techniques.
         */
        protected ArrayList<TechniqueConfig> getProcessedTechniques (GlContext ctx)
        {
            boolean compatibility = ctx.getApp().getCompatibilityMode();
            if (_processedTechniques == null || _compatibilityMode != compatibility) {
                _processedTechniques = new ArrayList<TechniqueConfig>(techniques.length);
                ArrayList<TechniqueConfig> fallbacks = new ArrayList<TechniqueConfig>(0);
                for (TechniqueConfig technique : techniques) {
                    // first try without fallbacks, then with
                    TechniqueConfig processed = technique.process(ctx, false);
                    if (processed == null) {
                        TechniqueConfig fallback = technique.process(ctx, true);
                        if (fallback != null) {
                            fallbacks.add(fallback);
                        }
                    } else {
                        _processedTechniques.add(processed);
                    }
                }
                _processedTechniques.addAll(fallbacks);
                _compatibilityMode = compatibility;
            }
            return _processedTechniques;
        }

        /** The cached list of supported techniques. */
        @DeepOmit
        protected transient ArrayList<TechniqueConfig> _processedTechniques;

        /** The compatibility mode setting when the techniques were processed. */
        @DeepOmit
        protected transient boolean _compatibilityMode;
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The material reference. */
        @Editable(nullable=true)
        public ConfigReference<MaterialConfig> material;

        @Override
        public void preload (GlContext ctx)
        {
            new Preloadable.Config(MaterialConfig.class, material).preload(ctx);
        }

        @Override
        public TechniqueConfig getTechnique (GlContext ctx, String scheme)
        {
            if (material == null) {
                return null;
            }
            MaterialConfig config = ctx.getConfigManager().getConfig(
                MaterialConfig.class, material);
            return (config == null) ? null : config.getTechnique(ctx, scheme);
        }
    }

    /** The actual material implementation. */
    @Editable
    public Implementation implementation = new Original();

    /**
     * Finds a technique to render this material.
     *
     * @param scheme the preferred render scheme to use.
     */
    public TechniqueConfig getTechnique (GlContext ctx, String scheme)
    {
        return implementation.getTechnique(ctx, scheme);
    }

    @Override
    public void preload (GlContext ctx)
    {
        implementation.preload(ctx);
    }

    @Override
    protected void fireConfigUpdated ()
    {
        // invalidate the implementation
        implementation.invalidate();
        super.fireConfigUpdated();
    }
}
