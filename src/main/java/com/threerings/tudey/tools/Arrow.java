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
        _editor.setSelectionAsRecent();
        _epanel.setObject(entry.clone());
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        _editor.incrementEditId();
        _ignoreUpdate = true;
        try {
            Entry entry = (Entry)_epanel.getObject();
            if (entry != null) {
                _editor.updateEntries((Entry)entry.clone());
            }
        } finally {
            _ignoreUpdate = false;
        }
    }

    @Override
    public void sceneChanged (TudeySceneModel scene)
    {
        super.sceneChanged(scene);
        _epanel.setObject(null);
    }

    @Override
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

    @Override
    public void entryRemoved (Entry oentry)
    {
        Entry entry = (Entry)_epanel.getObject();
        if (entry != null && entry.getKey().equals(oentry.getKey())) {
            _epanel.setObject(null);
        }
    }

    @Override
    public void tick (float elapsed)
    {
        if (_editor.isThirdButtonDown() && !_editor.isSpecialDown()) {
            _editor.deleteMouseEntry();
        }
    }

    @Override
    public void mousePressed (MouseEvent event)
    {
        if (event.getButton() == MouseEvent.BUTTON1 && !_editor.isSpecialDown()) {
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
