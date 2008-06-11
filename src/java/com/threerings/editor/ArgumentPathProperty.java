//
// $Id$

package com.threerings.editor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import java.util.Arrays;
import java.util.Map;

import com.threerings.util.DeepOmit;
import com.threerings.util.Shallow;

/**
 * Extends {@link PathProperty} to read values from and write values to a {@link Map}, using
 * the reference object passed to the constructor as a source of default values.
 */
public class ArgumentPathProperty extends PathProperty
{
    /**
     * Creates a new map property.
     *
     * @param name the name of the property.
     * @param reference the reference object from which we derive our property chains and default
     * values.
     * @param paths the list of paths.
     * @throws InvalidPathsException if none of the supplied paths are valid.
     */
    public ArgumentPathProperty (String name, Object reference, String... paths)
        throws InvalidPathsException
    {
        super(name, reference, paths);
        _reference = reference;
    }

    @Override // documentation inherited
    public Object get (Object object)
    {
        Map map = (Map)object;
        return map.containsKey(_name) ? map.get(_name) : super.get(_reference);
    }

    @Override // documentation inherited
    public void set (Object object, Object value)
    {
        @SuppressWarnings("unchecked") Map<Object, Object> map =
            (Map<Object, Object>)object;

        // use Arrays.deepEquals in order to compare arrays sensibly
        _a1[0] = super.get(_reference);
        _a2[0] = value;
        if (Arrays.deepEquals(_a1, _a2)) {
            map.remove(_name);
        } else {
            map.put(_name, value);
        }
    }

    /** The reference object from which we obtain the default values. */
    @Shallow
    protected Object _reference;

    /** Used for object comparisons using {@link Arrays#deepEquals}. */
    @DeepOmit
    protected Object[] _a1 = new Object[1], _a2 = new Object[1];
}
