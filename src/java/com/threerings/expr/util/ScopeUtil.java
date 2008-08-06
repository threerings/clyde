//
// $Id$

package com.threerings.expr.util;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import java.util.HashMap;

import com.google.common.collect.Maps;

import com.threerings.expr.Scope;

import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.opengl.renderer.Color4f;

import com.threerings.expr.Function;
import com.threerings.expr.Variable;
import com.threerings.expr.MutableFloat;
import com.threerings.expr.MutableLong;
import com.threerings.expr.Scoped;

import static com.threerings.ClydeLog.*;

/**
 * Some general utility methods relating to scopes.
 */
public class ScopeUtil
{
    /**
     * Attempts to resolve a quaternion symbol.
     */
    public static Quaternion resolve (Scope scope, String name, Quaternion defvalue)
    {
        return resolve(scope, name, defvalue, Quaternion.class);
    }

    /**
     * Attempts to resolve a transform symbol.
     */
    public static Transform3D resolve (Scope scope, String name, Transform3D defvalue)
    {
        return resolve(scope, name, defvalue, Transform3D.class);
    }
    /**
     * Attempts to resolve a vector symbol.
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
     * Attempts to resolve a mutable float symbol.
     */
    public static MutableFloat resolve (Scope scope, String name, MutableFloat defvalue)
    {
        return resolve(scope, name, defvalue, MutableFloat.class);
    }

    /**
     * Attempts to resolve a mutable long symbol.
     */
    public static MutableLong resolve (Scope scope, String name, MutableLong defvalue)
    {
        return resolve(scope, name, defvalue, MutableLong.class);
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
        Member member = getMembers(object.getClass()).get(name);
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
     * Retrieves the mapping from name to member for all scoped members of the specified
     * class.
     */
    protected static HashMap<String, Member> getMembers (Class clazz)
    {
        HashMap<String, Member> members = _members.get(clazz);
        if (members == null) {
            _members.put(clazz, members = createMembers(clazz));
        }
        return members;
    }

    /**
     * Creates the mapping from name to member for all scoped members of the specified
     * class.
     */
    protected static HashMap<String, Member> createMembers (Class clazz)
    {
        // add the superclass members
        HashMap<String, Member> members = new HashMap<String, Member>();
        Class sclazz = clazz.getSuperclass();
        if (sclazz != null) {
            members.putAll(getMembers(sclazz));
        }
        // add all scoped fields (stripping off the leading underscore, if present)
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Scoped.class)) {
                field.setAccessible(true);
                String name = field.getName();
                members.put(name.charAt(0) == '_' ? name.substring(1) : name, field);
            }
        }
        // add all scoped methods
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Scoped.class)) {
                method.setAccessible(true);
                members.put(method.getName(), method);
            }
        }
        return members;
    }

    /** Cached scoped members. */
    protected static HashMap<Class, HashMap<String, Member>> _members = Maps.newHashMap();
}
