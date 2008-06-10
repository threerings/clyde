//
// $Id$

package com.threerings.editor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import java.util.Map;

/**
 * A property that gets its type and annotations from a base property, but reads from and writes
 * to a map.
 */
public class MapProperty extends Property
{
    /**
     * Creates a new map property.
     *
     * @param name the name of the property.
     * @param base the base property from which we get our type and annotations.
     */
    public MapProperty (String name, Object baseObject, Property baseProperty)
    {
        _name = name;
        _baseObject = baseObject;
        _baseProperty = baseProperty;
    }

    @Override // documentation inherited
    public Class getType ()
    {
        return _baseProperty.getType();
    }

    @Override // documentation inherited
    public Type getGenericType ()
    {
        return _baseProperty.getGenericType();
    }

    @Override // documentation inherited
    public <T extends Annotation> T getAnnotation (Class<T> clazz)
    {
        return _baseProperty.getAnnotation(clazz);
    }

    @Override // documentation inherited
    public Object get (Object object)
    {
        Map map = (Map)object;
        return map.containsKey(_name) ? map.get(_name) : _baseProperty.get(_baseObject);
    }

    @Override // documentation inherited
    public void set (Object object, Object value)
    {
        @SuppressWarnings("unchecked") Map<Object, Object> map =
            (Map<Object, Object>)object;
        map.put(_name, value);
    }

    /** The base object from which we obtain the default value. */
    protected Object _baseObject;

    /** The base property from which we get our type and annotations. */
    protected Property _baseProperty;
}
