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

package com.threerings.util;

import java.awt.Rectangle;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.logging.LogManager;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;

import com.samskivert.util.ListUtil;
import com.samskivert.util.OneLineLogFormatter;
import com.samskivert.util.RepeatRecordFilter;
import com.samskivert.util.StringUtil;

import com.threerings.editor.Editable;
import com.threerings.export.util.ExportUtil;
import com.threerings.export.util.SerializableWrapper;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.resource.ResourceManager;

import static com.threerings.ClydeLog.log;

/**
 * Contains some static classes and methods used by our various (Swing-based) tool applications.
 */
public class ToolUtil
{
    /** A data flavor for referenced local wrapped objects (on this VM). */
    public static final DataFlavor LOCAL_WRAPPED_FLAVOR =
        createLocalFlavor(SerializableWrapper.class);

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
            String dstr = ResourceUtil.getPreferredResourceDir();
            _resourceDir = (dstr == null) ? null : new File(dstr);
            _rsrcmgr.initResourceDir(dstr);
        }

        /**
         * Sets the resource directory.
         */
        @Editable(mode="directory", nullable=true)
        public void setResourceDir (File dir)
        {
            _resourceDir = dir;
            String dstr = (dir == null) ? null : dir.toString();
            ResourceUtil.setPreferredResourceDir(dstr);
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
         * Bind and set the window location and size to the provided key prefix.
         */
        public void bindWindowBounds (final String keyPrefix, final Window w)
        {
            Rectangle r = w.getBounds();
            w.setBounds(_prefs.getInt(keyPrefix + "x", r.x),
                    _prefs.getInt(keyPrefix + "y", r.y),
                    _prefs.getInt(keyPrefix + "w", r.width),
                    _prefs.getInt(keyPrefix + "h", r.height));
            w.addComponentListener(new ComponentAdapter() {
                @Override public void componentMoved (ComponentEvent event) {
                    saveBounds();
                }
                @Override public void componentResized (ComponentEvent event) {
                    saveBounds();
                }

                protected void saveBounds () {
                    Rectangle r = w.getBounds();
                    _prefs.putInt(keyPrefix + "x", r.x);
                    _prefs.putInt(keyPrefix + "y", r.y);
                    _prefs.putInt(keyPrefix + "w", r.width);
                    _prefs.putInt(keyPrefix + "h", r.height);
                }
            });
        }

        /**
         * Bind and set the divider location of the specified pane to the provided key.
         */
        public void bindDividerLocation (final String key, final JSplitPane pane)
        {
            pane.setDividerLocation(_prefs.getInt(key, pane.getDividerLocation()));
            pane.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange (PropertyChangeEvent event) {
                    if (JSplitPane.DIVIDER_LOCATION_PROPERTY.equals(event.getPropertyName())) {
                        _prefs.putInt(key, pane.getDividerLocation());
                    }
                }
            });
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

        /**
         * Retrieves the value of an exportable preference.
         */
        protected Object getPref (String key, Object def)
        {
            byte[] bytes = _prefs.getByteArray(key, null);
            Object object = (bytes == null) ? null : ExportUtil.fromBytes(bytes);
            return (object == null) ? def : object;
        }

        /**
         * Sets the value of an exportable preference.
         */
        protected void putPref (String key, Object value)
        {
            byte[] bytes = ExportUtil.toBytes(value);
            if (bytes != null) {
                _prefs.putByteArray(key, bytes);
            }
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
     * Creates a data flavor for transferring local references to objects of the specified class.
     */
    public static DataFlavor createLocalFlavor (Class<?> clazz)
    {
        try {
            return new DataFlavor(
                DataFlavor.javaJVMLocalObjectMimeType + ";class=" + clazz.getName());
        } catch (ClassNotFoundException e) {
            return null; // won't happen
        }
    }

    /**
     * Returns the wrapped transfer data from the supplied transferable, or <code>null</code> if
     * the wrapped flavors are not supported (or an exception occurs).
     */
    public static Object getWrappedTransferData (Transferable t)
    {
        try {
            Object data;
            if (t.isDataFlavorSupported(LOCAL_WRAPPED_FLAVOR)) {
                data = t.getTransferData(LOCAL_WRAPPED_FLAVOR);
            } else if (t.isDataFlavorSupported(SERIALIZED_WRAPPED_FLAVOR)) {
                data = t.getTransferData(SERIALIZED_WRAPPED_FLAVOR);
            } else {
                return null;
            }
            return ((SerializableWrapper)data).getObject();

        } catch (Exception e) { // UnsupportedFlavorException, IOException
            log.warning("Error retrieving transfer data.", "transferable", t, e);
            return null;
        }
    }

    /**
     * Unless directed otherwise by a system property, redirects console output to the named log
     * file.
     */
    public static void configureLog (String logfile)
    {
        // make sure we haven't already configured and that redirection is not disabled
        if (_logConfigured || Boolean.getBoolean("no_log_redir")) {
            return;
        }
        // first delete any previous previous log file
        File olog = new File(getLogPath("old-" + logfile));
        if (olog.exists()) {
            olog.delete();
        }

        // next rename the previous log file
        File nlog = new File(getLogPath(logfile));
        if (nlog.exists()) {
            nlog.renameTo(olog);
        }

        // and now redirect our output
        try {
            PrintStream logOut = new PrintStream(
                new BufferedOutputStream(new FileOutputStream(nlog)), true);
            System.setOut(logOut);
            System.setErr(logOut);

            // reconfigure the log manager, since it caches its reference to stderr
            LogManager.getLogManager().readConfiguration();
            OneLineLogFormatter.configureDefaultHandler();
            RepeatRecordFilter.configureDefaultHandler(100);

        } catch (IOException ioe) {
            log.warning("Failed to open debug log.", "path", nlog, ioe);
            return;
        }

        // announce and note that we're configured
        log.info("Logging to '" + nlog + "'.");
        _logConfigured = true;
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
        return new JMenuItem(createAction(
            listener, msgs, action, mnemonic, accelerator, modifiers));
    }

    /**
     * Creates a check box menu item with the specified action, mnemonic, and (optional)
     * accelerator.
     */
    public static JCheckBoxMenuItem createCheckBoxMenuItem (
        ActionListener listener, MessageBundle msgs, String action, int mnemonic, int accelerator)
    {
        return createCheckBoxMenuItem(
            listener, msgs, action, mnemonic, accelerator, KeyEvent.CTRL_MASK);
    }

    /**
     * Creates a check box menu item with the specified action, mnemonic, and (optional)
     * accelerator key/modifiers.
     */
    public static JCheckBoxMenuItem createCheckBoxMenuItem (
        ActionListener listener, MessageBundle msgs, String action,
        int mnemonic, int accelerator, int modifiers)
    {
        return new JCheckBoxMenuItem(createAction(
            listener, msgs, action, mnemonic, accelerator, modifiers));
    }

    /**
     * Creates an action with the specified command, mnemonic, and (optional) accelerator.
     */
    public static Action createAction (
        ActionListener listener, MessageBundle msgs, String command, int mnemonic, int accelerator)
    {
        return createAction(listener, msgs, command, mnemonic, accelerator, KeyEvent.CTRL_MASK);
    }

    /**
     * Creates an action with the specified command, mnemonic, and (optional) accelerator
     * key/modifiers.
     */
    public static Action createAction (
        final ActionListener listener, MessageBundle msgs, String command,
        int mnemonic, int accelerator, int modifiers)
    {
        AbstractAction action = new AbstractAction(msgs.get("m." + command)) {
            public void actionPerformed (ActionEvent event) {
                listener.actionPerformed(event);
            }
        };
        action.putValue(Action.ACTION_COMMAND_KEY, command);
        action.putValue(Action.MNEMONIC_KEY, mnemonic);
        if (accelerator != -1) {
            action.putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke(accelerator, modifiers));
        }
        return action;
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

    /**
     * Notes that a window has been added.  When the window is removed, it should call
     * {@link #windowRemoved}.
     */
    public static void windowAdded ()
    {
        _windowCount++;
    }

    /**
     * Notes that a window has been removed.  When the window count reaches zero, the app will
     * exit.
     */
    public static void windowRemoved ()
    {
        if (--_windowCount == 0) {
            System.exit(0);
        }
    }

    /**
     * Returns the path at which to store the named log file.
     */
    protected static String getLogPath (String logfile)
    {
        String appdir = System.getProperty("appdir");
        if (StringUtil.isBlank(appdir)) {
            appdir = ".clyde";
            String home = System.getProperty("user.home");
            if (!StringUtil.isBlank(home)) {
                appdir = home + File.separator + appdir;
            }
            File appfile = new File(appdir);
            if (!appfile.exists()) {
                appfile.mkdir();
            }
        }
        return appdir + File.separator + logfile;
    }

    /** Set when we have configured our log to avoid reconfiguring. */
    protected static boolean _logConfigured;

    /** The number of open windows.  When this reaches zero, we can exit the app. */
    protected static int _windowCount;
}
