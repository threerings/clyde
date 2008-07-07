//
// $Id$

package com.threerings.expr;

/**
 * Provides access to variables.
 */
public interface Scope
{
    /**
     * Returns the name of this scope for purposes of qualification.  Can return <code>null</code>
     * if qualified variables cannot specifically address this scope.
     */
    public String getScopeName ();
    
    /**
     * Returns a reference to the parent scope, or <code>null</code> if this is the top level.
     */
    public Scope getParentScope ();
        
    /**
     * Looks up a variable in this scope.
     *
     * @return the value of the requested variable, or <code>null</code> if not found.
     */
    public <T> T get (String name, Class<T> clazz);
    
    /**
     * Adds a listener for changes in scope.
     */
    public void addListener (ScopeUpdateListener listener);
    
    /**
     * Removes a listener for changes in scope.
     */
    public void removeListener (ScopeUpdateListener listener);
}
