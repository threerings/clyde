package com.threerings.config.tools;

import java.util.EnumSet;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import com.threerings.export.Exporter;

import com.threerings.tudey.config.ActionConfig;
import com.threerings.tudey.shape.config.ShapeConfig;

/**
 * Standard Exporter.Replacers that may be of interest.
 */
public enum ExporterReplacers
  implements Exporter.Replacer
{
  /** Turn single-shape compound shapes into the new TransformedShape. */
  COMPOUND_SHAPE_CONFIG {
    @Override
    public Exporter.Replacement getReplacement (Object value, Class<?> clazz)
    {
      if ((value instanceof ShapeConfig.Compound) && (clazz != ShapeConfig.Compound.class)) {
        ShapeConfig.Compound cc = (ShapeConfig.Compound)value;
        if (cc.shapes.length == 1) {
          ShapeConfig.TransformedShape ts = cc.shapes[0];
          if (ts.transform.equals(new ShapeConfig.TransformedShape().transform)) {
            return new Exporter.Replacement(ts.shape, clazz);

          } else {
            ShapeConfig.Transformed repl = new ShapeConfig.Transformed();
            repl.shape = ts.shape;
            repl.transform = ts.transform;
            return new Exporter.Replacement(repl, clazz);
          }
        }
      }
      return null;
    }
  },

  COMPOUND_ACTION_CONFIG {
    @Override
    public Exporter.Replacement getReplacement (Object value, Class<?> clazz)
    {
      if ((value instanceof ActionConfig.Compound) &&
          (clazz != ActionConfig.Compound.class)) {
        ActionConfig.Compound ac = (ActionConfig.Compound)value;
        switch (ac.actions.length) {
        case 0: return new Exporter.Replacement(new ActionConfig.None(), clazz);
        case 1: return new Exporter.Replacement(ac.actions[0], clazz);
        default: break;
        }
      }
      return null;
    }
  },
  ;

  /**
   * Get all the standard replacers, combined.
   */
  public static Exporter.Replacer getAll ()
  {
    return compound(EnumSet.allOf(ExporterReplacers.class));
  }

  /**
   * Compound replacers.
   */
  public static Exporter.Replacer compound (Iterable<? extends Exporter.Replacer> itr)
  {
    final ImmutableList<Exporter.Replacer> replacers = ImmutableList.copyOf(itr);
    return new Exporter.Replacer() {
        public Exporter.Replacement getReplacement (Object value, Class<?> clazz)
        {
          for (Exporter.Replacer replacer : replacers) {
            Exporter.Replacement repl = replacer.getReplacement(value, clazz);
            if (repl != null) {
              // here's where it gets funky, we want the replacement to be replaceable
              Exporter.Replacement replRepl = getReplacement(repl.value, repl.clazz);
              return MoreObjects.firstNonNull(replRepl, repl);
            }
          }
          return null;
        }
      };
  }
}
