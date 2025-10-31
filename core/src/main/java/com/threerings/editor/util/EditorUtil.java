package com.threerings.editor.util;

import java.util.Arrays;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * Stuff that could one day become default methods on EditorContext perhaps.
 */
public class EditorUtil
{
  /**
   * Filter the list of selectable types.
   */
  public static Class<?>[] filterTypes (EditorContext ctx, Class<?>[] types)
  {
    Predicate<Class<?>> pred = ctx.getTypeFilter();
    return pred == null ? types
      : Iterables.toArray(Iterables.filter(Arrays.asList(types), pred), Class.class);
  }
}
