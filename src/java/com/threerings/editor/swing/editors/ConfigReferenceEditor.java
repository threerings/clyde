//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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
import java.awt.Dimension;
import java.awt.Point;
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

import com.threerings.config.ConfigReference;
import com.threerings.config.ManagedConfig;
import com.threerings.config.ParameterizedConfig;
import com.threerings.config.Parameter;
import com.threerings.config.swing.ConfigChooser;
import com.threerings.config.tools.BaseConfigEditor;

import com.threerings.editor.Editable;
import com.threerings.editor.Property;
import com.threerings.editor.swing.PropertyEditor;

/**
 * An editor for configuration references.
 */
public class ConfigReferenceEditor extends PropertyEditor
    implements ActionListener, ChangeListener
{
    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        ConfigReference ovalue = (ConfigReference)_property.get(_object);
        ConfigReference nvalue;
        Object source = event.getSource();
        if (source == _config) {
            if (_chooser == null) {
                _chooser = ConfigChooser.createInstance(
                    _msgmgr, _ctx.getConfigManager(),
                    _property.getArgumentType(ConfigReference.class));
            }
            _chooser.setSelectedConfig(ovalue == null ? null : ovalue.getName());
            if (!_chooser.showDialog(this)) {
                return;
            }
            String config = _chooser.getSelectedConfig();
            nvalue = (config == null) ? null : new ConfigReference(config);

        } else if (source == _edit) {
            BaseConfigEditor editor = BaseConfigEditor.createEditor(
                _ctx, _property.getArgumentType(ConfigReference.class), ovalue.getName());
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

    @Override // documentation inherited
    public void update ()
    {
        update((ConfigReference)_property.get(_object), false);
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        makeCollapsible(_ctx);
        _content.setLayout(
                new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        setBorder(BorderFactory.createTitledBorder(getPropertyLabel()));

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
        if (_property.getAnnotation().nullable()) {
            cpanel.add(_clear = new JButton(_msgs.get("m.clear")));
            _clear.addActionListener(this);
        }
        _content.add(_arguments = GroupLayout.makeVBox(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        _arguments.setBackground(null);
    }

    @Override // documentation inherited
    protected String getMousePath (Point pt)
    {
        Component comp = _arguments.getComponentAt(
            SwingUtilities.convertPoint(this, pt, _arguments));
        String arg = (comp instanceof PropertyEditor) ?
            ((PropertyEditor)comp).getProperty().getName() : null;
        return (arg == null) ? "" :
            ("[\"" + arg.replace("\"", "\\\"") + "\"]" + ((PropertyEditor)comp).getMousePath());
    }

    /**
     * Updates the state of the interface based on the current value.
     *
     * @param transfer if true, attempt to transfer values from the existing set of editors into
     * the current arguments.
     */
    protected void update (ConfigReference value, boolean transfer)
    {
        // update the button states
        boolean enable = (value != null);
        if (_edit != null) {
            _edit.setEnabled(enable);
        }
        if (_clear != null) {
            _clear.setEnabled(enable);
        }
        if (!enable) {
            _config.setText(_msgs.get("m.none"));
            _arguments.removeAll();
            return;
        }
        String name = value.getName();
        _config.setText(name.substring(name.lastIndexOf('/') + 1));

        // resolve the configuration reference
        @SuppressWarnings("unchecked") Class<ManagedConfig> clazz =
            (Class<ManagedConfig>)_property.getArgumentType(ConfigReference.class);
        ManagedConfig config = _ctx.getConfigManager().getConfig(clazz, name);
        if (!(config instanceof ParameterizedConfig)) {
            _arguments.removeAll();
            value.getArguments().clear();
            if (config == null) {
                _config.setForeground(Color.red);
            }
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

        // clear out any arguments that don't correspond to parameters
        for (Iterator<String> it = value.getArguments().keySet().iterator(); it.hasNext(); ) {
            if (pconfig.getParameter(it.next()) == null) {
                it.remove();
            }
        }
    }

    /** The config button. */
    protected JButton _config;

    /** The edit, and clear buttons. */
    protected JButton _edit, _clear;

    /** Holds the argument panels. */
    protected JPanel _arguments;

    /** The config chooser. */
    protected ConfigChooser _chooser;
}
