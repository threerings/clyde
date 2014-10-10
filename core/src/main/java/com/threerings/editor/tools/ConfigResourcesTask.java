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

package com.threerings.editor.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.List;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.PatternSet;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ManagedConfig;
import com.threerings.editor.util.PropertyUtil;

import static com.threerings.editor.Log.log;

/**
 * An Ant task that gathers all resources referenced by a set of configs and puts them in
 * a pattern set.
 */
public class ConfigResourcesTask extends Task
{
    /**
     * Sets the pattern set id.
     */
    public void setId (String id)
    {
        _id = id;
    }

    /**
     * Sets the file from which to read the config names.
     */
    public void setConfigfile (File file)
    {
        _configfile = file;
    }

    /**
     * Sets the file from which to read the resource names.
     */
    public void setResourcefile (File file)
    {
        _resourcefile = file;
    }

    @Override
    public void execute ()
        throws BuildException
    {
        ResourceManager rsrcmgr = new ResourceManager("rsrc/");
        MessageManager msgmgr = new MessageManager("rsrc.i18n");
        rsrcmgr.initResourceDir("rsrc/");
        ConfigManager cfgmgr = new ConfigManager(rsrcmgr, msgmgr, "config/");
        cfgmgr.init();

        // read the file containing the config names
        Set<String> resources = Sets.newHashSet();
        try {
            BufferedReader in = new BufferedReader(new FileReader(_configfile));
            String line;
            ConfigGroup<?> group = null;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) { // ignore blank lines
                    continue;
                }
                if (line.startsWith("[") && line.endsWith("]")) { // group header
                    String name = line.substring(1, line.length() - 1);
                    if ((group = cfgmgr.getGroup(name)) == null) {
                        log.warning("Invalid config group name.", "name", name);
                    }
                    continue;
                }
                if (group != null) {
                    getResources(cfgmgr, group, line, resources);
                }
            }
        } catch (IOException e) {
            log.warning("Error reading config list.", "file", _configfile, e);
        }

        // read the file containing the resource names
        List<String> includes = Lists.newArrayList();
        try {
            BufferedReader in = new BufferedReader(new FileReader(_resourcefile));
            String line;
            while ((line = in.readLine()) != null) {
                if (!(line = line.trim()).isEmpty()) {
                    includes.add(line);
                }
            }
        } catch (IOException e) {
            log.warning("Failed to read resource list.", "file", _resourcefile, e);
        }

        // scan the identified resources
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir("rsrc");
        scanner.setIncludes(includes.toArray(new String[includes.size()]));
        scanner.scan();
        for (String file : scanner.getIncludedFiles()) {
            if (!file.endsWith(".dat")) {
                continue;
            }
            ManagedConfig config = cfgmgr.getResourceConfig(file);
            PropertyUtil.getResources(config.getConfigManager(), config, resources);
        }

        // create a pattern set with the resources and assign it to the specified id
        PatternSet set = (PatternSet)getProject().createDataType("patternset");
        for (String resource : resources) {
            set.createInclude().setName(resource);
        }
        getProject().addReference(_id, set);
    }

    /**
     * Attempts to resolve the given config line and add the referenced resources to the set.
     */
    protected void getResources (
        ConfigManager cfgmgr, ConfigGroup<?> group, String line, Set<String> paths)
    {
        // TODO: handle wildcards?
        PropertyUtil.getResources(cfgmgr, group.getConfig(line), paths);
    }

    /** The id under which we'll store the resource pattern set. */
    protected String _id;

    /** The file from which we read the configs. */
    protected File _configfile;

    /** The file from which we read the resources. */
    protected File _resourcefile;
}
