package com.threerings.config;

import com.threerings.editor.Editable;

public class DerivedIdConfig extends DerivedConfig
  implements IdConfig
{
  @Editable(constant=true, weight=-1, units="i.config_id") // could be made editable ...
  @Override // from IdConfig
  public void setConfigId (int id)
  {
    _configId = id;
  }

  @Editable // see setter
  @Override // from IdConfig
  public int getConfigId ()
  {
    return _configId;
  }

  @Override
  protected void initializeInstance (ManagedConfig instance)
  {
    super.initializeInstance(instance);

    // require that our instances are also IdConfigs
    ((IdConfig)instance).setConfigId(_configId);
  }

  /** The config id for this config. */
  protected int _configId;
}
