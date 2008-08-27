//
// $Id$

package com.threerings.opengl.util;

import java.awt.image.BufferedImage;

import java.io.IOException;

import java.util.Arrays;

import javax.imageio.ImageIO;

import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;

import static com.threerings.opengl.Log.*;

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
     * Retrieves a buffered image from the cache.
     */
    public BufferedImage getBufferedImage (String path, Colorization... zations)
    {
        return _buffered.getResource(new ImageKey(path, zations));
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

        @Override // documentation inherited
        public int hashCode ()
        {
            return path.hashCode() ^ Arrays.hashCode(zations);
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            ImageKey okey = (ImageKey)other;
            return path.equals(okey.path) && Arrays.equals(zations, okey.zations);
        }
    }

    /** The buffered image subcache. */
    protected Subcache<ImageKey, BufferedImage> _buffered =
        new Subcache<ImageKey, BufferedImage>() {
        protected BufferedImage loadResource (ImageKey key) {
            if (key.zations.length > 0) {
                return ImageUtil.recolorImage(getBufferedImage(key.path), key.zations);
            }
            try {
                // TODO: simplify when we no longer need to support absolute paths
                return ImageIO.read(getResourceFile(key));
            } catch (IOException e) {
                log.warning("Failed to read image.", "path", key.path, e);
                return ImageUtil.createErrorImage(64, 64);
            }
        }
        protected String getResourcePath (ImageKey key) {
            return key.path;
        }
    };
}
