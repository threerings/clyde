//
// $Id$

package com.threerings.tudey.tools;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.threerings.editor.swing.EditorPanel;

import com.threerings.tudey.data.TudeySceneModel;

/**
 * The global editor tool.
 */
public class GlobalEditor extends SceneEditor.Tool
    implements ChangeListener
{
    /**
     * Creates the global editor tool.
     */
    public GlobalEditor (SceneEditor editor)
    {
        _editor = editor;

        // create and add the editor panel
        add(_epanel = new EditorPanel(editor));
        _epanel.addChangeListener(this);
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        _editor.getView().globalsChanged();
    }

    @Override // documentation inherited
    public void sceneChanged (TudeySceneModel scene)
    {
        _epanel.setObject(scene.globals);
    }

    /** The editor that created the tool. */
    protected SceneEditor _editor;

    /** The panel that we use to edit the scene's globals. */
    protected EditorPanel _epanel;
}
