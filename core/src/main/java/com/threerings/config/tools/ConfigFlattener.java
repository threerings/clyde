//
// $Id$

package com.threerings.config.tools;

import java.io.IOException;
import java.io.File;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import com.threerings.util.MessageManager;

import com.threerings.resource.ResourceManager;

import com.threerings.config.ArgumentMap;
import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ManagedConfig;
import com.threerings.config.Parameter;
import com.threerings.config.ParameterizedConfig;

import static com.threerings.ClydeLog.log;

/**
 */
public class ConfigFlattener
{
    /**
     * Tool entry point.
     */
    public static void main (String[] args)
        throws IOException
    {
        String rsrcDir;
        String outDir;
        switch (args.length) {
        default:
            System.err.println("Args: <rsrcdir> <outconfigdir>");
            System.exit(1);
            return;

        case 2:
            rsrcDir = args[0];
            outDir = args[1];
            break;
        }

        ResourceManager rsrcmgr = new ResourceManager(rsrcDir);
        File configDir = rsrcmgr.getResourceFile("config/");

        Preconditions.checkArgument(configDir.isDirectory(), "%s isn't a directory", configDir);
        Preconditions.checkArgument(
                new File(configDir, "manager.properties").exists() ||
                new File(configDir, "manager.txt").exists(), "cannot find manager descriptor");
        File destDir = rsrcmgr.getResourceFile(outDir);
        Preconditions.checkArgument(destDir.isDirectory(), "%s isn't a directory", destDir);


        MessageManager msgmgr = new MessageManager("rsrc.i18n");
        RePathableConfigManager cfgmgr = new RePathableConfigManager(rsrcmgr, msgmgr, "config/");
        cfgmgr.init();
        flatten(cfgmgr);

        // Save everything!
        cfgmgr.resetConfigPath(outDir);
        cfgmgr.saveAll();
    }

    public static void flatten (ConfigManager cfgmgr)
        throws IOException
    {
        // Map all referenced configs by type, then by name, and then have all the instances listed.
        // Note that we do not want to collate .equals() references, because we are going to
        // modify each config reference in-place!
        final Map<Class<?>, ListMultimap<String, ConfigReference<?>>> refs = Maps.newHashMap();
        gatherConfigs(cfgmgr,
            new ConfigReferenceSet() {
                @Override
                public <T extends ManagedConfig> boolean add (Class<T> clazz, String name)
                {
                    // Anything added with a name only *has no parameters*, and
                    // therefore we don't care about it.
                    return false;
                }

                @Override
                public <T extends ManagedConfig> boolean add (
                        Class<T> clazz, ConfigReference<T> ref)
                {
                    // omit config refs with no args: we don't care
                    if (ref == null || ref.getArguments().isEmpty()) {
                        return false;
                    }
                    ListMultimap<String, ConfigReference<?>> multimap = refs.get(clazz);
                    if (multimap == null) {
                        multimap = ArrayListMultimap.create();
                        refs.put(clazz, multimap);
                    }
                    return multimap.put(ref.getName(), ref);
                }
            });

//        // what have we got?
//        System.err.println("All refs: " + refs);

        // now let's go through and rewrite these configs
        for (Map.Entry<Class<?>, ListMultimap<String, ConfigReference<?>>> entry :
                refs.entrySet()) {
            @SuppressWarnings("unchecked")
            Class<ManagedConfig> mclass = (Class<ManagedConfig>)entry.getKey();
            System.err.println("Now looking at class: " + mclass);
            ConfigGroup<ManagedConfig> group = cfgmgr.getGroup(mclass);
            if (group == null) {
                log.warning("Couldn't find group, skipping.", "mclass", mclass);
                continue;
            }
            for (Map.Entry<String, List<ConfigReference<?>>> centry :
                    Multimaps.asMap(entry.getValue()).entrySet()) {
                String refName = centry.getKey();
                // let's look up that config and make a Set of the valid parameter names
                ManagedConfig cfg = group.getConfig(refName);
                Set<String> paramNames;
                Map<ArgumentMap, String> newNames;
                if (cfg instanceof ParameterizedConfig) {
                    paramNames = Sets.newHashSet();
                    for (Parameter p : ((ParameterizedConfig)cfg).parameters) {
                        paramNames.add(p.name);
                    }
                    newNames = Maps.newHashMap();

                } else {
                    paramNames = ImmutableSet.of();
                    newNames = null; // won't get used- just you watch and see
                }

                // jog through our refs and twiddle them
                for (ConfigReference<?> ref : centry.getValue()) {
                    ArgumentMap args = ref.getArguments();
                    args.keySet().retainAll(paramNames);
                    if (args.isEmpty()) {
                        // we're done with this one!
                        continue;
                    }
                    // otherwise, map it to a new config with the args baked-in
                    String newName = newNames.get(args);
                    if (newName == null) {
                        ArgumentMap key = args.clone();
                        int code = key.hashCode();
                        newName = Integer.toHexString(code);
                        // avoid hash collisions
                        while (newNames.containsValue(newName)) {
                            newName = Integer.toHexString(++code);
                        }
                        // create the new config
                        ManagedConfig newCfg = cfg.getInstance(key);
                        newCfg.setName(refName + "~" + newName);
                        group.addConfig(newCfg);
                        newNames.put(key, newName);
                    }

                    // now, copy a new config reference with the new name to the existing ref
                    new ConfigReference<ManagedConfig>(refName + "~" + newName).copy(ref);
                }
            }

            // after we're done, we want to blank out all parameters in this group
            for (ParameterizedConfig cfg :
                    Iterables.filter(group.getConfigs(), ParameterizedConfig.class)) {
                cfg.parameters = Parameter.EMPTY_ARRAY;
            }
        }
    }

    /**
     * Gather up every config into the specified reference set.
     */
    protected static void gatherConfigs (ConfigManager cfgmgr, ConfigReferenceSet refSet)
    {
        try {
            // this fucking method is protected... Consider making it public or moving
            // the code that accesses it into the /config/ package.
            Method method = ManagedConfig.class.getDeclaredMethod(
                    "getUpdateReferences", ConfigReferenceSet.class);
            method.setAccessible(true);
            Object[] args = new Object[] { refSet };

            // gather up all the referenced configs
            for (ConfigGroup<?> group : cfgmgr.getGroups()) {
                for (ManagedConfig cfg : group.getConfigs()) {
                    try {
                        method.invoke(cfg, args);
                    } catch (RuntimeException e) {
                        log.warning("Exception gathering configs", "group", group, "cfg", cfg, e);
                    }
                }
            }
        } catch (NoSuchMethodException nsme) {
            log.warning("Did getUpdateReferences() change?", nsme);

        } catch (IllegalAccessException iae) {
            log.warning("Unable to call getUpdateReferences()?", iae);

        } catch (InvocationTargetException ite) {
            log.warning("Unable to call getUpdateReferences()?", ite);
        }
    }

    /**
     * A ResourceManager that can load from one spot and save to another.
     */
    protected static class RePathableConfigManager extends ConfigManager
    {
        public RePathableConfigManager (ResourceManager rsrcmgr, MessageManager msgmgr, String path)
        {
            super(rsrcmgr, msgmgr, path);
        }

        public void resetConfigPath (String configPath)
        {
            _configPath = configPath;
            if (!configPath.endsWith("/")) {
                _configPath += "/";
            }
        }
    }
}
