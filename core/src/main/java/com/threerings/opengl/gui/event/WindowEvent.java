package com.threerings.opengl.gui.event;

import com.threerings.opengl.gui.Window;

/**
 * An event dispatched for windows.
 */
public class WindowEvent extends Event
{
  /** The type when a window is removed. */
  public static final int WINDOW_REMOVED = 0;

  /** The type of this event. */
  public final int type;

  public WindowEvent (Window source, long when, int type)
  {
    super(source, when);
    this.type = type;
  }

  @Override
  public boolean propagateUpHierarchy ()
  {
    return false;
  }

  @Override
  public void dispatch (ComponentListener listener)
  {
    if (listener instanceof WindowListener) {
      ((WindowListener)listener).windowRemoved(this);
    }
  }
}
