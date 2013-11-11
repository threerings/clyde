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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.google.common.collect.Lists;

import com.threerings.resource.ResourceManager;

import com.threerings.config.ConfigManager;
import com.threerings.editor.util.PropertyUtil;
import com.threerings.export.BinaryExporter;
import com.threerings.export.BinaryImporter;

/**
 * Strips classes and properties flagged as strippable from exported files.
 */
public class StripTask extends Task
{
    /**
     * Sets the destination directory to which generated files will be written.
     */
    public void setDest (File dest)
    {
        _dest = dest;
    }

    /**
     * Sets whether or not to compress the resulting files.
     */
    public void setCompress (boolean compress)
    {
        _compress = compress;
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
        rsrcmgr.initResourceDir("rsrc/");
        _cfgmgr = new ConfigManager(rsrcmgr, null, "config/");
        _cfgmgr.init();

        for (FileSet fs : _filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File fromDir = fs.getDir(getProject());
            for (String file : ds.getIncludedFiles()) {
                try {
                    strip(fromDir, file);
                } catch (Exception e) {
                    System.err.println("Error stripping " + new File(fromDir, file) + ": " + e);
                }
            }
        }
    }

    /**
     * Strips a single file.
     */
    protected void strip (File sourceDir, String sourceName)
        throws IOException
    {
        // find the path of the target file
        int didx = sourceName.lastIndexOf('.');
        String root = (didx == -1) ? sourceName : sourceName.substring(0, didx);
        File target = new File(_dest == null ? sourceDir : _dest, root + ".dat");

        // no need to strip if nothing has been modified
        File source = new File(sourceDir, sourceName);
        long lastmod = target.lastModified();
        if (source.lastModified() < lastmod) {
            return;
        }
        System.out.println("Stripping " + source + " to " + target + "...");

        // make sure the parent exists
        File parent = target.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        // perform the strip
        BinaryImporter in = new BinaryImporter(new FileInputStream(source));
        BinaryExporter out = new BinaryExporter(new FileOutputStream(target), _compress);
        try {
            while (true) {
                out.writeObject(strip(in.readObject()));
            }
        } catch (EOFException e) {
            // no problem
        } finally {
            in.close();
            out.close();
        }
    }

    /**
     * Do the stripping.
     */
    protected Object strip (Object object)
    {
        return PropertyUtil.strip(_cfgmgr, object);
    }

    /** The config manager. */
    protected ConfigManager _cfgmgr;

    /** The directory in which we will generate our output (in a directory tree mirroring the
     * source files. */
    protected File _dest;

    /** Whether or not to compress the output files. */
    protected boolean _compress = true;

    /** A list of filesets that contain XML exports. */
    protected List<FileSet> _filesets = Lists.newArrayList();
}
