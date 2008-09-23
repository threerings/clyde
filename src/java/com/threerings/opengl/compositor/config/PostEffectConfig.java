//
// $Id$

package com.threerings.opengl.compositor.config;

import java.util.ArrayList;

import com.samskivert.util.ObjectUtil;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.util.GlContext;

/**
 * Describes a post effect.
 */
public class PostEffectConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the post effect.
     */
    @EditorTypes({ Original.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Adds the implementation's update references to the provided set.
         */
        public abstract void getUpdateReferences (ConfigReferenceSet refs);

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

        /** The techniques available to render the post effect. */
        @Editable
        public Technique[] techniques = new Technique[0];

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            for (Technique technique : techniques) {
                technique.getUpdateReferences(refs);
            }
        }

        @Override // documentation inherited
        public Technique getTechnique (GlContext ctx, String scheme)
        {
            // first look for an exact match for the scheme
            Technique[] processed = getProcessedTechniques(ctx);
            for (Technique technique : processed) {
                if (ObjectUtil.equals(technique.scheme, scheme)) {
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

        @Override // documentation inherited
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
                ArrayList<Technique> list = new ArrayList<Technique>();
                for (Technique technique : techniques) {
                    Technique processed = technique.process(ctx);
                    if (processed != null) {
                        list.add(processed);
                    }
                }
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
        /** The post effect reference. */
        @Editable(nullable=true)
        public ConfigReference<PostEffectConfig> postEffect;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(PostEffectConfig.class, postEffect);
        }

        @Override // documentation inherited
        public Technique getTechnique (GlContext ctx, String scheme)
        {
            PostEffectConfig config = ctx.getConfigManager().getConfig(
                PostEffectConfig.class, postEffect);
            return (config == null) ? null : config.getTechnique(ctx, scheme);
        }
    }

    /**
     * A technique available to render the post effect.
     */
    public static class Technique extends DeepObject
        implements Exportable
    {
        /** The render scheme with which this technique is associated. */
        @Editable(editor="config", mode="render_scheme", nullable=true)
        public String scheme;

        /** The intermediate targets. */
        @Editable
        public TargetConfig[] targets = new TargetConfig[0];

        /** The final output target. */
        @Editable
        public TargetConfig.Output output = new TargetConfig.Output();

        /**
         * Adds the technique's update references to the provided set.
         */
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            for (TargetConfig target : targets) {
                target.getUpdateReferences(refs);
            }
            output.getUpdateReferences(refs);
        }

        /**
         * Processes this technique to accommodate the features of the hardware.
         *
         * @return the processed technique, or <code>null</code> if the technique is not supported.
         */
        public Technique process (GlContext ctx)
        {
            // for now, we don't do any actual processing; we just check for support
            return isSupported(ctx) ? this : null;
        }

        /**
         * Determines whether this technique is supported.
         */
        public boolean isSupported (GlContext ctx)
        {
            for (TargetConfig target : targets) {
                if (!target.isSupported(ctx)) {
                    return false;
                }
            }
            return output.isSupported(ctx);
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

    /** The actual post effect implementation. */
    @Editable
    public Implementation implementation = new Original();

    /**
     * Finds a technique to render this effect.
     *
     * @param scheme the preferred render scheme to use.
     */
    public Technique getTechnique (GlContext ctx, String scheme)
    {
        return implementation.getTechnique(ctx, scheme);
    }

    @Override // documentation inherited
    public void wasUpdated ()
    {
        // invalidate the implementation
        implementation.invalidate();
        super.wasUpdated();
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }
}
