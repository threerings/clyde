//
// $Id$

package com.threerings.util.function;

/**
 * Extends the Consumer interface to <em>hint</em> that it is done consuming.
 * A CheckedConsumer must not throw any exceptions if more elements are added even after
 * it has started hinting that it doesn't want any more, because that would violate the
 * contract of the super interface, Consumer. It is undefined what will happen if the hint
 * is ignored- presumably the consumer will discard the unwanted elements.
 *
 * The intention is that the hint can be used by entities populating the consumer to know
 * that they can stop iterating. 
 */
public interface CheckedConsumer<T> extends Consumer<T>
{
    /**
     * Contains common utilities for CheckedConsumers, since we cannot have static methods
     * on an interface until JDK8.
     */
    public static class Common
    {
        private Common () {}

        /**
         * Adapt a Consumer into a CheckedConsumer that is always hungry for more elements.
         */
        public static <T> CheckedConsumer adapt (final Consumer<T> consumer)
        {
            return new CheckedConsumer<T>() {
                public void accept (T t) {
                    consumer.accept(t);
                }
                public boolean wantsNext () {
                    return true;
                }
            };
        }
    }

    /**
     * Does this Consumer want the next input value?
     */
    public boolean wantsNext ();
}
