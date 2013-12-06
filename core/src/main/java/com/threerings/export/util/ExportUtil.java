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

package com.threerings.export.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.threerings.export.BinaryExporter;
import com.threerings.export.BinaryImporter;
import com.threerings.export.XMLExporter;
import com.threerings.export.XMLImporter;

import static com.threerings.export.Log.log;

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
        return toBytes(object, true);
    }

    /**
     * Converts an exportable object to a byte array containing the exported binary representation
     * of the object.  If an error occurs, a warning will be logged and <code>null</code> will
     * be returned.
     */
    public static byte[] toBytes (Object object, boolean compress)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryExporter out = new BinaryExporter(baos, compress);
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
