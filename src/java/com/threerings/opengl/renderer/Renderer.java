//
// $Id$

package com.threerings.opengl.renderer;

import java.awt.Font;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import java.util.ArrayList;
import java.util.Comparator;

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
import org.lwjgl.opengl.EXTTextureLODBias;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;

import com.samskivert.util.IntListUtil;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.QuickSort;

import com.threerings.math.FloatMath;
import com.threerings.math.Plane;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform;
import com.threerings.math.Vector3f;
import com.threerings.math.Vector4f;
import com.threerings.media.timer.MediaTimer;
import com.threerings.util.TimerUtil;

import com.threerings.opengl.renderer.state.ColorMaskState;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.gui.Root;
import com.threerings.opengl.gui.text.CharacterTextFactory;
import com.threerings.opengl.gui.text.Text;
import com.threerings.opengl.gui.util.Rectangle;

/**
 * Provides access to state associated with the renderer.  Any state changes made should be done
 * through this object so that its internal state is synchronized with the OpenGL state.
 */
public class Renderer
{
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

        // find out how many user clip planes the driver supports
        IntBuffer buf = BufferUtils.createIntBuffer(16);
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

        // to make things easier for texture loading, we just keep this at one (default is four)
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        // create the default camera
        setCamera(new Camera(width, height));

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

        // get the text factory for stats rendering
        _textFactory = CharacterTextFactory.getInstance(
            new Font("Dialog", Font.PLAIN, 12), true);
        _stats = _textFactory.createText("", Color4f.WHITE, 0, 0, Color4f.BLACK, true);

