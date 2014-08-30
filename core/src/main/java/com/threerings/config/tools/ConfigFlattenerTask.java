//
// $Id$

package com.threerings.config.tools;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * An ant task to flatten configs.
 */
public class ConfigFlattenerTask extends Task
{
    /**
     * Set the resources directory, which is the parent of the source config  directory.
     */
    public void setResourceDirectory (String dir)
    {
        _rsrcDir = dir;
    }

    /**
     * Set the output directory.
     */
    public void setOutputDirectory (String dir)
    {
        _outDir = dir;
    }

    /**
     * Set the output extension.
     * Default is ".xml".
     */
    public void setExtension (String ext)
    {
        _ext = ext;
    }

    @Override
    public void execute ()
        throws BuildException
    {
        ConfigFlattener.flatten(_rsrcDir, _outDir, _ext, ".xml".equalsIgnoreCase(_ext));
    }

    /** The source resource directory. */
    protected String _rsrcDir;

    /** The directory to output to. */
    protected String _outDir;

    /** The output extension. */
    protected String _ext = ".xml";
}
