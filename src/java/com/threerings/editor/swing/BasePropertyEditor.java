//
// $Id$

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
     * Gets the path of the property under the mouse cursor.
     */
    protected void getMousePath (StringBuffer buf)
    {
        Point pt = getMousePosition();
        if (pt == null) {
            return;
        }
        String own = getPathComponent(pt);
        if (own != null) {
            if (buf.length() > 0) {
                buf.append('/');
            }
            buf.append(own);
        }
        for (Container cont = this; cont != null; ) {
            Component comp = cont.getComponentAt(pt);
            if (comp == cont || !(comp instanceof Container)) {
                return;

            } else if (comp instanceof BasePropertyEditor && !skipChildPath(comp)) {
                ((BasePropertyEditor)comp).getMousePath(buf);
                return;
            }
            cont = (Container)comp;
            pt = cont.getMousePosition();
        }
    }

    /**
     * Returns this panel's own path component at the specified point (or <code>null</code> for
     * none).
     */
    protected String getPathComponent (Point pt)
    {
        return null;
    }

    /**
     * Determines whether to ignore the specified component (which is contained within this
     * one) when computing the path of the property under the mouse cursor.
     */
    protected boolean skipChildPath (Component comp)
    {
        return false;
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
