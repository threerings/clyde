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

package com.threerings.opengl.renderer.config;

import java.util.List;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.expr.Updater;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.util.GlContext;

/**
 * Represents the state of a single light.
 */
@EditorTypes({
    LightConfig.Directional.class,
    LightConfig.Point.class,
    LightConfig.Spot.class })
public abstract class LightConfig extends DeepObject
    implements Exportable
{
    /**
     * A directional light.
     */
    public static class Directional extends LightConfig
    {
        /** The direction of the light. */
        @Editable(mode="normalized", hgroup="p")
        public Vector3f direction = new Vector3f(0f, 0f, 1f);

        public Directional (LightConfig other)
        {
            super(other);
        }

        public Directional ()
        {
        }

        @Override
        protected Updater createUpdater (
            GlContext ctx, Scope scope, boolean world, final Light light)
        {
            final Transform3D transform = ScopeUtil.resolve(
                scope, world ? "worldTransform" : "viewTransform", new Transform3D());
            return new Updater() {
                public void update () {
                    transform.transformVector(direction, _dir);
                    light.position.set(_dir.x, _dir.y, _dir.z, 0f);
                    light.dirty = true;
                }
                protected Vector3f _dir = new Vector3f();
            };
        }
    }

    /**
     * A point light.
     */
    public static class Point extends LightConfig
    {
        /** The location of the light. */
        @Editable(step=0.01, hgroup="p")
        public Vector3f position = new Vector3f();

        /** The light's attenuation parameters. */
        @Editable(hgroup="p")
        public Attenuation attenuation = new Attenuation();

        public Point (Point other)
        {
            super(other);
            position.set(other.position);
            attenuation.constant = other.attenuation.constant;
            attenuation.linear = other.attenuation.linear;
            attenuation.quadratic = other.attenuation.quadratic;
        }

        public Point (LightConfig other)
        {
            super(other);
        }

        public Point ()
        {
        }

        @Override
        public Light createLight (
            GlContext ctx, Scope scope, boolean world, List<Updater> updaters)
        {
            Light light = super.createLight(ctx, scope, world, updaters);
            light.constantAttenuation = attenuation.constant;
            light.linearAttenuation = attenuation.linear;
            light.quadraticAttenuation = attenuation.quadratic;
            light.position.w = 1f;
            return light;
        }

        @Override
        protected Updater createUpdater (
            GlContext ctx, Scope scope, boolean world, final Light light)
        {
            final Transform3D transform = ScopeUtil.resolve(
                scope, world ? "worldTransform" : "viewTransform", new Transform3D());
            return new Updater() {
                public void update () {
                    transform.transformPoint(position, _pos);
                    light.position.set(_pos.x, _pos.y, _pos.z, 1f);
                    light.dirty = true;
                }
                protected Vector3f _pos = new Vector3f();
            };
        }
    }

    /**
     * A spot light.
     */
    public static class Spot extends Point
    {
        /** The spot direction. */
        @Editable(mode="normalized", hgroup="d")
        public Vector3f direction = new Vector3f(0f, 0f, -1f);

        /** The falloff parameters. */
        @Editable(hgroup="d")
        public Falloff falloff = new Falloff();

        public Spot (Point other)
        {
            super(other);
        }

        public Spot (LightConfig other)
        {
            super(other);
        }

        public Spot ()
        {
        }

        @Override
        public Light createLight (
            GlContext ctx, Scope scope, boolean world, List<Updater> updaters)
        {
            Light light = super.createLight(ctx, scope, world, updaters);
            light.spotExponent = falloff.exponent;
            light.spotCutoff = falloff.cutoff;
            return light;
        }

        @Override
        protected Updater createUpdater (
            GlContext ctx, Scope scope, boolean world, final Light light)
        {
            final Transform3D transform = ScopeUtil.resolve(
                scope, world ? "worldTransform" : "viewTransform", new Transform3D());
            return new Updater() {
                public void update () {
                    transform.transformPoint(position, _pos);
                    light.position.set(_pos.x, _pos.y, _pos.z, 1f);
                    transform.transformVector(direction, light.spotDirection);
                    light.dirty = true;
                }
                protected Vector3f _pos = new Vector3f();
            };
        }
    }

    /**
     * Represents the colors of the light.
     */
    public static class Colors extends DeepObject
        implements Exportable
    {
        /** The ambient light color. */
        @Editable
        public Color4f ambient = new Color4f(0f, 0f, 0f, 1f);

        /** The diffuse light color. */
        @Editable
        public Color4f diffuse = new Color4f(1f, 1f, 1f, 1f);

        /** The specular light color. */
        @Editable
        public Color4f specular = new Color4f(1f, 1f, 1f, 1f);
    }

    /**
     * Represents the light's attenuation coefficients.
     */
    public static class Attenuation extends DeepObject
        implements Exportable
    {
        /** The constant attenutation. */
        @Editable(min=0, step=0.01)
        public float constant = 1f;

        /** The linear attenuation. */
        @Editable(min=0, step=0.01)
        public float linear;

        /** The quadratic attenutation. */
        @Editable(min=0, step=0.01)
        public float quadratic;
    }

    /**
     * Represents the spot light falloff.
     */
    public static class Falloff extends DeepObject
        implements Exportable
    {
        /** The falloff exponent. */
        @Editable(min=0, max=128)
        public float exponent;

        /** The falloff cutoff. */
        @Editable(min=0, max=90)
        public float cutoff;
    }

    /** The color of the light. */
    @Editable(hgroup="p")
    public Colors colors = new Colors();

    public LightConfig (LightConfig other)
    {
        colors.ambient.set(other.colors.ambient);
        colors.diffuse.set(other.colors.diffuse);
        colors.specular.set(other.colors.specular);
    }

    public LightConfig ()
    {
    }

    /**
     * Creates a light object corresponding to this configuration.
     *
     * @param world if true, update the light's position in world space rather than view space.
     */
    public Light createLight (GlContext ctx, Scope scope, boolean world, List<Updater> updaters)
    {
        Light light = new Light();
        light.ambient.set(colors.ambient);
        light.diffuse.set(colors.diffuse);
        light.specular.set(colors.specular);
        light.position.set(0f, 0f, 0f, 0f);
        updaters.add(createUpdater(ctx, scope, world, light));
        return light;
    }

    /**
     * Creates the updater for the light.
     */
    protected abstract Updater createUpdater (
        GlContext ctx, Scope scope, boolean world, Light light);
}
