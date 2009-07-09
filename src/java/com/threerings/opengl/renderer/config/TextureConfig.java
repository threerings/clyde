//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

package com.threerings.opengl.renderer.config;

import java.awt.image.BufferedImage;

import java.lang.ref.SoftReference;

import java.util.HashSet;

import org.lwjgl.opengl.ARBShadow;
import org.lwjgl.opengl.ARBTextureBorderClamp;
import org.lwjgl.opengl.ARBTextureCompression;
import org.lwjgl.opengl.ARBTextureMirroredRepeat;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;

import com.threerings.io.Streamable;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.Texture;
import com.threerings.opengl.renderer.Texture1D;
import com.threerings.opengl.renderer.Texture2D;
import com.threerings.opengl.renderer.Texture3D;
import com.threerings.opengl.renderer.TextureCubeMap;
import com.threerings.opengl.util.GlContext;

/**
 * Texture metadata.
 */
public class TextureConfig extends ParameterizedConfig
{
    /** Format constants. */
    public enum Format
    {
        DEFAULT(-1) {
            public int getConstant (BufferedImage image) {
                switch (image == null ? 4 : image.getColorModel().getNumComponents()) {
                    case 1: return LUMINANCE.getConstant(image);
                    case 2: return LUMINANCE_ALPHA.getConstant(image);
                    case 3: return RGB.getConstant(image);
                    default: return RGBA.getConstant(image);
                }
            }
        },
        COMPRESSED_DEFAULT(-1) {
            public int getConstant (BufferedImage image) {
                switch (image == null ? 4 : image.getColorModel().getNumComponents()) {
                    case 1: return COMPRESSED_LUMINANCE.getConstant(image);
                    case 2: return COMPRESSED_LUMINANCE_ALPHA.getConstant(image);
                    case 3: return COMPRESSED_RGB.getConstant(image);
                    default: return COMPRESSED_RGBA.getConstant(image);
                }
            }
        },
        ALPHA(GL11.GL_ALPHA),
        COMPRESSED_ALPHA(ARBTextureCompression.GL_COMPRESSED_ALPHA_ARB, GL11.GL_ALPHA),
        LUMINANCE(GL11.GL_LUMINANCE),
        COMPRESSED_LUMINANCE(ARBTextureCompression.GL_COMPRESSED_LUMINANCE_ARB, GL11.GL_LUMINANCE),
        LUMINANCE_ALPHA(GL11.GL_LUMINANCE_ALPHA),
        COMPRESSED_LUMINANCE_ALPHA(
            ARBTextureCompression.GL_COMPRESSED_LUMINANCE_ALPHA_ARB, GL11.GL_LUMINANCE_ALPHA),
        INTENSITY(GL11.GL_INTENSITY),
        COMPRESSED_INTENSITY(ARBTextureCompression.GL_COMPRESSED_INTENSITY_ARB, GL11.GL_INTENSITY),
        RGB(GL11.GL_RGB),
        COMPRESSED_RGB(ARBTextureCompression.GL_COMPRESSED_RGB_ARB, GL11.GL_RGB),
        RGBA(GL11.GL_RGBA),
        COMPRESSED_RGBA(ARBTextureCompression.GL_COMPRESSED_RGBA_ARB, GL11.GL_RGBA),
        DEPTH_COMPONENT(GL11.GL_DEPTH_COMPONENT, -1, true);

        /**
         * Returns the OpenGL constant associated with this format.
         *
         * @param image the image used to guess the format if necessary.
         */
        public int getConstant (BufferedImage image)
        {
            // return the uncompressed equivalent if we don't support texture compression
            if (_uncompressed != -1 && !GLContext.getCapabilities().GL_ARB_texture_compression) {
                return _uncompressed;
            }
            return _constant;
        }

        public boolean isSupported (boolean fallback)
        {
            return (!_depth || GLContext.getCapabilities().GL_ARB_depth_texture);
        }

        Format (int constant)
        {
            this(constant, -1);
        }

        Format (int constant, int uncompressed)
        {
            this(constant, uncompressed, false);
        }

        Format (int constant, int uncompressed, boolean depth)
        {
            _constant = constant;
            _uncompressed = uncompressed;
            _depth = depth;
        }

        protected int _constant;
        protected int _uncompressed;
        protected boolean _depth;
    }

