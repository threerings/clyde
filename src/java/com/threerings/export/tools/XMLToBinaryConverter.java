//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

package com.threerings.export.tools;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.tools.ant.DirectoryScanner;

import com.samskivert.util.FileUtil;

import com.threerings.export.BinaryExporter;
import com.threerings.export.XMLImporter;

/**
 * Converts XML export files into binary export files.
 */
public class XMLToBinaryConverter
{
    /**
     * Program entry point.
     */
    public static void main (String[] args)
        throws Exception
    {
        if (args.length == 2) {
            convert(args[0], args[1]);
        } else if (args.length == 1) {
            convert(args[0]);
        } else {
            System.err.println(
                "Usage: XMLToBinaryConverter <xml input file> <binary output file>");
            System.err.println(
                "   or  XMLToBinaryConverter <xml input file pattern>");
        }
    }

    /**
     * Converts the file(s) identified by the given pattern.
     */
    public static void convert (String pattern)
        throws IOException
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(".");
        scanner.setIncludes(new String[] { pattern });
        scanner.scan();
        for (String source : scanner.getIncludedFiles()) {
            convert(source, FileUtil.resuffix(new File(source), ".xml", ".dat"));
        }
    }

    /**
     * Performs the actual conversion.
     */
    public static void convert (String source, String dest)
        throws IOException
    {
        XMLImporter in = new XMLImporter(new FileInputStream(source));
        BinaryExporter out = new BinaryExporter(new FileOutputStream(dest));
        try {
            while (true) {
                out.writeObject(in.readObject());
            }
        } catch (EOFException e) {
            // no problem
        } finally {
            in.close();
            out.close();
        }
    }
}
