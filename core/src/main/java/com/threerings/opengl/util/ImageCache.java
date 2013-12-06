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

package com.threerings.opengl.util;

import java.awt.image.BufferedImage;

import java.io.IOException;

import java.util.Arrays;

import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;

import com.threerings.opengl.gui.Image;
import com.threerings.opengl.renderer.Texture2D;
import com.threerings.opengl.util.DDSLoader;

import static com.threerings.opengl.Log.log;

/**
 * A cache for images.
 */
public class ImageCache extends ResourceCache
{
    /**
     * Creates a new image cache.
     *
     * @param checkTimestamps if true, check the last-modified timestamp of each resource file
     * when we retrieve it from the cache, reloading the resource if the file has been modified
     * externally.
     */
    public ImageCache (GlContext ctx, boolean checkTimestamps)
    {
        super(ctx, checkTimestamps);
    }

    /**
     * Retrieves a GUI image from the cache.
     */
    public Image getImage (String path, Colorization... zations)
    {
        return _images.getResource(new ImageKey(path, zations));
    }

    /**
     * Retrieves a buffered image from the cache.
     */
    public BufferedImage getBufferedImage (String path, Colorization... zations)
    {
        return _buffered.getResource(new ImageKey(path, zations));
    }

    /**
     * Clears the cache, forcing resources to be reloaded.
     */
    public void clear ()
    {
        _images.clear();
        _buffered.clear();
    }

    /**
     * Identifies a cached image.
     */
    protected static class ImageKey
    {
        /** The path of the image resource. */
        public String path;

        /** The colorizations to apply to the image. */
        public Colorization[] zations;

        public ImageKey (String path, Colorization[] zations)
        {
            this.path = path;
            this.zations = zations;
        }

        @Override
        public int hashCode ()
        {
            return path.hashCode() ^ Arrays.hashCode(zations);
        }

        @Override
        public boolean equals (Object other)
        {
            ImageKey okey = (ImageKey)other;
            return path.equals(okey.path) && Arrays.equals(zations, okey.zations);
        }
    }

    /** The GUI image subcache. */
    protected Subcache<ImageKey, Image> _images = new Subcache<ImageKey, Image>() {
        protected Image loadResource (ImageKey key) {
            if (key.path.endsWith(".dds")) {
                Texture2D texture = new Texture2D(_ctx.getRenderer());
                try {
                    DDSLoader.load(_ctx.getResourceManager().getResourceFile(key.path),
                        texture, false);
                    Image.configureTexture(texture);
                    return new Image(texture);
                } catch (IOException e) {
                    // fall through to buffered image loader
                }
            }
            return new Image(_buffered.getResource(key));
        }
        protected String getResourcePath (ImageKey key) {
            return key.path;
        }
    };

    /** The buffered image subcache. */
    protected Subcache<ImageKey, BufferedImage> _buffered =
        new Subcache<ImageKey, BufferedImage>() {
        protected BufferedImage loadResource (ImageKey key) {
            if (key.zations.length > 0) {
                return ImageUtil.recolorImage(getBufferedImage(key.path), key.zations);
            }
            BufferedImage image = null;
            try {
                if ((image = _ctx.getResourceManager().getImageResource(key.path)) == null) {
                    log.warning("Unknown image format.", "path", key.path);
                }
            } catch (IOException e) {
                log.warning("Failed to read image.", "path", key.path, e);
            }
            return (image == null) ? ImageUtil.createErrorImage(64, 64) : image;
        }
        protected String getResourcePath (ImageKey key) {
            return key.path;
        }
    };
}
