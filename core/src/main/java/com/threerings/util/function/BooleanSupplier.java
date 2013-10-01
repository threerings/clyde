//
// $Id$

package com.threerings.util.function;

/**
 * Supplies booleans.
 *
 * When we transition to Java 8:
 * <ul>
 *   <li>Uses of this interface in clyde will change to java.util.function.BooleanSupplier directly.
 *   <li>This interface will extend java.util.function.BooleanSupplier.
 *   <li>This interface will be deprecated.
 * </ul>
 * That will keep everything humming.
 */
public interface BooleanSupplier
{
    /**
     * Returns a boolean value.
     */
    public boolean getAsBoolean ();
}
