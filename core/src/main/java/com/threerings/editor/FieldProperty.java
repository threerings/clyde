//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.editor;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Type;

import com.samskivert.util.StringUtil;

import static com.threerings.editor.Log.log;

/**
 * A property accessed directly through a field.
 */
public class FieldProperty extends Property
{
    /**
     * Creates a new field property.
     */
    public FieldProperty (Field field)
    {
        _field = field;
        _name = StringUtil.toUSLowerCase(StringUtil.unStudlyName(_field.getName()));
    }

    @Override
    public Member getMember ()
    {
        return _field;
    }

    @Override
    public Class<?> getType ()
    {
        return _field.getType();
    }

    @Override
    public Type getGenericType ()
    {
        return _field.getGenericType();
    }

    @Override
    public boolean getBoolean (Object object)
    {
        try {
            return _field.getBoolean(object);
        } catch (IllegalAccessException e) {
            logWarning(e);
            return false;
        }
    }

    @Override
    public byte getByte (Object object)
    {
        try {
            return _field.getByte(object);
        } catch (IllegalAccessException e) {
            logWarning(e);
            return 0;
        }
    }

    @Override
    public char getChar (Object object)
    {
        try {
            return _field.getChar(object);
        } catch (IllegalAccessException e) {
            logWarning(e);
            return 0;
        }
    }

    @Override
    public double getDouble (Object object)
    {
        try {
            return _field.getDouble(object);
        } catch (IllegalAccessException e) {
            logWarning(e);
            return 0.0;
        }
    }

    @Override
    public float getFloat (Object object)
    {
        try {
            return _field.getFloat(object);
        } catch (IllegalAccessException e) {
            logWarning(e);
            return 0f;
        }
    }

    @Override
    public int getInt (Object object)
    {
        try {
            return _field.getInt(object);
        } catch (IllegalAccessException e) {
            logWarning(e);
            return 0;
        }
    }

    @Override
    public long getLong (Object object)
    {
        try {
            return _field.getLong(object);
        } catch (IllegalAccessException e) {
            logWarning(e);
            return 0L;
        }
    }

    @Override
    public short getShort (Object object)
    {
        try {
            return _field.getShort(object);
        } catch (IllegalAccessException e) {
            logWarning(e);
            return 0;
        }
    }

    @Override
    public Object get (Object object)
    {
        try {
            return _field.get(object);
        } catch (IllegalAccessException e) {
            logWarning(e);
            return null;
        }
    }

    @Override
    public void setBoolean (Object object, boolean value)
    {
        try {
            _field.setBoolean(object, value);
        } catch (IllegalAccessException e) {
            logWarning(e);
        }
    }

    @Override
    public void setByte (Object object, byte value)
    {
        try {
            _field.setByte(object, value);
        } catch (IllegalAccessException e) {
            logWarning(e);
        }
    }

    @Override
    public void setChar (Object object, char value)
    {
        try {
            _field.setChar(object, value);
        } catch (IllegalAccessException e) {
            logWarning(e);
        }
    }

    @Override
    public void setDouble (Object object, double value)
    {
        try {
            _field.setDouble(object, value);
        } catch (IllegalAccessException e) {
            logWarning(e);
        }
    }

    @Override
    public void setFloat (Object object, float value)
    {
        try {
            _field.setFloat(object, value);
        } catch (IllegalAccessException e) {
            logWarning(e);
        }
    }

    @Override
    public void setInt (Object object, int value)
    {
        try {
            _field.setInt(object, value);
        } catch (IllegalAccessException e) {
            logWarning(e);
        }
    }

    @Override
    public void setLong (Object object, long value)
    {
        try {
            _field.setLong(object, value);
        } catch (IllegalAccessException e) {
            logWarning(e);
        }
    }

    @Override
    public void setShort (Object object, short value)
    {
        try {
            _field.setShort(object, value);
        } catch (IllegalAccessException e) {
            logWarning(e);
        }
    }

    @Override
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
