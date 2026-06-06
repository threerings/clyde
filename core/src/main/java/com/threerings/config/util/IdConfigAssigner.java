package com.threerings.config.util;

import java.io.PrintStream;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.threerings.config.ConfigGroup;
import com.threerings.config.IdConfig;
import com.threerings.config.ManagedConfig;
import com.threerings.config.ParameterizedConfig;

/**
 * Validates and assigns ids to configs in a group.
 */
public class IdConfigAssigner
{
/**
   * Validate ids only.
   */
  public boolean validateIds (ConfigGroup<?> group, PrintStream out)
  {
    return checkIds(group, out, false);
  }

  /**
   * Validate and assign ids.
   */
  public boolean assignIds (ConfigGroup<?> group, PrintStream out)
  {
    return checkIds(group, out, true);
  }

  /**
   * Check ids of the configs in the specified group.
   *
   * @param assign if true, assign ids to configs that need it.
   *
   * Return true if all was successful.
   */
  protected boolean checkIds (ConfigGroup<?> group, PrintStream out, boolean assign)
  {
    if (!IdConfig.class.isAssignableFrom(group.getConfigClass())) return true;

    List<ManagedConfig> needsId = assign
      ? Lists.<ManagedConfig>newArrayList()
      : Collections.<ManagedConfig>emptyList();
    Map<Integer, ManagedConfig> ids = Maps.newHashMap();
    boolean success = true;

    for (ManagedConfig cfg : group.getRawConfigs()) {
      IdConfig idcfg = (IdConfig)cfg;
      if (cfg instanceof ParameterizedConfig) {
        ParameterizedConfig pcfg = (ParameterizedConfig)cfg;
        if (pcfg.parameters.length != 0) {
          if (idcfg.getConfigId() != 0) {
            out.println("Config with id now has parameters! " + group.getName() + ": " +
                        cfg.getName());
            success = false;
          }
          continue;
        }
        // if the parameters are 0-length, then it's ok for assigning...
      }
      int id = idcfg.getConfigId();
      if (id == 0) {
        if (assign) {
          needsId.add(cfg);

        } else {
          out.println("Config needs id " + group.getName() + ": " + cfg.getName());
          success = false;
        }

      } else {
        ManagedConfig oldie = ids.put(id, cfg);
        if (oldie != null) {
          out.println("Duplicate id found! " + group.getName() + ": " + cfg.getName() + " & " +
                      oldie.getName());
          success = false;
        }
      }
    }

    // block-out id 0...
    ids.put(0, null);

    // now assign ids to the configs that need it
    for (ManagedConfig cfg : needsId) {
      int id = cfg.getName().hashCode();
      while (ids.containsKey(id)) {
        id++;
      }
      ((IdConfig)cfg).setConfigId(id);
      cfg.wasUpdated();
      ids.put(id, cfg);
    }
    return success;
  }
}
