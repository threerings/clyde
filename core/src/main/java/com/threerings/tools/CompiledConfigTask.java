//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// https://github.com/threerings/nenya
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

package com.threerings.tools;

import java.util.ArrayList;

import java.io.File;
import java.io.Serializable;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.google.common.collect.Lists;

import com.samskivert.util.FileUtil;

import com.threerings.util.CompiledConfig;

import com.threerings.tools.xml.CompiledConfigParser;

/**
 * Used to parse configuration information from an XML file and create the
 * serialized representation that is used by the client and server.
 */
public class CompiledConfigTask extends Task
{
    public void setParser (String parser)
    {
        _parser = parser;
    }

    public void setConfigdef (File configdef)
    {
        _configdef = configdef;
    }

    public void setTarget (File target)
    {
        _target = target;
    }

    public void setDest (File dest)
    {
        _dest = dest;
    }

    public void addFileset (FileSet set)
    {
        _filesets.add(set);
    }

    @Override
    public void execute () throws BuildException
    {
        // instantiate and sanity check the parser class
        Object pobj = null;
        try {
            Class<?> pclass = Class.forName(_parser);
            pobj = pclass.newInstance();
        } catch (Exception e) {
            throw new BuildException("Error instantiating config parser", e);
        }
        if (!(pobj instanceof CompiledConfigParser)) {
            throw new BuildException("Invalid parser class: " + _parser);
        }
        CompiledConfigParser parser = (CompiledConfigParser)pobj;

        // if we have a single file and target specified, do those
        if (_configdef != null) {
            parse(parser, _configdef, _target == null ? getTarget(_configdef) : _target);
        }

        // deal with the filesets
        for (FileSet fs : _filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File fromDir = fs.getDir(getProject());
            for (String file : ds.getIncludedFiles()) {
                File source = new File(fromDir, file);
                parse(parser, source, getTarget(source));
            }
        }
    }

    protected File getTarget (File source)
    {
        if (_dest == null) {
            return null;
        }

        String baseDir = getProject().getBaseDir().getPath();
        File target = new File(source.getPath().replace(baseDir, _dest.getPath()));
        target = new File(FileUtil.resuffix(target, ".xml", ".dat"));
        return target;
    }

    protected void parse (CompiledConfigParser parser, File confdef, File target)
        throws BuildException
    {
        // make sure the source file exists
        if (!confdef.exists()) {
            String errmsg = "Config definition file not found: " + confdef;
            throw new BuildException(errmsg);
        }

        // if no target was specified, resuffix the source file as to .dat
        if (target == null) {
            target = new File(FileUtil.resuffix(confdef, ".xml", ".dat"));
        }

        System.out.println("Compiling " + target + "...");
        Serializable config = null;
        try {
            // parse it on up
            config = parser.parseConfig(confdef);
        } catch (Exception e) {
            throw new BuildException("Failure parsing config definition", e);
        }

        // create the target directory if necessary
        File parent = target.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new BuildException("Failed to create parent directory '" + parent + "'.");
        }

        try {
            // and write it on out
            CompiledConfig.saveConfig(target, config);
        } catch (Exception e) {
            throw new BuildException("Failure writing serialized config", e);
        }
    }

    protected File _configdef;
    protected File _target;
    protected File _dest;
    protected String _parser;
    protected ArrayList<FileSet> _filesets = Lists.newArrayList();
}
