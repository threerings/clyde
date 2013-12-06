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

package com.threerings.tudey.tools;

import java.io.File;
import java.io.FileInputStream;

import java.util.List;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;

import com.threerings.config.ConfigManager;
import com.threerings.export.BinaryImporter;

import com.threerings.tudey.data.TudeySceneModel;

import static com.threerings.tudey.Log.log;

/**
 * An Ant task that gathers all resources referenced by a group of scenes and puts them in
 * a pattern set.
 */
public class SceneResourcesTask extends Task
{
    /**
     * Sets the pattern set id.
     */
    public void setId (String id)
    {
        _id = id;
    }

    /**
     * Adds a fileset to the list of sets to process.
     */
    public void addFileset (FileSet set)
    {
        _filesets.add(set);
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

        // get all scene resources
        Set<String> resources = Sets.newHashSet();
        for (FileSet fs : _filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File fromDir = fs.getDir(getProject());
            for (String file : ds.getIncludedFiles()) {
                File source = new File(fromDir, file);
                try {
                    TudeySceneModel model = (TudeySceneModel)new BinaryImporter(
                        new FileInputStream(source)).readObject();
                    model.init(cfgmgr);
                    model.getResources(resources);

                } catch (Exception e) { // IOException, ClassCastException
                    log.warning("Failed to read scene.", "file", source, e);
                }
            }
        }

        // create a pattern set with the resources and assign it to the specified id
        PatternSet set = (PatternSet)getProject().createDataType("patternset");
        for (String resource : resources) {
            set.createInclude().setName(resource);
        }
        getProject().addReference(_id, set);
    }

    /** The id under which we'll store the resource pattern set. */
    protected String _id;

    /** A list of filesets that contain scenes. */
    protected List<FileSet> _filesets = Lists.newArrayList();
}
