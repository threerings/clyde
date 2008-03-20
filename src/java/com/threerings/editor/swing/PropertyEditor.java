//
// $Id$

package com.threerings.editor.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.lang.reflect.Array;

import java.io.File;

import java.util.HashMap;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ListUtil;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.math.Quaternion;
import com.threerings.math.Transform;
import com.threerings.math.Vector3f;

import com.threerings.opengl.renderer.Color4f;

import com.threerings.editor.Editable;
import com.threerings.editor.FileConstraints;
import com.threerings.editor.Property;

import static java.util.logging.Level.*;
import static com.threerings.editor.Log.*;

/**
 * Edits a single object property.
 */
public abstract class PropertyEditor extends BasePropertyEditor
{
    /**
     * Creates a property editor for the specified object property.
     *
     * @param ancestors the ancestor properties from which to inherit constraints, if any.
     */
    public static PropertyEditor createEditor (
        MessageBundle msgs, Property property, Property[] ancestors)
    {
        // look first by name, if a custom editor is specified
        String name = property.getAnnotation().editor();
        Class<? extends PropertyEditor> clazz = null;
        if (name.length() > 0 && (clazz = _classesByName.get(name)) == null) {
            log.warning("Missing custom editor class [name=" + name + "].");
        }
        // then by type
        Class type = property.getType();
        PropertyEditor editor;
        if (clazz != null || (clazz = _classesByType.get(type)) != null) {
            try {
                editor = clazz.newInstance();
            } catch (Exception e) {
                log.log(WARNING, "Failed to create property editor.", e);
                editor = new ObjectEditor();
            }
        } else if (type.isEnum()) {
            editor = new EnumEditor();
        } else if (type.isArray() || List.class.isAssignableFrom(type)) {
            editor = new ArrayListEditor();
        } else {
            editor = new ObjectEditor();
        }
        editor.init(msgs, property, ancestors);
        return editor;
    }

    /**
     * Adds a custom editor class for properties of the given type.
     */
    public static void registerEditorClass (Class type, Class<? extends PropertyEditor> clazz)
    {
        _classesByType.put(type, clazz);
    }

    /**
     * Adds a custom editor by name.
     */
    public static void registerEditorClass (String name, Class<? extends PropertyEditor> clazz)
    {
        _classesByName.put(name, clazz);
    }

    /**
     * Initializes the editor with its object and property references.
     */
    public void init (MessageBundle msgs, Property property, Property[] ancestors)
    {
        _msgs = msgs;
        _property = property;
        _lineage = (ancestors == null) ?
            new Property[] { _property } : ArrayUtil.append(ancestors, _property);

        // give subclasses a chance to initialize themselves
        didInit();
    }

    /**
     * Sets the object being edited.
     */
    public void setObject (Object object)
    {
        _object = object;

        // give subclasses a chance to update
        update();
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
    }

    /**
     * Updates the state of the editor from the object.
     */
    protected void update ()
    {
    }

    /**
     * Returns the name of the property, translating it if a translation exists.
     */
    protected String getPropertyLabel ()
    {
        return getLabel(_property.getName());
    }

    /**
     * Gets the mode string by walking up the lineage.
     */
    protected String getMode ()
    {
        for (int ii = _lineage.length - 1; ii >= 0; ii--) {
            String mode = _lineage[ii].getAnnotation().mode();
            if (!Editable.INHERIT_STRING.equals(mode)) {
                return mode;
            }
        }
        return "";
    }

    /**
     * Gets the units string by walking up the lineage.
     */
    protected String getUnits ()
    {
        for (int ii = _lineage.length - 1; ii >= 0; ii--) {
            String units = _lineage[ii].getAnnotation().units();
            if (!Editable.INHERIT_STRING.equals(units)) {
                return units;
            }
        }
        return "";
    }

