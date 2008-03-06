//
// $Id$

package com.threerings.export;

/**
 * Flags a class as being exportable, meaning that it can be written to {@link Exporter}s and read
 * from their corresponding {@link Importer}s.  Exportable classes must have a no-argument
 * constructor that initializes fields to their default values (fields containing default values
 * are omitted from the export).
 *
 * <p> All non-<code>transient</code> fields will be automatically written and restored for an
 * {@link Exportable} instance. Classes that wish to customize the export process should implement
 * methods with the following signatures:
 *
 * <p><code>
 * public void writeFields ({@link Exporter} out)
 *     throws IOException;
 * public void readFields ({@link Importer} in)
 *     throws IOException;
 * </code>
 *
 * <p> They can then handle the entirety of the export process, or call
 * {@link Exporter#defaultWriteFields} and {@link Importer#defaultReadFields} from within their
 * <code>writeFields</code> and <code>readFields</code> methods to perform the standard export
 * in addition to their customized behavior.
 */
public interface Exportable
{
}
