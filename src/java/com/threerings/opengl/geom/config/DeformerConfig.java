//
// $Id$

package com.threerings.opengl.geom.config;

import java.util.ArrayList;

import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Function;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Matrix4f;
import com.threerings.util.DeepObject;

import com.threerings.opengl.geom.Geometry;
import com.threerings.opengl.renderer.SimpleBatch.DrawCommand;
import com.threerings.opengl.renderer.config.ClientArrayConfig;
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
            // get the array of bone matrices
            final Matrix4f[] boneMatrices = config.getBoneMatrices(scope);

            // get the index and weight arrays; if we're missing anything, fall back to static
            ClientArrayConfig boneIndexArray = config.getVertexAttribArray("boneIndices");
            ClientArrayConfig boneWeightArray = config.getVertexAttribArray("boneWeights");
            if (boneMatrices == null || boneIndexArray == null || boneWeightArray == null) {
                return config.createStaticGeometry(ctx, scope, passes);
            }
            final int[] boneIndices = config.getIntArray(false, boneIndexArray);
            final float[] boneWeights = config.getFloatArray(false, boneWeightArray);

            // get the source data (tangents, normals, and vertices)
            PassSummary summary = new PassSummary(passes);
            ArrayList<ClientArrayConfig> sourceArrays = new ArrayList<ClientArrayConfig>();
            ClientArrayConfig tangentArray = summary.vertexAttribs.contains("tangents") ?
                config.getVertexAttribArray("tangents") : null;
            if (tangentArray != null) {
                sourceArrays.add(tangentArray);
            }
            if (summary.normals && config.normalArray != null) {
                sourceArrays.add(config.normalArray);
            }
            sourceArrays.add(config.vertexArray);
            final float[] source = config.getFloatArray(
                false, sourceArrays.toArray(new ClientArrayConfig[sourceArrays.size()]));

            // get the dest data (shared between instances)
            ArrayList<ClientArrayConfig> destArrays = new ArrayList<ClientArrayConfig>();
            for (String attrib : summary.vertexAttribs) {
                ClientArrayConfig vertexAttribArray = config.getVertexAttribArray(attrib);
                if (vertexAttribArray != null) {
                    destArrays.add(vertexAttribArray);
                }
            }
            for (int set : summary.texCoordSets) {
                ClientArrayConfig texCoordArray = config.getTexCoordArray(set);
                if (texCoordArray != null) {
                    destArrays.add(texCoordArray);
                }
            }
            if (summary.colors && config.colorArray != null) {
                destArrays.add(config.colorArray);
            }
            if (summary.normals && config.normalArray != null) {
                destArrays.add(config.normalArray);
            }
            destArrays.add(config.vertexArray);
            final float[] dest = config.getFloatArray(
                true, destArrays.toArray(new ClientArrayConfig[destArrays.size()]));

            // create the array states, draw command, and the geometry itself
            final ArrayState[] arrayStates = config.createArrayStates(
                ctx, passes, summary, false, true, false);
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