        // and create the timer
        _timer = TimerUtil.createTimer();
    }

    /**
     * Returns a reference to the drawable target of this renderer.
     */
    public Drawable getDrawable ()
    {
        return _drawable;
    }

    /**
     * Sets the camera state.
     */
    public void setCamera (Camera camera)
    {
        if (_camera == camera) {
            return;
        }
        if (_camera != null) {
            _camera.setRenderer(null);
        }
        if ((_camera = camera) != null) {
            _camera.setRenderer(this);
        }
    }

    /**
     * Returns a reference to the camera object.
     */
    public Camera getCamera ()
    {
        return _camera;
    }

    /**
     * Sets whether or not to show rendering statistics (fps, batch counts).
     */
    public void setShowStats (boolean showStats)
    {
        _showStats = showStats;
    }

    /**
     * Checks whether or not stats are being shown.
     */
    public boolean getShowStats ()
    {
        return _showStats;
    }

    /**
     * Enqueues a batch for rendering in the opaque queue.
     */
    public void enqueueOpaque (Batch batch)
    {
        _opaque.add(batch);
    }

    /**
     * Enqueues a batch for rendering in the transparent queue.
     */
    public void enqueueTransparent (Batch batch)
    {
        _transparent.add(batch);
    }

    /**
     * Enqueues a batch for rendering in the ortho queue.
     */
    public void enqueueOrtho (Batch batch)
    {
        _ortho.add(batch);
    }

    /**
     * Clears the frame.
     */
    public void clearFrame ()
    {
        setState(ColorMaskState.ALL);
        setState(DepthState.TEST_WRITE);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Renders a single frame.
     */
    public void renderFrame ()
    {
        // update the stats
        long interval = _timer.getElapsedMillis();
        _frameCount++;
        if (interval >= REPORT_INTERVAL) {
            if (_showStats) {
                int fps = (int)((_frameCount * 1000) / interval);
                _stats = _textFactory.createText(
                    fps + " fps (" +
                    "batches: " + _opaque.size() + " opaque, " + _transparent.size() + " transparent; " +
                    "primitives: " + _primitiveCount + "; textures: " + _textureCount + ")",
                    Color4f.WHITE, 0, 0, Color4f.BLACK, true);
            }
            _timer.reset();
            _frameCount = 0;
        }
        _textureCount = 0;
        _primitiveCount = 0;

        // do the actual rendering
        renderQueues();

        // delete any finalized objects
        deleteFinalizedObjects();
    }

    /**
     * Renders the contents of the queues.
     */
    public void renderQueues ()
    {
        // sort the opaque queue by state and render
        QuickSort.sort(_opaque, BY_KEY);
        render(_opaque);

        // sort the transparent queue by depth and render
        QuickSort.sort(_transparent, BACK_TO_FRONT);
        render(_transparent);

        // sort the ortho queue by layer, load the ortho matrix, and render
        QuickSort.sort(_ortho, BY_LAYER);
        setMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        Rectangle viewport = _camera.getViewport();
        GL11.glOrtho(0f, viewport.width, 0f, viewport.height, -1f, +1f);
        render(_ortho);
        if (_showStats) {
            setStates(Root.STATES);
            _stats.render(this, 16, 16, 1f);
        }
        setMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();

        // clear the queues
        _opaque.clear();
        _transparent.clear();
        _ortho.clear();
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
        float left, float right, float bottom, float top, float near, float far, boolean ortho)
    {
        if (_left != left || _right != _right || _bottom != bottom ||
            _top != top || _near != near || _far != far || _orthoProj != ortho) {
            setMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            if (_orthoProj = ortho) {
                GL11.glOrtho(
                    _left = left, _right = right, _bottom = bottom,
                    _top = top, _near = near, _far = far);
            } else {
                GL11.glFrustum(
                    _left = left, _right = right, _bottom = bottom,
                    _top = top, _near = near, _far = far);
            }
        }
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
                setState(TransformState.IDENTITY);
                prec.set(plane).get(_dbuf).rewind();
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

        boolean fogEnabled = (fogMode != -1);
        if (_fogEnabled != Boolean.valueOf(fogEnabled)) {
            setCapability(GL11.GL_FOG, _fogEnabled = fogEnabled);
        }
        if (!fogEnabled) {
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

        boolean fogEnabled = (fogMode != -1);
        if (_fogEnabled != Boolean.valueOf(fogEnabled)) {
            setCapability(GL11.GL_FOG, _fogEnabled = fogEnabled);
        }
        if (!fogEnabled) {
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
                setState(TransformState.IDENTITY);
                lrec.position.set(light.position).get(_vbuf).rewind();
                GL11.glLight(lname, GL11.GL_POSITION, _vbuf);
            }
            if (light.position.w == 0f) {
                continue; // light is directional; the rest does not apply
            }
            if (light.spotCutoff != 180f && !lrec.spotDirection.equals(light.spotDirection)) {
                setState(TransformState.IDENTITY);
                lrec.spotDirection.set(light.spotDirection).get(_vbuf).rewind();
                GL11.glLight(lname, GL11.GL_SPOT_DIRECTION, _vbuf);
            }
            if (lrec.spotExponent != light.spotExponent) {
                GL11.glLightf(lname, GL11.GL_SPOT_EXPONENT,
                    lrec.spotExponent = light.spotExponent);
            }
            if (lrec.spotCutoff != light.spotCutoff) {
                GL11.glLightf(lname, GL11.GL_SPOT_CUTOFF,
                    lrec.spotCutoff = light.spotCutoff);
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
     * Sets the GLSL shader state.
     */
    public void setShaderState (Program program)
    {
        if (_program != program) {
            int id = (program == null) ? 0 : program.getId();
            ARBShaderObjects.glUseProgramObjectARB(id);
            _program = program;
            _states[RenderState.SHADER_STATE] = null;
        }
    }

    /**
     * Invalidates the shader state, forcing it to be reapplied.
     */
    public void invalidateShaderState ()
    {
        if (GLContext.getCapabilities().GL_ARB_shader_objects) {
            _program = INVALID_PROGRAM;
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
        for (int ii = 0, nn = Math.max(_unitEnd, numUnits); ii < nn; ii++) {
            TextureUnitRecord urec = _units[ii];
            TextureUnit unit = (ii < numUnits) ? units[ii] : null;
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
                            (urec.texture = unit.texture).getId());
                        _textureCount++;
                    }
                    break;
                case GL11.GL_TEXTURE_2D:
                    if (urec.texture2D != unit.texture) {
                        setActiveUnit(ii);
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D,
                            (urec.texture = unit.texture).getId());
                        _textureCount++;
                    }
                    break;
                case ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB:
                    if (urec.textureRectangle != unit.texture) {
                        setActiveUnit(ii);
                        GL11.glBindTexture(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB,
                            (urec.texture = unit.texture).getId());
                        _textureCount++;
                    }
                    break;
                case GL12.GL_TEXTURE_3D:
                    if (urec.texture3D != unit.texture) {
                        setActiveUnit(ii);
                        GL11.glBindTexture(GL12.GL_TEXTURE_3D,
                            (urec.texture = unit.texture).getId());
                        _textureCount++;
                    }
                    break;
                case ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB:
                    if (urec.textureCubeMap != unit.texture) {
                        setActiveUnit(ii);
                        GL11.glBindTexture(ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB,
                            (urec.texture = unit.texture).getId());
                        _textureCount++;
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
                        setState(TransformState.IDENTITY);
                        urec.genEyePlaneS.set(unit.genPlaneS).get(_vbuf).rewind();
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
                        setState(TransformState.IDENTITY);
                        urec.genEyePlaneT.set(unit.genPlaneT).get(_vbuf).rewind();
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
                        setState(TransformState.IDENTITY);
                        urec.genEyePlaneR.set(unit.genPlaneR).get(_vbuf).rewind();
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
                        setState(TransformState.IDENTITY);
                        urec.genEyePlaneQ.set(unit.genPlaneQ).get(_vbuf).rewind();
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
    public void setTransformState (Transform modelview)
    {
        if (!_modelview.equals(modelview)) {
            setMatrixMode(GL11.GL_MODELVIEW);
            loadTransformMatrix(_modelview.set(modelview));
            _states[RenderState.TRANSFORM_STATE] = null;
        }
    }

    /**
     * Invalidates the transform state, forcing it to be reapplied.
     */
    public void invalidateTransformState ()
    {
        _modelview.setType(-1);
        _states[RenderState.TRANSFORM_STATE] = null;
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
    protected void render (ArrayList<Batch> batches)
    {
        // for each batch, set the states and call its draw command
        for (int ii = 0, nn = batches.size(); ii < nn; ii++) {
            Batch batch = batches.get(ii);
            if (batch.draw(this) || _colorArray.enabled != Boolean.FALSE) {
                // invalidate the color state if we used a color array or a display list
                // with color info
                invalidateColorState();
            }
            if (_showStats) {
                _primitiveCount += batch.getPrimitiveCount();
            }
        }
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
     * Loads the specified transform into the current matrix slot.
     */
    protected void loadTransformMatrix (Transform transform)
    {
        // supposedly, calling the "typed" matrix functions (glTranslatef, glRotatef, etc.)
        // allows driver optimizations that calling glLoadMatrix doesn't:
        // http://www.opengl.org/resources/code/samples/s2001/perfogl.pdf
        // (but that's kind of an old document; it's not clear that it's worth the extra native
        // calls)
        int type = transform.getType();
        if (type >= Transform.AFFINE) {
            transform.getMatrix().get(_vbuf).rewind();
            GL11.glLoadMatrix(_vbuf);
            return;
        }
        GL11.glLoadIdentity();
        if (type == Transform.IDENTITY) {
            return;
        }
        Vector3f translation = transform.getTranslation();
        if (!translation.equals(Vector3f.ZERO)) {
            GL11.glTranslatef(translation.x, translation.y, translation.z);
        }
        Quaternion rotation = transform.getRotation();
        if (!rotation.equals(Quaternion.IDENTITY)) {
            float angle = 2f * FloatMath.acos(rotation.w);
            float rsina = 1f / FloatMath.sqrt(1f - rotation.w*rotation.w);
            GL11.glRotatef(FloatMath.toDegrees(angle),
                rotation.x * rsina, rotation.y * rsina, rotation.z * rsina);
        }
        if (type == Transform.UNIFORM) {
            float scale = transform.getScale();
            if (scale != 1f) {
                GL11.glScalef(scale, scale, scale);
            }
        }
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
                    GL11.glBindTexture(GL11.GL_TEXTURE_1D, (unit.texture = texture).getId());
                    unit.unit = unit; // note that the unit is set
                }
                break;

            case GL11.GL_TEXTURE_2D:
                if (unit.texture2D != texture) {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, (unit.texture = texture).getId());
                    unit.unit = unit;
                }
                break;

            case ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB:
                if (unit.textureRectangle != texture) {
                    GL11.glBindTexture(ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB,
                        (unit.texture = texture).getId());
                    unit.unit = unit;
                }
                break;

            case GL12.GL_TEXTURE_3D:
                if (unit.texture3D != texture) {
                    GL11.glBindTexture(GL12.GL_TEXTURE_3D, (unit.texture = texture).getId());
                    unit.unit = unit;
                }
                break;

            case ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB:
                if (unit.textureCubeMap != texture) {
                    GL11.glBindTexture(ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB,
                        (unit.texture = texture).getId());
                    unit.unit = unit;
                }
                break;
        }
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
     * Called when a buffer object has been finalized.
     */
    protected synchronized void bufferObjectFinalized (int id)
    {
        _finalizedBufferObjects = IntListUtil.add(_finalizedBufferObjects, id);
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
    protected synchronized void textureFinalized (int id)
    {
        _finalizedTextures = IntListUtil.add(_finalizedTextures, id);
    }

    /**
     * Deletes all finalized objects.
     */
    protected synchronized void deleteFinalizedObjects ()
    {
        if (_finalizedBufferObjects != null) {
            IntBuffer idbuf = BufferUtils.createIntBuffer(_finalizedBufferObjects.length);
            idbuf.put(_finalizedBufferObjects).rewind();
            ARBBufferObject.glDeleteBuffersARB(idbuf);
            _finalizedBufferObjects = null;
        }
        if (_finalizedDisplayLists != null) {
            for (int id : _finalizedDisplayLists) {
                GL11.glDeleteLists(id, 1);
            }
            _finalizedDisplayLists = null;
        }
        if (_finalizedFramebuffers != null) {
            IntBuffer idbuf = BufferUtils.createIntBuffer(_finalizedFramebuffers.length);
            idbuf.put(_finalizedFramebuffers).rewind();
            EXTFramebufferObject.glDeleteFramebuffersEXT(idbuf);
            _finalizedFramebuffers = null;
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
                ARBShaderObjects.glDeleteObjectARB(id);
            }
            _finalizedShaderObjects = null;
        }
        if (_finalizedTextures != null) {
            IntBuffer idbuf = BufferUtils.createIntBuffer(_finalizedTextures.length);
            idbuf.put(_finalizedTextures).rewind();
            GL11.glDeleteTextures(idbuf);
            _finalizedTextures = null;
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
     * Compares two packed state keys.
     */
    protected static int compareKeys (int[] k1, int[] k2)
    {
        int l1 = (k1 == null) ? 0 : k1.length;
        int l2 = (k2 == null) ? 0 : k2.length;
        int v1, v2, comp;
        for (int ii = 0, nn = Math.max(l1, l2); ii < nn; ii++) {
            v1 = (ii < l1) ? k1[ii] : 0;
            v2 = (ii < l2) ? k2[ii] : 0;
            if ((comp = v1 - v2) != 0) {
                return comp;
            }
        }
        return 0;
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

    /** The camera object. */
    protected Camera _camera;

    /** The opaque render queue. */
    protected ArrayList<Batch> _opaque = new ArrayList<Batch>();

    /** The transparent render queue. */
    protected ArrayList<Batch> _transparent = new ArrayList<Batch>();

    /** The ortho render queue. */
    protected ArrayList<Batch> _ortho = new ArrayList<Batch>();

    /** Used to create text objects for stats display. */
    protected CharacterTextFactory _textFactory;

    /** Whether or not to show rendering statistics. */
    protected boolean _showStats;

    /** A timer for the stats display. */
    protected MediaTimer _timer;

    /** The stats display text. */
    protected Text _stats;

    /** The time of the last stats report. */
    protected long _lastReport;

    /** The number of frames rendered since the last report. */
    protected int _frameCount;

    /** The number of textures used in the current frame. */
    protected int _textureCount;

    /** The number of primitives rendered in the current frame. */
    protected int _primitiveCount;

    /** References to the last states applied. */
    protected RenderState[] _states = RenderState.DEFAULTS.clone();

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

    /** Whether or not we're using orthographic projection. */
    protected boolean _orthoProj = true;

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

    /** Whether or not depth testing is enabled. */
    protected Boolean _depthTestEnabled = false;

    /** The depth test function. */
    protected int _depthTestFunc = GL11.GL_LESS;

    /** Whether or not depth writing is currently enabled. */
    protected Boolean _depthMask = true;

    /** Whether or not fog is enabled. */
    protected Boolean _fogEnabled = false;

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

    /** The currently bound shader program. */
    protected Program _program;

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
    protected Transform _modelview = new Transform();

    /** The currently bound frame buffer. */
    protected Framebuffer _framebuffer;

    /** The currently bound render buffer. */
    protected Renderbuffer _renderbuffer;

    /** The list of buffer objects to be deleted. */
    protected int[] _finalizedBufferObjects;

    /** The list of display lists to be deleted. */
    protected int[] _finalizedDisplayLists;

    /** The list of frame buffers to be deleted. */
    protected int[] _finalizedFramebuffers;

    /** The list of queries to be deleted. */
    protected int[] _finalizedQueries;

    /** The list of render buffers to be deleted. */
    protected int[] _finalizedRenderbuffers;

    /** The list of shader objects to be deleted. */
    protected int[] _finalizedShaderObjects;

    /** The list of textures to be deleted. */
    protected int[] _finalizedTextures;

    /** A buffer for floating point values. */
    protected FloatBuffer _vbuf = BufferUtils.createFloatBuffer(16);

    /** A buffer for double values. */
    protected DoubleBuffer _dbuf = BufferUtils.createDoubleBuffer(16);

    /** Sorts batches by state. */
    protected static final Comparator<Batch> BY_KEY = new Comparator<Batch>() {
        public int compare (Batch b1, Batch b2) {
            // if keys are the same, sort front-to-back
            int comp = compareKeys(b1.key, b2.key);
            return (comp == 0) ? Float.compare(b2.depth, b1.depth) : comp;
        }
    };

    /** Sorts batches by depth, back-to-front. */
    protected static final Comparator<Batch> BACK_TO_FRONT = new Comparator<Batch>() {
        public int compare (Batch b1, Batch b2) {
            return Float.compare(b1.depth, b2.depth);
        }
    };

    /** Sorts batches by layer, back-to-front. */
    protected static final Comparator<Batch> BY_LAYER = new Comparator<Batch>() {
        public int compare (Batch b1, Batch b2) {
            return (b1.layer == b2.layer) ? 0 : (b1.layer < b2.layer ? -1 : +1);
        }
    };

    /** An invalid buffer to force reapplication. */
    protected static final BufferObject INVALID_BUFFER = new BufferObject();

    /** An invalid program to force reapplication. */
    protected static final Program INVALID_PROGRAM = new Program();

    /** An invalid texture to force reapplication. */
    protected static final Texture INVALID_TEXTURE = new Texture() {};

    /** The interval at which we update the stats. */
    protected static final long REPORT_INTERVAL = 1000L;
}
