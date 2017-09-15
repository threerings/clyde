package com.threerings.editor;

/**
 * Implemented by editable object that be coerced to another type.
 */
public interface Coercible
{
    /**
     * Attempt to coerce this instance to the exact type specified, or return null or an
     * invalid object. The caller will validate the type of the object so it's not
     * strictly necessary to do. For example if there is only one possible coercian
     * you may just return it directly and let the caller figure out if it "works", or
     * you can use the exactType parameter to assist in deciding what to return.
     */
    public Object coerceTo (Class<?> exactType);
}
