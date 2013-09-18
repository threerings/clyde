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

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBBufferObject;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.ARBOcclusionQuery;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBTextureCubeMap;
import org.lwjgl.opengl.ARBTextureEnvCombine;
import org.lwjgl.opengl.ARBTextureRectangle;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTRescaleNormal;
import org.lwjgl.opengl.EXTTextureLODBias;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.Pbuffer;

import com.samskivert.util.IntListUtil;
import com.samskivert.util.ListUtil;
import com.samskivert.util.ObserverList;
import com.samskivert.util.RunAnywhere;
import com.samskivert.util.WeakObserverList;

import com.threerings.math.FloatMath;
import com.threerings.math.Matrix4f;
import com.threerings.math.Plane;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.math.Vector4f;

import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.gui.util.Rectangle;

/**
 * Provides access to state associated with the renderer.  Any state changes made should be done
 * through this object so that its internal state is synchronized with the OpenGL state.
 */
public class Renderer
{
    /**
     * An interface for objects interested in renderer state changes.
     */
    public interface Observer
    {
        /**
         * Notes that the size of the renderer's drawable surface has changed.
         */
        public void sizeChanged (int width, int height);
    }

    /**
     * Initializes the renderer.
     *
     * @param drawable the drawable surface with which this renderer will be used.
     * @param width the initial viewport width.
     * @param height the initial viewport height.
     */
    public void init (Drawable drawable, int width, int height)
    {
        _drawable = drawable;
        _width = width;
        _height = height;

        // find out how many alpha bit planes are in the frame buffer
        IntBuffer buf = BufferUtils.createIntBuffer(16);
        GL11.glGetInteger(GL11.GL_ALPHA_BITS, buf);
        _alphaBits = buf.get(0);

        // how many stencil bit planes
        GL11.glGetInteger(GL11.GL_STENCIL_BITS, buf);
        _stencilBits = buf.get(0);

        // how many user clip planes
        GL11.glGetInteger(GL11.GL_MAX_CLIP_PLANES, buf);
        _maxClipPlanes = buf.get(0);

        // how many lights
        GL11.glGetInteger(GL11.GL_MAX_LIGHTS, buf);
        _maxLights = buf.get(0);

        // how many fixed-function texture units
        ContextCapabilities caps = GLContext.getCapabilities();
        if (caps.GL_ARB_multitexture) {
            GL11.glGetInteger(ARBMultitexture.GL_MAX_TEXTURE_UNITS_ARB, buf);
            _maxTextureUnits = buf.get(0);
        } else {
            _maxTextureUnits = 1;
        }

        // how many programmable texture units
        if (caps.GL_ARB_fragment_shader) {
            GL11.glGetInteger(ARBFragmentShader.GL_MAX_TEXTURE_IMAGE_UNITS_ARB, buf);
            _maxTextureImageUnits = buf.get(0);
        } else {
            _maxTextureImageUnits = _maxTextureUnits;
        }

        // and how many vertex attributes
        if (caps.GL_ARB_vertex_shader) {
            GL11.glGetInteger(ARBVertexShader.GL_MAX_VERTEX_ATTRIBS_ARB, buf);
            _maxVertexAttribs = buf.get(0);
        } else {
            _maxVertexAttribs = 0;
        }

        // determine the vendor
        String vendor = GL11.glGetString(GL11.GL_VENDOR).toLowerCase();
        _nvidia = vendor.contains("nvidia");
        _ati = vendor.contains("ati");
        _intel = vendor.contains("intel");

        // get the initial draw/read buffers
        GL11.glGetInteger(GL11.GL_DRAW_BUFFER, buf);
        _drawBuffer = buf.get(0);
        GL11.glGetInteger(GL11.GL_READ_BUFFER, buf);
        _readBuffer = buf.get(0);

        // to make things easier for texture loading, we just keep this at one (default is four)
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        // initialize the viewport
        _viewport.set(0, 0, width, height);

        // initialize the clip plane records
        _clipPlanes = new ClipPlaneRecord[_maxClipPlanes];
        for (int ii = 0; ii < _maxClipPlanes; ii++) {
            _clipPlanes[ii] = new ClipPlaneRecord();
        }

        // initialize the scissor box
        _scissor.set(0, 0, width, height);

        // initialize the array records
        _vertexAttribArrays = new ClientArrayRecord[_maxVertexAttribs];
        for (int ii = 0; ii < _maxVertexAttribs; ii++) {
            _vertexAttribArrays[ii] = new ClientArrayRecord();
        }
        _texCoordArrays = new ClientArrayRecord[_maxTextureUnits];
        for (int ii = 0; ii < _maxTextureUnits; ii++) {
            _texCoordArrays[ii] = new ClientArrayRecord();
        }

        // and the light records
        _lights = new LightRecord[_maxLights];
        for (int ii = 0; ii < _maxLights; ii++) {
            _lights[ii] = new LightRecord(ii);
        }

        // and the texture unit records
        _units = new TextureUnitRecord[_maxTextureImageUnits];
        for (int ii = 0; ii < _maxTextureImageUnits; ii++) {
            _units[ii] = new TextureUnitRecord();
        }
    }

    /**
     * Returns a reference to the drawable target of this renderer.
     */
    public Drawable getDrawable ()
    {
        return _drawable;
    }

    /**
     * Notes that the size of the renderer's drawable surface has changed.  This does not change
     * the viewport, scissor region, etc.; it simply records the change and notifies the observers.
     */
    public void setSize (int width, int height)
    {
        if (_width == width && _height == height) {
            return;
        }
        _width = width;
        _height = height;

        _observers.apply(new ObserverList.ObserverOp<Observer>() {
            public boolean apply (Observer observer) {
                observer.sizeChanged(_width, _height);
                return true;
            }
        });
    }

    /**
     * Returns the width of the renderer's drawable surface.
     */
    public int getWidth ()
    {
        return _width;
    }

    /**
     * Returns the height of the renderer's drawable surface.
     */
    public int getHeight ()
    {
        return _height;
    }

    /**
     * Adds an observer to the list of objects interested in state changes.  Note that only a weak
     * reference to the observer will be retained, and thus this will not prevent the observer
     * from being garbage-collected.
     */
    public void addObserver (Observer observer)
    {
        _observers.add(observer);
    }

    /**
     * Removes an observer from the list.
     */
    public void removeObserver (Observer observer)
    {
        _observers.remove(observer);
    }

    /**
     * Returns the number of alpha bit planes in the frame buffer.
     */
    public int getAlphaBits ()
    {
        return _alphaBits;
    }

    /**
     * Returns the number of stencil bit planes in the frame buffer.
     */
    public int getStencilBits ()
    {
        return _stencilBits;
    }

    /**
     * Returns the maximum number of user clip planes supported.
     */
    public int getMaxClipPlanes ()
    {
        return _maxClipPlanes;
    }

    /**
     * Returns the maximum number of lights supported.
     */
    public int getMaxLights ()
    {
        return _maxLights;
    }

    /**
     * Returns the maximum number of texture units usable with the fixed-function pipeline.
     */
    public int getMaxTextureUnits ()
    {
        return _maxTextureUnits;
    }

    /**
     * Returns the maximum number of texture units usable with the programmable pipeline.
     */
    public int getMaxTextureImageUnits ()
    {
        return _maxTextureImageUnits;
    }

    /**
     * Returns the maximum number of vertex attributes available to vertex shaders.
     */
    public int getMaxVertexAttribs ()
    {
        return _maxVertexAttribs;
    }

    /**
     * Returns the number of texture changes since the last call to {@link #resetStats}.
     */
    public int getTextureChangeCount ()
    {
        return _textureChangeCount;
    }

    /**
     * Returns the number of batches rendered since the last call to {@link #resetStats}.
     */
    public int getBatchCount ()
    {
        return _batchCount;
    }

    /**
     * Returns the number of primitives rendered since the last call to {@link #resetStats}.
     */
    public int getPrimitiveCount ()
    {
        return _primitiveCount;
    }

    /**
     * Resets the per-frame stats.
     */
    public void resetStats ()
    {
        _textureChangeCount = 0;
        _batchCount = 0;
        _primitiveCount = 0;
    }

    /**
     * Returns the number of active buffer objects.
     */
    public int getBufferObjectCount ()
    {
        return _bufferObjectCount;
    }

    /**
     * Returns the total number of bytes in buffer objects.
     */
    public int getBufferObjectBytes ()
    {
        return _bufferObjectBytes;
    }

    /**
     * Returns the number of active display lists.
     */
    public int getDisplayListCount ()
    {
        return _displayListCount;
    }

    /**
     * Returns the number of active shader objects.
     */
    public int getShaderObjectCount ()
    {
        return _shaderObjectCount;
    }

    /**
     * Returns the number of active textures.
     */
    public int getTextureCount ()
    {
        return _textureCount;
    }

    /**
     * Returns the total number of bytes in textures.
     */
    public int getTextureBytes ()
    {
        return _textureBytes;
    }

    /**
     * Gives the renderer a chance to perform any periodic cleanup necessary.
     */
    public void cleanup ()
    {
        // delete any finalized objects
        deleteFinalizedObjects();
    }

    /**
     * Sets the clear color.
     */
    public void setClearColor (Color4f color)
    {
        if (!_clearColor.equals(color)) {
            GL11.glClearColor(color.r, color.g, color.b, color.a);
            _clearColor.set(color);
        }
    }

    /**
     * Returns a reference to the clear color.
     */
    public Color4f getClearColor ()
    {
        return _clearColor;
    }

    /**
     * Sets the clear depth.
     */
    public void setClearDepth (float depth)
    {
        if (_clearDepth != depth) {
            GL11.glClearDepth(_clearDepth = depth);
        }
    }

    /**
     * Returns the clear depth.
     */
    public float getClearDepth ()
    {
        return _clearDepth;
    }

    /**
     * Sets the clear stencil value.
     */
    public void setClearStencil (int stencil)
    {
        if (_clearStencil != stencil) {
            GL11.glClearStencil(_clearStencil = stencil);
        }
    }

    /**
     * Returns the clear stencil value.
     */
    public int getClearStencil ()
    {
        return _clearStencil;
    }

    /**
     * Sets the viewport state.
     */
    public void setViewport (Rectangle viewport)
    {
        setViewport(viewport.x, viewport.y, viewport.width, viewport.height);
    }

    /**
     * Sets the viewport state.
     */
    public void setViewport (int x, int y, int width, int height)
    {
        if (_viewport.x != x || _viewport.y != y || _viewport.width != width ||
                _viewport.height != height) {
            GL11.glViewport(
                _viewport.x = x, _viewport.y = y,
                _viewport.width = width, _viewport.height = height);
        }
    }

    /**
     * Returns the current viewport state.
     */
    public Rectangle getViewport ()
    {
        return _viewport;
    }

    /**
     * Sets the projection matrix.
     */
    public void setProjection (
        float left, float right, float bottom, float top, float near,
        float far, Vector3f nearFarNormal, boolean ortho)
    {
        if (_left == left && _right == right && _bottom == bottom &&
            _top == top && _near == near && _far == far &&
            _nearFarNormal.equals(nearFarNormal) && _ortho == ortho) {
            return;
        }
        setMatrixMode(GL11.GL_PROJECTION);
        if (_nearFarNormal.set(nearFarNormal).equals(Vector3f.UNIT_Z)) {
            GL11.glLoadIdentity();
            if (_ortho = ortho) {
                GL11.glOrtho(
                    _left = left, _right = right, _bottom = bottom,
                    _top = top, _near = near, _far = far);
            } else {
                GL11.glFrustum(
                    _left = left, _right = right, _bottom = bottom,
                    _top = top, _near = near, _far = far);
            }
            return;
        }
        if (_ortho = ortho) {
            _mat.setToOrtho(
                _left = left, _right = right, _bottom = bottom,
                _top = top, _near = near, _far = far, _nearFarNormal);
        } else {
            _mat.setToFrustum(
                _left = left, _right = right, _bottom = bottom,
                _top = top, _near = near, _far = far, _nearFarNormal);
        }
        _mat.get(_vbuf).rewind();
        GL11.glLoadMatrix(_vbuf);
    }

    /**
     * Returns the left projection parameter.
     */
    public float getLeft ()
    {
        return _left;
    }

    /**
     * Returns the right projection parameter.
     */
    public float getRight ()
    {
        return _right;
    }

    /**
     * Returns the bottom projection parameter.
     */
    public float getBottom ()
    {
        return _bottom;
    }

    /**
     * Returns the top projection parameter.
     */
    public float getTop ()
    {
        return _top;
    }

    /**
     * Returns the near projection parameter.
     */
    public float getNear ()
    {
        return _near;
    }

    /**
     * Returns the far projection parameter.
     */
    public float getFar ()
    {
        return _far;
    }

