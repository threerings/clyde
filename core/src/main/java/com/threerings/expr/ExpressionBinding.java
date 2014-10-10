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

package com.threerings.expr;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Tuple;

import com.threerings.config.ConfigManager;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.PathProperty;
import com.threerings.editor.Property;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.math.Transform3D;

import com.threerings.opengl.renderer.Color4f;

import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Preloadable;

import static com.threerings.ClydeLog.log;

/**
 * The superclass of the expression bindings.
 */
@EditorTypes({
    ExpressionBinding.FloatBinding.class,
    ExpressionBinding.IntegerBinding.class,
    ExpressionBinding.Color4fBinding.class,
    ExpressionBinding.StringBinding.class,
    ExpressionBinding.Transform3DBinding.class })
public abstract class ExpressionBinding extends DeepObject
    implements Exportable, Preloadable.LoadableConfig
{
    /** An empty (and thus immutable and sharable) ExpressionBinding array. */
    public static final ExpressionBinding[] EMPTY_ARRAY = new ExpressionBinding[0];

    /**
     * A float binding.
     */
    public static class FloatBinding extends ExpressionBinding
    {
        /** The expression that determines the value. */
        @Editable
        public FloatExpression expression = new FloatExpression.Constant();

        @Override
        public Updater createUpdater (ConfigManager cfgmgr, Scope scope, Object object)
        {
            final FloatExpression.Evaluator evaluator = expression.createEvaluator(scope);
            final Tuple<Property, Object>[] targets = getTargets(cfgmgr, object, Float.TYPE);
            final Tuple<Property, Object>[] flags = getFlags(cfgmgr, object);
            return new Updater() {
                public void update () {
                    float value = evaluator.evaluate();
                    for (Tuple<Property, Object> target : targets) {
                        target.left.setFloat(target.right, value);
                    }
                    for (Tuple<Property, Object> flag : flags) {
                        flag.left.setBoolean(flag.right, true);
                    }
                }
            };
        }

        @Override
        public void preload (GlContext ctx)
        {
            expression.createEvaluator(ctx.getScope());
        }

        @Override
        public void invalidate ()
        {
            super.invalidate();
            expression.invalidate();
        }
    }

    /**
     * An integer binding.
     */
    public static class IntegerBinding extends ExpressionBinding
    {
        /** The expression that determines the value. */
        @Editable
        public IntegerExpression expression = new IntegerExpression.Constant();

        @Override
        public Updater createUpdater (ConfigManager cfgmgr, Scope scope, Object object)
        {
            final IntegerExpression.Evaluator evaluator = expression.createEvaluator(scope);
            final Tuple<Property, Object>[] targets = getTargets(cfgmgr, object, Integer.TYPE);
            final Tuple<Property, Object>[] flags = getFlags(cfgmgr, object);
            return new Updater() {
                public void update () {
                    int value = evaluator.evaluate();
                    for (Tuple<Property, Object> target : targets) {
                        target.left.setInt(target.right, value);
                    }
                    for (Tuple<Property, Object> flag : flags) {
                        flag.left.setBoolean(flag.right, true);
                    }
                }
            };
        }

        @Override
        public void preload (GlContext ctx)
        {
            expression.createEvaluator(ctx.getScope());
        }

        @Override
        public void invalidate ()
        {
            super.invalidate();
            expression.invalidate();
        }
    }

    /**
     * A color binding.
     */
    public static class Color4fBinding extends ExpressionBinding
    {
        /** The expression that determines the value. */
        @Editable
        public Color4fExpression expression = new Color4fExpression.Constant();

        @Override
        public Updater createUpdater (ConfigManager cfgmgr, Scope scope, Object object)
        {
            return createUpdater(cfgmgr, scope, object, expression, Color4f.class);
        }

        @Override
        public void preload (GlContext ctx)
        {
            expression.createEvaluator(ctx.getScope());
        }

        @Override
        public void invalidate ()
        {
            super.invalidate();
            expression.invalidate();
        }
    }

    /**
     * A string binding.
     */
    public static class StringBinding extends ExpressionBinding
    {
        /** The expression that determines the value. */
        @Editable
        public StringExpression expression = new StringExpression.Constant();

        @Override
        public Updater createUpdater (ConfigManager cfgmgr, Scope scope, Object object)
        {
            return createUpdater(cfgmgr, scope, object, expression, String.class);
        }

        @Override
        public void preload (GlContext ctx)
        {
            expression.createEvaluator(ctx.getScope());
        }

        @Override
        public void invalidate ()
        {
            super.invalidate();
            expression.invalidate();
        }
    }

    /**
     * A transform binding.
     */
    public static class Transform3DBinding extends ExpressionBinding
    {
        /** The expression that determines the value. */
        @Editable
        public Transform3DExpression expression = new Transform3DExpression.Constant();

        @Override
        public Updater createUpdater (ConfigManager cfgmgr, Scope scope, Object object)
        {
            return createUpdater(cfgmgr, scope, object, expression, Transform3D.class);
        }

        @Override
        public void preload (GlContext ctx)
        {
            expression.createEvaluator(ctx.getScope());
        }

        @Override
        public void invalidate ()
        {
            super.invalidate();
            expression.invalidate();
        }
    }

    /** The paths of the bound variables. */
    @Editable(width=40)
    public String[] paths = ArrayUtil.EMPTY_STRING;

    /** The paths of any dirty flags to set. */
    @Editable(width=40)
    public String[] flags = ArrayUtil.EMPTY_STRING;

    /**
     * Creates a value updater for the supplied context and target object.
     */
    public abstract Updater createUpdater (ConfigManager cfgmgr, Scope scope, Object object);

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        _paths = _flagPaths = null;
    }

    @Override
    public void preload (GlContext ctx)
    {
        // Do nothing
    }

    /**
     * Creates an updater for an object expression.
     */
    protected <T> Updater createUpdater (
        ConfigManager cfgmgr, Scope scope, Object object,
        ObjectExpression<T> expression, Class<T> clazz)
    {
        final ObjectExpression.Evaluator<T> evaluator = expression.createEvaluator(scope);
        final Tuple<Property, Object>[] targets = getTargets(cfgmgr, object, clazz);
        final Tuple<Property, Object>[] flags = getFlags(cfgmgr, object);
        return new Updater() {
            public void update () {
                T value = evaluator.evaluate();
                for (Tuple<Property, Object> target : targets) {
                    target.left.set(target.right, value);
                }
                for (Tuple<Property, Object> flag : flags) {
                    flag.left.setBoolean(flag.right, true);
                }
            }
        };
    }

    /**
     * Returns the array of property/object pairs representing the targets to update.
     *
     * @param type the required type.
     */
    protected Tuple<Property, Object>[] getTargets (
        ConfigManager cfgmgr, Object object, Class<?> type)
    {
        if (_paths == null) {
            _paths = createPaths(cfgmgr, object, paths, type);
        }
        return resolvePaths(object, _paths);
    }

    /**
     * Returns the array of property/object pairs representing the flags to set.
     */
    protected Tuple<Property, Object>[] getFlags (ConfigManager cfgmgr, Object object)
    {
        if (_flagPaths == null) {
            _flagPaths = createPaths(cfgmgr, object, flags, Boolean.TYPE);
        }
        return resolvePaths(object, _flagPaths);
    }

    /**
     * Creates and returns the paths in the supplied array using the given object as a reference.
     *
     * @param type the required end type.
     */
    protected Property[][] createPaths (
        ConfigManager cfgmgr, Object reference, String[] paths, Class<?> type)
    {
        ArrayList<Property[]> list = new ArrayList<Property[]>(paths.length);
        for (String path : paths) {
            Property[] props = PathProperty.createPath(cfgmgr, reference, path);
            if (props != null &&
                    ((Class<?>)props[props.length - 1].getType()).isAssignableFrom(type)) {
                list.add(props);
            }
        }
        return list.toArray(new Property[list.size()][]);
    }

    /**
     * Resolves the supplied paths against the object, return a property/object pair to use
     * to update the property.
     */
    protected Tuple<Property, Object>[] resolvePaths (Object object, Property[][] paths)
    {
        ArrayList<Tuple<Property, Object>> list = Lists.newArrayListWithCapacity(paths.length);
        for (Property[] path : paths) {
            Object ref = object;
            int lidx = path.length - 1;
            try {
                for (int ii = 0; ii < lidx; ii++) {
                    ref = path[ii].get(ref);
                }
                list.add(new Tuple<Property, Object>(path[lidx], ref));

            } catch (Exception e) {
                log.warning("Error resolving path.", "object", object, "path", path, e);
            }
        }
        @SuppressWarnings("unchecked") Tuple<Property, Object>[] array =
            (Tuple<Property, Object>[])new Tuple<?, ?>[list.size()];
        return list.toArray(array);
    }

    /** The cached paths. */
    @DeepOmit
    protected transient Property[][] _paths;

    /** The cached dirty flag paths. */
    @DeepOmit
    protected transient Property[][] _flagPaths;
}
