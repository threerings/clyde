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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.List;

import com.samskivert.util.ClassUtil;
import com.samskivert.util.StringUtil;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.DerivedConfig;
import com.threerings.config.ManagedConfig;
import com.threerings.config.Parameter;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.util.PropertyUtil;

import static com.threerings.editor.Log.log;

/**
 * A property that
 */
public class PathProperty extends Property
{
    /** A setting to disable get property warnings. */
    public static boolean showGetPropertyWarning = true;

    /**
     * Attempts to resolve the provided path into a property chain, returning <code>null</code>
     * on failure.
     */
    public static Property[] createPath (ConfigManager cfgmgr, Object object, String path)
    {
        // create the tokenizer for the path
        StreamTokenizer tok = new StreamTokenizer(new StringReader(path));
        tok.ordinaryChar('/');
        tok.ordinaryChar('.');
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
    public PathProperty (
        ConfigManager cfgmgr, String name, Object reference, String... paths)
            throws InvalidPathsException
    {
        _name = name;

        // attempt to resolve each path, storing the successes
        ArrayList<Property[]> list = new ArrayList<Property[]>(paths.length);
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

    @Override
    public boolean shouldTranslateName ()
    {
        return false;
    }

    @Override
    public String getColorName ()
    {
        Property[] path = _paths[0];
        return path[path.length - 1].getColorName();
    }

    @Override
    public Member getMember ()
    {
        Property[] path = _paths[0];
        return path[path.length - 1].getMember();
    }

    @Override
    public Object getMemberObject (Object object)
    {
        Property[] path = _paths[0];
        int last = path.length - 1;
        for (int ii = 0; ii < last; ii++) {
            object = path[ii].get(object);
        }
        return path[last].getMemberObject(object);
    }

    @Override
    public Class<?> getType ()
    {
        Property[] path = _paths[0];
        return path[path.length - 1].getType();
    }

    @Override
    public Type getGenericType ()
    {
        Property[] path = _paths[0];
        return path[path.length - 1].getGenericType();
    }

    @Override
    public String getMode ()
    {
        return PropertyUtil.getMode(_paths[0]);
    }

    @Override
    public String getUnits ()
    {
        return PropertyUtil.getUnits(_paths[0]);
    }

    @Override
    public double getMinimum ()
    {
        return PropertyUtil.getMinimum(_paths[0]);
    }

    @Override
    public double getMaximum ()
    {
        return PropertyUtil.getMaximum(_paths[0]);
    }

    @Override
    public double getStep ()
    {
        return PropertyUtil.getStep(_paths[0]);
    }

    @Override
    public double getScale ()
    {
        return PropertyUtil.getScale(_paths[0]);
    }

    @Override
    public int getMinSize ()
    {
        return PropertyUtil.getMinSize(_paths[0]);
    }

    @Override
    public int getMaxSize ()
    {
        return PropertyUtil.getMaxSize(_paths[0]);
    }

    @Override
    public Object get (Object object)
    {
        for (Property property : _paths[0]) {
            object = property.get(object);
        }
        return object;
    }

    @Override
    public void set (Object object, Object value)
    {
        for (int ii = 0; ii < _paths.length; ii++) {
            Property[] path = _paths[ii];
            Object obj = object;
            int last = path.length - 1;
            for (int jj = 0; jj < last; jj++) {
                obj = path[jj].get(obj);
            }
            // use some simple coercion rules on the paths after the first
            setProperty(obj, value, path[last], ii == 0);
        }
    }

    /**
     * Sets the property value.
     */
    protected void setProperty (Object obj, Object value, Property prop, boolean coerce)
    {
        prop.set(obj, coerce ? value : coerce(value, prop.getType()));
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
        Class<?> clazz = object.getClass();
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
        if (showGetPropertyWarning) {
            log.warning("Failed to find property.", "name", name, "object", object);
        }
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
            Class<?> refTypeClass = base.getArgumentType(ConfigReference.class);
            if (refTypeClass == null) {
                if (object instanceof DerivedConfig) {
                    refTypeClass = ((DerivedConfig)object).cclass;
                }
                if (refTypeClass == null) {
                    log.warning("Couldn't determine config reference type.", "ref", value);
                    return null;
                }
            }
            final @SuppressWarnings("unchecked") Class<ManagedConfig> clazz =
                    (Class<ManagedConfig>)refTypeClass;
            @SuppressWarnings("unchecked") ConfigReference<ManagedConfig> ref =
                    (ConfigReference<ManagedConfig>)value;
            if (cfgmgr == null) {
                log.warning("No config manager available.", "ref", value);
                return null;
            }
            if (ref == null) {
                return null;
            }
            ManagedConfig config = cfgmgr.getRawConfig(clazz, ref.getName());
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
                public Member getMember () {
                    return aprop.getMember();
                }
                public Object getMemberObject (Object object) {
                    ConfigReference<ManagedConfig> ref = getReference(object);
                    Property prop = getArgumentProperty(ref);
                    return (prop == null) ? null : prop.getMemberObject(ref.getArguments());
                }
                public Class<?> getType () {
                    return aprop.getType();
                }
                public Type getGenericType () {
                    return aprop.getGenericType();
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
                    ManagedConfig config = (ref == null) ?
                        null : cfgmgr.getRawConfig(clazz, ref.getName());
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
     * Coerces the supplied value to the given type.
     */
    protected static Object coerce (Object value, Class<?> type)
    {
        if (type.isPrimitive()) {
            type = ClassUtil.objectEquivalentOf(type);
        }
        if (value == null || type.isInstance(value)) {
            return value;

        } else if (value instanceof String) {
            if (type == Byte.class) {
                try {
                    return Byte.valueOf((String)value);
                } catch (NumberFormatException e) {
                    return Byte.valueOf((byte)0);
                }
            } else if (type == Double.class) {
                try {
                    return Double.valueOf((String)value);
                } catch (NumberFormatException e) {
                    return Double.valueOf(0.0);
                }
            } else if (type == Float.class) {
                try {
                    return Float.valueOf((String)value);
                } catch (NumberFormatException e) {
                    return Float.valueOf(0f);
                }
            } else if (type == Integer.class) {
                try {
                    return Integer.valueOf((String)value);
                } catch (NumberFormatException e) {
                    return Integer.valueOf(0);
                }
            } else if (type == Long.class) {
                try {
                    return Long.valueOf((String)value);
                } catch (NumberFormatException e) {
                    return Long.valueOf(0L);
                }
            } else if (type == Short.class) {
                try {
                    return Short.valueOf((String)value);
                } catch (NumberFormatException e) {
                    return Short.valueOf((short)0);
                }
            }
        } else if (value instanceof Number) {
            if (type == Byte.class) {
                return ((Number)value).byteValue();
            } else if (type == Double.class) {
                return ((Number)value).doubleValue();
            } else if (type == Float.class) {
                return ((Number)value).floatValue();
            } else if (type == Integer.class) {
                return ((Number)value).intValue();
            } else if (type == Long.class) {
                return ((Number)value).longValue();
            } else if (type == Short.class) {
                return ((Number)value).shortValue();
            } else if (type == String.class) {
                return value.toString();
            }
        }
        throw new IllegalArgumentException("Can't coerce " + value + " to " + type);
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

        @Override
        public boolean shouldTranslateName ()
        {
            return false;
        }

        @Override
        public Member getMember ()
        {
            return _base.getMember();
        }

        @Override
        public Class<?> getType ()
        {
            return _base.getComponentType();
        }

        @Override
        public Type getGenericType ()
        {
            return _base.getGenericComponentType();
        }

        /** The base property. */
        protected Property _base;
    }

    /** The property chains for each path. */
    protected Property[][] _paths;
}