    /**
     * Returns a reference to the near/far normal projection parameter.
     */
    public Vector3f getNearFarNormal ()
    {
        return _nearFarNormal;
    }

    /**
     * Returns the ortho projection parameter.
     */
    public boolean isOrtho ()
    {
        return _ortho;
    }

    /**
     * Sets the user clip planes.
     *
     * @param planes the array of clip planes to set, or <code>null</code> to disable all planes.
     */
    public void setClipPlanes (Plane[] planes)
    {
        // update the union of the requested planes and the ones already set
        int numPlanes = (planes == null) ? 0 : planes.length;
        for (int ii = 0, nn = Math.max(_clipPlaneEnd, numPlanes); ii < nn; ii++) {
            ClipPlaneRecord prec = _clipPlanes[ii];
            Plane plane = (ii < numPlanes) ? planes[ii] : null;
            boolean planeEnabled = (plane != null);
            int pname = GL11.GL_CLIP_PLANE0 + ii;
            if (prec.enabled != Boolean.valueOf(planeEnabled)) {
                setCapability(pname, prec.enabled = planeEnabled);
            }
            if (!planeEnabled) {
                continue;
            }
            if (!prec.equals(plane)) {
                // transform by transpose of modelview matrix to negate OpenGL's multiplying by
                // inverse
                Matrix4f mat = getModelviewMatrix();
                Vector3f normal = prec.set(plane).getNormal();
                _dbuf.put(normal.x*mat.m00 + normal.y*mat.m01 + normal.z*mat.m02);
                _dbuf.put(normal.x*mat.m10 + normal.y*mat.m11 + normal.z*mat.m12);
                _dbuf.put(normal.x*mat.m20 + normal.y*mat.m21 + normal.z*mat.m22);
                _dbuf.put(normal.x*mat.m30 + normal.y*mat.m31 + normal.z*mat.m32 + prec.constant);
                _dbuf.rewind();
                GL11.glClipPlane(pname, _dbuf);
            }
        }
        _clipPlaneEnd = numPlanes;
    }

    /**
     * Sets the scissor box.
     *
     * @param box the scissor box, or <code>null</code> to disable the scissor test.
     */
    public void setScissor (Rectangle box)
    {
        boolean scissorTestEnabled = (box != null);
        if (_scissorTestEnabled != Boolean.valueOf(scissorTestEnabled)) {
            setCapability(GL11.GL_SCISSOR_TEST, _scissorTestEnabled = scissorTestEnabled);
        }
        if (scissorTestEnabled && !_scissor.equals(box)) {
            GL11.glScissor(box.x, box.y, box.width, box.height);
            _scissor.set(box);
        }
    }

    /**
     * Returns a reference to the scissor box, or <code>null</code> if scissor testing is
     * disabled.
     */
    public Rectangle getScissor ()
    {
        return (_scissorTestEnabled == Boolean.TRUE) ? _scissor : null;
    }

    /**
     * Sets the front face.
     */
    public void setFrontFace (int face)
    {
        if (_frontFace != face) {
            GL11.glFrontFace(_frontFace = face);
        }
    }

    /**
     * Returns the current front face.
     */
    public int getFrontFace ()
    {
        return _frontFace;
    }

    /**
     * Sets the normalization parameters.
     */
    public void setNormalize (boolean normalize, boolean rescaleNormal)
    {
        if (_normalize != Boolean.valueOf(normalize)) {
            setCapability(GL11.GL_NORMALIZE, _normalize = normalize);
        }
        if (_rescaleNormal != Boolean.valueOf(rescaleNormal)) {
            setCapability(EXTRescaleNormal.GL_RESCALE_NORMAL_EXT, _rescaleNormal = rescaleNormal);
        }
    }

    /**
     * Starts a query.
     */
    public void startQuery (Query query)
    {
        if (query.getTarget() != ARBOcclusionQuery.GL_SAMPLES_PASSED_ARB ||
                _samplesPassed == query) {
            return;
        }
        if (_samplesPassed != null) {
            ARBOcclusionQuery.glEndQueryARB(ARBOcclusionQuery.GL_SAMPLES_PASSED_ARB);
        }
        _samplesPassed = query;
        ARBOcclusionQuery.glBeginQueryARB(query.getTarget(), query.getId());
    }

    /**
     * Stops a query.
     */
    public void stopQuery (Query query)
    {
        if (_samplesPassed != query) {
            return;
        }
        ARBOcclusionQuery.glEndQueryARB(ARBOcclusionQuery.GL_SAMPLES_PASSED_ARB);
        _samplesPassed = null;
    }

    /**
     * Sets an entire group of states at once.
     */
    public void setStates (RenderState[] states)
    {
        for (int ii = 0; ii < RenderState.STATE_COUNT; ii++) {
            RenderState state = states[ii];
            if (state != null && (_states[ii] != state || state.isDirty())) {
                state.apply(this);
                state.setDirty(false);
                _states[ii] = state;
            }
        }
    }

    /**
     * Sets a single render state.
     */
    public void setState (RenderState state)
    {
        int type = state.getType();
        if (_states[type] != state || state.isDirty()) {
            state.apply(this);
            state.setDirty(false);
            _states[type] = state;
        }
    }

    /**
     * Sets the alpha testing and blending state.  If <code>alphaTestFunc</code> is
     * {@link GL11#GL_ALWAYS}, alpha testing will be disabled.  If <code>srcBlendFactor</code>
     * is {@link GL11#GL_ONE} and <code>destBlendFactor</code> is {@link GL11#GL_ZERO}, blending
     * will be disabled.
     */
    public void setAlphaState (
        int alphaTestFunc, float alphaTestRef, int srcBlendFactor, int destBlendFactor)
    {
        // clear any cached reference
        _states[RenderState.ALPHA_STATE] = null;

        boolean alphaTestEnabled = (alphaTestFunc != GL11.GL_ALWAYS);
        if (_alphaTestEnabled != Boolean.valueOf(alphaTestEnabled)) {
            setCapability(GL11.GL_ALPHA_TEST, _alphaTestEnabled = alphaTestEnabled);
        }
        if (alphaTestEnabled &&
                (_alphaTestFunc != alphaTestFunc || _alphaTestRef != alphaTestRef)) {
            GL11.glAlphaFunc(_alphaTestFunc = alphaTestFunc, _alphaTestRef = alphaTestRef);
        }

        boolean blendEnabled = (srcBlendFactor != GL11.GL_ONE || destBlendFactor != GL11.GL_ZERO);
        if (_blendEnabled != Boolean.valueOf(blendEnabled)) {
            setCapability(GL11.GL_BLEND, _blendEnabled = blendEnabled);
        }
        if (blendEnabled &&
                (_srcBlendFactor != srcBlendFactor || _destBlendFactor != destBlendFactor)) {
            GL11.glBlendFunc(_srcBlendFactor = srcBlendFactor, _destBlendFactor = destBlendFactor);
        }
    }

    /**
     * Invalidates the alpha state, forcing it to be reapplied.
     */
    public void invalidateAlphaState ()
    {
        _alphaTestEnabled = _blendEnabled = null;
        _alphaTestFunc = _srcBlendFactor = _destBlendFactor = -1;
        _alphaTestRef = -1f;
        _states[RenderState.ALPHA_STATE] = null;
    }

    /**
     * Sets the client array state.
     */
    public void setArrayState (
        int firstVertexAttribIndex, ClientArray[] vertexAttribArrays, ClientArray[] texCoordArrays,
        ClientArray colorArray, ClientArray normalArray, ClientArray vertexArray,
        BufferObject elementArrayBuffer)
    {
        // clear any cached reference
        _states[RenderState.ARRAY_STATE] = null;

        // update the union of the requested attributes and the ones currently set
        int numVertexAttribArrays = (vertexAttribArrays == null) ? 0 : vertexAttribArrays.length;
        int lastVertexAttribIndex = firstVertexAttribIndex + numVertexAttribArrays;
        for (int ii = Math.min(_vertexAttribArrayStart, firstVertexAttribIndex),
                nn = Math.max(_vertexAttribArrayEnd, lastVertexAttribIndex); ii < nn; ii++) {
            ClientArrayRecord arec = _vertexAttribArrays[ii];
            ClientArray vertexAttribArray =
                (ii >= firstVertexAttribIndex && ii < lastVertexAttribIndex) ?
                    vertexAttribArrays[ii - firstVertexAttribIndex] : null;
            boolean enableVertexAttribArray = (vertexAttribArray != null);
            if (arec.array == vertexAttribArray &&
                    !(enableVertexAttribArray && vertexAttribArray.dirty)) {
                continue;
            }
            if (arec.enabled != Boolean.valueOf(enableVertexAttribArray)) {
                if (arec.enabled = enableVertexAttribArray) {
                    ARBVertexShader.glEnableVertexAttribArrayARB(ii);
                } else {
                    ARBVertexShader.glDisableVertexAttribArrayARB(ii);
                }
            }
            arec.array = vertexAttribArray;
            if (!enableVertexAttribArray) {
                continue;
            }
            vertexAttribArray.dirty = false;
            if (!arec.equals(vertexAttribArray)) {
                setVertexAttribArray(ii, arec.set(vertexAttribArray));
            }
        }
        _vertexAttribArrayStart = firstVertexAttribIndex;
        _vertexAttribArrayEnd = lastVertexAttribIndex;

        // update the union of the requested tex coords and the ones already set
        int numTexCoordArrays = (texCoordArrays == null) ? 0 : texCoordArrays.length;
        for (int ii = 0, nn = Math.max(_texCoordArrayEnd, numTexCoordArrays); ii < nn; ii++) {
            ClientArrayRecord arec = _texCoordArrays[ii];
            ClientArray texCoordArray = (ii < numTexCoordArrays) ? texCoordArrays[ii] : null;
            boolean enableTexCoordArray = (texCoordArray != null);
            if (arec.array == texCoordArray && !(enableTexCoordArray && texCoordArray.dirty)) {
                continue;
            }
            if (arec.enabled != Boolean.valueOf(enableTexCoordArray)) {
                setClientActiveUnit(ii);
                setClientCapability(GL11.GL_TEXTURE_COORD_ARRAY,
                    arec.enabled = enableTexCoordArray);
            }
            arec.array = texCoordArray;
            if (!enableTexCoordArray) {
                continue;
            }
            texCoordArray.dirty = false;
            if (!arec.equals(texCoordArray)) {
                setClientActiveUnit(ii);
                setTexCoordArray(arec.set(texCoordArray));
            }
        }
        _texCoordArrayEnd = numTexCoordArrays;

        boolean enableColorArray = (colorArray != null);
        if (_colorArray.array != colorArray || (enableColorArray && colorArray.dirty)) {
            if (_colorArray.enabled != Boolean.valueOf(enableColorArray)) {
                setClientCapability(GL11.GL_COLOR_ARRAY,
                    _colorArray.enabled = enableColorArray);
            }
            _colorArray.array = colorArray;
            if (enableColorArray) {
                colorArray.dirty = false;
                if (!_colorArray.equals(colorArray)) {
                    setColorArray(_colorArray.set(colorArray));
                }
            }
        }

        boolean enableNormalArray = (normalArray != null);
        if (_normalArray.array != normalArray || (enableNormalArray && normalArray.dirty)) {
            if (_normalArray.enabled != Boolean.valueOf(enableNormalArray)) {
                setClientCapability(GL11.GL_NORMAL_ARRAY,
                    _normalArray.enabled = enableNormalArray);
            }
            _normalArray.array = normalArray;
            if (enableNormalArray) {
                normalArray.dirty = false;
                if (!_normalArray.equals(normalArray)) {
                    setNormalArray(_normalArray.set(normalArray));
                }
            }
        }

        boolean enableVertexArray = (vertexArray != null);
        if (_vertexArray.array != vertexArray || (enableVertexArray && vertexArray.dirty)) {
            if (_vertexArray.enabled != Boolean.valueOf(enableVertexArray)) {
                setClientCapability(GL11.GL_VERTEX_ARRAY,
                    _vertexArray.enabled = enableVertexArray);
            }
            _vertexArray.array = vertexArray;
            if (enableVertexArray) {
                vertexArray.dirty = false;
                if (!_vertexArray.equals(vertexArray)) {
                    setVertexArray(_vertexArray.set(vertexArray));
                }
            }
        }

        if (_elementArrayBuffer != elementArrayBuffer) {
            int id = (elementArrayBuffer == null) ? 0 : elementArrayBuffer.getId();
            ARBBufferObject.glBindBufferARB(
                ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, id);
            _elementArrayBuffer = elementArrayBuffer;
        }
    }

