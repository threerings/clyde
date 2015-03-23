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

package com.threerings.opengl.renderer.config;

import java.awt.image.BufferedImage;

import java.io.IOException;

import java.lang.ref.SoftReference;

import java.util.HashSet;
import java.util.List;

import org.lwjgl.opengl.ARBShadow;
import org.lwjgl.opengl.ARBTextureBorderClamp;
import org.lwjgl.opengl.ARBTextureCompression;
import org.lwjgl.opengl.ARBTextureMirroredRepeat;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;

import com.google.common.collect.Lists;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.expr.FloatExpression;
import com.threerings.expr.Scope;
import com.threerings.expr.Updater;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Box;
import com.threerings.math.FloatMath;
import com.threerings.math.Plane;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.media.image.Colorization;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.camera.Camera;
import com.threerings.opengl.compositor.Compositor;
import com.threerings.opengl.compositor.Dependency;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.Texture;
import com.threerings.opengl.renderer.Texture1D;
import com.threerings.opengl.renderer.Texture2D;
import com.threerings.opengl.renderer.Texture3D;
import com.threerings.opengl.renderer.TextureCubeMap;
import com.threerings.opengl.renderer.TextureUnit;
import com.threerings.opengl.renderer.state.TextureState;
import com.threerings.opengl.scene.config.ShadowConfig;
import com.threerings.opengl.util.DDSLoader;
import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.Log.log;

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

        protected final int _constant;
        protected final int _uncompressed;
        protected final boolean _depth;
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

        protected final int _constant;
        protected final boolean _mipmapped;
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

        protected final int _constant;
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
        MIRRORED_REPEAT(GL14.GL_MIRRORED_REPEAT) {
            public int getConstant () {
                if (GLContext.getCapabilities().OpenGL14) {
                    return _constant;
                } else if (GLContext.getCapabilities().GL_ARB_texture_mirrored_repeat) {
                    return ARBTextureMirroredRepeat.GL_MIRRORED_REPEAT_ARB;
                } else {
                    return GL11.GL_REPEAT;
                }
            }
            public boolean isSupported (boolean fallback) {
                return GLContext.getCapabilities().OpenGL14 ||
                    GLContext.getCapabilities().GL_ARB_texture_mirrored_repeat || fallback;
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

        protected final int _constant;
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

        protected final int _constant;
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

        protected final int _constant;
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

        protected final int _constant;
    }

    /**
     * Contains the actual implementation of the texture.
     */
    @EditorTypes({
        Original1D.class, Original2D.class, Original2DTarget.class, OriginalRectangle.class,
        Original3D.class, OriginalCubeMap.class, Reflection.class, Refraction.class,
        CubeRender.class, Shadow.class, Animated.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        @Deprecated
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
        public Texture getTexture (GlContext ctx)
        {
            return getTexture(ctx, null, null, null, null, null);
        }

        /**
         * Returns the (possibly dynamic) texture corresponding to this configuration.
         */
        public abstract Texture getTexture (
            GlContext ctx, TextureState state, TextureUnit unit,
            Scope scope, List<Dependency.Adder> adders, List<Updater> updaters);

        /**
         * Fetches a texture from the shared pool, or returns <code>null</code> if the
         * implementation doesn't contain a pool.
         */
        public Texture getFromPool (GlContext ctx)
        {
            return null;
        }

        /**
         * Returns a texture to the shared pool.
         */
        public void returnToPool (GlContext ctx, Texture texture)
        {
            // no-op
        }

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

        @Override
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            return format.isSupported(fallback) && wrapS.isSupported(fallback) &&
                wrapT.isSupported(fallback) && wrapR.isSupported(fallback) &&
                compareMode.isSupported(fallback);
        }

        @Override
        public Texture getTexture (
            GlContext ctx, TextureState state, TextureUnit unit,
            Scope scope, List<Dependency.Adder> adders, List<Updater> updaters)
        {
            Texture texture = (_texture == null) ? null : _texture.get();
            if (texture == null) {
                _texture = new SoftReference<Texture>(texture = createTexture(ctx));
                configureTexture(texture);
            }
            return texture;
        }

        @Override
        public Texture getFromPool (GlContext ctx)
        {
            List<SoftReference<Texture>> pool = getPool();
            for (int ii = pool.size() - 1; ii >= 0; ii--) {
                Texture texture = pool.remove(ii).get();
                if (texture != null) {
                    return texture;
                }
            }
            Texture texture = createTexture(ctx);
            configureTexture(texture);
            return texture;
        }

        @Override
        public void returnToPool (GlContext ctx, Texture texture)
        {
            getPool().add(new SoftReference<Texture>(texture));
        }

        @Override
        public void invalidate ()
        {
            _texture = null;
            _pool = null;
        }

        /**
         * Creates the texture for this configuration.
         */
        protected abstract Texture createTexture (GlContext ctx);

        /**
         * Configures the supplied texture with this config's parameters.
         */
        protected void configureTexture (Texture texture)
        {
            texture.setFilters(minFilter.getConstant(), magFilter.getConstant());
            texture.setMaxAnisotropy(maxAnisotropy);
            texture.setWrap(wrapS.getConstant(), wrapT.getConstant(), wrapR.getConstant());
            texture.setBorderColor(borderColor);
            texture.setCompare(compareMode.getConstant(), compareFunc.getConstant());
            texture.setDepthMode(depthMode.getConstant());
        }

        /**
         * Returns a reference to the (lazily created) texture pool.
         */
        protected List<SoftReference<Texture>> getPool ()
        {
            if (_pool == null) {
                _pool = Lists.newArrayList();
            }
            return _pool;
        }

        /** The texture corresponding to this configuration. */
        @DeepOmit
        protected transient SoftReference<Texture> _texture;

        /** The pool of unique texture instances. */
        @DeepOmit
        protected transient List<SoftReference<Texture>> _pool;
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

            @Override
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
                extensions={".png", ".jpg", ".dds"},
                directory="image_dir")
            public String file;

            /** The colorizations to apply to the texture. */
            @Editable
            public ColorizationConfig[] colorizations = new ColorizationConfig[0];

            /** Whether or not the image alpha should be premultiplied. */
            @Editable
            public boolean premultiply = true;

            @Override
            public void getUpdateResources (HashSet<String> paths)
            {
                if (file != null) {
                    paths.add(file);
                }
            }

            @Override
            public void load (
                GlContext ctx, Texture1D texture, Format format, boolean border, boolean mipmap)
            {
                if (file == null) {
                    return;
                }
                if (file.endsWith(".dds")) {
                    try {
                        DDSLoader.load(ctx.getResourceManager().getResourceFile(file),
                            texture, border);
                        return;
                    } catch (IOException e) {
                        // fall through to the buffered image loader
                    }
                }
                BufferedImage image = getImage(ctx, file, colorizations);
                texture.setImage(
                    format.getConstant(image), border, image, premultiply, true, mipmap);
            }
        }

        /** The initial contents of the texture. */
        @Editable(category="data")
        public Contents contents = new ImageFile();

        @Override
        public void getUpdateResources (HashSet<String> paths)
        {
            contents.getUpdateResources(paths);
        }

        @Override
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

            @Override
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
                extensions={".png", ".jpg", ".dds"},
                directory="image_dir")
            public String file;

            /** The colorizations to apply to the texture. */
            @Editable
            public ColorizationConfig[] colorizations = new ColorizationConfig[0];

            /** Whether or not the image alpha should be premultiplied. */
            @Editable
            public boolean premultiply = true;

            @Override
            public void getUpdateResources (HashSet<String> paths)
            {
                if (file != null) {
                    paths.add(file);
                }
            }

            @Override
            public void load (
                GlContext ctx, Texture2D texture, Format format, boolean border, boolean mipmap)
            {
                if (file == null) {
                    return;
                }
                if (file.endsWith(".dds")) {
                    try {
                        DDSLoader.load(ctx.getResourceManager().getResourceFile(file),
                            texture, border);
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                        // fall through to the buffered image loader
                    }
                }
                BufferedImage image = getImage(ctx, file, colorizations);
                texture.setImage(
                    format.getConstant(image), border, image, premultiply, true, mipmap);
            }
        }

        /** The initial contents of the texture. */
        @Editable(category="data")
        public Contents contents = new ImageFile();

        @Override
        public void getUpdateResources (HashSet<String> paths)
        {
            contents.getUpdateResources(paths);
        }

        @Override
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
        @Override
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
        @Override
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            return super.isSupported(ctx, fallback) &&
                GLContext.getCapabilities().GL_ARB_texture_rectangle;
        }

        @Override
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

            @Override
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
                extensions={".png", ".jpg", ".dds"},
                directory="image_dir")
            public String file;

            /** The colorizations to apply to the texture. */
            @Editable
            public ColorizationConfig[] colorizations = new ColorizationConfig[0];

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

            @Override
            public void getUpdateResources (HashSet<String> paths)
            {
                if (file != null) {
                    paths.add(file);
                }
            }

            @Override
            public void load (
                GlContext ctx, Texture3D texture, Format format, boolean border, boolean mipmap)
            {
                if (file == null) {
                    return;
                }
                if (file.endsWith(".dds")) {
                    try {
                        DDSLoader.load(ctx.getResourceManager().getResourceFile(file),
                            texture, border);
                        return;
                    } catch (IOException e) {
                        // fall through to the buffered image loader
                    }
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

        @Override
        public void getUpdateResources (HashSet<String> paths)
        {
            contents.getUpdateResources(paths);
        }

        @Override
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            return super.isSupported(ctx, fallback) && GLContext.getCapabilities().OpenGL12;
        }

        @Override
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

            @Override
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
                extensions={".png", ".jpg", ".dds"},
                directory="image_dir")
            public String file;

            /** The colorizations to apply to the texture. */
            @Editable
            public ColorizationConfig[] colorizations = new ColorizationConfig[0];

            /** Whether or not the image alpha should be premultiplied. */
            @Editable
            public boolean premultiply = true;

            /** The number of divisions in the S direction. */
            @Editable(min=1, max=6, hgroup="d")
            public int divisionsS = 3;

            /** The number of divisions in the T direction. */
            @Editable(min=1, max=6, hgroup="d")
            public int divisionsT = 2;

            @Override
            public void getUpdateResources (HashSet<String> paths)
            {
                if (file != null) {
                    paths.add(file);
                }
            }

            @Override
            public void load (
                GlContext ctx, TextureCubeMap texture, Format format,
                boolean border, boolean mipmap)
            {
                if (file == null) {
                    return;
                }
                if (file.endsWith(".dds")) {
                    try {
                        DDSLoader.load(ctx.getResourceManager().getResourceFile(file),
                            texture, border);
                        return;
                    } catch (IOException e) {
                        // fall through to the buffered image loader
                    }
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
            public ColorizationConfig[] colorizations = new ColorizationConfig[0];

            /** Whether or not the image alpha should be premultiplied. */
            @Editable
            public boolean premultiply = true;

            @Override
            public void getUpdateResources (HashSet<String> paths)
            {
                negative.getUpdateResources(paths);
                positive.getUpdateResources(paths);
            }

            @Override
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
                extensions={".png", ".jpg", ".dds"},
                directory="image_dir")
            public String x;

            /** The image resource from which to load the y face. */
            @Editable(editor="resource", nullable=true)
            @FileConstraints(
                description="m.image_files_desc",
                extensions={".png", ".jpg", ".dds"},
                directory="image_dir")
            public String y;

            /** The image resource from which to load the z face. */
            @Editable(editor="resource", nullable=true)
            @FileConstraints(
                description="m.image_files_desc",
                extensions={".png", ".jpg", ".dds"},
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

        @Override
        public void getUpdateResources (HashSet<String> paths)
        {
            contents.getUpdateResources(paths);
        }

        @Override
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            return super.isSupported(ctx, fallback) &&
                GLContext.getCapabilities().GL_ARB_texture_cube_map;
        }

        @Override
        protected Texture createTexture (GlContext ctx)
        {
            TextureCubeMap texture = new TextureCubeMap(ctx.getRenderer());
            contents.load(ctx, texture, format, border, minFilter.isMipmapped());
            return texture;
        }
    }

    /**
     * Base class of {@link Derived} and related implementations.
     */
    public static abstract class BaseDerived extends Implementation
    {
        /** The texture reference. */
        @Editable(nullable=true)
        public ConfigReference<TextureConfig> texture;

        @Override
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            TextureConfig config = getConfig(ctx);
            return config == null || config.isSupported(ctx, fallback);
        }

        @Override
        public Texture getTexture (
            GlContext ctx, TextureState state, TextureUnit unit,
            Scope scope, List<Dependency.Adder> adders, List<Updater> updaters)
        {
            TextureConfig config = getConfig(ctx);
            return (config == null) ? null :
                config.getTexture(ctx, state, unit, scope, adders, updaters);
        }

        /**
         * Attempts to resolve the texture config.
         */
        protected TextureConfig getConfig (GlContext ctx)
        {
            return ctx.getConfigManager().getConfig(TextureConfig.class, texture);
        }
    }

    /**
     * A planar reflection texture.
     */
    public static class Reflection extends BaseDerived
    {
        /** Whether to enable rendering to the front and back. */
        @Editable(hgroup="f")
        public boolean front = true, back;

        /** The maximum allowable depth. */
        @Editable(min=0, hgroup="f")
        public int maxDepth;

        @Override
        public Texture getTexture (
            final GlContext ctx, final TextureState state, final TextureUnit unit,
            Scope scope, List<Dependency.Adder> adders, List<Updater> updaters)
        {
            if (adders == null) {
                log.warning("Tried to create reflection texture in static context.");
                return null;
            }
            final TextureConfig config = getConfig(ctx);
            if (config == null) {
                return null;
            }
            final IntMap<Dependency.ReflectionTexture> dependencies = IntMaps.newHashIntMap();
            final Transform3D transform = ScopeUtil.resolve(
                scope, "worldTransform", new Transform3D());
            final Box bounds = ScopeUtil.resolve(scope, "bounds", new Box(), Box.class);
            adders.add(new Dependency.Adder() {
                public boolean add () {
                    Compositor compositor = ctx.getCompositor();
                    int depth = compositor.getSubrenderDepth();
                    Object source = compositor.getSubrenderSource();
                    if (depth > maxDepth ||
                            (source != null && source == dependencies.get(depth - 1))) {
                        return false;
                    }
                    Dependency.ReflectionTexture dependency = dependencies.get(depth);
                    if (dependency == null) {
                        dependencies.put(depth,
                            dependency = new Dependency.ReflectionTexture(ctx));
                    }
                    Plane.XY_PLANE.transform(transform, dependency.worldPlane);
                    Plane eyePlane = dependency.eyePlane;
                    Camera camera = compositor.getCamera();
                    dependency.worldPlane.transform(camera.getViewTransform(), eyePlane);
                    Vector3f normal = eyePlane.getNormal();
                    boolean away = (normal.z < 0f);
                    if (Math.abs(normal.z) < FloatMath.EPSILON || !(away ? back : front) ||
                            eyePlane.constant / normal.z < camera.getNear()) {
                        return false;
                    }
                    if (away) {
                        dependency.worldPlane.negateLocal();
                        eyePlane.negateLocal();
                    }
                    dependency.bounds.set(bounds);
                    dependency.texture = null;
                    compositor.addDependency(dependency);
                    if (dependency.texture == null) {
                        dependency.texture = config.getFromPool(ctx);
                        dependency.config = config;
                    }
                    return true;
                }
            });
            updaters.add(new Updater() {
                public void update () {
                    Compositor compositor = ctx.getCompositor();
                    Dependency.ReflectionTexture dependency = dependencies.get(
                        compositor.getSubrenderDepth());
                    unit.setTexture(dependency.texture);
                    compositor.getCamera().getTexGenPlanes(
                        unit.genPlaneS, unit.genPlaneT, unit.genPlaneQ);
                    state.setDirty(true);
                }
            });
            return null;
        }
    }

    /**
     * A planar refraction texture.
     */
    public static class Refraction extends BaseDerived
    {
        /** Whether to enable rendering to the front and back. */
        @Editable(hgroup="f")
        public boolean front = true, back;

        /** The maximum allowable depth. */
        @Editable(min=0, hgroup="f")
        public int maxDepth;

        /** The index of refraction of the source material. */
        @Editable(min=0.0, step=0.01, hgroup="n")
        public float sourceIndex = 1f;

        /** The index of refraction of the destination material. */
        @Editable(min=0.0, step=0.01, hgroup="n")
        public float destIndex = 1.5f;

        @Override
        public Texture getTexture (
            final GlContext ctx, final TextureState state, final TextureUnit unit,
            Scope scope, List<Dependency.Adder> adders, List<Updater> updaters)
        {
            if (adders == null) {
                log.warning("Tried to create refraction texture in static context.");
                return null;
            }
            final TextureConfig config = getConfig(ctx);
            if (config == null) {
                return null;
            }
            final IntMap<Dependency.RefractionTexture> dependencies = IntMaps.newHashIntMap();
            final Transform3D transform = ScopeUtil.resolve(
                scope, "worldTransform", new Transform3D());
            final Box bounds = ScopeUtil.resolve(scope, "bounds", new Box(), Box.class);
            adders.add(new Dependency.Adder() {
                public boolean add () {
                    Compositor compositor = ctx.getCompositor();
                    int depth = compositor.getSubrenderDepth();
                    Object source = compositor.getSubrenderSource();
                    if (depth > maxDepth ||
                            (source != null && source == dependencies.get(depth - 1))) {
                        return false;
                    }
                    Dependency.RefractionTexture dependency = dependencies.get(depth);
                    if (dependency == null) {
                        dependencies.put(depth,
                            dependency = new Dependency.RefractionTexture(ctx));
                    }
                    Plane.XY_PLANE.transform(transform, dependency.worldPlane);
                    Plane eyePlane = dependency.eyePlane;
                    Camera camera = compositor.getCamera();
                    dependency.worldPlane.transform(camera.getViewTransform(), eyePlane);
                    Vector3f normal = eyePlane.getNormal();
                    boolean away = (normal.z < 0f);
                    if (Math.abs(normal.z) < FloatMath.EPSILON || !(away ? back : front) ||
                            eyePlane.constant / normal.z < camera.getNear()) {
                        return false;
                    }
                    if (away) {
                        dependency.worldPlane.negateLocal();
                        eyePlane.negateLocal();
                    }
                    dependency.bounds.set(bounds);
                    dependency.ratio = sourceIndex / destIndex;
                    dependency.texture = null;
                    compositor.addDependency(dependency);
                    if (dependency.texture == null) {
                        dependency.texture = config.getFromPool(ctx);
                        dependency.config = config;
                    }
                    return true;
                }
            });
            updaters.add(new Updater() {
                public void update () {
                    Compositor compositor = ctx.getCompositor();
                    Dependency.RefractionTexture dependency = dependencies.get(
                        compositor.getSubrenderDepth());
                    unit.setTexture(dependency.texture);
                    compositor.getCamera().getTexGenPlanes(
                        unit.genPlaneS, unit.genPlaneT, unit.genPlaneQ);
                    state.setDirty(true);
                }
            });
            return null;
        }
    }

    /**
     * A dynamically rendered cube map.
     */
    public static class CubeRender extends BaseDerived
    {
        /** The distance to the near clip plane. */
        @Editable(min=0.0, step=0.01, hgroup="f")
        public float near = 1f;

        /** The distance to the far clip plane. */
        @Editable(min=0.0, step=0.01, hgroup="f")
        public float far = 100f;

        /** Toggles for the faces. */
        @Editable(editor="mask", mode="cube_map_face", hgroup="m")
        public int faces = 63;

        /** The maximum allowable depth. */
        @Editable(min=0, hgroup="m")
        public int maxDepth;

        @Override
        public Texture getTexture (
            final GlContext ctx, final TextureState state, final TextureUnit unit,
            Scope scope, List<Dependency.Adder> adders, List<Updater> updaters)
        {
            if (adders == null) {
                log.warning("Tried to create cube render texture in static context.");
                return null;
            }
            final TextureConfig config = getConfig(ctx);
            if (config == null) {
                return null;
            }
            final IntMap<Dependency.CubeTexture> dependencies = IntMaps.newHashIntMap();
            final Transform3D transform = ScopeUtil.resolve(
                scope, "worldTransform", new Transform3D());
            adders.add(new Dependency.Adder() {
                public boolean add () {
                    Compositor compositor = ctx.getCompositor();
                    int depth = compositor.getSubrenderDepth();
                    Object source = compositor.getSubrenderSource();
                    if (depth > maxDepth ||
                            (source != null && source == dependencies.get(depth - 1))) {
                        return false;
                    }
                    Dependency.CubeTexture dependency = dependencies.get(depth);
                    if (dependency == null) {
                        dependencies.put(depth, dependency = new Dependency.CubeTexture(ctx));
                    }
                    transform.extractTranslation(dependency.origin);
                    dependency.near = near;
                    dependency.far = far;
                    dependency.faces = faces;
                    dependency.texture = null;
                    compositor.addDependency(dependency);
                    if (dependency.texture == null) {
                        dependency.texture = config.getFromPool(ctx);
                        dependency.config = config;
                    }
                    return true;
                }
            });
            updaters.add(new Updater() {
                public void update () {
                    Dependency.CubeTexture dependency = dependencies.get(
                        ctx.getCompositor().getSubrenderDepth());
                    unit.setTexture(dependency.texture);
                    state.setDirty(true);
                }
            });
            return null;
        }
    }

    /**
     * A dynamically rendered shadow map.
     */
    public static class Shadow extends BaseDerived
    {
        /** Whether or not to render a depth texture. */
        @Editable
        public boolean depth;

        @Override
        public Texture getTexture (
            final GlContext ctx, final TextureState state, final TextureUnit unit,
            Scope scope, List<Dependency.Adder> adders, List<Updater> updaters)
        {
            if (adders == null) {
                log.warning("Tried to create shadow texture in static context.");
                return null;
            }
            final TextureConfig config = getConfig(ctx);
            if (config == null) {
                return null;
            }
            final IntMap<Dependency.ShadowTexture> dependencies = IntMaps.newHashIntMap();
            final ShadowConfig.TextureData data = ScopeUtil.resolve(
                scope, "data", null, ShadowConfig.TextureData.class);
            adders.add(new Dependency.Adder() {
                public boolean add () {
                    Compositor compositor = ctx.getCompositor();
                    int depth = compositor.getSubrenderDepth();
                    Object source = compositor.getSubrenderSource();
                    if (source != null && source == dependencies.get(depth - 1)) {
                        return false;
                    }
                    Dependency.ShadowTexture dependency = dependencies.get(depth);
                    if (dependency == null) {
                        dependencies.put(depth, dependency = new Dependency.ShadowTexture(ctx));
                    }
                    dependency.data = data;
                    dependency.color = dependency.depth = null;
                    compositor.addDependency(dependency);
                    if (Shadow.this.depth) {
                        if (dependency.depth == null) {
                            dependency.depth = config.getFromPool(ctx);
                            dependency.depthConfig = config;
                        }
                    } else if (dependency.color == null) {
                        dependency.color = config.getFromPool(ctx);
                        dependency.colorConfig = config;
                    }
                    return true;
                }
            });
            updaters.add(new Updater() {
                public void update () {
                    Dependency.ShadowTexture dependency = dependencies.get(
                        ctx.getCompositor().getSubrenderDepth());
                    unit.setTexture(depth ? dependency.depth : dependency.color);
                    state.setDirty(true);
                }
            });
            return null;
        }
    }

    /**
     * Switches between subtextures according to an expression.
     */
    public static class Animated extends Implementation
    {
        /** The expression that determines the frame index. */
        @Editable
        public FloatExpression frame = new FloatExpression.Constant(0f);

        /** The list of frames. */
        @Editable
        public Frame[] frames = new Frame[0];

        @Override
        public boolean isSupported (GlContext ctx, boolean fallback)
        {
            for (Frame frame : frames) {
                if (!frame.isSupported(ctx, fallback)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Texture getTexture (
            GlContext ctx, final TextureState state, final TextureUnit unit,
            Scope scope, List<Dependency.Adder> adders, List<Updater> updaters)
        {
            if (adders == null) {
                log.warning("Tried to create animated texture in static context.");
                return null;
            }
            if (frames.length == 0) {
                return null;
            }
            final FloatExpression.Evaluator frame = this.frame.createEvaluator(scope);
            final Texture[] textures = new Texture[frames.length];
            for (int ii = 0; ii < textures.length; ii++) {
                textures[ii] = frames[ii].getTexture(ctx);
            }
            updaters.add(new Updater() {
                public void update () {
                    int idx = FloatMath.ifloor(frame.evaluate()) % textures.length;
                    unit.setTexture(textures[idx < 0 ? (idx + textures.length) : idx]);
                    state.setDirty(true);
                }
            });
            return null;
        }

        @Override
        public void invalidate ()
        {
            frame.invalidate();
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends BaseDerived
    {
        @Override
        public Texture getFromPool (GlContext ctx)
        {
            TextureConfig config = getConfig(ctx);
            return (config == null) ? null : config.getFromPool(ctx);
        }

        @Override
        public void returnToPool (GlContext ctx, Texture texture)
        {
            TextureConfig config = getConfig(ctx);
            if (config != null) {
                config.returnToPool(ctx, texture);
            }
        }
    }

    /**
     * A frame within an animated texture.
     */
    public static class Frame extends BaseDerived
    {
    }

    /** The actual texture implementation. */
    @Editable
    public Implementation implementation = new Original2D();

    /**
     * Gets the requested image from the cache and applies the specified colorizations.
     */
    public static BufferedImage getImage (
        GlContext ctx, String file, ColorizationConfig[] colorizations)
    {
        Colorization[] zations = new Colorization[colorizations.length];
        for (int ii = 0; ii < zations.length; ii++) {
            zations[ii] = colorizations[ii].getColorization(ctx);
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
        return getTexture(ctx, null, null, null, null, null);
    }

    /**
     * Returns the (possibly dynamic) texture corresponding to this configuration.
     */
    public Texture getTexture (
        GlContext ctx, TextureState state, TextureUnit unit,
        Scope scope, List<Dependency.Adder> adders, List<Updater> updaters)
    {
        return implementation.getTexture(ctx, state, unit, scope, adders, updaters);
    }

    /**
     * Returns an instance of this texture from the shared pool.
     */
    public Texture getFromPool (GlContext ctx)
    {
        return implementation.getFromPool(ctx);
    }

    /**
     * Returns an instance of this texture to the shared pool.
     */
    public void returnToPool (GlContext ctx, Texture texture)
    {
        implementation.returnToPool(ctx, texture);
    }

    @Override
    protected void fireConfigUpdated ()
    {
        // invalidate the implementation
        implementation.invalidate();
        super.fireConfigUpdated();
    }

    @Override
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
