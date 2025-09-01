package com.threerings.editor.swing.editors.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A list of recent directories that can be installed on a JFileChooser as its accessory.
 */
public class RecentDirectoryList extends AbstractRecentList
{
  /**
   * Construct with the specified (non-null) prefsKey.
   */
  public RecentDirectoryList (String prefsKey)
  {
    super(prefsKey, Preferences.userNodeForPackage(RecentDirectoryList.class)
        .node("RecentDirectoryList"));
    add(new JLabel("Recent Dirs"), BorderLayout.NORTH);
    setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
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

    // find our chooser and configure it
    for (Component c = this; ((c = c.getParent()) != null); ) {
      if (c instanceof JFileChooser) {
        configureChooser((JFileChooser)c);
        break;
      }
    }
  }

  @Override
  public void removeNotify ()
  {
    unconfigureChooser();
    super.removeNotify();
  }

  @Override
  protected int getMaximumChop (String value)
  {
    return value.lastIndexOf(File.separatorChar) + 1;
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

  /**
   * Configure a JFileChooser that we've been added to.
   */
  protected void configureChooser (JFileChooser chooser)
  {
    _chooser = chooser;

    // tweak the size of the chooser
    _chooser.setPreferredSize(null); // reset preferred size
    Dimension dialogSize = _chooser.getPreferredSize();
    Dimension accessorySize = this.getPreferredSize();
    // the L&Fs I've seen arrange the accessory on the left or right side.
    // accommodate this space. (Why doesn't the layout manager of the chooser do this?)
    dialogSize.width += accessorySize.width;
    dialogSize.height = Math.max(dialogSize.height, accessorySize.height);
    _chooser.setPreferredSize(dialogSize);

    _chooser.addActionListener(_actionListener);
    readPrefs();

    // add the current directory as a recent without saving it
    String theKey = _prefKey;
    _prefKey = null;
    try {
      addRecent(_chooser.getCurrentDirectory());
    } finally {
      _prefKey = theKey;
    }
  }

  /**
   * Remove ourselves as a listener of our JFileChooser.
   */
  protected void unconfigureChooser ()
  {
    if (_chooser != null) {
      _chooser.removeActionListener(_actionListener);
      _chooser = null;
    }
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

  /** The number of pixels bordering our component. */
  protected static final int BORDER = 8;
}
