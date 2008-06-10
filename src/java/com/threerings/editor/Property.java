//
// $Id$

package com.threerings.editor;

import java.lang.annotation.Annotation;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.Collection;

import static com.threerings.editor.Log.*;

/**
 * Provides access to an editable property of an object.
 */
public abstract class Property
{
    /**
     * Returns the name of the property.
     */
    public String getName ()
    {
        return _name;
    }

    /**
     * Returns an array containing the available subtypes of this type.
     */
    public Class[] getSubtypes ()
    {
        return getSubtypes(getType());
    }

    /**
     * Returns an array containing the available subtypes for components of this (array or
     * collection) type.
     */
    public Class[] getComponentSubtypes ()
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
     * Returns the property type.
     */
    public abstract Class getType ();

    /**
     * Returns the component type of this (array or collection) type.
     */
    public Class getComponentType ()
    {
        Class type = getType();
        if (type.isArray()) {
            return type.getComponentType();
        } else if (Collection.class.isAssignableFrom(type)) {
            return getArgumentType();
        }
        return null;
    }

    /**
     * Returns the class of the first type argument, or <code>null</code> if there is no such
     * argument or it isn't a normal class.
     */
    public Class getArgumentType ()
    {
        Type gtype = getGenericType();
        if (gtype instanceof ParameterizedType) {
            Type[] args = ((ParameterizedType)gtype).getActualTypeArguments();
            if (args.length > 0 && args[0] instanceof Class) {
                return (Class)args[0];
            }
        }
        return null;
    }

    /**
     * Returns the generic property type.
     */
    public abstract Type getGenericType ();

    /**
     * Returns the generic component type of this (array or collection) type.
     */
    public Type getGenericComponentType ()
    {
        Type gtype = getGenericType();
        if (gtype instanceof GenericArrayType) {
            return ((GenericArrayType)gtype).getGenericComponentType();

        } else if (gtype instanceof Class) {
            Class clazz = (Class)gtype;
            if (clazz.isArray()) {
                return clazz.getComponentType();
            }
        }
        return null;
    }

    /**
     * Returns a reference to the {@link Editable} annotation, which contains simple constraints.
     */
    public Editable getAnnotation ()
    {
        return getAnnotation(Editable.class);
    }

    /**
     * Returns a reference to the annotation of the specified class, if it exists.
     */
    public abstract <T extends Annotation> T getAnnotation (Class<T> clazz);

    /**
     * Retrieves the value of the property.
     */
    public abstract Object get (Object object);

    /**
     * Sets the value of the property.
     */
    public abstract void set (Object object, Object value);

    /**
     * Returns the label for subtypes of the specified type.
     */
    protected String getTypeLabel (Class<?> type)
    {
        // look for a static method in the class
        try {
            Method method = type.getDeclaredMethod("getEditorTypeLabel");
            Class rtype = method.getReturnType();
            if (method.getReturnType() == String.class &&
                Modifier.isStatic(method.getModifiers())) {
                return (String)method.invoke(null);
            }
        } catch (NoSuchMethodException e) {
            // fall through
        } catch (Exception e) {
            log.warning("Failed to get editor type label [type=" + type + "].", e);
        }
        return "type";
    }

    /**
     * Returns an array containing the available subtypes of the specified type, first looking to
     * subtypes listed in the annotation, then attempting to find a method using reflection.
     */
    protected Class[] getSubtypes (Class<?> type)
    {
        // first look for subtypes specified in the annotation
        Editable annotation = getAnnotation();
        Class[] subtypes = annotation.types();
        if (subtypes.length > 0) {
            // prepend the null class if the property is nullable
            if (!annotation.nullable()) {
                return subtypes;
            }
            Class[] nsubtypes = new Class[subtypes.length + 1];
            System.arraycopy(subtypes, 0, nsubtypes, 1, subtypes.length);
            return nsubtypes;
        }
        // then look for a static method in the class
        try {
            Method method = type.getDeclaredMethod("getEditorTypes");
            Class rtype = method.getReturnType();
            if (rtype.getComponentType() == Class.class &&
                Modifier.isStatic(method.getModifiers())) {
                return (Class[])method.invoke(null);
            }
        } catch (NoSuchMethodException e) {
            // fall through
        } catch (Exception e) {
            log.warning("Failed to get editor types [type=" + type + "].", e);
        }
        // just use the class itself
        return annotation.nullable() ? new Class[] { null, type } : new Class[] { type };
    }

    /** The property name. */
    protected String _name;
}
