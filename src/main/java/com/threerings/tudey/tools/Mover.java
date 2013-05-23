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

    @Override
    public void deactivate ()
    {
        // cancel any movement in process
        super.deactivate();
        clear();
    }

    @Override
    public void mousePressed (MouseEvent event)
    {
        if (event.getButton() != MouseEvent.BUTTON1 || _editor.isSpecialDown()) {
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
