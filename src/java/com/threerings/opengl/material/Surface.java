//
// $Id$

package com.threerings.opengl.material;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.IdentityHashMap;

import org.lwjgl.BufferUtils;

import com.threerings.opengl.renderer.ClientArray;
import com.threerings.opengl.renderer.Program.Uniform;
import com.threerings.opengl.renderer.state.ArrayState;
import com.threerings.opengl.renderer.state.ShaderState;
import com.threerings.opengl.util.Renderable;

/**
 * Represents an instance of a mesh using a material.
 */
public abstract class Surface
    implements Renderable, Cloneable
{
    /**
     * Sets the host of the surface, which provides access to the parameters that determine how
     * the surface will be rendered.
     */
    public abstract void setHost (SurfaceHost host);

    /**
     * Updates the surface to reflect a change in the parameters of its host.
     */
    public abstract void update ();

    /**
     * Enqueues this surface for rendering.
     */
    public abstract void enqueue ();

    @Override // documentation inherited
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null; // should never happen
        }
    }

    /**
     * Creates a new shader state with cloned uniforms.
     */
    protected static ShaderState copyShaderState (ShaderState sstate)
    {
        Uniform[] uniforms = sstate.getUniforms();
        Uniform[] cuniforms = null;
        if (uniforms != null) {
            cuniforms = new Uniform[uniforms.length];
            for (int ii = 0; ii < uniforms.length; ii++) {
                cuniforms[ii] = uniforms[ii].clone(null);
            }
        }
        return new ShaderState(sstate.getProgram(), cuniforms);
    }

    /**
     * Creates a new array state with cloned vertex buffers.
     */
    protected static ArrayState copyArrayState (ArrayState astate)
    {
        IdentityHashMap<Buffer, Buffer> copies = new IdentityHashMap<Buffer, Buffer>();
        return new ArrayState(
            astate.getFirstVertexAttribIndex(),
            copyArrays(astate.getVertexAttribArrays(), copies),
            copyArrays(astate.getTexCoordArrays(), copies),
            copyArray(astate.getColorArray(), copies),
            copyArray(astate.getNormalArray(), copies),
            copyArray(astate.getVertexArray(), copies),
            astate.getElementArrayBuffer());
    }

    /**
     * Copies the provided arrays, making sure that shared buffers are shared in the copy as well.
     */
    protected static ClientArray[] copyArrays (
        ClientArray[] arrays, IdentityHashMap<Buffer, Buffer> copies)
    {
        if (arrays == null) {
            return null;
        }
        ClientArray[] carrays = new ClientArray[arrays.length];
        for (int ii = 0; ii < arrays.length; ii++) {
            carrays[ii] = copyArray(arrays[ii], copies);
        }
        return carrays;
    }

    /**
     * Copies the provided array, making sure that shared buffers are shared in the copy as well.
     */
    protected static ClientArray copyArray (
        ClientArray array, IdentityHashMap<Buffer, Buffer> copies)
    {
        if (array == null) {
            return null;
        }
        ClientArray carray = new ClientArray();
        carray.set(array);
        if (array.floatArray != null) {
            carray.floatArray = copyBuffer(array.floatArray, copies);
        }
        return carray;
    }

    /**
     * Copies the provided buffer.
     */
    protected static FloatBuffer copyBuffer (
        FloatBuffer buffer, IdentityHashMap<Buffer, Buffer> copies)
    {
        FloatBuffer copy = (FloatBuffer)copies.get(buffer);
        if (copy == null) {
            copies.put(buffer, copy = BufferUtils.createFloatBuffer(buffer.remaining()));
            copy.put(buffer).rewind();
            buffer.rewind();
        }
        return copy;
    }
}
