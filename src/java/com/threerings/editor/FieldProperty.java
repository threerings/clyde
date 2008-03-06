//
// $Id$

package com.threerings.editor;

import java.lang.annotation.Annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import com.samskivert.util.StringUtil;

import static java.util.logging.Level.*;
import static com.threerings.editor.Log.*;

/**
 * A property accessed directly through a field.
 */
public class FieldProperty extends Property
{
    public FieldProperty (Field field)
    {
        _field = field;
        _name = StringUtil.toUSLowerCase(StringUtil.unStudlyName(_field.getName()));
    }

    @Override // documentation inherited
    public Class getType ()
    {
        return _field.getType();
    }

    @Override // documentation inherited
    public Type getGenericType ()
    {
        return _field.getGenericType();
    }

    @Override // documentation inherited
    public <T extends Annotation> T getAnnotation (Class<T> clazz)
    {
        return _field.getAnnotation(clazz);
    }

    @Override // documentation inherited
    public Object get (Object object)
    {
        try {
            return _field.get(object);
        } catch (IllegalAccessException e) {
            log.log(WARNING, "Failed to get property [field=" + _field + "].", e);
            return null;
        }
    }

    @Override // documentation inherited
    public void set (Object object, Object value)
    {
        try {
            _field.set(object, value);
        } catch (IllegalAccessException e) {
            log.log(WARNING, "Failed to set property [field=" + _field + "].", e);
        }
    }

    /** The property field. */
    protected Field _field;
}
