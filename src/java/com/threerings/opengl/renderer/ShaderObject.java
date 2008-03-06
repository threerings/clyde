//
// $Id$

package com.threerings.opengl.renderer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;

/**
 * An OpenGL shader object.
 */
public abstract class ShaderObject
{
    /**
     * Creates a shader object for the specified renderer.
     */
    public ShaderObject (Renderer renderer)
    {
        _renderer = renderer;
    }

    /**
     * Returns the OpenGL identifier for this object.
     */
    public final int getId ()
    {
        return _id;
    }

    /**
     * Retrieves and returns the shader object info log.
     */
    public String getInfoLog ()
    {
        // get the length of the log
        IntBuffer ibuf = BufferUtils.createIntBuffer(1);
        ARBShaderObjects.glGetObjectParameterARB(
            _id, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB, ibuf);

        // then the log itself
        ByteBuffer bbuf = BufferUtils.createByteBuffer(ibuf.get(0));
        ARBShaderObjects.glGetInfoLogARB(_id, ibuf, bbuf);
        bbuf.limit(ibuf.get(0));

        // convert from ASCII and return
        return ASCII_CHARSET.decode(bbuf).toString();
    }

    /**
     * Deletes this object, rendering it unusable.
     */
    public void delete ()
    {
        ARBShaderObjects.glDeleteObjectARB(_id);
        _id = 0;
    }

    @Override // documentation inherited
    protected void finalize ()
        throws Throwable
    {
        super.finalize();
        if (_id > 0) {
            _renderer.shaderObjectFinalized(_id);
        }
    }

    /** The renderer that loaded this object. */
    protected Renderer _renderer;

    /** The OpenGL identifier for this object. */
    protected int _id;

    /** The ASCII charset. */
    protected static final Charset ASCII_CHARSET = Charset.forName("US-ASCII");
}
