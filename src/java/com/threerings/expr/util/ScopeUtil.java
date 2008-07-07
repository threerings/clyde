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
     * Attempts to resolve a quaternion variable.
     */
    public static Quaternion resolve (Scope scope, String name, Quaternion defvalue)
    {
        return resolve(scope, name, defvalue, Quaternion.class);
    }
    
    /**
     * Attempts to resolve a transform variable.
     */
    public static Transform3D resolve (Scope scope, String name, Transform3D defvalue)
    {
        return resolve(scope, name, defvalue, Transform3D.class);
    }
    /**
     * Attempts to resolve a vector variable.
     */
    public static Vector3f resolve (Scope scope, String name, Vector3f defvalue)
    {
        return resolve(scope, name, defvalue, Vector3f.class);
    }
    
    /**
     * Attempts to resolve a color variable.
     */
    public static Color4f resolve (Scope scope, String name, Color4f defvalue)
    {
        return resolve(scope, name, defvalue, Color4f.class);
    }
    
    /**
     * Attempts to resolve a function variable.
     */
    public static Function resolve (Scope scope, String name, Function defvalue)
    {
        return resolve(scope, name, defvalue, Function.class);
    }
    
    /**
     * Attempts to resolve a mutable float variable.
     */
    public static MutableFloat resolve (Scope scope, String name, MutableFloat defvalue)
    {
        return resolve(scope, name, defvalue, MutableFloat.class);
    }
    
    /**
     * Attempts to resolve a mutable long variable.
     */
    public static MutableLong resolve (Scope scope, String name, MutableLong defvalue)
    {
        return resolve(scope, name, defvalue, MutableLong.class);
    }
    
    /**
     * Attempts to resolve the identified variable in the given scope.  If not found there,
     * searches the parent of that scope, and so on.
     *
     * @return the variable value, or <code>defvalue</code> if not found anywhere in the chain.
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
        
        // rise up through the scopes looking for the requested variable
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
     * Attempts to retrieve the value of the identified variable using reflection.
     *
     * @param object the object upon which to reflect.
     * @return the variable value, or <code>null</code> if not found.
     */
    public static <T> T get (final Object object, String name, Class<T> clazz)
    {
        Member member = getMembers(object.getClass()).get(name);
        if (member instanceof Field) {
            try {
                Object value = ((Field)member).get(object);
                if (clazz.isInstance(value)) {
                    return clazz.cast(value);
                }
            } catch (IllegalAccessException e) {
                log.warning("Couldn't access field.", "class",
                    object.getClass(), "field", member, e);
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
