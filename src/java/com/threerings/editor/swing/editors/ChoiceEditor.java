//
// $Id$

package com.threerings.editor.swing.editors;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import com.samskivert.util.ObjectUtil;
import com.samskivert.util.StringUtil;

import com.threerings.editor.swing.PropertyEditor;

import static com.threerings.editor.Log.*;

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
        if (!ObjectUtil.equals(_property.get(_object), selected)) {
            _property.set(_object, selected);
            fireStateChanged();
        }
    }

    @Override // documentation inherited
    public void update ()
    {
        Object[] options = getOptions();
        _box.setModel(new DefaultComboBoxModel(options));
        _box.setSelectedItem(_property.get(_object));
    }

    @Override // documentation inherited
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
