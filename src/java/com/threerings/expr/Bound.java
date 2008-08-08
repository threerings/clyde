//
// $Id$

package com.threerings.expr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Flags a field as being bound to a {@link Scoped} symbol.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface Bound
{
    /** The symbol to which this field is bound, or the empty string if the name is the same as
     * the field name. */
    String value () default "";
}
