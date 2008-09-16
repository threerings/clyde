//
// $Id$

package com.threerings.tudey.tools;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JPanel;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.opengl.GlCanvas;
import com.threerings.opengl.util.Renderable;
import com.threerings.opengl.util.Tickable;

import com.threerings.tudey.data.TudeySceneModel;

/**
 * A tool to use in the scene editor.
 */
public abstract class EditorTool extends JPanel
    implements Tickable, Renderable, MouseListener, MouseMotionListener, MouseWheelListener
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
        // nothing by default
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

    /** A reference to the creating editor. */
    protected SceneEditor _editor;
}
