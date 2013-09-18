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

import java.awt.event.MouseEvent;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import com.google.common.base.CaseFormat;
import com.google.common.base.Predicate;

import com.samskivert.swing.GroupLayout;
import com.samskivert.util.ArrayUtil;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector3f;

import com.threerings.opengl.model.Model;
import com.threerings.opengl.scene.SceneElement;

import com.threerings.tudey.client.sprite.AreaSprite;
import com.threerings.tudey.config.AreaConfig;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.AreaEntry;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.Vertex;

/**
 * The area definer tool.
 */
public class AreaDefiner extends ConfigTool<AreaConfig>
{
    /**
     * Snap styles.
     */
    enum SnapStyle
    {
        CENTER {
            @Override public void applySnap (Vector3f isect) {
                EDGE.applySnap(isect);
                isect.x += 0.5f;
                isect.y += 0.5f;
            }
        },
        EDGE {
            @Override public void applySnap (Vector3f isect) {
                isect.x = FloatMath.floor(isect.x);
                isect.y = FloatMath.floor(isect.y);
            }
        },
        NONE;

        /**
         * Snap just the x/y values of the provided vector.
         */
        public void applySnap (Vector3f isect)
        {
            // the NONE implementation: do nothing!
        }

        @Override
        public String toString ()
        {
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
        }
    }

    /**
     * Creates the area definer tool.
     */
    public AreaDefiner (SceneEditor editor)
    {
        super(editor, AreaConfig.class, new AreaReference());

        _snapStyle = new JComboBox(SnapStyle.values());
        add(GroupLayout.makeButtonBox(new JLabel("Snap to:"), _snapStyle), GroupLayout.FIXED);
    }

    @Override
    public void deactivate ()
    {
        // release any vertex being moved
        super.deactivate();
        if (_entry != null) {
            release();
        }
    }

    @Override
    public void sceneChanged (TudeySceneModel scene)
    {
        super.sceneChanged(scene);
        _entry = null;
    }

    @Override
    public void mousePressed (MouseEvent event)
    {
        if (_editor.isSpecialDown()) {
            return;
        }
        int button = event.getButton();
        if (_entry != null) {
            if (button == MouseEvent.BUTTON1) { // continue placing
                insertVertex(_entry, _idx + 1);
            } else if (button == MouseEvent.BUTTON3) { // remove the vertex
                release();
            }
            return;
        }
        if (_editor.getMouseRay(_pick)) {
            SceneElement element = _editor.getView().getScene().getIntersection(
                _pick, _isect, AREA_FILTER);
            if (element instanceof Model) {
                Model model = (Model)element;
                AreaSprite sprite = (AreaSprite)model.getUserObject();
                AreaEntry entry = (AreaEntry)sprite.getEntry();
                int idx = sprite.getVertexIndex(model);
                if (idx != -1) {
                    if (button == MouseEvent.BUTTON1) { // start moving the vertex
                        _entry = entry;
                        _idx = idx;
                    } else if (button == MouseEvent.BUTTON3) {
                        removeVertices(entry, idx, 1);
                    }
                    return;
                }
                idx = sprite.getEdgeIndex(model);

                if (button == MouseEvent.BUTTON1) { // insert in between
                    insertVertex(entry, idx + 1);
                } else if (button == MouseEvent.BUTTON3) {
                    if (idx == entry.vertices.length - 1) { // last edge
                        removeVertices(entry, idx, 1);
                        removeVertices(entry, 0, 1);
                    } else { // middle edge
                        removeVertices(entry, idx, 2);
                    }
                }
                return;

            } else if (element != null && button == MouseEvent.BUTTON3) {
                // delete the entire area
                AreaSprite sprite = (AreaSprite)element.getUserObject();
                _editor.removeEntries(sprite.getEntry().getKey());
            }
        }
        ConfigReference<AreaConfig> area = _eref.getReference();
        if (button == MouseEvent.BUTTON1 && area != null && getMousePlaneIntersection(_isect)) {
            // start a new area
            _entry = new AreaEntry();
            _idx = 1;
            _entry.area = area;
            _entry.vertices = new Vertex[] { new Vertex(), new Vertex() };
            setMouseLocation(_entry.vertices[0]);
            _editor.addEntries(_entry);
        }
    }

    @Override
    public void tick (float elapsed)
    {
        if (_entry == null || !getMousePlaneIntersection(_isect) || _editor.isSpecialDown()) {
            return;
        }
        _entry = (AreaEntry)_entry.clone();
        setMouseLocation(_entry.vertices[_idx]);
        _editor.updateEntries(_entry);
    }

    @Override
    public void entryUpdated (Entry oentry, Entry nentry)
    {
        if (_entry != null && _entry.getKey().equals(oentry.getKey()) && _entry != nentry) {
            _entry = null;
        }
    }

    @Override
    public void entryRemoved (Entry oentry)
    {
        if (_entry != null && _entry.getKey().equals(oentry.getKey())) {
            _entry = null;
        }
    }

    /**
     * Sets the location of the specified vertex to the one indicated by the mouse cursor.
     */
    protected void setMouseLocation (Vertex vertex)
    {
        ((SnapStyle)_snapStyle.getSelectedItem()).applySnap(_isect);
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
    protected void insertVertex (AreaEntry entry, int idx)
    {
        _entry = (AreaEntry)entry.clone();
        _idx = idx;
        _entry.vertices = ArrayUtil.insert(_entry.vertices, new Vertex(), idx);
        _editor.updateEntries(_entry);
    }

    /**
     * Removes the indexed vertices from the supplied entry (removing the entry itself if it has no
     * more vertices).
     */
    protected void removeVertices (AreaEntry entry, int idx, int count)
    {
        if (entry.vertices.length <= count) {
            _editor.removeEntries(entry.getKey());
        } else {
            AreaEntry nentry = (AreaEntry)entry.clone();
            nentry.vertices = ArrayUtil.splice(nentry.vertices, idx, count);
            _editor.updateEntries(nentry);
        }
    }

    /**
     * Allows us to edit the area reference.
     */
    protected static class AreaReference extends EditableReference<AreaConfig>
    {
        /** The area reference. */
        @Editable(nullable=true)
        public ConfigReference<AreaConfig> area;

        @Override
        public ConfigReference<AreaConfig> getReference ()
        {
            return area;
        }

        @Override
        public void setReference (ConfigReference<AreaConfig> ref)
        {
            area = ref;
        }
    }

    /** Our current snap style. */
    protected JComboBox _snapStyle;

    /** The entry containing the vertex we're moving, if any. */
    protected AreaEntry _entry;

    /** The index of the vertex we're moving. */
    protected int _idx;

    /** Holds the result of an intersection test. */
    protected Vector3f _isect = new Vector3f();

    /** A filter that only passes area models. */
    protected final Predicate<SceneElement> AREA_FILTER = new Predicate<SceneElement>() {
        public boolean apply (SceneElement element) {
            Object obj = element.getUserObject();
            return (obj instanceof AreaSprite) &&
                _editor.getLayerPredicate().apply(((AreaSprite) obj).getEntry());
        }
    };
}
