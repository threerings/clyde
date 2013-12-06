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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import com.google.common.base.Objects;

import com.samskivert.util.StringUtil;

import com.threerings.editor.swing.PropertyEditor;

import static com.threerings.editor.Log.log;

/**
 * Provides a means of selecting between several different objects.
 */
public class ChoiceEditor extends PropertyEditor
    implements ActionListener
{
    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        Object selected = _box.getSelectedItem();
        if (!Objects.equal(_property.get(_object), selected)) {
            _property.set(_object, selected);
            fireStateChanged();
        }
    }

    @Override
    public void update ()
    {
        Object[] options = getOptions();
        _box.setModel(new DefaultComboBoxModel(options));
        _box.setSelectedItem(_property.get(_object));
    }

    @Override
    protected void didInit ()
    {
        add(new JLabel(getPropertyLabel() + ":"));
        add(_box = new JComboBox());
        _box.addActionListener(this);
    }

    /**
     * Returns the array of options available for selection.
     */
    protected Object[] getOptions ()
    {
        Object mobj = _property.getMemberObject(_object);
        if (mobj == null) {
            return new Object[0];
        }
        Class<?> mclass = mobj.getClass();
        Member member = _property.getMember();
        String mname = member.getName();
        mname = (member instanceof Method) ? mname.substring(3) : StringUtil.capitalize(mname);
        try {
            return (Object[])mclass.getMethod("get" + mname + "Options").invoke(mobj);
        } catch (NoSuchMethodException nsme) {
            // fall through
        } catch (Exception e) {
            log.warning("Error retrieving options.", "class", mclass, "member", mname, e);
        }
        return new Object[0];
    }

    /** The combo box. */
    protected JComboBox _box;
}
