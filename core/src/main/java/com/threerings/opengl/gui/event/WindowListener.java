package com.threerings.opengl.gui.event;

/**
 * Dispatches a Window removed event.
 */
public interface WindowListener extends ComponentListener
{
  /**
   * The window that is the source of the event has been removed.
   */
  public void windowRemoved (WindowEvent event);
}
