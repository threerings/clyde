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

package com.threerings.resource;

import java.io.IOException;
import java.io.InputStream;

import java.awt.image.BufferedImage;

/**
 * A resource bundle provides access to media resources.
 */
public abstract class ResourceBundle
{
    /**
     * Uniquely identifies this resource bundle. Used to create cache keys.
     */
    public abstract String getIdent ();

    /**
     * Fetches the named resource from this bundle. The path should be specified as a relative,
     * platform independent path (forward slashes). For example <code>sounds/scream.au</code>.
     *
     * @param path the path to the resource in this jar file.
     *
     * @return an input stream from which the resource can be loaded or null if no such resource
     * exists.
     *
     * @exception IOException thrown if an error occurs locating the resource in the jar file.
     */
    public abstract InputStream getResource (String path)
        throws IOException;

    /**
     * Decodes and returns the specified image resource. Returns null if no resource exists at the
     * specified path.
     */
    public abstract BufferedImage getImageResource (String path, boolean useFastIO)
        throws IOException;
}
