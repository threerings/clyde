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

package com.threerings.expr.util;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import com.threerings.expr.Scope;

import com.threerings.math.Quaternion;
import com.threerings.math.Transform2D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;
import com.threerings.opengl.renderer.Color4f;

import com.threerings.expr.Bound;
import com.threerings.expr.Function;
import com.threerings.expr.Variable;
import com.threerings.expr.MutableBoolean;
import com.threerings.expr.MutableFloat;
import com.threerings.expr.MutableInteger;
import com.threerings.expr.MutableLong;
import com.threerings.expr.Scoped;

import static com.threerings.ClydeLog.log;

/**
 * Some general utility methods relating to scopes.
 */
public class ScopeUtil
{
    /**
     * Updates the {@link Bound} fields of the specified object using the provided scope.
     */
    public static void updateBound (Object object, Scope scope)
    {
        for (Field field : getBound(object.getClass())) {
            String name = field.getAnnotation(Bound.class).value();
            if (name.isEmpty()) {
                name = stripUnderscore(field.getName());
            }
            @SuppressWarnings("unchecked") Class<Object> type = (Class<Object>)field.getType();
            try {
                field.set(object, resolve(scope, name, field.get(object), type));
            } catch (IllegalAccessException e) {
                log.warning("Error accessing bound field.", "field", field, e);
            }
        }
    }

    /**
     * Attempts to resolve, then call the specified function with the given arguments.
     */
    public static Object call (Scope scope, String name, Object... args)
    {
        return resolve(scope, name, Function.NULL).call(args);
    }

    /**
     * Attempts to resolve a quaternion symbol.
     */
    public static Quaternion resolve (Scope scope, String name, Quaternion defvalue)
    {
        return resolve(scope, name, defvalue, Quaternion.class);
    }

    /**
     * Attempts to resolve a 2D transform symbol.
     */
    public static Transform2D resolve (Scope scope, String name, Transform2D defvalue)
    {
        return resolve(scope, name, defvalue, Transform2D.class);
    }

    /**
     * Attempts to resolve a 3D transform symbol.
     */
    public static Transform3D resolve (Scope scope, String name, Transform3D defvalue)
    {
        return resolve(scope, name, defvalue, Transform3D.class);
    }

    /**
     * Attempts to resolve a 2D vector symbol.
     */
    public static Vector2f resolve (Scope scope, String name, Vector2f defvalue)
    {
        return resolve(scope, name, defvalue, Vector2f.class);
    }

    /**
     * Attempts to resolve a 3D vector symbol.
     */
    public static Vector3f resolve (Scope scope, String name, Vector3f defvalue)
    {
        return resolve(scope, name, defvalue, Vector3f.class);
    }

    /**
     * Attempts to resolve a color symbol.
     */
    public static Color4f resolve (Scope scope, String name, Color4f defvalue)
    {
        return resolve(scope, name, defvalue, Color4f.class);
    }

    /**
     * Attempts to resolve a string symbol.
     */
    public static String resolve (Scope scope, String name, String defvalue)
    {
        return resolve(scope, name, defvalue, String.class);
    }

    /**
     * Attempts to resolve a function symbol.
     */
    public static Function resolve (Scope scope, String name, Function defvalue)
    {
        return resolve(scope, name, defvalue, Function.class);
    }

    /**
     * Attempts to resolve a variable symbol.
     */
    public static Variable resolve (Scope scope, String name, Variable defvalue)
    {
        return resolve(scope, name, defvalue, Variable.class);
    }

    /**
     * Attempts to resolve a mutable boolean symbol.
     */
    public static MutableBoolean resolve (Scope scope, String name, MutableBoolean defvalue)
    {
        return resolve(scope, name, defvalue, MutableBoolean.class);
    }

    /**
     * Attempts to resolve a mutable float symbol.
     */
    public static MutableFloat resolve (Scope scope, String name, MutableFloat defvalue)
    {
        return resolve(scope, name, defvalue, MutableFloat.class);
    }

    /**
     * Attempts to resolve a mutable integer symbol.
     */
    public static MutableInteger resolve (Scope scope, String name, MutableInteger defvalue)
    {
        return resolve(scope, name, defvalue, MutableInteger.class);
    }

    /**
     * Attempts to resolve a mutable long symbol.
     */
    public static MutableLong resolve (Scope scope, String name, MutableLong defvalue)
    {
        return resolve(scope, name, defvalue, MutableLong.class);
    }

