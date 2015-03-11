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

package com.threerings.editor.swing;

import java.lang.reflect.Array;

import java.awt.Component;

import java.io.File;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.google.common.base.Objects;

import com.samskivert.util.ArrayUtil;

import com.threerings.config.ConfigReference;
import com.threerings.config.Reference;
import com.threerings.util.ReflectionUtil;

import com.threerings.math.Quaternion;
import com.threerings.math.Transform2D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;

import com.threerings.opengl.renderer.Color4f;

import com.threerings.editor.EditorMessageBundle;
import com.threerings.editor.Property;
import com.threerings.editor.util.EditorContext;
import com.threerings.editor.util.PropertyUtil;

import com.threerings.editor.swing.editors.BooleanEditor;
import com.threerings.editor.swing.editors.ChoiceEditor;
import com.threerings.editor.swing.editors.Color4fEditor;
import com.threerings.editor.swing.editors.ColorizationEditor;
import com.threerings.editor.swing.editors.ConfigEditor;
import com.threerings.editor.swing.editors.ConfigReferenceEditor;
import com.threerings.editor.swing.editors.ConfigReferencePanelArrayListEditor;
import com.threerings.editor.swing.editors.ConfigTypeEditor;
import com.threerings.editor.swing.editors.DateTimeEditor;
import com.threerings.editor.swing.editors.DerivedConfigReferenceEditor;
import com.threerings.editor.swing.editors.EnumEditor;
import com.threerings.editor.swing.editors.EnumPanelArrayListEditor;
import com.threerings.editor.swing.editors.FileEditor;
import com.threerings.editor.swing.editors.GetPathEditor;
import com.threerings.editor.swing.editors.MaskEditor;
import com.threerings.editor.swing.editors.NumberEditor;
import com.threerings.editor.swing.editors.ObjectEditor;
import com.threerings.editor.swing.editors.ObjectPanelArrayListEditor;
import com.threerings.editor.swing.editors.PathTableArrayListEditor;
import com.threerings.editor.swing.editors.QuaternionEditor;
import com.threerings.editor.swing.editors.ResourceEditor;
import com.threerings.editor.swing.editors.StringEditor;
import com.threerings.editor.swing.editors.TableArrayListEditor;
import com.threerings.editor.swing.editors.Transform2DEditor;
import com.threerings.editor.swing.editors.Transform3DEditor;
import com.threerings.editor.swing.editors.Vector2fEditor;
import com.threerings.editor.swing.editors.Vector3fEditor;

