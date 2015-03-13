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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.Permission;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.io.ByteArrayOutInputStream;
import com.samskivert.net.AttachableURLFactory;
import com.samskivert.util.StringUtil;

import com.threerings.media.image.ColorPository;
import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;

import com.threerings.geom.GeomUtil;

import static com.threerings.resource.Log.log;

/**
 * This class is not used directly, except by a registering ResourceManager so that we can load
 * data from the resource manager using URLs of the form
 * <code>resource://&lt;resourceSet&gt;/&lt;path&gt;</code>. ResourceSet may be the empty string
 * to load from the default resource sets.
 */
public class Handler extends URLStreamHandler
{
    /**
     * Register this class to handle "resource" urls ("resource://<i>resourceSet</i>/<i>path</i>")
     * with the specified ResourceManager.
     */
    public static void registerHandler (ResourceManager rmgr)
    {
        // we only want to wire up our Handler once, but if a new resource
        // manager comes along, we'll point the existing handler there
        if (_rmgr == null) {
            // wire up our handler with the handy dandy attachable URL factory
            AttachableURLFactory.attachHandler("resource", Handler.class);
        }
        _rmgr = rmgr;
    }

    @Override
    protected int hashCode (URL url)
    {
        return String.valueOf(url).hashCode();
    }

    @Override
    protected boolean equals (URL u1, URL u2)
    {
        return String.valueOf(u1).equals(String.valueOf(u2));
    }

    @Override
    protected URLConnection openConnection (URL url)
        throws IOException
    {
        return new URLConnection(url) {
            @Override
            public void connect ()
                throws IOException
            {
                // the host is the bundle name
                String bundle = this.url.getHost();
                // and we need to remove the leading '/' from path;
                String path = this.url.getPath().substring(1);
                try {
                    // if there are query parameters, we need special magic
                    String query = url.getQuery();
                    if (!StringUtil.isBlank(query)) {
                        _stream = getStream(bundle, path, query);
                    } else if (StringUtil.isBlank(bundle)) {
                        _stream = _rmgr.getResource(path);
                    } else {
                        _stream = _rmgr.getResource(bundle, path);
                    }
                    this.connected = true;

                } catch (IOException ioe) {
                    log.warning("Could not find resource",
                        "url", this.url, "error", ioe.getMessage());
                    throw ioe; // rethrow
                }
            }

            @Override
            public InputStream getInputStream ()
                throws IOException
            {
                if (!this.connected) {
                    connect();
                }
                return _stream;
            }

            @Override
            public Permission getPermission ()
                throws IOException
            {
                // We allow anything in the resource bundle to be loaded
                // without any permission restrictions.
                return null;
            }

            protected InputStream _stream;
        };
    }

    /**
     * Does some magic to allow a subset of an image to be extracted, reencoded as a PNG and then
     * spat back out to the Java content handler system for inclusion in internal documentation.
     */
    protected InputStream getStream (String bundle, String path, String query)
        throws IOException
    {
        // we can only do this with PNGs
        if (!path.endsWith(".png")) {
            log.warning("Requested sub-tile of non-PNG resource",
                "bundle", bundle, "path", path, "dims", query);
            return _rmgr.getResource(bundle, path);
        }

        // parse the query string
        String[] bits = StringUtil.split(query, "&");
        int width = -1, height = -1, tidx = -1;
        HashMap<String, String> zations = null;
        try {
            for (String bit : bits) {
                if (bit.startsWith("width=")) {
                    width = Integer.parseInt(bit.substring(6));
                } else if (bit.startsWith("height=")) {
                    height = Integer.parseInt(bit.substring(7));
                } else if (bit.startsWith("tile=")) {
                    tidx = Integer.parseInt(bit.substring(5));
                } else if (bit.startsWith("zation=")) {
                    String[] zation = bit.substring(7).split(":");
                    if (zations == null) {
                        zations = Maps.newHashMap();
                    }

                    zations.put(zation[0], zation[1]);
                }
            }
        } catch (NumberFormatException nfe) {
        }
        if (width <= 0 || height <= 0 || tidx < 0) {
            log.warning("Bogus sub-image dimensions",
                "bundle", bundle, "path", path, "dims", query);
            throw new FileNotFoundException(path);
        }

        // locate the tile image, then write that subimage back out in PNG format into memory and
        // return an input stream for that
        BufferedImage src = StringUtil.isBlank(bundle) ?
            _rmgr.getImageResource(path) : _rmgr.getImageResource(bundle, path);
        Rectangle trect = GeomUtil.getTile(src.getWidth(), src.getHeight(), width, height, tidx);
        BufferedImage tile = src.getSubimage(trect.x, trect.y, trect.width, trect.height);
        if (zations != null) {
            ColorPository pository = ColorPository.loadColorPository(_rmgr);
            ArrayList<Colorization> colorizations = Lists.newArrayList();
            for (Map.Entry<String, String> entry : zations.entrySet()) {
                String zClass = entry.getKey();
                String zColor = entry.getValue();

                Colorization zation = null;

                // First try looking if we got a number
                try {
                    zation = pository.getColorization(zClass, Integer.parseInt(zColor));
                } catch (NumberFormatException nfe) { }

                // If that didn't work, try it as a zation name
                if (zation == null) {
                    zation = pository.getColorization(zClass, zColor);
                }

                if (zation == null) {
                    log.warning("Couldn't figure out requested zation",
                        "class", zClass, "color", zColor);
                } else {
                    colorizations.add(zation);
                }

            }

            tile = ImageUtil.recolorImage(tile,
                colorizations.toArray(new Colorization[colorizations.size()]));
        }

        ByteArrayOutInputStream data = new ByteArrayOutInputStream();
        ImageIO.write(tile, "PNG", data);
        return data.getInputStream();
    }

    /** Our singleton resource manager. */
    protected static ResourceManager _rmgr;
}
