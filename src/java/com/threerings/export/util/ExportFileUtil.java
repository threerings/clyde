//
// $Id$
package com.threerings.export.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.google.common.io.Closeables;

import com.threerings.export.BinaryImporter;

/**
 * File-related export utilities.
 */
public class ExportFileUtil
{
    /**
     * Read a single object of the specified type from the file.
     */
    public static <T> T readObject (File file, Class<T> clazz)
        throws IOException
    {
        BinaryImporter in = null;
        try {
            in = new BinaryImporter(new BufferedInputStream(new FileInputStream(file)));
            return clazz.cast(in.readObject());

        } catch (ClassCastException cce) {
            throw new IOException("File " + file + " doesn't contain a " + clazz, cce);

        } finally {
            Closeables.closeQuietly(in);
        }
    }
}
