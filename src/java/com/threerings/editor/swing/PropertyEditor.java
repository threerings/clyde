//
// $Id$

package com.threerings.editor.swing;

import java.awt.Point;

import java.lang.reflect.Array;

import java.io.File;

import java.util.HashMap;
import java.util.List;

import com.samskivert.util.ArrayUtil;

import com.threerings.config.ConfigReference;

import com.threerings.math.Quaternion;
import com.threerings.math.Transform;
import com.threerings.math.Vector3f;

import com.threerings.opengl.renderer.Color4f;

import com.threerings.editor.Editable;
import com.threerings.editor.Property;
import com.threerings.editor.util.EditorContext;

import com.threerings.editor.swing.editors.BooleanEditor;
import com.threerings.editor.swing.editors.Color4fEditor;
import com.threerings.editor.swing.editors.ConfigReferenceEditor;
import com.threerings.editor.swing.editors.EnumEditor;
import com.threerings.editor.swing.editors.FileEditor;
import com.threerings.editor.swing.editors.NumberEditor;
import com.threerings.editor.swing.editors.ObjectEditor;
import com.threerings.editor.swing.editors.PanelArrayListEditor;
import com.threerings.editor.swing.editors.QuaternionEditor;
import com.threerings.editor.swing.editors.ResourceEditor;
import com.threerings.editor.swing.editors.StringEditor;
import com.threerings.editor.swing.editors.TableArrayListEditor;
import com.threerings.editor.swing.editors.TransformEditor;
import com.threerings.editor.swing.editors.Vector3fEditor;

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
     * Returns the property path component corresponding to the specified point.
     */
    public String getPathComponent (Point pt)
    {
        return _property.getName();
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

    /** The property being edited. */
    protected Property _property;

    /** The ancestors of the property (if any), followed by the property itself. */
    protected Property[] _lineage;

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
