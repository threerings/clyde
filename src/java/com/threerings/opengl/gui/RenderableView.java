//
// $Id$

package com.threerings.opengl.gui;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import com.threerings.opengl.camera.Camera;
import com.threerings.opengl.compositor.Compositor;
import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.state.DepthState;
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
        _group = new RenderQueue.Group(ctx);
    }

    /**
     * Returns a reference to the view camera.
     */
    public Camera getCamera ()
    {
        return _camera;
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
        // save the compositor's original camera and queue group
        Compositor compositor = _ctx.getCompositor();
        Camera ocamera = compositor.getCamera();
        RenderQueue.Group ogroup = compositor.getGroup();

        // install our camera and group
        compositor.setCamera(_camera);
        compositor.setGroup(_group);

        // update the camera viewport
        Insets insets = getInsets();
        _camera.getViewport().set(
            getAbsoluteX() + insets.left, getAbsoluteY() + insets.bottom,
            _width - insets.getHorizontal(), _height - insets.getVertical());
        _camera.updateTransform();

        try {
            // push the modelview matrix
            renderer.setMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();

            // enqueue the config models
            for (Model model : _configModels) {
                model.enqueue();
            }

            // enqueue the other renderables
            for (int ii = 0, nn = _renderables.size(); ii < nn; ii++) {
                _renderables.get(ii).enqueue();
            }

            // sort the queues in preparation for rendering
            _group.sortQueues();

            // apply the camera state
            _camera.apply(renderer);

            // clear the depth buffer
            renderer.setClearDepth(1f);
            renderer.setState(DepthState.TEST_WRITE);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

            // render the contents of the queues
            _group.renderQueues(RenderQueue.NORMAL_TYPE);

        } finally {
            // restore the original camera and queue group
            compositor.setCamera(ocamera);
            compositor.setGroup(ogroup);

            // restore the original camera state
            ocamera.apply(renderer);

            // clear out the render queues
            _group.clearQueues();

            // reapply the overlay states
            renderer.setStates(_root.getStates());

            // pop the modelview matrix
            renderer.setMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
        }
    }

    /** The UI root with which we've registered as a tick participant. */
    protected Root _root;

    /** The renderer camera. */
    protected Camera _camera = new Camera();

    /** The base render queue group. */
    protected RenderQueue.Group _group;

    /** The models loaded from the configuration. */
    protected Model[] _configModels = new Model[0];

    /** The list of other renderables to include. */
    protected ArrayList<Renderable> _renderables = new ArrayList<Renderable>();
}