    /** Minification filter constants. */
    public enum MinFilter
    {
        NEAREST(GL11.GL_NEAREST, false),
        LINEAR(GL11.GL_LINEAR, false),
        NEAREST_MIPMAP_NEAREST(GL11.GL_NEAREST_MIPMAP_NEAREST, true),
        LINEAR_MIPMAP_NEAREST(GL11.GL_LINEAR_MIPMAP_NEAREST, true),
        NEAREST_MIPMAP_LINEAR(GL11.GL_NEAREST_MIPMAP_LINEAR, true),
        LINEAR_MIPMAP_LINEAR(GL11.GL_LINEAR_MIPMAP_LINEAR, true);

        public int getConstant ()
        {
            return _constant;
        }

        public boolean isMipmapped ()
        {
            return _mipmapped;
        }

        MinFilter (int constant, boolean mipmapped)
        {
            _constant = constant;
            _mipmapped = mipmapped;
        }

        protected int _constant;
        protected boolean _mipmapped;
    }

    /** Magnification filter constants. */
    public enum MagFilter
    {
        NEAREST(GL11.GL_NEAREST),
        LINEAR(GL11.GL_LINEAR);

        public int getConstant ()
        {
            return _constant;
        }

        MagFilter (int constant)
        {
            _constant = constant;
        }

        protected int _constant;
    }

    /** Wrap constants. */
    public enum Wrap
    {
        CLAMP(GL11.GL_CLAMP),
        CLAMP_TO_EDGE(GL12.GL_CLAMP_TO_EDGE) {
            public int getConstant () {
                return GLContext.getCapabilities().OpenGL12 ? _constant : GL11.GL_CLAMP;
            }
            public boolean isSupported (boolean fallback) {
                return GLContext.getCapabilities().OpenGL12 || fallback;
            }
        },
        REPEAT(GL11.GL_REPEAT),
        CLAMP_TO_BORDER(ARBTextureBorderClamp.GL_CLAMP_TO_BORDER_ARB) {
            public int getConstant () {
                return GLContext.getCapabilities().GL_ARB_texture_border_clamp ?
                    _constant : GL11.GL_CLAMP;
            }
            public boolean isSupported (boolean fallback) {
                return GLContext.getCapabilities().GL_ARB_texture_border_clamp || fallback;
            }
        },
        MIRRORED_REPEAT(ARBTextureMirroredRepeat.GL_MIRRORED_REPEAT_ARB) {
            public boolean isSupported (boolean fallback) {
                return GLContext.getCapabilities().GL_ARB_texture_mirrored_repeat;
            }
        };

        public int getConstant ()
        {
            return _constant;
        }

        public boolean isSupported (boolean fallback)
        {
            return true;
        }

        Wrap (int constant)
        {
            _constant = constant;
        }

        protected int _constant;
    }

    /** Compare mode constants. */
    public enum CompareMode
    {
        NONE(GL11.GL_NONE),
        COMPARE_R_TO_TEXTURE(ARBShadow.GL_COMPARE_R_TO_TEXTURE_ARB) {
            public boolean isSupported (boolean fallback) {
                return GLContext.getCapabilities().GL_ARB_shadow;
            }
        };

        public int getConstant ()
        {
            return _constant;
        }

        public boolean isSupported (boolean fallback)
        {
            return true;
        }

        CompareMode (int constant)
        {
            _constant = constant;
        }

        protected int _constant;
    }

    /** Compare function constants. */
    public enum CompareFunc
    {
        LEQUAL(GL11.GL_LEQUAL),
        GEQUAL(GL11.GL_GEQUAL);

        public int getConstant ()
        {
            return _constant;
        }

        CompareFunc (int constant)
        {
            _constant = constant;
        }

        protected int _constant;
    }

    /** Depth mode constants. */
    public enum DepthMode
    {
        LUMINANCE(GL11.GL_LUMINANCE),
        INTENSITY(GL11.GL_INTENSITY),
        ALPHA(GL11.GL_ALPHA);

        public int getConstant ()
        {
            return _constant;
        }

        DepthMode (int constant)
        {
            _constant = constant;
        }

        protected int _constant;
    }

