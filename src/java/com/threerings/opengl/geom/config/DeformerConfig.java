//
// $Id$

package com.threerings.opengl.geom.config;

import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Function;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Matrix4f;
import com.threerings.util.DeepObject;

import com.threerings.opengl.geom.Geometry;
import com.threerings.opengl.renderer.SimpleBatch.DrawCommand;
import com.threerings.opengl.renderer.config.CoordSpace;
import com.threerings.opengl.renderer.state.ArrayState;
import com.threerings.opengl.util.GlContext;

/**
 * Deformer configuration.
 */
@EditorTypes({ DeformerConfig.Skin.class })
public abstract class DeformerConfig extends DeepObject
    implements Exportable
{
    /**
     * Performs software skinning.
     */
    public static class Skin extends DeformerConfig
    {
        @Override // documentation inherited
        public Geometry createGeometry (
            GlContext ctx, Scope scope, GeometryConfig.Stored config, PassDescriptor[] passes)
        {
            final Matrix4f[] boneMatrices = config.getBoneMatrices(scope);
            if (boneMatrices == null) {
                return config.createStaticGeometry(ctx, scope, passes);
            }
            final ArrayState[] arrayStates = config.createArrayStates(ctx, passes, false, true);
            final DrawCommand drawCommand = config.createDrawCommand(true);
            return new Geometry() {
                public CoordSpace getCoordSpace (int pass) {
                    return CoordSpace.EYE;
                }
                public ArrayState getArrayState (int pass) {
                    return arrayStates[pass];
                }
                public DrawCommand getDrawCommand (int pass) {
                    return drawCommand;
                }
                public boolean requiresUpdate () {
                    return true;
                }
                public void update () {

                }
            };
        }
    }

    /**
     * Creates a deformed geometry object.
     */
    public abstract Geometry createGeometry (
        GlContext ctx, Scope scope, GeometryConfig.Stored config, PassDescriptor[] passes);
}
