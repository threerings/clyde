//
// $Id$

package com.threerings.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Properties;
import java.util.prefs.Preferences;

import static com.threerings.ClydeLog.*;

/**
 * Utility methods relating to resources.
 */
public class ResourceUtil
{
    /**
     * Returns the resource directory stored in the preferences, which (if present) overrides the
     * default resource directory.
     *
     * @return the stored resource directory, or <code>null</code> to use the default.
     */
    public static String getPreferredResourceDir ()
    {
        return _prefs.get("resource_dir", null);
    }

    /**
     * Sets the preferred resource directory.
     *
     * @param dir the resource directory to store, or <code>null</code> to use to the default.
     */
    public static void setPreferredResourceDir (String dir)
    {
        if (dir == null) {
            _prefs.remove("resource_dir");
        } else {
            _prefs.put("resource_dir", dir);
        }
    }

    /**
     * Reads a list of newline-delimited strings from a resource.
     */
    public static String[] loadStrings (String path)
    {
        ArrayList<String> strings = new ArrayList<String>();
        InputStream stream = getResourceAsStream(path);
        if (stream != null) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String line;
                while ((line = reader.readLine()) != null) {
                    strings.add(line);
                }
            } catch (IOException e) {
                log.warning("Failed to read strings from resource [path=" +
                    path + ", error=" + e + "].");
            }
        }
        return strings.toArray(new String[strings.size()]);
    }

    /**
     * Reads a set of properties from a resource.
     */
    public static Properties loadProperties (String path)
    {
        Properties props = new Properties();
        InputStream stream = getResourceAsStream(path);
        if (stream != null) {
            try {
                props.load(stream);
            } catch (IOException e) {
                log.warning("Failed to read properties from resource [path=" +
                    path + ", error=" + e + "].");
            }
        }
        return props;
    }

    /**
     * Gets the identified resource as a stream, logging an error and returning an empty stream if
     * it doesn't exist.
     */
    public static InputStream getResourceAsStream (String path)
    {
        InputStream stream = ResourceUtil.class.getResourceAsStream("/rsrc/" + path);
        if (stream == null) {
            log.warning("Missing resource [path=" + path + "].");
            stream = new ByteArrayInputStream(new byte[0]);
        }
        return stream;
    }

    /** The package preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(ResourceUtil.class);
}
