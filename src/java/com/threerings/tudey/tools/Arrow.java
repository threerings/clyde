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
        _epanel.setObject(entry.clone());
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        Object object = _epanel.getObject();
        if (object instanceof Entry) {
            Entry entry = (Entry)object;
            _editor.updateEntry((Entry)entry.clone());
        }
    }

    @Override // documentation inherited
    public void sceneChanged (TudeySceneModel scene)
    {
        super.sceneChanged(scene);
        _epanel.setObject(null);
    }

    @Override // documentation inherited
    public void entryRemoved (Entry oentry)
    {
        Object object = _epanel.getObject();
        if (object instanceof Entry && ((Entry)object).getKey().equals(oentry.getKey())) {
            _epanel.setObject(null);
        }
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        if (_editor.isThirdButtonDown() && !_editor.isControlDown()) {
            _editor.deleteMouseObject();
        }
    }

    @Override // documentation inherited
    public void mousePressed (MouseEvent event)
    {
        if (event.getButton() == MouseEvent.BUTTON1 && !_editor.isControlDown()) {
            _editor.editMouseObject();
        }
    }

    /** The editor panel that we use to edit things. */
    protected EditorPanel _epanel;
}