    /**
     * Attempts to resolve a boolean symbol.
     */
    public static Boolean resolve (Scope scope, String name, Boolean defvalue)
    {
        return resolve(scope, name, defvalue, Boolean.class);
    }

    /**
     * Attempts to resolve a timestamp.
     */
    public static MutableLong resolveTimestamp (Scope scope, String name)
    {
        MutableLong res = resolve(scope, name, null, MutableLong.class);
        return (res != null)
            ? res
            : new MutableLong(System.currentTimeMillis());
    }

    /**
     * Attempts to resolve the identified symbol in the given scope.  If not found there,
     * searches the parent of that scope, and so on.
     *
     * @return the mapping for the symbol, or <code>defvalue</code> if not found anywhere in the
     * chain.
     */
    public static <T> T resolve (Scope scope, String name, T defvalue, Class<T> clazz)
    {
        // if the name includes a scope qualifier, look for that scope
        int idx = name.indexOf(':');
        if (idx != -1) {
            String qualifier = name.substring(0, idx);
            name = name.substring(idx + 1);
            while (scope != null && !qualifier.equals(scope.getScopeName())) {
                scope = scope.getParentScope();
            }
        }

        // rise up through the scopes looking for the requested symbol
        for (; scope != null; scope = scope.getParentScope()) {
            T value = scope.get(name, clazz);
            if (value != null) {
                return value;
            }
        }

        // no luck; return the default value
        return defvalue;
    }

    /**
     * Attempts to retrieve the value of the identified symbol using reflection.
     *
     * @param object the object upon which to reflect.
     * @return the symbol value, or <code>null</code> if not found.
     */
    public static <T> T get (final Object object, String name, Class<T> clazz)
    {
        if ("this".equals(name) && clazz.isInstance(object)) {
            return clazz.cast(object);
        }
        Member member = getScoped(object.getClass()).get(name);
        if (member instanceof Field) {
            if (clazz.isAssignableFrom(Variable.class)) {
                final Field field = (Field)member;
                return clazz.cast(new Variable() {
                    public boolean getBoolean () {
                        try {
                            return field.getBoolean(object);
                        } catch (Exception e) {
                            logWarning(e);
                            return false;
                        }
                    }
                    public byte getByte () {
                        try {
                            return field.getByte(object);
                        } catch (Exception e) {
                            logWarning(e);
                            return 0;
                        }
                    }
                    public char getChar () {
                        try {
                            return field.getChar(object);
                        } catch (Exception e) {
                            logWarning(e);
                            return 0;
                        }
                    }
                    public double getDouble () {
                        try {
                            return field.getDouble(object);
                        } catch (Exception e) {
                            logWarning(e);
                            return 0.0;
                        }
                    }
                    public float getFloat () {
                        try {
                            return field.getFloat(object);
                        } catch (Exception e) {
                            logWarning(e);
                            return 0f;
                        }
                    }
                    public int getInt () {
                        try {
                            return field.getInt(object);
                        } catch (Exception e) {
                            logWarning(e);
                            return 0;
                        }
                    }
                    public long getLong () {
                        try {
                            return field.getLong(object);
                        } catch (Exception e) {
                            logWarning(e);
                            return 0L;
                        }
                    }
                    public short getShort () {
                        try {
                            return field.getShort(object);
                        } catch (Exception e) {
                            logWarning(e);
                            return 0;
                        }
                    }
                    public Object get () {
                        try {
                            return field.get(object);
                        } catch (IllegalAccessException e) {
                            logWarning(e);
                            return null;
                        }
                    }
                    public void setBoolean (boolean value) {
                        try {
                            field.setBoolean(object, value);
                        } catch (Exception e) {
                            logWarning(e);
                        }
                    }
                    public void setByte (byte value) {
                        try {
                            field.setByte(object, value);
                        } catch (Exception e) {
                            logWarning(e);
                        }
                    }
                    public void setChar (char value) {
                        try {
                            field.setChar(object, value);
                        } catch (Exception e) {
                            logWarning(e);
                        }
                    }
                    public void setDouble (double value) {
                        try {
                            field.setDouble(object, value);
                        } catch (Exception e) {
                            logWarning(e);
                        }
                    }
                    public void setFloat (float value) {
                        try {
                            field.setFloat(object, value);
                        } catch (Exception e) {
                            logWarning(e);
                        }
                    }
                    public void setInt (int value) {
                        try {
                            field.setInt(object, value);
                        } catch (Exception e) {
                            logWarning(e);
                        }
                    }
                    public void setLong (long value) {
                        try {
                            field.setLong(object, value);
                        } catch (Exception e) {
                            logWarning(e);
                        }
                    }
                    public void setShort (short value) {
                        try {
                            field.setShort(object, value);
                        } catch (Exception e) {
                            logWarning(e);
                        }
                    }
                    public void set (Object value) {
                        try {
                            field.set(object, value);
                        } catch (Exception e) {
                            logWarning(e);
                        }
                    }
                    protected void logWarning (Exception e) {
                        log.warning("Error accessing field.", "class",
                            object.getClass(), "field", field, e);
                    }
                });
            } else {
                try {
                    Object value = ((Field)member).get(object);
                    if (clazz.isInstance(value)) {
                        return clazz.cast(value);
                    }
                } catch (IllegalAccessException e) {
                    log.warning("Error accessing field.", "class",
                        object.getClass(), "field", member, e);
                }
            }
        } else if (member instanceof Method && clazz.isAssignableFrom(Function.class)) {
            final Method method = (Method)member;
            return clazz.cast(new Function() {
                public Object call (Object... args) {
                    try {
                        return method.invoke(object, args);
                    } catch (Exception e) {
                        log.warning("Error invoking method.", "class", object.getClass(),
                            "method", method, "args", args, e);
                        return null;
                    }
                }
            });
        }
        return null;
    }

