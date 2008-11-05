//
// $Id$

package com.threerings.tudey.tools;

import java.awt.event.MouseEvent;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Predicate;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector3f;

import com.threerings.opengl.mod.Model;
import com.threerings.opengl.scene.SceneElement;

import com.threerings.tudey.client.sprite.PathSprite;
import com.threerings.tudey.config.PathConfig;
import com.threerings.tudey.data.TudeySceneModel.PathEntry;
import com.threerings.tudey.data.TudeySceneModel.Vertex;

/**
 * The path definer tool.
 */
public class PathDefiner extends ConfigTool<PathConfig>
{
    /**
     * Creates the path definer tool.
     */
    public PathDefiner (SceneEditor editor)
    {
        super(editor, PathConfig.class, new PathReference());
    }

    @Override // documentation inherited
    public void deactivate ()
    {
        // release any vertex being moved
        super.deactivate();
        if (_entry != null) {
            release();
        }
    }

    @Override // documentation inherited
    public void mousePressed (MouseEvent event)
    {
        if (_editor.isControlDown()) {
            return;
        }
        int button = event.getButton();
        if (_entry != null) {
            if (button == MouseEvent.BUTTON1) { // continue placing
                insertVertex(_entry, (_idx == 0) ? 0 : _idx + 1);
            } else if (button == MouseEvent.BUTTON3) { // remove the vertex
                release();
            }
            return;
        }
        if (_editor.getMouseRay(_pick)) {
            Model model = (Model)_editor.getView().getScene().getIntersection(
                _pick, _isect, PATH_FILTER);
            if (model != null) {
                PathSprite sprite = (PathSprite)model.getUserObject();
                PathEntry entry = (PathEntry)sprite.getEntry();
                int idx = sprite.getVertexIndex(model);
                if (idx != -1) {
                    if (button == MouseEvent.BUTTON1) {
                        if (idx == 0) { // insert at start
                            insertVertex(entry, 0);
                        } else if (idx == entry.vertices.length - 1) { // insert at end
                            insertVertex(entry, entry.vertices.length);
                        } else { // start moving the vertex
                            _entry = entry;
                            _idx = idx;
                        }
                    } else if (button == MouseEvent.BUTTON3) {
                        removeVertices(entry, idx, 1);
                    }
                    return;
                }
                idx = sprite.getEdgeIndex(model);
                if (button == MouseEvent.BUTTON1) { // insert in between
                    insertVertex(entry, idx + 1);
                } else if (button == MouseEvent.BUTTON3) {
                    if (idx == 0) { // first edge
                        removeVertices(entry, 0, 1);
                    } else if (idx == entry.vertices.length - 2) { // last edge
                        removeVertices(entry, entry.vertices.length - 1, 1);
                    } else { // middle edge
                        removeVertices(entry, idx, 2);
                    }
                }
                return;
            }
        }
        ConfigReference<PathConfig> path = _eref.getReference();
        if (button == MouseEvent.BUTTON1 && path != null && getMousePlaneIntersection(_isect)) {
            // start a new path
            _entry = new PathEntry();
            _idx = 1;
            _entry.path = path;
            _entry.vertices = new Vertex[] { new Vertex(), new Vertex() };
            setMouseLocation(_entry.vertices[0]);
            _editor.addEntry(_entry);
        }
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        if (_entry == null || !getMousePlaneIntersection(_isect) || _editor.isControlDown()) {
            return;
        }
        _entry = (PathEntry)_entry.clone();
        setMouseLocation(_entry.vertices[_idx]);
        _editor.updateEntry(_entry);
    }

    /**
     * Sets the location of the specified vertex to the one indicated by the mouse cursor.
     */
    protected void setMouseLocation (Vertex vertex)
    {
        // snap to tile grid if shift not held down
        if (!_editor.isShiftDown()) {
            _isect.x = FloatMath.floor(_isect.x) + 0.5f;
            _isect.y = FloatMath.floor(_isect.y) + 0.5f;
        }
        vertex.set(_isect.x, _isect.y, _editor.getGrid().getZ());
    }

    /**
     * Releases the vertex being moved.
     */
    protected void release ()
    {
        removeVertices(_entry, _idx, 1);
        _entry = null;
    }

    /**
     * Inserts a new vertex at the specified location and starts moving it.
     */
    protected void insertVertex (PathEntry entry, int idx)
    {
        _entry = (PathEntry)entry.clone();
        _idx = idx;
        _entry.vertices = ArrayUtil.insert(_entry.vertices, new Vertex(), idx);
        _editor.updateEntry(_entry);
    }

    /**
     * Removes the indexed vertices from the supplied entry (removing the entry itself if it has no
     * more vertices).
     */
    protected void removeVertices (PathEntry entry, int idx, int count)
    {
        if (entry.vertices.length <= count) {
            _editor.removeEntry(entry.getKey());
        } else {
            PathEntry nentry = (PathEntry)entry.clone();
            nentry.vertices = ArrayUtil.splice(nentry.vertices, idx, count);
            _editor.updateEntry(nentry);
        }
    }

    /**
     * Allows us to edit the path reference.
     */
    protected static class PathReference extends EditableReference<PathConfig>
    {
        /** The path reference. */
        @Editable(nullable=true)
        public ConfigReference<PathConfig> path;

        @Override // documentation inherited
        public ConfigReference<PathConfig> getReference ()
        {
            return path;
        }

        @Override // documentation inherited
        public void setReference (ConfigReference<PathConfig> ref)
        {
            path = ref;
        }
    }

    /** The entry containing the vertex we're moving, if any. */
    protected PathEntry _entry;

    /** The index of the vertex we're moving. */
    protected int _idx;

    /** Holds the result of an intersection test. */
    protected Vector3f _isect = new Vector3f();

    /** A filter that only passes path vertex or edge models. */
    protected static final Predicate<SceneElement> PATH_FILTER = new Predicate<SceneElement>() {
        public boolean isMatch (SceneElement element) {
            return element.getUserObject() instanceof PathSprite;
        }
    };
}
