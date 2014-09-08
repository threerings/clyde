//
// $Id$

package com.threerings.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Contains constraints on ConfigReference fields.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface ReferenceConstraints
{
    /** The translatable description. */
    String description () default "";

    /** The allowable sub-types of the config that the config reference must resolve to. */
    Class<? extends ManagedConfig>[] value ();
}
