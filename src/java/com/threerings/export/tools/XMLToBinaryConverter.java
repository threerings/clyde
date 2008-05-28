//
// $Id$

package com.threerings.export.tools;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

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
        if (args.length < 2) {
            System.err.println(
                "Usage: XMLToBinaryConverter <xml input file> <binary output file>");
            return;
        }
        convert(new File(args[0]), new File(args[1]));
    }

    /**
     * Performs the actual conversion.
     */
    public static void convert (File source, File dest)
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
