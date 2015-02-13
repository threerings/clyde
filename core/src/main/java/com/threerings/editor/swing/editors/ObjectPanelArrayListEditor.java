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
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.threerings.editor.swing.ObjectPanel;

/**
 * An editor for arrays or lists of objects. Uses embedded panels.
 */
public class ObjectPanelArrayListEditor extends PanelArrayListEditor
    implements ChangeListener
{
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        ObjectPanel panel = (ObjectPanel)event.getSource();
        int idx = ((EntryPanel)panel.getParent()).getIndex();
        setValue(idx, panel.getValue());
        fireStateChanged(true);
    }

    @Override
    protected void updatePanel (EntryPanel panel, Object value)
    {
        ObjectPanel opanel = (ObjectPanel)panel.getContent();
        opanel.setOuter(_object);
        opanel.setValue(value);
    }

    @Override
    protected void addPanel (Object value)
    {
        _panels.add(new ObjectEntryPanel(value));
    }

    /**
     * A panel for an object entry.
     */
    protected class ObjectEntryPanel extends EntryPanel
    {
        public ObjectEntryPanel (Object value)
        {
            super(value);
        }

        @Override
        public String getComponentPath (Component comp, boolean mouse)
        {
            return ((ObjectPanel)_content).getComponentPath(comp, mouse);
        }

        @Override
        protected JPanel createPanel (Object value)
        {
            ObjectPanel opanel = new ObjectPanel(
                _ctx, _property.getComponentTypeLabel(),
                _property.getComponentSubtypes(), _lineage, _object);
            opanel.setValue(value);
            opanel.addChangeListener(ObjectPanelArrayListEditor.this);
            return opanel;
        }
    }
}
