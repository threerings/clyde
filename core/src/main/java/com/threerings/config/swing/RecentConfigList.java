//
// $Id$

package com.threerings.config.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import java.util.prefs.Preferences;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.google.common.base.Splitter;

import com.samskivert.util.ObserverList;

import com.threerings.editor.swing.editors.util.AbstractRecentList;

import com.threerings.config.ConfigReference;
import com.threerings.config.ManagedConfig;

/**
 * Displays and saves recent configs.
 */
public class RecentConfigList extends AbstractRecentList
{
  /**
   * Observer class for RecentConfigList.
   */
  public static abstract class Observer
  {
    /**
     * Called when a config is selected from the list.
     */
    public void configSelected (ConfigReference<?> ref) {}
  }

  /**
   * Create a RecentConfigList.
   *
   * @param prefKey a unique String to identify the context in which this is being used,
   * or null to not persist.
   */
  public RecentConfigList (String prefKey)
  {
    super(prefKey, (prefKey == null)
        ? null
        : Preferences.userNodeForPackage(RecentConfigList.class).node("RecentConfigList"));
  }

  /**
   * Add an Observer of this RecentConfigList.
   */
  public void addObserver (Observer obs)
  {
    _observers.add(obs);
  }

  /**
   * Remove an Observer of this RecentConfigList.
   */
  public void removeObserver (Observer obs)
  {
    _observers.remove(obs);
  }

  /**
   * Add a config reference that's been used recently.
   */
  public void addRecent (ConfigReference<?> ref)
  {
    addRecent(ref.getName());
  }

  @Override
  protected int getMaximumChop (String value)
  {
    return value.lastIndexOf('/') + 1;
  }

  @Override
  protected void valueSelected (String value)
  {
    final ConfigReference<?> ref = new ConfigReference<ManagedConfig>(value);
    _observers.apply(new ObserverList.ObserverOp<Observer>() {
      public boolean apply (Observer obs) {
        obs.configSelected(ref);
        return true;
      }
    });
  }

  /** The observers of this recent list. */
  protected ObserverList<Observer> _observers = ObserverList.newFastUnsafe();
}
