//
// $Id$

package com.threerings.export.tools;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;

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
        if (args.length < 2) {
            System.err.println(
                "Usage: BinaryToXMLConverter <binary input file> <xml output file>");
            return;
        }
        BinaryImporter in = new BinaryImporter(new FileInputStream(args[0]));
        XMLExporter out = new XMLExporter(new FileOutputStream(args[1]));
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
