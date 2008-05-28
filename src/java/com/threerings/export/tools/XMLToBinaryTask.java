//
// $Id$

package com.threerings.export.tools;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * Converts XML export files into binary export files.
 */
public class XMLToBinaryTask extends Task
{
    /**
     * Sets the destination directory to which generated files will be written.
     */
    public void setDest (File dest)
    {
        _dest = dest;
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
        String baseDir = getProject().getBaseDir().getPath();
        for (FileSet fs : _filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File fromDir = fs.getDir(getProject());
            for (String file : ds.getIncludedFiles()) {
                File source = new File(fromDir, file);
                File destDir = (_dest == null) ? source.getParentFile() :
                    new File(source.getParent().replaceAll(baseDir, _dest.getPath()));
                try {
                    convert(source, destDir);
                } catch (Exception e) {
                    System.err.println("Error converting " + source + ": " + e);
                }
            }
        }
    }

    /**
     * Converts a single file.
     */
    protected void convert (File source, File targetDir)
        throws IOException
    {
        // find the path of the target file
        String sname = source.getName();
        int didx = sname.lastIndexOf('.');
        String root = (didx == -1) ? sname : sname.substring(0, didx);
        File target = new File(targetDir, root + ".dat");

        // no need to compile if nothing has been modified
        long lastmod = target.lastModified();
        if (source.lastModified() < lastmod) {
            return;
        }
        System.out.println("Converting " + source + " to " + target + "...");

        // perform the conversion
        XMLToBinaryConverter.convert(source, target);
    }

    /** The directory in which we will generate our output (in a directory tree mirroring the
     * source files. */
    protected File _dest;

    /** A list of filesets that contain XML exports. */
    protected ArrayList<FileSet> _filesets = new ArrayList<FileSet>();
}