    /**
     * Gets the minimum value by walking up the lineage.
     */
    protected double getMinimum ()
    {
        for (int ii = _lineage.length - 1; ii >= 0; ii--) {
            double min = _lineage[ii].getAnnotation().min();
            if (min != Editable.INHERIT_DOUBLE) {
                return min;
            }
        }
        return -Double.MAX_VALUE;
    }

    /**
     * Gets the maximum value by walking up the lineage.
     */
    protected double getMaximum ()
    {
        for (int ii = _lineage.length - 1; ii >= 0; ii--) {
            double max = _lineage[ii].getAnnotation().max();
            if (max != Editable.INHERIT_DOUBLE) {
                return max;
            }
        }
        return +Double.MAX_VALUE;
    }

    /**
     * Gets the step by walking up the lineage.
     */
    protected double getStep ()
    {
        for (int ii = _lineage.length - 1; ii >= 0; ii--) {
            double step = _lineage[ii].getAnnotation().step();
            if (step != Editable.INHERIT_DOUBLE) {
                return step;
            }
        }
        return 1.0;
    }

    /**
     * Gets the scale by walking up the lineage.
     */
    protected double getScale ()
    {
        for (int ii = _lineage.length - 1; ii >= 0; ii--) {
            double scale = _lineage[ii].getAnnotation().scale();
            if (scale != Editable.INHERIT_DOUBLE) {
                return scale;
            }
        }
        return 1.0;
    }

    /**
     * Gets the minimum size by walking up the lineage.
     */
    protected int getMinSize ()
    {
        for (int ii = _lineage.length - 1; ii >= 0; ii--) {
            int min = _lineage[ii].getAnnotation().minsize();
            if (min != Editable.INHERIT_INTEGER) {
                return min;
            }
        }
        return 0;
    }

    /**
     * Gets the maximum size by walking up the lineage.
     */
    protected int getMaxSize ()
    {
        for (int ii = _lineage.length - 1; ii >= 0; ii--) {
            int max = _lineage[ii].getAnnotation().maxsize();
            if (max != Editable.INHERIT_INTEGER) {
                return max;
            }
        }
        return +Integer.MAX_VALUE;
    }

    /**
     * Editor for boolean properties.
     */
    protected static class BooleanEditor extends PropertyEditor
        implements ActionListener
    {
        // documentation inherited from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            Boolean selected = _box.isSelected();
            if (!_property.get(_object).equals(selected)) {
                _property.set(_object, selected);
                fireStateChanged();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            add(_box = new JCheckBox(getPropertyLabel()));
            _box.setBackground(null);
            Dimension size = _box.getPreferredSize();
            _box.setPreferredSize(new Dimension(size.width, 16));
            _box.addActionListener(this);
        }

        @Override // documentation inherited
        protected void update ()
        {
            _box.setSelected((Boolean)_property.get(_object));
        }

        /** The check box. */
        protected JCheckBox _box;
    }

