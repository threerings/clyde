//
// $Id$

package com.threerings.expr;

import java.util.HashMap;

import com.samskivert.util.ObserverList;

import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.renderer.Color4f;

import com.threerings.expr.util.ScopeUtil;

/**
 * A {@link Scope} that allows dynamic reparenting and the addition and removal of variables.
 */
public class DynamicScope
    implements Scope
{
    /**
     * Creates a new scope.
     */
    public DynamicScope (Object owner, String name)
    {
        this(owner, name, null);
    }

    /**
     * Creates a new scope with the supplied scope as its parent.
     */
    public DynamicScope (Object owner, String name, Scope parent)
    {
        _owner = owner;
        _name = name;
        _parent = parent;
    }

    /**
     * Sets the parent of this scope.
     */
    public void setParent (Scope parent)
    {
        _parent = parent;
        wasUpdated(); 
    }

    /**
     * Sets the value of the named variable in this scope.
     */
    public void put (String name, Object value)
    {
        _variables.put(name, value);
        wasUpdated();
    }
    
    /**
     * Removes the named variable from this scope.
     */
    public void remove (String name)
    {
        _variables.remove(name);
        wasUpdated();
    }
    
    /**
     * Starts a compound update.  Update notifications will be deferred until
     * {@link #endCompoundUpdate} is called.
     */
    public void startCompoundUpdate ()
    {
        _compoundDepth++;
    }
    
    /**
     * Ends a compound update.
     */
    public void endCompoundUpdate ()
    {
        if (--_compoundDepth == 0) {
            wasUpdated();
        }
    }
    
    // documentation inherited from interface Scope
    public String getScopeName ()
    {
        return _name;
    }
    
    // documentation inherited from interface Scope
    public Scope getParentScope ()
    {
        return _parent;
    }
    
    // documentation inherited from interface Scope
    public <T> T get (String name, Class<T> clazz)
    {
        // first try the dynamic variables, then the reflective ones
        Object value = _variables.get(name);
        return clazz.isInstance(value) ? clazz.cast(value) : ScopeUtil.get(_owner, name, clazz);
    }
    
    // documentation inherited from interface Scope
    public void addListener (ScopeUpdateListener listener)
    {
        _listeners.add(listener);
    }
    
    // documentation inherited from interface Scope
    public void removeListener (ScopeUpdateListener listener)
    {
        _listeners.remove(listener);
    }
    
    /**
     * Notes that this scope has been updated.
     */
    public void wasUpdated ()
    {
        if (_compoundDepth == 0) {
            final ScopeEvent event = new ScopeEvent(this);
            _listeners.apply(new ObserverList.ObserverOp<ScopeUpdateListener>() {
                public boolean apply (ScopeUpdateListener listener) {
                    listener.scopeUpdated(event);
                    return true;
                }
            });
        }
    }
    
    /** The owner of this scope. */
    protected Object _owner;
    
    /** The name of this scope. */
    protected String _name;

    /** A reference to the parent scope. */
    protected Scope _parent;

    /** The compound update depth. */
    protected int _compoundDepth;
    
    /** The dynamic variables in this scope. */
    protected HashMap<String, Object> _variables = new HashMap<String, Object>();

    /** The listeners to this scope. */
    protected ObserverList<ScopeUpdateListener> _listeners = ObserverList.newFastUnsafe();
}
