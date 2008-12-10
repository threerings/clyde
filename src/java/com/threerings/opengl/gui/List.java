//
// $Id$

package com.threerings.opengl.gui;

import java.util.ArrayList;

import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.layout.GroupLayout;

/**
 * Displays a list of selectable entries and fires an {@link ActionEvent} when the selected value
 * changes. Each entry is displayed as a string obtained by calling {@link Object#toString} on the
 * supplied values.
 */
public class List extends Container
{
    /** The action fired when the list selection changes. */
    public static final String SELECT = "select";

    /**
     * Creates an empty list.
     */
    public List ()
    {
        this(null);
    }

    /**
     * Creates a list and populates it with the supplied values.
     */
    public List (Object[] values)
    {
        super(GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
            GroupLayout.STRETCH));
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
        ToggleButton button = new ToggleButton(value.toString()) {
            protected void fireAction (long when, int modifiers) {
                if (!_selected) {
                    super.fireAction(when, modifiers);
                }
            }
        };
        button.setStyleClass("list_entry");
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

    /**
     * Returns the currently selected value.
     *
     * @return the selected value, or <code>null</code> for none
     */
    public Object getSelectedValue ()
    {
        return (_selidx == -1) ? null : _values.get(_selidx);
    }

    /**
     * Sets the selected value.
     *
     * @param value the value to select, or <code>null</code> for none
     */
    public void setSelectedValue (Object value)
    {
        int idx = (value == null) ? -1 : _values.indexOf(value);
        if (idx == _selidx) {
            return;
        }
        if (_selidx != -1) {
            ((ToggleButton)_children.get(_selidx)).setSelected(false);
        }
        if (idx != -1) {
            ((ToggleButton)_children.get(idx)).setSelected(true);
        }
        _selidx = idx;
    }

    // documentation inherited
    protected String getDefaultStyleClass ()
    {
        return "list";
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
            _selidx = _children.indexOf(e.getSource());
            emitEvent(new ActionEvent(List.this, e.getWhen(),
                e.getModifiers(), SELECT));
        }
    };
}