    /**
     * Editor for string properties.
     */
    protected static class StringEditor extends PropertyEditor
        implements DocumentListener
    {
        // documentation inherited from interface DocumentListener
        public void insertUpdate (DocumentEvent event)
        {
            changedUpdate(null);
        }

        // documentation inherited from interface DocumentListener
        public void removeUpdate (DocumentEvent event)
        {
            changedUpdate(null);
        }

        // documentation inherited from interface DocumentListener
        public void changedUpdate (DocumentEvent event)
        {
            String text = _field.getText();
            if (!_property.get(_object).equals(text)) {
                _property.set(_object, text);
                fireStateChanged();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            add(new JLabel(getPropertyLabel() + ":"));
            Editable annotation = _property.getAnnotation();
            add(_field = new JTextField(annotation.width()));
            _field.getDocument().addDocumentListener(this);
        }

        @Override // documentation inherited
        protected void update ()
        {
            _field.setText((String)_property.get(_object));
        }

        /** The text field. */
        protected JTextField _field;
    }

    /**
     * Editor for color properties.
     */
    protected static class Color4fEditor extends PropertyEditor
        implements ChangeListener
    {
        // documentation inherited from interface ChangeListener
        public void stateChanged (ChangeEvent event)
        {
            Color4f color = new Color4f(_chooser.getColor());
            if (_spinner != null) {
                color.a = ((Number)_spinner.getValue()).floatValue();
            }
            if (!_property.get(_object).equals(color)) {
                _property.set(_object, color);
                _button.setBackground(_chooser.getColor());
                fireStateChanged();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            add(new JLabel(getPropertyLabel() + ":"));
            add(_button = new JButton());
            _button.setPreferredSize(new Dimension(40, 20));
            _button.addActionListener(new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    _dialog.setVisible(true);
                }
            });
            _chooser = new JColorChooser();
            _chooser.getSelectionModel().addChangeListener(this);
            _dialog = JColorChooser.createDialog(
                this, getPropertyLabel(), false, _chooser, null, null);
            if (getMode().equals("alpha")) {
                add(_spinner = new DraggableSpinner(1f, 0f, 1f, 0.01f));
                _spinner.addChangeListener(this);
            }
        }

        @Override // documentation inherited
        protected void update ()
        {
            Color4f color = (Color4f)_property.get(_object);
            _chooser.setColor(color.getColor(false));
            _button.setBackground(_chooser.getColor());
            if (_spinner != null) {
                _spinner.setValue(color.a);
            }
        }

        /** The button that brings up the color selection dialog. */
        protected JButton _button;

        /** The color chooser. */
        protected JColorChooser _chooser;

        /** The color chooser dialog. */
        protected JDialog _dialog;

        /** The alpha spinner. */
        protected DraggableSpinner _spinner;
    }

