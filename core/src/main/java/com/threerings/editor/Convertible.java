package com.threerings.editor;

public interface Convertible
{
    /**
     * Return an instance of the exact type specified, if this object can be "converted".
     */
    public Object convertTo (Class<?> exactType);
}