    /**
     * Contains the actual implementation of the texture.
     */
    @EditorTypes({
        Original1D.class, Original2D.class, Original2DTarget.class, OriginalRectangle.class,
        Original3D.class, OriginalCubeMap.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Adds the implementation's update references to the provided set.
         */
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            // nothing by default
        }

        /**
         * Adds the implementation's update resources to the provided set.
         */
        public void getUpdateResources (HashSet<String> paths)
        {
            // nothing by default
        }

        /**
         * Determines whether this configuration is supported by the hardware.
         */
        public abstract boolean isSupported (GlContext ctx, boolean fallback);

        /**
         * Returns the texture corresponding to this configuration.
         */
        public abstract Texture getTexture (GlContext ctx);

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            // nothing by default
        }
    }

    /**
     * The superclass of the implementations describing an original texture, as opposed to one
     * derived from another configuration.
     */
    public static abstract class Original extends Implementation
    {
        /** The texture format. */
        @Editable(category="data")
        public Format format = Format.DEFAULT;

        /** Used to create separate instances of the same configuration. */
        @Editable(category="data")
        public String identity = "";

        /** The minification filter. */
        @Editable(category="filter", hgroup="f")
        public MinFilter minFilter = MinFilter.LINEAR_MIPMAP_LINEAR;

        /** The magnification filter. */
        @Editable(category="filter", hgroup="f")
        public MagFilter magFilter = MagFilter.LINEAR;

        /** The maximum degree of anisotropy. */
        @Editable(min=1.0, step=0.01, category="filter")
        public float maxAnisotropy = 1f;

        /** The s wrap mode. */
        @Editable(category="wrap", hgroup="w")
        public Wrap wrapS = Wrap.REPEAT;

        /** The t wrap mode. */
        @Editable(category="wrap", hgroup="w")
        public Wrap wrapT = Wrap.REPEAT;

        /** The r wrap mode. */
        @Editable(category="wrap", hgroup="w")
        public Wrap wrapR = Wrap.REPEAT;

        /** Whether or not the texture has a border. */
        @Editable(category="wrap", hgroup="b")
        public boolean border;

        /** The border color. */
        @Editable(mode="alpha", category="wrap", hgroup="b")
        public Color4f borderColor = new Color4f(0f, 0f, 0f, 0f);

        /** The texture compare mode. */
        @Editable(category="compare", hgroup="c")
        public CompareMode compareMode = CompareMode.NONE;

        /** The texture compare function. */
        @Editable(category="compare", hgroup="c")
        public CompareFunc compareFunc = CompareFunc.LEQUAL;

        /** The depth texture mode. */
        @Editable(category="compare")
        public DepthMode depthMode = DepthMode.LUMINANCE;

        @Override // documentation inherited
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            return format.isSupported(fallback) && wrapS.isSupported(fallback) &&
                wrapT.isSupported(fallback) && wrapR.isSupported(fallback) &&
                compareMode.isSupported(fallback);
        }

        @Override // documentation inherited
        public Texture getTexture (GlContext ctx)
        {
            Texture texture = (_texture == null) ? null : _texture.get();
            if (texture == null) {
                _texture = new SoftReference<Texture>(texture = createTexture(ctx));
                texture.setFilters(minFilter.getConstant(), magFilter.getConstant());
                texture.setMaxAnisotropy(maxAnisotropy);
                texture.setWrap(wrapS.getConstant(), wrapT.getConstant(), wrapR.getConstant());
                texture.setBorderColor(borderColor);
                texture.setCompare(compareMode.getConstant(), compareFunc.getConstant());
                texture.setDepthMode(depthMode.getConstant());
            }
            return texture;
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            _texture = null;
        }

        /**
         * Creates the texture for this configuration.
         */
        protected abstract Texture createTexture (GlContext ctx);

        /** The texture corresponding to this configuration. */
        @DeepOmit
        protected transient SoftReference<Texture> _texture;
    }

    /**
     * A 1D texture.
     */
    public static class Original1D extends Original
    {
        /**
         * The initial contents of the texture.
         */
        @EditorTypes({ Blank.class, ImageFile.class })
        public static abstract class Contents extends DeepObject
            implements Exportable
        {
            /**
             * Adds the contents' update resources to the provided set.
             */
            public void getUpdateResources (HashSet<String> paths)
            {
                // nothing by default
            }

            /**
             * Loads the texture with the contents.
             */
            public abstract void load (
                GlContext ctx, Texture1D texture, Format format, boolean border, boolean mipmap);
        }

        /**
         * Creates a blank texture.
         */
        public static class Blank extends Contents
        {
            /** The width of the texture. */
            @Editable(min=1)
            public int width = 1;

            @Override // documentation inherited
            public void load (
                GlContext ctx, Texture1D texture, Format format, boolean border, boolean mipmap)
            {
                texture.setImage(format.getConstant(null), width, border, mipmap);
            }
        }

        /**
         * Creates a texture from the specified image file.
         */
        public static class ImageFile extends Contents
        {
            /** The image resource from which to load the texture. */
            @Editable(editor="resource", nullable=true)
            @FileConstraints(
                description="m.image_files_desc",
                extensions={".png", ".jpg"},
                directory="image_dir")
            public String file;

            /** The colorizations to apply to the texture. */
            @Editable
            public ColorizationReference[] colorizations = new ColorizationReference[0];

            /** Whether or not the image alpha should be premultiplied. */
            @Editable
            public boolean premultiply = true;

            @Override // documentation inherited
            public void getUpdateResources (HashSet<String> paths)
            {
                if (file != null) {
                    paths.add(file);
                }
            }

            @Override // documentation inherited
            public void load (
                GlContext ctx, Texture1D texture, Format format, boolean border, boolean mipmap)
            {
                if (file == null) {
                    return;
                }
                BufferedImage image = getImage(ctx, file, colorizations);
                texture.setImage(
                    format.getConstant(image), border, image, premultiply, true, mipmap);
            }
        }

        /** The initial contents of the texture. */
        @Editable(category="data")
        public Contents contents = new ImageFile();

        @Override // documentation inherited
        public void getUpdateResources (HashSet<String> paths)
        {
            contents.getUpdateResources(paths);
        }

        @Override // documentation inherited
        protected Texture createTexture (GlContext ctx)
        {
            Texture1D texture = new Texture1D(ctx.getRenderer());
            contents.load(ctx, texture, format, border, minFilter.isMipmapped());
            return texture;
        }
    }

    /**
     * A 2D texture.
     */
    public static class Original2D extends Original
    {
        /**
         * The initial contents of the texture.
         */
        @EditorTypes({ Blank.class, ImageFile.class })
        public static abstract class Contents extends DeepObject
            implements Exportable
        {
            /**
             * Adds the contents' update resources to the provided set.
             */
            public void getUpdateResources (HashSet<String> paths)
            {
                // nothing by default
            }

            /**
             * Loads the texture with the contents.
             */
            public abstract void load (
                GlContext ctx, Texture2D texture, Format format, boolean border, boolean mipmap);
        }

        /**
         * Creates a blank texture.
         */
        public static class Blank extends Contents
        {
            /** The width of the texture. */
            @Editable(min=1, hgroup="d")
            public int width = 1;

            /** The height of the texture. */
            @Editable(min=1, hgroup="d")
            public int height = 1;

            @Override // documentation inherited
            public void load (
                GlContext ctx, Texture2D texture, Format format, boolean border, boolean mipmap)
            {
                texture.setImage(format.getConstant(null), width, height, border, mipmap);
            }
        }

        /**
         * Creates a texture from the specified image.
         */
        public static class ImageFile extends Contents
        {
            /** The image resource from which to load the texture. */
            @Editable(editor="resource", nullable=true)
            @FileConstraints(
                description="m.image_files_desc",
                extensions={".png", ".jpg"},
                directory="image_dir")
            public String file;

            /** The colorizations to apply to the texture. */
            @Editable
            public ColorizationReference[] colorizations = new ColorizationReference[0];

            /** Whether or not the image alpha should be premultiplied. */
            @Editable
            public boolean premultiply = true;

            @Override // documentation inherited
            public void getUpdateResources (HashSet<String> paths)
            {
                if (file != null) {
                    paths.add(file);
                }
            }

            @Override // documentation inherited
            public void load (
                GlContext ctx, Texture2D texture, Format format, boolean border, boolean mipmap)
            {
                if (file == null) {
                    return;
                }
                BufferedImage image = getImage(ctx, file, colorizations);
                texture.setImage(
                    format.getConstant(image), border, image, premultiply, true, mipmap);
            }
        }

        /** The initial contents of the texture. */
        @Editable(category="data")
        public Contents contents = new ImageFile();

        @Override // documentation inherited
        public void getUpdateResources (HashSet<String> paths)
        {
            contents.getUpdateResources(paths);
        }

        @Override // documentation inherited
        protected Texture createTexture (GlContext ctx)
        {
            Texture2D texture = new Texture2D(ctx.getRenderer(), false);
            contents.load(ctx, texture, format, border, minFilter.isMipmapped());
            return texture;
        }
    }

    /**
     * A 2D texture that matches the dimensions of the render surface.
     */
    public static class Original2DTarget extends Original
    {
        @Override // documentation inherited
        protected Texture createTexture (GlContext ctx)
        {
            return new Texture2DTarget(ctx.getRenderer(), format, border, minFilter.isMipmapped());
        }
    }

    /**
     * A 2D texture that uses the rectangle target.
     */
    public static class OriginalRectangle extends Original2D
    {
        @Override // documentation inherited
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            return super.isSupported(ctx, fallback) &&
                GLContext.getCapabilities().GL_ARB_texture_rectangle;
        }

        @Override // documentation inherited
        protected Texture createTexture (GlContext ctx)
        {
            Texture2D texture = new Texture2D(ctx.getRenderer(), true);
            contents.load(ctx, texture, format, border, minFilter.isMipmapped());
            return texture;
        }
    }

    /**
     * A 3D texture.
     */
    public static class Original3D extends Original
    {
        /**
         * The initial contents of the texture.
         */
        @EditorTypes({ Blank.class, ImageFile.class })
        public static abstract class Contents extends DeepObject
            implements Exportable
        {
            /**
             * Adds the contents' update resources to the provided set.
             */
            public void getUpdateResources (HashSet<String> paths)
            {
                // nothing by default
            }

            /**
             * Loads the texture with the contents.
             */
            public abstract void load (
                GlContext ctx, Texture3D texture, Format format, boolean border, boolean mipmap);
        }

        /**
         * Creates a blank texture.
         */
        public static class Blank extends Contents
        {
            /** The width of the texture. */
            @Editable(min=1, hgroup="d")
            public int width = 1;

            /** The height of the texture. */
            @Editable(min=1, hgroup="d")
            public int height = 1;

            /** The depth of the texture. */
            @Editable(min=1, hgroup="d")
            public int depth = 1;

            @Override // documentation inherited
            public void load (
                GlContext ctx, Texture3D texture, Format format, boolean border, boolean mipmap)
            {
                texture.setImage(format.getConstant(null), width, height, depth, border, mipmap);
            }
        }

        /**
         * Creates a texture from the specified image.
         */
        public static class ImageFile extends Contents
        {
            /** The image resource from which to load the texture. */
            @Editable(editor="resource", nullable=true)
            @FileConstraints(
                description="m.image_files_desc",
                extensions={".png", ".jpg"},
                directory="image_dir")
            public String file;

            /** The colorizations to apply to the texture. */
            @Editable
            public ColorizationReference[] colorizations = new ColorizationReference[0];

            /** Whether or not the image alpha should be premultiplied. */
            @Editable
            public boolean premultiply = true;

            /** The number of divisions in the S direction. */
            @Editable(min=1, hgroup="d")
            public int divisionsS = 1;

            /** The number of divisions in the T direction. */
            @Editable(min=1, hgroup="d")
            public int divisionsT = 1;

            /** The depth of the image. */
            @Editable(min=1, hgroup="d")
            public int depth = 1;

            @Override // documentation inherited
            public void getUpdateResources (HashSet<String> paths)
            {
                if (file != null) {
                    paths.add(file);
                }
            }

            @Override // documentation inherited
            public void load (
                GlContext ctx, Texture3D texture, Format format, boolean border, boolean mipmap)
            {
                if (file == null) {
                    return;
                }
                BufferedImage image = getImage(ctx, file, colorizations);
                texture.setImages(
                    format.getConstant(image), border, image, divisionsS, divisionsT,
                    depth, premultiply, true, mipmap);
            }
        }

        /** The initial contents of the texture. */
        @Editable(category="data")
        public Contents contents = new ImageFile();

        @Override // documentation inherited
        public void getUpdateResources (HashSet<String> paths)
        {
            contents.getUpdateResources(paths);
        }

        @Override // documentation inherited
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            return super.isSupported(ctx, fallback) && GLContext.getCapabilities().OpenGL12;
        }

        @Override // documentation inherited
        protected Texture createTexture (GlContext ctx)
        {
            Texture3D texture = new Texture3D(ctx.getRenderer());
            contents.load(ctx, texture, format, border, minFilter.isMipmapped());
            return texture;
        }
    }

    /**
     * A cube map texture.
     */
    public static class OriginalCubeMap extends Original
    {
        /**
         * The initial contents of the texture.
         */
        @EditorTypes({ Blank.class, ImageFile.class, ImageFiles.class })
        public static abstract class Contents extends DeepObject
            implements Exportable
        {
            /**
             * Adds the contents' update resources to the provided set.
             */
            public void getUpdateResources (HashSet<String> paths)
            {
                // nothing by default
            }

            /**
             * Loads the texture with the contents.
             */
            public abstract void load (
                GlContext ctx, TextureCubeMap texture, Format format,
                boolean border, boolean mipmap);
        }

        /**
         * Creates a blank texture.
         */
        public static class Blank extends Contents
        {
            /** The size of the textures. */
            @Editable(min=1)
            public int size = 1;

            @Override // documentation inherited
            public void load (
                GlContext ctx, TextureCubeMap texture, Format format,
                boolean border, boolean mipmap)
            {
                texture.setImages(format.getConstant(null), size, border, mipmap);
            }
        }

        /**
         * Creates a texture from the specified image.
         */
        public static class ImageFile extends Contents
        {
            /** The image resource from which to load the texture. */
            @Editable(editor="resource", nullable=true)
            @FileConstraints(
                description="m.image_files_desc",
                extensions={".png", ".jpg"},
                directory="image_dir")
            public String file;

            /** The colorizations to apply to the texture. */
            @Editable
            public ColorizationReference[] colorizations = new ColorizationReference[0];

            /** Whether or not the image alpha should be premultiplied. */
            @Editable
            public boolean premultiply = true;

            /** The number of divisions in the S direction. */
            @Editable(min=1, max=6, hgroup="d")
            public int divisionsS = 3;

            /** The number of divisions in the T direction. */
            @Editable(min=1, max=6, hgroup="d")
            public int divisionsT = 2;

            @Override // documentation inherited
            public void getUpdateResources (HashSet<String> paths)
            {
                if (file != null) {
                    paths.add(file);
                }
            }

            @Override // documentation inherited
            public void load (
                GlContext ctx, TextureCubeMap texture, Format format,
                boolean border, boolean mipmap)
            {
                if (file == null) {
                    return;
                }
                BufferedImage image = getImage(ctx, file, colorizations);
                texture.setImages(
                    format.getConstant(image), border, image, divisionsS, divisionsT,
                    premultiply, true, mipmap);
            }
        }

        /**
         * Creates a texture from the specified images.
         */
        public static class ImageFiles extends Contents
        {
            /** The negative x, y, and z face files. */
            @Editable
            public FileTrio negative = new FileTrio();

            /** The positive x, y, and z face files. */
            @Editable
            public FileTrio positive = new FileTrio();

            /** The colorizations to apply to the textures. */
            @Editable
            public ColorizationReference[] colorizations = new ColorizationReference[0];

            /** Whether or not the image alpha should be premultiplied. */
            @Editable
            public boolean premultiply = true;

            @Override // documentation inherited
            public void getUpdateResources (HashSet<String> paths)
            {
                negative.getUpdateResources(paths);
                positive.getUpdateResources(paths);
            }

            @Override // documentation inherited
            public void load (
                GlContext ctx, TextureCubeMap texture, Format format,
                boolean border, boolean mipmap)
            {
                BufferedImage[] images = new BufferedImage[] {
                    (positive.x == null) ? null : getImage(ctx, positive.x, colorizations),
                    (negative.x == null) ? null : getImage(ctx, negative.x, colorizations),
                    (positive.y == null) ? null : getImage(ctx, positive.y, colorizations),
                    (negative.y == null) ? null : getImage(ctx, negative.y, colorizations),
                    (positive.z == null) ? null : getImage(ctx, positive.z, colorizations),
                    (negative.z == null) ? null : getImage(ctx, negative.z, colorizations)
                };
                int constant = -1;
                for (BufferedImage image : images) {
                    if (image != null) {
                        constant = format.getConstant(image);
                        break;
                    }
                }
                if (constant == -1) {
                    return;
                }
                texture.setImages(constant, border, images, premultiply, true, mipmap);
            }
        }

        /**
         * Contains the files for the x, y, and z faces.
         */
        public static class FileTrio extends DeepObject
            implements Exportable
        {
            /** The image resource from which to load the x face. */
            @Editable(editor="resource", nullable=true)
            @FileConstraints(
                description="m.image_files_desc",
                extensions={".png", ".jpg"},
                directory="image_dir")
            public String x;

            /** The image resource from which to load the y face. */
            @Editable(editor="resource", nullable=true)
            @FileConstraints(
                description="m.image_files_desc",
                extensions={".png", ".jpg"},
                directory="image_dir")
            public String y;

            /** The image resource from which to load the z face. */
            @Editable(editor="resource", nullable=true)
            @FileConstraints(
                description="m.image_files_desc",
                extensions={".png", ".jpg"},
                directory="image_dir")
            public String z;

            /**
             * Adds the trio's update resources to the provided set.
             */
            public void getUpdateResources (HashSet<String> paths)
            {
                if (x != null) {
                    paths.add(x);
                }
                if (y != null) {
                    paths.add(y);
                }
                if (z != null) {
                    paths.add(z);
                }
            }
        }

        /** The initial contents of the texture. */
        @Editable(category="data")
        public Contents contents = new ImageFiles();

        @Override // documentation inherited
        public void getUpdateResources (HashSet<String> paths)
        {
            contents.getUpdateResources(paths);
        }

        @Override // documentation inherited
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            return super.isSupported(ctx, fallback) &&
                GLContext.getCapabilities().GL_ARB_texture_cube_map;
        }

        @Override // documentation inherited
        protected Texture createTexture (GlContext ctx)
        {
            TextureCubeMap texture = new TextureCubeMap(ctx.getRenderer());
            contents.load(ctx, texture, format, border, minFilter.isMipmapped());
            return texture;
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The texture reference. */
        @Editable(nullable=true)
        public ConfigReference<TextureConfig> texture;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(TextureConfig.class, texture);
        }

        @Override // documentation inherited
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            TextureConfig config = ctx.getConfigManager().getConfig(TextureConfig.class, texture);
            return config == null || config.isSupported(ctx, fallback);
        }

        @Override // documentation inherited
        public Texture getTexture (GlContext ctx)
        {
            TextureConfig config = ctx.getConfigManager().getConfig(TextureConfig.class, texture);
            return (config == null) ? null : config.getTexture(ctx);
        }
    }

    /**
     * A reference to a colorization.
     */
    public static class ColorizationReference extends DeepObject
        implements Exportable, Streamable
    {
        /** The colorization reference. */
        @Editable(editor="colorization")
        public int colorization;
    }

    /** The actual texture implementation. */
    @Editable
    public Implementation implementation = new Original2D();

    /**
     * Gets the requested image from the cache and applies the specified colorizations.
     */
    public static BufferedImage getImage (
        GlContext ctx, String file, ColorizationReference[] colorizations)
    {
        Colorization[] zations = new Colorization[colorizations.length];
        for (int ii = 0; ii < zations.length; ii++) {
            zations[ii] = ctx.getColorPository().getColorization(
                colorizations[ii].colorization);
        }
        return ctx.getImageCache().getBufferedImage(file, zations);
    }

    /**
     * Checks whether the texture configuration is supported.
     */
    public boolean isSupported (GlContext ctx, boolean fallback)
    {
        return implementation.isSupported(ctx, fallback);
    }

    /**
     * Returns the texture corresponding to this configuration.
     */
    public Texture getTexture (GlContext ctx)
    {
        return implementation.getTexture(ctx);
    }

    @Override // documentation inherited
    protected void fireConfigUpdated ()
    {
        // invalidate the implementation
        implementation.invalidate();
        super.fireConfigUpdated();
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }

    @Override // documentation inherited
    protected void getUpdateResources (HashSet<String> paths)
    {
        implementation.getUpdateResources(paths);
    }

    /**
     * A texture that automatically adjusts itself to match the dimensions of the render surface.
     */
    protected static class Texture2DTarget extends Texture2D
        implements Renderer.Observer
    {
        public Texture2DTarget (Renderer renderer, Format format, boolean border, boolean mipmap)
        {
            super(renderer);
            _format = format;
            _border = border;
            _mipmap = mipmap;

            sizeChanged(renderer.getWidth(), renderer.getHeight());
            renderer.addObserver(this);
        }

        // documentation inherited from interface Renderer.Observer
        public void sizeChanged (int width, int height)
        {
            setImage(_format.getConstant(null), width, height, _border, _mipmap);
        }

        /** The requested format. */
        protected Format _format;

        /** Whether or not to include a border. */
        protected boolean _border;

        /** Whether or not to define mipmaps. */
        protected boolean _mipmap;
    }
}
