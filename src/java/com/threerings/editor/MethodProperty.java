//
// $Id$

package com.threerings.editor;

import java.lang.annotation.Annotation;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.samskivert.util.StringUtil;

import static com.threerings.editor.Log.*;

/**
 * A property accessed through a pair of getter/setter methods.
 */
public class MethodProperty extends Property
{
    public MethodProperty (Method getter, Method setter)
    {
        _getter = getter;
        _setter = setter;
        _name = StringUtil.toUSLowerCase(StringUtil.unStudlyName(_setter.getName().substring(3)));
    }

    @Override // documentation inherited
    public Class getType ()
    {
        return _getter.getReturnType();
    }

    @Override // documentation inherited
    public Type getGenericType ()
    {
        return _getter.getGenericReturnType();
    }

    @Override // documentation inherited
    public <T extends Annotation> T getAnnotation (Class<T> clazz)
    {
        return _setter.getAnnotation(clazz);
    }

    @Override // documentation inherited
    public Object get (Object object)
    {
        try {
            return _getter.invoke(object);
        } catch (Exception e) {
            log.warning("Failed to get property [getter=" + _getter + "].", e);
            return null;
        }
    }

    @Override // documentation inherited
    public void set (Object object, Object value)
    {
        try {
            _setter.invoke(object, value);
        } catch (Exception e) {
            log.warning("Failed to set property [setter=" + _setter + "].", e);
        }
    }

    /** The getter and setter methods. */
    protected Method _getter, _setter;
}
