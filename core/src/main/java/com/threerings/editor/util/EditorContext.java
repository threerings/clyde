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

package com.threerings.editor.util;

import java.util.Arrays;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import com.threerings.util.ResourceColorContext;
import com.threerings.util.RunQueueContext;

import com.threerings.config.ManagedConfig;
import com.threerings.editor.Property;

/**
 * Provides access to the services required by the editor.
 */
public interface EditorContext extends ResourceColorContext, RunQueueContext
{
  /**
   * Get a filter for choosable configs, or null for all.
   */
  public default Predicate<? super ManagedConfig> getChoosingFilter (
    Class<? extends ManagedConfig> clazz)
  {
    return null;
  }

  /**
   * Get a type filter for general selectable types, or null for all.
   */
  public default Predicate<Class<?>> getTypeFilter ()
  {
    return null;
  }

  /**
   * Use the typeFilter to filder the types!
   */
  public default Class<?>[] filterTypes (Class<?>[] types)
  {
    Predicate<Class<?>> pred = getTypeFilter();
    return pred == null ? types
      : Iterables.toArray(Iterables.filter(Arrays.asList(types), pred), Class.class);
  }

  /**
   * Get a property for editing an argument whose parameter can't be resolved, because the
   * configs its paths lead into aren't loaded, or null to leave the argument uneditable.
   */
  public default Property getArgumentHint (Class<? extends ManagedConfig> clazz, String name)
  {
    return null;
  }
}
