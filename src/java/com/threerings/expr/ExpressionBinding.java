//
// $Id$

package com.threerings.expr;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import com.samskivert.util.Tuple;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.PathProperty;
import com.threerings.editor.Property;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.math.Transform3D;

import com.threerings.opengl.renderer.Color4f;

import static com.threerings.ClydeLog.*;

/**
 * The superclass of the expression bindings.
 */
@EditorTypes({
    ExpressionBinding.FloatBinding.class,
    ExpressionBinding.Color4fBinding.class,
    ExpressionBinding.Transform3DBinding.class })
public abstract class ExpressionBinding extends DeepObject
    implements Exportable
{
    /**
     * A float binding.
     */
    public static class FloatBinding extends ExpressionBinding
    {
        /** The expression that determines the value. */
        @Editable
        public FloatExpression expression = new FloatExpression.Constant();

        @Override // documentation inherited
        public Updater createUpdater (Scope scope, Object object)
        {
            final FloatExpression.Evaluator evaluator = expression.createEvaluator(scope);
            final Tuple<Property, Object>[] targets = getTargets(object, Float.TYPE);
            final Tuple<Property, Object>[] flags = getFlags(object);
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
    }

    /**
     * A color binding.
     */
    public static class Color4fBinding extends ExpressionBinding
    {
        /** The expression that determines the value. */
        @Editable
        public Color4fExpression expression = new Color4fExpression.Constant();

        @Override // documentation inherited
        public Updater createUpdater (Scope scope, Object object)
        {
            return createUpdater(scope, object, expression, Color4f.class);
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

        @Override // documentation inherited
        public Updater createUpdater (Scope scope, Object object)
        {
            return createUpdater(scope, object, expression, Transform3D.class);
        }
    }

    /** The paths of the bound variables. */
    @Editable(width=40)
    public String[] paths = new String[0];

    /** The paths of any dirty flags to set. */
    @Editable(width=40)
    public String[] flags = new String[0];

    /**
     * Creates a value updater for the supplied context and target object.
     */
    public abstract Updater createUpdater (Scope scope, Object object);

    /**
     * Invalidates the paths, forcing them to be recreated.
     */
    public void invalidatePaths ()
    {
        _paths = _flagPaths = null;
    }

    /**
     * Creates an updater for an object expression.
     */
    protected <T> Updater createUpdater (
        Scope scope, Object object, ObjectExpression<T> expression, Class<T> clazz)
    {
        final ObjectExpression.Evaluator<T> evaluator = expression.createEvaluator(scope);
        final Tuple<Property, Object>[] targets = getTargets(object, clazz);
        final Tuple<Property, Object>[] flags = getFlags(object);
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
    protected Tuple<Property, Object>[] getTargets (Object object, Class type)
    {
        if (_paths == null) {
            _paths = createPaths(object, paths, type);
        }
        return resolvePaths(object, _paths);
    }

    /**
     * Returns the array of property/object pairs representing the flags to set.
     */
    protected Tuple<Property, Object>[] getFlags (Object object)
    {
        if (_flagPaths == null) {
            _flagPaths = createPaths(object, flags, Boolean.TYPE);
        }
        return resolvePaths(object, _flagPaths);
    }

    /**
     * Creates and returns the paths in the supplied array using the given object as a reference.
     *
     * @param type the required end type.
     */
    protected Property[][] createPaths (Object reference, String[] paths, Class type)
    {
        ArrayList<Property[]> list = new ArrayList<Property[]>();
        for (String path : paths) {
            Property[] props = PathProperty.createPath(null, reference, path);
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
        ArrayList<Tuple<Property, Object>> list = Lists.newArrayList();
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
            (Tuple<Property, Object>[])new Tuple[list.size()];
        return list.toArray(array);
    }

    /** The cached paths. */
    @DeepOmit
    protected transient Property[][] _paths;

    /** The cached dirty flag paths. */
    @DeepOmit
    protected transient Property[][] _flagPaths;
}
