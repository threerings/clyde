//
// $Id$

package com.threerings.config.swing;

import java.util.Collection;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import com.samskivert.util.ObjectUtil;
import com.samskivert.util.QuickSort;

import com.threerings.util.MessageBundle;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigGroupListener;
import com.threerings.config.ManagedConfig;

/**
 * Allows the user to select a config from a drop-down.
 */
public class ConfigBox extends JComboBox
    implements ConfigGroupListener<ManagedConfig>
{
    /**
     * Creates a new config box.
     */
    public ConfigBox (MessageBundle msgs, ConfigGroup group, boolean nullable)
    {
        this(msgs, group, nullable, null);
    }

    /**
     * Creates a new config box.
     */
    public ConfigBox (MessageBundle msgs, ConfigGroup group, boolean nullable, String config)
    {
        _msgs = msgs;
        @SuppressWarnings("unchecked") ConfigGroup<ManagedConfig> mgroup =
            (ConfigGroup<ManagedConfig>)group;
        _group = mgroup;
        _nullable = nullable;

        updateModel();
        setSelectedConfig(config);
    }

    /**
     * Sets the path of the selected config.
     */
    public void setSelectedConfig (String config)
    {
        setSelectedItem(new ConfigItem(config));
    }

    /**
     * Returns the path of the selected config.
     */
    public String getSelectedConfig ()
    {
        ConfigItem item = (ConfigItem)getSelectedItem();
        return (item == null) ? null : item.name;
    }

    // documentation inherited from interface ConfigGroupListener
    public void configAdded (ConfigEvent<ManagedConfig> event)
    {
        updateModel();
    }

    // documentation inherited from interface ConfigGroupListener
    public void configRemoved (ConfigEvent<ManagedConfig> event)
    {
        updateModel();
    }

    @Override // documentation inherited
    public void addNotify ()
    {
        super.addNotify();
        updateModel();
        _group.addListener(this);
    }

    @Override // documentation inherited
    public void removeNotify ()
    {
        super.removeNotify();
        _group.removeListener(this);
    }

    /**
     * Updates the combo box model and current selection.
     */
    protected void updateModel ()
    {
        Collection<ManagedConfig> configs = _group.getConfigs();
        int offset = _nullable ? 1 : 0;
        String[] names = new String[configs.size() + offset];
        int idx = offset;
        for (ManagedConfig config : configs) {
            names[idx++] = config.getName();
        }
        QuickSort.sort(names, offset, names.length - 1);
        ConfigItem[] items = new ConfigItem[names.length];
        for (int ii = 0; ii < names.length; ii++) {
            items[ii] = new ConfigItem(names[ii]);
        }
        String config = getSelectedConfig();
        setModel(new DefaultComboBoxModel(items));
        setSelectedConfig(config);
    }

    /**
     * An item in the configuration list.
     */
    protected class ConfigItem
    {
        /** The name of the config. */
        public String name;

        public ConfigItem (String name)
        {
            this.name = name;
        }

        @Override // documentation inherited
        public String toString ()
        {
            return (name == null) ? _msgs.get("m.none") : name;
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return ObjectUtil.equals(name, ((ConfigItem)other).name);
        }
    }

    /** The message bundle to use for translation. */
    protected MessageBundle _msgs;

    /** The configuration manager. */
    protected ConfigGroup<ManagedConfig> _group;

    /** Whether or not the null value is selectable. */
    protected boolean _nullable;
}
