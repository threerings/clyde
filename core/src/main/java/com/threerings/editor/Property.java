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

package com.threerings.editor;

import java.lang.annotation.Annotation;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ClassUtil;
import com.samskivert.util.Config;
import com.samskivert.util.ListUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import static com.threerings.editor.Log.log;

/**
 * Provides access to an editable property of an object.
 */
public abstract class Property extends DeepObject
{
    /**
     * Returns the name of the property.
     */
    public String getName ()
    {
        return _name;
    }

    /**
     * Returns whether or not we should attempt to translate the name.
     */
    public boolean shouldTranslateName ()
    {
        return true;
    }

    /**
     * Returns the name of the color lookup for the property.
     */
    public String getColorName ()
    {
        return _name;
    }

    /**
     * Returns an array containing the available subtypes of this type.
     */
    public Class<?>[] getSubtypes ()
    {
        return getSubtypes(getType());
    }

    /**
     * Returns an array containing the available subtypes for components of this (array or
     * collection) type.
     */
    public Class<?>[] getComponentSubtypes ()
    {
        return getSubtypes(getComponentType());
    }

    /**
     * Returns the type label for subtypes of this type.
     */
    public String getTypeLabel ()
    {
        return getTypeLabel(getType());
    }

    /**
     * Returns the type label for subtypes of this (array or collection) type's components.
     */
    public String getComponentTypeLabel ()
    {
        return getTypeLabel(getComponentType());
    }

    /**
     * Returns the name of the message bundle to use when translating the property name and other
     * bits.
     */
    public String getMessageBundle ()
    {
        return Introspector.getMessageBundle(getMember().getDeclaringClass());
    }

    /**
     * Returns the underlying member of the property (the field or method that provides the
     * annotations).
     */
    public abstract Member getMember ();

    /**
     * Returns a reference to the member object (the object to whose member {@link #getMember}
     * refers, given the object one would pass to {@link #get} or {@link #set}).
     */
    public Object getMemberObject (Object object)
    {
        return object;
    }

    /**
     * Returns the property type.
     */
    public abstract Class<?> getType ();

    /**
     * Returns the generic property type.
     */
    public abstract Type getGenericType ();

    /**
     * Returns the component type of this (array or collection) type.
     */
    public Class<?> getComponentType ()
    {
        Class<?> type = getType();
        if (type.isArray()) {
            return type.getComponentType();

        } else if (Collection.class.isAssignableFrom(type)) {
            return getArgumentType(Collection.class);
        }
        return null;
    }

    /**
     * Returns the generic component type of this (array or collection) type.
     */
    public Type getGenericComponentType ()
    {
        Class<?> type = getType();
        Type gtype = getGenericType();
        if (gtype instanceof GenericArrayType) {
            return ((GenericArrayType)gtype).getGenericComponentType();

        } else if (type.isArray()) {
            return type.getComponentType();

        } else if (Collection.class.isAssignableFrom(type)) {
            return getGenericArgumentType(Collection.class);
        }
        return null;
    }

    /**
     * Given a generic class or interface, determines the first actual type argument provided to
     * the given type (for example, passing {@link Collection} can be used to determine the type of
     * collection).
     *
     * @return the first argument of the generic class or interface, or <code>null</code> if not
     * found.
     */
    public Class<?> getArgumentType (Class<?> clazz)
    {
        Class<?>[] types = getArgumentTypes(clazz);
        return (types == null || types.length == 0) ? null : types[0];
    }

    /**
     * Given a generic class or interface, determines the actual type arguments provided to the
     * given type (for example, passing {@link Collection} can be used to determine the type of
     * collection).
     *
     * @return the arguments of the generic class or interface, or <code>null</code> if not found.
     */
    public Class<?>[] getArgumentTypes (Class<?> clazz)
    {
        if (_argumentTypes == null) {
            _argumentTypes = new HashMap<Class<?>, Class<?>[]>(0);
        }
        Class<?>[] classes = _argumentTypes.get(clazz);
        if (classes == null) {
            Type[] types = getGenericArgumentTypes(clazz);
            if (types != null) {
                _argumentTypes.put(clazz, classes = new Class<?>[types.length]);
                for (int ii = 0; ii < classes.length; ii++) {
                    classes[ii] = getTypeClass(types[ii]);
                }
            }
        }
        return classes;
    }

    /**
     * Given a generic class or interface, determines the first actual type argument provided to
     * the given type (for example, passing {@link Collection} can be used to determine the type of
     * collection).
     *
     * @return the first argument of the generic class or interface, or <code>null</code> if not
     * found.
     */
    public Type getGenericArgumentType (Class<?> clazz)
    {
        Type[] types = getGenericArgumentTypes(clazz);
        return (types == null || types.length == 0) ? null : types[0];
    }

