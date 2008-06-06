//
// $Id$

package com.threerings.editor.swing;

import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.editor.util.EditorContext;

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
        String name = (type == null) ? "none" : type.getName();
        name = name.substring(
            Math.max(name.lastIndexOf('$'), name.lastIndexOf('.')) + 1);
        name = StringUtil.toUSLowerCase(StringUtil.unStudlyName(name));
        return getLabel(name);
    }

    /**
     * Returns an array of labels for the supplied names, translating those that have translations.
     */
    public String[] getLabels (String[] names)
    {
        String[] labels = new String[names.length];
        for (int ii = 0; ii < names.length; ii++) {
            labels[ii] = getLabel(names[ii]);
        }
        return labels;
    }

    /**
     * Returns a label for the supplied name, translating it if a translation exists.
     */
    protected String getLabel (String name)
    {
        name = (name.length() == 0) ? "default" : name;
        String key = "m." + name;
        return _msgs.exists(key) ? _msgs.get(key) : name;
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
    protected static Color getDarkerBackground (int shades)
    {
        int value = BASE_BACKGROUND - shades*SHADE_DECREMENT;
        return new Color(value, value, value);
    }

    /** Provides access to common services. */
    protected EditorContext _ctx;

    /** The message bundle used for property translations. */
    protected MessageBundle _msgs;

    /** The object being edited. */
    protected Object _object;

    /** The base background value that we darken to indicate nesting. */
    protected static final int BASE_BACKGROUND = 0xEE;

    /** The number of units to darken for each shade. */
    protected static final int SHADE_DECREMENT = 8;
}
