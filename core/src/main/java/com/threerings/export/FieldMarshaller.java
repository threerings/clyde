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

import java.io.IOException;

import java.lang.reflect.Field;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * Used to read and write individual fields.
 */
public abstract class FieldMarshaller
{
    /**
     * Retrieves a field marshaller for the specified field.
     */
    public static FieldMarshaller getFieldMarshaller (Field field)
    {
        // check for a type-specific marshaller
        FieldMarshaller marshaller = MARSHALLERS.get(field.getType());
        if (marshaller != null) {
            return marshaller;
        }

        // otherwise, just return the generic one
        return MARSHALLERS.get(Object.class);
    }

    /**
     * Reads the contents of the supplied field from the supplied importer and sets it in the
     * object.
     */
    public abstract void readField (
        Field field, String name, Object target, Object prototype, Importer importer)
        throws IOException, IllegalAccessException;

    /**
     * Writes the contents of the supplied field in the supplied object to the exporter.
     */
    public abstract void writeField (
        Field field, String name, Object source, Object prototype, Exporter exporter)
        throws IOException, IllegalAccessException;

    /** Field marshallers mapped by class. */
    protected static final Map<Class<?>, FieldMarshaller> MARSHALLERS =
        ImmutableMap.<Class<?>, FieldMarshaller>builder()
            .put(Boolean.TYPE, new FieldMarshaller() {
                public void readField (
                    Field field, String name, Object target, Object prototype, Importer importer)
                        throws IOException, IllegalAccessException {
                    field.setBoolean(target, importer.read(name, field.getBoolean(prototype)));
                }
                public void writeField (
                    Field field, String name, Object source, Object prototype, Exporter exporter)
                        throws IOException, IllegalAccessException {
                    exporter.write(name, field.getBoolean(source), field.getBoolean(prototype));
                }
            })
            .put(Byte.TYPE, new FieldMarshaller() {
                public void readField (
                    Field field, String name, Object target, Object prototype, Importer importer)
                        throws IOException, IllegalAccessException {
                    field.setByte(target, importer.read(name, field.getByte(prototype)));
                }
                public void writeField (
                    Field field, String name, Object source, Object prototype, Exporter exporter)
                        throws IOException, IllegalAccessException {
                    exporter.write(name, field.getByte(source), field.getByte(prototype));
                }
            })
            .put(Character.TYPE, new FieldMarshaller() {
                public void readField (
                    Field field, String name, Object target, Object prototype, Importer importer)
                        throws IOException, IllegalAccessException {
                    field.setChar(target, importer.read(name, field.getChar(prototype)));
                }
                public void writeField (
                    Field field, String name, Object source, Object prototype, Exporter exporter)
                        throws IOException, IllegalAccessException {
                    exporter.write(name, field.getChar(source), field.getChar(prototype));
                }
            })
            .put(Double.TYPE, new FieldMarshaller() {
                public void readField (
                    Field field, String name, Object target, Object prototype, Importer importer)
                        throws IOException, IllegalAccessException {
                    field.setDouble(target, importer.read(name, field.getDouble(prototype)));
                }
                public void writeField (
                    Field field, String name, Object source, Object prototype, Exporter exporter)
                        throws IOException, IllegalAccessException {
                    exporter.write(name, field.getDouble(source), field.getDouble(prototype));
                }
            })
            .put(Float.TYPE, new FieldMarshaller() {
                public void readField (
                    Field field, String name, Object target, Object prototype, Importer importer)
                        throws IOException, IllegalAccessException {
                    field.setFloat(target, importer.read(name, field.getFloat(prototype)));
                }
                public void writeField (
                    Field field, String name, Object source, Object prototype, Exporter exporter)
                        throws IOException, IllegalAccessException {
                    exporter.write(name, field.getFloat(source), field.getFloat(prototype));
                }
            })
            .put(Integer.TYPE, new FieldMarshaller() {
                public void readField (
                    Field field, String name, Object target, Object prototype, Importer importer)
                        throws IOException, IllegalAccessException {
                    field.setInt(target, importer.read(name, field.getInt(prototype)));
                }
                public void writeField (
                    Field field, String name, Object source, Object prototype, Exporter exporter)
                        throws IOException, IllegalAccessException {
                    exporter.write(name, field.getInt(source), field.getInt(prototype));
                }
            })
            .put(Long.TYPE, new FieldMarshaller() {
                public void readField (
                    Field field, String name, Object target, Object prototype, Importer importer)
                        throws IOException, IllegalAccessException {
                    field.setLong(target, importer.read(name, field.getLong(prototype)));
                }
                public void writeField (
                    Field field, String name, Object source, Object prototype, Exporter exporter)
                        throws IOException, IllegalAccessException {
                    exporter.write(name, field.getLong(source), field.getLong(prototype));
                }
            })
            .put(Short.TYPE, new FieldMarshaller() {
                public void readField (
                    Field field, String name, Object target, Object prototype, Importer importer)
                        throws IOException, IllegalAccessException {
                    field.setShort(target, importer.read(name, field.getShort(prototype)));
                }
                public void writeField (
                    Field field, String name, Object source, Object prototype, Exporter exporter)
                        throws IOException, IllegalAccessException {
                    exporter.write(name, field.getShort(source), field.getShort(prototype));
                }
            })
            .put(Object.class, new FieldMarshaller() {
                public void readField (
                    Field field, String name, Object target, Object prototype, Importer importer)
                        throws IOException, IllegalAccessException {
                    // only set the field if it's present; otherwise, we would have to clone the
                    // value of the prototype field
                    Object defvalue = field.get(prototype);
                    Object value = importer.read(name, defvalue, field);
                    if (value != defvalue) {
                        field.set(target, value);
                    }
                }
                public void writeField (
                    Field field, String name, Object source, Object prototype, Exporter exporter)
                        throws IOException, IllegalAccessException {
                    @SuppressWarnings("unchecked") Class<Object> clazz =
                        (Class<Object>)field.getType();
                    exporter.write(name, field.get(source), field.get(prototype), clazz);
                }
            })
            .build();
}
