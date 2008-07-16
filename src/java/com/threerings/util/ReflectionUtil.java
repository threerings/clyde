//
// $Id$

package com.threerings.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.HashMap;

import static com.threerings.ClydeLog.*;

/**
 * Some general reflection utility methods.
 */
public class ReflectionUtil
{
    /**
     * Creates a new instance of the named class.
     *
     * @param classname the name of the class to instantiate.
     */
    public static Object newInstance (String classname)
    {
        return newInstance(classname, null);
    }

    /**
     * Creates a new instance of the named inner class.
     *
     * @param clazz the name of the class to instantiate.
     * @param outer an instance of the enclosing class.
     */
    public static Object newInstance (String classname, Object outer)
    {
        try {
            return newInstance(Class.forName(classname), outer);
        } catch (Exception e) {
            log.warning("Failed to get class by name [class=" + classname + "].", e);
            return null;
        }
    }

    /**
     * Creates a new instance of the specified class.
     *
     * @param clazz the class to instantiate.
     */
    public static Object newInstance (Class clazz)
    {
        return newInstance(clazz, null);
    }

    /**
     * Creates a new instance of the specified (possibly inner) class.
     *
     * @param clazz the class to instantiate.
     * @param outer for inner classes, a reference to the enclosing instance (otherwise
     * <code>null</code>).
     * @return the newly created object, or <code>null</code> if there was some error
     * (in which case a message will be logged).
     */
    public static Object newInstance (Class clazz, Object outer)
    {
        if (!isInner(clazz)) {
            outer = null;
        }
        Constructor ctor = _ctors.get(clazz);
        if (ctor == null) {
            Class eclazz = clazz.getEnclosingClass();
            for (Constructor octor : clazz.getDeclaredConstructors()) {
                Class[] ptypes = octor.getParameterTypes();
                if (outer == null ? (ptypes.length == 0) :
                        (ptypes.length == 1 && ptypes[0] == eclazz)) {
                    ctor = octor;
                    break;
                }
            }
            if (ctor == null) {
                log.warning("Class has no default constructor [class=" + clazz + "].");
                return null;
            }
            ctor.setAccessible(true);
            _ctors.put(clazz, ctor);
        }
        try {
            return (outer == null) ? ctor.newInstance() : ctor.newInstance(outer);
        } catch (Exception e) {
            log.warning("Failed to create new instance [class=" + clazz + "].", e);
            return null;
        }
    }

    /**
     * Determines whether the specified class is a non-static inner class.
     */
    public static boolean isInner (Class clazz)
    {
        return clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers());
    }

    /**
     * Returns a reference to an inner object's outer class reference, or <code>null</code> if
     * the object represents an instance of a static class.
     */
    public static Object getOuter (Object object)
    {
        Class clazz = object.getClass();
        Class oclazz = getOuterClass(clazz);
        if (oclazz == null) {
            return null;
        }
        Field field = _outers.get(clazz);
        if (field == null) {
            for (Field ofield : clazz.getDeclaredFields()) {
                if (ofield.isSynthetic() && ofield.getType() == oclazz &&
                        ofield.getName().startsWith("this")) {
                    field = ofield;
                    break;
                }
            }
            field.setAccessible(true);
            _outers.put(clazz, field);
        }
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            return null; // shouldn't happen
        }
    }

    /**
     * Returns a reference to the specified inner class's outer class, or <code>null</code>
     * if the class is not an inner class.
     */
    public static Class getOuterClass (Class clazz)
    {
        return Modifier.isStatic(clazz.getModifiers()) ? null : clazz.getEnclosingClass();
    }

    /** Maps inner classes to their outer class reference fields. */
    protected static HashMap<Class, Field> _outers = new HashMap<Class, Field>();

    /** Maps classes to their default constructors. */
    protected static HashMap<Class, Constructor> _ctors = new HashMap<Class, Constructor>();
}
