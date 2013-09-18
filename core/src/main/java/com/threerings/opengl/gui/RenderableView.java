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

package com.threerings.opengl.gui;

import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.PixelFormat;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.util.StringUtil;

import com.threerings.expr.DynamicScope;
import com.threerings.expr.Scoped;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.camera.Camera;
import com.threerings.opengl.camera.CameraHandler;
import com.threerings.opengl.camera.OrbitCameraHandler;
import com.threerings.opengl.compositor.Compositable;
import com.threerings.opengl.compositor.Compositor;
import com.threerings.opengl.compositor.Dependency;
import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.gui.util.Rectangle;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.Texture2D;
import com.threerings.opengl.renderer.TextureRenderer;
import com.threerings.opengl.renderer.state.ColorMaskState;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.GlUtil;
import com.threerings.opengl.util.Tickable;

/**
 * Displays an embedded 3D view.
 */
public class RenderableView extends Component
    implements Tickable
{
    /**
     * Creates a new renderable view.
     */
    public RenderableView (GlContext ctx)
    {
        super(ctx);
        _camhand = createCameraHandler();
    }

    /**
     * Returns a reference to the view's scope.
     */
    public DynamicScope getScope ()
    {
        return _scope;
    }

    /**
     * Returns a reference to the view camera.
     */
    public Camera getCamera ()
    {
        return _camera;
    }

    /**
     * Returns a reference to the camera handler.
     */
    public CameraHandler getCameraHandler ()
    {
        return _camhand;
    }

    /**
     * Sets whether this view is static and must be rendered manually.
     */
    public void setStatic (boolean stat)
    {
        if (_static == stat) {
            return;
        }
        if (_static = stat) {
            invalidate();
        } else {
            _image = null;
            _renderer = null;
        }
    }

    /**
     * Checks whether this view is configured as static.
     */
    public boolean isStatic ()
    {
        return _static;
    }

    /**
     * Sets the name of the view node.  If non-blank, the camera transform will assume the
     * transform of the first node encountered with this name in the model list (overriding
     * the transform applied by the camera handler).
     */
    public void setViewNode (String node)
    {
        _viewNode = node;
    }

    /**
     * Returns the name of the view node.
     */
    public String getViewNode ()
    {
        return _viewNode;
    }

    /**
     * Sets if we use the provided hints when computing our preferred size.
     */
    public void setUsePreferredSizeHints (boolean hints)
    {
        if (hints != _usePreferredSizeHints) {
            _usePreferredSizeHints = hints;
            invalidate();
        }
    }

    /**
     * Returns true if we use the hints to compute our preferred size.
     */
    public boolean usePreferredSizeHints ()
    {
        return _usePreferredSizeHints;
    }

    /**
     * Sets the array of config models.
     */
    public void setConfigModels (Model[] models)
    {
        _configModels = models;
    }

    /**
     * Returns a reference to the array of config models.
     */
    public Model[] getConfigModels ()
    {
        return _configModels;
    }

    /**
     * Adds a compositable to the view.
     */
    public void add (Compositable compositable)
    {
        _compositables.add(compositable);
    }

    /**
     * Removes a compositable from the view.
     */
    public void remove (Compositable compositable)
    {
        _compositables.remove(compositable);
    }

    /**
     * Removes all compositables from the view.
     */
    public void removeAll ()
    {
        _compositables.clear();
    }

    /**
     * Manually rerenders the (static) view.
     */
    public void render ()
    {
        if (!_static) {
            return;
        }
        Insets insets = getInsets();
        int width = _width - insets.getHorizontal(), height = _height - insets.getVertical();
        if (_image == null || _image.getWidth() != width || _image.getHeight() != height) {
            Renderer renderer = _ctx.getRenderer();
            Texture2D texture = (_image == null) ? null : _image.getTexture(renderer);
            if (texture == null) {
                texture = new Texture2D(renderer);
                texture.setMinFilter(GL11.GL_LINEAR);
                texture.setWrap(GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);
            }
            int twidth = GlUtil.nextPowerOfTwo(width), theight = GlUtil.nextPowerOfTwo(height);
            if (texture.getWidth() != twidth || texture.getHeight() != theight) {
                texture.setImage(GL11.GL_RGBA, twidth, theight, false, false);
            }
            _image = new Image(texture, width, height);
            _renderer = new TextureRenderer(
                _ctx, texture, null, width, height, new PixelFormat(1, 8, 0));
        }
        _renderer.startRender();
        try {
            renderView(_ctx.getRenderer());
        } finally {
            _renderer.commitRender();
        }
    }

    /**
     * Get access to the image onto which we've rendered, which is only valid
     * if we're static and a call has been made to render(). Otherwise null will be returned.
     */
    public Image getImage ()
    {
        return _image;
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        // no time elapses for static views
        if (_static) {
            elapsed = 0f;
        }

        // set the camera in the compositor in case anything wants its position
        Compositor compositor = _ctx.getCompositor();
        Camera ocamera = compositor.getCamera();
        compositor.setCamera(_camera);

        try {
            // tick the config models
            for (Model model : _configModels) {
                model.tick(elapsed);
            }

            // tick the other compositables
            for (int ii = 0, nn = _compositables.size(); ii < nn; ii++) {
                Compositable compositable = _compositables.get(ii);
                if (compositable instanceof Tickable) {
                    ((Tickable)compositable).tick(elapsed);
                }
            }
        } finally {
            // restore the camera
            compositor.setCamera(ocamera);
        }
    }

    @Override
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        Dimension dim = super.computePreferredSize(whint, hhint);
        if (_usePreferredSizeHints) {
            if (whint > 0) {
                dim.width = whint;
            }
            if (hhint > 0) {
                dim.height = hhint;
            }
        }
        return dim;
    }

    @Override
    protected void wasAdded ()
    {
        super.wasAdded();
        _root = getWindow().getRoot();
        _root.addTickParticipant(this);
    }

    @Override
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _root.removeTickParticipant(this);
        _root = null;
    }

    @Override
    protected void layout ()
    {
        render();
    }

    @Override
    protected void renderComponent (Renderer renderer)
    {
        // static views simply draw the prerendered image
        if (_static) {
            if (_image != null) {
                Insets insets = getInsets();
                _image.render(renderer, insets.left, insets.bottom,
                    _width - insets.getHorizontal(), _height - insets.getVertical(), _alpha);
            }
        } else {
            renderView(renderer);
        }
    }

    /**
     * Creates the camera handler for the view.
     */
    protected CameraHandler createCameraHandler ()
    {
        return new OrbitCameraHandler(_ctx, _camera, false);
    }

    /**
     * Renders the view.
     */
    protected void renderView (Renderer renderer)
    {
        // save the compositor's original camera and dependency map, swap in our group state
        Compositor compositor = _ctx.getCompositor();
        Camera ocamera = compositor.getCamera();
        Map<Dependency, Dependency> odeps = compositor.getDependencies();
        RenderQueue.Group group = compositor.getGroup();
        _gstate.swap(group);

        // install our camera, dependency map
        compositor.setCamera(_camera);
        compositor.setDependencies(_dependencies);

        // update the camera viewport
        Insets insets = getInsets();
        _camera.getViewport().set(
            _static ? 0 : (getAbsoluteX() + insets.left),
            _static ? 0 : (getAbsoluteY() + insets.bottom),
            _width - insets.getHorizontal(), _height - insets.getVertical());

        // update the camera handler/camera position
        _camhand.updatePerspective();
        Transform3D viewNodeTransform = getViewNodeTransform();
        if (viewNodeTransform == null) {
            _camhand.updatePosition();
        } else {
            _camera.getWorldTransform().set(viewNodeTransform);
            _camera.updateTransform();
        }

        // update the view transform state
        _viewTransformState.getModelview().set(_viewTransform);
        _viewTransformState.setDirty(true);

        try {
            // push the modelview matrix
            if (!_static) {
                renderer.setMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPushMatrix();
                GL11.glLoadIdentity();
            }

            // composite the config models
            for (Model model : _configModels) {
                model.composite();
            }

            // composite the other compositables
            for (int ii = 0, nn = _compositables.size(); ii < nn; ii++) {
                _compositables.get(ii).composite();
            }

            // enqueue and clear the enqueueables
            compositor.enqueueEnqueueables();

            // sort the queues in preparation for rendering
            group.sortQueues();

            // apply the camera state
            _camera.apply(renderer);

            // clear the buffers
            Rectangle oscissor = renderer.getScissor();
            if (oscissor != null) {
                _oscissor.set(oscissor);
            }
            renderer.setScissor(_camera.getViewport());
            renderer.setClearDepth(1f);
            renderer.setState(DepthState.TEST_WRITE);
            if (_static) {
                renderer.setState(ColorMaskState.ALL);
                renderer.setClearColor(new Color4f(0f, 0f, 0f, 0f));
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            } else {
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            }
            renderer.setScissor(oscissor == null ? null : _oscissor);

            // render the contents of the queues
            group.renderQueues(RenderQueue.NORMAL_TYPE);

        } finally {
            // clear out the dependencies, render queues
            compositor.clearDependencies();
            group.clearQueues();

            // restore the original camera, dependency map, group state
            compositor.setCamera(ocamera);
            compositor.setDependencies(odeps);
            _gstate.swap(group);

            // restore the original viewport and projection
            if (!_static) {
                Rectangle viewport = ocamera.getViewport();
                renderer.setViewport(viewport);
                renderer.setProjection(
                    0f, viewport.width, 0f, viewport.height, -1f, +1f, Vector3f.UNIT_Z, true);

                // reapply the overlay states
                renderer.setStates(_root.getStates());

                // pop the modelview matrix
                renderer.setMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
            }
        }
    }

    /**
     * Returns the transform corresponding to the view node, or <code>null</code> for none.
     */
    protected Transform3D getViewNodeTransform ()
    {
        if (StringUtil.isBlank(_viewNode)) {
            return null;
        }
        for (Model model : _configModels) {
            Transform3D xform = model.getPointWorldTransform(_viewNode);
            if (xform != null) {
                return xform;
            }
        }
        for (int ii = 0, nn = _compositables.size(); ii < nn; ii++) {
            Compositable compositable = _compositables.get(ii);
            if (compositable instanceof Model) {
                Transform3D xform = ((Model)compositable).getPointWorldTransform(_viewNode);
                if (xform != null) {
                    return xform;
                }
            }
        }
        return null;
    }

    /** The view scope. */
    protected DynamicScope _scope = new DynamicScope(this, "view");

    /** The UI root with which we've registered as a tick participant. */
    protected Root _root;

    /** The renderer camera. */
    protected Camera _camera = new Camera();

    /** The handler that controls the camera's parameters. */
    protected CameraHandler _camhand;

    /** Stores the state of the render queue. */
    protected RenderQueue.Group.State _gstate = new RenderQueue.Group.State();

    /** Stores the dependency set. */
    protected Map<Dependency, Dependency> _dependencies = Maps.newHashMap();

    /** Whether or not the view is static. */
    protected boolean _static;

    /** The name of the view node, if any. */
    protected String _viewNode;

    /** The models loaded from the configuration. */
    protected Model[] _configModels = new Model[0];

    /** The list of other compositables to include. */
    protected List<Compositable> _compositables = Lists.newArrayList();

    /** For static views, the rendered image. */
    protected Image _image;

    /** For static views, the texture renderer. */
    protected TextureRenderer _renderer;

    /** If we use hints while computing preferred size. */
    protected boolean _usePreferredSizeHints;

    /** A scoped reference to the camera's view transform. */
    @Scoped
    protected Transform3D _viewTransform = _camera.getViewTransform();

    /** A transform state containing the camera's view transform. */
    @Scoped
    protected TransformState _viewTransformState = new TransformState();

    /** Used to save the scissor region. */
    protected Rectangle _oscissor = new Rectangle();
}
