//
// $Id$

package com.threerings.config.tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

import com.threerings.editor.swing.EditorPanel;
import com.threerings.editor.util.EditorContext;

import com.threerings.config.ConfigManager;

/**
 * The superclass of {@link ConfigEditor} and {@link ConfigResourceEditor}.
 */
public abstract class BaseConfigEditor extends JFrame
    implements EditorContext, ActionListener
{
    /**
     * Utility method to create an editor for the identified config.
     */
    public static BaseConfigEditor createEditor (EditorContext ctx, Class clazz, String name)
    {
        MessageManager msgmgr = ctx.getMessageManager();
        ConfigManager cfgmgr = ctx.getConfigManager();
        ColorPository colorpos = ctx.getColorPository();
        if (cfgmgr.isResourceClass(clazz)) {
            return new ResourceEditor(
                msgmgr, cfgmgr.getRoot(), colorpos,
                ctx.getResourceManager().getResourceFile(name).toString());
        } else {
            return new ConfigEditor(msgmgr, cfgmgr, colorpos, clazz, name);
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

        setTitle(_msgs.get("m.title"));

        // dispose when the window is closed
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // create and init the editable prefs, which also (re)sets the resource directory
        _eprefs = new ToolUtil.EditablePrefs(_prefs);
        _eprefs.init(_rsrcmgr);

        // initialize the configuration manager if not yet initialized
        if (!cfgmgr.isInitialized()) {
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
        }
    }

    @Override // documentation inherited
    public void addNotify ()
    {
        super.addNotify();
        ToolUtil.windowAdded();
    }

    @Override // documentation inherited
    public void removeNotify ()
    {
        super.removeNotify();
        ToolUtil.windowRemoved();
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
     * Shows a frame slightly offset from this one.
     */
    protected void showFrame (JFrame frame)
    {
        frame.setLocation(getX() + 16, getY() + 16);
        frame.setVisible(true);
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

    /** The package preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(BaseConfigEditor.class);
}
