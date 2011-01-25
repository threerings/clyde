//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2011 Three Rings Design, Inc.
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

import java.awt.Point;

import javax.swing.BorderFactory;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.editor.swing.ObjectPanel;
import com.threerings.editor.swing.PropertyEditor;

/**
 * An editor for objects with editable properties.
 */
public class ObjectEditor extends PropertyEditor
    implements ChangeListener
{
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        _property.set(_object, _panel.getValue());
        fireStateChanged();
    }

    @Override // documentation inherited
    public void update ()
    {
        _panel.setOuter(_object);
        _panel.setValue(_property.get(_object));
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        makeCollapsible(_ctx, getPropertyLabel());
        _content.add(_panel = new ObjectPanel(
            _ctx, _property.getTypeLabel(), _property.getSubtypes(), _lineage, _object));
        _panel.addChangeListener(this);
    }

    @Override // documentation inherited
    protected String getMousePath (Point pt)
    {
        return _panel.getMousePath();
    }

    /** The object panel. */
    protected ObjectPanel _panel;
}
