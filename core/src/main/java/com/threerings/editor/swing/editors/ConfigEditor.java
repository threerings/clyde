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

package com.threerings.editor.swing.editors;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ManagedConfig;
import com.threerings.config.Reference;
import com.threerings.config.ReferenceConstraints;
import com.threerings.config.swing.ConfigBox;

import com.threerings.editor.swing.PropertyEditor;

import static com.threerings.editor.Log.log;

/**
 * Editor for simple string config references.
 */
public class ConfigEditor extends PropertyEditor
    implements ActionListener
{
    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        Object value = _box.getSelectedConfig();
        if (!Objects.equal(_property.get(_object), value)) {
            _property.set(_object, value);
            fireStateChanged();
        }
    }

    @Override
    public void update ()
    {
        _box.setSelectedConfig((String)_property.get(_object));
    }

    @Override
    protected void didInit ()
    {
        add(new JLabel(getPropertyLabel() + ":"));

        // look for a @Reference annotation
        Reference ref = _property.getAnnotation(Reference.class);
        ConfigGroup<?>[] groups = (ref != null)
                ? _ctx.getConfigManager().getGroups(ref.value()) // new way
                : _ctx.getConfigManager().getGroups(getMode()); // old way
        if (groups.length == 0) {
            log.warning("Missing groups for config editor.",
                    "type", (ref != null) ? ref.value() : getMode());
            return;
        }
        _box = new ConfigBox(
                _msgs, groups, _property.nullable(),
                _property.getAnnotation(ReferenceConstraints.class),
                new Supplier<Predicate<? super ManagedConfig>>() {
                            public Predicate<? super ManagedConfig> get () {
                                // if we're not editing a ManagedConfig, anything goes
                                if (!(_object instanceof ManagedConfig)) {
                                    return Predicates.alwaysTrue();
                                }
                                // if we are, don't let ourselves be referenced
                                final ManagedConfig ourCfg = (ManagedConfig)_object;
                                return new Predicate<ManagedConfig>() {
                                    public boolean apply (ManagedConfig cfg) {
                                        return (ourCfg.getName() != cfg.getName()) ||
                                                (ourCfg.getConfigGroup() != cfg.getConfigGroup());
                                    }
                                };
                            }
                        });
        _box.addActionListener(this);
        add(_box);
    }

    /** The combo box. */
    protected ConfigBox _box;
}
