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

package com.threerings.opengl.renderer;

import java.nio.ShortBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;

import com.threerings.opengl.renderer.state.ArrayState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.state.ShaderState;
import com.threerings.opengl.renderer.state.TextureState;

/**
 * A batch that consists of a set of render states and a stateless draw command.
 */
public class SimpleBatch extends Batch
{
    /**
     * The superclass for the stateless draw commands that actually draw the primitives.
     */
    public static abstract class DrawCommand
    {
        /**
         * Calls the draw command, causing the batch's primitives to be rendered.
         *
         * @return true if the command affected the color state, in which case it should be
         * invalidated.
         */
        public abstract boolean call ();

        /**
         * Returns the number of primitives rendered by this command.
         */
        public abstract int getPrimitiveCount ();
    }

    /**
     * Draws the batch by calling a display list containing the geometry.
     */
    public static class CallList extends DrawCommand
    {
        public CallList (DisplayList list, boolean modifiesColorState, int primitiveCount)
        {
            _list = list;
            _modifiesColorState = modifiesColorState;
            _primitiveCount = primitiveCount;
        }

        /**
         * Returns a reference to the display list.
         */
        public DisplayList getList ()
        {
            return _list;
        }

        @Override
        public boolean call ()
        {
            _list.call();
            return _modifiesColorState;
        }

        @Override
        public int getPrimitiveCount ()
        {
            return _primitiveCount;
        }

        /** The display list to call. */
        protected DisplayList _list;

        /** Whether or not the display list modifies the color state. */
        protected boolean _modifiesColorState;

        /** The number of primitives rendered in the list. */
        protected int _primitiveCount;
    }

    /**
     * Draws the batch by calling {@link GL11#glDrawArrays}.
     */
    public static class DrawArrays extends DrawCommand
    {
        public DrawArrays (int mode, int first, int count)
        {
            _mode = mode;
            _first = first;
            _count = count;
        }

        @Override
        public boolean call ()
        {
            GL11.glDrawArrays(_mode, _first, _count);
            return false;
        }

        @Override
        public int getPrimitiveCount ()
        {
            return SimpleBatch.getPrimitiveCount(_mode, _count);
        }

        /** The primitive mode. */
        protected int _mode;

        /** The first index in the arrays. */
        protected int _first;

        /** The number of indices to be rendered. */
        protected int _count;
    }

    /**
     * Draws the batch by calling {@link GL11#glDrawElements}.
     */
    public static abstract class DrawElements extends DrawCommand
    {
        public DrawElements (int mode)
        {
            _mode = mode;
        }

        /**
         * Sets the limits defining the range of indices to draw.
         */
        public abstract void setLimits (int offset, int length);

        /**
         * Changes the command's range parameters, if it has them.
         */
        public void setRange (int start, int end)
        {
            // nothing by default
        }

        /** The primitive mode. */
        protected int _mode;
    }

    /**
     * Draws the batch by calling {@link GL11#glDrawElements} using the current buffer object.
     */
    public static class DrawBufferElements extends DrawElements
    {
        public DrawBufferElements (int mode, int count, int type, long offset)
        {
            super(mode);
            _count = count;
            _type = type;
            _offset = offset;
        }

        @Override
        public void setLimits (int offset, int length)
        {
            _count = length;
            _offset = offset * sizeof(_type);
        }

        @Override
        public boolean call ()
        {
            GL11.glDrawElements(_mode, _count, _type, _offset);
            return false;
        }

        @Override
        public int getPrimitiveCount ()
        {
            return SimpleBatch.getPrimitiveCount(_mode, _count);
        }

        /** The number of elements to render. */
        protected int _count;

        /** The type of data in the index buffer. */
        protected int _type;

        /** The offset into the index buffer. */
        protected long _offset;
    }

