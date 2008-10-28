//
// $Id$

package com.threerings.tudey.tools;

import java.awt.event.MouseEvent;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.threerings.editor.swing.EditorPanel;

import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;

/**
 * The arrow tool.
 */
public class Arrow extends EditorTool
    implements ChangeListener
{
    /**
     * Creates the arrow tool.
     */
    public Arrow (SceneEditor editor)
    {
        super(editor);
        add(_epanel = new EditorPanel(editor));
        _epanel.addChangeListener(this);
    }

    /**
     * Requests to start editing the specified entry.
     */
    public void edit (Entry entry)
    {
        _editor.setSelection(entry);
        _epanel.setObject(entry.clone());
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        _editor.incrementEditId();
        _ignoreUpdate = true;
        try {
            Entry entry = (Entry)_epanel.getObject();
            _editor.updateEntry((Entry)entry.clone());
        } finally {
            _ignoreUpdate = false;
        }
    }

    @Override // documentation inherited
    public void sceneChanged (TudeySceneModel scene)
    {
        super.sceneChanged(scene);
        _epanel.setObject(null);
    }

    @Override // documentation inherited
    public void entryUpdated (Entry oentry, Entry nentry)
    {
        if (_ignoreUpdate) {
            return;
        }
        Entry entry = (Entry)_epanel.getObject();
        if (entry != null && entry.getKey().equals(oentry.getKey())) {
            _epanel.setObject(nentry.clone());
        }
    }

    @Override // documentation inherited
    public void entryRemoved (Entry oentry)
    {
        Entry entry = (Entry)_epanel.getObject();
        if (entry != null && entry.getKey().equals(oentry.getKey())) {
            _epanel.setObject(null);
        }
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        if (_editor.isThirdButtonDown() && !_editor.isControlDown()) {
            _editor.deleteMouseEntry();
        }
    }

    @Override // documentation inherited
    public void mousePressed (MouseEvent event)
    {
        if (event.getButton() == MouseEvent.BUTTON1 && !_editor.isControlDown()) {
            Entry entry = _editor.getMouseEntry();
            if (entry != null) {
                if (_editor.isSelected(entry)) {
                    _editor.moveSelection();
                } else {
                    _editor.select(entry);
                }
            } else {
                _editor.clearSelection();
                _epanel.setObject(null);
            }
        }
    }

    /** The editor panel that we use to edit things. */
    protected EditorPanel _epanel;

    /** Notes that we should ignore an update because we're the one effecting it. */
    protected boolean _ignoreUpdate;
}
