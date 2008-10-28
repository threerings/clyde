//
// $Id$

package com.threerings.tudey.tools;

import java.awt.event.MouseEvent;

import com.threerings.tudey.data.TudeySceneModel.Entry;

/**
 * The mover tool.
 */
public class Mover extends BaseMover
{
    /**
     * Creates the mover tool.
     */
    public Mover (SceneEditor editor)
    {
        super(editor);
    }

    @Override // documentation inherited
    public void deactivate ()
    {
        // cancel any movement in process
        super.deactivate();
        clear();
    }

    @Override // documentation inherited
    public void mousePressed (MouseEvent event)
    {
        if (event.getButton() != MouseEvent.BUTTON1 || _editor.isControlDown()) {
            return;
        }
        if (_cursorVisible) {
            // place the transformed entries and clear the tool
            _editor.select(placeEntries());
            clear();
        } else {
            Entry entry = _editor.getMouseEntry();
            if (entry != null) {
                if (_editor.isSelected(entry)) {
                    _editor.moveSelection();
                } else {
                    _editor.removeAndMove(entry);
                }
            }
        }
    }
}
