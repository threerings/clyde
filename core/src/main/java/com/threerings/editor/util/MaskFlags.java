package com.threerings.editor.util;

import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Config;
import com.samskivert.util.ListUtil;

/**
 * The named bits of each bitmask mode, from {@code rsrc/config/editor/mask}, as edited with
 * {@code @Editable(editor="mask", mode=...)}.
 *
 * Kept apart from the Swing editor so that game data can look up its flags in constant
 * initializers without loading the editors, which may themselves load that game data.
 */
public class MaskFlags
{
  /**
   * Retrieves the value of a flag, or zero if not found.
   */
  public static int getFlag (String mode, String flag)
  {
    String[] flags = _modes.get(mode);
    int idx = (flags == null) ? -1 : ListUtil.indexOf(flags, flag);
    return (idx == -1) ? 0 : (1 << idx);
  }

  /**
   * Returns the names of a mode's flags, by bit index, or null if the mode is unknown. Unused bits
   * have blank names.
   */
  public static String[] getFlags (String mode)
  {
    String[] flags = _modes.get(mode);
    return (flags == null) ? null : flags.clone();
  }

  /**
   * Turn the bits into a string of arbitrary format, for debugging. Do not try to reparse this.
   *
   * @return {@code "<none>"}, or the names of the flags that are on, separated by comma and space.
   * Leftover bits are also reported.
   */
  public static String toString (String mode, int bits)
  {
    if (bits == 0) {
      return "<none>";
    }
    String[] flags = _modes.get(mode);
    StringBuilder buf = new StringBuilder();
    for (int ii = 0, nn = flags.length; bits != 0 && ii < nn; ii++) {
      if ((bits & 1) == 1) {
        if (buf.length() > 0) {
          buf.append(", ");
        }
        buf.append(flags[ii]);
      }
      bits >>>= 1;
    }
    if (bits != 0) {
      buf.append(" _leftover-bits-").append(bits << flags.length);
    }
    return buf.toString();
  }

  /** The names of the flags for each mode. */
  protected static final Map<String, String[]> _modes = loadModes();

  /**
   * Loads the modes from the configuration.
   */
  protected static Map<String, String[]> loadModes ()
  {
    ImmutableMap.Builder<String, String[]> modes = ImmutableMap.builder();
    Config config = new Config("/rsrc/config/editor/mask");
    for (Iterator<String> it = config.keys(); it.hasNext(); ) {
      String key = it.next();
      modes.put(key, config.getValue(key, ArrayUtil.EMPTY_STRING));
    }
    return modes.build();
  }
}
