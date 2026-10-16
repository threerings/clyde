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

package com.threerings.tudey.data.actor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.ImmutableSortedMap;

import com.threerings.editor.Property;

import static com.threerings.tudey.Log.log;

/**
 * An actor flag as configs name it, by the name of its {@link ActorFlag} constant. Each name has
 * one shared instance, which looks the name up in an actor's own class the first time it sees that
 * class, so FIZZLE is Bullet's flag on a Bullet and OrbitalBullet's on an OrbitalBullet.
 */
public class ActorFlagName
{
  /**
   * Returns the shared instance for the named flag.
   */
  public static ActorFlagName of (String name)
  {
    // computeIfAbsent can lock even when the name is present, and this runs per frame
    ActorFlagName flag = _names.get(name);
    return (flag != null) ? flag : _names.computeIfAbsent(name, ActorFlagName::new);
  }

  /**
   * Determines whether a config may name the specified flag: whether the name is empty (no flag)
   * or one of the actor classes registered for the editor has the flag (see {@link ActorFlag}).
   */
  public static boolean isValid (String name)
  {
    return name.isEmpty() || Known.DECLARERS.containsKey(name);
  }

  /**
   * Returns the flag names for an editor choice: the known flags, preceded by the current name if
   * it isn't one of them, so that the editor shows the actual value.
   */
  public static String[] getEditorOptions (String current)
  {
    List<String> options = new ArrayList<>();
    if (!Known.DECLARERS.containsKey(current)) {
      options.add(current);
    }
    options.addAll(Known.DECLARERS.keySet());
    return options.toArray(new String[options.size()]);
  }

  /**
   * Formats a flag name for an editor choice, noting the classes that declare it.
   */
  public static String formatEditorOption (String name)
  {
    if (name == null || name.isEmpty()) {
      return "(none)";
    }
    String declarers = Known.DECLARERS.get(name);
    return name + " (" + ((declarers == null) ? "unknown" : declarers) + ")";
  }

  /**
   * Determines whether this flag is set on the actor, which it isn't if the actor's class has no
   * such flag.
   */
  public boolean isSet (Actor actor)
  {
    return actor.isSet(getFlag(actor.getClass()));
  }

  /**
   * Sets or clears this flag on the actor.
   *
   * @return false if the actor's class has no such flag.
   */
  public boolean set (Actor actor, boolean value)
  {
    int flag = getFlag(actor.getClass());
    if (flag == 0) {
      return false;
    }
    actor.set(flag, value);
    return true;
  }

  /**
   * Returns this flag's value in the specified class, or 0 if the class has no such flag.
   */
  public int getFlag (Class<? extends Actor> clazz)
  {
    return _flags.get(clazz);
  }

  @Override
  public String toString ()
  {
    return _name;
  }

  /**
   * Creates the instance for the named flag. Use {@link #of}.
   */
  protected ActorFlagName (String name)
  {
    _name = name;
  }

  /**
   * Finds this flag's value in the specified class, warning if the class has no such flag.
   */
  protected int resolve (Class<?> clazz)
  {
    try {
      Field field = clazz.getField(_name);
      if (isFlag(field)) {
        return field.getInt(null);
      }
    } catch (NoSuchFieldException e) {
      // warned below
    } catch (IllegalAccessException e) {
      log.warning("Actor flag isn't accessible.", "flag", _name, "class", clazz.getName(), e);
      return 0;
    }
    log.warning("Actor class has no such flag.", "flag", _name, "class", clazz.getName());
    return 0;
  }

  /**
   * Determines whether the field is a flag constant.
   */
  protected static boolean isFlag (Field field)
  {
    int mods = field.getModifiers();
    return Modifier.isPublic(mods) && Modifier.isStatic(mods) &&
      field.getType() == Integer.TYPE && field.isAnnotationPresent(ActorFlag.class);
  }

  /**
   * Finds the flags of the actor classes registered for the editor.
   *
   * @return the simple names of the classes that declare each flag, by flag name.
   */
  protected static ImmutableSortedMap<String, String> findDeclarers ()
  {
    Map<String, Set<String>> declarers = new TreeMap<>();
    Set<Class<?>> seen = new HashSet<>();
    for (Class<?> clazz : Property.getSubtypesOf(Actor.class)) {
      // a seen class's superclasses were seen with it
      for (Class<?> cc = clazz; cc != null && seen.add(cc); cc = cc.getSuperclass()) {
        for (Field field : cc.getDeclaredFields()) {
          if (isFlag(field)) {
            declarers.computeIfAbsent(field.getName(), name -> new TreeSet<>())
              .add(cc.getSimpleName());
          }
        }
      }
    }
    ImmutableSortedMap.Builder<String, String> builder = ImmutableSortedMap.naturalOrder();
    declarers.forEach((name, classes) -> builder.put(name, String.join(", ", classes)));
    return builder.build();
  }

  /**
   * The flags that the editor and validator know, found on first use.
   */
  protected static class Known
  {
    /** The simple names of the classes that declare each flag, by flag name. */
    public static final ImmutableSortedMap<String, String> DECLARERS = findDeclarers();
  }

  /** The name of the flag's constant. */
  protected final String _name;

  /** The flag's value in each actor class, or 0 in those that have no such flag. */
  protected final ClassValue<Integer> _flags = new ClassValue<>() {
    @Override protected Integer computeValue (Class<?> clazz) {
      return resolve(clazz);
    }
  };

  /** The shared instances, by name. */
  protected static final ConcurrentMap<String, ActorFlagName> _names = new ConcurrentHashMap<>();
}
