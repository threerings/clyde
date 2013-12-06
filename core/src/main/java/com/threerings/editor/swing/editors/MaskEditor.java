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

import java.util.Iterator;
import java.util.Map;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.google.common.collect.Maps;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Config;
import com.samskivert.util.ListUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.editor.swing.PropertyEditor;

import static com.threerings.editor.Log.log;

/**
 * Editor for bitmask properties.
 */
public class MaskEditor extends PropertyEditor
    implements DocumentListener, ActionListener
{
    /**
     * Retrieves the value of a flag from the configuration, or zero if not found.
     */
    public static int getFlag (String mode, String flag)
    {
        String[] flags = _modes.get(mode);
        int idx = (flags == null) ? -1 : ListUtil.indexOf(flags, flag);
        return (idx == -1) ? 0 : (1 << idx);
    }

    // documentation inherited from interface DocumentListener
    public void insertUpdate (DocumentEvent event)
    {
        changedUpdate(null);
    }

    // documentation inherited from interface DocumentListener
    public void removeUpdate (DocumentEvent event)
    {
        changedUpdate(null);
    }

    // documentation inherited from interface DocumentListener
    public void changedUpdate (DocumentEvent event)
    {
        long value;
        try {
            value = Long.parseLong(_field.getText());
        } catch (NumberFormatException e) {
            return;
        }
        Number nvalue = fromLong(value);
        if (!_property.get(_object).equals(nvalue)) {
            _property.set(_object, nvalue);
            fireStateChanged();
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if (event.getSource() != _button) {
            long value = 0L;
            for (int ii = 0, nn = _popup.getComponentCount(); ii < nn; ii++) {
                JCheckBoxMenuItem item = (JCheckBoxMenuItem)_popup.getComponent(ii);
                int idx = ListUtil.indexOf(_flags, item.getText());
                if (item.isSelected()) {
                    value |= (1L << idx);
                }
            }
            Number nvalue = fromLong(value);
            if (!_property.get(_object).equals(nvalue)) {
                _property.set(_object, nvalue);
                fireStateChanged();
            }
            update();
            return;
        }
        if (_popup == null) {
            _popup = new JPopupMenu();
            for (String flag : _flags) {
                if (!StringUtil.isBlank(flag)) {
                    JCheckBoxMenuItem item = new JCheckBoxMenuItem(flag);
                    _popup.add(item);
                    item.addActionListener(this);
                }
            }
        }
        long value = ((Number)_property.get(_object)).longValue();
        for (int ii = 0, nn = _popup.getComponentCount(); ii < nn; ii++) {
            JCheckBoxMenuItem item = (JCheckBoxMenuItem)_popup.getComponent(ii);
            int idx = ListUtil.indexOf(_flags, item.getText());
            item.setSelected((value & (1L << idx)) != 0);
        }
        _popup.show(_button, 0, _button.getHeight());
    }

    @Override
    public void update ()
    {
        // this generates two documents events: first a remove, then an add.  we don't want to
        // fire a state change, so we remove ourselves as a document listener when updating
        _field.getDocument().removeDocumentListener(this);
        _field.setValue(_property.get(_object));
        _field.getDocument().addDocumentListener(this);
    }

    @Override
    protected void didInit ()
    {
        add(new JLabel(getPropertyLabel() + ":"));
        NumberFormat fmt = NumberFormat.getIntegerInstance();
        fmt.setGroupingUsed(false);
        add(_field = new JFormattedTextField(fmt));
        _field.setColumns(4);
        _field.setHorizontalAlignment(JFormattedTextField.RIGHT);
        _field.getDocument().addDocumentListener(this);

        add(_button = new JButton("..."));
        _button.setPreferredSize(new Dimension(20, 20));
        _button.addActionListener(this);

        String mode = getMode();
        String[] names = _modes.get(mode);
        if (names == null) {
            if (!StringUtil.isBlank(mode)) {
                log.warning("Unknown mask mode.", "mode", mode);
            }
            _flags = new String[8];
            for (int ii = 0; ii < _flags.length; ii++) {
                _flags[ii] = String.valueOf(ii);
            }
        } else {
            MessageBundle msgs = _ctx.getMessageManager().getBundle("editor.mask");
            _flags = new String[names.length];
            for (int ii = 0; ii < names.length; ii++) {
                String name = names[ii];
                _flags[ii] = StringUtil.isBlank(name) ? name : getLabel(name, msgs);
            }
        }
    }

    /**
     * Converts a long value to a value of the property's type.
     */
    protected Number fromLong (long value)
    {
        Class<?> type = _property.getType();
        if (type == Byte.TYPE || type == Byte.class) {
            return (byte)value;
        } else if (type == Integer.TYPE || type == Integer.class) {
            return (int)value;
        } else if (type == Long.TYPE || type == Long.class) {
            return value;
        } else { // type == Short.TYPE || type == Short.class
            return (short)value;
        }
    }

    /** The text field that shows the numeric mask value. */
    protected JFormattedTextField _field;

    /** The button that brings up the flag selection menu. */
    protected JButton _button;

    /** The lazily created popup menu. */
    protected JPopupMenu _popup;

    /** The translated mode flags. */
    protected String[] _flags;

    /** The names of the flags for each mode. */
    protected static Map<String, String[]> _modes = Maps.newHashMap();

    static {
        // load the modes from the configuration
        Config config = new Config("/rsrc/config/editor/mask");
        for (Iterator<String> it = config.keys(); it.hasNext(); ) {
            String key = it.next();
            _modes.put(key, config.getValue(key, ArrayUtil.EMPTY_STRING));
        }
    }
}
