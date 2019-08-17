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

package com.threerings.tudey.tools;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.google.common.base.Predicate;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.math.Ray3D;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;
import com.threerings.util.MessageBundle;

import com.threerings.opengl.compositor.Compositable;
import com.threerings.opengl.util.Tickable;

import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.AreaEntry;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.PathEntry;
import com.threerings.tudey.data.TudeySceneModel.PlaceableEntry;
import com.threerings.tudey.data.TudeySceneModel.TileEntry;

import static com.threerings.tudey.Log.log;

/**
 * A tool to use in the scene editor.
 */
public abstract class EditorTool extends JPanel
    implements Tickable, Compositable, TudeySceneModel.Observer,
        MouseListener, MouseMotionListener, MouseWheelListener
{
    /**
     * Creates the tool.
     */
    public EditorTool (SceneEditor editor)
    {
        super(new VGroupLayout(GroupLayout.STRETCH, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        _editor = editor;
        _msgs = _editor.getMessageManager().getBundle("scene");
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

        Component canvas = _editor.getCanvas();
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addMouseWheelListener(this);
    }

    /**
     * Notes that the tool has been deactivated.
     */
    public void deactivate ()
    {
        Component canvas = _editor.getCanvas();
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

    // documentation inherited from interface Compositable
    public void composite ()
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

    /** The directional constraints. */
    protected enum DirectionalConstraint { HORIZONTAL, VERTICAL };

    /**
     * Determines which kinds of entries we want to affect.
     */
    protected static class Filter extends DeepObject
        implements Predicate<Entry>, Exportable
    {
        /** Whether or not we select tiles. */
        @Editable(hgroup="t")
        public boolean tiles = true;

        /** Whether or not we select placeables. */
        @Editable(hgroup="t")
        public boolean placeables = true;

        /** Whether or not we selected paths. */
        @Editable(hgroup="p")
        public boolean paths = true;

        /** Whether or not we select areas. */
        @Editable(hgroup="p")
        public boolean areas = true;

        // from Predicate
        public boolean apply (Entry entry)
        {
            if (entry instanceof TileEntry) {
                return tiles;
            } else if (entry instanceof PlaceableEntry) {
                return placeables;
            } else if (entry instanceof PathEntry) {
                return paths;
            } else if (entry instanceof AreaEntry) {
                return areas;
            } else {
                log.warning("Unknown entry type.", "class", entry.getClass());
                return true;
            }
        }
    }

    /** A reference to the creating editor. */
    protected SceneEditor _editor;

    /** The editor message bundle. */
    protected MessageBundle _msgs;

    /** The tool's button. */
    protected JToggleButton _button;

    /** A reference to the scene. */
    protected TudeySceneModel _scene;

    /** Used for picking. */
    protected Ray3D _pick = new Ray3D();

    /** The fine rotation increment. */
    protected static final float FINE_ROTATION_INCREMENT = FloatMath.toRadians(5f);
}