    /**
     * Given a generic class or interface, determines the actual type arguments provided to the
     * given type (for example, passing {@link Collection} can be used to determine the type of
     * collection).
     *
     * @return the arguments of the generic class or interface, or <code>null</code> if not found.
     */
    public Type[] getGenericArgumentTypes (Class<?> clazz)
    {
        if (_genericArgumentTypes == null) {
            _genericArgumentTypes = new HashMap<Class<?>, Type[]>(0);
        }
        Type[] types = _genericArgumentTypes.get(clazz);
        if (types == null) {
            _genericArgumentTypes.put(clazz, types = getTypeArguments(getGenericType(), clazz));
        }
        return types;
    }

    /**
     * Determines whether the supplied value is legal for this property.
     */
    public boolean isLegalValue (Object value)
    {
        return isLegalValue(getType(), value);
    }

    /**
     * Determines whether the supplied value is legal for this (array or list) property's
     * components.
     */
    public boolean isLegalComponentValue (Object value)
    {
        return isLegalValue(getComponentType(), value);
    }

    /**
     * Returns the mode string.  Usually this comes from the property annotation, but derived
     * classes may inherit values from elsewhere.
     */
    public String getMode ()
    {
        return getAnnotation().mode();
    }

    /**
     * Returns the units string.
     */
    public String getUnits ()
    {
        return getAnnotation().units();
    }

    /**
     * Get the width specified in the annotation, or the default if none set.
     */
    public int getWidth (int defaultWidth)
    {
        int width = getAnnotation().width();
        return (width <= 0) ? defaultWidth : width;
    }

    /**
     * Get the height specified in the annotation, or the default if none set.
     */
    public int getHeight (int defaultHeight)
    {
        int height = getAnnotation().height();
        return (height <= 0) ? defaultHeight : height;
    }

    /**
     * Is the value nullable?
     */
    public boolean nullable ()
    {
        return getAnnotation().nullable();
    }

    /**
     * Returns the minimum value.
     */
    public double getMinimum ()
    {
        return getAnnotation().min();
    }

    /**
     * Returns the maximum value.
     */
    public double getMaximum ()
    {
        return getAnnotation().max();
    }

    /**
     * Returns the step value.
     */
    public double getStep ()
    {
        return getAnnotation().step();
    }

    /**
     * Returns the scale value.
     */
    public double getScale ()
    {
        return getAnnotation().scale();
    }

    /**
     * Returns the minimum size.
     */
    public int getMinSize ()
    {
        return getAnnotation().minsize();
    }

    /**
     * Returns the maximum size;
     */
    public int getMaxSize ()
    {
        return getAnnotation().maxsize();
    }

    /**
     * Returns if we're a fixed sized array/list.
     */
    public boolean isFixedSize ()
    {
        return getAnnotation().fixedsize();
    }

    /**
     * Returns a reference to the {@link Editable} annotation, which contains simple constraints.
     */
    public Editable getAnnotation ()
    {
        return getAnnotation(Editable.class);
    }

    /**
     * Determines whether the property has an annotation of the specified class.
     */
    public boolean isAnnotationPresent (Class<? extends Annotation> clazz)
    {
        return getAnnotation(clazz) != null;
    }

    /**
     * Returns a reference to the annotation of the specified class, if it exists.
     */
    public <T extends Annotation> T getAnnotation (Class<T> clazz)
    {
        return ((AnnotatedElement)getMember()).getAnnotation(clazz);
    }

    /**
     * Retrieves the value of the property.
     */
    public boolean getBoolean (Object object)
    {
        return (Boolean)get(object);
    }

    /**
     * Retrieves the value of the property.
     */
    public byte getByte (Object object)
    {
        return (Byte)get(object);
    }

    /**
     * Retrieves the value of the property.
     */
    public char getChar (Object object)
    {
        return (Character)get(object);
    }

    /**
     * Retrieves the value of the property.
     */
    public double getDouble (Object object)
    {
        return (Double)get(object);
    }

    /**
     * Retrieves the value of the property.
     */
    public float getFloat (Object object)
    {
        return (Float)get(object);
    }

    /**
     * Retrieves the value of the property.
     */
    public int getInt (Object object)
    {
        return (Integer)get(object);
    }

    /**
     * Retrieves the value of the property.
     */
    public long getLong (Object object)
    {
        return (Long)get(object);
    }

    /**
     * Retrieves the value of the property.
     */
    public short getShort (Object object)
    {
        return (Short)get(object);
    }

    /**
     * Retrieves the value of the property.
     */
    public abstract Object get (Object object);

    /**
     * Sets the value of the property.
     */
    public void setBoolean (Object object, boolean value)
    {
        set(object, value);
    }

