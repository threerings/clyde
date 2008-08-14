//
// $Id$

package com.threerings.opengl.geom.config;

import java.nio.FloatBuffer;

import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBBufferObject;
import org.lwjgl.opengl.GLContext;

import com.samskivert.util.ListUtil;

import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Function;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Matrix4f;
import com.threerings.util.DeepObject;

import com.threerings.opengl.geom.Geometry;
import com.threerings.opengl.renderer.BufferObject;
import com.threerings.opengl.renderer.ClientArray;
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
            final boolean tangents = (tangentArray != null);
            if (tangents) {
                sourceArrays.add(tangentArray);
            }
            final boolean normals = (summary.normals && config.normalArray != null);
            if (normals) {
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

            // create the array states and, if possible, a VBO to hold the skinned data
            final boolean vbo = GLContext.getCapabilities().GL_ARB_vertex_buffer_object;
            final BufferObject arrayBuffer = vbo ? new BufferObject(ctx.getRenderer()) : null;
            final FloatBuffer floatArray = vbo ? getScratchBuffer(dest.length) :
                BufferUtils.createFloatBuffer(dest.length);
            final ArrayState[] arrayStates = config.createArrayStates(
                ctx, passes, summary, false, true, arrayBuffer, vbo ? null : floatArray);
            final int tangentOffset = tangents ? getTangentOffset(passes, arrayStates) : 0;
            final int normalOffset = normals ? getNormalOffset(arrayStates) : 0;
            ClientArray vertexArray = arrayStates[0].getVertexArray();
            final int vertexOffset = (int)(vertexArray.offset / 4);
            final int vertexStride = vertexArray.stride / 4;

            // finally, create the draw command and the geometry itself
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
                    // skin based on attributes
                    if (tangents && normals) {
                        skinVertices(
                            source, dest, boneMatrices, boneIndices, boneWeights,
                            tangentOffset, normalOffset, vertexOffset, vertexStride);
                    } else if (normals) {
                        skinVertices(
                            source, dest, boneMatrices, boneIndices, boneWeights,
                            normalOffset, vertexOffset, vertexStride);
                    } else {
                        skinVertices(
                            source, dest, boneMatrices, boneIndices,
                            boneWeights, vertexOffset, vertexStride);
                    }

                    // copy from array to buffer
                    floatArray.clear();
                    floatArray.put(dest).flip();

                    // copy from buffer to vbo if using one
                    if (vbo) {
                        arrayBuffer.setData(floatArray, ARBBufferObject.GL_STREAM_DRAW_ARB);
                    }
                }
            };
        }

        /**
         * Retrieves the tangent offset from the supplied array states.
         */
        protected int getTangentOffset (PassDescriptor[] passes, ArrayState[] arrayStates)
        {
            for (int ii = 0; ii < passes.length; ii++) {
                int idx = ListUtil.indexOf(passes[ii].vertexAttribs, "tangents");
                if (idx != -1) {
                    return (int)(arrayStates[ii].getVertexAttribArrays()[idx].offset / 4);
                }
            }
            return 0;
        }

        /**
         * Retrieves the normal offset from the supplied array states.
         */
        protected int getNormalOffset (ArrayState[] arrayStates)
        {
            for (ArrayState state : arrayStates) {
                ClientArray normalArray = state.getNormalArray();
                if (normalArray != null) {
                    return (int)(normalArray.offset / 4);
                }
            }
            return 0;
        }
    }

    /**
     * Creates a deformed geometry object.
     */
    public abstract Geometry createGeometry (
        GlContext ctx, Scope scope, GeometryConfig.Stored config, PassDescriptor[] passes);

    /**
     * Returns a reference to the scratch buffer, (re)creating it if necessary to provide the
     * supplied size.
     */
    protected static FloatBuffer getScratchBuffer (int size)
    {
        if (_scratchBuffer == null || _scratchBuffer.capacity() < size) {
            _scratchBuffer = BufferUtils.createFloatBuffer(size);
        }
        return _scratchBuffer;
    }

    /**
     * Skins a set of vertices, normals, and tangents.
     *
     * @param tidx the index of the first tangent in the destination array.
     * @param nidx the index of the first normal in the destination array.
     * @param vidx the index of the first vertex in the destination array.
     * @param dinc the stride between adjacent vertices in the destination array.
     */
    protected static void skinVertices (
        float[] source, float[] dest, Matrix4f[] boneMatrices, int[] boneIndices,
        float[] boneWeights, int tidx, int nidx, int vidx, int dinc)
    {
        for (int sidx = 0, bidx = 0; sidx < source.length; ) {
            // retrieve the source tangent, normal, and vertex
            float stx = source[sidx++], sty = source[sidx++], stz = source[sidx++];
            float snx = source[sidx++], sny = source[sidx++], snz = source[sidx++];
            float svx = source[sidx++], svy = source[sidx++], svz = source[sidx++];

            // blend in the tangent, normal, and vertex as transformed by each indexed bone matrix
            float dtx = 0f, dty = 0f, dtz = 0f;
            float dnx = 0f, dny = 0f, dnz = 0f;
            float dvx = 0f, dvy = 0f, dvz = 0f;
            for (int ii = 0; ii < 4; ii++) {
                Matrix4f m = boneMatrices[boneIndices[bidx]];
                float weight = boneWeights[bidx++];

                float m00 = m.m00, m10 = m.m10, m20 = m.m20;
                float m01 = m.m01, m11 = m.m11, m21 = m.m21;
                float m02 = m.m02, m12 = m.m12, m22 = m.m22;
                dtx += (m00*stx + m10*sty + m20*stz) * weight;
                dty += (m01*stx + m11*sty + m21*stz) * weight;
                dtz += (m02*stx + m12*sty + m22*stz) * weight;

                dnx += (m00*snx + m10*sny + m20*snz) * weight;
                dny += (m01*snx + m11*sny + m21*snz) * weight;
                dnz += (m02*snx + m12*sny + m22*snz) * weight;

                dvx += (m00*svx + m10*svy + m20*svz + m.m30) * weight;
                dvy += (m01*svx + m11*svy + m21*svz + m.m31) * weight;
                dvz += (m02*svx + m12*svy + m22*svz + m.m32) * weight;
            }

            // write the blended tangent
            dest[tidx] = dtx;
            dest[tidx + 1] = dty;
            dest[tidx + 2] = dtz;
            tidx += dinc;

            // and normal
            dest[nidx] = dnx;
            dest[nidx + 1] = dny;
            dest[nidx + 2] = dnz;
            nidx += dinc;

            // and vertex
            dest[vidx] = dvx;
            dest[vidx + 1] = dvy;
            dest[vidx + 2] = dvz;
            vidx += dinc;
        }
    }

    /**
     * Skins a set of vertices and normals.
     *
     * @param nidx the index of the first normal in the destination array.
     * @param vidx the index of the first vertex in the destination array.
     * @param dinc the stride between adjacent vertices in the destination array.
     */
    protected static void skinVertices (
        float[] source, float[] dest, Matrix4f[] boneMatrices,
        int[] boneIndices, float[] boneWeights, int nidx, int vidx, int dinc)
    {
        for (int sidx = 0, bidx = 0; sidx < source.length; ) {
            // retrieve the source normal and vertex
            float snx = source[sidx++], sny = source[sidx++], snz = source[sidx++];
            float svx = source[sidx++], svy = source[sidx++], svz = source[sidx++];

            // blend in the normal and vertex as transformed by each indexed bone matrix
            float dnx = 0f, dny = 0f, dnz = 0f;
            float dvx = 0f, dvy = 0f, dvz = 0f;
            for (int ii = 0; ii < 4; ii++) {
                Matrix4f m = boneMatrices[boneIndices[bidx]];
                float weight = boneWeights[bidx++];

                float m00 = m.m00, m10 = m.m10, m20 = m.m20;
                float m01 = m.m01, m11 = m.m11, m21 = m.m21;
                float m02 = m.m02, m12 = m.m12, m22 = m.m22;
                dnx += (m00*snx + m10*sny + m20*snz) * weight;
                dny += (m01*snx + m11*sny + m21*snz) * weight;
                dnz += (m02*snx + m12*sny + m22*snz) * weight;

                dvx += (m00*svx + m10*svy + m20*svz + m.m30) * weight;
                dvy += (m01*svx + m11*svy + m21*svz + m.m31) * weight;
                dvz += (m02*svx + m12*svy + m22*svz + m.m32) * weight;
            }

            // write the blended normal
            dest[nidx] = dnx;
            dest[nidx + 1] = dny;
            dest[nidx + 2] = dnz;
            nidx += dinc;

            // and vertex
            dest[vidx] = dvx;
            dest[vidx + 1] = dvy;
            dest[vidx + 2] = dvz;
            vidx += dinc;
        }
    }

    /**
     * Skins a set of vertices.
     *
     * @param vidx the index of the first vertex in the destination array.
     * @param dinc the stride between adjacent vertices in the destination array.
     */
    protected static void skinVertices (
        float[] source, float[] dest, Matrix4f[] boneMatrices,
        int[] boneIndices, float[] boneWeights, int vidx, int dinc)
    {
        for (int sidx = 0, bidx = 0; sidx < source.length; ) {
            // retrieve the source vertex
            float svx = source[sidx++], svy = source[sidx++], svz = source[sidx++];

            // blend in the vertex as transformed by each indexed bone matrix
            float dvx = 0f, dvy = 0f, dvz = 0f;
            for (int ii = 0; ii < 4; ii++) {
                Matrix4f m = boneMatrices[boneIndices[bidx]];
                float weight = boneWeights[bidx++];

                dvx += (m.m00*svx + m.m10*svy + m.m20*svz + m.m30) * weight;
                dvy += (m.m01*svx + m.m11*svy + m.m21*svz + m.m31) * weight;
                dvz += (m.m02*svx + m.m12*svy + m.m22*svz + m.m32) * weight;
            }

            // write the blended vertex
            dest[vidx] = dvx;
            dest[vidx + 1] = dvy;
            dest[vidx + 2] = dvz;
            vidx += dinc;
        }
    }

    /** The shared scratch buffer used to hold vertex data before copying to the VBO. */
    protected static FloatBuffer _scratchBuffer;
}
