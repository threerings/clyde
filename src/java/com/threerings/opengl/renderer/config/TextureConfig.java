//
// $Id$

package com.threerings.opengl.renderer.config;

import org.lwjgl.opengl.ARBShadow;
import org.lwjgl.opengl.ARBTextureBorderClamp;
import org.lwjgl.opengl.ARBTextureCompression;
import org.lwjgl.opengl.ARBTextureMirroredRepeat;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;

import com.threerings.config.ConfigReference;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Texture;

/**
 * Texture metadata.
 */
public class TextureConfig extends ParameterizedConfig
{
    /** Format constants. */
    public enum Format
    {
        DEFAULT(-1),
        COMPRESSED_DEFAULT(-1),
        ALPHA(GL11.GL_ALPHA),
        COMPRESSED_ALPHA(ARBTextureCompression.GL_COMPRESSED_ALPHA_ARB, true),
        LUMINANCE(GL11.GL_LUMINANCE),
        COMPRESSED_LUMINANCE(ARBTextureCompression.GL_COMPRESSED_LUMINANCE_ARB, true),
        LUMINANCE_ALPHA(GL11.GL_LUMINANCE_ALPHA),
        COMPRESSED_LUMINANCE_ALPHA(ARBTextureCompression.GL_COMPRESSED_LUMINANCE_ALPHA_ARB, true),
        INTENSITY(GL11.GL_INTENSITY),
        COMPRESSED_INTENSITY(ARBTextureCompression.GL_COMPRESSED_INTENSITY_ARB, true),
        RGB(GL11.GL_RGB),
        COMPRESSED_RGB(ARBTextureCompression.GL_COMPRESSED_RGB_ARB, true),
        RGBA(GL11.GL_RGBA),
        COMPRESSED_RGBA(ARBTextureCompression.GL_COMPRESSED_RGBA_ARB, true),
        DEPTH_COMPONENT(GL11.GL_DEPTH_COMPONENT, false, true);

        public int getGLConstant ()
        {
            return _glConstant;
        }

        public boolean isSupported ()
        {
            return (!_compressed || GLContext.getCapabilities().GL_ARB_texture_compression) &&
                (!_depth || GLContext.getCapabilities().GL_ARB_depth_texture);
        }

        Format (int glConstant)
        {
            this(glConstant, false);
        }

        Format (int glConstant, boolean compressed)
        {
            this(glConstant, compressed, false);
        }

        Format (int glConstant, boolean compressed, boolean depth)
        {
            _glConstant = glConstant;
            _compressed = compressed;
            _depth = depth;
        }

        protected int _glConstant;
        protected boolean _compressed;
        protected boolean _depth;
    }

    /** Minification filter constants. */
    public enum MinFilter
    {
        NEAREST(GL11.GL_NEAREST),
        LINEAR(GL11.GL_LINEAR),
        NEAREST_MIPMAP_NEAREST(GL11.GL_NEAREST_MIPMAP_NEAREST),
        LINEAR_MIPMAP_NEAREST(GL11.GL_LINEAR_MIPMAP_NEAREST),
        NEAREST_MIPMAP_LINEAR(GL11.GL_NEAREST_MIPMAP_LINEAR),
        LINEAR_MIPMAP_LINEAR(GL11.GL_LINEAR_MIPMAP_LINEAR);

        public int getGLConstant ()
        {
            return _glConstant;
        }

