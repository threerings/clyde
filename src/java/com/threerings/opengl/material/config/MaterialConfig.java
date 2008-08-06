//
// $Id$

package com.threerings.opengl.material.config;

import com.threerings.config.ConfigReference;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;

import com.threerings.opengl.compositor.config.RenderSchemeConfig;
import com.threerings.opengl.geom.config.GeometryConfig;
import com.threerings.opengl.geom.config.PassDescriptor;
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
    }

    /**
     * An original implementation.
     */
    public static class Original extends Implementation
    {
        /** The techniques available to render the material. */
        @Editable
        public Technique[] techniques = new Technique[] { new Technique() };
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The material reference. */
        @Editable(nullable=true)
        public ConfigReference<MaterialConfig> material;
    }

    /**
     * A technique available to render the material.
     */
    public static class Technique extends DeepObject
        implements Exportable
    {
        /** The scheme with which this technique is associated. */
        @Editable(editor="config", mode="render_scheme", nullable=true)
        public String scheme;

        /** The passes used to render the material. */
        @Editable
        public PassConfig[] passes = new PassConfig[] { new PassConfig() };

        /**
         * Determines whether this technique is supported.
         */
        public boolean isSupported (GlContext ctx)
        {
            for (PassConfig pass : passes) {
                if (!pass.isSupported(ctx)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Creates the descriptors for this technique's passes.
         */
        public PassDescriptor[] createDescriptors (GlContext ctx)
        {
            PassDescriptor[] descriptors = new PassDescriptor[passes.length];
            for (int ii = 0; ii < passes.length; ii++) {
                descriptors[ii] = passes[ii].createDescriptor(ctx);
            }
            return descriptors;
        }
    }

    /** The actual material implementation. */
    @Editable
    public Implementation implementation = new Original();
}
