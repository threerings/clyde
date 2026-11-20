package com.threerings.config.util;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import com.threerings.config.ConfigManager;
import com.threerings.config.IdConfig;
import com.threerings.config.ManagedConfig;
import com.threerings.config.Reference;

import static com.threerings.ClydeLog.log;

/**
 * Reads the values of {@link Reference} properties: a config name (String) or config id (int),
 * alone or in an array or list.
 */
public class ReferenceUtil
{
  /**
   * Receives the configs that a reference value refers to.
   */
  public interface Visitor
  {
    /**
     * Called with the name of each referenced config.
     */
    void name (String name);

    /**
     * Called with each id that matches no config.
     */
    default void missingId (int id)
    {
      // nothing by default
    }
  }

  /**
   * Visits the configs that a reference value refers to. Blank names and zero ids mean "none"
   * and are skipped.
   */
  public static void visit (
    ConfigManager cfgmgr, Class<? extends ManagedConfig> clazz, Object value, Visitor visitor)
  {
    switch (value) {
      case null -> { }
      case String name -> {
        if (!name.isEmpty()) {
          visitor.name(name);
        }
      }
      case Integer id -> {
        if (id != 0) {
          ManagedConfig config = cfgmgr.getRawConfig(clazz, id);
          if (config == null) {
            visitor.missingId(id);
          } else {
            visitor.name(config.getName());
          }
        }
      }
      case int[] ids -> {
        for (int id : ids) {
          visit(cfgmgr, clazz, id, visitor);
        }
      }
      case Object[] array -> {
        for (Object element : array) {
          visit(cfgmgr, clazz, element, visitor);
        }
      }
      case Collection<?> coll -> {
        for (Object element : coll) {
          visit(cfgmgr, clazz, element, visitor);
        }
      }
      default -> log.warning("TODO: Unsupported @Reference value",
        "class", clazz, "valueType", value.getClass());
    }
  }

  /**
   * Returns the names of the configs that a reference value refers to, skipping missing ids.
   */
  public static List<String> getNames (
    ConfigManager cfgmgr, Class<? extends ManagedConfig> clazz, Object value)
  {
    List<String> names = Lists.newArrayList();
    visit(cfgmgr, clazz, value, names::add);
    return names;
  }

  /**
   * Whether a value of this type holds config ids, rather than names.
   */
  public static boolean holdsIds (Class<?> type)
  {
    Class<?> ctype = type.isArray() ? type.getComponentType() : type;
    return ctype == int.class || ctype == Integer.class;
  }

  /**
   * Returns the id of the named config, or 0 if it has none.
   */
  public static int getId (ConfigManager cfgmgr, Class<? extends ManagedConfig> clazz, String name)
  {
    return (cfgmgr.getRawConfig(clazz, name) instanceof IdConfig idcfg) ? idcfg.getConfigId() : 0;
  }
}
