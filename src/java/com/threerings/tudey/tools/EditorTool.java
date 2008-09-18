//
// $Id$

package com.threerings.tudey.tools;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.math.Ray;
import com.threerings.math.Vector3f;

import com.threerings.opengl.GlCanvas;
import com.threerings.opengl.util.Renderable;
import com.threerings.opengl.util.Tickable;

import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;

/**
 * A tool to use in the scene editor.
 */
public abstract class EditorTool extends JPanel
    implements Tickable, Renderable, TudeySceneModel.Observer,
        MouseListener, MouseMotionListener, MouseWheelListener
{
    /**
     * Creates the tool.
     */
    public EditorTool (SceneEditor editor)
    {
        super(new VGroupLayout(GroupLayout.STRETCH, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        _editor = editor;
    }

    /**
     * Configures the tool with a reference to its button.
     */
    public void setButton (JToggleButton button)
    {
        _button = button;
    }

    /**
     * Initializes the tool after the renderer has been initialized.
     */
    public void init ()
    {
        // nothing by default
    }

    /**
     * Notes that the tool has been activated.
     */
    public void activate ()
    {
        _button.setSelected(true);

        GlCanvas canvas = _editor.getCanvas();
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addMouseWheelListener(this);
    }

    /**
     * Notes that the tool has been deactivated.
     */
    public void deactivate ()
    {
        GlCanvas canvas = _editor.getCanvas();
        canvas.removeMouseListener(this);
        canvas.removeMouseMotionListener(this);
        canvas.removeMouseWheelListener(this);
    }

    /**
     * Notes that the scene object has changed.
     */
    public void sceneChanged (TudeySceneModel scene)
    {
        if (_scene != null) {
            _scene.removeObserver(this);
        }
        (_scene = scene).addObserver(this);
    }

    /**
     * Determines whether this tool (currently) allows the user to move the camera using the
     * mouse (without holding down the control key).
     */
    public boolean allowsMouseCamera ()
    {
        return false;
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        // nothing by default
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        // nothing by default
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryAdded (Entry entry)
    {
        // nothing by default
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryUpdated (Entry oentry, Entry nentry)
    {
        // nothing by default
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryRemoved (Entry oentry)
    {
        // nothing by default
    }

    // documentation inherited from interface MouseListener
    public void mouseClicked (MouseEvent event)
    {
        // nothing by default
    }

    // documentation inherited from interface MouseListener
    public void mousePressed (MouseEvent event)
    {
        // nothing by default
    }

    // documentation inherited from interface MouseListener
    public void mouseReleased (MouseEvent event)
    {
        // nothing by default
    }

    // documentation inherited from interface MouseListener
    public void mouseEntered (MouseEvent event)
    {
        // nothing by default
    }

    // documentation inherited from interface MouseListener
    public void mouseExited (MouseEvent event)
    {
        // nothing by default
    }

    // documentation inherited from interface MouseMotionListener
    public void mouseDragged (MouseEvent event)
    {
        // nothing by default
    }

    // documentation inherited from interface MouseMotionListener
    public void mouseMoved (MouseEvent event)
    {
        // nothing by default
    }

    // documentation inherited from interface MouseWheelListener
    public void mouseWheelMoved (MouseWheelEvent event)
    {
        // nothing by default
    }

    /**
     * Finds the point at which the mouse ray intersects the grid plane.
     *
     * @return true if the mouse is on the canvas and the mouse ray intersects the grid plane
     * (in which case the result object will be populated), false if not.
     */
    protected boolean getMousePlaneIntersection (Vector3f result)
    {
        return _editor.getMouseRay(_pick) &&
            _editor.getGrid().getPlane().getIntersection(_pick, result);
    }

    /** A reference to the creating editor. */
    protected SceneEditor _editor;

    /** The tool's button. */
    protected JToggleButton _button;

    /** A reference to the scene. */
    protected TudeySceneModel _scene;

    /** Used for picking. */
    protected Ray _pick = new Ray();
}