    /**
     * Invalidates the array state, forcing it to be reapplied.
     */
    public void invalidateArrayState ()
    {
        for (ClientArrayRecord arec : _vertexAttribArrays) {
            arec.invalidate();
        }
        _vertexAttribArrayStart = 0;
        _vertexAttribArrayEnd = _vertexAttribArrays.length;
        for (ClientArrayRecord arec : _texCoordArrays) {
            arec.invalidate();
        }
        _texCoordArrayEnd = _texCoordArrays.length;
        _colorArray.invalidate();
        _normalArray.invalidate();
        _vertexArray.invalidate();
        if (GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
            _elementArrayBuffer = INVALID_BUFFER;
        }
        _states[RenderState.ARRAY_STATE] = null;
    }

    /**
     * Sets the draw color state.
     */
    public void setColorState (Color4f color)
    {
        if (!_color.equals(color)) {
            GL11.glColor4f(color.r, color.g, color.b, color.a);
            _color.set(color);
            _states[RenderState.COLOR_STATE] = null;
        }
    }

    /**
     * Sets the draw color state.
     */
    public void setColorState (float r, float g, float b, float a)
    {
        if (_color.r != r || _color.g != g || _color.b != b || _color.a != a) {
            GL11.glColor4f(r, g, b, a);
            _color.set(r, g, b, a);
            _states[RenderState.COLOR_STATE] = null;
        }
    }

    /**
     * Invalidates the color state, forcing it to be reapplied.
     */
    public void invalidateColorState ()
    {
        _color.r = -1f;
        _states[RenderState.COLOR_STATE] = null;
    }

    /**
     * Sets the color mask state.
     */
    public void setColorMaskState (boolean red, boolean green, boolean blue, boolean alpha)
    {
        if (_redMask != Boolean.valueOf(red) ||
            _greenMask != Boolean.valueOf(green) ||
            _blueMask != Boolean.valueOf(blue) ||
            _alphaMask != Boolean.valueOf(alpha)) {
            GL11.glColorMask(
                _redMask = red, _greenMask = green, _blueMask = blue, _alphaMask = alpha);
            _states[RenderState.COLOR_MASK_STATE] = null;
        }
    }

    /**
     * Invalidates the color mask state, forcing it to be reapplied.
     */
    public void invalidateColorMaskState ()
    {
        _redMask = _greenMask = _blueMask = _alphaMask = null;
        _states[RenderState.COLOR_MASK_STATE] = null;
    }

    /**
     * Sets the back-face culling state.
     */
    public void setCullState (int cullFace)
    {
        // clear any cached reference
        _states[RenderState.CULL_STATE] = null;

        boolean cullFaceEnabled = (cullFace != -1);
        if (_cullFaceEnabled != Boolean.valueOf(cullFaceEnabled)) {
            setCapability(GL11.GL_CULL_FACE, _cullFaceEnabled = cullFaceEnabled);
        }
        if (cullFaceEnabled && _cullFace != cullFace) {
            GL11.glCullFace(_cullFace = cullFace);
        }
    }

    /**
     * Invalidates the back-face culling state, forcing it to be reapplied.
     */
    public void invalidateCullState ()
    {
        _cullFaceEnabled = null;
        _cullFace = -1;
        _states[RenderState.CULL_STATE] = null;
    }

    /**
     * Sets the depth buffer testing/writing state.  If <code>depthTestFunc</code> is
     * {@link GL11#GL_ALWAYS} and <code>depthMask</code> is <code>false</code>,
     * depth testing will be disabled.
     */
    public void setDepthState (int depthTestFunc, boolean depthMask)
    {
        // clear any cached reference
        _states[RenderState.DEPTH_STATE] = null;

        boolean depthTestEnabled = (depthTestFunc != GL11.GL_ALWAYS || depthMask);
        if (_depthTestEnabled != Boolean.valueOf(depthTestEnabled)) {
            setCapability(GL11.GL_DEPTH_TEST, _depthTestEnabled = depthTestEnabled);
        }
        if (!depthTestEnabled) {
            return;
        }
        if (_depthTestFunc != depthTestFunc) {
            GL11.glDepthFunc(_depthTestFunc = depthTestFunc);
        }
        if (_depthMask != Boolean.valueOf(depthMask)) {
            GL11.glDepthMask(_depthMask = depthMask);
        }
    }

    /**
     * Invalidates the depth state, forcing it to be reapplied.
     */
    public void invalidateDepthState ()
    {
        _depthTestEnabled = _depthMask = null;
        _depthTestFunc = -1;
        _states[RenderState.DEPTH_STATE] = null;
    }

    /**
     * Sets the fog state.  If <code>fogMode</code> is -1, fog will be disabled.
     */
    public void setFogState (int fogMode, float fogDensity, Color4f fogColor)
    {
        // clear any cached reference
        _states[RenderState.FOG_STATE] = null;

        _wouldEnableFog = (fogMode != -1);
        updateFogEnabled();
        if (!_wouldEnableFog) {
            return;
        }
        if (_fogMode != fogMode) {
            GL11.glFogi(GL11.GL_FOG_MODE, _fogMode = fogMode);
        }
        if (_fogDensity != fogDensity) {
            GL11.glFogf(GL11.GL_FOG_DENSITY, _fogDensity = fogDensity);
        }
        if (!_fogColor.equals(fogColor)) {
            _fogColor.set(fogColor).get(_vbuf).rewind();
            GL11.glFog(GL11.GL_FOG_COLOR, _vbuf);
        }
    }


    /**
     * Sets the linear fog state.  If <code>fogMode</code> is -1, fog will be disabled.
     */
    public void setFogState (int fogMode, float fogStart, float fogEnd, Color4f fogColor)
    {
        // clear any cached reference
        _states[RenderState.FOG_STATE] = null;

        _wouldEnableFog = (fogMode != -1);
        updateFogEnabled();
        if (!_wouldEnableFog) {
            return;
        }
        if (_fogMode != fogMode) {
            GL11.glFogi(GL11.GL_FOG_MODE, _fogMode = fogMode);
        }
        if (_fogStart != fogStart) {
            GL11.glFogf(GL11.GL_FOG_START, _fogStart = fogStart);
        }
        if (_fogEnd != fogEnd) {
            GL11.glFogf(GL11.GL_FOG_END, _fogEnd = fogEnd);
        }
        if (!_fogColor.equals(fogColor)) {
            _fogColor.set(fogColor).get(_vbuf).rewind();
            GL11.glFog(GL11.GL_FOG_COLOR, _vbuf);
        }
    }

    /**
     * Invalidates the fog state, forcing it to be reapplied.
     */
    public void invalidateFogState ()
    {
        _fogEnabled = null;
        _fogMode = -1;
        _fogDensity = _fogStart = _fogEnd = -1f;
        _fogColor.a = -1;
        _states[RenderState.FOG_STATE] = null;
    }

