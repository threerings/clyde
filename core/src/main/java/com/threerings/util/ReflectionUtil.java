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

//import sun.reflect.ReflectionFactory;

import java.util.HashMap;
import java.util.Map;

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

  /**
   * Contains cached information about classes we've been asked to handle.
   */
  protected static class ClassInfo
  {
    /** The class that is the subject of this record. */
    public final Class<?> clazz;

    /** The constructor that we'll use to create instances. */
    public final Constructor<?> constructor;

    /** If non-null, the outer class for the clazz, an instance of which will need to be the
     * first argument to the constructor. */
    public final Class<?> outerClazz;

    /** A field to read/write the outer instance, which may be null even for inner classes! */
    public final Field outerField;

    /** If non-null, indicates that the outerField doesn't exist, and this can be used as a non-null
     * instance provided to the constructor. */
    private final Object fakeOuter;

    /**
     * Construct ClassInfo for a class.
     */
    public ClassInfo (Class<?> clazz)
    {
      Constructor<?> ctor = null;
      Class<?> oclazz = null;
      Field field = null;
      Object fakeOuter = null;

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
          // we've found an outer class
          if (!Modifier.isStatic(clazz.getModifiers())) oclazz = dclazz;

        } else if (clazz.getEnclosingClass() instanceof Class<?> eclazz) {
          // An anonymous inner class technically has an outer but the constructors/references
          // will be unknowable. Warn.
          log.warning("You might not have good success with anonymous inner classes pal!",
            "clazz", clazz, "eclazz", eclazz);
        }

        // Look for the constructor and the field to access the outer value...
FIND_CTOR:
        for (Class<?> ocl = clazz; ocl != null; ocl = ocl.getSuperclass()) {
          for (Constructor<?> cc : ocl.getDeclaredConstructors()) {
            Class<?>[] ptypes = cc.getParameterTypes();
            if (oclazz == null ? ptypes.length == 0
                : (ptypes.length == 1 && ptypes[0] == oclazz)) {
              ctor = cc;
              ctor.setAccessible(true);
              break FIND_CTOR;
            }
          }
        }
        if (oclazz != null) { // try to find the field?
FIND_FIELD:
          for (Class<?> ocl = clazz; ocl != null; ocl = ocl.getSuperclass()) {
            for (Field ff : ocl.getDeclaredFields()) {
              if (ff.isSynthetic() && ff.getType() == oclazz && ff.getName().startsWith("this")) {
                field = ff;
                field.setAccessible(true);
                break FIND_FIELD;
              }
            }
          }
          // if we never find the field, maybe compiler erased it. We need to cope.
          if (field == null) {
            // See if we can find a constructor to make the outer
            try {
              for (Constructor<?> cc : oclazz.getDeclaredConstructors()) {
                if (cc.getParameterTypes().length == 0) {
                  cc.setAccessible(true);
                  fakeOuter = cc.newInstance();
                  log.info("YES. We made a fake outer.", "oclazz", oclazz,
                      "fakeOuter", fakeOuter.getClass());
                  break;
                }
              }
            } catch (Exception e) {
              log.warning("Trouble making an outer", "oclazz", oclazz);
            }
            if (fakeOuter == null) {
              log.warning("Class has erased 'outer', and we're unable to create an instance." +
                  "Consider making static or adding an explicit reference?",
                  "clazz", clazz, "outer", oclazz);
//              try {
//                fakeOuter = ReflectionFactory.getReflectionFactory()
//                  .newConstructorForSerialization(oclazz, Object.class.getDeclaredConstructor())
//                  .newInstance();
//              } catch (Exception e) {
//                log.warning("Could not make a fake outer", "clazz", clazz, e);
//              }
            }
          }
        }
      }

      this.clazz = clazz;
      this.constructor = ctor;
      this.outerClazz = oclazz;
      this.outerField = field;
      this.fakeOuter = fakeOuter;
    }

    /**
     * Create a new instance.
     */
    public Object newInstance (Object outer)
    {
      try {
        if (outerClazz == null) return constructor.newInstance();
        if (outer == null && fakeOuter != null) outer = fakeOuter;
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