    /**
     * Draws the batch by calling {@link GL11#glDrawElements} using a short buffer.
     */
    public static class DrawShortElements extends DrawElements
    {
        public DrawShortElements (int mode, ShortBuffer indices)
        {
            super(mode);
            _indices = indices;
        }

        @Override
        public void setLimits (int offset, int length)
        {
            _indices.limit(offset + length).position(offset);
        }

        @Override
        public boolean call ()
        {
            GL11.glDrawElements(_mode, _indices);
            return false;
        }

        @Override
        public int getPrimitiveCount ()
        {
            return SimpleBatch.getPrimitiveCount(_mode, _indices.remaining());
        }

        /** The indices to render. */
        protected ShortBuffer _indices;
    }

    /**
     * Draws the batch by calling {@link GL12#glDrawRangeElements}.
     */
    public static abstract class DrawRangeElements extends DrawElements
    {
        public DrawRangeElements (int mode, int start, int end)
        {
            super(mode);
            setRange(start, end);
        }

        @Override
        public void setRange (int start, int end)
        {
            _start = start;
            _end = end;
        }

        /** The minimum array index. */
        protected int _start;

        /** The maximum array index. */
        protected int _end;
    }

    /**
     * Draws the batch by calling {@link GL12#glDrawRangeElements} using the current buffer object.
     */
    public static class DrawBufferRangeElements extends DrawRangeElements
    {
        public DrawBufferRangeElements (int mode, int start, int end, int count, int type, long offset)
        {
            super(mode, start, end);
            _count = count;
            _type = type;
            _offset = offset;
        }

        @Override
        public void setLimits (int offset, int length)
        {
            _count = length;
            _offset = offset * sizeof(_type);
        }

        @Override
        public boolean call ()
        {
            GL12.glDrawRangeElements(_mode, _start, _end, _count, _type, _offset);
            return false;
        }

        @Override
        public int getPrimitiveCount ()
        {
            return SimpleBatch.getPrimitiveCount(_mode, _count);
        }

        /** The number of elements to render. */
        protected int _count;

        /** The type of data in the index buffer. */
        protected int _type;

        /** The offset into the index buffer. */
        protected long _offset;
    }

    /**
     * Draws the batch by calling {@link GL12#glDrawRangeElements} using a short buffer.
     */
    public static class DrawShortRangeElements extends DrawRangeElements
    {
        public DrawShortRangeElements (int mode, int start, int end, ShortBuffer indices)
        {
            super(mode, start, end);
            _indices = indices;
        }

        @Override
        public void setLimits (int offset, int length)
        {
            _indices.limit(offset + length).position(offset);
        }

        @Override
        public boolean call ()
        {
            GL12.glDrawRangeElements(_mode, _start, _end, _indices);
            return false;
        }

        @Override
        public int getPrimitiveCount ()
        {
            return SimpleBatch.getPrimitiveCount(_mode, _indices.remaining());
        }

        /** The indices to render. */
        protected ShortBuffer _indices;
    }

    /**
     * Creates a {@link DrawBufferRangeElements} if the driver supports it; otherwise, falls back
     * and creates a {@link DrawBufferElements}.
     */
    public static DrawElements createDrawBufferElements (
        int mode, int start, int end, int count, int type, long offset)
    {
        if (GLContext.getCapabilities().OpenGL12) {
            return new DrawBufferRangeElements(mode, start, end, count, type, offset);
        } else {
            return new DrawBufferElements(mode, count, type, offset);
        }
    }

    /**
     * Creates a {@link DrawShortRangeElements} if the driver supports it; otherwise, falls back
     * and creates a {@link DrawShortElements}.
     */
    public static DrawElements createDrawShortElements (
        int mode, int start, int end, ShortBuffer indices)
    {
        if (GLContext.getCapabilities().OpenGL12) {
            return new DrawShortRangeElements(mode, start, end, indices);
        } else {
            return new DrawShortElements(mode, indices);
        }
    }

