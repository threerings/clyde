//
// $Id$

package com.threerings.util.function;

import java.util.Collection;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * Collects elements.
 *
 * When we transition to Java 8:
 * <ul>
 *   <li>Uses of this interface in clyde will change to use java.util.function.Consumer directly.
 *   <li>This interface will extend java.util.function.Consumer.
 *   <li>This interface will be deprecated.
 * </ul>
 * That will keep everything humming.
 */
public interface Consumer<T>
{
    /**
     * A repository of common methods, since we cannot put static methods on an interface pre-JDK8.
     */
    public static class Common
    {
        private Common () {} // uninstantiable

        /**
         * Return a Consumer that populates the specified Collection.
         */
        public static <T> Consumer<T> adapt (final Collection<? super T> collection)
        {
            return new Consumer<T>() {
                public void accept (T t) {
                    collection.add(t);
                }
            };
        }

        /**
         * Return a Consumer that transforms elements using the specified function before
         * adding them to the other consumer.
         */
        public static <T, A> Consumer<T> transform (
                final Consumer<A> consumer, final Function<? super T, ? extends A> func)
        {
            return new Consumer<T>() {
                public void accept (T t) {
                    consumer.accept(func.apply(t));
                }
            };
        }

        /**
         * Return a Consumer that filters elements using the specified predicate before
         * adding them to the other consumer.
         */
        public static <T> Consumer<T> filter (
                final Consumer<T> consumer, final Predicate<? super T> pred)
        {
            return new Consumer<T>() {
                public void accept (T t) {
                    if (pred.apply(t)) {
                        consumer.accept(t);
                    }
                }
            };
        }
    }

    /**
     * Accept an input value.
     */
    public void accept (T t);
}
