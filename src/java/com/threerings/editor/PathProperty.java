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
import java.util.List;

import com.samskivert.util.StringUtil;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ManagedConfig;
import com.threerings.config.Parameter;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.util.PropertyUtil;

import static com.threerings.editor.Log.*;

/**
 * A property that
 */
public class PathProperty extends Property
{
    /**
     * Attempts to resolve the provided path into a property chain, returning <code>null</code>
     * on failure.
     */
    public static Property[] createPath (ConfigManager cfgmgr, Object object, String path)
    {
        // create the tokenizer for the path
        StreamTokenizer tok = new StreamTokenizer(new StringReader(path));
        tok.ordinaryChar('/');
        tok.ordinaryChar('\'');
        tok.wordChars('_', '_');

        // step through the path components
        ArrayList<Property> props = new ArrayList<Property>();
        try {
            while (tok.nextToken() != StreamTokenizer.TT_EOF) {
                if (tok.ttype != StreamTokenizer.TT_WORD) {
                    log.warning("Unexpected token [path=" + path + ", token=" + tok + "].");
                    return null;
                }
                Property prop = getProperty(cfgmgr, object, tok);
                if (prop == null) {
                    return null;
                }
                props.add(prop);
                object = prop.get(object);
            }
        } catch (IOException e) {
            log.warning("Error parsing path [path=" + path + "].", e);
            return null;
        }

        // do not return a zero-length array
        int size = props.size();
        return (size == 0) ? null : props.toArray(new Property[size]);
    }

    /**
     * Creates a new path property.
     *
     * @param cfgmgr the config manager to use when resolving references.
     * @param name the name of the property.
     * @param reference the reference object from which we derive our property chains.
     * @param paths the list of paths.
     * @throws InvalidPathsException if none of the supplied paths are valid.
     */
    public PathProperty (ConfigManager cfgmgr, String name, Object reference, String... paths)
        throws InvalidPathsException
    {
        _name = name;

        // attempt to resolve each path, storing the successes
        ArrayList<Property[]> list = new ArrayList<Property[]>();
        for (String path : paths) {
            // the final component must be editable
            Property[] props = createPath(cfgmgr, reference, path);
            if (props != null && props[props.length - 1].getAnnotation() != null) {
                list.add(props);
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
     * Attempts to find and return the named property, returning <code>null</code> on failure.
     */
    protected static Property getProperty (
        ConfigManager cfgmgr, Object object, StreamTokenizer tok)
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
                return getProperty(cfgmgr, object, prop, tok);
            }
        }
        // then look for a normal field or getter
        for (Class<?> sclazz = clazz; sclazz != null; sclazz = sclazz.getSuperclass()) {
            try {
                Field field = sclazz.getDeclaredField(name);
                field.setAccessible(true);
                return getProperty(cfgmgr, object, new FieldProperty(field), tok);
            } catch (NoSuchFieldException e) { }

            try {
                Method method = sclazz.getDeclaredMethod(name);
                method.setAccessible(true);

                // a slight abuse of MethodProperty: use another reference to the getter instead
                // of a setter so that we don't have to check for a null setter in getAnnotation.
                // the set method should never be called
                return getProperty(cfgmgr, object, new MethodProperty(method, method), tok);
            } catch (NoSuchMethodException e) { }
        }
        log.warning("Failed to find property [name=" + name + "].");
        return null;
    }

