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

package com.threerings.editor.swing.editors;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import com.threerings.editor.PathProperty;
import com.threerings.editor.swing.BasePropertyEditor;

/**
 * A property editor that simply returns the path to the current property.
 */
public class GetPathEditor extends ObjectEditor
    implements ActionListener
{
    // documentation inehrtied from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        BasePropertyEditor editor = this;
        for (Component comp = this; comp != null; ) {
            if (comp instanceof BasePropertyEditor) {
                editor = (BasePropertyEditor)comp;
            }
            comp = comp.getParent();
        }
        copyPropertyPath(editor.getMousePath());
    }

    @Override
    public void update ()
    {
        if (_panel != null) {
            super.update();
        }
    }

    @Override
    public String getComponentPath (Component comp, boolean mouse)
    {
        if (_panel != null) {
            return super.getComponentPath(comp, mouse);
        }
        return "";
    }

    @Override
    protected void didInit ()
    {
        if (_property instanceof PathProperty) {
            super.didInit();
        } else {
            JButton getPath = new JButton(_msgs.get("m.get_path"));
            add(getPath);
            getPath.addActionListener(this);
        }
    }
}
