package com.threerings.config;

import java.util.Arrays;

import com.threerings.editor.Editable;

/**
 * Implemented by ManagedConfig types that wish to be identified by int ids.
 */
public interface IdConfig
{
  /**
   * Set the id for this config, or 0 to indicate that it doesn't have an id.
   */
  @Editable(constant=true, width=11, weight=-1000, hgroup="top", units="i.config_id")
  public void setConfigId (int id);

  /**
   * Get the id for this config, or 0 if it doesn't have an id.
   */
  @Editable // see setter
  public int getConfigId ();

  /**
   * Do we have an id?
   */
  public default boolean hasConfigId ()
  {
    return getConfigId() != 0;
  }

  /**
   * Should we assign an id to this config, do we think? Parameters named with a leading
   * underscore are per-instance arguments: the config is complete without them.
   */
  public default boolean shouldAssignId ()
  {
    return !(this instanceof ParameterizedConfig pcfg) ||
      Arrays.stream(pcfg.parameters).allMatch(param -> param.name.startsWith("_"));
  }
}