    /**
     * Creates a new batch with the specified states and command.
     */
    public SimpleBatch (RenderState[] states, DrawCommand command)
    {
        _states = states;
        _command = command;
        updateKey();
    }

    /**
     * Returns a reference to the batch's set of render states.
     */
    public RenderState[] getStates ()
    {
        return _states;
    }

    /**
     * Returns a reference to the batch's draw command.
     */
    public DrawCommand getCommand ()
    {
        return _command;
    }

    /**
     * Updates the batch's state key using the current set of states.
     */
    public void updateKey ()
    {
        TextureState tstate = (TextureState)_states[RenderState.TEXTURE_STATE];
        TextureUnit[] units = (tstate == null) ? null : tstate.getUnits();
        int textures = (units == null) ? 0 : units.length;

        // key consists of vertex shader id, fragment shader id, texture ids,
        // -1 (to signify end of textures), buffer id
        int size = 1 + 1 + textures + 1 + 1;
        if (key == null || key.length != size) {
            key = new int[size];
        }
        ShaderState sstate = (ShaderState)_states[RenderState.SHADER_STATE];
        Program program = (sstate == null) ? null : sstate.getProgram();
        Shader vertexShader = (program == null) ? null : program.getVertexShader();
        Shader fragmentShader = (program == null) ? null : program.getFragmentShader();
        int idx = 0;
        key[idx++] = (vertexShader == null) ? 0 : vertexShader.getId();
        key[idx++] = (fragmentShader == null) ? 0 : fragmentShader.getId();

        for (int ii = 0; ii < textures; ii++) {
            Texture texture = (units[ii] == null) ? null : units[ii].texture;
            key[idx++] = (texture == null) ? 0 : texture.getId();
        }
        key[idx++] = -1;

        ArrayState astate = (ArrayState)_states[RenderState.ARRAY_STATE];
        BufferObject buffer = (astate == null) ? null : astate.getElementArrayBuffer();
        if (buffer != null) {
            key[idx++] = buffer.getId();
        } else if (_command instanceof CallList) {
            key[idx++] = ((CallList)_command).getList().getId();
        } else {
            key[idx++] = 0;
        }
    }

    @Override
    public boolean draw (Renderer renderer)
    {
        renderer.setStates(_states);
        return _command.call();
    }

    @Override
    public int getPrimitiveCount ()
    {
        return _command.getPrimitiveCount();
    }

    @Override
    public SimpleBatch clone ()
    {
        // make a shallow clone of the states
        SimpleBatch obatch = (SimpleBatch)super.clone();
        obatch._states = _states.clone();
        return obatch;
    }

    /**
     * Returns the size, in bytes, of the specified OpenGL primitive type.
     */
    protected static int sizeof (int type)
    {
        switch (type) {
            default:
            case GL11.GL_UNSIGNED_BYTE:
                return 1;
            case GL11.GL_UNSIGNED_SHORT:
                return 2;
            case GL11.GL_UNSIGNED_INT:
                return 4;
        }
    }

    /**
     * Returns the number of primitives rendered based on the mode and index count.
     */
    protected static int getPrimitiveCount (int mode, int count)
    {
        switch (mode) {
            case GL11.GL_POINTS: case GL11.GL_LINE_LOOP: return count;
            case GL11.GL_LINES: return count/2;
            case GL11.GL_LINE_STRIP: return count-1;
            case GL11.GL_TRIANGLES: return count/3;
            case GL11.GL_TRIANGLE_STRIP: case GL11.GL_TRIANGLE_FAN: return count-2;
            case GL11.GL_QUADS: return count/4;
            case GL11.GL_QUAD_STRIP: return count/2 - 1;
            case GL11.GL_POLYGON: return 1;
            default: return 0;
        }
    }

    /** The render states to apply before drawing. */
    protected RenderState[] _states;

    /** The stateless draw command that actually draws the batch. */
    protected DrawCommand _command;
}
