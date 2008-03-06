//
// $Id$

package com.threerings.opengl.renderer;

import java.io.UnsupportedEncodingException;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;

/**
 * Contains a shader.
 */
public class Shader extends ShaderObject
{
    /**
     * Creates a new shader.
     */
    public Shader (Renderer renderer, int type)
    {
        super(renderer);
        _id = ARBShaderObjects.glCreateShaderObjectARB(type);
    }

    /**
     * Sets the source to the shader and compiles it.  If compilation fails (or even if it
     * succeeds), {@link #getInfoLog} can be used to return more information.
     *
     * @return true if the shader compiled successfully, false if there was an error.
     */
    public boolean setSource (String source)
    {
        // convert the source to an ASCII buffer
        ByteBuffer buf = ASCII_CHARSET.encode(source);

        // and copy that to a direct buffer
        buf = BufferUtils.createByteBuffer(buf.remaining()).put(buf);
        buf.rewind();

        // load the source and compile
        ARBShaderObjects.glShaderSourceARB(_id, buf);
        ARBShaderObjects.glCompileShaderARB(_id);

        // check the result
        IntBuffer ibuf = BufferUtils.createIntBuffer(1);
        ARBShaderObjects.glGetObjectParameterARB(
            _id, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB, ibuf);
        return (ibuf.get(0) == GL11.GL_TRUE);
    }
}
