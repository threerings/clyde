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

package com.threerings.tools;

import java.io.File;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.google.common.collect.Lists;

/**
 * Abstract class for Tasks that operate on Files in a FileSet.
 */
public abstract class FileSetTask extends Task
{
    /**
     * Adds a fileset to the list of sets to process.
     */
    public void addFileset (FileSet set)
    {
        _filesets.add(set);
    }

    /**
     * Get the files from all the filesets.
     */
    protected Iterable<File> getFiles ()
    {
        return Iterables.concat(Iterables.transform(_filesets, _filesetToFiles));
    }

    /** A function to transform a fileset into the files it represents. */
    protected final Function<FileSet, Iterable<File>> _filesetToFiles =
            new Function<FileSet, Iterable<File>>() {
                public Iterable<File> apply (FileSet fileset) {
                    DirectoryScanner ds = fileset.getDirectoryScanner(getProject());
                    final File fromDir = fileset.getDir(getProject());
                    return Iterables.transform(Arrays.asList(ds.getIncludedFiles()),
                            new Function<String, File>() {
                                public File apply (String file) {
                                    return new File(fromDir, file);
                                }
                            });
                }
            };

    /** A list of filesets. */
    protected List<FileSet> _filesets = Lists.newArrayList();
}
