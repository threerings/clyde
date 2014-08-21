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

package com.threerings.config.tools;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.util.prefs.Preferences;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.threerings.media.image.ColorPository;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;
import com.threerings.util.ToolUtil;

import com.threerings.editor.Introspector;
import com.threerings.editor.swing.BaseEditorPanel;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.editor.swing.FindDialog;
import com.threerings.editor.util.EditorContext;
import com.threerings.swing.LogPanel;

import com.threerings.config.ConfigManager;

/**
 * The superclass of {@link ConfigEditor} and {@link ResourceEditor}.
 */
public abstract class BaseConfigEditor extends JFrame
    implements EditorContext, ActionListener
{
    /**
     * Utility method to create an editor for the identified config.
     */
    public static BaseConfigEditor createEditor (EditorContext ctx, Class<?> clazz, String name)
    {
        if (ctx.getConfigManager().isResourceClass(clazz)) {
            return new ResourceEditor(
                ctx.getMessageManager(), ctx.getConfigManager().getRoot(), ctx.getColorPository(),
                ctx.getResourceManager().getResourceFile(name).toString());

        } else {
            return ConfigEditor.create(ctx, clazz, name);
        }
    }

    /**
     * Creates a new config editor.
     */
    public BaseConfigEditor (
        MessageManager msgmgr, ConfigManager cfgmgr, ColorPository colorpos, String msgs)
    {
        _rsrcmgr = cfgmgr.getResourceManager();
        _msgmgr = msgmgr;
        _cfgmgr = cfgmgr;
        _colorpos = colorpos;
        _msgs = _msgmgr.getBundle(msgs);

        // configure the log file
        ToolUtil.configureLog(msgs + ".log");

        // initialize the title
        setTitle(_msgs.get("m.title"));

        // dispose when the window is closed
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // create and init the editable prefs, which also (re)sets the resource directory
        _eprefs = createEditablePrefs(_prefs);
        _eprefs.init(_rsrcmgr);

        // add the log status panel
        boolean first = !cfgmgr.isInitialized();
        add(new LogPanel(_msgmgr, first), BorderLayout.SOUTH);

        // initialize the configuration manager if not yet initialized
        if (first) {
            cfgmgr.init();
        }
    }

    // documentation inherited from interface EditorContext
    public ResourceManager getResourceManager ()
    {
        return _rsrcmgr;
    }

    // documentation inherited from interface EditorContext
    public MessageManager getMessageManager ()
    {
        return _msgmgr;
    }

    // documentation inherited from interface EditorContext
    public ConfigManager getConfigManager ()
    {
        return _cfgmgr;
    }

    // documentation inherited from interface EditorContext
    public ColorPository getColorPository ()
    {
        return _colorpos;
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getActionCommand();
        if (action.equals("close")) {
            dispose();
        } else if (action.equals("quit")) {
            System.exit(0);
        } else if (action.equals("preferences")) {
            if (_pdialog == null) {
                _pdialog = EditorPanel.createDialog(
                    this, this, _msgs.get("t.preferences"), _eprefs);
            }
            _pdialog.setVisible(true);
        } else if (action.equals("find")) {
            if (_fdialog == null) {
                _fdialog = FindDialog.createDialog(this, this);
            }
            _fdialog.show(getFindEditorPanel());
        } else if (action.equals("find_next")) {
            if (_fdialog != null) {
                _fdialog.find(getFindEditorPanel());
            }
        }
    }

    @Override
    public void addNotify ()
    {
        super.addNotify();
        ToolUtil.windowAdded();
    }

    @Override
    public void removeNotify ()
    {
        super.removeNotify();
        ToolUtil.windowRemoved();
    }

    /**
     * Create the editable prefs we'll use for this tool.
     */
    protected ToolUtil.EditablePrefs createEditablePrefs (Preferences prefs)
    {
        return new ToolUtil.EditablePrefs(prefs);
    }

    /**
     * Creates a menu with the specified name and mnemonic.
     */
    protected JMenu createMenu (String name, int mnemonic)
    {
        return ToolUtil.createMenu(_msgs, name, mnemonic);
    }

    /**
     * Creates a menu item with the specified action, mnemonic, and (optional) accelerator.
     */
    protected JMenuItem createMenuItem (String action, int mnemonic, int accelerator)
    {
        return ToolUtil.createMenuItem(this, _msgs, action, mnemonic, accelerator);
    }

    /**
     * Creates a menu item with the specified action, mnemonic, and (optional) accelerator
     * key/modifiers.
     */
    protected JMenuItem createMenuItem (
        String action, int mnemonic, int accelerator, int modifiers)
    {
        return ToolUtil.createMenuItem(this, _msgs, action, mnemonic, accelerator, modifiers);
    }

    /**
     * Creates an action with the specified command, mnemonic, and (optional) accelerator.
     */
    protected Action createAction (String command, int mnemonic, int accelerator)
    {
        return ToolUtil.createAction(this, _msgs, command, mnemonic, accelerator);
    }

    /**
     * Creates an action with the specified command, mnemonic, and (optional) accelerator
     * key/modifiers.
     */
    protected Action createAction (String command, int mnemonic, int accelerator, int modifiers)
    {
        return ToolUtil.createAction(this, _msgs, command, mnemonic, accelerator, modifiers);
    }

    /**
     * Creates a button with the specified action.
     */
    protected JButton createButton (String action)
    {
        return ToolUtil.createButton(this, _msgs, action);
    }

    /**
     * Creates a button with the specified action and translation key.
     */
    protected JButton createButton (String action, String key)
    {
        return ToolUtil.createButton(this, _msgs, action, key);
    }

    /**
     * Returns a translated label for the supplied one, if one exists; otherwise, simply returns
     * the untranslated name.
     */
    protected String getLabel (String name)
    {
        String key = "m." + name;
        return _msgs.exists(key) ? _msgs.get(key) : name;
    }

    /**
     * Returns the label for the specified class.
     */
    protected String getLabel (Class<?> clazz, String type)
    {
        MessageBundle msgs = _msgmgr.getBundle(Introspector.getMessageBundle(clazz));
        String key = "m." + type;
        return msgs.exists(key) ? msgs.get(key) : type;
    }

    /**
     * Shows a frame slightly offset from this one.
     */
    protected void showFrame (JFrame frame)
    {
        frame.setLocation(getX() + 16, getY() + 16);
        frame.setVisible(true);
    }

    /**
     * Adds the find functionality to a menu.
     */
    protected void addFindMenu (JMenu menu)
    {
        menu.addSeparator();
        menu.add(new JMenuItem(_find = createAction("find", KeyEvent.VK_F, KeyEvent.VK_F)));
        menu.add(new JMenuItem(_findNext = createAction(
                        "find_next", KeyEvent.VK_N, KeyEvent.VK_F3, 0)));
    }

    /**
     * Returns the editor panel we'll be finding on.
     */
    protected BaseEditorPanel getFindEditorPanel ()
    {
        return null;
    }

    /** The resource manager. */
    protected ResourceManager _rsrcmgr;

    /** The message manager. */
    protected MessageManager _msgmgr;

    /** The config manager. */
    protected ConfigManager _cfgmgr;

    /** The color pository. */
    protected ColorPository _colorpos;

    /** The config message bundle. */
    protected MessageBundle _msgs;

    /** The editable preferences object. */
    protected ToolUtil.EditablePrefs _eprefs;

    /** The preferences dialog. */
    protected JDialog _pdialog;

    /** The find dialog. */
    protected FindDialog _fdialog;

    /** The find menu actions. */
    protected Action _find, _findNext;

    /** The package preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(BaseConfigEditor.class);
}
