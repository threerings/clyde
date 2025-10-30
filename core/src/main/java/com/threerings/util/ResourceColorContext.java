package com.threerings.util;

import com.threerings.media.image.ColorPository;

/**
 * Provides extended resource servics.
 */
public interface ResourceColorContext extends ResourceContext
{
  /**
   * Returns a reference to the color pository.
   */
  public ColorPository getColorPository ();
}
