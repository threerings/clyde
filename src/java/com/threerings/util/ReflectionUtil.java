//
// $Id$

package com.threerings.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.HashMap;

import static java.util.logging.Level.*;
import static com.threerings.ClydeLog.*;

/**
 * Some general reflection utility methods.
 */
public class ReflectionUtil
{
    /**
     * Returns a reference to an inner object's outer class reference, or <code>null</code> if
     * the object represents an instance of a static class.
     */
    public static Object getOuter (Object object)
    {
        Class clazz = object.getClass();
        Class eclazz = clazz.getEnclosingClass();
        if (eclazz == null || Modifier.isStatic(clazz.getModifiers())) {
            return null;
        }
        Field field = _outers.get(clazz);
        if (field == null) {
            for (Field ofield : clazz.getDeclaredFields()) {
                if (ofield.isSynthetic() && ofield.getType() == eclazz &&
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
     * Creates a new instance of the named class.
     */
    public static Object newInstance (String classname, Object... params)
    {
        try {
            Class[] pclass = new Class[params.length];
            for (int ii = 0, nn = params.length; ii < nn; ii++) {
                pclass[ii] = params[ii].getClass();
            }
            Class clazz = Class.forName(classname);
            Constructor ctor = clazz.getConstructor(pclass);
            return ctor.newInstance(params);
        } catch (Exception e) {
            log.log(WARNING, "Failed to create new instance [class=" + classname + "].", e);
            return null;
        }
    }

    /**
     * Creates a new instance of the specified class.
     */
    public static Object newInstance (Class clazz)
    {
        return newInstance(clazz, null);
    }

    /**
     * Creates a new instance of the specified (possibly inner) class.
     *
     * @param outer for inner classes, a reference to the enclosing instance (otherwise
     * <code>null</code>).
     * @return the newly created object, or <code>null</code> if there was some error
     * (in which case a message will be logged).
     */
    public static Object newInstance (Class clazz, Object outer)
    {
        Constructor ctor = _ctors.get(clazz);
        if (ctor == null) {
            Class eclazz = clazz.getEnclosingClass();
            for (Constructor octor : clazz.getDeclaredConstructors()) {
                Class[] ptypes = octor.getParameterTypes();
                if ((outer == null && ptypes.length == 0) ||
                    (outer != null && ptypes.length == 1 && ptypes[0] == eclazz)) {
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
            log.log(WARNING, "Failed to create new instance [class=" + clazz + "].", e);
            return null;
        }
    }

    /** Maps inner classes to their outer class reference fields. */
    protected static HashMap<Class, Field> _outers = new HashMap<Class, Field>();

    /** Maps classes to their default constructors. */
    protected static HashMap<Class, Constructor> _ctors = new HashMap<Class, Constructor>();
}
