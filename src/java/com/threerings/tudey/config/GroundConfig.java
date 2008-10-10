//
// $Id$

package com.threerings.tudey.config;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * The configuration of a ground type.
 */
public class GroundConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the ground type.
     */
    @EditorTypes({ Original.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Adds the implementation's update references to the provided set.
         */
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            // nothing by default
        }
    }

    /**
     * An original ground implementation.
     */
    public static class Original extends Implementation
    {
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The ground reference. */
        @Editable(nullable=true)
        public ConfigReference<GroundConfig> ground;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(GroundConfig.class, ground);
        }
    }

    /** The actual ground implementation. */
    @Editable
    public Implementation implementation = new Original();

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }
}