    /**
     * Sets the value of the property.
     */
    public void setByte (Object object, byte value)
    {
        set(object, value);
    }

    /**
     * Sets the value of the property.
     */
    public void setChar (Object object, char value)
    {
        set(object, value);
    }

    /**
     * Sets the value of the property.
     */
    public void setDouble (Object object, double value)
    {
        set(object, value);
    }

    /**
     * Sets the value of the property.
     */
    public void setFloat (Object object, float value)
    {
        set(object, value);
    }

    /**
     * Sets the value of the property.
     */
    public void setInt (Object object, int value)
    {
        set(object, value);
    }

    /**
     * Sets the value of the property.
     */
    public void setLong (Object object, long value)
    {
        set(object, value);
    }

    /**
     * Sets the value of the property.
     */
    public void setShort (Object object, short value)
    {
        set(object, value);
    }

    /**
     * Sets the value of the property.
     */
    public abstract void set (Object object, Object value);

    @Override
    public String toString ()
    {
        return _name;
    }

    /**
     * Is this property compatible with the other property, based only on type and
     * annotations?
     *
     * Two properties are compatible if they have the same generic Type and the exact same
     * set of annotations. The names of the properties, their lineage, and the kinds of Member
     * that define them do not matter.
     */
    public boolean isCompatible (Property other)
    {
        // make sure they have the same generic type
        if (!getGenericType().equals(other.getGenericType())) {
            return false;
        }

        // Do a Setwise comparison of all their annotations and require an exact match.
        // Note: in theory we could be less anal, but we don't know which of the annotation
        // values are "important" for the property. For example, the "min" and "max"
        // values of the Editable annotation may define a fixed array size for a field.
        Set<Annotation> annos = Sets.newHashSet(
                ((AnnotatedElement)this.getMember()).getAnnotations());
        Set<Annotation> otherAnnos = Sets.newHashSet(
                ((AnnotatedElement)other.getMember()).getAnnotations());
        return annos.equals(otherAnnos);
    }

    /**
     * Returns the label for subtypes of the specified type.
     */
    protected String getTypeLabel (Class<?> type)
    {
        EditorTypes annotation = getEditorTypes(type);
        return (annotation == null) ? "type" : annotation.label();
    }

    /**
     * Returns an array containing the available subtypes of the specified type, first looking to
     * subtypes listed in the annotation, then attempting to find a method using reflection.
     */
    protected Class<?>[] getSubtypes (Class<?> type)
    {
        ArrayList<Class<?>> types = new ArrayList<Class<?>>();

        // start with the null class, if allowed
        boolean nullable = nullable();
        if (nullable) {
            types.add(null);
        }

        // look for a subtype annotation and add its types
        EditorTypes ownAnnotation = getAnnotation(EditorTypes.class);
        EditorTypes annotation = (ownAnnotation == null) ?
            type.getAnnotation(EditorTypes.class) : ownAnnotation;
        if (annotation != null) {
            addSubtypes(annotation, types);
        }

        // get the config key and add the config types
        String key = (annotation == null) ? type.getName() : annotation.key();
        if (StringUtil.isBlank(key)) {
            if (annotation == ownAnnotation) {
                Member member = getMember();
                key = member.getDeclaringClass().getName() + "." + member.getName();
            } else {
                key = type.getName();
            }
        }
        Class<?>[] ctypes = _configTypes.get(key);
        if (ctypes != null) {
            Collections.addAll(types, ctypes);
        }

        // if we don't have at least one non-null class, add the type itself
        if (types.size() == (nullable ? 1 : 0)) {
            types.add(type);
        }

        // convert to array, return
        return types.toArray(new Class<?>[types.size()]);
    }

    /**
     * Returns the editor types annotation on the property, if it exists, or on the specified
     * type, if that exists, or <code>null</code> if neither one exists.
     */
    protected EditorTypes getEditorTypes (Class<?> type)
    {
        EditorTypes annotation = getAnnotation(EditorTypes.class);
        return (annotation == null) ? type.getAnnotation(EditorTypes.class) : annotation;
    }

