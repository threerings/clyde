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

package com.threerings.opengl.gui;

import java.util.ArrayList;

import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.layout.GroupLayout;

/**
 * Displays a list of selectable entries and fires an {@link ActionEvent} when the selected value
 * changes. Each entry is displayed as a string obtained by calling {@link Object#toString} on the
 * supplied values.
 */
public class List extends Container
    implements Selectable<Object>
{
    /**
     * Creates an empty list.
     */
    public List (GlContext ctx)
    {
        this(ctx, null);
    }

    /**
     * Creates a list and populates it with the supplied values.
     */
    public List (GlContext ctx, Object[] values)
    {
        super(ctx, GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        setValues(values);
    }

    /**
     * Sets the list's values.
     */
    public void setValues (Object[] values)
    {
        _selidx = -1;
        removeAll();
        _values.clear();
        if (values != null) {
            for (Object value : values) {
                addValue(value);
            }
        }
    }

    /**
     * Adds a value to the list.
     */
    public void addValue (Object value)
    {
        // list entries can be selected by clicking on them, but unselected
        // only by clicking another entry
        ToggleButton button = new ToggleButton(_ctx, String.valueOf(value)) {
            protected void fireAction (long when, int modifiers) {
                if (!_selected) {
                    super.fireAction(when, modifiers);
                }
            }
        };
        button.setStyleConfig("Default/ListEntry");
        button.addListener(_slistener);
        add(button);
        _values.add(value);
    }

    /**
     * Removes a value from the list, if it is present.
     *
     * @return true if the value was removed, false if it was not in the list
     */
    public boolean removeValue (Object value)
    {
        int idx = _values.indexOf(value);
        if (idx == -1) {
            return false;
        }
        if (idx == _selidx) {
            _selidx = -1;
        }
        remove(_children.get(idx));
        _values.remove(idx);
        return true;
    }

    // from Selectable<Object>
    public Object getSelected ()
    {
        return (_selidx == -1) ? null : _values.get(_selidx);
    }

    // from Selectable<Object>
    public void setSelected (Object value)
    {
        setSelectedIndex(_values.indexOf(value));
    }

    // from Selectable<Object>
    public int getSelectedIndex ()
    {
        return _selidx;
    }

    // from Selectable<Object>
    public void setSelectedIndex (int index)
    {
        if (index == _selidx) {
            return;
        }
        if (_selidx != -1) {
            ((ToggleButton)_children.get(_selidx)).setSelected(false);
        }
        if (index != -1) {
            ((ToggleButton)_children.get(index)).setSelected(true);
        }
        _selidx = index;
    }

    @Override
    protected String getDefaultStyleConfig ()
    {
        return "Default/List";
    }

    /** The values contained in the list. */
    protected ArrayList<Object> _values = new ArrayList<Object>();

    /** The index of the current selection (or -1 for none). */
    protected int _selidx = -1;

    /** Listens for button selections. */
    protected ActionListener _slistener = new ActionListener() {
        public void actionPerformed (ActionEvent e) {
            if (_selidx != -1) {
                ((ToggleButton)_children.get(_selidx)).setSelected(false);
            }
            _selidx = getComponentIndex((ToggleButton)e.getSource());
            emitEvent(new ActionEvent(List.this, e.getWhen(), e.getModifiers(),
                SELECT, getSelected()));
        }
    };
}
