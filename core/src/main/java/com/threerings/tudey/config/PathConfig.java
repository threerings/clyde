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

package com.threerings.tudey.config;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.Strippable;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.util.PreloadableSet;

import com.threerings.tudey.client.cursor.PathCursor;
import com.threerings.tudey.client.sprite.PathSprite;
import com.threerings.tudey.util.TudeyContext;

/**
 * The configuration of a path.
 */
public class PathConfig extends ParameterizedConfig
{
  /** Used when we can't resolve the path's underlying original implementation. */
  public static final Original NULL_ORIGINAL = new Original(Color4f.RED);

  /**
   * Contains the actual implementation of the path.
   */
  @EditorTypes({ Original.class, Derived.class })
  public static abstract class Implementation extends DeepObject
    implements Exportable
  {
    /**
     * Returns a reference to the config's underlying original implementation.
     */
    public abstract Original getOriginal (ConfigManager cfgmgr);

    /**
     * Creates or updates a cursor implementation for this configuration.
     *
     * @param scope the path's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public abstract PathCursor.Implementation getCursorImplementation (
      TudeyContext ctx, Scope scope, PathCursor.Implementation impl);

    /**
     * Creates or updates a sprite implementation for this configuration.
     *
     * @param scope the path's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public abstract PathSprite.Implementation getSpriteImplementation (
      TudeyContext ctx, Scope scope, PathSprite.Implementation impl);

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
      // nothing by default
    }
  }

  /**
   * An original implementation.
   */
  public static class Original extends Implementation
  {
    /** The color to use when showing this path in the scene editor. */
    @Editable(mode="alpha", hgroup="c")
    @Strippable
    public Color4f color = new Color4f();

    /** Whether or not the path should be used as a default entrance. */
    @Editable(hgroup="c")
    @Strippable
    public boolean defaultEntrance;

    /** Tags used to identify the path within the scene. */
    @Editable
    @Strippable
    public TagConfig tags = new TagConfig();

    /** The path's event handlers. */
    @Editable
    public HandlerConfig[] handlers = HandlerConfig.EMPTY_ARRAY;

    /**
     * Default constructor.
     */
    public Original ()
    {
    }

    /**
     * Creates an implementation with the specified color.
     */
    public Original (Color4f color)
    {
      this.color.set(color);
    }

    /**
     * Returns the name of the server-side logic class to use for the path, or
     * <code>null</code> for none.
     */
    public String getLogicClassName ()
    {
      return (tags.getLength() == 0 && handlers.length == 0 && !defaultEntrance) ? null :
        "com.threerings.tudey.server.logic.EntryLogic";
    }

    /**
     * Adds the resources to preload for this path into the provided set.
     */
    public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
    {
      for (HandlerConfig handler : handlers) {
        handler.getPreloads(cfgmgr, preloads);
      }
    }

    @Override
    public Original getOriginal (ConfigManager cfgmgr)
    {
      return this;
    }

    @Override
    public PathCursor.Implementation getCursorImplementation (
      TudeyContext ctx, Scope scope, PathCursor.Implementation impl)
    {
      if (impl instanceof PathCursor.Original) {
        ((PathCursor.Original)impl).setConfig(this);
      } else {
        impl = new PathCursor.Original(ctx, scope, this);
      }
      return impl;
    }

    @Override
    public PathSprite.Implementation getSpriteImplementation (
      TudeyContext ctx, Scope scope, PathSprite.Implementation impl)
    {
      if (!ScopeUtil.resolve(scope, "markersVisible", false)) {
        return null;
      }
      if (impl instanceof PathSprite.Original) {
        ((PathSprite.Original)impl).setConfig(this);
      } else {
        impl = new PathSprite.Original(ctx, scope, this);
      }
      return impl;
    }

    @Override
    public void invalidate ()
    {
      for (HandlerConfig handler : handlers) {
        handler.invalidate();
      }
    }
  }

  /**
   * A derived implementation.
   */
  public static class Derived extends Implementation
  {
    /** The path reference. */
    @Editable(nullable=true)
    public ConfigReference<PathConfig> path;

    @Override
    public Original getOriginal (ConfigManager cfgmgr)
    {
      PathConfig config = cfgmgr.getConfig(PathConfig.class, path);
      return (config == null) ? null : config.getOriginal(cfgmgr);
    }

    @Override
    public PathCursor.Implementation getCursorImplementation (
      TudeyContext ctx, Scope scope, PathCursor.Implementation impl)
    {
      PathConfig config = ctx.getConfigManager().getConfig(PathConfig.class, path);
      return (config == null) ? null : config.getCursorImplementation(ctx, scope, impl);
    }

    @Override
    public PathSprite.Implementation getSpriteImplementation (
      TudeyContext ctx, Scope scope, PathSprite.Implementation impl)
    {
      PathConfig config = ctx.getConfigManager().getConfig(PathConfig.class, path);
      return (config == null) ? null : config.getSpriteImplementation(ctx, scope, impl);
    }
  }

  /** The actual path implementation. */
  @Editable
  public Implementation implementation = new Original();

  /**
   * Returns a reference to the config's underlying original implementation.
   */
  public Original getOriginal (ConfigManager cfgmgr)
  {
    return implementation.getOriginal(cfgmgr);
  }

  /**
   * Creates or updates a cursor implementation for this configuration.
   *
   * @param scope the path's expression scope.
   * @param impl an existing implementation to reuse, if possible.
   * @return either a reference to the existing implementation (if reused), a new
   * implementation, or <code>null</code> if no implementation could be created.
   */
  public PathCursor.Implementation getCursorImplementation (
    TudeyContext ctx, Scope scope, PathCursor.Implementation impl)
  {
    return implementation.getCursorImplementation(ctx, scope, impl);
  }

  /**
   * Creates or updates a sprite implementation for this configuration.
   *
   * @param scope the path's expression scope.
   * @param impl an existing implementation to reuse, if possible.
   * @return either a reference to the existing implementation (if reused), a new
   * implementation, or <code>null</code> if no implementation could be created.
   */
  public PathSprite.Implementation getSpriteImplementation (
    TudeyContext ctx, Scope scope, PathSprite.Implementation impl)
  {
    return implementation.getSpriteImplementation(ctx, scope, impl);
  }

  @Override
  protected void fireConfigUpdated ()
  {
    // invalidate the implementation
    implementation.invalidate();
    super.fireConfigUpdated();
  }
}
