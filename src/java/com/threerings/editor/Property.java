//
// $Id$

package com.threerings.editor;

import java.lang.annotation.Annotation;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import java.util.Collection;
import java.util.HashMap;

import com.samskivert.util.ClassUtil;
import com.samskivert.util.ListUtil;

import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import static com.threerings.editor.Log.*;

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
     * Returns the generic property type.
     */
    public abstract Type getGenericType ();

    /**
     * Returns the component type of this (array or collection) type.
     */
    public Class getComponentType ()
    {
        Class type = getType();
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
        Class type = getType();
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
    public Class getArgumentType (Class clazz)
    {
        Class[] types = getArgumentTypes(clazz);
        return (types == null || types.length == 0) ? null : types[0];
    }

    /**
     * Given a generic class or interface, determines the actual type arguments provided to the
     * given type (for example, passing {@link Collection} can be used to determine the type of
     * collection).
     *
     * @return the arguments of the generic class or interface, or <code>null</code> if not found.
     */
    public Class[] getArgumentTypes (Class clazz)
    {
        if (_argumentTypes == null) {
            _argumentTypes = new HashMap<Class, Class[]>();
        }
        Class[] classes = _argumentTypes.get(clazz);
        if (classes == null) {
            Type[] types = getGenericArgumentTypes(clazz);
            if (types != null) {
                _argumentTypes.put(clazz, classes = new Class[types.length]);
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
    public Type getGenericArgumentType (Class clazz)
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
    public Type[] getGenericArgumentTypes (Class clazz)
    {
        if (_genericArgumentTypes == null) {
            _genericArgumentTypes = new HashMap<Class, Type[]>();
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
        Class type = getType();
        if (type.isPrimitive()) {
            if (value == null) {
                return false;
            }
            type = ClassUtil.objectEquivalentOf(type);
        }
        if (!(value == null || type.isInstance(value))) {
            return false;
        }
        Editable annotation = getAnnotation();
        if (value == null && !annotation.nullable()) {
            return false;
        }
        // TODO: check other constraints
        return true;
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

    @Override // documentation inherited
    public String toString ()
    {
        return _name;
    }

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

    /**
     * Given a generic class or interface, determines the actual type arguments provided to the
     * given type (for example, passing {@link Collection} can be used to determine the type of
     * collection).
     *
     * @return the arguments of the generic class or interface, or <code>null</code> if not found.
     */
    protected static Type[] getTypeArguments (Type type, Class clazz)
    {
        if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType)type;
            return getTypeArguments(
                (Class)ptype.getRawType(), ptype.getActualTypeArguments(), clazz);

        } else if (type instanceof Class) {
            return getTypeArguments((Class)type, null, clazz);
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
    protected static Type[] getTypeArguments (Class type, Type[] args, Class clazz)
    {
        if (type == clazz) {
            return args;
        }
        TypeVariable[] params = type.getTypeParameters();
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
        Type type, TypeVariable[] params, Type[] values, Class clazz)
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
            return getTypeArguments((Class)ptype.getRawType(), args, clazz);

        } else if (type instanceof Class) {
            return getTypeArguments((Class)type, null, clazz);
        }
        return null;
    }

    /**
     * Returns the underlying class of the supplied type.
     */
    protected Class getTypeClass (Type type)
    {
        if (type instanceof Class) {
            return (Class)type;

        } else if (type instanceof ParameterizedType) {
            return (Class)((ParameterizedType)type).getRawType();

        } else if (type instanceof GenericArrayType) {
            // TODO: is there a better way to do this?
            Class cclass = getTypeClass(((GenericArrayType)type).getGenericComponentType());
            return Array.newInstance(cclass, 0).getClass();
        }
        return null;
    }

    /** The property name. */
    protected String _name;

    /** Cached argument types. */
    @DeepOmit
    protected HashMap<Class, Class[]> _argumentTypes;

    /** Cached generic argument types. */
    @DeepOmit
    protected HashMap<Class, Type[]> _genericArgumentTypes;
}
