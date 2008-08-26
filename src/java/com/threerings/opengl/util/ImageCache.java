//
// $Id$

package com.threerings.opengl.util;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.threerings.media.image.ImageUtil;

import static com.threerings.opengl.Log.*;

/**
 * A cache for images.
 */
public class ImageCache extends ResourceCache<BufferedImage>
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
     * Retrieves an image from the cache.
     */
    public BufferedImage getImage (String path)
    {
        return getResource(path);
    }

    @Override // documentation inherited
    protected BufferedImage loadResource (File file)
    {
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            log.warning("Failed to read image.", "file", file, e);
            return ImageUtil.createErrorImage(64, 64);
        }
    }
}
