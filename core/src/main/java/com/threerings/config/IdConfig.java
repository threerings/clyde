package com.threerings.config;

/**
 * Implemented by ManagedConfig types that wish to be identified by int ids.
 */
public interface IdConfig
{
  /**
   * Set the id for this config, or 0 to indicate that it doesn't have an id.
   */
  public void setConfigId (int id);

  /**
   * Get the id for this config, or 0 if it doesn't have an id.
   */
  public int getConfigId ();
}
