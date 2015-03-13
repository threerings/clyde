//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// https://github.com/threerings/nenya
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Used to load and store compiled configuration data (generally XML files that are parsed into
 * Java object models and then serialized for rapid and simple access on the client and server).
 */
public class CompiledConfig
{
    /**
     * Unserializes a configuration object from the supplied input stream.
     */
    public static Serializable loadConfig (InputStream source)
        throws IOException
    {
        try {
            ObjectInputStream oin = new ObjectInputStream(source);
            return (Serializable)oin.readObject();
        } catch (ClassNotFoundException cnfe) {
            String errmsg = "Unknown config class";
            throw (IOException) new IOException(errmsg).initCause(cnfe);
        }
    }

    /**
     * Serializes the supplied configuration object to the specified file path.
     */
    public static void saveConfig (File target, Serializable config)
        throws IOException
    {
        FileOutputStream fout = new FileOutputStream(target);
        ObjectOutputStream oout = new ObjectOutputStream(fout);
        oout.writeObject(config);
        oout.close();
    }
}
