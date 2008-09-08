//
// $Id$

package com.threerings.export.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.threerings.export.BinaryExporter;
import com.threerings.export.BinaryImporter;
import com.threerings.export.XMLExporter;
import com.threerings.export.XMLImporter;

import static com.threerings.export.Log.*;

/**
 * Some general utility methods relating to exporting.
 */
public class ExportUtil
{
    /**
     * Converts an exportable object to a string containing the exported XML representation of the
     * object.  If an error occurs, a warning will be logged and <code>null</code> will be
     * returned.
     */
    public static String toString (Object object)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLExporter out = new XMLExporter(baos);
        try {
            out.writeObject(object);
            out.close();
            return baos.toString();
        } catch (IOException e) {
            log.warning("Error exporting object to string.", "object", object, e);
            return null;
        }
    }

    /**
     * Parses a string containing an XML export and returns the decoded object.  If an error
     * occurs, a warning will be logged and <code>null</code> will be returned.
     */
    public static Object fromString (String string)
    {
        XMLImporter in = new XMLImporter(new ByteArrayInputStream(string.getBytes()));
        try {
            Object object = in.readObject();
            in.close();
            return object;
        } catch (IOException e) {
            log.warning("Error importing object from string.", "string", string, e);
            return null;
        }
    }

    /**
     * Converts an exportable object to a byte array containing the exported binary representation
     * of the object.  If an error occurs, a warning will be logged and <code>null</code> will
     * be returned.
     */
    public static byte[] toBytes (Object object)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryExporter out = new BinaryExporter(baos);
        try {
            out.writeObject(object);
            out.close();
            return baos.toByteArray();
        } catch (IOException e) {
            log.warning("Error exporting object to byte array.", "object", object, e);
            return new byte[0];
        }
    }

    /**
     * Parses a byte array containing a binary export and returns the decoded object.  If an error
     * occurs, a warning will be logged and <code>null</code> will be returned.
     */
    public static Object fromBytes (byte[] bytes)
    {
        BinaryImporter in = new BinaryImporter(new ByteArrayInputStream(bytes));
        try {
            Object object = in.readObject();
            in.close();
            return object;
        } catch (IOException e) {
            log.warning("Error importing object from byte array.", e);
            return null;
        }
    }
}