    /**
     * Given a generic class or interface, determines the actual type arguments provided to the
     * given type (for example, passing {@link Collection} can be used to determine the type of
     * collection).
     *
     * @return the arguments of the generic class or interface, or <code>null</code> if not found.
     */
    protected static Type[] getTypeArguments (Type type, Class<?> clazz)
    {
        if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType)type;
            return getTypeArguments(
                (Class<?>)ptype.getRawType(), ptype.getActualTypeArguments(), clazz);

        } else if (type instanceof Class<?>) {
            return getTypeArguments((Class<?>)type, null, clazz);
        }
        return null;
    }

    /**
     * Returns the arguments of the provided generic class or interface.
     *
     * @param type the class to search.
     * @param args the class arguments.
     * @param clazz the generic class or interface of interest.
     * @return the arguments of the generic class or interface, or <code>null</code> if not found.
     */
    protected static Type[] getTypeArguments (Class<?> type, Type[] args, Class<?> clazz)
    {
        if (type == clazz) {
            return args;
        }
        TypeVariable<?>[] params = type.getTypeParameters();
        for (Type iface : type.getGenericInterfaces()) {
            Type[] result = getTypeArguments(iface, params, args, clazz);
            if (result != null) {
                return result;
            }
        }
        return getTypeArguments(type.getGenericSuperclass(), params, args, clazz);
    }

    /**
     * Returns the arguments of the provided generic class or interface.
     *
     * @param type the type to search.
     * @param params the parameters to replace if the type is parameterized.
     * @param values the values with which to replace each parameter.
     * @param clazz the generic class of interface of interest.
     * @return the arguments of the generic class or interface, or <code>null</code> if
     * not found.
     */
    protected static Type[] getTypeArguments (
        Type type, TypeVariable<?>[] params, Type[] values, Class<?> clazz)
    {
        if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType)type;
            Type[] args = ptype.getActualTypeArguments();
            for (int ii = 0; ii < args.length; ii++) {
                Type arg = args[ii];
                if (arg instanceof TypeVariable) {
                    int idx = ListUtil.indexOf(params, arg);
                    if (idx != -1) {
                        args[ii] = values[idx];
                    }
                }
            }
            return getTypeArguments((Class<?>)ptype.getRawType(), args, clazz);

        } else if (type instanceof Class<?>) {
            return getTypeArguments((Class<?>)type, null, clazz);
        }
        return null;
    }

    /**
     * Returns the underlying class of the supplied type.
     */
    protected Class<?> getTypeClass (Type type)
    {
        if (type instanceof Class<?>) {
            return (Class<?>)type;

        } else if (type instanceof ParameterizedType) {
            return (Class<?>)((ParameterizedType)type).getRawType();

        } else if (type instanceof GenericArrayType) {
            // TODO: is there a better way to do this?
            Class<?> cclass = getTypeClass(((GenericArrayType)type).getGenericComponentType());
            return Array.newInstance(cclass, 0).getClass();
        }
        return null;
    }

    /**
     * Determines whether the supplied value is legal for this property.
     */
    protected boolean isLegalValue (Class<?> type, Object value)
    {
        if (type.isPrimitive()) {
            if (value == null) {
                return false;
            }
            type = ClassUtil.objectEquivalentOf(type);
        }
        return (value == null)
            ? nullable()
            : type.isInstance(value);
    }

    /**
     * Adds the subtypes listed in the provided annotation to the supplied list.
     */
    protected static void addSubtypes (EditorTypes annotation, Collection<Class<?>> types)
    {
        for (Class<?> sclazz : annotation.value()) {
            // handle the case where the annotation includes the annotated class
            if (sclazz.getAnnotation(EditorTypes.class) == annotation) {
                types.add(sclazz);
            } else {
                addSubtypes(sclazz, types);
            }
        }
    }

    /**
     * Adds the subtypes of the specified class to the provided list.  If the class has an
     * {@link EditorTypes} annotation, the classes therein will be added; otherwise, the
     * class itself will be added.
     */
    protected static void addSubtypes (Class<?> clazz, Collection<Class<?>> types)
    {
        EditorTypes annotation = clazz.getAnnotation(EditorTypes.class);
        if (annotation == null) {
            types.add(clazz);
        } else {
            addSubtypes(annotation, types);
        }
    }

    /** The property name. */
    protected String _name;

    /** Cached argument types. */
    @DeepOmit
    protected HashMap<Class<?>, Class<?>[]> _argumentTypes;

    /** Cached generic argument types. */
    @DeepOmit
    protected HashMap<Class<?>, Type[]> _genericArgumentTypes;

    /** Class<?> lists read from the type configuration. */
    protected static HashMap<String, Class<?>[]> _configTypes = new HashMap<String, Class<?>[]>();

    static {
        // load the types from the configuration
        Config config = new Config("/rsrc/config/editor/type");
        for (Iterator<String> it = config.keys(); it.hasNext(); ) {
            String key = it.next();
            ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
            for (String cname : config.getValue(key, ArrayUtil.EMPTY_STRING)) {
                try {
                    addSubtypes(Class.forName(cname), classes);
                } catch (ClassNotFoundException e) {
                    log.warning("Missing type config class.", "class", cname, e);
                }
            }
            _configTypes.put(key, classes.toArray(new Class<?>[classes.size()]));
        }
    }
}
