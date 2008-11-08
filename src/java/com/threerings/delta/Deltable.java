//
// $Id$

package com.threerings.delta;

/**
 * Flags a class as being deltable, meaning that it supports the creation of delta objects that
 * compactly represent a set of changes that may be applied to an original object in order to
 * create an updated object.  Deltable classes must have a no-argument constructor.
 *
 * <p> All non-<code>transient</code> fields will be automatically included in the delta
 * generation process for a {@link Deltable} instance. Classes that wish to customize the delta
 * process should implement a method with the following signature:
 *
 * <p><code>
 * public Delta createDelta ({@link Object} revised);
 * </code>
 *
 * <p> This method should return a {@link Delta} instance that, when applied to an object identical
 * to the object whose <code>createDelta</code> method was called, will return an object identical
 * to <code>revised</code>.
 */
public interface Deltable
{
}