    /**
     * Provides additional handling for subscripts.
     */
    protected static Property getProperty (
        final ConfigManager cfgmgr, Object object, final Property base, StreamTokenizer tok)
        throws IOException
    {
        if (tok.nextToken() != '[') {
            return base;
        }
        Object value = base.get(object);
        if (value == null) {
            return null;
        }
        Property prop = null;
        if (tok.nextToken() == StreamTokenizer.TT_NUMBER) {
            final int idx = (int)tok.nval;
            if (value instanceof List) {
                if (idx >= 0 && idx < ((List)value).size()) {
                    prop = new IndexProperty(base, idx) {
                        public Object get (Object object) {
                            return ((List)base.get(object)).get(idx);
                        }
                        public void set (Object object, Object value) {
                            @SuppressWarnings("unchecked") List<Object> list =
                                (List<Object>)base.get(object);
                            list.set(idx, value);
                        }
                    };
                }
            } else if (value.getClass().isArray()) {
                if (idx >= 0 && idx < Array.getLength(value)) {
                    prop = new IndexProperty(base, idx) {
                        public Object get (Object object) {
                            return Array.get(base.get(object), idx);
                        }
                        public void set (Object object, Object value) {
                            Array.set(base.get(object), idx, value);
                        }
                    };
                }
            }
        } else if (tok.ttype == '"' && value instanceof ConfigReference) {
            final String arg = tok.sval;
            final @SuppressWarnings("unchecked") Class<ManagedConfig> clazz =
                (Class<ManagedConfig>)base.getArgumentType(ConfigReference.class);
            if (clazz == null) {
                log.warning("Couldn't determine config reference type [ref=" + value + "].");
                return null;
            }
            @SuppressWarnings("unchecked") ConfigReference<ManagedConfig> ref =
                (ConfigReference<ManagedConfig>)value;
            ManagedConfig config = cfgmgr.getConfig(clazz, ref);
            if (!(config instanceof ParameterizedConfig)) {
                return null;
            }
            ParameterizedConfig pconfig = (ParameterizedConfig)config;
            Parameter parameter = pconfig.getParameter(arg);
            if (parameter == null) {
                return null;
            }
            final Property aprop = parameter.getArgumentProperty(pconfig);
            if (aprop == null) {
                return null;
            }
            prop = new Property () { {
                    _name = base.getName() + "[\"" + arg.replace("\"", "\\\"") + "\"]";
                }
                public Class getType () {
                    return aprop.getType();
                }
                public Type getGenericType () {
                    return aprop.getGenericType();
                }
                public <T extends Annotation> T getAnnotation (Class<T> clazz) {
                    return aprop.getAnnotation(clazz);
                }
                public Object get (Object object) {
                    ConfigReference<ManagedConfig> ref = getReference(object);
                    Property prop = getArgumentProperty(ref);
                    return (prop == null) ? null : prop.get(ref.getArguments());
                }
                public void set (Object object, Object value) {
                    ConfigReference<ManagedConfig> ref = getReference(object);
                    Property prop = getArgumentProperty(ref);
                    if (prop != null) {
                        prop.set(ref.getArguments(), value);
                    }
                }
                @SuppressWarnings("unchecked")
                protected ConfigReference<ManagedConfig> getReference (Object object) {
                    return (ConfigReference<ManagedConfig>)base.get(object);
                }
                protected Property getArgumentProperty (ConfigReference<ManagedConfig> ref) {
                    ManagedConfig config = cfgmgr.getConfig(clazz, ref);
                    if (!(config instanceof ParameterizedConfig)) {
                        return null;
                    }
                    ParameterizedConfig pconfig = (ParameterizedConfig)config;
                    Parameter parameter = pconfig.getParameter(arg);
                    if (parameter == null) {
                        return null;
                    }
                    return parameter.getArgumentProperty(pconfig);
                }
            };
        }
        if (tok.nextToken() != ']') {
            log.warning("Missing matching bracket [token=" + tok + "].");
            return null;
        }
        return (prop == null) ? null : getProperty(cfgmgr, object, prop, tok);
    }

    /**
     * Superclass for properties addressing components of other properties.
     */
    protected static abstract class IndexProperty extends Property
    {
        public IndexProperty (Property base, int idx)
        {
            _name = (_base = base).getName() + "[" + idx + "]";
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

    /** The property chains for each path. */
    protected Property[][] _paths;
}
