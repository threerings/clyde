//
// $Id$

package com.threerings.config;

import java.lang.ref.SoftReference;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.samskivert.util.ArrayUtil;

import com.threerings.editor.Editable;
import com.threerings.expr.Scope;

import com.threerings.util.DeepOmit;
import com.threerings.util.Shallow;

/**
 * Represents a config that is directly derived from another config.
 *
 * This class should not typically be subclassed.
 */
public class DerivedConfig extends ParameterizedConfig
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
        // A derived config can never itself be a bound config, but the configs we derive from
        // can be.
        // We use this method to actualize the config.
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
        initializeInstance(instance);
        // the instance should listen for changes from us
        addListener(instance);

        // finally, keep a soft reference to it and return it
        _derivation = new SoftReference<ManagedConfig>(instance);
        return instance;
    }

    /**
     * Initialize the derivated instance that we've created.
     */
    protected void initializeInstance (ManagedConfig instance)
    {
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
    }

    /**
     * Translate OUR parameters so that the paths point to their new locations on the instance.
     */
    protected void translateParameters (ParameterizedConfig instance)
    {
        // translate our parameter paths...
        List<Parameter> newParams = Lists.newArrayList();
        for (Parameter p : parameters) {
            if (p instanceof Parameter.Direct) {
                newParams.add(translateDirectParameter(instance, (Parameter.Direct)p));

            } else if (p instanceof Parameter.Choice) {
                Parameter.Choice pChoice = (Parameter.Choice)p;
                List<Parameter.Direct> newDirects = Lists.newArrayList();
                for (Parameter.Direct direct : pChoice.directs) {
                    newDirects.add(translateDirectParameter(instance, direct));
                }
                Parameter.Choice newChoice = (Parameter.Choice)pChoice.clone();
                newChoice.directs = newDirects.toArray(Parameter.Direct.EMPTY_ARRAY);
                newChoice.setOuter(instance);
                newParams.add(newChoice);
            }
        }
        instance.parameters = newParams.toArray(Parameter.EMPTY_ARRAY);
    }

    /**
     * Return a clone of the specified Direct parameter, translated such that its paths
     * refer now to the derived instance.
     */
    protected Parameter.Direct translateDirectParameter (
            ParameterizedConfig instance, Parameter.Direct ourParam)
    {
        // Since we can't do all of it correctly, do none of it correctly.
        Parameter.DerivedConfigParameter newParam = new Parameter.DerivedConfigParameter();
        newParam.name = ourParam.name;
        return newParam;
//
//        Set<String> newPaths = Sets.newLinkedHashSet(); // remove dupes, but preserve order
//        for (String path : ourParam.paths) {
//            if (isInvalidParameterPath(path) ||
//                    !path.startsWith("base[\"") || !path.endsWith("\"]")) {
//                continue; // skip it for now, but it will fail validation
//            }
//            String name = path.substring(6, path.length() - 2); // get just the arg part
//            // find the parameter on actual with that path
//            Parameter instanceParam = ParameterizedConfig.getParameter(instance.parameters, name);
//            if (instanceParam instanceof Parameter.Direct) {
//                newPaths.addAll(Arrays.asList(((Parameter.Direct)instanceParam).paths));
//                // also, per the note below, we treat Translated parameters as normal directs.
//
//            } else if (instanceParam instanceof Parameter.Choice) {
//                // We cannot translate a choice parameter (unless it's very simple)
//                // because a choice bundles together multiple settings into one. A direct parameter
//                // can only apply one value to a series of properties.
//                // - Just clear out the path.
//                //
//                // This can be revisited... other options are:
//                // - add a comment field to parameters and note the lost path there.
//                // - make paths able to hold comments...
//                // - create a new type of parameter that can do what we need.
//
//                // (suppress adding this path)
//            }
//        }
//        Parameter.Direct newParam = (Parameter.Direct)ourParam.clone();
//        newParam.paths = newPaths.toArray(ArrayUtil.EMPTY_STRING);
//        return newParam;
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
