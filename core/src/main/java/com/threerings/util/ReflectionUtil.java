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

package com.threerings.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.HashMap;

import static com.threerings.ClydeLog.log;

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
     * @param classname the name of the class to instantiate.
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
    public static Object newInstance (Class<?> clazz)
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
    public static Object newInstance (Class<?> clazz, Object outer)
    {
        if (!isInner(clazz)) {
            outer = null;
        }
        Constructor<?> ctor = _ctors.get(clazz);
        if (ctor == null) {
            for (Constructor<?> octor : clazz.getDeclaredConstructors()) {
                Class<?>[] ptypes = octor.getParameterTypes();
                if (outer == null ? (ptypes.length == 0) :
                        (ptypes.length == 1 && ptypes[0].isInstance(outer))) {
                    ctor = octor;
                    break;
                }
            }
            if (ctor == null) {
                log.warning("Class has no default constructor.", "class", clazz, new Exception());
                return null;
            }
            ctor.setAccessible(true);
            _ctors.put(clazz, ctor);
        }
        try {
            return (outer == null) ? ctor.newInstance() : ctor.newInstance(outer);
        } catch (Exception e) {
            log.warning("Failed to create new instance.", "class", clazz, e);
            return null;
        }
    }

    /**
     * Sets an inner object's outer class reference if it has one.
     */
    public static void setOuter (Object object, Object outer)
    {
        Class<?> clazz = object.getClass();
        if (!isInner(clazz)) {
            return;
        }
        if (object instanceof Inner) {
            ((Inner)object).setOuter(outer);
            return;
        }
        try {
            getOuterField(clazz).set(object, outer);
        } catch (IllegalAccessException e) {
            // shouldn't happen
        }
    }

    /**
     * Returns a reference to an inner object's outer class reference, or <code>null</code> if
     * the object represents an instance of a static class.
     */
    public static Object getOuter (Object object)
    {
        Class<?> clazz = object.getClass();
        if (!isInner(clazz)) {
            return null;
        }
        if (object instanceof Inner) {
            return ((Inner)object).getOuter();
        }
        try {
            return getOuterField(clazz).get(object);
        } catch (IllegalAccessException e) {
            return null; // shouldn't happen
        }
    }

    /**
     * Determines whether the specified class is a non-static inner class.
     */
    public static boolean isInner (Class<?> clazz)
    {
        return getOuterClass(clazz) != null;
    }

    /**
     * Returns the outer class for the given inner class (or <code>null</code> if not an inner
     * class).
     */
    public static Class<?> getOuterClass (Class<?> clazz)
    {
        Class<?> oclazz = _oclasses.get(clazz);
        if (oclazz == null) {
            Class<?> dclazz = clazz.getDeclaringClass();
            if (dclazz != null && !Modifier.isStatic(clazz.getModifiers())) {
                oclazz = dclazz;

            } else if (Inner.class.isAssignableFrom(clazz)) {
                for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
                    Class<?>[] ptypes = ctor.getParameterTypes();
                    if (ptypes.length > 0) {
                        oclazz = ptypes[0];
                        break;
                    }
                }
            } else {
                oclazz = Void.class;
            }
            _oclasses.put(clazz, oclazz);
        }
        return (oclazz == Void.class) ? null : oclazz;
    }

    /**
     * Returns a reference to the outer class reference field.
     */
    protected static Field getOuterField (Class<?> clazz)
    {
        Field field = _outers.get(clazz);
        if (field == null) {
            Class<?> dclazz = clazz.getDeclaringClass();
            for (Field ofield : clazz.getDeclaredFields()) {
                if (ofield.isSynthetic() && ofield.getType() == dclazz &&
                        ofield.getName().startsWith("this")) {
                    field = ofield;
                    break;
                }
            }
            field.setAccessible(true);
            _outers.put(clazz, field);
        }
        return field;
    }

    /** Maps inner classes to their outer class reference fields. */
    protected static HashMap<Class<?>, Field> _outers = new HashMap<Class<?>, Field>();

    /** Maps classes to their outer classes, or to {@link Void} if they are not inner classes. */
    protected static HashMap<Class<?>, Class<?>> _oclasses = new HashMap<Class<?>, Class<?>>();

    /** Maps classes to their default constructors. */
    protected static HashMap<Class<?>, Constructor<?>> _ctors =
            new HashMap<Class<?>, Constructor<?>>();
}
