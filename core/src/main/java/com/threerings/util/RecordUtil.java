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

package com.threerings.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;

/**
 * Works with records through only their public API: the accessors of their components and their
 * canonical constructor. Unlike {@link ReflectionUtil}, nothing here gets around access checks,
 * so only public records are supported.
 */
public class RecordUtil
{
  /**
   * Returns the values of the supplied record's components, in declaration order, with
   * primitives boxed.
   *
   * @throws IllegalArgumentException if the record isn't public.
   */
  public static Object[] getValues (Record record)
  {
    Method[] accessors = TYPES.get(record.getClass()).accessors;
    Object[] values = new Object[accessors.length];
    for (int ii = 0; ii < accessors.length; ii++) {
      try {
        values[ii] = accessors[ii].invoke(record);
      } catch (InvocationTargetException ite) {
        throw unchecked(ite);
      } catch (IllegalAccessException iae) {
        throw new AssertionError(iae); // a public record's accessors are public
      }
    }
    return values;
  }

  /**
   * Creates a record of the specified class with the supplied component values, in declaration
   * order, with primitives boxed. Anything thrown by the canonical constructor (such as by the
   * validation in a compact constructor) is passed along as is.
   *
   * @throws IllegalArgumentException if the record isn't public, or the values don't match its
   * components.
   */
  public static <T extends Record> T newInstance (Class<T> clazz, Object[] values)
  {
    try {
      return clazz.cast(TYPES.get(clazz).ctor.newInstance(values));
    } catch (InvocationTargetException ite) {
      throw unchecked(ite);
    } catch (InstantiationException | IllegalAccessException e) {
      throw new AssertionError(e); // a public record's canonical constructor is public
    }
  }

  /**
   * Returns the exception thrown by an accessor or canonical constructor for the caller to
   * rethrow (neither may declare a checked one), or throws it directly if it's an Error.
   */
  protected static RuntimeException unchecked (InvocationTargetException ite)
  {
    Throwable cause = ite.getCause();
    if (cause instanceof Error error) {
      throw error;
    }
    return (cause instanceof RuntimeException rte) ? rte : new RuntimeException(cause);
  }

  /**
   * The accessors and canonical constructor of a record class.
   */
  protected static class RecordType
  {
    /** The accessors of the components, in declaration order. */
    public final Method[] accessors;

    /** The canonical constructor. */
    public final Constructor<?> ctor;

    public RecordType (Class<?> clazz)
    {
      if (!clazz.isRecord()) {
        // a subclass of Record that isn't one has lost its Record attribute, most likely to
        // an obfuscator
        throw new IllegalArgumentException(
          "Class is not a record (was its Record attribute stripped?): " + clazz.getName());
      }
      if (!Modifier.isPublic(clazz.getModifiers())) {
        throw new IllegalArgumentException("Record is not public: " + clazz.getName());
      }
      RecordComponent[] comps = clazz.getRecordComponents();
      accessors = new Method[comps.length];
      Class<?>[] types = new Class<?>[comps.length];
      for (int ii = 0; ii < comps.length; ii++) {
        accessors[ii] = comps[ii].getAccessor();
        if (accessors[ii] == null) {
          // possible if an obfuscator stripped it as unused
          throw new IllegalArgumentException("Record component lacks an accessor [class=" +
            clazz.getName() + ", component=" + comps[ii].getName() + "]");
        }
        types[ii] = comps[ii].getType();
      }
      try {
        ctor = clazz.getConstructor(types);
      } catch (NoSuchMethodException nsme) {
        throw new AssertionError(nsme); // a public record's canonical constructor is public
      }
    }
  }

  /** The record types, computed as needed. */
  protected static final ClassValue<RecordType> TYPES = new ClassValue<RecordType>() {
    @Override protected RecordType computeValue (Class<?> clazz) {
      return new RecordType(clazz);
    }
  };
}
