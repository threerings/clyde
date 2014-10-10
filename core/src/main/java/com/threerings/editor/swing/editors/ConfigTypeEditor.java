//
// $Id$

package com.threerings.editor.swing.editors;

import java.lang.reflect.Member;
import java.lang.reflect.Type;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import com.samskivert.util.ArrayUtil;

import com.threerings.util.DeepUtil;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ManagedConfig;
import com.threerings.config.tools.ConfigEditor;

import com.threerings.editor.MethodProperty;
import com.threerings.editor.Property;
import com.threerings.editor.util.PropertyUtil;

import static com.threerings.editor.Log.log;

/**
 * A special editor for selecting the type of a config that might have subtypes.
 */
public class ConfigTypeEditor extends ChoiceEditor
{
    @Override
    public void update ()
    {
        super.update();
        setVisible(_box.getItemCount() > 1);
    }

    @Override
    protected void didInit ()
    {
        // replace our property...
        _property = _lineage[_lineage.length - 1] = new TypeProperty((MethodProperty)_property);
        super.didInit();
    }

    @Override
    public Object[] getOptions ()
    {
        ConfigGroup<?> group = ((ManagedConfig)_object).getConfigGroup();
        if (group == null) {
            return ArrayUtil.EMPTY_OBJECT;
        }
        @SuppressWarnings("unchecked") // compiler bug???
        List<Class<?>> classes = group.getRawConfigClasses();
        ClassBox[] boxes = new ClassBox[classes.size()];
        for (int ii = 0, nn = classes.size(); ii < nn; ii++) {
            boxes[ii] = new ClassBox(classes.get(ii), getLabel(classes.get(ii)));
        }
        return boxes;
    }

    @Override
    protected void fireStateChanged ()
    {
        // Suppress. If our state has changed that means we've selected a new type
        // and so the object we were editing disappears.
    }

    /**
     * Transfer the specified config to a new config with the desired class.
     */
    protected static ManagedConfig transfer (ManagedConfig source, Class<?> destClass)
    {
        String cfgName = source.getName();
        if (!cfgName.equals(_cachedName)) {
            _cachedName = cfgName;
            _cachedInstances.clear();
        }

        _cachedInstances.put(source.getClass(), source);
        ManagedConfig dest = _cachedInstances.get(destClass);
        if (dest == null) {
            try {
                dest = (ManagedConfig)destClass.newInstance();
            } catch (Exception e) {
                log.warning("Failed to change type", e);
                return null;
            }
            _cachedInstances.put(destClass, dest);
        }

        PropertyUtil.transferCompatibleProperties(source, dest);
        dest.setName(cfgName);
        return dest;
    }

    /**
     * A special property for getting/setting the type.
     */
    protected class TypeProperty extends Property
    {
        public TypeProperty (MethodProperty prop)
        {
            _base = prop;
            _name = prop.getName();
        }

        @Override
        public Member getMember ()
        {
            return _base.getMember();
        }

        @Override
        public Class<?> getType ()
        {
            return Class.class;
        }

        @Override
        public Type getGenericType ()
        {
            return getType();
        }

        @Override
        public Object get (Object object)
        {
            return new ClassBox(object.getClass(), getLabel(object.getClass()));
        }

        @Override
        public void set (Object object, Object value)
        {
            Class<?> clazz = ((ClassBox)value).clazz;
            if (object.getClass() == clazz) {
                return;
            }

            ManagedConfig oldCfg = (ManagedConfig)object;
            ManagedConfig newCfg = transfer(oldCfg, clazz);
            oldCfg.getConfigGroup().addConfig(newCfg);
        }

        /** Our base property, for accessing the annotation. */
        protected MethodProperty _base;
    }

    /**
     * Wraps a class instance with a nicer name.
     */
    protected static class ClassBox
    {
        /** The class we're representing. */
        public final Class<?> clazz;

        /** The label for this class. */
        public final String label;

        public ClassBox (Class<?> clazz, String label)
        {
            this.clazz = clazz;
            this.label = label;
        }

        @Override
        public String toString ()
        {
            return label;
        }

        @Override
        public boolean equals (Object other)
        {
            return (other instanceof ClassBox) && (clazz == ((ClassBox)other).clazz);
        }

        @Override
        public int hashCode ()
        {
            return clazz.hashCode();
        }
    }

    /** The name of the last config that was having its type edited. */
    protected static String _cachedName;

    /** A cache of configs, by type, that have been created with the specified name. */
    protected static Map<Class<?>, ManagedConfig> _cachedInstances = Maps.newHashMap();
}
