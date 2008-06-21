//
// $Id$

package com.threerings.editor;

import java.util.Map;

import com.threerings.config.ConfigManager;
import com.threerings.util.DeepUtil;
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
     * @param cfgmgr the config manager to use when resolving references.
     * @param name the name of the property.
     * @param reference the reference object from which we derive our property chains and default
     * values.
     * @param paths the list of paths.
     * @throws InvalidPathsException if none of the supplied paths are valid.
     */
    public ArgumentPathProperty (
        ConfigManager cfgmgr, String name, Object reference, String... paths)
        throws InvalidPathsException
    {
        super(cfgmgr, name, reference, paths);
        _reference = reference;
    }

    @Override // documentation inherited
    public Object get (Object object)
    {
        @SuppressWarnings("unchecked") Map<Object, Object> map =
            (Map<Object, Object>)object;
        if (!map.containsKey(_name)) {
            Object value = DeepUtil.copy(super.get(_reference));
            map.put(_name, value);
            return value;
        }
        return map.get(_name);
    }

    @Override // documentation inherited
    public void set (Object object, Object value)
    {
        @SuppressWarnings("unchecked") Map<Object, Object> map =
            (Map<Object, Object>)object;
        map.put(_name, value);
    }

    /** The reference object from which we obtain the default values. */
    @Shallow
    protected Object _reference;
}
