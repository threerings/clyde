//
// $Id$

package com.threerings.expr;

/**
 * A general-purpose variable object.
 */
public abstract class Variable
{
    /**
     * Creates a new variable with the given initial value.
     */
    public static Variable newInstance (final boolean value)
    {
        return new Variable () {
            public boolean getBoolean () {
                return _value;
            }
            public Object get () {
                return _value;
            }
            public void setBoolean (boolean value) {
                _value = value;
            }
            public void set (Object value) {
                _value = (Boolean)value;
            }
            protected boolean _value = value;
        };
    }
    
    /**
     * Creates a new variable with the given initial value.
     */
    public static Variable newInstance (final byte value)
    {
        return new Variable () {
            public byte getByte () {
                return _value;
            }
            public Object get () {
                return _value;
            }
            public void setByte (byte value) {
                _value = value;
            }
            public void set (Object value) {
                _value = (Byte)value;
            }
            protected byte _value = value;
        };
    }
    
    /**
     * Creates a new variable with the given initial value.
     */
    public static Variable newInstance (final char value)
    {
        return new Variable () {
            public char getChar () {
                return _value;
            }
            public Object get () {
                return _value;
            }
            public void setChar (char value) {
                _value = value;
            }
            public void set (Object value) {
                _value = (Character)value;
            }
            protected char _value = value;
        };
    }
    
    /**
     * Creates a new variable with the given initial value.
     */
    public static Variable newInstance (final double value)
    {
        return new Variable () {
            public double getDouble () {
                return _value;
            }
            public Object get () {
                return _value;
            }
            public void setDouble (double value) {
                _value = value;
            }
            public void set (Object value) {
                _value = (Double)value;
            }
            protected double _value = value;
        };
    }
    
    /**
     * Creates a new variable with the given initial value.
     */
    public static Variable newInstance (final float value)
    {
        return new Variable () {
            public float getFloat () {
                return _value;
            }
            public Object get () {
                return _value;
            }
            public void setFloat (float value) {
                _value = value;
            }
            public void set (Object value) {
                _value = (Float)value;
            }
            protected float _value = value;
        };
    }
    
    /**
     * Creates a new variable with the given initial value.
     */
    public static Variable newInstance (final int value)
    {
        return new Variable () {
            public int getInt () {
                return _value;
            }
            public Object get () {
                return _value;
            }
            public void setInt (int value) {
                _value = value;
            }
            public void set (Object value) {
                _value = (Integer)value;
            }
            protected int _value = value;
        };
    }
    
    /**
     * Creates a new variable with the given initial value.
     */
    public static Variable newInstance (final long value)
    {
        return new Variable () {
            public long getLong () {
                return _value;
            }
            public Object get () {
                return _value;
            }
            public void setLong (long value) {
                _value = value;
            }
            public void set (Object value) {
                _value = (Long)value;
            }
            protected long _value = value;
        };
    }
    
    /**
     * Creates a new variable with the given initial value.
     */
    public static Variable newInstance (final short value)
    {
        return new Variable () {
            public short getShort () {
                return _value;
            }
            public Object get () {
                return _value;
            }
            public void setShort (short value) {
                _value = value;
            }
            public void set (Object value) {
                _value = (Short)value;
            }
            protected short _value = value;
        };
    }
    
    /**
     * Creates a new variable with the given initial value.
     */
    public static Variable newInstance (final Object value)
    {
        return new Variable () {
            public Object get () {
                return _value;
            }
            public void set (Object value) {
                _value = value;
            }
            protected Object _value = value;
        };
    }
    
    /**
     * Retrieves the value of the variable as a boolean.
     */
    public boolean getBoolean ()
    {
        return (Boolean)get();
    }
    
    /**
     * Retrieves the value of the variable as a byte.
     */
    public byte getByte ()
    {
        return (Byte)get();
    }
    
    /**
     * Retrieves the value of the variable as a char.
     */
    public char getChar ()
    {
        return (Character)get();
    }
    
    /**
     * Retrieves the value of the variable as a double.
     */
    public double getDouble ()
    {
        return (Double)get();
    }
    
    /**
     * Retrieves the value of the variable as a float.
     */
    public float getFloat ()
    {
        return (Float)get();
    }
    
    /**
     * Retrieves the value of the variable as an integer.
     */
    public int getInt ()
    {
        return (Integer)get();
    }
    
    /**
     * Retrieves the value of the variable as a long.
     */
    public long getLong ()
    {
        return (Long)get();
    }
    
    /**
     * Retrieves the value of the variable as a short.
     */
    public short getShort ()
    {
        return (Short)get();
    }
    
    /**
     * Retrieves the value of the variable.
     */
    public abstract Object get ();
    
    /**
     * Sets the value of the variable as a boolean.
     */
    public void setBoolean (boolean value)
    {
        set(value);
    }
    
    /**
     * Sets the value of the variable as a boolean.
     */
    public void setByte (byte value)
    {
        set(value);
    }
    
    /**
     * Sets the value of the variable as a boolean.
     */
    public void setChar (char value)
    {
        set(value);
    }
    
    /**
     * Sets the value of the variable as a boolean.
     */
    public void setDouble (double value)
    {
        set(value);
    }
    
    /**
     * Sets the value of the variable as a boolean.
     */
    public void setFloat (float value)
    {
        set(value);
    }
    
    /**
     * Sets the value of the variable as a boolean.
     */
    public void setInt (int value)
    {
        set(value);
    }
    
    /**
     * Sets the value of the variable as a boolean.
     */
    public void setLong (long value)
    {
        set(value);
    }
    
    /**
     * Sets the value of the variable as a boolean.
     */
    public void setShort (short value)
    {
        set(value);
    }
    
    /**
     * Sets the value of the variable.
     */
    public abstract void set (Object value);
}
