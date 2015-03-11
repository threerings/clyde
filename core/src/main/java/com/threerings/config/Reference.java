//
// $Id$

package com.threerings.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a bare String as being a ConfigReference with no arguments.
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