    /**
     * Sets the light state.  If <code>lights</code> is null, lighting will be disabled.
     */
    public void setLightState (Light[] lights, Color4f globalAmbient)
    {
        // invalidate any cached reference
        _states[RenderState.LIGHT_STATE] = null;

        boolean lightingEnabled = (lights != null);
        if (_lightingEnabled != Boolean.valueOf(lightingEnabled)) {
            setCapability(GL11.GL_LIGHTING, _lightingEnabled = lightingEnabled);
        }
        if (!lightingEnabled) {
            return;
        }
        // update the union of the requested lights and the ones already set
        int numLights = (lights == null) ? 0 : lights.length;
        boolean cleared = false;
        for (int ii = 0, nn = Math.max(_lightEnd, numLights); ii < nn; ii++) {
            LightRecord lrec = _lights[ii];
            Light light = (ii < numLights) ? lights[ii] : null;
            boolean lightEnabled = (light != null);
            if (lrec.light == light && !(lightEnabled && light.dirty)) {
                continue;
            }
            int lname = GL11.GL_LIGHT0 + ii;
            if (lrec.enabled != Boolean.valueOf(lightEnabled)) {
                setCapability(lname, lrec.enabled = lightEnabled);
            }
            lrec.light = light;
            if (!lightEnabled) {
                continue;
            }
            light.dirty = false;
            if (!lrec.ambient.equals(light.ambient)) {
                lrec.ambient.set(light.ambient).get(_vbuf).rewind();
                GL11.glLight(lname, GL11.GL_AMBIENT, _vbuf);
            }
            if (!lrec.diffuse.equals(light.diffuse)) {
                lrec.diffuse.set(light.diffuse).get(_vbuf).rewind();
                GL11.glLight(lname, GL11.GL_DIFFUSE, _vbuf);
            }
            if (!lrec.specular.equals(light.specular)) {
                lrec.specular.set(light.specular).get(_vbuf).rewind();
                GL11.glLight(lname, GL11.GL_SPECULAR, _vbuf);
            }
            if (!lrec.position.equals(light.position)) {
                // OpenGL multiplies by the modelview matrix, so we have to clear it first
                cleared = maybeClearModelview(cleared);
                lrec.position.set(light.position).get(_vbuf).rewind();
                GL11.glLight(lname, GL11.GL_POSITION, _vbuf);
            }
            if (lrec.spotExponent != light.spotExponent) {
                GL11.glLightf(lname, GL11.GL_SPOT_EXPONENT,
                    lrec.spotExponent = light.spotExponent);
            }
            if (lrec.spotCutoff != light.spotCutoff) {
                GL11.glLightf(lname, GL11.GL_SPOT_CUTOFF,
                    lrec.spotCutoff = light.spotCutoff);
            }
            if (light.spotCutoff != 180f && !lrec.spotDirection.equals(light.spotDirection)) {
                // as with the position, clear the modelview matrix
                cleared = maybeClearModelview(cleared);
                lrec.spotDirection.set(light.spotDirection).get(_vbuf).rewind();
                GL11.glLight(lname, GL11.GL_SPOT_DIRECTION, _vbuf);
            }
            if (light.position.w == 0f) {
                continue; // light is directional; the rest does not apply
            }
            if (lrec.constantAttenuation != light.constantAttenuation) {
                GL11.glLightf(lname, GL11.GL_CONSTANT_ATTENUATION,
                    lrec.constantAttenuation = light.constantAttenuation);
            }
            if (lrec.linearAttenuation != light.linearAttenuation) {
                GL11.glLightf(lname, GL11.GL_LINEAR_ATTENUATION,
                    lrec.linearAttenuation = light.linearAttenuation);
            }
            if (lrec.quadraticAttenuation != light.quadraticAttenuation) {
                GL11.glLightf(lname, GL11.GL_QUADRATIC_ATTENUATION,
                    lrec.quadraticAttenuation = light.quadraticAttenuation);
            }
        }
        _lightEnd = numLights;
        maybeRestoreModelview(cleared);

        if (!_globalAmbient.equals(globalAmbient)) {
            _globalAmbient.set(globalAmbient).get(_vbuf).rewind();
            GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, _vbuf);
        }
    }

    /**
     * Invalidates the light state, forcing it to be reapplied.
     */
    public void invalidateLightState ()
    {
        _lightingEnabled = null;
        for (LightRecord lrec : _lights) {
            lrec.invalidate();
        }
        _lightEnd = _lights.length;
        _globalAmbient.a = -1f;
        _states[RenderState.LIGHT_STATE] = null;
    }

    /**
     * Sets the line state.
     */
    public void setLineState (float lineWidth)
    {
        if (_lineWidth != lineWidth) {
            GL11.glLineWidth(_lineWidth = lineWidth);
            _states[RenderState.LINE_STATE] = null;
        }
    }

    /**
     * Invalidates the line state, forcing it to be reapplied.
     */
    public void invalidateLineState ()
    {
        _lineWidth = -1f;
        _states[RenderState.LINE_STATE] = null;
    }

    /**
     * Sets the material state.
     */
    public void setMaterialState (
        Color4f frontAmbient, Color4f frontDiffuse, Color4f frontSpecular, Color4f frontEmission,
            float frontShininess,
        Color4f backAmbient, Color4f backDiffuse, Color4f backSpecular, Color4f backEmission,
            float backShininess,
        int colorMaterialMode, int colorMaterialFace,
        boolean twoSide, boolean localViewer, boolean separateSpecular, boolean flatShading)
    {
        // invalidate any cached reference
        _states[RenderState.MATERIAL_STATE] = null;

        if (!_frontAmbient.equals(frontAmbient)) {
            _frontAmbient.set(frontAmbient).get(_vbuf).rewind();
            GL11.glMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT, _vbuf);
        }
        if (!_frontDiffuse.equals(frontDiffuse)) {
            _frontDiffuse.set(frontDiffuse).get(_vbuf).rewind();
            GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, _vbuf);
        }
        if (!_frontSpecular.equals(frontSpecular)) {
            _frontSpecular.set(frontSpecular).get(_vbuf).rewind();
            GL11.glMaterial(GL11.GL_FRONT, GL11.GL_SPECULAR, _vbuf);
        }
        if (!_frontEmission.equals(frontEmission)) {
            _frontEmission.set(frontEmission).get(_vbuf).rewind();
            GL11.glMaterial(GL11.GL_FRONT, GL11.GL_EMISSION, _vbuf);
        }
        if (_frontShininess != frontShininess) {
            GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS, _frontShininess = frontShininess);
        }
        if (twoSide) {
            if (!_backAmbient.equals(backAmbient)) {
                _backAmbient.set(backAmbient).get(_vbuf).rewind();
                GL11.glMaterial(GL11.GL_BACK, GL11.GL_AMBIENT, _vbuf);
            }
            if (!_backDiffuse.equals(backDiffuse)) {
                _backDiffuse.set(backDiffuse).get(_vbuf).rewind();
                GL11.glMaterial(GL11.GL_BACK, GL11.GL_DIFFUSE, _vbuf);
            }
            if (!_backSpecular.equals(backSpecular)) {
                _backSpecular.set(backSpecular).get(_vbuf).rewind();
                GL11.glMaterial(GL11.GL_BACK, GL11.GL_SPECULAR, _vbuf);
            }
            if (!_backEmission.equals(backEmission)) {
                _backEmission.set(backEmission).get(_vbuf).rewind();
                GL11.glMaterial(GL11.GL_BACK, GL11.GL_EMISSION, _vbuf);
            }
            if (_backShininess != backShininess) {
                GL11.glMaterialf(GL11.GL_BACK, GL11.GL_SHININESS, _backShininess = backShininess);
            }
        }

        boolean colorMaterialEnabled = (colorMaterialMode != -1);
        if (_colorMaterialEnabled != Boolean.valueOf(colorMaterialEnabled)) {
            setCapability(GL11.GL_COLOR_MATERIAL, _colorMaterialEnabled = colorMaterialEnabled);
        }
        if (colorMaterialEnabled && (_colorMaterialFace != colorMaterialFace ||
                _colorMaterialMode != colorMaterialMode)) {
            GL11.glColorMaterial(
                _colorMaterialFace = colorMaterialFace, _colorMaterialMode = colorMaterialMode);
        }

        if (_twoSide != Boolean.valueOf(twoSide)) {
            GL11.glLightModeli(GL11.GL_LIGHT_MODEL_TWO_SIDE, (_twoSide = twoSide) ? 1 : 0);
        }
        if (_localViewer != Boolean.valueOf(localViewer)) {
            GL11.glLightModeli(GL11.GL_LIGHT_MODEL_LOCAL_VIEWER,
                (_localViewer = localViewer) ? 1 : 0);
        }
        if (_separateSpecular != Boolean.valueOf(separateSpecular)) {
            GL11.glLightModeli(GL12.GL_LIGHT_MODEL_COLOR_CONTROL,
                (_separateSpecular = separateSpecular) ?
                    GL12.GL_SEPARATE_SPECULAR_COLOR : GL12.GL_SINGLE_COLOR);
        }
        if (_flatShading != Boolean.valueOf(flatShading)) {
            GL11.glShadeModel((_flatShading = flatShading) ? GL11.GL_FLAT : GL11.GL_SMOOTH);
        }
    }

    /**
     * Invalidates the material state, forcing it to be reapplied.
     */
    public void invalidateMaterialState ()
    {
        _frontAmbient.a = _frontDiffuse.a = _frontSpecular.a = _frontEmission.a = -1f;
        _backAmbient.a = _backDiffuse.a = _backSpecular.a = _backEmission.a = -1f;
        _frontShininess = _backShininess = -1f;
        _colorMaterialEnabled = _twoSide = _localViewer = _separateSpecular = _flatShading = null;
        _colorMaterialFace = _colorMaterialMode = -1;
        _states[RenderState.MATERIAL_STATE] = null;
    }

    /**
     * Sets the point state.
     */
    public void setPointState (float pointSize)
    {
        if (_pointSize != pointSize) {
            GL11.glPointSize(_pointSize = pointSize);
            _states[RenderState.POINT_STATE] = null;
        }
    }

    /**
     * Invalidates the point state, forcing it to be reapplied.
     */
    public void invalidatePointState ()
    {
        _pointSize = -1f;
        _states[RenderState.POINT_STATE] = null;
    }

    /**
     * Sets the polygon state.
     */
    public void setPolygonState (
        int frontPolygonMode, int backPolygonMode, float polygonOffsetFactor,
        float polygonOffsetUnits)
    {
        // invalidate any cached reference
        _states[RenderState.POLYGON_STATE] = null;

        if (_frontPolygonMode != frontPolygonMode) {
            GL11.glPolygonMode(GL11.GL_FRONT, _frontPolygonMode = frontPolygonMode);
        }
        if (_backPolygonMode != backPolygonMode) {
            GL11.glPolygonMode(GL11.GL_BACK, _backPolygonMode = backPolygonMode);
        }
        boolean enablePolygonOffset = (polygonOffsetFactor != 0f || polygonOffsetUnits != 0f);
        if ((frontPolygonMode == GL11.GL_FILL || backPolygonMode == GL11.GL_FILL) &&
                _polygonOffsetFillEnabled != Boolean.valueOf(enablePolygonOffset)) {
            setCapability(GL11.GL_POLYGON_OFFSET_FILL,
                _polygonOffsetFillEnabled = enablePolygonOffset);
        }
        if ((frontPolygonMode == GL11.GL_LINE || backPolygonMode == GL11.GL_LINE) &&
                _polygonOffsetLineEnabled != Boolean.valueOf(enablePolygonOffset)) {
            setCapability(GL11.GL_POLYGON_OFFSET_LINE,
                _polygonOffsetLineEnabled = enablePolygonOffset);
        }
        if ((frontPolygonMode == GL11.GL_POINT || backPolygonMode == GL11.GL_POINT) &&
                _polygonOffsetPointEnabled != Boolean.valueOf(enablePolygonOffset)) {
            setCapability(GL11.GL_POLYGON_OFFSET_POINT,
                _polygonOffsetPointEnabled = enablePolygonOffset);
        }
        if (enablePolygonOffset && (_polygonOffsetFactor != polygonOffsetFactor ||
                _polygonOffsetUnits != polygonOffsetUnits)) {
            GL11.glPolygonOffset(
                _polygonOffsetFactor = polygonOffsetFactor,
                _polygonOffsetUnits = polygonOffsetUnits);
        }
    }

    /**
     * Invalidates the polygon state, forcing it to be reapplied.
     */
    public void invalidatePolygonState ()
    {
        _frontPolygonMode = _backPolygonMode = -1;
        _polygonOffsetFactor = _polygonOffsetUnits = Float.NaN;
        _polygonOffsetFillEnabled = _polygonOffsetLineEnabled = _polygonOffsetPointEnabled = null;
        _states[RenderState.POLYGON_STATE] = null;
    }

    /**
     * Sets the GLSL shader state.
     */
    public void setShaderState (Program program, boolean vertexProgramTwoSide)
    {
        if (_program != program) {
            int id = (program == null) ? 0 : program.getId();
            ARBShaderObjects.glUseProgramObjectARB(id);
            _program = program;
            _states[RenderState.SHADER_STATE] = null;
        }
        if (program != null && program.getVertexShader() != null &&
                _vertexProgramTwoSide != Boolean.valueOf(vertexProgramTwoSide)) {
            setCapability(ARBVertexShader.GL_VERTEX_PROGRAM_TWO_SIDE_ARB,
                _vertexProgramTwoSide = vertexProgramTwoSide);
        }

        // fog state depends on shader state
        updateFogEnabled();
    }

    /**
     * Invalidates the shader state, forcing it to be reapplied.
     */
    public void invalidateShaderState ()
    {
        if (GLContext.getCapabilities().GL_ARB_shader_objects) {
            _program = INVALID_PROGRAM;
            _vertexProgramTwoSide = null;
        }
        _states[RenderState.SHADER_STATE] = null;
    }

    /**
     * Sets the stencil state.
     */
    public void setStencilState (
        int stencilTestFunc, int stencilTestRef, int stencilTestMask,
        int stencilFailOp, int stencilDepthFailOp, int stencilPassOp,
        int stencilWriteMask)
    {
        // invalidate any cached reference
        _states[RenderState.STENCIL_STATE] = null;

        boolean stencilTestEnabled = (stencilTestFunc != GL11.GL_ALWAYS ||
            stencilDepthFailOp != GL11.GL_KEEP || stencilPassOp != GL11.GL_KEEP);
        if (_stencilTestEnabled != Boolean.valueOf(stencilTestEnabled)) {
            setCapability(GL11.GL_STENCIL_TEST, _stencilTestEnabled = stencilTestEnabled);
        }
        if (!stencilTestEnabled) {
            return;
        }
        if (_stencilTestFunc != stencilTestFunc || _stencilTestRef != stencilTestRef ||
            _stencilTestMask != stencilTestMask) {
            GL11.glStencilFunc(
                _stencilTestFunc = stencilTestFunc,
                _stencilTestRef = stencilTestRef,
                _stencilTestMask = stencilTestMask);
        }
        if (_stencilFailOp != stencilFailOp || _stencilDepthFailOp != stencilDepthFailOp ||
            _stencilPassOp != stencilPassOp) {
            GL11.glStencilOp(
                _stencilFailOp = stencilFailOp,
                _stencilDepthFailOp = stencilDepthFailOp,
                _stencilPassOp = stencilPassOp);
        }
        if (_stencilWriteMask != stencilWriteMask) {
            GL11.glStencilMask(_stencilWriteMask = stencilWriteMask);
        }
    }

    /**
     * Invalidates the stencil state, forcing it to be reapplied.
     */
    public void invalidateStencilState ()
    {
        _stencilTestEnabled = null;
        _stencilTestFunc = _stencilTestRef = _stencilTestMask = -1;
        _stencilFailOp = _stencilDepthFailOp = _stencilPassOp = -1;
        _stencilWriteMask = -1;
        _states[RenderState.STENCIL_STATE] = null;
    }

    /**
     * Sets the texture state.
     */
    public void setTextureState (TextureUnit[] units)
    {
        // clear any cached reference
        _states[RenderState.TEXTURE_STATE] = null;

        // update the union of the requested units and the ones already set
        int numUnits = (units == null) ? 0 : units.length;
        boolean cleared = false;
        for (int ii = 0, nn = Math.max(_unitEnd, numUnits); ii < nn; ii++) {
            TextureUnitRecord urec = _units[ii];
            TextureUnit unit = (ii < numUnits) ? units[ii] : null;
            if (unit != null && unit.texture == null) {
                unit = null;
            }
            boolean unitEnabled = (unit != null);
            if (urec.unit == unit && !(unitEnabled && unit.dirty)) {
                continue;
            }
            int target = unitEnabled ? unit.texture.getTarget() : 0;
            boolean enabledCubeMap = (target == ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB);
            if (urec.enabledCubeMap != Boolean.valueOf(enabledCubeMap)) {
                setActiveUnit(ii);
                setCapability(ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB,
                    urec.enabledCubeMap = enabledCubeMap);
            }
            if (!enabledCubeMap) {
                boolean enabled3D = (target == GL12.GL_TEXTURE_3D);
                if (urec.enabled3D != Boolean.valueOf(enabled3D)) {
                    setActiveUnit(ii);
                    setCapability(GL12.GL_TEXTURE_3D, urec.enabled3D = enabled3D);
                }
                if (!enabled3D) {
                    boolean enabledRectangle =
                        (target == ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB);
                    if (urec.enabledRectangle != Boolean.valueOf(enabledRectangle)) {
                        setActiveUnit(ii);
                        setCapability(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB,
                            urec.enabledRectangle = enabledRectangle);
                    }
                    if (!enabledRectangle) {
                        boolean enabled2D = (target == GL11.GL_TEXTURE_2D);
                        if (urec.enabled2D != Boolean.valueOf(enabled2D)) {
                            setActiveUnit(ii);
                            setCapability(GL11.GL_TEXTURE_2D, urec.enabled2D = enabled2D);
                        }
                        if (!enabled2D) {
                            boolean enabled1D = (target == GL11.GL_TEXTURE_1D);
                            if (urec.enabled1D != Boolean.valueOf(enabled1D)) {
                                setActiveUnit(ii);
                                setCapability(GL11.GL_TEXTURE_1D, urec.enabled1D = enabled1D);
                            }
                        }
                    }
                }
            }
            urec.unit = unit;
            if (!unitEnabled) {
                continue;
            }
            unit.dirty = false;
            switch (target) {
                case GL11.GL_TEXTURE_1D:
                    if (urec.texture1D != unit.texture) {
                        setActiveUnit(ii);
                        GL11.glBindTexture(GL11.GL_TEXTURE_1D,
                            (urec.texture1D = unit.texture).getId());
                        _textureChangeCount++;
                    }
                    break;
                case GL11.GL_TEXTURE_2D:
                    if (urec.texture2D != unit.texture) {
                        setActiveUnit(ii);
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D,
                            (urec.texture2D = unit.texture).getId());
                        _textureChangeCount++;
                    }
                    break;
                case ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB:
                    if (urec.textureRectangle != unit.texture) {
                        setActiveUnit(ii);
                        GL11.glBindTexture(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB,
                            (urec.textureRectangle = unit.texture).getId());
                        _textureChangeCount++;
                    }
                    break;
                case GL12.GL_TEXTURE_3D:
                    if (urec.texture3D != unit.texture) {
                        setActiveUnit(ii);
                        GL11.glBindTexture(GL12.GL_TEXTURE_3D,
                            (urec.texture3D = unit.texture).getId());
                        _textureChangeCount++;
                    }
                    break;
                case ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB:
                    if (urec.textureCubeMap != unit.texture) {
                        setActiveUnit(ii);
                        GL11.glBindTexture(ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB,
                            (urec.textureCubeMap = unit.texture).getId());
                        _textureChangeCount++;
                    }
                    break;
            }
            if (urec.envMode != unit.envMode) {
                setActiveUnit(ii);
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE,
                    urec.envMode = unit.envMode);
            }
            boolean combine = (unit.envMode == ARBTextureEnvCombine.GL_COMBINE_ARB);
            if ((combine || unit.envMode == GL11.GL_BLEND) &&
                    !urec.envColor.equals(unit.envColor)) {
                setActiveUnit(ii);
                urec.envColor.set(unit.envColor).get(_vbuf).rewind();
                GL11.glTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, _vbuf);
            }
            if (combine) {
                if (urec.rgbCombine != unit.rgbCombine) {
                    setActiveUnit(ii);
                    GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_COMBINE_RGB_ARB,
                        urec.rgbCombine = unit.rgbCombine);
                }
                if (urec.alphaCombine != unit.alphaCombine) {
                    setActiveUnit(ii);
                    GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_COMBINE_ALPHA_ARB,
                        urec.alphaCombine = unit.alphaCombine);
                }
                if (urec.rgbSource0 != unit.rgbSource0) {
                    setActiveUnit(ii);
                    GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE0_RGB_ARB,
                        urec.rgbSource0 = unit.rgbSource0);
                }
                if (urec.rgbOperand0 != unit.rgbOperand0) {
                    setActiveUnit(ii);
                    GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND0_RGB_ARB,
                        urec.rgbOperand0 = unit.rgbOperand0);
                }
                if (unit.rgbCombine != GL11.GL_REPLACE) {
                    if (urec.rgbSource1 != unit.rgbSource1) {
                        setActiveUnit(ii);
                        GL11.glTexEnvi(
                            GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE1_RGB_ARB,
                            urec.rgbSource1 = unit.rgbSource1);
                    }
                    if (urec.rgbOperand1 != unit.rgbOperand1) {
                        setActiveUnit(ii);
                        GL11.glTexEnvi(
                            GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND1_RGB_ARB,
                            urec.rgbOperand1 = unit.rgbOperand1);
                    }
                    if (unit.rgbCombine == ARBTextureEnvCombine.GL_INTERPOLATE_ARB) {
                        if (urec.rgbSource2 != unit.rgbSource2) {
                            setActiveUnit(ii);
                            GL11.glTexEnvi(
                                GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE2_RGB_ARB,
                                urec.rgbSource2 = unit.rgbSource2);
                        }
                        if (urec.rgbOperand2 != unit.rgbOperand2) {
                            setActiveUnit(ii);
                            GL11.glTexEnvi(
                                GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND2_RGB_ARB,
                                urec.rgbOperand2 = unit.rgbOperand2);
                        }
                    }
                }
                if (urec.alphaSource0 != unit.alphaSource0) {
                    setActiveUnit(ii);
                    GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE0_ALPHA_ARB,
                        urec.alphaSource0 = unit.alphaSource0);
                }
                if (urec.alphaOperand0 != unit.alphaOperand0) {
                    setActiveUnit(ii);
                    GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND0_ALPHA_ARB,
                        urec.alphaOperand0 = unit.alphaOperand0);
                }
                if (unit.alphaCombine != GL11.GL_REPLACE) {
                    if (urec.alphaSource1 != unit.alphaSource1) {
                        setActiveUnit(ii);
                        GL11.glTexEnvi(
                            GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE1_ALPHA_ARB,
                            urec.alphaSource1 = unit.alphaSource1);
                    }
                    if (urec.alphaOperand1 != unit.alphaOperand1) {
                        setActiveUnit(ii);
                        GL11.glTexEnvi(
                            GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND1_ALPHA_ARB,
                            urec.alphaOperand1 = unit.alphaOperand1);
                    }
                    if (unit.alphaCombine == ARBTextureEnvCombine.GL_INTERPOLATE_ARB) {
                        if (urec.alphaSource2 != unit.alphaSource2) {
                            setActiveUnit(ii);
                            GL11.glTexEnvi(
                                GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE2_ALPHA_ARB,
                                urec.alphaSource2 = unit.alphaSource2);
                        }
                        if (urec.alphaOperand2 != unit.alphaOperand2) {
                            setActiveUnit(ii);
                            GL11.glTexEnvi(
                                GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND2_ALPHA_ARB,
                                urec.alphaOperand2 = unit.alphaOperand2);
                        }
                    }
                }
                if (urec.rgbScale != unit.rgbScale) {
                    setActiveUnit(ii);
                    GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_RGB_SCALE_ARB,
                        urec.rgbScale = unit.rgbScale);
                }
                if (urec.alphaScale != unit.alphaScale) {
                    setActiveUnit(ii);
                    GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_ALPHA_SCALE,
                        urec.alphaScale = unit.alphaScale);
                }
            }
            if (urec.lodBias != unit.lodBias) {
                setActiveUnit(ii);
                GL11.glTexEnvf(
                    EXTTextureLODBias.GL_TEXTURE_FILTER_CONTROL_EXT,
                    EXTTextureLODBias.GL_TEXTURE_LOD_BIAS_EXT, urec.lodBias = unit.lodBias);
            }
            boolean genEnabledS = (unit.genModeS != -1);
            if (urec.genEnabledS != Boolean.valueOf(genEnabledS)) {
                setActiveUnit(ii);
                setCapability(GL11.GL_TEXTURE_GEN_S, urec.genEnabledS = genEnabledS);
            }
            if (genEnabledS) {
                if (urec.genModeS != unit.genModeS) {
                    setActiveUnit(ii);
                    GL11.glTexGeni(
                        GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE, urec.genModeS = unit.genModeS);
                }
                if (unit.genModeS == GL11.GL_OBJECT_LINEAR) {
                    if (!urec.genPlaneS.equals(unit.genPlaneS)) {
                        setActiveUnit(ii);
                        urec.genPlaneS.set(unit.genPlaneS).get(_vbuf).rewind();
                        GL11.glTexGen(GL11.GL_S, GL11.GL_OBJECT_PLANE, _vbuf);
                    }
                } else if (unit.genModeS == GL11.GL_EYE_LINEAR) {
                    if (!urec.genEyePlaneS.equals(unit.genPlaneS)) {
                        setActiveUnit(ii);
                        cleared = clearModelviewOrTransposeTransform(
                            cleared, urec.genEyePlaneS.set(unit.genPlaneS), _vbuf);
                        GL11.glTexGen(GL11.GL_S, GL11.GL_EYE_PLANE, _vbuf);
                    }
                }
            }
            boolean genEnabledT = (unit.genModeT != -1);
            if (urec.genEnabledT != Boolean.valueOf(genEnabledT)) {
                setActiveUnit(ii);
                setCapability(GL11.GL_TEXTURE_GEN_T, urec.genEnabledT = genEnabledT);
            }
            if (genEnabledT) {
                if (urec.genModeT != unit.genModeT) {
                    setActiveUnit(ii);
                    GL11.glTexGeni(
                        GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE, urec.genModeT = unit.genModeT);
                }
                if (unit.genModeT == GL11.GL_OBJECT_LINEAR) {
                    if (!urec.genPlaneT.equals(unit.genPlaneT)) {
                        setActiveUnit(ii);
                        urec.genPlaneT.set(unit.genPlaneT).get(_vbuf).rewind();
                        GL11.glTexGen(GL11.GL_T, GL11.GL_OBJECT_PLANE, _vbuf);
                    }
                } else if (unit.genModeT == GL11.GL_EYE_LINEAR) {
                    if (!urec.genEyePlaneT.equals(unit.genPlaneT)) {
                        setActiveUnit(ii);
                        cleared = clearModelviewOrTransposeTransform(
                            cleared, urec.genEyePlaneT.set(unit.genPlaneT), _vbuf);
                        GL11.glTexGen(GL11.GL_T, GL11.GL_EYE_PLANE, _vbuf);
                    }
                }
            }
            boolean genEnabledR = (unit.genModeR != -1);
            if (urec.genEnabledR != Boolean.valueOf(genEnabledR)) {
                setActiveUnit(ii);
                setCapability(GL11.GL_TEXTURE_GEN_R, urec.genEnabledR = genEnabledR);
            }
            if (genEnabledR) {
                if (urec.genModeR != unit.genModeR) {
                    setActiveUnit(ii);
                    GL11.glTexGeni(
                        GL11.GL_R, GL11.GL_TEXTURE_GEN_MODE, urec.genModeR = unit.genModeR);
                }
                if (unit.genModeR == GL11.GL_OBJECT_LINEAR) {
                    if (!urec.genPlaneR.equals(unit.genPlaneR)) {
                        setActiveUnit(ii);
                        urec.genPlaneR.set(unit.genPlaneR).get(_vbuf).rewind();
                        GL11.glTexGen(GL11.GL_R, GL11.GL_OBJECT_PLANE, _vbuf);
                    }
                } else if (unit.genModeR == GL11.GL_EYE_LINEAR) {
                    if (!urec.genEyePlaneR.equals(unit.genPlaneR)) {
                        setActiveUnit(ii);
                        cleared = clearModelviewOrTransposeTransform(
                            cleared, urec.genEyePlaneR.set(unit.genPlaneR), _vbuf);
                        GL11.glTexGen(GL11.GL_R, GL11.GL_EYE_PLANE, _vbuf);
                    }
                }
            }
            boolean genEnabledQ = (unit.genModeQ != -1);
            if (urec.genEnabledQ != Boolean.valueOf(genEnabledQ)) {
                setActiveUnit(ii);
                setCapability(GL11.GL_TEXTURE_GEN_Q, urec.genEnabledQ = genEnabledQ);
            }
            if (genEnabledQ) {
                if (urec.genModeQ != unit.genModeQ) {
                    setActiveUnit(ii);
                    GL11.glTexGeni(
                        GL11.GL_Q, GL11.GL_TEXTURE_GEN_MODE, urec.genModeQ = unit.genModeQ);
                }
                if (unit.genModeQ == GL11.GL_OBJECT_LINEAR) {
                    if (!urec.genPlaneQ.equals(unit.genPlaneQ)) {
                        setActiveUnit(ii);
                        urec.genPlaneQ.set(unit.genPlaneQ).get(_vbuf).rewind();
                        GL11.glTexGen(GL11.GL_Q, GL11.GL_OBJECT_PLANE, _vbuf);
                    }
                } else if (unit.genModeQ == GL11.GL_EYE_LINEAR) {
                    if (!urec.genEyePlaneQ.equals(unit.genPlaneQ)) {
                        setActiveUnit(ii);
                        cleared = clearModelviewOrTransposeTransform(
                            cleared, urec.genEyePlaneQ.set(unit.genPlaneQ), _vbuf);
                        GL11.glTexGen(GL11.GL_Q, GL11.GL_EYE_PLANE, _vbuf);
                    }
                }
            }
            if (!urec.transform.equals(unit.transform)) {
                setActiveUnit(ii);
                setMatrixMode(GL11.GL_TEXTURE);
                loadTransformMatrix(urec.transform.set(unit.transform));
            }
        }
        _unitEnd = numUnits;
        maybeRestoreModelview(cleared);
    }

    /**
     * Invalidates the texture state, forcing it to be reapplied.
     */
    public void invalidateTextureState ()
    {
        for (TextureUnitRecord urec : _units) {
            urec.invalidate();
        }
        _unitEnd = _units.length;
        _states[RenderState.TEXTURE_STATE] = null;
    }

    /**
     * Sets the transform state.
     */
    public void setTransformState (Transform3D modelview)
    {
        if (!_modelview.equals(modelview)) {
            setMatrixMode(GL11.GL_MODELVIEW);
            loadTransformMatrix(_modelview.set(modelview));

            // set the normalization based on the transform type
            int type = modelview.getType();
            if (type == Transform3D.IDENTITY || type == Transform3D.RIGID) {
                setNormalize(false, false);
            } else if (type == Transform3D.UNIFORM &&
                    GLContext.getCapabilities().GL_EXT_rescale_normal && !RunAnywhere.isMacOS()) {
                // OS X has a bug where the normal scale affects the parameters to glTexGen, so
                // we just disable it there
                setNormalize(false, true);
            } else {
                setNormalize(true, false);
            }

            // invalidate associated state
            _states[RenderState.TRANSFORM_STATE] = null;
            _modelviewMatrixValid = false;
        }
    }

    /**
     * Invalidates the transform state, forcing it to be reapplied.
     */
    public void invalidateTransformState ()
    {
        _modelview.setType(-1);
        _states[RenderState.TRANSFORM_STATE] = null;
        _modelviewMatrixValid = false;
    }

    /**
     * Sets the matrix mode.
     */
    public void setMatrixMode (int matrixMode)
    {
        if (_matrixMode != matrixMode) {
            GL11.glMatrixMode(_matrixMode = matrixMode);
        }
    }

    /**
     * Renders the provided list of batches.
     */
    public void render (List<Batch> batches)
    {
        // for each batch, set the states and call its draw command
        int size = batches.size();
        for (int ii = 0; ii < size; ii++) {
            Batch batch = batches.get(ii);
            if (batch.draw(this) || _colorArray.enabled != Boolean.FALSE) {
                // invalidate the color state if we used a color array or a display list
                // with color info
                invalidateColorState();
            }
            _primitiveCount += batch.getPrimitiveCount();
        }
        _batchCount += size;
    }

    /**
     * Sets one of the vertex attribute arrays to the one supplied.
     */
    protected void setVertexAttribArray (int idx, ClientArray array)
    {
        if (array.arrayBuffer != null) {
            setArrayBuffer(array.arrayBuffer);
            ARBVertexShader.glVertexAttribPointerARB(
                idx, array.size, array.type, array.normalized, array.stride, array.offset);
        } else { // array.floatArray != null
            setArrayBuffer(null);
            array.floatArray.position((int)array.offset / 4); // offsets are in bytes
            ARBVertexShader.glVertexAttribPointerARB(
                idx, array.size, array.normalized, array.stride, array.floatArray);
            array.floatArray.rewind();
        }
    }

    /**
     * Sets the texture coordinate array to the one supplied.
     */
    protected void setTexCoordArray (ClientArray array)
    {
        if (array.arrayBuffer != null) {
            setArrayBuffer(array.arrayBuffer);
            GL11.glTexCoordPointer(array.size, array.type, array.stride, array.offset);
        } else { // array.floatArray != null
            setArrayBuffer(null);
            array.floatArray.position((int)array.offset / 4);
            GL11.glTexCoordPointer(array.size, array.stride, array.floatArray);
            array.floatArray.rewind();
        }
    }

    /**
     * Sets the color array to the one supplied.
     */
    protected void setColorArray (ClientArray array)
    {
        if (array.arrayBuffer != null) {
            setArrayBuffer(array.arrayBuffer);
            GL11.glColorPointer(array.size, array.type, array.stride, array.offset);
        } else { // array.floatArray != null
            setArrayBuffer(null);
            array.floatArray.position((int)array.offset / 4);
            GL11.glColorPointer(array.size, array.stride, array.floatArray);
            array.floatArray.rewind();
        }
    }

    /**
     * Sets the normal array to the one supplied.
     */
    protected void setNormalArray (ClientArray array)
    {
        if (array.arrayBuffer != null) {
            setArrayBuffer(array.arrayBuffer);
            GL11.glNormalPointer(array.type, array.stride, array.offset);
        } else { // array.floatArray != null
            setArrayBuffer(null);
            array.floatArray.position((int)array.offset / 4);
            GL11.glNormalPointer(array.stride, array.floatArray);
            array.floatArray.rewind();
        }
    }

    /**
     * Sets the vertex array to the one supplied.
     */
    protected void setVertexArray (ClientArray array)
    {
        if (array.arrayBuffer != null) {
            setArrayBuffer(array.arrayBuffer);
            GL11.glVertexPointer(array.size, array.type, array.stride, array.offset);
        } else { // array.floatArray != null
            setArrayBuffer(null);
            array.floatArray.position((int)array.offset / 4);
            GL11.glVertexPointer(array.size, array.stride, array.floatArray);
            array.floatArray.rewind();
        }
    }

    /**
     * Binds the specified array buffer.
     */
    protected void setArrayBuffer (BufferObject arrayBuffer)
    {
        if (_arrayBuffer != arrayBuffer) {
            int id = (arrayBuffer == null) ? 0 : arrayBuffer.getId();
            ARBBufferObject.glBindBufferARB(
                ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, id);
            _arrayBuffer = arrayBuffer;
        }
    }

    /**
     * Updates the fog enabled state, which depends on the fog state and the shader state.
     */
    protected void updateFogEnabled ()
    {
        boolean fogEnabled = (_wouldEnableFog && _program == null);
        if (_fogEnabled != Boolean.valueOf(fogEnabled)) {
            setCapability(GL11.GL_FOG, _fogEnabled = fogEnabled);
        }
    }

    /**
     * Loads the specified transform into the current matrix slot.
     */
    protected void loadTransformMatrix (Transform3D transform)
    {
        // supposedly, calling the "typed" matrix functions (glTranslatef, glRotatef, etc.)
        // allows driver optimizations that calling glLoadMatrix doesn't:
        // http://www.opengl.org/resources/code/samples/s2001/perfogl.pdf
        // (but that's kind of an old document; it's not clear that it's worth the extra native
        // calls)
        int type = transform.getType();
        if (type >= Transform3D.AFFINE) {
            transform.getMatrix().get(_vbuf).rewind();
            GL11.glLoadMatrix(_vbuf);
            return;
        }
        GL11.glLoadIdentity();
        if (type == Transform3D.IDENTITY) {
            return;
        }
        Vector3f translation = transform.getTranslation();
        if (!translation.equals(Vector3f.ZERO)) {
            GL11.glTranslatef(translation.x, translation.y, translation.z);
        }
        Quaternion rotation = transform.getRotation();
        if (!rotation.equals(Quaternion.IDENTITY)) {
            float w = FloatMath.clamp(rotation.w, -1f, +1f);
            float angle = 2f * FloatMath.acos(w);
            float rsina = 1f / FloatMath.sqrt(1f - w*w);
            GL11.glRotatef(FloatMath.toDegrees(angle),
                rotation.x * rsina, rotation.y * rsina, rotation.z * rsina);
        }
        if (type == Transform3D.UNIFORM) {
            float scale = transform.getScale();
            if (scale != 1f) {
                GL11.glScalef(scale, scale, scale);
            }
        }
    }

    /**
     * On Intel cards, this clears the modelview matrix and loads the plane coefficients into
     * the supplied buffer.  On non-Intel cards, it multiplies the coefficients by the transpose
     * of the modelview matrix and stores them in the buffer.  It's a workaround for dueling
     * issues with the ATI and Intel drivers.
     *
     * @return true if we cleared the modelview matrix.
     */
    protected boolean clearModelviewOrTransposeTransform (
        boolean cleared, Vector4f vector, FloatBuffer result)
    {
        if (_intel) {
            maybeClearModelview(cleared);
            vector.get(result).rewind();
            return true;

        } else {
            transposeTransform(vector, result);
            return false;
        }
    }

    /**
     * Clears the modelview matrix if it hasn't already been.
     *
     * @return the new value for the cleared flag (true).
     */
    protected boolean maybeClearModelview (boolean cleared)
    {
        if (!cleared) {
            setMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
        }
        return true;
    }

    /**
     * Restores the stored modelview matrix if it was previously cleared.
     */
    protected void maybeRestoreModelview (boolean cleared)
    {
        if (cleared) {
            setMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
        }
    }

    /**
     * Transforms a vector by the transpose of the modelview matrix and stores it in the supplied
     * buffer.
     */
    protected void transposeTransform (Vector4f vector, FloatBuffer result)
    {
        Matrix4f mat = getModelviewMatrix();
        result.put(vector.x*mat.m00 + vector.y*mat.m01 + vector.z*mat.m02);
        result.put(vector.x*mat.m10 + vector.y*mat.m11 + vector.z*mat.m12);
        result.put(vector.x*mat.m20 + vector.y*mat.m21 + vector.z*mat.m22);
        result.put(vector.x*mat.m30 + vector.y*mat.m31 + vector.z*mat.m32 + vector.w);
        result.rewind();
    }

    /**
     * Returns a reference to the modelview matrix, ensuring that it is up-to-date.
     */
    protected Matrix4f getModelviewMatrix ()
    {
        if (!_modelviewMatrixValid) {
            _modelview.update(Transform3D.AFFINE);
            _modelviewMatrixValid = true;
        }
        return _modelview.getMatrix();
    }

    /**
     * Sets the active client texture unit.
     */
    protected void setClientActiveUnit (int unit)
    {
        if (_clientActiveUnit != unit) {
            ARBMultitexture.glClientActiveTextureARB(
                ARBMultitexture.GL_TEXTURE0_ARB + (_clientActiveUnit = unit));
        }
    }

    /**
     * Sets the active texture unit.
     */
    protected void setActiveUnit (int unit)
    {
        if (_activeUnit != unit) {
            ARBMultitexture.glActiveTextureARB(
                ARBMultitexture.GL_TEXTURE0_ARB + (_activeUnit = unit));
        }
    }

    /**
     * Binds the specified texture in the active unit.
     */
    protected void setTexture (Texture texture)
    {
        TextureUnitRecord unit = _units[_activeUnit];
        switch (texture.getTarget()) {
            case GL11.GL_TEXTURE_1D:
                if (unit.texture1D != texture) {
                    GL11.glBindTexture(GL11.GL_TEXTURE_1D, (unit.texture1D = texture).getId());
                    unit.unit = unit; // note that the unit is set
                }
                break;

            case GL11.GL_TEXTURE_2D:
                if (unit.texture2D != texture) {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, (unit.texture2D = texture).getId());
                    unit.unit = unit;
                }
                break;

            case ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB:
                if (unit.textureRectangle != texture) {
                    GL11.glBindTexture(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB,
                        (unit.textureRectangle = texture).getId());
                    unit.unit = unit;
                }
                break;

            case GL12.GL_TEXTURE_3D:
                if (unit.texture3D != texture) {
                    GL11.glBindTexture(GL12.GL_TEXTURE_3D, (unit.texture3D = texture).getId());
                    unit.unit = unit;
                }
                break;

            case ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB:
                if (unit.textureCubeMap != texture) {
                    GL11.glBindTexture(ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB,
                        (unit.textureCubeMap = texture).getId());
                    unit.unit = unit;
                }
                break;
        }
    }

    /**
     * Sets the active buffers for drawing and reading.
     */
    protected void setBuffers (int drawBuffer, int readBuffer)
    {
        if (_drawBuffer != drawBuffer) {
            GL11.glDrawBuffer(_drawBuffer = drawBuffer);
        }
        if (_readBuffer != readBuffer) {
            GL11.glReadBuffer(_readBuffer = readBuffer);
        }
    }

    /**
     * Returns the active draw buffer.
     */
    protected int getDrawBuffer ()
    {
        return _drawBuffer;
    }

    /**
     * Returns the active read buffer.
     */
    protected int getReadBuffer ()
    {
        return _readBuffer;
    }

    /**
     * Binds the specified frame buffer.
     */
    protected void setFramebuffer (Framebuffer framebuffer)
    {
        if (_framebuffer != framebuffer) {
            int id = (framebuffer == null) ? 0 : framebuffer.getId();
            EXTFramebufferObject.glBindFramebufferEXT(
                EXTFramebufferObject.GL_FRAMEBUFFER_EXT, id);
            _framebuffer = framebuffer;
        }
    }

    /**
     * Returns a reference to the currently bound frame buffer.
     */
    protected Framebuffer getFramebuffer ()
    {
        return _framebuffer;
    }

    /**
     * Binds the specified render buffer.
     */
    protected void setRenderbuffer (Renderbuffer renderbuffer)
    {
        if (_renderbuffer != renderbuffer) {
            int id = (renderbuffer == null) ? 0 : renderbuffer.getId();
            EXTFramebufferObject.glBindRenderbufferEXT(
                EXTFramebufferObject.GL_RENDERBUFFER_EXT, id);
            _renderbuffer = renderbuffer;
        }
    }

    /**
     * Notes that a buffer object has been created.
     */
    protected void bufferObjectCreated ()
    {
        _bufferObjectCount++;
    }

    /**
     * Notes that a buffer object's size has changed.
     *
     * @param delta the difference in bytes between the new and old sizes.
     */
    protected void bufferObjectResized (int delta)
    {
        _bufferObjectBytes += delta;
    }

    /**
     * Notes that a buffer object has been deleted.
     */
    protected void bufferObjectDeleted (int bytes)
    {
        _bufferObjectCount--;
        _bufferObjectBytes -= bytes;
    }

    /**
     * Notes that a display list has been created.
     */
    protected void displayListCreated ()
    {
        _displayListCount++;
    }

    /**
     * Notes that a display list has been deleted.
     */
    protected void displayListDeleted ()
    {
        _displayListCount--;
    }

    /**
     * Notes that a shader object has been created.
     */
    protected void shaderObjectCreated ()
    {
        _shaderObjectCount++;
    }

    /**
     * Notes that a shader object has been deleted.
     */
    protected void shaderObjectDeleted ()
    {
        _shaderObjectCount--;
    }

    /**
     * Notes that a texture has been created.
     */
    protected void textureCreated ()
    {
        _textureCount++;
    }

    /**
     * Notes that a texture's size has changed.
     *
     * @param delta the difference in bytes between the new and old sizes.
     */
    protected void textureResized (int delta)
    {
        _textureBytes += delta;
    }

    /**
     * Notes that a texture has been deleted.
     */
    protected void textureDeleted (int bytes)
    {
        _textureCount--;
        _textureBytes -= bytes;
    }

    /**
     * Called when a buffer object has been finalized.
     */
    protected synchronized void bufferObjectFinalized (int id, int bytes)
    {
        _finalizedBufferObjects = IntListUtil.add(_finalizedBufferObjects, id);
        _finalizedBufferObjectBytes += bytes;
    }

    /**
     * Called when a display list has been finalized.
     */
    protected synchronized void displayListFinalized (int id)
    {
        _finalizedDisplayLists = IntListUtil.add(_finalizedDisplayLists, id);
    }

    /**
     * Called when a frame buffer has been finalized.
     */
    protected synchronized void framebufferFinalized (int id)
    {
        _finalizedFramebuffers = IntListUtil.add(_finalizedFramebuffers, id);
    }

    /**
     * Called when a pbuffer has been finalized.
     */
    protected synchronized void pbufferFinalized (Pbuffer pbuffer)
    {
        _finalizedPbuffers = ListUtil.add(_finalizedPbuffers, pbuffer);
    }

    /**
     * Called when a query has been finalized.
     */
    protected synchronized void queryFinalized (int id)
    {
        _finalizedQueries = IntListUtil.add(_finalizedQueries, id);
    }

    /**
     * Called when a render buffer has been finalized.
     */
    protected synchronized void renderbufferFinalized (int id)
    {
        _finalizedRenderbuffers = IntListUtil.add(_finalizedRenderbuffers, id);
    }

    /**
     * Called when a shader object has been finalized.
     */
    protected synchronized void shaderObjectFinalized (int id)
    {
        _finalizedShaderObjects = IntListUtil.add(_finalizedShaderObjects, id);
    }

    /**
     * Called when a texture has been finalized.
     */
    protected synchronized void textureFinalized (int id, int bytes)
    {
        _finalizedTextures = IntListUtil.add(_finalizedTextures, id);
        _finalizedTextureBytes += bytes;
    }

    /**
     * Deletes all finalized objects.
     */
    protected synchronized void deleteFinalizedObjects ()
    {
        if (_finalizedBufferObjects != null) {
            int[] compacted = IntListUtil.compact(_finalizedBufferObjects);
            IntBuffer idbuf = BufferUtils.createIntBuffer(compacted.length);
            idbuf.put(compacted).rewind();
            ARBBufferObject.glDeleteBuffersARB(idbuf);
            _bufferObjectCount -= compacted.length;
            _bufferObjectBytes -= _finalizedBufferObjectBytes;
            _finalizedBufferObjects = null;
            _finalizedBufferObjectBytes = 0;
        }
        if (_finalizedDisplayLists != null) {
            for (int id : _finalizedDisplayLists) {
                if (id != 0) {
                    GL11.glDeleteLists(id, 1);
                    _displayListCount--;
                }
            }
            _finalizedDisplayLists = null;
        }
        if (_finalizedFramebuffers != null) {
            IntBuffer idbuf = BufferUtils.createIntBuffer(_finalizedFramebuffers.length);
            idbuf.put(_finalizedFramebuffers).rewind();
            EXTFramebufferObject.glDeleteFramebuffersEXT(idbuf);
            _finalizedFramebuffers = null;
        }
        if (_finalizedPbuffers != null) {
            for (Object buf : _finalizedPbuffers) {
                if (buf != null) {
                    ((Pbuffer)buf).destroy();
                }
            }
            _finalizedPbuffers = null;
        }
        if (_finalizedQueries != null) {
            IntBuffer idbuf = BufferUtils.createIntBuffer(_finalizedQueries.length);
            idbuf.put(_finalizedQueries).rewind();
            ARBOcclusionQuery.glDeleteQueriesARB(idbuf);
            _finalizedQueries = null;
        }
        if (_finalizedRenderbuffers != null) {
            IntBuffer idbuf = BufferUtils.createIntBuffer(_finalizedRenderbuffers.length);
            idbuf.put(_finalizedRenderbuffers).rewind();
            EXTFramebufferObject.glDeleteRenderbuffersEXT(idbuf);
            _finalizedRenderbuffers = null;
        }
        if (_finalizedShaderObjects != null) {
            for (int id : _finalizedShaderObjects) {
                // technically glDeleteObject is supposed to silently ignore zero values, but
                // instead, at least on some systems, it raises an invalid value error
                if (id != 0) {
                    ARBShaderObjects.glDeleteObjectARB(id);
                    _shaderObjectCount--;
                }
            }
            _finalizedShaderObjects = null;
        }
        if (_finalizedTextures != null) {
            int[] compacted = IntListUtil.compact(_finalizedTextures);
            IntBuffer idbuf = BufferUtils.createIntBuffer(compacted.length);
            idbuf.put(compacted).rewind();
            GL11.glDeleteTextures(idbuf);
            _textureCount -= compacted.length;
            _textureBytes -= _finalizedTextureBytes;
            _finalizedTextures = null;
            _finalizedTextureBytes = 0;
        }
    }

    /**
     * Enables or disables an OpenGL capability.
     */
    protected static void setCapability (int capability, boolean enable)
    {
        if (enable) {
            GL11.glEnable(capability);
        } else {
            GL11.glDisable(capability);
        }
    }

    /**
     * Enables or disables an OpenGL client capability.
     */
    protected static void setClientCapability (int capability, boolean enable)
    {
        if (enable) {
            GL11.glEnableClientState(capability);
        } else {
            GL11.glDisableClientState(capability);
        }
    }

    /**
     * Represents the state of a single clip plane.
     */
    protected static class ClipPlaneRecord extends Plane
    {
        /** Whether or not this clip plane is currently enabled. */
        public Boolean enabled = false;

        /**
         * Invalidates this record, forcing it to be reapplied.
         */
        public void invalidate ()
        {
            enabled = null;
            constant = Float.NaN;
        }
    }

    /**
     * Represents the state of a single client array.
     */
    protected static class ClientArrayRecord extends ClientArray
    {
        /** Whether or not this array is currently enabled. */
        public Boolean enabled = false;

        /** A reference to the last array set. */
        public ClientArray array;

        /**
         * Invalidates this record, forcing it to be reapplied.
         */
        public void invalidate ()
        {
            enabled = null;
            array = this;
            size = -1;
        }
    }

    /**
     * Represents the current state of a single light.
     */
    protected static class LightRecord extends Light
    {
        /** Whether or not this light is currently enabled. */
        public Boolean enabled = false;

        /** A reference to the last light set. */
        public Light light;

        /**
         * Creates a new light record.
         */
        public LightRecord (int idx)
        {
            // the defaults are different for the first light
            if (idx > 0) {
                diffuse.set(0f, 0f, 0f, 0f);
                specular.set(0f, 0f, 0f, 0f);
            }
        }

        /**
         * Invalidates this record, forcing it to be reapplied.
         */
        public void invalidate ()
        {
            enabled = null;
            light = this;
            ambient.r = diffuse.r = specular.r = -1f;
            position.w = Float.NaN;
            spotDirection.x = Float.NaN;
            spotExponent = spotCutoff = -1f;
            constantAttenuation = linearAttenuation = quadraticAttenuation = Float.NaN;
        }
    }

    /**
     * Represents the current state of a single texture unit.
     */
    protected static class TextureUnitRecord extends TextureUnit
    {
        /** Whether or not 1D texturing is enabled for this unit. */
        public Boolean enabled1D = false;

        /** The texture bound to the 1D target. */
        public Texture texture1D;

        /** Whether or not 2D texturing is enabled for this unit. */
        public Boolean enabled2D = false;

        /** The texture bound to the 2D target. */
        public Texture texture2D;

        /** Whether or not rectangular texturing is enabled for this unit. */
        public Boolean enabledRectangle = false;

        /** The texture bound to the rectangular target. */
        public Texture textureRectangle;

        /** Whether or not 3D texturing is enabled for this unit. */
        public Boolean enabled3D = false;

        /** The texture bound to the 3D target. */
        public Texture texture3D;

        /** Whether or not cube map texturing is enabled for this unit. */
        public Boolean enabledCubeMap = false;

        /** The texture bound to the cube map target. */
        public Texture textureCubeMap;

        /** Whether or not texture coordinate generation is enabled for the s coordinate. */
        public Boolean genEnabledS = false;

        /** The s texture coordinate generation eye plane. */
        public Vector4f genEyePlaneS = new Vector4f(1f, 0f, 0f, 0f);

        /** Whether or not texture coordinate generation is enabled for the t coordinate. */
        public Boolean genEnabledT = false;

        /** The t texture coordinate generation eye plane. */
        public Vector4f genEyePlaneT = new Vector4f(0f, 1f, 0f, 0f);

        /** Whether or not texture coordinate generation is enabled for the r coordinate. */
        public Boolean genEnabledR = false;

        /** The r texture coordinate generation eye plane. */
        public Vector4f genEyePlaneR = new Vector4f(0f, 0f, 0f, 0f);

        /** Whether or not texture coordinate generation is enabled for the q coordinate. */
        public Boolean genEnabledQ = false;

        /** The q texture coordinate generation eye plane. */
        public Vector4f genEyePlaneQ = new Vector4f(0f, 0f, 0f, 0f);

        /** A reference to the last unit set. */
        public TextureUnit unit;

        /**
         * Creates a new texture unit record.
         */
        public TextureUnitRecord ()
        {
            genModeS = genModeT = genModeR = genModeQ = GL11.GL_EYE_LINEAR;

            // for some reason the texture matrices aren't starting out as identity on ATI cards,
            // so we have to invalidate them
            transform.setType(-1);
        }

        /**
         * Invalidates this record, forcing it to be reapplied.
         */
        public void invalidate ()
        {
            enabled1D = enabled2D = null;
            texture1D = texture2D = INVALID_TEXTURE;
            if (GLContext.getCapabilities().GL_ARB_texture_rectangle) {
                enabledRectangle = null;
                textureRectangle = INVALID_TEXTURE;
            }
            if (GLContext.getCapabilities().OpenGL12) {
                enabled3D = null;
                texture3D = INVALID_TEXTURE;
            }
            if (GLContext.getCapabilities().GL_ARB_texture_cube_map) {
                enabledCubeMap = null;
                textureCubeMap = INVALID_TEXTURE;
            }
            if (GLContext.getCapabilities().GL_EXT_texture_lod_bias) {
                lodBias = Float.NaN;
            }
            genEnabledS = genEnabledT = genEnabledR = genEnabledQ = null;
            genEyePlaneS.x = genEyePlaneT.y = genEyePlaneR.x = genEyePlaneQ.x = Float.NaN;
            genPlaneS.x = genPlaneT.x = genPlaneR.x = genPlaneQ.x = Float.NaN;
            unit = this;
            envMode = genModeS = genModeT = genModeR = genModeQ = -1;
            envColor.r = -1f;
            rgbCombine = alphaCombine = -1;
            rgbSource0 = rgbSource1 = rgbSource2 = -1;
            alphaSource0 = alphaSource1 = alphaSource2 = -1;
            rgbOperand0 = rgbOperand1 = rgbOperand2 = -1;
            alphaOperand0 = alphaOperand1 = alphaOperand2 = -1;
            rgbScale = alphaScale = Float.NaN;
            transform.setType(-1);
        }
    }

    /** The drawable with which this renderer is being used. */
    protected Drawable _drawable;

    /** The width and height of the drawable surface. */
    protected int _width, _height;

    /** The list of renderer observers. */
    protected WeakObserverList<Observer> _observers = WeakObserverList.newFastUnsafe();

    /** The number of alpha bit planes in the frame buffer. */
    protected int _alphaBits;

    /** The number of stencil bit planes in the frame buffer. */
    protected int _stencilBits;

    /** The maximum number of clip planes supported. */
    protected int _maxClipPlanes;

    /** The maximum number of lights supported. */
    protected int _maxLights;

    /** The maximum number of textures available in the fixed function pipeline. */
    protected int _maxTextureUnits;

    /** The maximum number of textures available in the programmable pipeline. */
    protected int _maxTextureImageUnits;

    /** The maximum number of vertex attributes available to vertex shaders. */
    protected int _maxVertexAttribs;

    /** Vendor flags for special casery. */
    protected boolean _nvidia, _ati, _intel;

    /** The number of texture changes in the current frame. */
    protected int _textureChangeCount;

    /** The number of batches rendered in the current frame. */
    protected int _batchCount;

    /** The number of primitives rendered in the current frame. */
    protected int _primitiveCount;

    /** References to the last states applied. */
    protected RenderState[] _states = RenderState.createDefaultSet();

    /** The clear color. */
    protected Color4f _clearColor = new Color4f(0f, 0f, 0f, 0f);

    /** The clear depth. */
    protected float _clearDepth = 1f;

    /** The clear stencil. */
    protected int _clearStencil;

    /** The active viewport. */
    protected Rectangle _viewport = new Rectangle();

    /** The projection parameters. */
    protected float _left = -1f, _right = +1f, _bottom = -1f, _top = +1f, _near = +1f, _far = -1f;

    /** The normal of the near/far clip planes.  */
    protected Vector3f _nearFarNormal = new Vector3f(Vector3f.UNIT_Z);

    /** Whether or not we're using orthographic projection. */
    protected boolean _ortho = true;

    /** The user clip plane records. */
    protected ClipPlaneRecord[] _clipPlanes;

    /** One greater than the index of the last clipping plane possibly enabled. */
    protected int _clipPlaneEnd;

    /** Whether or not scissor testing is enabled (where null means "unknown"). */
    protected Boolean _scissorTestEnabled = false;

    /** The scissor box. */
    protected Rectangle _scissor = new Rectangle();

    /** The current query for the samples-passed target, if any. */
    protected Query _samplesPassed;

    /** Whether or not alpha testing is enabled. */
    protected Boolean _alphaTestEnabled = false;

    /** The alpha test function. */
    protected int _alphaTestFunc = GL11.GL_ALWAYS;

    /** The reference alpha value for testing. */
    protected float _alphaTestRef;

    /** Whether or not blending is enabled. */
    protected Boolean _blendEnabled = false;

    /** The source blend factor. */
    protected int _srcBlendFactor = GL11.GL_ONE;

    /** The destination blend factor. */
    protected int _destBlendFactor = GL11.GL_ZERO;

    /** The currently bound array buffer object. */
    protected BufferObject _arrayBuffer;

    /** The currently bound element array buffer object. */
    protected BufferObject _elementArrayBuffer;

    /** The active client texture unit. */
    protected int _clientActiveUnit;

    /** The state of the vertex attribute arrays. */
    protected ClientArrayRecord[] _vertexAttribArrays;

    /** The index of the first vertex attribute array possibly enabled. */
    protected int _vertexAttribArrayStart;

    /** One greater than the index of the last vertex attribute array possibly enabled. */
    protected int _vertexAttribArrayEnd;

    /** The state of the texture coordinate arrays. */
    protected ClientArrayRecord[] _texCoordArrays;

    /** One greater than the index of the last texture coordinate array possibly enabled. */
    protected int _texCoordArrayEnd;

    /** The state of the color array. */
    protected ClientArrayRecord _colorArray = new ClientArrayRecord();

    /** The state of the normal array. */
    protected ClientArrayRecord _normalArray = new ClientArrayRecord();

    /** The state of the vertex array. */
    protected ClientArrayRecord _vertexArray = new ClientArrayRecord();

    /** The current render color. */
    protected Color4f _color = new Color4f(1f, 1f, 1f, 1f);

    /** The current color mask state. */
    protected Boolean _redMask = true, _greenMask = true, _blueMask = true, _alphaMask = true;

    /** Whether or not face culling is enabled. */
    protected Boolean _cullFaceEnabled = false;

    /** The cull face. */
    protected int _cullFace = GL11.GL_BACK;

    /** The front face. */
    protected int _frontFace = GL11.GL_CCW;

    /** Whether or not to normalize normals after transformation. */
    protected Boolean _normalize = false;

    /** Whether or not to rescale normals according to the modelview matrix scale. */
    protected Boolean _rescaleNormal = false;

    /** Whether or not depth testing is enabled. */
    protected Boolean _depthTestEnabled = false;

    /** The depth test function. */
    protected int _depthTestFunc = GL11.GL_LESS;

    /** Whether or not depth writing is currently enabled. */
    protected Boolean _depthMask = true;

    /** Whether or not fog is enabled. */
    protected Boolean _fogEnabled = false;

    /** Whether or not we would enable fog if not for the shader state. */
    protected boolean _wouldEnableFog;

    /** The current fog mode. */
    protected int _fogMode = GL11.GL_EXP;

    /** The current fog density. */
    protected float _fogDensity = 1f;

    /** The current fog start distance. */
    protected float _fogStart = 0f;

    /** The current fog end distance. */
    protected float _fogEnd = 1f;

    /** The current fog color. */
    protected Color4f _fogColor = new Color4f(0f, 0f, 0f, 0f);

    /** Whether or not lighting is currently enabled. */
    protected Boolean _lightingEnabled = false;

    /** The state of the lights. */
    protected LightRecord[] _lights;

    /** One greater than the index of the last light possibly enabled. */
    protected int _lightEnd;

    /** The current line width. */
    protected float _lineWidth = 1f;

    /** The current global ambient intensity. */
    protected Color4f _globalAmbient = new Color4f(0.2f, 0.2f, 0.2f, 1f);

    /** The current material's front ambient color. */
    protected Color4f _frontAmbient = new Color4f(0.2f, 0.2f, 0.2f, 1f);

    /** The current material's front diffuse color. */
    protected Color4f _frontDiffuse = new Color4f(0.8f, 0.8f, 0.8f, 1f);

    /** The current material's front specular color. */
    protected Color4f _frontSpecular = new Color4f(0f, 0f, 0f, 1f);

    /** The current material's front emissive color. */
    protected Color4f _frontEmission = new Color4f(0f, 0f, 0f, 1f);

    /** The current material's front shininess. */
    protected float _frontShininess = 0f;

    /** The current material's back ambient color. */
    protected Color4f _backAmbient = new Color4f(0.2f, 0.2f, 0.2f, 1f);

    /** The current material's back diffuse color. */
    protected Color4f _backDiffuse = new Color4f(0.8f, 0.8f, 0.8f, 1f);

    /** The current material's back specular color. */
    protected Color4f _backSpecular = new Color4f(0f, 0f, 0f, 1f);

    /** The current material's back emissive color. */
    protected Color4f _backEmission = new Color4f(0f, 0f, 0f, 1f);

    /** The current material's back shininess. */
    protected float _backShininess = 0f;

    /** Whether or not color material tracking is enabled. */
    protected Boolean _colorMaterialEnabled = false;

    /** The color material face. */
    protected int _colorMaterialFace = GL11.GL_FRONT_AND_BACK;

    /** The color material mode. */
    protected int _colorMaterialMode = GL11.GL_AMBIENT_AND_DIFFUSE;

    /** Whether or not two-sided lighting is enabled. */
    protected Boolean _twoSide = false;

    /** Whether or not specular colors are calculated using a local viewer model. */
    protected Boolean _localViewer = false;

    /** Whether or not specular highlights are applied as a separate pass. */
    protected Boolean _separateSpecular = false;

    /** Whether or not we are using flat shading. */
    protected Boolean _flatShading = false;

    /** The point size. */
    protected float _pointSize = 1f;

    /** The polygon mode for front-facing polygons. */
    protected int _frontPolygonMode = GL11.GL_FILL;

    /** The polygon mode for back-facing polygons. */
    protected int _backPolygonMode = GL11.GL_FILL;

    /** Whether or not offsets are enabled for filled polygons. */
    protected Boolean _polygonOffsetFillEnabled = false;

    /** Whether or not offsets are enabled for line polygons. */
    protected Boolean _polygonOffsetLineEnabled = false;

    /** Whether or not offsets are enabled for point polygons. */
    protected Boolean _polygonOffsetPointEnabled = false;

    /** The proportional polygon offset. */
    protected float _polygonOffsetFactor;

    /** The constant polygon offset. */
    protected float _polygonOffsetUnits;

    /** The currently bound shader program. */
    protected Program _program;

    /** Whether or not two-sided vertex program mode is enabled. */
    protected Boolean _vertexProgramTwoSide = false;

    /** Whether or not stencil testing is enabled. */
    protected Boolean _stencilTestEnabled = false;

    /** The current stencil test function. */
    protected int _stencilTestFunc = GL11.GL_ALWAYS;

    /** The current stencil test reference value. */
    protected int _stencilTestRef = 0;

    /** The current stencil test mask. */
    protected int _stencilTestMask = 0x7FFFFFFF;

    /** The current stencil test failure op. */
    protected int _stencilFailOp = GL11.GL_KEEP;

    /** The current stencil test depth failure op. */
    protected int _stencilDepthFailOp = GL11.GL_KEEP;

    /** The current stencil test pass op. */
    protected int _stencilPassOp = GL11.GL_KEEP;

    /** The current stencil write mask. */
    protected int _stencilWriteMask = 0x7FFFFFFF;

    /** The state of the texture units. */
    protected TextureUnitRecord[] _units;

    /** One greater than the index of the last texture unit possibly enabled. */
    protected int _unitEnd;

    /** The active texture unit. */
    protected int _activeUnit;

    /** The current matrix mode. */
    protected int _matrixMode = GL11.GL_MODELVIEW;

    /** The current modelview transform. */
    protected Transform3D _modelview = new Transform3D();

    /** Whether or not the modelview matrix is valid. */
    protected boolean _modelviewMatrixValid;

    /** The currently bound frame buffer. */
    protected Framebuffer _framebuffer;

    /** The currently bound render buffer. */
    protected Renderbuffer _renderbuffer;

    /** The active buffers for drawing and reading. */
    protected int _drawBuffer, _readBuffer;

    /** The number of active buffer objects. */
    protected int _bufferObjectCount;

    /** The total number of bytes in buffer objects. */
    protected int _bufferObjectBytes;

    /** The number of active display lists. */
    protected int _displayListCount;

    /** The number of active shader objects. */
    protected int _shaderObjectCount;

    /** The number of active textures. */
    protected int _textureCount;

    /** The total number of bytes in textures. */
    protected int _textureBytes;

    /** The list of buffer objects to be deleted. */
    protected int[] _finalizedBufferObjects;

    /** The total number of bytes in the buffer objects to be deleted. */
    protected int _finalizedBufferObjectBytes;

    /** The list of display lists to be deleted. */
    protected int[] _finalizedDisplayLists;

    /** The list of frame buffers to be deleted. */
    protected int[] _finalizedFramebuffers;

    /** The list of pbuffers to be destroyed. */
    protected Object[] _finalizedPbuffers;

    /** The list of queries to be deleted. */
    protected int[] _finalizedQueries;

    /** The list of render buffers to be deleted. */
    protected int[] _finalizedRenderbuffers;

    /** The list of shader objects to be deleted. */
    protected int[] _finalizedShaderObjects;

    /** The list of textures to be deleted. */
    protected int[] _finalizedTextures;

    /** The total number of bytes in the textures to be deleted. */
    protected int _finalizedTextureBytes;

    /** Temporary matrix. */
    protected Matrix4f _mat = new Matrix4f();

    /** A buffer for floating point values. */
    protected FloatBuffer _vbuf = BufferUtils.createFloatBuffer(16);

    /** A buffer for double values. */
    protected DoubleBuffer _dbuf = BufferUtils.createDoubleBuffer(16);

    /** An invalid buffer to force reapplication. */
    protected static final BufferObject INVALID_BUFFER = new BufferObject();

    /** An invalid program to force reapplication. */
    protected static final Program INVALID_PROGRAM = new Program();

    /** An invalid texture to force reapplication. */
    protected static final Texture INVALID_TEXTURE = new Texture() {
        public int getWidth () {
            return -1;
        }
        public int getHeight () {
            return -1;
        }
    };
}
