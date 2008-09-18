//
// $Id$

package com.threerings.opengl.material.config;

import java.util.ArrayList;

import com.samskivert.util.ObjectUtil;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.compositor.config.RenderSchemeConfig;
import com.threerings.opengl.geometry.config.GeometryConfig;
import com.threerings.opengl.geometry.config.PassDescriptor;
import com.threerings.opengl.mat.Surface;
import com.threerings.opengl.util.GlContext;

/**
 * Describes a material.
 */
public class MaterialConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the material.
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

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            for (TechniqueConfig technique : techniques) {
                technique.getUpdateReferences(refs);
            }
        }

        @Override // documentation inherited
        public TechniqueConfig getTechnique (GlContext ctx, String scheme)
        {
            // first look for an exact match for the scheme
            TechniqueConfig[] processed = getProcessedTechniques(ctx);
            for (TechniqueConfig technique : processed) {
                if (ObjectUtil.equals(technique.scheme, scheme)) {
                    return technique;
                }
            }

            // then look for a compatible match
            RenderSchemeConfig sconfig = (scheme == null) ?
                null : ctx.getConfigManager().getConfig(RenderSchemeConfig.class, scheme);
            for (TechniqueConfig technique : processed) {
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
            for (TechniqueConfig technique : techniques) {
                technique.invalidate();
            }
        }

        /**
         * Returns the lazily-constructed list of processed techniques.
         */
        protected TechniqueConfig[] getProcessedTechniques (GlContext ctx)
        {
            if (_processedTechniques == null) {
                ArrayList<TechniqueConfig> list = new ArrayList<TechniqueConfig>();
                for (TechniqueConfig technique : techniques) {
                    TechniqueConfig processed = technique.process(ctx);
                    if (processed != null) {
                        list.add(processed);
                    }
                }
                _processedTechniques = list.toArray(new TechniqueConfig[list.size()]);
            }
            return _processedTechniques;
        }

        /** The cached list of supported techniques. */
        @DeepOmit
        protected transient TechniqueConfig[] _processedTechniques;
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The material reference. */
        @Editable(nullable=true)
        public ConfigReference<MaterialConfig> material;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(MaterialConfig.class, material);
        }

        @Override // documentation inherited
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
