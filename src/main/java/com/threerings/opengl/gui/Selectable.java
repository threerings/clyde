//
// $Id$

package com.threerings.opengl.gui;

/**
 * Implemented by GUI components that allow selection from amongst enumerated values.
 */
public interface Selectable<T>
{
    /** ActionEvent action fired when the selection changes; arg is the selected item. */
    public static final String SELECTION_CHANGED = "selectionChanged";

    /**
     * Get the selected item, if any.
     */
    T getSelected ();

    /**
     * Set the selected item.
     */
    void setSelected (T item);
}
