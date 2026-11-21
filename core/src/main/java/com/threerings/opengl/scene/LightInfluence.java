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

package com.threerings.opengl.scene;

import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.config.LightConfig;

/**
 * Represents the influence of a light.
 */
public class LightInfluence extends SceneInfluence
{
  /**
   * Creates a new light influence.
   */
  public LightInfluence (Light light)
  {
    this(light, null, null);
  }

  /**
   * Creates a new light influence that knows its copies (see {@link #isCopy}).
   *
   * @param worldTransform the light's world transform, live.
   */
  public LightInfluence (Light light, LightConfig config, Transform3D worldTransform)
  {
    _light = light;
    _config = config;
    _worldTransform = worldTransform;
  }

  /**
   * Is this a copy of the other's light over another area: a directional light of the same
   * config, turned the same way, with other bounds? The lights themselves can't tell, as their
   * directions are only set once they're drawn, after influences are gathered.
   */
  public boolean isCopy (LightInfluence other)
  {
    if (!(_config instanceof LightConfig.Directional dir) ||
        !(other._config instanceof LightConfig.Directional odir) || _worldTransform == null ||
        other._worldTransform == null || _bounds.equals(other._bounds)) {
      return false;
    }
    // the cheap direction test first; the config compare is deep
    _worldTransform.transformVector(dir.direction, _direction);
    other._worldTransform.transformVector(odir.direction, _odirection);
    return _direction.equals(_odirection) &&
      (_config == other._config || _config.equals(other._config));
  }

  @Override
  public Light getLight ()
  {
    return _light;
  }

  /** The light object. */
  protected Light _light;

  /** The light's config, if it can tell its copies. */
  protected LightConfig _config;

  /** The light's world transform, if it can tell its copies. */
  protected Transform3D _worldTransform;

  /** Hold this light's and the other's world directions while comparing. */
  protected Vector3f _direction = new Vector3f(), _odirection = new Vector3f();
}
