//
// $Id$

package com.threerings.editor;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Type;

import com.samskivert.util.StringUtil;

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
    public Member getMember ()
    {
        return _field;
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
    public boolean getBoolean (Object object)
    {
        try {
            return _field.getBoolean(object);
        } catch (IllegalAccessException e) {
            logWarning(e);
            return false;
        }
    }

    @Override // documentation inherited
    public byte getByte (Object object)
    {
        try {
            return _field.getByte(object);
        } catch (IllegalAccessException e) {
            logWarning(e);
            return 0;
        }
    }

    @Override // documentation inherited
    public char getChar (Object object)
    {
        try {
            return _field.getChar(object);
        } catch (IllegalAccessException e) {
            logWarning(e);
            return 0;
        }
    }

    @Override // documentation inherited
    public double getDouble (Object object)
    {
        try {
            return _field.getDouble(object);
        } catch (IllegalAccessException e) {
            logWarning(e);
            return 0.0;
        }
    }

    @Override // documentation inherited
    public float getFloat (Object object)
    {
        try {
            return _field.getFloat(object);
        } catch (IllegalAccessException e) {
            logWarning(e);
            return 0f;
        }
    }

    @Override // documentation inherited
    public int getInt (Object object)
    {
        try {
            return _field.getInt(object);
        } catch (IllegalAccessException e) {
            logWarning(e);
            return 0;
        }
    }

    @Override // documentation inherited
    public long getLong (Object object)
    {
        try {
            return _field.getLong(object);
        } catch (IllegalAccessException e) {
            logWarning(e);
            return 0L;
        }
    }

    @Override // documentation inherited
    public short getShort (Object object)
    {
        try {
            return _field.getShort(object);
        } catch (IllegalAccessException e) {
            logWarning(e);
            return 0;
        }
    }

    @Override // documentation inherited
    public Object get (Object object)
    {
        try {
            return _field.get(object);
        } catch (IllegalAccessException e) {
            logWarning(e);
            return null;
        }
    }

    @Override // documentation inherited
    public void setBoolean (Object object, boolean value)
    {
        try {
            _field.setBoolean(object, value);
        } catch (IllegalAccessException e) {
            logWarning(e);
        }
    }

    @Override // documentation inherited
    public void setByte (Object object, byte value)
    {
        try {
            _field.setByte(object, value);
        } catch (IllegalAccessException e) {
            logWarning(e);
        }
    }

    @Override // documentation inherited
    public void setChar (Object object, char value)
    {
        try {
            _field.setChar(object, value);
        } catch (IllegalAccessException e) {
            logWarning(e);
        }
    }

    @Override // documentation inherited
    public void setDouble (Object object, double value)
    {
        try {
            _field.setDouble(object, value);
        } catch (IllegalAccessException e) {
            logWarning(e);
        }
    }

    @Override // documentation inherited
    public void setFloat (Object object, float value)
    {
        try {
            _field.setFloat(object, value);
        } catch (IllegalAccessException e) {
            logWarning(e);
        }
    }

    @Override // documentation inherited
    public void setInt (Object object, int value)
    {
        try {
            _field.setInt(object, value);
        } catch (IllegalAccessException e) {
            logWarning(e);
        }
    }

    @Override // documentation inherited
    public void setLong (Object object, long value)
    {
        try {
            _field.setLong(object, value);
        } catch (IllegalAccessException e) {
            logWarning(e);
        }
    }

    @Override // documentation inherited
    public void setShort (Object object, short value)
    {
        try {
            _field.setShort(object, value);
        } catch (IllegalAccessException e) {
            logWarning(e);
        }
    }

    @Override // documentation inherited
    public void set (Object object, Object value)
    {
        try {
            _field.set(object, value);
        } catch (IllegalAccessException e) {
            logWarning(e);
        }
    }

    /**
     * Logs a warning when we fail to set a field.
     */
    protected void logWarning (IllegalAccessException e)
    {
        log.warning("Failed to access property [field=" + _field + "].", e);
    }

    /** The property field. */
    protected Field _field;
}
