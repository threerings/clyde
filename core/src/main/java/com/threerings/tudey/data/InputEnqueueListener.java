package com.threerings.tudey.data;

import com.threerings.presents.dobj.ChangeListener;

public interface InputEnqueueListener extends ChangeListener
{
  void enqueueInput (InputEnqueueEvent event);
}
