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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.HashMap;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.swing.util.SwingUtil;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.config.ManagedConfig;
import com.threerings.config.NoDependency;
import com.threerings.config.ParameterizedConfig;
import com.threerings.config.Parameter;
import com.threerings.config.ReferenceConstraints;
import com.threerings.config.swing.ConfigChooser;
import com.threerings.config.tools.BaseConfigEditor;

import com.threerings.editor.Property;
import com.threerings.editor.util.PropertyUtil;
import com.threerings.editor.swing.PropertyEditor;

/**
 * An editor for configuration references.
 */
public class ConfigReferenceEditor extends PropertyEditor
    implements ActionListener, ChangeListener, ConfigUpdateListener<ManagedConfig>
{
    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        ConfigReference<?> ovalue = (ConfigReference<?>)_property.get(_object);
        ConfigReference<?> nvalue;
        Object source = event.getSource();
        if (source == _config) {
            if (_chooser == null) {
                _chooser = ConfigChooser.createInstance(
                    _msgmgr, _ctx.getConfigManager(), getArgumentType(),
                    getProperty().getAnnotation(ReferenceConstraints.class));
            }
            _chooser.setSelectedConfig(ovalue == null ? null : ovalue.getName());
            if (!_chooser.showDialog(this)) {
                return;
            }
            String config = _chooser.getSelectedConfig();
            nvalue = (config == null) ? null : new ConfigReference<ManagedConfig>(config);
            if (nvalue != null && !validateNewValue(nvalue)) {
                return;
            }

        } else if (source == _edit) {
            BaseConfigEditor editor = BaseConfigEditor.createEditor(
                _ctx, getArgumentType(), ovalue.getName());
            editor.setLocationRelativeTo(this);
            editor.setVisible(true);
            return;

        } else if (source == _clear) {
            nvalue = null;
        } else {
            super.actionPerformed(event);
            return;
        }
        _property.set(_object, nvalue);
        update(nvalue, true);
        fireStateChanged();
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        fireStateChanged();
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<ManagedConfig> event)
    {
        update();
    }

    @Override
    public void addNotify ()
    {
        super.addNotify();
        if (_listenee != null) {
            update();
        }
    }

    @Override
    public void removeNotify ()
    {
        super.removeNotify();
        if (_listenee != null) {
            _listenee.removeListener(this);
        }
    }

    @Override
    public void update ()
    {
        update((ConfigReference)_property.get(_object), false);
    }

    @Override
    public String getComponentPath (Component comp, boolean mouse)
    {
        PropertyEditor editor = getNextChildComponent(PropertyEditor.class, comp);
        return editor == null ? "" :
            ("[\"" + editor.getProperty().getName().replace("\"", "\\\"") + "\"]" +
             editor.getComponentPath(comp, mouse));
    }

    /**
     * Get the type of the config reference.
     */
    protected Class<?> getArgumentType ()
    {
        return _property.getArgumentType(ConfigReference.class);
    }

    @Override
    protected void didInit ()
    {
        if (_property.getAnnotation().constant()) {
            add(new JLabel(getPropertyLabel() + ":"));
            add(_name = new JLabel(" "));
            return;
        }

        makeCollapsible(_ctx, getPropertyLabel(), false);

        JPanel cpanel = new JPanel();
        _content.add(cpanel);
        cpanel.add(new JLabel(_msgs.get("m.config") + ":"));
        cpanel.add(_config = new JButton(" "));
        cpanel.setBackground(null);
        _config.addActionListener(this);
        if (!getMode().equals("compact")) {
            cpanel.add(_edit = new JButton(_msgs.get("m.edit")));
            _edit.addActionListener(this);
        }
        if (_property.nullable()) {
            cpanel.add(_clear = new JButton(_msgs.get("m.clear")));
            _clear.addActionListener(this);
        }
        _content.add(_arguments = GroupLayout.makeVBox(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        _arguments.setBackground(null);
    }

    /**
     * Validate that the new value is good, possibly informing the user if not.
     */
    protected boolean validateNewValue (ConfigReference<?> value)
    {
        return true;
    }

    /**
     * Updates the state of the interface based on the current value.
     *
     * @param transfer if true, attempt to transfer values from the existing set of editors into
     * the current arguments.
     */
    protected void update (ConfigReference<?> value, boolean transfer)
    {
        // make sure we're not listening to anything
        if (_listenee != null) {
            _listenee.removeListener(this);
            _listenee = null;
        }

        if (_property.getAnnotation().constant()) {
            _name.setText(value == null ? _msgs.get("m.null_value") : value.getName());
            return;
        }

        // update the button states
        boolean enable = (value != null);
        if (_edit != null) {
            _edit.setEnabled(enable);
        }
        if (_clear != null) {
            _clear.setEnabled(enable);
        }
        if (!enable) {
            _config.setText(_msgs.get("m.null_value"));
            _arguments.removeAll();
            _config.setForeground(_property.nullable() ? _content.getForeground() : Color.red);
            return;
        }
        String name = value.getName();
        _config.setToolTipText(name);
        _config.setText(name.substring(name.lastIndexOf('/') + 1));

        // resolve the configuration reference
        @SuppressWarnings("unchecked") Class<ManagedConfig> clazz =
                (Class<ManagedConfig>)getArgumentType();
        ManagedConfig config = _ctx.getConfigManager().getRawConfig(clazz, name);
        if (!(config instanceof ParameterizedConfig) ||
                getProperty().isAnnotationPresent(NoDependency.class)) {
            _arguments.removeAll();
            value.getArguments().clear();
            if (config == null) {
                _config.setForeground(Color.red);
            }
            return;
        }
        if (!PropertyUtil.getRawConfigPredicate(
                    getProperty().getAnnotation(ReferenceConstraints.class)).apply(config)) {
            _config.setForeground(Color.red);
            return;
        }
        _config.setForeground(_content.getForeground());

        // store the existing editors mapped by name in case we want to reuse their values
        int ocount = _arguments.getComponentCount();
        HashMap<String, PropertyEditor> oeditors = new HashMap<String, PropertyEditor>();
        if (transfer) {
            for (int ii = 0; ii < ocount; ii++) {
                PropertyEditor editor = (PropertyEditor)_arguments.getComponent(ii);
                oeditors.put(editor.getProperty().getName(), editor);
            }
        }

        // scan through the parameters
        int idx = 0;
        Object nargs = value.getArguments();
        ParameterizedConfig pconfig = (ParameterizedConfig)config;
        for (Parameter parameter : pconfig.parameters) {
            Property property = parameter.getArgumentProperty(pconfig);
            if (property == null) {
                continue;
            }
            PropertyEditor editor = null;
            if (idx < ocount) {
                // see if we can copy the argument to the new set and reuse the editor
                PropertyEditor oeditor = (PropertyEditor)_arguments.getComponent(idx);
                if (oeditor.getProperty().equals(property)) {
                    if (transfer) {
                        property.set(nargs, property.get(oeditor.getObject()));
                    }
                    editor = oeditor;
                } else {
                    _arguments.remove(idx);
                }
            }
            if (editor == null) {
                editor = PropertyEditor.createEditor(_ctx, property, _lineage);
                editor.addChangeListener(this);
                _arguments.add(editor, idx);

                // see if we can reuse the value from the previous editor
                PropertyEditor oeditor = oeditors.get(parameter.name);
                if (oeditor != null) {
                    Property oproperty = oeditor.getProperty();
                    if (property.getGenericType().equals(oproperty.getGenericType())) {
                        Object ovalue = oproperty.get(oeditor.getObject());
                        if (property.isLegalValue(ovalue)) {
                            property.set(nargs, ovalue);
                        }
                    }
                }
            }
            editor.setObject(nargs);
            idx++;
        }

        // remove the remaining editors
        while (ocount > idx) {
            _arguments.remove(--ocount);
        }
        // we may need to refresh the panel
        SwingUtil.refresh(_arguments);

        // clear out any arguments that don't correspond to parameters
        for (Iterator<String> it = value.getArguments().keySet().iterator(); it.hasNext(); ) {
            if (pconfig.getParameter(it.next()) == null) {
                it.remove();
            }
        }

        // listen for parameter changes
        (_listenee = pconfig).addListener(this);
    }

    /** The config button. */
    protected JButton _config;

    /** The edit, and clear buttons. */
    protected JButton _edit, _clear;

    /** Holds the argument panels. */
    protected JPanel _arguments;

    /** The label for a constant config. */
    protected JLabel _name;

    /** The config chooser. */
    protected ConfigChooser _chooser;

    /** The config that we're listening to, if any. */
    protected ParameterizedConfig _listenee;
}
