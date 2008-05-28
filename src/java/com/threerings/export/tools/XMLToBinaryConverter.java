//
// $Id$

package com.threerings.export.tools;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;

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
        XMLImporter in = new XMLImporter(new FileInputStream(args[0]));
        BinaryExporter out = new BinaryExporter(new FileOutputStream(args[1]));
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
