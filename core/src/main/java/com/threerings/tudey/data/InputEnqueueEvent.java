package com.threerings.tudey.data;

import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessException;

public class InputEnqueueEvent extends DEvent
{
  /** */
  public int acknowledge;

  /** */
  public int smoothedTime;

  /** */
  public InputFrame[] frames;

  public InputEnqueueEvent (
    int targetOid, int acknowledge, int smoothedTime, InputFrame[] frames)
  {
    super(targetOid);
    this.acknowledge = acknowledge;
    this.smoothedTime = smoothedTime;
    this.frames = frames;
  }

  @Override
  public boolean isPrivate ()
  {
    return true;
  }

  @Override
  public boolean applyToObject (DObject target)
    throws ObjectAccessException
  {
    return true;
  }

  @Override
  protected void notifyListener (Object listener)
  {
    if (listener instanceof InputEnqueueListener lner) lner.enqueueInput(this);
  }
}
