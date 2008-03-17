//
// $Id$

package com.threerings.export.tools;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;

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
        Object obj;
        while ((obj = in.readObject()) != null) {
            out.writeObject(obj);
        }
        in.close();
        out.close();
    }
}
