package com.threerings.util;

import com.samskivert.util.RunQueue;

/**
 * A context that provides access to a RunQueue.
 */
public interface RunQueueContext
{
  /**
   * Get the RunQueue that is our "main event loop".
   */
  public RunQueue getRunQueue ();

  /**
   * Post a runnable to our RunQueue.
   */
  public default void postRunnable (Runnable r)
  {
    getRunQueue().postRunnable(r);
  }

  /**
   * Run the runnable immediately if called from the RunQueue thread, otherwise post it.
   */
  public default void runOnRunQueue (Runnable r)
  {
    var rq = getRunQueue();
    if (rq.isDispatchThread()) r.run();
    else rq.postRunnable(r);
  }
}
