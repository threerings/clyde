//
// $Id$

package com.threerings.config.tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;
import com.threerings.util.ToolUtil;

/**
 * The superclass of {@link ConfigEditor} and {@link ConfigResourceEditor}.
 */
public abstract class BaseConfigEditor extends JFrame
    implements ActionListener
{
    /**
     * Creates a new config editor.
     */
    public BaseConfigEditor (
        ResourceManager rsrcmgr, MessageManager msgmgr, String msgs, boolean standalone)
    {
        _rsrcmgr = rsrcmgr;
        _msgmgr = msgmgr;
        _msgs = _msgmgr.getBundle(msgs);
        _standalone = standalone;

        setTitle(_msgs.get("m.title"));

        // shutdown when the window is closed
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing (WindowEvent event) {
                BaseConfigEditor.this.windowClosing();
            }
        });
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getActionCommand();
        if (action.equals("quit") || action.equals("close")) {
            windowClosing();
        }
    }

    /**
     * Called when the user closes the window to give the editor a chance to pop up a confirm
     * dialog.
     */
    protected void windowClosing ()
    {
        if (_standalone) {
            System.exit(0);
        } else {
            setVisible(false);
        }
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

    /** The resource manager. */
    protected ResourceManager _rsrcmgr;

    /** The message manager. */
    protected MessageManager _msgmgr;

    /** The config message bundle. */
    protected MessageBundle _msgs;

    /** Whether or not we're running as a standalone application. */
    protected boolean _standalone;
}
