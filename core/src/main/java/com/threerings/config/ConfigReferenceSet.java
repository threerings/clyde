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

package com.threerings.config;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

/**
 * This class will be deprecated and removed. It's not deprecated now because that causes a lot
 * of annoying warnings merely when we import it.
 *
 * Old documentation follows:
 *
 * A set of config references of different types.
 * <em>Old and outdated</em>.
 *
 * NOTE: <em>Problem!</em> This is incapable of correctly collecting ConfigReferences because the
 * arguments of a ConfigReference may contain more ConfigReferences, but those will have
 * their Class lost due to type erasure. We've coped with this deficiency in the past but
 * if we wanted to correct it..
 * <ul>
 *   <li> We could have a new subclass of ConfigReference that contains the Class, and have
 *        configs and new code use that...</li>
 *   <li> If we have access to the ConfigManager we can look up the Properties associated with
 *        each parameter argument and determine the generic type of the ConfigReference. This
 *        is done by the subclass in ConfigFlattener.</li>
 * </ul>
 */
//@Deprecated // commented out because the warnings were too annoying
public abstract class ConfigReferenceSet
{
    /**
     * Adds a reference to the set.
     */
    public abstract <T extends ManagedConfig> boolean add (
            Class<T> clazz, @Nullable ConfigReference<T> ref);

    /**
     * Adds a reference to the set, by name only.
     */
    public <T extends ManagedConfig> boolean add (Class<T> clazz, @Nullable String name)
    {
        return name != null && add(clazz, new ConfigReference<T>(name));
    }

//    /**
//     * Adds a list of references to the set, by name only.
//     */
//    public <T extends ManagedConfig> boolean addAllNames (
//            Class<T> clazz, @Nullable List<String> names)
//    {
//        if (names == null) {
//            return false;
//        }
//        boolean anyAdded = false;
//        for (String name : names) {
//            anyAdded |= add(clazz, name);
//        }
//        return anyAdded;
//    }

    /**
     * Convenience method for adding an entire list of refs.
     */
    public <T extends ManagedConfig> boolean addAll (
            Class<T> clazz, @Nullable List<ConfigReference<T>> list)
    {
        if (list == null) {
            return false;
        }
        boolean anyAdded = false;
        for (ConfigReference<T> ref : list) {
            anyAdded |= add(clazz, ref);
        }
        return anyAdded;
    }

    /**
     * Convenience method for adding an entire array of refs.
     */
    public <T extends ManagedConfig> boolean addAll (
            Class<T> clazz, @Nullable ConfigReference<T>[] array)
    {
        return (array != null) && addAll(clazz, Arrays.asList(array));
    }

    /**
     * A default implementation.
     */
    public static class Default extends ConfigReferenceSet
    {
        /**
         * Get the gathered config references.
         */
        public SetMultimap<Class<? extends ManagedConfig>, ConfigReference<?>> getGathered ()
        {
            return _refs;
        }

        @Override
        public <T extends ManagedConfig> boolean add (
                Class<T> clazz, @Nullable ConfigReference<T> ref)
        {
            return (ref != null) && _refs.put(clazz, ref);
        }

        /** The gathered references. */
        protected SetMultimap<Class<? extends ManagedConfig>, ConfigReference<?>> _refs =
                HashMultimap.create();
    }
}
