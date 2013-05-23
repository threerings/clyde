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

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.threerings.tudey.data.TudeySceneModel;

/**
 * The notepad tool.
 */
public class Notepad extends EditorTool
    implements DocumentListener
{
    /**
     * Creates the notepad tool.
     */
    public Notepad (SceneEditor editor)
    {
        super(editor);
        add(new JScrollPane(_area = new JTextArea()));
        _area.getDocument().addDocumentListener(this);
        _area.setLineWrap(true);
        _area.setWrapStyleWord(true);
    }

    // documentation inherited from interface DocumentListener
    public void insertUpdate (DocumentEvent event)
    {
        _scene.setNotes(_area.getText());
    }

    // documentation inherited from interface DocumentListener
    public void removeUpdate (DocumentEvent event)
    {
        _scene.setNotes(_area.getText());
    }

    // documentation inherited from interface DocumentListener
    public void changedUpdate (DocumentEvent event)
    {
        _scene.setNotes(_area.getText());
    }

    @Override
    public void sceneChanged (TudeySceneModel scene)
    {
        super.sceneChanged(scene);
        _area.setText(scene.getNotes());
    }

    /** The text area. */
    protected JTextArea _area;
}
