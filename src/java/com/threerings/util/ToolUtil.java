//
// $Id$

package com.threerings.util;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.io.File;

import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import com.samskivert.util.ListUtil;
import com.samskivert.util.StringUtil;

import com.threerings.editor.Editable;
import com.threerings.export.SerializableWrapper;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.resource.ResourceManager;

import static com.threerings.ClydeLog.*;

/**
 * Contains some static classes and methods used by our various (Swing-based) tool applications.
 */
public class ToolUtil
{
    /** A data flavor for referenced local wrapped objects (on this VM). */
    public static final DataFlavor LOCAL_WRAPPED_FLAVOR;
    static {
        DataFlavor flavor = null;
        try {
            flavor = new DataFlavor(
                DataFlavor.javaJVMLocalObjectMimeType +
                ";class=com.threerings.export.SerializableWrapper");
        } catch (ClassNotFoundException e) {
             // won't happen
        }
        LOCAL_WRAPPED_FLAVOR = flavor;
    }

    /** A data flavor for serialized wrapped objects (from another VM). */
    public static final DataFlavor SERIALIZED_WRAPPED_FLAVOR =
        new DataFlavor(SerializableWrapper.class, null);

    /** The flavors for local and serialized wrapped objects. */
    public static final DataFlavor[] WRAPPED_FLAVORS = {
        LOCAL_WRAPPED_FLAVOR, SERIALIZED_WRAPPED_FLAVOR };

    /**
     * A simple editable object used to manipulate preferences.
     */
    public static class EditablePrefs
    {
        /**
         * Creates a new editable prefs object using the specified underlying preferences.
         */
        public EditablePrefs (Preferences prefs)
        {
            _prefs = prefs;
        }

        /**
         * Initializes the prefs with a reference to the resource manager.
         */
        public void init (ResourceManager rsrcmgr)
        {
            _rsrcmgr = rsrcmgr;
            String dstr = _prefs.get("resource_dir", null);
            _resourceDir = (dstr == null) ? null : new File(dstr);
            _rsrcmgr.initResourceDir(dstr);
        }

        /**
         * Sets the resource directory.
         */
        @Editable(mode="directory")
        public void setResourceDir (File dir)
        {
            _resourceDir = dir;
            String dstr = (dir == null) ? null : dir.toString();
            if (dstr == null) {
                _prefs.remove("resource_dir");
            } else {
                _prefs.put("resource_dir", dstr);
            }
            _rsrcmgr.initResourceDir(dstr);
        }

        /**
         * Returns the resource directory.
         */
        @Editable
        public File getResourceDir ()
        {
            return _resourceDir;
        }

        /**
         * Retrieves the value of a color preference.
         */
        protected Color4f getPref (String key, Color4f def)
        {
            String cstr = _prefs.get(key, null);
            if (cstr != null) {
                try {
                    return new Color4f(StringUtil.parseFloatArray(cstr));
                } catch (Exception e) {
                    log.warning("Error reading color preference [prefs=" + _prefs +
                        ", key=" + key + ", value=" + cstr + "].", e);
                }
            }
            return def;
        }

        /**
         * Sets the value of a color preference.
         */
        protected void putPref (String key, Color4f value)
        {
            _prefs.put(key, value.r + ", " + value.g + ", " + value.b + ", " + value.a);
        }

        /** The preferences node to use. */
        protected Preferences _prefs;

        /** The resource manager. */
        protected ResourceManager _rsrcmgr;

        /** The resource directory. */
        protected File _resourceDir;
    }

    /**
     * A {@link Transferable} available in two flavors: one for use within a single VM and one that
     * uses binary export/import to exchange data between different VMs.  Either way, the actual
     * data returned is a {@link SerializableWrapper} containing the object of interest.
     */
    public static class WrappedTransfer
        implements Transferable
    {
        public WrappedTransfer (Object object)
        {
            _wrapper = new SerializableWrapper(object);
        }

        // documentation inherited from interface Transferable
        public DataFlavor[] getTransferDataFlavors ()
        {
            return WRAPPED_FLAVORS;
        }

        // documentation inherited from interface Transferable
        public boolean isDataFlavorSupported (DataFlavor flavor)
        {
            return ListUtil.contains(WRAPPED_FLAVORS, flavor);
        }

        // documentation inherited from interface Transferable
        public Object getTransferData (DataFlavor flavor)
        {
            return _wrapper;
        }

        /** The wrapped object. */
        protected SerializableWrapper _wrapper;
    }

    /**
     * Creates a menu with the specified name and mnemonic.
     */
    public static JMenu createMenu (MessageBundle msgs, String name, int mnemonic)
    {
        JMenu menu = new JMenu(msgs.get("m." + name));
        menu.setMnemonic(mnemonic);
        return menu;
    }

    /**
     * Creates a menu item with the specified action, mnemonic, and (optional) accelerator.
     */
    public static JMenuItem createMenuItem (
        ActionListener listener, MessageBundle msgs, String action, int mnemonic, int accelerator)
    {
        return createMenuItem(listener, msgs, action, mnemonic, accelerator, KeyEvent.CTRL_MASK);
    }

    /**
     * Creates a menu item with the specified action, mnemonic, and (optional) accelerator
     * key/modifiers.
     */
    public static JMenuItem createMenuItem (
        ActionListener listener, MessageBundle msgs, String action,
        int mnemonic, int accelerator, int modifiers)
    {
        JMenuItem item = new JMenuItem(msgs.get("m." + action), mnemonic);
        item.setActionCommand(action);
        item.addActionListener(listener);
        if (accelerator != -1) {
            item.setAccelerator(KeyStroke.getKeyStroke(accelerator, modifiers));
        }
        return item;
    }

    /**
     * Creates a button with the specified action.
     */
    public static JButton createButton (ActionListener listener, MessageBundle msgs, String action)
    {
        return createButton(listener, msgs, action, "m." + action);
    }

    /**
     * Creates a button with the specified action and translation key.
     */
    public static JButton createButton (
        ActionListener listener, MessageBundle msgs, String action, String key)
    {
        JButton button = new JButton(msgs.get(key));
        button.setActionCommand(action);
        button.addActionListener(listener);
        return button;
    }
}
