//
// $Id$

package com.threerings.config;

import java.lang.ref.SoftReference;

import java.util.List;

import com.google.common.collect.Lists;

import com.threerings.editor.Editable;
import com.threerings.expr.Scope;

import com.threerings.util.DeepOmit;
import com.threerings.util.Shallow;

/**
 * Represents a config that is directly derived from another config.
 */
public final class DerivedConfig extends ParameterizedConfig
{
    /** The other configuration from which we derive. */
    @Editable(editor="derivedRef", nullable=true)
    public ConfigReference<? extends ManagedConfig> base;

    /** The config class of the base, that we are actually deriving from. */
    @Shallow
    public transient Class<? extends ManagedConfig> cclass;

    @Override
    public void configUpdated (ConfigEvent<ManagedConfig> event)
    {
        // instead of calling super, we call wasUpdated, which will more broadly update us
        // and will end up calling fireConfigUpdated() anyway, which is what super does.
        wasUpdated();
    }

    @Override
    public void wasUpdated ()
    {
        // release our derivation and clear the hard reference to the source
        _derivation = NO_DERIVATION;
        _source = null;

        super.wasUpdated();
    }

    @Override
    public boolean isInvalidParameterPath (String path)
    {
        // do not let 'base' be parameterized!
        return super.isInvalidParameterPath(path) || path.equals("base");
    }

    @Override
    protected ManagedConfig getBound (Scope scope)
    {
        // A derived config can never itself be a bound config, because we are final.
        // We use this method to actualize the config.
        assert(java.lang.reflect.Modifier.isFinal(getClass().getModifiers()));

        ManagedConfig instance = _derivation.get();
        if (instance != null) {
            return instance;
        }

        // if we have no base then jump ship now
        if (base == null) {
            return null;
        }

        // loose types slip lines
        @SuppressWarnings("unchecked")
        ConfigReference<ManagedConfig> ref = (ConfigReference<ManagedConfig>)base;
        @SuppressWarnings("unchecked")
        Class<ManagedConfig> clazz = (Class<ManagedConfig>)cclass;

        // keep a hard reference to our source
        _source = _cfgmgr.getConfig(clazz, ref, scope);
        if (_source == null) {
            return null;
        }

        // instance must be a clone so that we can change the name
        instance = (ManagedConfig)_source.clone();
        instance.init(_cfgmgr);
        instance.setName(getName());
        instance.setComment(getComment());
        if (instance instanceof ParameterizedConfig) {
            ParameterizedConfig instanceP = (ParameterizedConfig)instance;
            // have the instance keep a hard reference to us, after all we could be held softly
            // by the real DerivedConfig if we are a clone based on parameters...!
            instanceP._base = this;
            instanceP._args = this._args;
            translateParameters(instanceP);
        }
        // the instance should listen for changes from us
        addListener(instance);

        // finally, keep a soft reference to it and return it
        _derivation = new SoftReference<ManagedConfig>(instance);
        return instance;
    }

    /**
     * Translate OUR parameters so that the paths point to their new locations on the instance.
     */
    protected void translateParameters (ParameterizedConfig instance)
    {
        // translate our parameter paths...
        List<Parameter> params = Lists.newArrayList();
        for (Parameter p : parameters) {
            // TODO: handle Choice
            // TODO: argggg lots of edge cases here, TODO
            if (p instanceof Parameter.Direct) {
                Parameter.Direct pDirect = (Parameter.Direct)p;
                for (String path : pDirect.paths) {
                    if (isInvalidParameterPath(path)) {
                        continue; // skip it for now, but this will fail validation
                    }
                    if (!path.startsWith("base[\"") || !path.endsWith("\"]")) {
                        throw new RuntimeException("What the hell?");
                    }
                    String name = path.substring(6, path.length() - 2); // get just the arg part
                    // find the parameter on actual with that path
                    Parameter actualParam = ParameterizedConfig.getParameter(
                            instance.parameters, name);
                    if (actualParam != null) {
                        actualParam.name = p.name;
                        params.add(actualParam);
                    }
                }
            } else {
                throw new RuntimeException("TODO: handle translating " + p.getClass());
            }
        }
        instance.parameters = params.toArray(Parameter.EMPTY_ARRAY);
    }

    @Override
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        super.getUpdateReferences(refs);

        @SuppressWarnings("unchecked")
        ConfigReference<ManagedConfig> ref = (ConfigReference<ManagedConfig>)base;
        @SuppressWarnings("unchecked")
        Class<ManagedConfig> clazz = (Class<ManagedConfig>)cclass;
        refs.add(clazz, ref);
    }

    /** A hard reference to the instance of the config from which we create our derivation. */
    @DeepOmit
    protected transient ManagedConfig _source;

    /** The derivation we create. */
    @DeepOmit
    protected transient SoftReference<ManagedConfig> _derivation = NO_DERIVATION;

    /** A sharable empty reference. */
    protected static final SoftReference<ManagedConfig> NO_DERIVATION =
            new SoftReference<ManagedConfig>(null);
}
