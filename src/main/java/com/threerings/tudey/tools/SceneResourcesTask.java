//
// $Id$

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

import com.threerings.config.ConfigManager;
import com.threerings.export.BinaryImporter;

import com.threerings.tudey.data.TudeySceneModel;

import static com.threerings.tudey.Log.*;

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

    @Override // documentation inherited
    public void execute ()
        throws BuildException
    {
        ResourceManager rsrcmgr = new ResourceManager("rsrc/");
        rsrcmgr.initResourceDir("rsrc/");
        ConfigManager cfgmgr = new ConfigManager(rsrcmgr, "config/");
        cfgmgr.init();

        // get all scene resources
        Set<String> resources = Sets.newHashSet();
        String baseDir = getProject().getBaseDir().getPath();
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
