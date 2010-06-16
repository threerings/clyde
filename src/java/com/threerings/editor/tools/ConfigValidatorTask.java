//
// $Id$

package com.threerings.editor.tools;

import java.io.File;

import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.google.common.collect.Lists;

import com.threerings.resource.ResourceManager;

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
        ResourceManager rsrcmgr = new ResourceManager("rsrc/");
        rsrcmgr.initResourceDir("rsrc/");
        ConfigManager cfgmgr = new ConfigManager(rsrcmgr, "config/");
        cfgmgr.init();

        // validate the base configs
        cfgmgr.validateReferences("", System.err);

        // validate the resource configs
        for (FileSet fs : _filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File fromDir = fs.getDir(getProject());
            for (String file : ds.getIncludedFiles()) {
                File source = new File(fromDir, file);
                String path = rsrcmgr.getResourcePath(source);
                ManagedConfig config = (path == null) ? null : cfgmgr.getResourceConfig(path);
                if (config != null) {
                    config.validateReferences(path, System.err);
                }
            }
        }
    }

    /** A list of filesets that contain resource configs. */
    protected List<FileSet> _filesets = Lists.newArrayList();
}
