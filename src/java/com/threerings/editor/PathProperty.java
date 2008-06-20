//
// $Id$

package com.threerings.editor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;

import java.util.ArrayList;

import com.samskivert.util.StringUtil;

import com.threerings.config.ConfigReference;
import com.threerings.editor.util.PropertyUtil;

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
     * @param reference the reference object from which we derive our property chains.
     * @param paths the list of paths.
     * @throws InvalidPathsException if none of the supplied paths are valid.
     */
    public PathProperty (String name, Object reference, String... paths)
        throws InvalidPathsException
    {
        _name = name;

        // attempt to resolve each path, storing the successes
        ArrayList<Property[]> list = new ArrayList<Property[]>();
        for (String path : paths) {
            try {
                Property[] props = resolvePath(reference, path);
                if (props != null) {
                    list.add(props);
                }
            } catch (IOException e) {
                log.warning("Error parsing path [path=" + path + "].", e);
            }
        }
        if (list.isEmpty()) {
            throw new InvalidPathsException(StringUtil.toString(paths));
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
    public String getMode ()
    {
        return PropertyUtil.getMode(_paths[0]);
    }

    @Override // documentation inherited
    public String getUnits ()
    {
        return PropertyUtil.getUnits(_paths[0]);
    }

    @Override // documentation inherited
    public double getMinimum ()
    {
        return PropertyUtil.getMinimum(_paths[0]);
    }

    @Override // documentation inherited
    public double getMaximum ()
    {
        return PropertyUtil.getMaximum(_paths[0]);
    }

    @Override // documentation inherited
    public double getStep ()
    {
        return PropertyUtil.getStep(_paths[0]);
    }

    @Override // documentation inherited
    public double getScale ()
    {
        return PropertyUtil.getScale(_paths[0]);
    }

    @Override // documentation inherited
    public int getMinSize ()
    {
        return PropertyUtil.getMinSize(_paths[0]);
    }

    @Override // documentation inherited
    public int getMaxSize ()
    {
        return PropertyUtil.getMaxSize(_paths[0]);
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
            int last = path.length - 1;
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
        throws IOException
    {
        // create the tokenizer for the path
        StreamTokenizer tok = new StreamTokenizer(new StringReader(path));
        tok.ordinaryChar('/');
        tok.wordChars('_', '_');

        // step through the path components
        ArrayList<Property> props = new ArrayList<Property>();
        while (tok.nextToken() != StreamTokenizer.TT_EOF) {
            if (tok.ttype != StreamTokenizer.TT_WORD) {
                log.warning("Unexpected token [path=" + path + ", token=" + tok + "].");
                return null;
            }
            Property prop = getProperty(object, tok);
            if (prop == null) {
                return null;
            }
            props.add(prop);
            object = prop.get(object);
        }

        // the final component must be editable
        int size = props.size();
        return (size == 0 || props.get(size - 1).getAnnotation() == null) ?
            null : props.toArray(new Property[size]);
    }

    /**
     * Attempts to find and return the named property, returning <code>null</code> on failure.
     */
    protected static Property getProperty (Object object, StreamTokenizer tok)
        throws IOException
    {
        if (object == null) {
            return null;
        }
        // first token is the name of the property
        String name = tok.sval;

        // first search the (cached) editable properties
        Class clazz = object.getClass();
        Property[] props = Introspector.getProperties(clazz);
        for (Property prop : props) {
            if (prop.getName().equals(name)) {
                return getProperty(object, prop, tok);
            }
        }
        // then look for a normal field or getter
        for (Class<?> sclazz = clazz; sclazz != null; sclazz = sclazz.getSuperclass()) {
            try {
                Field field = sclazz.getDeclaredField(name);
                field.setAccessible(true);
                return getProperty(object, new FieldProperty(field), tok);
            } catch (NoSuchFieldException e) { }

            try {
                Method method = sclazz.getDeclaredMethod(name);
                method.setAccessible(true);

                // a slight abuse of MethodProperty: use another reference to the getter instead
                // of a setter so that we don't have to check for a null setter in getAnnotation.
                // the set method should never be called
                return getProperty(object, new MethodProperty(method, method), tok);
            } catch (NoSuchMethodException e) { }
        }
        log.warning("Failed to find property [name=" + name + "].");
        return null;
    }

    /**
     * Provides additional handling for subscripts.
     */
    protected static Property getProperty (Object object, Property prop, StreamTokenizer tok)
        throws IOException
    {
        if (tok.nextToken() != '[') {
            return prop;
        }
        Object value = prop.get(object);
        if (value == null) {
            return null;
        }
        if (tok.nextToken() == StreamTokenizer.TT_NUMBER && value.getClass().isArray()) {
            int idx = (int)tok.nval;
            if (idx >= 0 && idx < Array.getLength(value)) {
                if (tok.nextToken() != ']') {
                    log.warning("Missing matching bracket [token=" + tok + "].");
                    return null;
                }
                return getProperty(object, new ArrayIndexProperty(prop, idx), tok);
            }

        } else if (tok.ttype == '"' && value instanceof ConfigReference) {
            String key = tok.sval;

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
