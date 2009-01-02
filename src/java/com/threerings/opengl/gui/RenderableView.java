//
// $Id$

package com.threerings.opengl.gui;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import com.threerings.expr.DynamicScope;
import com.threerings.expr.Scoped;
import com.threerings.math.Transform3D;

import com.threerings.opengl.camera.Camera;
import com.threerings.opengl.camera.CameraHandler;
import com.threerings.opengl.camera.OrbitCameraHandler;
import com.threerings.opengl.compositor.Compositor;
import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.gui.util.Rectangle;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Renderable;
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
     * Adds a renderable to the view.
     */
    public void add (Renderable renderable)
    {
        _renderables.add(renderable);
    }

    /**
     * Removes a renderable from the view.
     */
    public void remove (Renderable renderable)
    {
        _renderables.remove(renderable);
    }

    /**
     * Removes all renderables from the view.
     */
    public void removeAll ()
    {
        _renderables.clear();
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        // tick the config models
        for (Model model : _configModels) {
            model.tick(elapsed);
        }

        // tick the other renderables
        for (int ii = 0, nn = _renderables.size(); ii < nn; ii++) {
            Renderable renderable = _renderables.get(ii);
            if (renderable instanceof Tickable) {
                ((Tickable)renderable).tick(elapsed);
            }
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _root = getWindow().getRoot();
        _root.addTickParticipant(this);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _root.removeTickParticipant(this);
        _root = null;
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        // save the compositor's original camera and swap in our group state
        Compositor compositor = _ctx.getCompositor();
        Camera ocamera = compositor.getCamera();
        RenderQueue.Group group = compositor.getGroup();
        _gstate.swap(group);

        // install our camera
        compositor.setCamera(_camera);

        // update the camera viewport
        Insets insets = getInsets();
        _camera.getViewport().set(
            getAbsoluteX() + insets.left, getAbsoluteY() + insets.bottom,
            _width - insets.getHorizontal(), _height - insets.getVertical());

        // update the camera handler
        _camhand.updatePerspective();
        _camhand.updatePosition();

        // update the view transform state
        _viewTransformState.getModelview().set(_viewTransform);
        _viewTransformState.setDirty(true);

        try {
            // push the modelview matrix
            renderer.setMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            renderer.setState(TransformState.IDENTITY);

            // enqueue the config models
            for (Model model : _configModels) {
                model.enqueue();
            }

            // enqueue the other renderables
            for (int ii = 0, nn = _renderables.size(); ii < nn; ii++) {
                _renderables.get(ii).enqueue();
            }

            // sort the queues in preparation for rendering
            group.sortQueues();

            // apply the camera state
            _camera.apply(renderer);

            // clear the depth buffer
            Rectangle oscissor = renderer.getScissor();
            if (oscissor != null) {
                _oscissor.set(oscissor);
            }
            renderer.setScissor(_camera.getViewport());
            renderer.setClearDepth(1f);
            renderer.setState(DepthState.TEST_WRITE);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            renderer.setScissor(oscissor == null ? null : _oscissor);

            // render the contents of the queues
            group.renderQueues(RenderQueue.NORMAL_TYPE);

        } finally {
            // clear out the render queues
            group.clearQueues();

            // restore the original camera and group state
            compositor.setCamera(ocamera);
            _gstate.swap(group);

            // restore the original viewport and projection
            Rectangle viewport = ocamera.getViewport();
            renderer.setViewport(viewport);
            renderer.setProjection(0f, viewport.width, 0f, viewport.height, -1f, +1f, true);

            // reapply the overlay states
            renderer.setStates(_root.getStates());

            // pop the modelview matrix
            renderer.setMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
        }
    }

    /**
     * Creates the camera handler for the view.
     */
    protected CameraHandler createCameraHandler ()
    {
        return new OrbitCameraHandler(_ctx, _camera, false);
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

    /** The models loaded from the configuration. */
    protected Model[] _configModels = new Model[0];

    /** The list of other renderables to include. */
    protected ArrayList<Renderable> _renderables = new ArrayList<Renderable>();

    /** A scoped reference to the camera's view transform. */
    @Scoped
    protected Transform3D _viewTransform = _camera.getViewTransform();

    /** A transform state containing the camera's view transform. */
    @Scoped
    protected TransformState _viewTransformState = new TransformState();

    /** Used to save the scissor region. */
    protected Rectangle _oscissor = new Rectangle();
}
