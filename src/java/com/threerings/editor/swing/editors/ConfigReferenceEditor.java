//
// $Id$

package com.threerings.editor.swing.editors;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.config.ConfigReference;
import com.threerings.config.ManagedConfig;
import com.threerings.config.ParameterizedConfig;
import com.threerings.config.ParameterizedConfig.Parameter;
import com.threerings.config.swing.ConfigChooser;

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
        if (event.getSource() == _config) {
            if (_chooser == null) {
                _chooser = new ConfigChooser(
                    _msgs, _ctx.getConfigManager(),
                    _property.getArgumentType(ConfigReference.class));
            }
            _chooser.setSelectedConfig(ovalue == null ? null : ovalue.getName());
            if (!_chooser.showDialog(this)) {
                return;
            }
            nvalue = new ConfigReference(_chooser.getSelectedConfig());

        } else { // event.getSource() == _clear
            nvalue = null;
        }
        _property.set(_object, nvalue);
        updateButtons(nvalue);
        fireStateChanged();
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        fireStateChanged();
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        setBorder(BorderFactory.createTitledBorder(getPropertyLabel()));

        JPanel cpanel = new JPanel();
        cpanel.setBackground(null);
        add(cpanel);
        cpanel.add(new JLabel(_msgs.get("m.config") + ":"));
        cpanel.add(_config = new JButton(" "));
        _config.setPreferredSize(new Dimension(75, _config.getPreferredSize().height));
        _config.addActionListener(this);
        if (_property.getAnnotation().nullable()) {
            cpanel.add(_clear = new JButton(_msgs.get("m.clear")));
            _clear.addActionListener(this);
        }
        add(_params = GroupLayout.makeVBox(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        _params.setBackground(null);
    }

    @Override // documentation inherited
    protected void update ()
    {
        ConfigReference value = (ConfigReference)_property.get(_object);
        updateButtons(value);
        updateParameters(value);
    }

    /**
     * Updates the state of the buttons.
     */
    protected void updateButtons (ConfigReference value)
    {
        if (value != null) {
            String name = value.getName();
            _config.setText(name.substring(name.lastIndexOf('/') + 1));
        } else {
            _config.setText(_msgs.get("m.none"));
        }
        if (_clear != null) {
            _clear.setEnabled(value != null);
        }
    }

    /**
     * Updates the parameters.
     */
    protected void updateParameters (ConfigReference value)
    {
        if (value == null) {
            _params.removeAll();
            return;
        }
        @SuppressWarnings("unchecked") Class<ManagedConfig> clazz =
            (Class<ManagedConfig>)_property.getArgumentType(ConfigReference.class);
        ManagedConfig config = _ctx.getConfigManager().getConfig(clazz, value.getName());
        ParameterizedConfig pconfig;
        if (!(config instanceof ParameterizedConfig) ||
                (pconfig = (ParameterizedConfig)config).parameters.length == 0) {
            _params.removeAll();
            return;
        }
        Parameter[] parameters = pconfig.parameters;
        for (int ii = 0; ii < parameters.length; ii++) {
            Parameter parameter = parameters[ii];
            Property property = parameter.createProperty(config);
            if (property == null) {
                continue;
            }
            PropertyEditor editor = PropertyEditor.createEditor(_ctx, property, _lineage);
            editor.setObject(config);
            editor.addChangeListener(this);
            _params.add(editor);
        }
    }

    /** The config button. */
    protected JButton _config;

    /** The clear button. */
    protected JButton _clear;

    /** Holds the parameters. */
    protected JPanel _params;

    /** The config chooser. */
    protected ConfigChooser _chooser;
}
