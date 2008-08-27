//
// $Id$

package com.threerings.opengl.util;

import java.awt.image.BufferedImage;

import java.util.Arrays;

import com.samskivert.util.SoftCache;

import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;

import com.threerings.opengl.renderer.Texture;
import com.threerings.opengl.renderer.Texture2D;

/**
 * Caches loaded textures.
 */
public class TextureCache
{
    /**
     * Creates a new texture cache.
     */
    public TextureCache (GlContext ctx)
    {
        _ctx = ctx;
    }

    /**
     * Loads and returns the texture at the specified path.
     */
    public Texture getTexture (String path)
    {
        return getTexture(path, null);
    }

    /**
     * Loads and returns the texture at the specified path with the given colorizations.
     */
    public Texture getTexture (String path, Colorization[] zations)
    {
        return getTexture(path, zations, true, true, true);
    }

    /**
     * Loads and returns the texture at the specified path with the given colorizations and
     * parameters.
     *
     * @param premultiply if true, premultiply the image's alpha values.
     * @param mipmap if true, generate a complete set of mipmaps for the image.
     * @param compress if true, use a compressed texture format if supported (if not, scale it down
     * by one half).
     */
    public Texture getTexture (
        String path, Colorization[] zations, boolean premultiply, boolean mipmap, boolean compress)
    {
        TextureKey tkey = new TextureKey(path, zations);
        Texture texture = _textures.get(tkey);
        if (texture == null) {
            BufferedImage image = _ctx.getImageCache().getBufferedImage(path);
            if (zations != null) {
                image = ImageUtil.recolorImage(image, zations);
            }
            Texture2D tex2d = new Texture2D(_ctx.getRenderer());
            tex2d.setImage(image, premultiply, mipmap, compress);
            _textures.put(tkey, texture = tex2d);
        }
        return texture;
    }

    /**
     * Identifies a loaded texture.
     */
    protected static class TextureKey
    {
        /** The texture resource path. */
        public String path;

        /** The colorizations to apply to the image (if any). */
        public Colorization[] zations;

        public TextureKey (String path, Colorization[] zations)
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
            TextureKey okey = (TextureKey)other;
            return path.equals(okey.path) && Arrays.equals(zations, okey.zations);
        }
    }

    /** The renderer context. */
    protected GlContext _ctx;

    /** The set of loaded textures. */
    protected SoftCache<TextureKey, Texture> _textures = new SoftCache<TextureKey, Texture>();
}
