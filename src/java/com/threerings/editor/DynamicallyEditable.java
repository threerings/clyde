//
// $Id$

package com.threerings.editor;

/**
 * An interface for objects with dynamically generated editable properties.
 */
public interface DynamicallyEditable
{
    /**
     * Returns an array containing the dynamic properties of this object.
     */
    public Property[] getDynamicProperties ();
}
