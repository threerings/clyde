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

package com.threerings.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.HashMap;
import java.util.Map;

import sun.reflect.ReflectionFactory;

import static com.threerings.ClydeLog.log;

/**
 * Some general reflection utility methods.
 */
public class ReflectionUtil
{
  /**
   * Creates a new instance of the named class.
   *
   * @param classname the name of the class to instantiate.
   */
  public static Object newInstance (String classname)
  {
    return newInstance(classname, null);
  }

  /**
   * Creates a new instance of the named inner class.
   *
   * @param classname the name of the class to instantiate.
   * @param outer an instance of the enclosing class.
   */
  public static Object newInstance (String classname, Object outer)
  {
    try {
      return newInstance(Class.forName(classname), outer);
    } catch (Exception e) {
      log.warning("Failed to get class by name [class=" + classname + "].", e);
      return null;
    }
  }

  /**
   * Creates a new instance of the specified class.
   *
   * @param clazz the class to instantiate.
   */
  public static Object newInstance (Class<?> clazz)
  {
    return newInstance(clazz, null);
  }

  /**
   * Creates a new instance of the specified (possibly inner) class.
   *
   * @param clazz the class to instantiate.
   * @param outer for inner classes, a reference to the enclosing instance (otherwise
   * <code>null</code>).
   * @return the newly created object, or <code>null</code> if there was some error
   * (in which case a message will be logged).
   */
  public static Object newInstance (Class<?> clazz, Object outer)
  {
    return getClassInfo(clazz).newInstance(outer);
  }

  /**
   * Sets an inner object's outer class reference if it has one.
   */
  public static void setOuter (Object object, Object outer)
  {
    if (object instanceof Inner inner) inner.setOuter(outer);
    else getClassInfo(object.getClass()).setOuter(object, outer);
  }

  /**
   * Returns a reference to an inner object's outer class reference, or <code>null</code> if
   * the object represents an instance of a static class.
   */
  public static Object getOuter (Object object)
  {
    return object instanceof Inner inner ? inner.getOuter()
      : getClassInfo(object.getClass()).getOuter(object);
  }

  /**
   * Determines whether the specified class is a non-static inner class.
   */
  public static boolean isInner (Class<?> clazz)
  {
    return null != getOuterClass(clazz);
  }

  /**
   * Returns the outer class for the given inner class (or <code>null</code> if not an inner
   * class).
   */
  public static Class<?> getOuterClass (Class<?> clazz)
  {
    return getClassInfo(clazz).outerClazz;
  }

  /**
   * Get the class info for the specified class. Never fails.
   */
  protected static ClassInfo getClassInfo (Class<?> clazz)
  {
    var info = _infos.get(clazz);
    if (info == null) _infos.put(clazz, info = new ClassInfo(clazz));
    return info;
  }

  protected static class ClassInfo
  {
    public final Class<?> clazz;

    public final Constructor<?> constructor;

    public final Class<?> outerClazz;

    public final Field outerField;

    private final Object syntheticOuter;

    public ClassInfo (Class<?> clazz)
    {
      Constructor<?> ctor = null;
      Class<?> oclazz = null;
      Field field = null;
      Object syntheticOuter = null;

      if (Inner.class.isAssignableFrom(clazz)) {
        for (Constructor<?> cc : clazz.getDeclaredConstructors()) {
          Class<?>[] ptypes = cc.getParameterTypes();
          if (ptypes.length > 0) { // Guess
            oclazz = ptypes[0];
            ctor = cc;
            break;
          }
        }
      } else {
        Class<?> dclazz = clazz.getDeclaringClass();
        if (dclazz != null) {
          if (!Modifier.isStatic(clazz.getModifiers())) oclazz = dclazz;

        } else if (clazz.getEnclosingClass() instanceof Class<?> eclazz) {
          log.warning("You might not have good success with anonymous inner classes pal!",
            "clazz", clazz, "eclazz", eclazz);
        }

        for (Class<?> ocl = clazz; ocl != null; ocl = ocl.getSuperclass()) {
          for (Constructor<?> cc : ocl.getDeclaredConstructors()) {
            Class<?>[] ptypes = cc.getParameterTypes();
            if (oclazz == null ? ptypes.length == 0
                : (ptypes.length == 1 && ptypes[0] == oclazz)) {
              ctor = cc;
              ctor.setAccessible(true);
              break;
            }
          }
          if (ctor == null) continue;
          if (oclazz != null) { // try to find the field?
            for (Field ff : ocl.getDeclaredFields()) {
              if (ff.isSynthetic() && ff.getType() == oclazz && ff.getName().startsWith("this")) {
                field = ff;
                field.setAccessible(true);
                break;
              }
            }
            // if we never find the field, maybe compiler erased it. We need to cope.
            if (field == null) {
              try {
                syntheticOuter = ReflectionFactory.getReflectionFactory()
                  .newConstructorForSerialization(oclazz, Object.class.getDeclaredConstructor())
                  .newInstance();
              } catch (Exception e) {
                log.warning("Could not make synthetic outer", "clazz", clazz, e);
              }
            }
          }
          break;
        }
      }

      this.clazz = clazz;
      this.constructor = ctor;
      this.outerClazz = oclazz;
      this.outerField = field;
      this.syntheticOuter = syntheticOuter;
    }

    /**
     * Create a new instance.
     */
    public Object newInstance (Object outer)
    {
      try {
        if (outerClazz == null) return constructor.newInstance();
        if (outer == null && syntheticOuter != null) outer = syntheticOuter;
        return constructor.newInstance(outer);
      } catch (Exception e) {
        log.warning("Failed to create new instance.", "class", clazz, "outer", outerClazz, e);
        return null;
      }
    }

    public void setOuter (Object object, Object outer)
    {
      // if not outer or outer field missing, skip
      if (outerField != null) { // As of Java 22 or so, the compiler might erase the field. Cope.
        try {
          outerField.set(object, outer);
        } catch (IllegalAccessException e) {
          // shouldn't happen
        }
      }
    }

    public Object getOuter (Object object)
    {
      // whether or not the class is inner or not is somewhat irrelevant, as we may not be able
      // to find an outer anyway. Just see if we have the field and try it
      if (outerField != null) {
        try {
          return outerField.get(object);
        } catch (IllegalAccessException e) {
          // shouldn't happen
        }
      }
      return null;
    }
  }

  /** Maps class to memoized information regarding it. */
  protected static final Map<Class<?>, ClassInfo> _infos = new HashMap<>();
}
