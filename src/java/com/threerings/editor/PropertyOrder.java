//
// $Id$

package com.threerings.editor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the order in which properties should be listed.  When present, this overrides the
 * ordering determined by the {@link Editable#weight} attribute.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface PropertyOrder
{
    /** The names of the ordered properties, in the desired order. */
    String[] value () default {};
}