import static com.threerings.editor.Log.log;

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
        String name = property.getAnnotation().editor();
        Class<?> type = property.getType();
        Class<? extends PropertyEditor> clazz = null;
        // look first by name, if a custom editor is specified
        if (name.length() > 0 && (clazz = _classesByName.get(name)) == null) {
            log.warning("Missing custom editor class [name=" + name + "].");

        // if a String, and the @Reference annotation is present, it's a bare config reference.
        } else if (type == String.class && property.isAnnotationPresent(Reference.class)) {
            clazz = ConfigEditor.class;
        }
        PropertyEditor editor;
        // then by type
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
            try {
                editor = getArrayListEditorType(property).newInstance();
            } catch (Exception e) {
                log.warning("Failed to create array list editor.", e);
                editor = new ObjectEditor();
            }
        } else {
            editor = new ObjectEditor();
        }
        editor.init(ctx, property, ancestors);
        return editor;
    }

    /**
     * Returns the type of editor class to use to edit the specified array/list property.
     */
    public static Class<? extends PropertyEditor> getArrayListEditorType (Property property)
    {
        // use the table editor when the array components are
        // primitives (or similar, or arrays thereof)
        Class<?> ctype = property.getComponentType();
        if (isTableCellType(ctype) ||
                (ctype.isArray() && isTableCellType(ctype.getComponentType()))) {
            return TableArrayListEditor.class;
        } else if (ctype.isEnum() || (ctype.isArray() && ctype.getComponentType().isEnum())) {
            return EnumPanelArrayListEditor.class;
        } else if (ConfigReference.class == ctype) {
            // Note: There's actually nothing ConfigReference-specific in
            // ConfigReferencePanelArrayListEditor, such that in theory it could take the place of
            // ObjectPanelArrayListEditor.
            // But it only seems to work on ConfigReferences, for which is was designed.
            // In the future, someone may want to dig around in there.
            return ConfigReferencePanelArrayListEditor.class;
        } else {
            return ObjectPanelArrayListEditor.class;
        }
    }

    /**
     * Adds a custom editor class for properties of the given type.
     */
    public static void registerEditorClass (Class<?> type, Class<? extends PropertyEditor> clazz)
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
        _msgmgr = ctx.getMessageManager();
        _msgs = _msgmgr.getBundle(EditorMessageBundle.DEFAULT);
        _property = property;
        _lineage = (ancestors == null) ?
            new Property[] { _property } : ArrayUtil.append(ancestors, _property);

        setBackground(getBackgroundColor(_lineage));

        // give subclasses a chance to initialize themselves
        didInit();
    }

    /**
     * Returns a reference to the edited property.
     */
    public Property getProperty ()
    {
        return _property;
    }

    /**
     * Sets the object being edited.
     */
    public void setObject (Object object)
    {
        _object = object;

        // give subclasses a chance to update
        update();

        fireStateChanged();
    }

    /**
     * Returns a reference to the object being edited.
     */
    public Object getObject ()
    {
        return _object;
    }

    /**
     * Updates the state of the editor from the object.
     */
    public abstract void update ();

    @Override
    public final String toString ()
    {
        return toStringHelper().toString();
    }

    /**
     * Overrideable helper for toString().
     */
    protected Objects.ToStringHelper toStringHelper ()
    {
        return Objects.toStringHelper(this)
            .add("property", _property.getName());
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
    }

    /**
     * Returns the name of the property, translating it if a translation exists.
     */
    protected String getPropertyLabel ()
    {
        return getLabel(_property);
    }

    /**
     * Add a Label for the units, if applicable.
     */
    protected void addUnits (JPanel panel)
    {
        String units = getUnits();
        if (units.length() > 0) {
            panel.add(new JLabel(_msgmgr.getBundle(_property.getMessageBundle()).xlate(units)));
        }
    }

    /**
     * Find the topmost BaseEditorPanel in our component ancestry.
     */
    protected BaseEditorPanel findBaseEditor ()
    {
        BaseEditorPanel bep = null;
        for (Component c = this; c != null; c = c.getParent()) {
            if (c instanceof BaseEditorPanel) {
                bep = (BaseEditorPanel)c;
            }
        }
        return bep;
    }

    /**
     * Find the root object that's being edited.
     */
    protected Object getRootObject ()
    {
        BaseEditorPanel bep = findBaseEditor();
        return (bep == null) ? null : bep.getObject();
    }

    /**
     * Returns the base color for this property.
     */
    protected int getPropertyColor ()
    {
        return getPropertyColor(_lineage);
    }

    /**
     * Gets the mode string by walking up the lineage.
     */
    protected String getMode ()
    {
        return PropertyUtil.getMode(_lineage);
    }

    /**
     * Gets the units string by walking up the lineage.
     */
    protected String getUnits ()
    {
        return PropertyUtil.getUnits(_lineage);
    }

    /**
     * Gets the minimum value by walking up the lineage.
     */
    protected double getMinimum ()
    {
        return PropertyUtil.getMinimum(_lineage);
    }

    /**
     * Gets the maximum value by walking up the lineage.
     */
    protected double getMaximum ()
    {
        return PropertyUtil.getMaximum(_lineage);
    }

    /**
     * Gets the step by walking up the lineage.
     */
    protected double getStep ()
    {
        return PropertyUtil.getStep(_lineage);
    }

    /**
     * Gets the scale by walking up the lineage.
     */
    protected double getScale ()
    {
        return PropertyUtil.getScale(_lineage);
    }

    /**
     * Gets the minimum size by walking up the lineage.
     */
    protected int getMinSize ()
    {
        return PropertyUtil.getMinSize(_lineage);
    }

    /**
     * Gets the maximum size by walking up the lineage.
     */
    protected int getMaxSize ()
    {
        return PropertyUtil.getMaxSize(_lineage);
    }

    /**
     * Checks if fixed size by walking up the lineage.
     */
    protected boolean isFixedSize ()
    {
        return PropertyUtil.isFixedSize(_lineage);
    }

    /**
     * Checks whether the supplied type can be edited in the cell of a table.
     */
    protected static boolean isTableCellType (Class<?> type)
    {
        return type.isPrimitive() || Number.class.isAssignableFrom(type) ||
            type == Boolean.class || type == Character.class ||
            type == String.class;
    }

    /**
     * Returns a default instance for the supplied type, either by instantiating it with its no-arg
     * constructor or by obtaining some type-specific default;
     */
    protected static Object getDefaultInstance (Class<?> type, Object outer)
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
        } else if (type == ConfigReference.class) {
            return null; // otherwise, ReflectionUtil will make one with a null name and null args
        } else if (type.isEnum()) {
            return type.getEnumConstants()[0];
        } else if (type.isArray()) {
            return Array.newInstance(type.getComponentType(), 0);
        } else {
            return ReflectionUtil.newInstance(type, outer);
        }
    }

    /** Provides access to common services. */
    protected EditorContext _ctx;

    /** The property being edited. */
    protected Property _property;

    /** The ancestors of the property (if any), followed by the property itself. */
    protected Property[] _lineage;

    /** The object being edited. */
    protected Object _object;

    /** Maps names to editor classes. */
    protected static HashMap<String, Class<? extends PropertyEditor>> _classesByName =
        new HashMap<String, Class<? extends PropertyEditor>>();

    /** Maps types to editor classes. */
    protected static HashMap<Class<?>, Class<? extends PropertyEditor>> _classesByType =
        new HashMap<Class<?>, Class<? extends PropertyEditor>>();
    static {
        registerEditorClass("choice", ChoiceEditor.class);
        registerEditorClass("colorization", ColorizationEditor.class);
        registerEditorClass("config", ConfigEditor.class);
        registerEditorClass("resource", ResourceEditor.class);
        registerEditorClass("table", TableArrayListEditor.class);
        registerEditorClass("mask", MaskEditor.class);
        registerEditorClass("paths", PathTableArrayListEditor.class);
        registerEditorClass("getPath", GetPathEditor.class);
        registerEditorClass("datetime", DateTimeEditor.class);
        registerEditorClass("date", DateTimeEditor.DateOnlyEditor.class);
        registerEditorClass("time", DateTimeEditor.TimeOnlyEditor.class);
        registerEditorClass("configType", ConfigTypeEditor.class);
        registerEditorClass("derivedRef", DerivedConfigReferenceEditor.class);

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
        registerEditorClass(Date.class, DateTimeEditor.class);
        registerEditorClass(Transform2D.class, Transform2DEditor.class);
        registerEditorClass(Transform3D.class, Transform3DEditor.class);
        registerEditorClass(Vector2f.class, Vector2fEditor.class);
        registerEditorClass(Vector3f.class, Vector3fEditor.class);
    }
}
