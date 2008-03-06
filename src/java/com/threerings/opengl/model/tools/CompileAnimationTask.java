//
// $Id$

package com.threerings.opengl.model.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import org.xml.sax.SAXException;

import com.threerings.export.BinaryExporter;

import com.threerings.opengl.model.Animation;

/**
 * An ant task for compiling 3D animations defined in XML to fast-loading binary files.
 */
public class CompileAnimationTask extends Task
{
    public void setDest (File dest)
    {
        _dest = dest;
    }

    public void addFileset (FileSet set)
    {
        _filesets.add(set);
    }

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
                    compile(source, destDir);
                } catch (Exception e) {
                    System.err.println("Error compiling " + source + ": " + e);
                }
            }
        }
    }

    /**
     * Compiles the animation described by the specified properties file, placing the resulting
     * file in the identified destination.
     */
    protected void compile (File pfile, File targetDir)
        throws IOException, SAXException
    {
        String pname = pfile.getName();
        int didx = pname.lastIndexOf('.');
        String root = (didx == -1) ? pname : pname.substring(0, didx);
        File target = new File(targetDir, root + ".dat");
        File afile = new File(pfile.getParentFile(), root + ".mxml");

        // no need to compile if nothing has been modified
        long lastmod = target.lastModified();
        if (pfile.lastModified() < lastmod && afile.lastModified() < lastmod) {
            return;
        }
        System.out.println("Compiling to " + target + "...");

        // read the animation
        Animation anim = AnimationReader.read(pfile, afile);

        // write it out
        BinaryExporter out = new BinaryExporter(new FileOutputStream(target));
        out.writeObject(anim);
        out.close();
    }

    /** The directory in which we will generate our output (in a directory tree mirroring the
     * source files. */
    protected File _dest;

    /** A list of filesets that contain animation definitions. */
    protected ArrayList<FileSet> _filesets = new ArrayList<FileSet>();
}
