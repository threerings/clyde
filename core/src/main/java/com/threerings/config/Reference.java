//
// $Id$

package com.threerings.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a bare config reference, without arguments: a config name (String) or the id (int) of an
 * {@link IdConfig}, alone or in an array or list. The editor picks configs for it with a chooser.
 * A null or blank name, or a zero id, means none.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface Reference
{
  /**
   * The type of the reference.
   */
  Class<? extends ManagedConfig> value ();
}
