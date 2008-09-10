//
// $Id$

package com.threerings.opengl.model.config;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Executor;
import com.threerings.expr.Function;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Transform3D;
import com.threerings.util.DeepObject;

import com.threerings.openal.Sounder;
import com.threerings.openal.config.SounderConfig;
import com.threerings.opengl.mod.Articulated;
import com.threerings.opengl.util.GlContext;

/**
 * Configurations for actions taken by models.
 */
@EditorTypes({
    ActionConfig.CallFunction.class, ActionConfig.SpawnTransient.class,
    ActionConfig.PlaySound.class })
public abstract class ActionConfig extends DeepObject
    implements Exportable
{
    /**
     * Generic action that calls a scoped function.
     */
    public static class CallFunction extends ActionConfig
    {
        /** The name of the function to call. */
        @Editable
        public String name = "";

        @Override // documentation inherited
        public Executor createExecutor (GlContext ctx, Scope scope)
        {
            final Function fn = ScopeUtil.resolve(scope, name, Function.NULL);
            return new Executor() {
                public void execute () {
                    fn.call();
                }
            };
        }
    }

    /**
     * Creates a transient model (such as a particle system) and adds it to the scene at the
     * location of one of the model's nodes.
     */
    public static class SpawnTransient extends ActionConfig
    {
        /** The model to spawn. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        /** The node at whose transform the model should be added. */
        @Editable
        public String node = "";

        @Override // documentation inherited
        public Executor createExecutor (GlContext ctx, Scope scope)
        {
            final Function spawnTransient = ScopeUtil.resolve(
                scope, "spawnTransient", Function.NULL);
            Articulated.Node node = (Articulated.Node)ScopeUtil.call(scope, "getNode", this.node);
            final Transform3D transform = (node == null) ?
                ScopeUtil.resolve(scope, "worldTransform", new Transform3D()) :
                node.getWorldTransform();
            return new Executor() {
                public void execute () {
                    spawnTransient.call(model, transform);
                }
            };
        }
    }

    /**
     * Plays a sound.
     */
    public static class PlaySound extends ActionConfig
    {
        /** The configuration of the sounder that will play the sound. */
        @Editable(nullable=true)
        public ConfigReference<SounderConfig> sounder;

        /** The node at whose transform the sound should be played. */
        @Editable
        public String node = "";

        @Override // documentation inherited
        public Executor createExecutor (GlContext ctx, Scope scope)
        {
            Articulated.Node node = (Articulated.Node)ScopeUtil.call(scope, "getNode", this.node);
            final Transform3D transform = (node == null) ?
                ScopeUtil.resolve(scope, "worldTransform", new Transform3D()) :
                node.getWorldTransform();
            final Sounder sounder = new Sounder(ctx, scope, this.sounder);
            return new Executor() {
                public void execute () {
                    sounder.start();
                }
            };
        }
    }

    /**
     * Creates an executor for this action.
     */
    public abstract Executor createExecutor (GlContext ctx, Scope scope);
}
