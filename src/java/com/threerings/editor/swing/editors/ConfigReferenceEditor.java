//
// $Id$

package com.threerings.editor.swing.editors;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.HashMap;

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
            nvalue = new ConfigReference(_chooser.getSelectedConfig());

        } else if (source == _edit) {
            BaseConfigEditor editor = BaseConfigEditor.createEditor(
                _ctx, _property.getArgumentType(ConfigReference.class), ovalue.getName());
            Point pt = getLocationOnScreen();
            editor.setLocation(
                pt.x + (getWidth() - editor.getWidth()) / 2,
                pt.y + (getHeight() - editor.getHeight()) / 2);
            editor.setVisible(true);
            return;

        } else if (source == _reload) {
            nvalue = ovalue;

        } else { // event.getSource() == _clear
            nvalue = null;
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
        setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        setBorder(BorderFactory.createTitledBorder(getPropertyLabel()));

        JPanel cpanel = new JPanel();
        cpanel.setBackground(null);
        add(cpanel);
        cpanel.add(new JLabel(_msgs.get("m.config") + ":"));
        cpanel.add(_config = new JButton(" "));
        _config.setPreferredSize(new Dimension(75, _config.getPreferredSize().height));
        _config.addActionListener(this);
        if (!getMode().equals("compact")) {
            cpanel.add(_edit = new JButton(_msgs.get("m.edit")));
            _edit.addActionListener(this);
            cpanel.add(_reload = new JButton(_msgs.get("m.reload")));
            _reload.addActionListener(this);
        }
        if (_property.getAnnotation().nullable()) {
            cpanel.add(_clear = new JButton(_msgs.get("m.clear")));
            _clear.addActionListener(this);
        }
        add(_arguments = GroupLayout.makeVBox(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        _arguments.setBackground(null);
    }

    @Override // documentation inherited
    protected String getPathComponent (Point pt)
    {
        Component comp = _arguments.getComponentAt(
            SwingUtilities.convertPoint(this, pt, _arguments));
        String arg = (comp instanceof PropertyEditor) ?
            ((PropertyEditor)comp).getProperty().getName() : null;
        return _property.getName() + (arg == null ?
            "" : ("[\"" + arg.replace("\"", "\\\"") + "\"]"));
    }

    @Override // documentation inherited
    protected boolean skipChildPath (Component comp)
    {
        return _arguments.getComponentZOrder(comp) != -1;
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
            _reload.setEnabled(enable);
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
            return;
        }

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
    }

    /** The config button. */
    protected JButton _config;

    /** The reload, edit, and clear buttons. */
    protected JButton _edit, _reload, _clear;

    /** Holds the argument panels. */
    protected JPanel _arguments;

    /** The config chooser. */
    protected ConfigChooser _chooser;
}
