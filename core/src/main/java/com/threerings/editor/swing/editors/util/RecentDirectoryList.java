package com.threerings.editor.swing.editors.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A list of recent directories that can be installed on a JFileChooser as its accessory.
 */
public class RecentDirectoryList extends AbstractRecentList
{
    public RecentDirectoryList (String prefsKey)
    {
        super(prefsKey, Preferences.userNodeForPackage(RecentDirectoryList.class)
                .node("RecentDirectoryList"));
        add(new JLabel("Recent Dirs"), BorderLayout.NORTH);
        setPreferredSize(new Dimension(200, 200)); // why this isn't min size, I don't know
    }

    @Override
    protected void valueSelected (String value)
    {
        if (value != null) {
            _chooser.setCurrentDirectory(new File(value));
        }
    }

    @Override
    public void addNotify ()
    {
        super.addNotify();

        for (Component c = this; ((c = c.getParent()) != null); ) {
            if (c instanceof JFileChooser) {
                _chooser = (JFileChooser)c;
                _chooser.addActionListener(_actionListener);
                addRecent(_chooser.getCurrentDirectory());
                break;
            }
        }
    }

    @Override
    public void removeNotify ()
    {
        if (_chooser != null) {
            _chooser.removeActionListener(_actionListener);
            _chooser = null;
        }

        super.removeNotify();
    }

    /**
     * Version of addRecent that takes a file.
     */
    protected void addRecent (File file)
    {
        if (!file.isDirectory()) {
            file = file.getParentFile();
        }
        addRecent(file.getAbsolutePath());
    }

    /** The chooser that we accessorize. */
    protected JFileChooser _chooser;

    /** Our listener. */
    protected ActionListener _actionListener = new ActionListener() {
        public void actionPerformed (ActionEvent event)
        {
            File file = _chooser.getSelectedFile();
            if (file != null) {
                addRecent(file);
            }
        }
    };
}
