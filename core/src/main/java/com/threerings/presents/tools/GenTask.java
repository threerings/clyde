//
// $Id$
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/narya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.presents.tools;

import static com.google.common.base.Charsets.UTF_8;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.ClasspathUtils;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.samskivert.io.StreamUtil;
import com.samskivert.mustache.Mustache;

public abstract class GenTask extends Task
{
    /**
     * Adds a nested &lt;fileset&gt; element which enumerates service declaration source files.
     */
    public void addFileset (FileSet set)
    {
        _filesets.add(set);
    }

    /**
     * Configures us with a header file that we'll prepend to all
     * generated source files.
     */
    public void setHeader (File header)
    {
        try {
            _header = StreamUtil.toString(new FileReader(header));
        } catch (IOException ioe) {
            System.err.println("Unabled to load header '" + header + ": " +
                               ioe.getMessage());
        }
    }

    /** Configures our classpath which we'll use to load service classes. */
    public void setClasspathref (Reference pathref)
    {
        _cloader = ClasspathUtils.getClassLoaderForPath(getProject(), pathref);

        // set the parent of the classloader to be the classloader used to load this task, rather
        // than the classloader used to load Ant, so that we have access to Narya classes like
        // TransportHint
        ((AntClassLoader)_cloader).setParent(getClass().getClassLoader());
    }

    /**
     * Fails the build if generation would change files rather than generating
     * code.
     */
    public void setChecking (boolean checking)
    {
        _checking = checking;
    }

    /**
     * Performs the actual work of the task.
     */
    @Override
    public void execute ()
    {
        if (_checking) {
            log("Only checking if generation would change files", Project.MSG_VERBOSE);
        }
        for (FileSet fs : _filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File fromDir = fs.getDir(getProject());
            String[] srcFiles = ds.getIncludedFiles();
            for (String srcFile : srcFiles) {
                File source = new File(fromDir, srcFile);
                try {
                    processClass(source, loadClass(source));
                } catch (Exception e) {
                    throw new BuildException(e);
                }
            }
        }
        if (_checking && !_modifiedPaths.isEmpty()) {
            throw new BuildException("Generation would produce changes!");
        }

    }

    protected void writeTemplate (String templatePath, String outputPath, Object... data)
        throws IOException
    {
        writeTemplate(templatePath, outputPath, createMap(data));
    }

    protected void writeTemplate (String templatePath, String outputPath, Map<String, Object> data)
        throws IOException
    {
        String output = mergeTemplate(templatePath, data);
        if (_header != null) {
            output = convertEols(_header) + output;
        }
        writeFile(outputPath, output);
    }

    protected void writeFile (String outputPath, String output) throws IOException
    {
        File dest = new File(outputPath);
        if (dest.exists()) {
            if (wouldProduceSameFile(output, dest)) {
                log("Skipping '" + outputPath + "' as it hasn't changed", Project.MSG_VERBOSE);
                return;
            }
        } else if (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs()) {
            throw new BuildException("Unable to create directory for '" + dest.getAbsolutePath() + "'");
        }
        _modifiedPaths.add(outputPath);
        if (_checking) {
            log("Generating '" + outputPath + "' would have produced changes!", Project.MSG_ERR);
            return;
        }
        log("Writing file " + outputPath, Project.MSG_VERBOSE);

        new PrintWriter(dest, "UTF-8").append(output).close();
    }

    /**
     * Returns true if the given string has the same content as the file, sans svn prop lines.
     */
    protected boolean wouldProduceSameFile (String generated, File existing)
        throws IOException
    {
        Iterator<String> generatedLines = Splitter.on(EOL).split(generated).iterator();
        for (String prev : Files.readLines(existing, UTF_8)) {
            if (!generatedLines.hasNext()) {
                return false;
            }
            String cur = generatedLines.next();
            if (!prev.equals(cur) && !(prev.startsWith("// $Id") && cur.startsWith("// $Id"))) {
                return false;
            }
        }

        // If the generated output ends with a newline, it'll have one more next from the splitter
        // that reading the file doesn't produce.
        if (generatedLines.hasNext()) {
            return generatedLines.next().equals("") && !generatedLines.hasNext();
        }
        return true;
    }

    /**
     * Merges the specified template using the supplied mapping of keys to objects.
     *
     * @param data a series of key, value pairs where the keys must be strings and the values can
     * be any object.
     */
    protected String mergeTemplate (String template, Object... data)
        throws IOException
    {
        return mergeTemplate(template, createMap(data));
    }

    /**
     * Merges the specified template using the supplied mapping of string keys to objects.
     *
     * @return a string containing the merged text.
     */
    protected String mergeTemplate (String template, Map<String, Object> data)
        throws IOException
    {
        Reader reader =
            new InputStreamReader(getClass().getClassLoader().getResourceAsStream(template), UTF_8);
        return convertEols(Mustache.compiler().escapeHTML(false).compile(reader).execute(data));
    }

    protected Map<String, Object> createMap (Object... data)
    {
        Map<String, Object> ctx = Maps.newHashMap();
        for (int ii = 0; ii < data.length; ii += 2) {
            ctx.put((String)data[ii], data[ii+1]);
        }
        return ctx;
    }

    /**
     * Process a class found from the given source file that was on the filesets given to this
     * task.
     */
    protected abstract void processClass (File source, Class<?> klass)
        throws Exception;

    protected Class<?> loadClass (File source)
    {
        // load up the file and determine it's package and classname
        String name;
        try {
            name = GenUtil.readClassName(source);
        } catch (Exception e) {
            throw new BuildException("Failed to parse " + source, e);
        }
        return loadClass(name);
    }

    protected Class<?> loadClass (String name)
    {
        if (_cloader == null) {
            throw new BuildException("This task requires a 'classpathref' attribute " +
                "to be set to the project's classpath.");
        }
        try {
            return _cloader.loadClass(name);
        } catch (ClassNotFoundException cnfe) {
            throw new BuildException(
                "Failed to load " + name + ".  Be sure to set the 'classpathref' attribute to a " +
                "classpath that contains your project's presents classes.", cnfe);
        }
    }

    protected String convertEols (String str)
    {
        return str.replace("\n", EOL);
    }

    protected static String EOL = System.getProperty("line.separator");

    /** A list of filesets that contain java source to be processed. */
    protected List<FileSet> _filesets = Lists.newArrayList();

    /** Used to do our own classpath business. */
    protected ClassLoader _cloader;

    /** A header to put on all generated source files. */
    protected String _header;

    protected boolean _checking;
    protected Set<String> _modifiedPaths = Sets.newHashSet();
}
