//
// $Id$

package com.threerings.tudey.tools;

import java.io.File;
import java.io.FileInputStream;

import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.google.common.collect.Lists;

import com.threerings.resource.ResourceManager;

import com.threerings.config.ConfigManager;
import com.threerings.export.BinaryImporter;

import com.threerings.tudey.data.TudeySceneModel;

import static com.threerings.tudey.Log.*;

/**
 * Validates the references in a set of scenes.
 */
public class SceneValidatorTask extends Task
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

        for (FileSet fs : _filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File fromDir = fs.getDir(getProject());
            for (String file : ds.getIncludedFiles()) {
                File source = new File(fromDir, file);
                try {
                    TudeySceneModel model = (TudeySceneModel)new BinaryImporter(
                        new FileInputStream(source)).readObject();

                } catch (Exception e) { // IOException, ClassCastException
                    log.warning("Failed to read scene.", "file", source, e);
                }
            }
        }
    }

    /** A list of filesets that contain resource configs. */
    protected List<FileSet> _filesets = Lists.newArrayList();
}