        MinFilter (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** Magnification filter constants. */
    public enum MagFilter
    {
        NEAREST(GL11.GL_NEAREST),
        LINEAR(GL11.GL_LINEAR);

        public int getGLConstant ()
        {
            return _glConstant;
        }

        MagFilter (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** Wrap constants. */
    public enum Wrap
    {
        CLAMP(GL11.GL_CLAMP),
        CLAMP_TO_EDGE(GL12.GL_CLAMP_TO_EDGE) {
            public boolean isSupported () {
                return GLContext.getCapabilities().OpenGL12;
            }
        },
        REPEAT(GL11.GL_REPEAT),
        CLAMP_TO_BORDER(ARBTextureBorderClamp.GL_CLAMP_TO_BORDER_ARB) {
            public boolean isSupported () {
                return GLContext.getCapabilities().GL_ARB_texture_border_clamp;
            }
        },
        MIRRORED_REPEAT(ARBTextureMirroredRepeat.GL_MIRRORED_REPEAT_ARB) {
            public boolean isSupported () {
                return GLContext.getCapabilities().GL_ARB_texture_mirrored_repeat;
            }
        };

        public int getGLConstant ()
        {
            return _glConstant;
        }

        public boolean isSupported ()
        {
            return true;
        }

        Wrap (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** Compare mode constants. */
    public enum CompareMode
    {
        NONE(GL11.GL_NONE),
        COMPARE_R_TO_TEXTURE(ARBShadow.GL_COMPARE_R_TO_TEXTURE_ARB) {
            public boolean isSupported () {
                return GLContext.getCapabilities().GL_ARB_shadow;
            }
        };

        public int getGLConstant ()
        {
            return _glConstant;
        }

        public boolean isSupported ()
        {
            return true;
        }

        CompareMode (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** Compare function constants. */
    public enum CompareFunc
    {
        LEQUAL(GL11.GL_LEQUAL),
        GEQUAL(GL11.GL_GEQUAL);

        public int getGLConstant ()
        {
            return _glConstant;
        }

        CompareFunc (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** Depth mode constants. */
    public enum DepthMode
    {
        LUMINANCE(GL11.GL_LUMINANCE),
        INTENSITY(GL11.GL_INTENSITY),
        ALPHA(GL11.GL_ALPHA);

        public int getGLConstant ()
        {
            return _glConstant;
        }

        DepthMode (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /**
     * Contains the actual implementation of the texture.
     */
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Returns the subclasses available for selection in the editor.
         */
        public static Class[] getEditorTypes ()
        {
            return new Class[] {
                Original1D.class, Original2D.class, OriginalRectangle.class,
                Original3D.class, OriginalCubeMap.class, Derived.class };
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

        /** The minification filter. */
        @Editable(category="filter", hgroup="f")
        public MinFilter minFilter = MinFilter.NEAREST_MIPMAP_LINEAR;

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
    }

    /**
     * A 1D texture.
     */
    public static class Original1D extends Original
    {
        /**
         * The initial contents of the texture.
         */
        public static abstract class Contents extends DeepObject
            implements Exportable
        {
        }

        /**
         * Creates a blank texture.
         */
        public static class Blank extends Contents
        {
            /** The width of the texture. */
            @Editable(min=1)
            public int width = 1;
        }

        /**
         * Creates a texture from the specified image file.
         */
        public static class ImageFile extends Contents
        {
            /** The image resource from which to load the texture. */
            @Editable(editor="resource")
            @FileConstraints(
                description="m.image_files",
                extensions={".png", ".jpg"},
                directory="image_dir")
            public String file;
        }

        /** The initial contents of the texture. */
        @Editable(types={ Blank.class, ImageFile.class }, nullable=false, category="data")
        public Contents contents = new ImageFile();
    }

    /**
     * A 2D texture.
     */
    public static class Original2D extends Original
    {
        /**
         * The initial contents of the texture.
         */
        public static abstract class Contents extends DeepObject
            implements Exportable
        {
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
        }

        /**
         * Creates a texture from the specified image.
         */
        public static class ImageFile extends Contents
        {
            /** The image resource from which to load the texture. */
            @Editable(editor="resource")
            @FileConstraints(
                description="m.image_files",
                extensions={".png", ".jpg"},
                directory="image_dir")
            public String file;
        }

        /** The initial contents of the texture. */
        @Editable(types={ Blank.class, ImageFile.class }, nullable=false, category="data")
        public Contents contents = new ImageFile();
    }

    /**
     * A 2D texture that uses the rectangle target.
     */
    public static class OriginalRectangle extends Original2D
    {
    }

    /**
     * A 3D texture.
     */
    public static class Original3D extends Original
    {
        /**
         * The initial contents of the texture.
         */
        public static abstract class Contents extends DeepObject
            implements Exportable
        {
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
        }

        /**
         * Creates a texture from the specified image.
         */
        public static class ImageFile extends Contents
        {
            /** The image resource from which to load the texture. */
            @Editable(editor="resource")
            @FileConstraints(
                description="m.image_files",
                extensions={".png", ".jpg"},
                directory="image_dir")
            public String file;
        }

        /** The initial contents of the texture. */
        @Editable(types={ Blank.class, ImageFile.class }, nullable=false, category="data")
        public Contents contents = new ImageFile();
    }

    /**
     * A cube map texture.
     */
    public static class OriginalCubeMap extends Original
    {
        /**
         * The initial contents of the texture.
         */
        public static abstract class Contents extends DeepObject
            implements Exportable
        {
        }

        /**
         * Creates a blank texture.
         */
        public static class Blank extends Contents
        {
            /** The size of the textures. */
            @Editable(min=1)
            public int size = 1;
        }

        /**
         * Creates a texture from the specified image.
         */
        public static class ImageFiles extends Contents
        {
            /** The negative x, y, and z face files. */
            @Editable(nullable=false, hgroup="f")
            public FileTrio negative = new FileTrio();

            /** The positive x, y, and z face files. */
            @Editable(nullable=false, hgroup="f")
            public FileTrio positive = new FileTrio();
        }

        /**
         * Contains the files for the x, y, and z faces.
         */
        public static class FileTrio extends DeepObject
            implements Exportable
        {
            /** The image resource from which to load the x face. */
            @Editable(editor="resource")
            @FileConstraints(
                description="m.image_files",
                extensions={".png", ".jpg"},
                directory="image_dir")
            public String x;

            /** The image resource from which to load the y face. */
            @Editable(editor="resource")
            @FileConstraints(
                description="m.image_files",
                extensions={".png", ".jpg"},
                directory="image_dir")
            public String y;

            /** The image resource from which to load the y face. */
            @Editable(editor="resource")
            @FileConstraints(
                description="m.image_files",
                extensions={".png", ".jpg"},
                directory="image_dir")
            public String z;
        }

        /** The initial contents of the texture. */
        @Editable(types={ Blank.class, ImageFiles.class }, nullable=false, category="data")
        public Contents contents = new ImageFiles();
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The derived reference. */
        @Editable
        public ConfigReference<TextureConfig> base;
    }

    /** The actual texture implementation. */
    @Editable
    public Implementation implementation = new Original2D();
}
