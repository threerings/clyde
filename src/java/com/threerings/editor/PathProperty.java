//
// $Id$

package com.threerings.editor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import java.util.ArrayList;

import static com.threerings.editor.Log.*;

/**
 * A property that
 */
public class PathProperty extends Property
{
    /**
     * Creates a new path property.
     *
     * @param name the name of the property.
     * @param object the example object from which we derive our property chains.
     * @param paths the list of paths.
     */
    public PathProperty (String name, Object object, String... paths)
    {
        _name = name;

        ArrayList<Property[]> list = new ArrayList<Property[]>();
        for (String path : paths) {
            Property[] props = resolvePath(object, path);
            if (props != null) {
                list.add(props);
            }
        }
        _paths = list.toArray(new Property[list.size()][]);
    }

    @Override // documentation inherited
    public Class getType ()
    {
        return _paths[0][_paths[0].length - 1].getType();
    }

    @Override // documentation inherited
    public Type getGenericType ()
    {
        return _paths[0][_paths[0].length - 1].getGenericType();
    }

    @Override // documentation inherited
    public <T extends Annotation> T getAnnotation (Class<T> clazz)
    {
        return _paths[0][_paths[0].length - 1].getAnnotation(clazz);
    }

    @Override // documentation inherited
    public Object get (Object object)
    {
        for (Property property : _paths[0]) {
            object = property.get(object);
        }
        return object;
    }

    @Override // documentation inherited
    public void set (Object object, Object value)
    {
        for (Property[] path : _paths) {
            Object obj = object;
            int last = _paths.length - 1;
            for (int ii = 0; ii < last; ii++) {
                obj = path[ii].get(obj);
            }
            path[last].set(obj, value);
        }
    }

    /**
     * Attempts to resolve the provided path into a property chain, returning <code>null</code>
     * on failure.
     */
    protected static Property[] resolvePath (Object object, String path)
    {
        // step through the path components
        ArrayList<Property> props = new ArrayList<Property>();
        int idx;
        do {
            idx = path.indexOf('/');
            String name = (idx == -1) ? path : path.substring(0, idx);
            path = path.substring(idx + 1);
            Property prop = getProperty(object, name);
            if (prop == null) {
                return null;
            }
            props.add(prop);
            object = prop.get(object);

        } while (idx != -1);

        // the final component must be editable
        int size = props.size();
        return (props.get(size - 1).getAnnotation() == null) ?
            null : props.toArray(new Property[size]);
    }

    /**
     * Attempts to find and return the named property, returning <code>null</code> on failure.
     */
    protected static Property getProperty (Object object, String name)
    {
        if (object == null) {
            return null;
        }
        // remove the array index, if any
        String index = "";
        int idx = name.indexOf('[');
        if (idx != -1) {
            index = name.substring(idx);
            name = name.substring(0, idx);
        }
        // first search the (cached) editable properties
        Class clazz = object.getClass();
        Property[] props = Introspector.getProperties(clazz);
        for (Property prop : props) {
            if (prop.getName().equals(name)) {
                return getProperty(object, prop, index);
            }
        }
        // then look for a normal field or getter
        for (Class<?> sclazz = clazz; sclazz != null; sclazz = sclazz.getSuperclass()) {
            try {
                Field field = sclazz.getDeclaredField(name);
                field.setAccessible(true);
                return getProperty(object, new FieldProperty(field), index);
            } catch (NoSuchFieldException e) { }

            try {
                Method method = sclazz.getDeclaredMethod(name);
                method.setAccessible(true);

                // a slight abuse of MethodProperty: use another reference to the getter instead
                // of a setter so that we don't have to check for a null setter in getAnnotation.
                // the set method should never be called
                return getProperty(object, new MethodProperty(method, method), index);
            } catch (NoSuchMethodException e) { }
        }
        return null;
    }

    /**
     * Provides additional handling for array indices.
     */
    protected static Property getProperty (Object object, Property prop, String index)
    {
        if (index.length() == 0) {
            return prop;
        }
        Object value = prop.get(object);
        if (value == null) {
            return null;
        }
        int idx = index.indexOf(']', 1);
        if (idx == -1) {
            log.warning("Missing closing bracket in path index [index=" + index + "].");
            return null;
        }
        String content = index.substring(1, idx);
        index = index.substring(idx + 1);
        if (value.getClass().isArray()) {
            try {
                int iidx = Integer.parseInt(content);
                if (iidx >= 0 && iidx < Array.getLength(value)) {
                    return getProperty(object, new ArrayIndexProperty(prop, iidx), index);
                }
            } catch (NumberFormatException e) {
                log.warning("Invalid index in path [value=" + content + "].", e);
            }
        }
        return null;
    }

    /**
     * Superclass for properties addressing components of other properties.
     */
    protected static abstract class ComponentProperty extends Property
    {
        public ComponentProperty (Property base)
        {
            _base = base;
        }

        @Override // documentation inherited
        public Class getType ()
        {
            return _base.getComponentType();
        }

        @Override // documentation inherited
        public Type getGenericType ()
        {
            return _base.getGenericComponentType();
        }

        @Override // documentation inherited
        public <T extends Annotation> T getAnnotation (Class<T> clazz)
        {
            return _base.getAnnotation(clazz);
        }

        /** The base property. */
        protected Property _base;
    }

    /**
     * Addresses an element of an array.
     */
    protected static class ArrayIndexProperty extends ComponentProperty
    {
        public ArrayIndexProperty (Property base, int index)
        {
            super(base);
            _name = base.getName() + "[" + index + "]";
            _index = index;
        }

        @Override // documentation inherited
        public Object get (Object object)
        {
            return Array.get(_base.get(object), _index);
        }

        @Override // documentation inherited
        public void set (Object object, Object value)
        {
            Array.set(_base.get(object), _index, value);
        }

        /** The index of the element to address. */
        protected int _index;
    }

    /** The property chains for each path. */
    protected Property[][] _paths;
}
