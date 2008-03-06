//
// $Id$

package com.threerings.opengl.model.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import org.xml.sax.SAXException;

import com.threerings.export.BinaryExporter;

import com.threerings.opengl.model.Model;

/**
 * An ant task for compiling 3D models defined in XML to fast-loading binary files.
 */
public class CompileModelTask extends Task
{
    public void setDest (File dest)
    {
        _dest = dest;
    }

    public void setSet (boolean set)
    {
        _set = set;
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
                    if (_set) {
                        compileSet(source, destDir);
                    } else {
                        compile(source, destDir);
                    }
                } catch (Exception e) {
                    System.err.println("Error compiling " + source + ": " + e);
                }
            }
        }
    }

    /**
     * Compiles the model described by the specified properties file, placing the resulting
     * file in the identified destination.
     */
    protected void compile (File pfile, File targetDir)
        throws IOException, SAXException
    {
        String pname = pfile.getName();
        int didx = pname.lastIndexOf('.');
        String root = (didx == -1) ? pname : pname.substring(0, didx);
        File target = new File(targetDir, root + ".dat");
        File mfile = new File(pfile.getParentFile(), root + ".mxml");

        // no need to compile if nothing has been modified
        long lastmod = target.lastModified();
        if (pfile.lastModified() < lastmod && mfile.lastModified() < lastmod) {
            return;
        }
        System.out.println("Compiling to " + target + "...");

        // read the model
        Model model = ModelReader.read(pfile, mfile);

        // write it out
        BinaryExporter out = new BinaryExporter(new FileOutputStream(target));
        out.writeObject(model);
        out.close();
    }

    /**
     * Compiles a set of models described by the specified properties file, placing the
     * resulting files in the identified destination.
     */
    protected void compileSet (File pfile, File targetDir)
        throws IOException, SAXException
    {
        // read the models
        File mfile = ModelReader.getModelFile(pfile);
        HashMap<String, Model> models = ModelReader.readSet(pfile, mfile);

        // write them out
        for (Map.Entry<String, Model> entry : models.entrySet()) {
            File target = new File(targetDir, entry.getKey() + ".dat");
            long lastmod = target.lastModified();
            if (pfile.lastModified() < lastmod && mfile.lastModified() < lastmod) {
                continue;
            }
            System.out.println("Compiling to " + target + "...");
            BinaryExporter out = new BinaryExporter(new FileOutputStream(target));
            out.writeObject(entry.getValue());
            out.close();
        }
    }

    /** The directory in which we will generate our output (in a directory tree mirroring the
     * source files. */
    protected File _dest;

    /** Whether or not to compile as a model set. */
    protected boolean _set;

    /** A list of filesets that contain model definitions. */
    protected ArrayList<FileSet> _filesets = new ArrayList<FileSet>();
}