    /**
     * Editor for vector properties.
     */
    protected static class Vector3fEditor extends PropertyEditor
        implements ChangeListener
    {
        // documentation inherited from interface ChangeListener
        public void stateChanged (ChangeEvent event)
        {
            Vector3f value = _panel.getValue();
            if (!_property.get(_object).equals(value)) {
                _property.set(_object, value);
                fireStateChanged();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
            setBorder(BorderFactory.createTitledBorder(getPropertyLabel()));
            Editable annotation = _property.getAnnotation();
            String mstr = getMode();
            VectorPanel.Mode mode = VectorPanel.Mode.CARTESIAN;
            try {
                mode = Enum.valueOf(VectorPanel.Mode.class, StringUtil.toUSUpperCase(mstr));
            } catch (IllegalArgumentException e) { }
            add(_panel = new VectorPanel(_msgs, mode, (float)getStep(), (float)getScale()));
            _panel.setBackground(getDarkerBackground(_lineage.length));
            _panel.addChangeListener(this);
        }

        @Override // documentation inherited
        protected void update ()
        {
            _panel.setValue((Vector3f)_property.get(_object));
        }

        /** The vector panel. */
        protected VectorPanel _panel;
    }

    /**
     * Editor for quaternion properties.
     */
    protected static class QuaternionEditor extends PropertyEditor
        implements ChangeListener
    {
        // documentation inherited from interface ChangeListener
        public void stateChanged (ChangeEvent event)
        {
            Quaternion value = _panel.getValue();
            if (!_property.get(_object).equals(value)) {
                _property.set(_object, value);
                fireStateChanged();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
            setBorder(BorderFactory.createTitledBorder(getPropertyLabel()));
            add(_panel = new QuaternionPanel(_msgs));
            _panel.setBackground(getDarkerBackground(_lineage.length));
            _panel.addChangeListener(this);
        }

        @Override // documentation inherited
        protected void update ()
        {
            _panel.setValue((Quaternion)_property.get(_object));
        }

        /** The quaternion panel. */
        protected QuaternionPanel _panel;
    }

    /**
     * Editor for transform properties.
     */
    protected static class TransformEditor extends PropertyEditor
        implements ChangeListener
    {
        // documentation inherited from interface ChangeListener
        public void stateChanged (ChangeEvent event)
        {
            Transform value = _panel.getValue();
            if (!_property.get(_object).equals(value)) {
                _property.set(_object, value);
                fireStateChanged();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
            setBorder(BorderFactory.createTitledBorder(getPropertyLabel()));
            Editable annotation = _property.getAnnotation();
            String mstr = getMode();
            TransformPanel.Mode mode = TransformPanel.Mode.UNIFORM;
            try {
                mode = Enum.valueOf(TransformPanel.Mode.class, StringUtil.toUSUpperCase(mstr));
            } catch (IllegalArgumentException e) { }
            add(_panel = new TransformPanel(_msgs, mode, (float)getStep(), (float)getScale()));
            _panel.setBackground(getDarkerBackground(_lineage.length));
            Color ddarker = getDarkerBackground(_lineage.length + 1);
            _panel.getTranslationPanel().setBackground(ddarker);
            _panel.getRotationPanel().setBackground(ddarker);
            _panel.addChangeListener(this);
        }

        @Override // documentation inherited
        protected void update ()
        {
            _panel.setValue((Transform)_property.get(_object));
        }

        /** The transform panel. */
        protected TransformPanel _panel;
    }

    /**
     * Editor for enumerated type properties.
     */
    protected static class EnumEditor extends PropertyEditor
        implements ActionListener
    {
        // documentation inherited from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            Object value = _property.getType().getEnumConstants()[_box.getSelectedIndex()];
            if (!_property.get(_object).equals(value)) {
                _property.set(_object, value);
                fireStateChanged();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            add(new JLabel(getPropertyLabel() + ":"));
            Enum[] constants = (Enum[])_property.getType().getEnumConstants();
            String[] names = new String[constants.length];
            for (int ii = 0; ii < constants.length; ii++) {
                names[ii] = getLabel(StringUtil.toUSLowerCase(constants[ii].name()));
            }
            add(_box = new JComboBox(names));
            _box.addActionListener(this);
        }

        @Override // documentation inherited
        protected void update ()
        {
            _box.setSelectedIndex(ListUtil.indexOf(
                _property.getType().getEnumConstants(), _property.get(_object)));
        }

        /** The combo box. */
        protected JComboBox _box;
    }

    /**
     * Editor for file properties.
     */
    protected static class FileEditor extends PropertyEditor
        implements ActionListener
    {
        // documentation inherited from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            File value;
            if (event.getSource() == _file) {
                final FileConstraints constraints = _property.getAnnotation(FileConstraints.class);
                String key = (constraints == null || constraints.directory().length() == 0) ?
                    null : constraints.directory();
                if (_chooser == null) {
                    _chooser = new JFileChooser(key == null ? null : _prefs.get(key, null));
                    if (getMode().equals("directory")) {
                        _chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                    } else if (constraints != null) {
                        _chooser.setFileFilter(new FileFilter() {
                            public boolean accept (File file) {
                                if (file.isDirectory()) {
                                    return true;
                                }
                                String name = file.getName();
                                for (String extension : constraints.extensions()) {
                                    if (name.endsWith(extension)) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                            public String getDescription () {
                                return _msgs.get(constraints.description());
                            }
                        });
                    }
                }
                _chooser.setSelectedFile((File)_property.get(_object));
                int result = _chooser.showOpenDialog(this);
                if (key != null) {
                    _prefs.put(key, _chooser.getCurrentDirectory().toString());
                }
                if (result != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                value = _chooser.getSelectedFile();

            } else { // event.getSource() == _clear
                value = null;
            }
            if (!ObjectUtil.equals(_property.get(_object), value)) {
                _property.set(_object, value);
                updateButtons(value);
                fireStateChanged();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            add(new JLabel(getPropertyLabel() + ":"));
            add(_file = new JButton(" "));
            _file.setPreferredSize(new Dimension(75, _file.getPreferredSize().height));
            _file.addActionListener(this);
            if (_property.getAnnotation().nullable()) {
                add(_clear = new JButton(_msgs.get("m.clear")));
                _clear.addActionListener(this);
            }
        }

        @Override // documentation inherited
        protected void update ()
        {
            updateButtons((File)_property.get(_object));
        }

        /**
         * Updates the state of the buttons.
         */
        protected void updateButtons (File value)
        {
            _file.setText(value == null ? _msgs.get("m.none") : value.getName());
            if (_clear != null) {
                _clear.setEnabled(value != null);
            }
        }

        /** The file button. */
        protected JButton _file;

        /** The clear button. */
        protected JButton _clear;

        /** The file chooser. */
        protected JFileChooser _chooser;
    }

    /**
     * An editor for numerical values.
     */
    protected static class NumberEditor extends PropertyEditor
        implements ChangeListener
    {
        // documentation inherited from interface ChangeListener
        public void stateChanged (ChangeEvent event)
        {
            double value;
            if (event.getSource() == _slider) {
                value = _slider.getValue() * _step;
                _spinner.setValue(value);
            } else { // event.getSource() == _spinner
                value = (Double)_spinner.getValue();
                if (_slider != null) {
                    _slider.setValue((int)Math.round(value / _step));
                }
            }
            Number nvalue = fromDouble(value * _scale);
            if (!_property.get(_object).equals(nvalue)) {
                _property.set(_object, nvalue);
                fireStateChanged();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            add(new JLabel(getPropertyLabel() + ":"));
            Editable annotation = _property.getAnnotation();
            double min = getMinimum();
            double max = getMaximum();
            _step = getStep();
            _scale = getScale();
            if (getMode().equals("wide") && min != -Double.MAX_VALUE &&
                    max != +Double.MAX_VALUE) {
                add(_slider = new JSlider(
                    (int)Math.round(min / _step),
                    (int)Math.round(max / _step)));
                _slider.setBackground(null);
                _slider.addChangeListener(this);
            }
            add(_spinner = new DraggableSpinner(min, min, max, _step));
            _spinner.addChangeListener(this);
            String units = getUnits();
            if (units.length() > 0) {
                add(new JLabel(getLabel(units)));
            }
        }

        @Override // documentation inherited
        protected void update ()
        {
            double value = ((Number)_property.get(_object)).doubleValue() / _scale;
            _spinner.setValue(value);
            if (_slider != null) {
                _slider.setValue((int)Math.round(value / _step));
            }
        }

        /**
         * Converts a double value to a value of the property's type.
         */
        protected Number fromDouble (double value)
        {
            Class type = _property.getType();
            if (type == Byte.TYPE || type == Byte.class) {
                return (byte)value;
            } else if (type == Double.TYPE || type == Double.class) {
                return value;
            } else if (type == Float.TYPE || type == Float.class) {
                return (float)value;
            } else if (type == Integer.TYPE || type == Integer.class) {
                return (int)value;
            } else if (type == Long.TYPE || type == Long.class) {
                return (long)value;
            } else { // type == Short.TYPE || type == Short.class
                return (short)value;
            }
        }

        /** The slider. */
        protected JSlider _slider;

        /** The spinner. */
        protected DraggableSpinner _spinner;

        /** The step size as retrieved from the annotation. */
        protected double _step;

        /** The scale as retrieved from the annotation. */
        protected double _scale;
    }

    /**
     * An editor for objects with editable properties.
     */
    protected static class ObjectEditor extends PropertyEditor
        implements ChangeListener
    {
        // documentation inherited from interface ChangeListener
        public void stateChanged (ChangeEvent event)
        {
            _property.set(_object, _panel.getValue());
            fireStateChanged();
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
            setBorder(BorderFactory.createTitledBorder(getPropertyLabel()));
            add(_panel = new ObjectPanel(
                _msgs, _property.getTypeLabel(), _property.getSubtypes(), _lineage));
            _panel.addChangeListener(this);
        }

        @Override // documentation inherited
        protected void update ()
        {
            _panel.removeChangeListener(this);
            _panel.setValue(_property.get(_object));
            _panel.addChangeListener(this);
        }

        /** The object panel. */
        protected ObjectPanel _panel;
    }

    /**
     * An editor for arrays or lists of objects.
     */
    protected static class ArrayListEditor extends PropertyEditor
        implements ActionListener, ChangeListener
    {
        // documentation inherited from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            Class[] types = _property.getComponentSubtypes();
            Class type = (types[0] == null) ? types[1] : types[0];
            try {
                addValue(type.newInstance());
            } catch (Exception e) {
                log.log(WARNING, "Failed to instantiate component [class=" + type + "].", e);
            }
        }

        // documentation inherited from interface ChangeListener
        public void stateChanged (ChangeEvent event)
        {
            ObjectPanel panel = (ObjectPanel)event.getSource();
            int idx = getPanelIndex(panel);
            if (_property.getType().isArray()) {
                Array.set(_property.get(_object), idx, panel.getValue());
            } else {
                @SuppressWarnings("unchecked") List<Object> values =
                    (List<Object>)_property.get(_object);
                values.set(idx, panel.getValue());
            }
            fireStateChanged();
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _min = getMinSize();
            _max = getMaxSize();

            setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
            setBorder(BorderFactory.createTitledBorder(getPropertyLabel()));

            add(_panels = GroupLayout.makeVBox(
                GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
            _panels.setBackground(null);

            JPanel bpanel = new JPanel();
            bpanel.setBackground(null);
            add(bpanel);
            bpanel.add(_addButton = new JButton(_msgs.get("m.add")));
            _addButton.addActionListener(this);
        }

        @Override // documentation inherited
        protected void update ()
        {
            int pcount = _panels.getComponentCount();
            int length = getLength();
            for (int ii = 0; ii < length; ii++) {
                Object value = getValue(ii);
                if (ii < pcount) {
                    ObjectPanel panel = (ObjectPanel)_panels.getComponent(ii);
                    panel.removeChangeListener(this);
                    panel.setValue(value);
                    panel.addChangeListener(this);
                } else {
                    addPanel(value);
                }
            }
            while (pcount > length) {
                _panels.remove(--pcount);
            }
            _panels.revalidate();
        }

        /**
         * Returns the length of the array or list property.
         */
        protected int getLength ()
        {
            Object values = _property.get(_object);
            return (values == null) ? 0 :
                (values.getClass().isArray() ? Array.getLength(values) : ((List)values).size());
        }

        /**
         * Returns the element at the specified index of the array or list.
         */
        protected Object getValue (int idx)
        {
            Object values = _property.get(_object);
            return values.getClass().isArray() ? Array.get(values, idx) : ((List)values).get(idx);
        }

        /**
         * Adds an object to the end of the list.
         */
        protected void addValue (Object value)
        {
            Class type = _property.getType();
            if (type.isArray()) {
                Object ovalues = _property.get(_object);
                int olength = (ovalues == null) ? 0 : Array.getLength(ovalues);
                Object nvalues = Array.newInstance(type.getComponentType(), olength + 1);
                if (olength > 0) {
                    System.arraycopy(ovalues, 0, nvalues, 0, olength);
                }
                Array.set(nvalues, olength, value);
                _property.set(_object, nvalues);

            } else {
                List values = (List)_property.get(_object);
                if (values == null) {
                    try {
                        _property.set(_object, values = (List)type.newInstance());
                    } catch (Exception e) {
                        log.log(WARNING, "Failed to instantiate list [class=" + type + "].", e);
                        return;
                    }
                }
                @SuppressWarnings("unchecked") List<Object> list = values;
                list.add(value);
            }
            addPanel(value);
            _panels.revalidate();
            _addButton.setEnabled(getLength() < _max);
            fireStateChanged();
        }

        /**
         * Removes the element at the specified index.
         */
        protected void removeValue (int idx)
        {
            Class type = _property.getType();
            if (type.isArray()) {
                Object ovalues = _property.get(_object);
                int olength = Array.getLength(ovalues);
                Object nvalues = Array.newInstance(type.getComponentType(), olength - 1);
                System.arraycopy(ovalues, 0, nvalues, 0, idx);
                System.arraycopy(ovalues, idx + 1, nvalues, idx, olength - idx - 1);
                _property.set(_object, nvalues);

            } else {
                List values = (List)_property.get(_object);
                values.remove(idx);
            }
            _panels.remove(idx);
            _panels.revalidate();
            _addButton.setEnabled(getLength() < _max);
            fireStateChanged();
        }

        /**
         * Adds an object panel for the specified entry.
         */
        protected void addPanel (Object value)
        {
            final ObjectPanel panel = new ObjectPanel(
                _msgs, _property.getComponentTypeLabel(),
                _property.getComponentSubtypes(), _lineage);
            _panels.add(panel);
            panel.setValue(value);
            panel.addChangeListener(this);

            JPanel bpanel = new JPanel();
            bpanel.setBackground(null);
            panel.add(bpanel);
            if (getLength() > _min) {
                JButton delete = new JButton(_msgs.get("m.delete"));
                bpanel.add(delete);
                delete.addActionListener(new ActionListener() {
                    public void actionPerformed (ActionEvent event) {
                        int idx = ListUtil.indexOfRef(_panels.getComponents(), panel);
                        removeValue(idx);
                    }
                });
            }

            panel.add(new JSeparator());
        }

        /**
         * Finds and returns the index of the specified panel.
         */
        protected int getPanelIndex (ObjectPanel panel)
        {
            for (int ii = 0, nn = _panels.getComponentCount(); ii < nn; ii++) {
                if (_panels.getComponent(ii) == panel) {
                    return ii;
                }
            }
            return -1;
        }

        /** The container holding the panels. */
        protected JPanel _panels;

        /** The add-value button. */
        protected JButton _addButton;

        /** The minimum and maximum sizes of the list. */
        protected int _min, _max;
    }

    /** The property being edited. */
    protected Property _property;

    /** The ancestors of the property (if any), followed by the property itself. */
    protected Property[] _lineage;

    /** User preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(PropertyEditor.class);

    /** Maps names to editor classes. */
    protected static HashMap<String, Class<? extends PropertyEditor>> _classesByName =
        new HashMap<String, Class<? extends PropertyEditor>>();

    /** Maps types to editor classes. */
    protected static HashMap<Class, Class<? extends PropertyEditor>> _classesByType =
        new HashMap<Class, Class<? extends PropertyEditor>>();
    static {
        registerEditorClass(Boolean.class, BooleanEditor.class);
        registerEditorClass(Boolean.TYPE, BooleanEditor.class);
        registerEditorClass(Byte.class, NumberEditor.class);
        registerEditorClass(Byte.TYPE, NumberEditor.class);
        registerEditorClass(Color4f.class, Color4fEditor.class);
        registerEditorClass(Double.class, NumberEditor.class);
        registerEditorClass(Double.TYPE, NumberEditor.class);
        registerEditorClass(File.class, FileEditor.class);
        registerEditorClass(Float.class, NumberEditor.class);
        registerEditorClass(Float.TYPE, NumberEditor.class);
        registerEditorClass(Integer.class, NumberEditor.class);
        registerEditorClass(Integer.TYPE, NumberEditor.class);
        registerEditorClass(Long.class, NumberEditor.class);
        registerEditorClass(Long.TYPE, NumberEditor.class);
        registerEditorClass(Quaternion.class, QuaternionEditor.class);
        registerEditorClass(Short.class, NumberEditor.class);
        registerEditorClass(Short.TYPE, NumberEditor.class);
        registerEditorClass(String.class, StringEditor.class);
        registerEditorClass(Transform.class, TransformEditor.class);
        registerEditorClass(Vector3f.class, Vector3fEditor.class);
    }
}
