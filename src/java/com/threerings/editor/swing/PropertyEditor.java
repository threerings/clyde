//
// $Id$

package com.threerings.editor.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
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
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import javax.swing.filechooser.FileFilter;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntTuple;
import com.samskivert.util.ListUtil;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.config.ConfigReference;
import com.threerings.config.ManagedConfig;
import com.threerings.config.ParameterizedConfig;
import com.threerings.config.swing.ConfigChooser;

import com.threerings.math.Quaternion;
import com.threerings.math.Transform;
import com.threerings.math.Vector3f;

import com.threerings.opengl.renderer.Color4f;

import com.threerings.editor.Editable;
import com.threerings.editor.FileConstraints;
import com.threerings.editor.Introspector;
import com.threerings.editor.Property;
import com.threerings.editor.util.EditorContext;

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
        EditorContext ctx, Property property, Property[] ancestors)
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
                log.warning("Failed to create property editor.", e);
                editor = new ObjectEditor();
            }
        } else if (type.isEnum()) {
            editor = new EnumEditor();
        } else if (type.isArray() || List.class.isAssignableFrom(type)) {
            // use the table editor when the array components are primitives (or similar, or
            // arrays thereof)
            Class ctype = property.getComponentType();
            if (isTableCellType(ctype) ||
                    (ctype.isArray() && isTableCellType(ctype.getComponentType()))) {
                editor = new TableArrayListEditor();
            } else {
                editor = new PanelArrayListEditor();
            }
        } else {
            editor = new ObjectEditor();
        }
        editor.init(ctx, property, ancestors);
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
    public void init (EditorContext ctx, Property property, Property[] ancestors)
    {
        _ctx = ctx;
        _msgs = ctx.getMessageBundle();
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
     * Checks whether the supplied type can be edited in the cell of a table.
     */
    protected static boolean isTableCellType (Class type)
    {
        return type.isPrimitive() || Number.class.isAssignableFrom(type) ||
            type == Boolean.class || type == Character.class ||
            type == String.class || type.isEnum();
    }

    /**
     * Returns a default instance for the supplied type, either by instantiating it with its no-arg
     * constructor or by obtaining some type-specific default;
     */
    protected static Object getDefaultInstance (Class type)
    {
        if (type == Boolean.class || type == Boolean.TYPE) {
            return Boolean.valueOf(false);
        } else if (type == Byte.class || type == Byte.TYPE) {
            return Byte.valueOf((byte)0);
        } else if (type == Character.class || type == Character.TYPE) {
            return Character.valueOf(' ');
        } else if (type == Double.class || type == Double.TYPE) {
            return Double.valueOf(0.0);
        } else if (type == Float.class || type == Float.TYPE) {
            return Float.valueOf(0f);
        } else if (type == Integer.class || type == Integer.TYPE) {
            return Integer.valueOf(0);
        } else if (type == Long.class || type == Long.TYPE) {
            return Long.valueOf(0L);
        } else if (type == Short.class || type == Short.TYPE) {
            return Short.valueOf((short)0);
        } else if (type == String.class) {
            return "";
        } else if (type.isEnum()) {
            return type.getEnumConstants()[0];
        } else if (type.isArray()) {
            return Array.newInstance(type.getComponentType(), 0);
        }
        try {
            return type.newInstance();
        } catch (Exception e) {
            log.warning("Failed to obtain default instance [class=" + type + "].", e);
            return null;
        }
    }

    /**
     * Returns the wrapper class for the supplied type, if it is a primitive type (otherwise
     * returns the type itself).
     */
    protected static Class getWrapperClass (Class type)
    {
        if (type == Boolean.TYPE) {
            return Boolean.class;
        } else if (type == Byte.TYPE) {
            return Byte.class;
        } else if (type == Character.TYPE) {
            return Character.class;
        } else if (type == Double.TYPE) {
            return Double.class;
        } else if (type == Float.TYPE) {
            return Float.class;
        } else if (type == Integer.TYPE) {
            return Integer.class;
        } else if (type == Long.TYPE) {
            return Long.class;
        } else if (type == Short.TYPE) {
            return Short.class;
        } else {
            return type;
        }
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
     * Edits file properties.
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
                    String ddir = getDefaultDirectory();
                    _chooser = new JFileChooser(key == null ? ddir : _prefs.get(key, ddir));
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
                _chooser.setSelectedFile(getPropertyFile());
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
            if (!ObjectUtil.equals(getPropertyFile(), value)) {
                setPropertyFile(value);
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
            updateButtons(getPropertyFile());
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

        /**
         * Returns the default directory to start in, if there is no stored preference.
         */
        protected String getDefaultDirectory ()
        {
            return null;
        }

        /**
         * Returns the value of the property as a {@link File}.
         */
        protected File getPropertyFile ()
        {
            return (File)_property.get(_object);
        }

        /**
         * Sets the value of the property as a {@link File}.
         */
        protected void setPropertyFile (File file)
        {
            _property.set(_object, file);
        }

        /** The file button. */
        protected JButton _file;

        /** The clear button. */
        protected JButton _clear;

        /** The file chooser. */
        protected JFileChooser _chooser;
    }

    /**
     * Editor for resource references, which are set as files but stored as string paths relative
     * to the resource directory.
     */
    protected static class ResourceEditor extends FileEditor
    {
        @Override // documentation inherited
        protected String getDefaultDirectory ()
        {
            return _ctx.getResourceManager().getResourceFile("").toString();
        }

        @Override // documentation inherited
        protected File getPropertyFile ()
        {
            String path = (String)_property.get(_object);
            return (path == null) ? null : _ctx.getResourceManager().getResourceFile(path);
        }

        @Override // documentation inherited
        protected void setPropertyFile (File file)
        {
            String path = null;
            if (file != null) {
                String parent = _ctx.getResourceManager().getResourceFile("").toString();
                if (!parent.endsWith(File.separator)) {
                    parent += File.separator;
                }
                String child = file.toString();
                if (child.startsWith(parent)) {
                    path = child.substring(parent.length()).replace(File.separatorChar, '/');
                }
            }
            _property.set(_object, path);
        }
    }

    /**
     * An editor for configuration references.
     */
    protected static class ConfigReferenceEditor extends PropertyEditor
        implements ActionListener
    {
        // documentation inherited from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            ConfigReference ovalue = (ConfigReference)_property.get(_object);
            ConfigReference nvalue;
            if (event.getSource() == _config) {
                if (_chooser == null) {
                    _chooser = new ConfigChooser(
                        _msgs, _ctx.getConfigManager(), _property.getArgumentType());
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
                (Class<ManagedConfig>)_property.getArgumentType();
            ManagedConfig config = _ctx.getConfigManager().getConfig(clazz, value.getName());
            ParameterizedConfig pconfig;
            if (!(config instanceof ParameterizedConfig) ||
                    (pconfig = (ParameterizedConfig)config).parameters.length == 0) {
                _params.removeAll();
                return;
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
                _ctx, _property.getTypeLabel(), _property.getSubtypes(), _lineage));
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
     * Superclass of the array/list editors.
     */
    protected static abstract class ArrayListEditor extends PropertyEditor
        implements ActionListener
    {
        // documentation inherited from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            if (event.getSource() == _add) {
                Class[] types = _property.getComponentSubtypes();
                Class type = (types[0] == null) ? types[1] : types[0];
                addValue(getDefaultInstance(type));
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _min = getMinSize();
            _max = getMaxSize();

            setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
            setBorder(BorderFactory.createTitledBorder(getPropertyLabel()));
        }

        /**
         * Returns a label for the specified action.
         */
        protected String getActionLabel (String action)
        {
            return getActionLabel(action, null);
        }

        /**
         * Returns a label for the specified action.
         *
         * @param units an optional override for the units parameter.
         */
        protected String getActionLabel (String action, String units)
        {
            units = (units == null) ? _property.getAnnotation().units() : units;
            return _msgs.get("m." + action + "_entry", (units.length() > 0) ?
                getLabel(units) : getLabel(_property.getComponentType()));
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
         * Sets the element at the specified index of the array or list.
         */
        protected void setValue (int idx, Object value)
        {
            Object values = _property.get(_object);
            if (values.getClass().isArray()) {
                Array.set(values, idx, value);
            } else {
                @SuppressWarnings("unchecked") List<Object> list = (List<Object>)values;
                list.set(idx, value);
            }
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
                        log.warning("Failed to instantiate list [class=" + type + "].", e);
                        return;
                    }
                }
                @SuppressWarnings("unchecked") List<Object> list = values;
                list.add(value);
            }
            _add.setEnabled(getLength() < _max);
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
            _add.setEnabled(getLength() < _max);
            fireStateChanged();
        }

        /** The minimum and maximum sizes of the list. */
        protected int _min, _max;

        /** The add value button. */
        protected JButton _add;
    }

    /**
     * An editor for arrays or lists of objects.  Uses embedded panels.
     */
    protected static class PanelArrayListEditor extends ArrayListEditor
        implements ChangeListener
    {
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
            super.didInit();

            add(_panels = GroupLayout.makeVBox(
                GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
            _panels.setBackground(null);

            JPanel bpanel = new JPanel();
            bpanel.setBackground(null);
            add(bpanel);
            bpanel.add(_add = new JButton(getActionLabel("new")));
            _add.addActionListener(this);
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

        @Override // documentation inherited
        protected void addValue (Object value)
        {
            super.addValue(value);
            addPanel(value);
            _panels.revalidate();
        }

        @Override // documentation inherited
        protected void removeValue (int idx)
        {
            super.removeValue(idx);
            _panels.remove(idx);
            _panels.revalidate();
        }

        /**
         * Adds an object panel for the specified entry.
         */
        protected void addPanel (Object value)
        {
            final ObjectPanel panel = new ObjectPanel(
                _ctx, _property.getComponentTypeLabel(),
                _property.getComponentSubtypes(), _lineage);
            _panels.add(panel);
            panel.setValue(value);
            panel.addChangeListener(this);

            JPanel bpanel = new JPanel();
            bpanel.setBackground(null);
            panel.add(bpanel);
            if (getLength() > _min) {
                JButton delete = new JButton(getActionLabel("delete"));
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
    }

    /**
     * An editor for objects or lists of objects or primitives.  Uses a table.
     */
    protected static class TableArrayListEditor extends ArrayListEditor
        implements TableModel, ListSelectionListener
    {
        // documentation inherited from interface TableModel
        public int getRowCount ()
        {
            return getLength();
        }

        // documentation inherited from interface TableModel
        public int getColumnCount ()
        {
            return _columns.length;
        }

        // documentation inherited from interface TableModel
        public String getColumnName (int column)
        {
            return _columns[column].getName();
        }

        // documentation inherited from interface TableModel
        public Class<?> getColumnClass (int column)
        {
            return _columns[column].getColumnClass();
        }

        // documentation inherited from interface TableModel
        public boolean isCellEditable (int row, int column)
        {
            return true;
        }

        // documentation inherited from interface TableModel
        public Object getValueAt (int row, int column)
        {
            return _columns[column].getColumnValue(row);
        }

        // documentation inherited from interface TableModel
        public void setValueAt (Object value, int row, int column)
        {
            _columns[column].setColumnValue(row, value);
            fireTableChanged(row, row, column, TableModelEvent.UPDATE);
            fireStateChanged();
        }

        // documentation inherited from interface TableModel
        public void addTableModelListener (TableModelListener listener)
        {
            listenerList.add(TableModelListener.class, listener);
        }

        // documentation inherited from interface TableModel
        public void removeTableModelListener (TableModelListener listener)
        {
            listenerList.add(TableModelListener.class, listener);
        }

        // documentation inherited from interface ListSelectionListener
        public void valueChanged (ListSelectionEvent event)
        {
            updateSelected();
        }

        @Override // documentation inherited
        public void actionPerformed (ActionEvent event)
        {
            Object source = event.getSource();
            if (source == _add && is2DArray()) {
                // create a new row of the required type, populated with default instances
                Class cctype = _property.getComponentType().getComponentType();
                Object value = Array.newInstance(cctype, _columns.length);
                for (int ii = 0; ii < _columns.length; ii++) {
                    Array.set(value, ii, getDefaultInstance(cctype));
                }
                addValue(value);

            } else if (source == _addColumn) {
                addColumn();

            } else if (source == _delete) {
                IntTuple selection = getSelection();
                if (selection.right == -1) {
                    removeValue(selection.left);
                } else {
                    removeColumn(selection.right);
                }
            } else {
                super.actionPerformed(event);
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            super.didInit();

            // determine the column model
            final Class ctype = _property.getComponentType();
            if (is2DArray()) {
                _columns = new Column[0]; // actual columns will be created on update

            } else if (isTableCellType(ctype)) {
                _columns = new Column[] { new Column() {
                    public String getName () {
                        return null;
                    }
                    public Class getColumnClass () {
                        return getWrapperClass(ctype);
                    }
                    public Object getColumnValue (int row) {
                        return getValue(row);
                    }
                    public void setColumnValue (int row, Object value) {
                        setValue(row, value);
                    }
                    public int getWidth () {
                        return _property.getAnnotation().width();
                    }
                }};
            } else {
                Property[] properties = Introspector.getProperties(ctype);
                _columns = new Column[properties.length];
                for (int ii = 0; ii < properties.length; ii++) {
                    final Property property = properties[ii];
                    _columns[ii] = new Column() {
                        public String getName () {
                            return getLabel(property.getName());
                        }
                        public Class getColumnClass () {
                            return getWrapperClass(property.getType());
                        }
                        public Object getColumnValue (int row) {
                            return property.get(getValue(row));
                        }
                        public void setColumnValue (int row, Object value) {
                            property.set(getValue(row), value);
                        }
                        public int getWidth () {
                           return property.getAnnotation().width();
                        }
                    };
                }
            }

            ((GroupLayout)getLayout()).setOffAxisPolicy(GroupLayout.NONE);

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(null);
            add(panel);
            panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            _table = new JTable(this);
            if (!isTableCellType(ctype)) {
                _table.getTableHeader().setReorderingAllowed(false);
                panel.add(_table.getTableHeader(), BorderLayout.NORTH);
            }
            updateColumnWidths();
            panel.add(_table, BorderLayout.CENTER);
            if (is2DArray()) {
                _table.setColumnSelectionAllowed(true);
                _table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            } else {
                _table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            }
            _table.getSelectionModel().addListSelectionListener(this);
            _table.getColumnModel().getSelectionModel().addListSelectionListener(this);

            // hacky transferable lets us move rows around in the array
            _table.setDragEnabled(true);
            final DataFlavor cflavor = new DataFlavor(IntTuple.class, null);
            _table.setTransferHandler(new TransferHandler() {
                public int getSourceActions (JComponent comp) {
                    return MOVE;
                }
                public boolean canImport (JComponent comp, DataFlavor[] flavors) {
                    return ListUtil.containsRef(flavors, cflavor);
                }
                public boolean importData (JComponent comp, Transferable t) {
                    try {
                        IntTuple selection = (IntTuple)t.getTransferData(cflavor);
                        if (selection.left == -1) {
                            moveColumn(selection.right);
                        } else if (selection.right == -1) {
                            moveValue(selection.left);
                        } else {
                            moveCell(selection.left, selection.right);
                        }
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
                protected Transferable createTransferable (JComponent c) {
                    final IntTuple selection = getSelection();
                    if (selection == null) {
                        return null;
                    }
                    // set the selection mode depending on the selection type
                    if (is2DArray()) {
                        if (selection.left == -1) {
                            _table.setRowSelectionAllowed(false);
                        } else if (selection.right == -1) {
                            _table.setColumnSelectionAllowed(false);
                        }
                    }
                    return new Transferable() {
                        public Object getTransferData (DataFlavor flavor) {
                            return selection;
                        }
                        public DataFlavor[] getTransferDataFlavors () {
                            return new DataFlavor[] { cflavor };
                        }
                        public boolean isDataFlavorSupported (DataFlavor flavor) {
                            return flavor == cflavor;
                        }
                    };
                }
                protected void exportDone (JComponent source, Transferable data, int action) {
                    // restore the selection mode
                    if (is2DArray()) {
                        _table.setCellSelectionEnabled(true);
                    }
                }
            });

            JPanel bpanel = new JPanel();
            bpanel.setBackground(null);
            add(bpanel);
            bpanel.add(_add = new JButton(is2DArray() ?
                getActionLabel("new", "row") : _msgs.get("m.new")));
            _add.addActionListener(this);
            if (is2DArray()) {
                bpanel.add(_addColumn = new JButton(getActionLabel("new", "column")));
                _addColumn.addActionListener(this);
            }
            bpanel.add(_delete = new JButton(_msgs.get("m.delete")));
            _delete.addActionListener(this);
        }

        @Override // documentation inherited
        protected void update ()
        {
            int min = 0, max = Integer.MAX_VALUE;
            if (is2DArray()) {
                createArrayColumns();
                min = max = TableModelEvent.HEADER_ROW;
            }
            fireTableChanged(min, max, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
            if (min == TableModelEvent.HEADER_ROW) {
                updateColumnWidths();
            }
            updateSelected();
        }

        @Override // documentation inherited
        protected void addValue (Object value)
        {
            super.addValue(value);
            int row = getLength() - 1;
            fireTableChanged(row, row, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
            if (_columns.length > 0) {
                setSelection(row, -1);
            }
        }

        @Override // documentation inherited
        protected void removeValue (int idx)
        {
            super.removeValue(idx);
            fireTableChanged(idx, idx, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE);
            setSelection(Math.min(idx, getLength() - 1), -1);
        }

        /**
         * Adds a new column.
         */
        protected void addColumn ()
        {
            // update the column model
            _columns = ArrayUtil.append(_columns, createArrayColumn(_columns.length));

            // expand all rows to include the new column
            Class cctype = _property.getComponentType().getComponentType();
            for (int ii = 0, nn = getLength(); ii < nn; ii++) {
                Object ovalue = getValue(ii);
                Object nvalue = Array.newInstance(cctype, _columns.length);
                System.arraycopy(ovalue, 0, nvalue, 0, _columns.length - 1);
                Array.set(nvalue, _columns.length - 1, getDefaultInstance(cctype));
                setValue(ii, nvalue);
            }

            // fire notification events, update selection
            fireStateChanged();
            fireTableChanged(
                TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW,
                TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
            updateColumnWidths();
            if (getLength() > 0) {
                setSelection(-1, _columns.length - 1);
            }
        }

        /**
         * Deletes the column at the specified index.
         */
        protected void removeColumn (int column)
        {
            // update the column model
            _columns = ArrayUtil.splice(_columns, _columns.length - 1);

            // remove the column from all rows
            Class cctype = _property.getComponentType().getComponentType();
            for (int ii = 0, nn = getLength(); ii < nn; ii++) {
                Object ovalue = getValue(ii);
                Object nvalue = Array.newInstance(cctype, _columns.length);
                System.arraycopy(ovalue, 0, nvalue, 0, column);
                System.arraycopy(ovalue, column + 1, nvalue, column, _columns.length - column);
                setValue(ii, nvalue);
            }

            // fire notification events, update selection
            fireStateChanged();
            fireTableChanged(
                TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW,
                TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
            updateColumnWidths();
            setSelection(-1, Math.min(column, _columns.length - 1));
        }

        /**
         * Updates the preferred widths of the columns.
         */
        protected void updateColumnWidths ()
        {
            for (int ii = 0; ii < _columns.length; ii++) {
                // the default width is in characters, so fudge it a bit for pixels
                _table.getColumnModel().getColumn(ii).setPreferredWidth(
                    _columns[ii].getWidth() * 10);
            }
        }

        /**
         * Determines whether the property is a 2D array.
         */
        protected boolean is2DArray ()
        {
            Class ctype = _property.getComponentType();
            return ctype.isArray() && isTableCellType(ctype.getComponentType());
        }

        /**
         * (Re)creates the columns for a 2D array property.
         */
        protected void createArrayColumns ()
        {
            Object element = (getLength() == 0) ? null : getValue(0);
            _columns = new Column[element == null ? 0 : Array.getLength(element)];
            for (int ii = 0; ii < _columns.length; ii++) {
                _columns[ii] = createArrayColumn(ii);
            }
        }

        /**
         * Creates and returns an array column.
         */
        protected Column createArrayColumn (final int column)
        {
            final Class cctype = _property.getComponentType().getComponentType();
            return new Column() {
                public String getName () {
                    return Integer.toString(column);
                }
                public Class getColumnClass () {
                    return getWrapperClass(cctype);
                }
                public Object getColumnValue (int row) {
                    return Array.get(getValue(row), column);
                }
                public void setColumnValue (int row, Object value) {
                    Array.set(getValue(row), column, value);
                }
                public int getWidth () {
                    return _property.getAnnotation().width();
                }
            };
        }

        /**
         * Moves the specified row to the selected row.
         */
        protected void moveValue (int row)
        {
            int selected = _table.getSelectedRow();
            if (selected == row) {
                return;
            }
            // store the value at the original row and shift the intermediate values up/down
            Object value = getValue(row);
            int dir = (selected < row) ? -1 : +1;
            for (int ii = row; ii != selected; ii += dir) {
                setValue(ii, getValue(ii + dir));
            }
            setValue(selected, value);
            fireTableChanged(
                Math.min(selected, row), Math.max(selected, row),
                TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
            fireStateChanged();
            setSelection(selected, -1);
        }

        /**
         * Moves a column to the selected column.
         */
        protected void moveColumn (int column)
        {
            int selected = _table.getSelectedColumn();
            if (selected == column) {
                return;
            }
            for (int ii = 0, nn = getLength(); ii < nn; ii++) {
                moveWithinArray(getValue(ii), column, selected);
            }
            fireTableChanged(
                0, Integer.MAX_VALUE, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
            fireStateChanged();
            setSelection(-1, selected);
        }

        /**
         * Moves a single cell to the selected cell.
         */
        protected void moveCell (int row, int col)
        {
            int srow = _table.getSelectedRow();
            int scol = _table.getSelectedColumn();
            if (!(srow == row ^ scol == col)) {
                return; // must move within same column or same row
            }
            if (srow == row) {
                moveWithinArray(getValue(row), col, scol);
                fireTableChanged(row, row, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
            } else { // scol == col
                Column column = _columns[col];
                Object value = column.getColumnValue(row);
                int dir = (srow < row) ? -1 : +1;
                for (int ii = row; ii != srow; ii += dir) {
                    column.setColumnValue(ii, column.getColumnValue(ii + dir));
                }
                column.setColumnValue(srow, value);
                fireTableChanged(
                    Math.min(srow, row), Math.max(srow, row), col, TableModelEvent.UPDATE);
            }
            fireStateChanged();
        }

        /**
         * Moves the value at <code>source</code> to <code>dest</code>, shifting values left
         * or right to make room.
         */
        protected void moveWithinArray (Object array, int source, int dest)
        {
            Object value = Array.get(array, source);
            if (dest < source) {
                System.arraycopy(array, dest, array, dest + 1, source - dest);
            } else {
                System.arraycopy(array, source + 1, array, source, dest - source);
            }
            Array.set(array, dest, value);
        }

        /**
         * Updates based on the selection state.
         */
        protected void updateSelected ()
        {
            IntTuple selection = getSelection();
            _delete.setEnabled(selection != null && (selection.left == -1 ||
                (selection.right == -1 && getLength() > _min)));
        }

        /**
         * Returns the selection as a (row, column) pair.  If an entire row is selected, column
         * will be -1.  If an entire column is selected, row will be -1.  If both numbers are
         * valid, a single cell at that location is selected.  Otherwise, the method returns
         * <code>null</code> to indicate that there is no usable selection.
         */
        protected IntTuple getSelection ()
        {
            if (!_table.getColumnSelectionAllowed()) {
                int row = _table.getSelectedRow();
                return (row == -1) ? null : new IntTuple(row, -1);
            } else if (!_table.getRowSelectionAllowed()) {
                int column = _table.getSelectedColumn();
                return (column == -1) ? null : new IntTuple(-1, column);
            }
            int[] rows = _table.getSelectedRows();
            int[] cols = _table.getSelectedColumns();
            if (rows.length == 1) {
                if (cols.length == 1) {
                    return new IntTuple(rows[0], cols[0]);
                } else if (cols.length == _columns.length) {
                    return new IntTuple(rows[0], -1);
                }
            } else if (cols.length == 1 && rows.length == getLength()) {
                return new IntTuple(-1, cols[0]);
            }
            return null;
        }

        /**
         * Sets the selection in using the convention of {@link #getSelection}.
         */
        protected void setSelection (int row, int column)
        {
            if (row == -1 && column == -1) {
                _table.clearSelection();
                return;
            }
            if (row == -1) {
                _table.setRowSelectionInterval(0, getLength() - 1);
            } else {
                _table.setRowSelectionInterval(row, row);
            }
            if (!is2DArray()) {
                return;
            }
            if (column == -1) {
                _table.setColumnSelectionInterval(0, _columns.length - 1);
            } else {
                _table.setColumnSelectionInterval(column, column);
            }
        }

        /**
         * Fires a {@link TableModelEvent}.
         */
        protected void fireTableChanged (int firstRow, int lastRow, int column, int type)
        {
            Object[] listeners = listenerList.getListenerList();
            TableModelEvent event = null;
            for (int ii = listeners.length - 2; ii >= 0; ii -= 2) {
                if (listeners[ii] == TableModelListener.class) {
                    if (event == null) {
                        event = new TableModelEvent(this, firstRow, lastRow, column, type);
                    }
                    ((TableModelListener)listeners[ii + 1]).tableChanged(event);
                }
            }
        }

        /**
         * Represents a column in the table.
         */
        protected abstract class Column
        {
            /**
             * Returns the name of this column.
             */
            public abstract String getName ();

            /**
             * Returns the class of this column.
             */
            public abstract Class getColumnClass ();

            /**
             * Returns the value of this column at the specified row.
             */
            public abstract Object getColumnValue (int row);

            /**
             * Sets the value at the specified row.
             */
            public abstract void setColumnValue (int row, Object value);

            /**
             * Returns the preferred width of the column.
             */
            public abstract int getWidth ();
        }

        /** The column info. */
        protected Column[] _columns;

        /** The table containing the array data. */
        protected JTable _table;

        /** The add column button. */
        protected JButton _addColumn;

        /** The delete button. */
        protected JButton _delete;

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
        registerEditorClass("resource", ResourceEditor.class);
        registerEditorClass("table", TableArrayListEditor.class);

        registerEditorClass(Boolean.class, BooleanEditor.class);
        registerEditorClass(Boolean.TYPE, BooleanEditor.class);
        registerEditorClass(Byte.class, NumberEditor.class);
        registerEditorClass(Byte.TYPE, NumberEditor.class);
        registerEditorClass(Color4f.class, Color4fEditor.class);
        registerEditorClass(ConfigReference.class, ConfigReferenceEditor.class);
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
