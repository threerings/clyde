//
// $Id$

package com.threerings.opengl.gui.config;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * Describes a user interface.
 */
public class UserInterfaceConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the interface.
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
     * An original implementation.
     */
    public static class Original extends Implementation
    {
        /** The root of the interface. */
        @Editable
        public ComponentConfig root = new ComponentConfig.Spacer();
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The interface reference. */
        @Editable(nullable=true)
        public ConfigReference<UserInterfaceConfig> userInterface;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(UserInterfaceConfig.class, userInterface);
        }
    }

    /** The actual interface implementation. */
    @Editable
    public Implementation implementation = new Original();

    @Override // documentation inherited
    public void init (ConfigManager cfgmgr)
    {
        _configs.init("user_interface", cfgmgr);
        super.init(_configs);
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }

    /** The model's local config library. */
    protected ConfigManager _configs = new ConfigManager();
}
