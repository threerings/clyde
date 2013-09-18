//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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
