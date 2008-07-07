//
// $Id$

package com.threerings.opengl.compositor.config;

import com.threerings.config.ConfigReference;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

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
    }

    /**
     * An original implementation.
     */
    public static class Original extends Implementation
    {
        /** The techniques available to render the post effect. */
        @Editable
        public Technique[] techniques = new Technique[0];
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The post effect reference. */
        @Editable(nullable=true)
        public ConfigReference<PostEffectConfig> postEffect;
    }

    /**
     * A technique available to render the post effect.
     */
    public static class Technique extends DeepObject
        implements Exportable
    {
        /** The intermediate targets. */
        @Editable
        public TargetConfig[] targets = new TargetConfig[0];

        /** The final output target. */
        @Editable
        public TargetConfig.Output output = new TargetConfig.Output();
    }

    /** The actual post effect implementation. */
    @Editable
    public Implementation implementation = new Original();
}