    /**
     * Retrieves the list of the specified class's bound fields.
     */
    protected static Field[] getBound (Class<?> clazz)
    {
        Field[] fields = _bound.get(clazz);
        if (fields == null) {
            _bound.put(clazz, fields = createBound(clazz));
        }
        return fields;
    }

    /**
     * Creates the list of bound fields for the specified class.
     */
    protected static Field[] createBound (Class<?> clazz)
    {
        // add the superclass fields
        ArrayList<Field> fields = new ArrayList<Field>();
        Class<?> sclazz = clazz.getSuperclass();
        if (sclazz != null) {
            Collections.addAll(fields, getBound(sclazz));
        }
        // add all bound fields
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Bound.class)) {
                field.setAccessible(true);
                fields.add(field);
            }
        }
        return fields.toArray(new Field[fields.size()]);
    }

    /**
     * Retrieves the mapping from name to member for all scoped members of the specified class.
     */
    protected static Map<String, Member> getScoped (Class<?> clazz)
    {
        Map<String, Member> members = _scoped.get(clazz);
        if (members == null) {
            // populate a mutable HashMap, but then copy it into an ImmutableMap
            members = Maps.newHashMap();
            populateScoped(clazz, members);
            members = ImmutableMap.copyOf(members);
            _scoped.put(clazz, members);
        }
        return members;
    }

    /**
     * Populates the mapping from name to member for all scoped members of the specified class.
     */
    protected static void populateScoped (Class<?> clazz, Map<String, Member> members)
    {
        // add the superclass members first
        Class<?> sclazz = clazz.getSuperclass();
        if (sclazz != null) {
            populateScoped(sclazz, members);
        }
        // add all scoped fields (stripping off the leading underscore, if present)
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Scoped.class)) {
                field.setAccessible(true);
                Object oldValue = members.put(stripUnderscore(field.getName()), field);
                if (oldValue != null) {
                    log.warning("Scoped field overwrote member from superclass",
                        "clazz", clazz, "name", field.getName());
                }
            }
        }
        // add all scoped methods
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Scoped.class)) {
                method.setAccessible(true);
                Object oldValue = members.put(method.getName(), method);
                if (oldValue != null) {
                    log.warning("Scoped method overwrote member from superclass",
                        "clazz", clazz, "name", method.getName());
                }
            }
        }
    }

    /**
     * Strips the leading underscore from the specified name, if present.
     */
    protected static String stripUnderscore (String name)
    {
        return (name.charAt(0) == '_') ? name.substring(1) : name;
    }

    /** Cached bound fields. */
    protected static Map<Class<?>, Field[]> _bound = Maps.newHashMap();

    /** Cached scoped members. */
    protected static Map<Class<?>, Map<String, Member>> _scoped = Maps.newHashMap();
}
