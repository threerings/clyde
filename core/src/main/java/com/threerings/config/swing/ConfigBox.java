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

package com.threerings.config.swing;

import java.util.HashSet;

import javax.annotation.Nullable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;

import com.samskivert.util.QuickSort;

import com.threerings.util.MessageBundle;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigGroupListener;
import com.threerings.config.ManagedConfig;
import com.threerings.config.ReferenceConstraints;

import com.threerings.editor.util.PropertyUtil;

/**
 * Allows the user to select a config from a drop-down.
 */
public class ConfigBox extends JComboBox
    implements ConfigGroupListener
{
    /**
     * Creates a new config box.
     */
    public ConfigBox (
            MessageBundle msgs, ConfigGroup<?>[] groups, boolean nullable,
            ReferenceConstraints constraints,
            @Nullable Supplier<Predicate<? super ManagedConfig>> additionalFilterSupplier)
    {
        _msgs = msgs;
        @SuppressWarnings("unchecked")
        ConfigGroup<ManagedConfig>[] mgroups = (ConfigGroup<ManagedConfig>[])groups;
        _groups = mgroups;
        _nullable = nullable;
        _filter = PropertyUtil.getRawConfigPredicate(constraints);
        _additionalFilter = additionalFilterSupplier;

        updateModel();
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

    @Override
    public void addNotify ()
    {
        super.addNotify();
        updateModel();
        for (ConfigGroup<ManagedConfig> group : _groups) {
            group.addListener(this);
        }
    }

    @Override
    public void removeNotify ()
    {
        super.removeNotify();
        for (ConfigGroup<ManagedConfig> group : _groups) {
            group.removeListener(this);
        }
    }

    /**
     * Updates the combo box model and current selection.
     */
    protected void updateModel ()
    {
        // gather all config names into a set
        HashSet<String> names = new HashSet<String>();
        Predicate<? super ManagedConfig> curFilter; // cannot convert to ternary: compiler bug.
        if (_additionalFilter == null) {
            curFilter = _filter;
        } else {
            curFilter = Predicates.and(_filter, _additionalFilter.get());
        }
        for (ConfigGroup<ManagedConfig> group : _groups) {
            for (ManagedConfig config : Iterables.filter(group.getRawConfigs(), curFilter)) {
                names.add(config.getName());
            }
        }

        // create an array containing the items
        int offset = _nullable ? 1 : 0;
        ConfigItem[] items = new ConfigItem[offset + names.size()];
        int idx = 0;
        if (_nullable) {
            items[idx++] = new ConfigItem(null);
        }
        for (String name : names) {
            items[idx++] = new ConfigItem(name);
        }

        // sort the non-null items
        QuickSort.sort(items, offset, items.length - 1);

        // update the model, preserving the selected config
        String config = getSelectedConfig();
        setModel(new DefaultComboBoxModel(items));
        setSelectedConfig(config);
    }

    /**
     * An item in the configuration list.
     */
    protected class ConfigItem
        implements Comparable<ConfigItem>
    {
        /** The name of the config. */
        public String name;

        public ConfigItem (String name)
        {
            this.name = name;
        }

        // documentation inherited from interface Comparable
        public int compareTo (ConfigItem other)
        {
            return name.compareTo(other.name);
        }

        @Override
        public String toString ()
        {
            return (name == null) ? _msgs.get("m.null_value") : name;
        }

        @Override
        public boolean equals (Object other)
        {
            return Objects.equal(name, ((ConfigItem)other).name);
        }

        @Override
        public int hashCode ()
        {
            return name != null ? name.hashCode() : 0;
        }
    }

    /** The message bundle to use for translation. */
    protected MessageBundle _msgs;

    /** Whether or not the null value is selectable. */
    protected boolean _nullable;

    /** The configuration groups. */
    protected ConfigGroup<ManagedConfig>[] _groups;

    /** The filter used for configs. */
    protected Predicate<ManagedConfig> _filter;

    /** A supplier of additional filters. */
    protected Supplier<Predicate<? super ManagedConfig>> _additionalFilter;
}
