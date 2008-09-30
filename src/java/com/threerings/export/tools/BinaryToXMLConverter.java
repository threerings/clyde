//
// $Id$

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
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(".");
        scanner.setIncludes(new String[] { pattern });
        scanner.scan();
        for (String source : scanner.getIncludedFiles()) {
            convert(source, FileUtil.resuffix(new File(source), ".dat", ".xml"));
        }
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
