package com.threerings.util;

import java.util.function.Consumer;

public class FunctionUtil
{
  /**
   * Get a singleton no-op consumer that removes itself from the chain when andThen() is called
   * upon it.
   */
  @SuppressWarnings("unchecked")
  public static <T> Consumer<T> getNoopConsumer ()
  {
    return (Consumer<T>)NoopConsumer.INSTANCE;
  }

  /**
   * Implementation: Singleton NoopConsumer.
   */
  enum NoopConsumer implements Consumer<Object>
  {
    INSTANCE;

    @Override public void accept (Object obj) { /* nothing */ }
    @Override public Consumer<Object> andThen (Consumer<Object> next) { return next; }
  }
}
