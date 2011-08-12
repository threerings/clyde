//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2011 Three Rings Design, Inc.
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

import java.io.File;

import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.google.common.collect.Lists;

import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;

import com.threerings.config.ConfigManager;
import com.threerings.config.ManagedConfig;

/**
 * Validates the references in a set of configs.
 */
public class ConfigValidatorTask extends Task
{
    /**
     * Adds a fileset to the list of sets to process.
     */
    public void addFileset (FileSet set)
    {
        _filesets.add(set);
    }

    @Override // documentation inherited
    public void execute ()
        throws BuildException
    {
        boolean valid = true;
        ResourceManager rsrcmgr = new ResourceManager("rsrc/");
        MessageManager msgmgr = new MessageManager("rsrc.i18n");
        rsrcmgr.initResourceDir("rsrc/");
        ConfigManager cfgmgr = new ConfigManager(rsrcmgr, msgmgr, "config/");
        cfgmgr.init();

        // validate the base configs
        valid = cfgmgr.validateReferences("", System.err) && valid;

        // validate the resource configs
        for (FileSet fs : _filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File fromDir = fs.getDir(getProject());
            for (String file : ds.getIncludedFiles()) {
                File source = new File(fromDir, file);
                String path = rsrcmgr.getResourcePath(source);
                ManagedConfig config = (path == null) ? null : cfgmgr.getResourceConfig(path);
                if (config != null) {
                    try {
                        valid = config.validateReferences(path, System.err) && valid;
                    } catch (Exception e) {
                        throw new BuildException("Error validating '" + path + "': " + e);
                    }
                }
            }
        }

        if (!valid) {
            throw new BuildException();
        }
    }

    /** A list of filesets that contain resource configs. */
    protected List<FileSet> _filesets = Lists.newArrayList();
}
