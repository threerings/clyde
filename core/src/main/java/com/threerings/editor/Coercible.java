package com.threerings.editor;

/**
 * Implemented by editable object that be coerced to another type.
 */
public interface Coercible
{
    /**
     * Coerce this instance to the exact type specified, or return null.
     */
    public Object coerceTo (Class<?> exactType);
}
