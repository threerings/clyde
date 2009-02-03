//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

package com.threerings.editor.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.editor.Introspector;

/**
 * Abstract base class for {@link PropertyEditor} and {@link EditorPanel}.
 */
public abstract class BasePropertyEditor extends JPanel
{
    public BasePropertyEditor ()
    {
        // make sure we inherit the parent's background color
        setBackground(null);
    }

    /**
     * Adds a listener for change events.
     */
    public void addChangeListener (ChangeListener listener)
    {
        listenerList.add(ChangeListener.class, listener);
    }

    /**
     * Removes a change event listener.
     */
    public void removeChangeListener (ChangeListener listener)
    {
        listenerList.remove(ChangeListener.class, listener);
    }

    /**
     * Returns a label for the supplied type.
     */
    public String getLabel (Class type)
    {
        if (type == null) {
            return _msgs.get("m.none");
        }
        String name = type.getName();
        name = name.substring(
            Math.max(name.lastIndexOf('$'), name.lastIndexOf('.')) + 1);
        name = StringUtil.toUSLowerCase(StringUtil.unStudlyName(name));
        return getLabel(name, Introspector.getMessageBundle(type));
    }

    /**
     * Returns an array of labels for the supplied names, translating those that have translations.
     */
    public String[] getLabels (String[] names)
    {
        return getLabels(names, _msgs);
    }

    /**
     * Returns an array of labels for the supplied names, translating those that have translations.
     */
    public String[] getLabels (String[] names, String bundle)
    {
        return getLabels(names, _msgmgr.getBundle(bundle));
    }

    /**
     * Returns an array of labels for the supplied names, translating those that have translations.
     */
    public String[] getLabels (String[] names, MessageBundle msgs)
    {
        String[] labels = new String[names.length];
        for (int ii = 0; ii < names.length; ii++) {
            labels[ii] = getLabel(names[ii], msgs);
        }
        return labels;
    }

    /**
     * Returns the path of the property under the mouse cursor relative to this property.
     */
    public String getMousePath ()
    {
        Point pt = getMousePosition();
        return (pt == null) ? "" : getMousePath(pt);
    }

    /**
     * Returns the path of the property under the mouse cursor relative to this property.
     *
     * @param pt the location of the mouse cursor.
     */
    protected String getMousePath (Point pt)
    {
        return "";
    }

    /**
     * Returns a label for the supplied name, translating it if a translation exists.
     */
    protected String getLabel (String name)
    {
        return getLabel(name, _msgs);
    }

    /**
     * Returns a label for the supplied name, translating it if a translation exists.
     */
    protected String getLabel (String name, String bundle)
    {
        return getLabel(name, _msgmgr.getBundle(bundle));
    }

    /**
     * Returns a label for the supplied name, translating it if a translation exists.
     */
    protected String getLabel (String name, MessageBundle msgs)
    {
        name = (name.length() == 0) ? "default" : name;
        String key = "m." + name;
        return msgs.exists(key) ? msgs.get(key) : name;
    }

    /**
     * Fires a state changed event.
     */
    protected void fireStateChanged ()
    {
        Object[] listeners = listenerList.getListenerList();
        ChangeEvent event = null;
        for (int ii = listeners.length - 2; ii >= 0; ii -= 2) {
            if (listeners[ii] == ChangeListener.class) {
                if (event == null) {
                    event = new ChangeEvent(this);
                }
                ((ChangeListener)listeners[ii + 1]).stateChanged(event);
            }
        }
    }

    /**
     * Returns a background color darkened by the specified number of shades.
     */
    protected static Color getDarkerBackground (float shades)
    {
        int value = BASE_BACKGROUND - (int)(shades*SHADE_DECREMENT);
        return new Color(value, value, value);
    }

    /** The message manager to use for translations. */
    protected MessageManager _msgmgr;

    /** The default message bundle. */
    protected MessageBundle _msgs;

    /** The base background value that we darken to indicate nesting. */
    protected static final int BASE_BACKGROUND = 0xEE;

    /** The number of units to darken for each shade. */
    protected static final int SHADE_DECREMENT = 8;
}
