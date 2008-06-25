//
// $Id$

package com.threerings.expr;

import java.util.HashMap;

import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.renderer.Color4f;

/**
 * Provides access to variables.
 */
public class Scope
{
    /**
     * Creates a new scope.
     */
    public Scope (String name)
    {
        this(name, null);
    }

    /**
     * Creates a new scope with the supplied scope as its parent.
     */
    public Scope (String name, Scope parent)
    {
        _name = name;
        _parent = parent;
    }

    /**
     * Returns the name of this scope.
     */
    public String getName ()
    {
        return _name;
    }

    /**
     * Returns a reference to the parent of this scope (or <code>null</code> if this is the
     * top level).
     */
    public Scope getParent ()
    {
        return _parent;
    }

    /**
     * Resets the scope's time epoch.
     */
    public void resetTime ()
    {
        _epoch.value = System.currentTimeMillis();
    }

    /**
     * Returns a reference to the epoch at the specified scope.  If the scope is not found,
     * the topmost epoch will be returned.
     */
    public MutableLong getEpoch (String scope)
    {
        return (_parent == null || _name.equals(scope)) ? _epoch : _parent.getEpoch(scope);
    }

    /**
     * Retrieves a reference to the variable with the specified name, returning the supplied
     * default if not found.
     */
    public MutableFloat getVariable (String name, float defvalue)
    {
        return getVariable(name, new MutableFloat(defvalue), MutableFloat.class);
    }

    /**
     * Retrieves a reference to the variable with the specified name, returning the supplied
     * default if not found.
     */
    public Quaternion getVariable (String name, Quaternion defvalue)
    {
        return getVariable(name, defvalue, Quaternion.class);
    }

    /**
     * Retrieves a reference to the variable with the specified name, returning the supplied
     * default if not found.
     */
    public Transform3D getVariable (String name, Transform3D defvalue)
    {
        return getVariable(name, defvalue, Transform3D.class);
    }

    /**
     * Retrieves a reference to the variable with the specified name, returning the supplied
     * default if not found.
     */
    public Vector3f getVariable (String name, Vector3f defvalue)
    {
        return getVariable(name, defvalue, Vector3f.class);
    }

    /**
     * Retrieves a reference to the variable with the specified name, returning the supplied
     * default if not found.
     */
    public Color4f getVariable (String name, Color4f defvalue)
    {
        return getVariable(name, defvalue, Color4f.class);
    }

    /**
     * Retrieves a reference to the variable with the specified name and class.  If the variable
     * does not exist in this scope, the search will continue to the parent.  If no scope in
     * the ancestry contains the variable, the supplied default value will be returned.
     */
    public <T> T getVariable (String name, T defvalue, Class<T> clazz)
    {
        Object obj = _variables.get(name);
        T var = clazz.isInstance(obj) ? clazz.cast(obj) : null;
        if (var != null) {
            return var;
        }
        return (_parent == null) ? defvalue : _parent.getVariable(name, defvalue, clazz);
    }

    /** The name of this scope. */
    protected String _name;

    /** A reference to the parent scope. */
    protected Scope _parent;

    /** The variables in this scope. */
    protected HashMap<String, Object> _variables = new HashMap<String, Object>();

    /** The epoch variable for this scope. */
    protected MutableLong _epoch = new MutableLong(System.currentTimeMillis());
}
