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

package com.threerings.export.tools;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.tools.ant.DirectoryScanner;

import com.samskivert.util.FileUtil;

import com.threerings.export.BinaryImporter;
import com.threerings.export.XMLExporter;

import static com.threerings.export.Log.log;

/**
 * Converts binary export files into XML export files.
 */
public class BinaryToXMLConverter
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
                "Usage: BinaryToXMLConverter <binary input file> <xml output file>");
            System.err.println(
                "   or  BinaryToXMLConverter <binary input file pattern>");
        }
    }

    /**
     * Converts the file(s) identified by the given pattern.
     */
    public static void convert (String pattern)
        throws IOException
    {
        log.info("Starting conversion", "pattern", pattern, "pwd", new File(".").getCanonicalPath());
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(".");
        scanner.setIncludes(new String[] { pattern });
        scanner.scan();
        int count = 0;
        for (String source : scanner.getIncludedFiles()) {
            try {
                convert(source, FileUtil.resuffix(new File(source), ".dat", ".xml"));
                count++;
            } catch (Exception e) {
                log.warning("Error converting file.", "file", source, e);
            }
        }
        log.info("Finished conversion", "count", count);
    }

    /**
     * Performs the actual conversion.
     */
    public static void convert (String source, String dest)
        throws IOException
    {
        BinaryImporter in = new BinaryImporter(new FileInputStream(source));
        XMLExporter out = new XMLExporter(new FileOutputStream(dest));
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
