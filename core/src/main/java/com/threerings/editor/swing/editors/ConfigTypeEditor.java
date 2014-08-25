//
// $Id$

package com.threerings.editor.swing.editors;

import java.lang.reflect.Member;
import java.lang.reflect.Type;

import com.threerings.util.DeepUtil;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.DerivedConfig;
import com.threerings.config.ManagedConfig;

import com.threerings.editor.EditorTypes;
import com.threerings.editor.MethodProperty;
import com.threerings.editor.Property;

import static com.threerings.editor.Log.log;

public class ConfigTypeEditor extends ChoiceEditor
{
    @Override
    public void update ()
    {
        super.update();
        // TODO: hide if only one option
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
        Class<?> clazz = (_object instanceof DerivedConfig)
            ? ((DerivedConfig)_object).cclass
            : _object.getClass();

        for (Class<?> c = clazz; c != ManagedConfig.class; c = c.getSuperclass()) {
            EditorTypes anno = c.getAnnotation(EditorTypes.class);
            if (anno != null) {
                Class<?>[] classes = anno.value();
                // box up these types so that they have nice names...
                ClassBox[] boxes = new ClassBox[classes.length];
                for (int ii = 0; ii < classes.length; ii++) {
                    boxes[ii] = new ClassBox(classes[ii]);
                }
                return boxes;
            }
        }
        // if we never found it, then only the base type is used
        return new ClassBox[] { new ClassBox(clazz) };
    }

    /**
     * Utility to get the group for any config.
     */
    protected static ConfigGroup<? extends ManagedConfig> getGroup (ManagedConfig cfg)
    {
        ConfigManager cfgmgr = cfg.getConfigManager();
        Class<?> clazz = (cfg instanceof DerivedConfig)
            ? ((DerivedConfig)cfg).cclass
            : cfg.getClass();
        for (Class<?> c = clazz; c != ManagedConfig.class; c = c.getSuperclass()) {
            @SuppressWarnings("unchecked")
            Class<ManagedConfig> cm = (Class<ManagedConfig>)c;
            ConfigGroup<? extends ManagedConfig> group = cfgmgr.getGroup(cm);
            if (group != null) {
                return group;
            }
        }
        return null;
    }

    protected static class TypeProperty extends Property
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
            return new ClassBox(object.getClass());
        }

        @Override
        public void set (Object object, Object value)
        {
            Class<?> clazz = ((ClassBox)value).clazz;
            if (object.getClass() == clazz) {
                return;
            }

            ManagedConfig oldCfg = (ManagedConfig)object;
            ManagedConfig newCfg;
            try {
                newCfg = (ManagedConfig)clazz.newInstance();
            } catch (Exception e) {
                log.warning("Failed to change type", e);
                return;
            }
            DeepUtil.transfer(oldCfg, newCfg);
            newCfg.setComment(oldCfg.getComment());
            getGroup(oldCfg).addConfig(newCfg);
        }

        protected MethodProperty _base;
    }

    /**
     * Wraps a class instance with a nicer name.
     */
    protected static class ClassBox
    {
        public final Class<?> clazz;

        public ClassBox (Class<?> clazz)
        {
            this.clazz = clazz;
        }

        @Override
        public String toString ()
        {
            return ConfigGroup.getName(clazz);
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
}
