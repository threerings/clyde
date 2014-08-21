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

package com.threerings.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Properties;
import java.util.prefs.Preferences;

import static com.threerings.ClydeLog.log;

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
        return _prefs.get(_prefPrefix + "resource_dir", null);
    }

    /**
     * Sets the preferred resource directory.
     *
     * @param dir the resource directory to store, or <code>null</code> to use to the default.
     */
    public static void setPreferredResourceDir (String dir)
    {
        if (dir == null) {
            _prefs.remove(_prefPrefix + "resource_dir");
        } else {
            _prefs.put(_prefPrefix + "resource_dir", dir);
        }
    }

    /**
     * Get the prefix to use for project-wide preferences.
     */
    public static String getPrefsPrefix ()
    {
        return _prefPrefix;
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
        InputStream stream = ResourceUtil.class.getResourceAsStream("/" + path);
        if (stream == null) {
            log.warning("Missing resource.", "path", path);
            stream = new ByteArrayInputStream(new byte[0]);
        }
        return stream;
    }

    /** The package preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(ResourceUtil.class);

    /** The pref prefix. */
    protected static String _prefPrefix =
            System.getProperty("com.threerings.resource.pref_prefix", "");
}
