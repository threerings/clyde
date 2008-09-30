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
