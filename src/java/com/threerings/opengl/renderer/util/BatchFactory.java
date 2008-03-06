//
// $Id$

package com.threerings.opengl.renderer.util;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.threerings.opengl.renderer.Batch;
import com.threerings.opengl.renderer.BufferObject;
import com.threerings.opengl.renderer.ClientArray;
import com.threerings.opengl.renderer.DisplayList;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.state.ArrayState;
import com.threerings.opengl.renderer.state.RenderState;

/**
 * Contains methods to create optimal {@link Batch}es for rendering various kinds of geometry.
 */
public class BatchFactory
{
    /**
     * Creates a batch containing the specified array of lines.
     */
    public static SimpleBatch createLineBatch (Renderer renderer, FloatBuffer vbuf)
    {
        // use a buffer object if they're available; otherwise, use a display list
        RenderState[] states = RenderState.createEmptySet();
        SimpleBatch.DrawCommand command;
        int vcount = vbuf.remaining() / 3;
        if (GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
            BufferObject buffer = new BufferObject(renderer);
            buffer.setData(vbuf);
            states[RenderState.ARRAY_STATE] = new ArrayState(
                0, null, null, null, null, new ClientArray(3, GL11.GL_FLOAT, buffer), null);
            command = new SimpleBatch.DrawArrays(GL11.GL_LINES, 0, vcount);
        } else {
            DisplayList list = new DisplayList(renderer);
            renderer.setArrayState(
                0, null, null, null, null, new ClientArray(3, vbuf), null);
            GL11.glNewList(list.getId(), GL11.GL_COMPILE);
            GL11.glDrawArrays(GL11.GL_LINES, 0, vcount);
            GL11.glEndList();
            states[RenderState.ARRAY_STATE] = ArrayState.DISABLED;
            command = new SimpleBatch.CallList(list, false, vcount/2);
        }
        return new SimpleBatch(states, command);
    }

    /**
     * Creates a batch containing the specified triangles (whose texture coordinates, normals, and
     * vertices are packed as in the {@link GL11#GL_T2F_N3F_V3F} interleaved format).
     */
    public static SimpleBatch createTriangleBatchT2N3V3 (
        Renderer renderer, FloatBuffer vbuf, ShortBuffer ibuf)
    {
        RenderState[] states = RenderState.createEmptySet();
        SimpleBatch.DrawCommand command;
        int vcount = vbuf.remaining() / 8, icount = ibuf.remaining();
        if (GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
            BufferObject vbobj = new BufferObject(renderer);
            BufferObject ibobj = new BufferObject(renderer);
            vbobj.setData(vbuf);
            ibobj.setData(ibuf);
            states[RenderState.ARRAY_STATE] = new ArrayState(
                0, null,
                new ClientArray[] { new ClientArray(2, GL11.GL_FLOAT, 32, 0, vbobj) },
                null,
                new ClientArray(3, GL11.GL_FLOAT, 32, 8, vbobj),
                new ClientArray(3, GL11.GL_FLOAT, 32, 20, vbobj),
                ibobj);
            command = SimpleBatch.createDrawBufferElements(
                GL11.GL_TRIANGLES, 0, vcount - 1, icount, GL11.GL_UNSIGNED_SHORT, 0);
        } else {
            DisplayList list = new DisplayList(renderer);
            renderer.setArrayState(
                0, null,
                new ClientArray[] { new ClientArray(2, 32, 0, vbuf) },
                null,
                new ClientArray(3, 32, 8, vbuf),
                new ClientArray(3, 32, 20, vbuf),
                null);
            GL11.glNewList(list.getId(), GL11.GL_COMPILE);
            GL11.glDrawElements(GL11.GL_TRIANGLES, ibuf);
            GL11.glEndList();
            states[RenderState.ARRAY_STATE] = ArrayState.DISABLED;
            command = new SimpleBatch.CallList(list, false, icount/3);
        }
        return new SimpleBatch(states, command);
    }
}
