//
// $Id$

package com.threerings.opengl.material;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.samskivert.util.StringUtil;

import com.threerings.math.Matrix4f;
import com.threerings.math.Transform;

import com.threerings.opengl.geometry.Geometry;
import com.threerings.opengl.model.SkinMesh;
import com.threerings.opengl.renderer.ClientArray;
import com.threerings.opengl.renderer.Program;
import com.threerings.opengl.renderer.Program.Uniform;
import com.threerings.opengl.renderer.Program.UniformMatrix4f;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.TextureUnit;
import com.threerings.opengl.renderer.state.ArrayState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.state.ShaderState;
import com.threerings.opengl.renderer.state.TextureState;
import com.threerings.opengl.util.GlContext;

/**
 * Extends {@link DefaultSurface} to add support for skinned meshes.
 */
public class SkinSurface extends DefaultSurface
{
    public SkinSurface (GlContext ctx, DefaultMaterial material, SkinMesh mesh)
    {
        super(ctx, material, mesh);
        _bones = mesh.getBones();

        // determine whether we're using a sphere map
        RenderState[] states = _bbatch.getStates();
        TextureState tstate = (TextureState)states[RenderState.TEXTURE_STATE];
        TextureUnit unit = (tstate == null) ? null : tstate.getUnit(0);

        Program program;
        if (unit.genModeS == GL11.GL_SPHERE_MAP) {
            if ((program = material.getSkinProgram(true)) != null) {
                unit.genModeS = unit.genModeT = -1; // the shader will handle texture generation
            }
        } else {
            program = material.getSkinProgram(false);
        }

        // create the shader state if we're using one
        if (program != null) {
            Uniform[] uniforms = new Uniform[_bones.length];
            for (int ii = 0; ii < _bones.length; ii++) {
                int loc = program.getUniformLocation("boneMatrices[" + ii + "]");
                uniforms[ii] = new UniformMatrix4f(loc);
            }
            _bbatch.getStates()[RenderState.SHADER_STATE] = new ShaderState(program, uniforms);
        }
        _bbatch.updateKey();
    }

    @Override // documentation inherited
    public void setHost (SurfaceHost host)
    {
        super.setHost(host);

        // get the array of shader uniforms
        ShaderState sstate = (ShaderState)_bbatch.getStates()[RenderState.SHADER_STATE];
        Uniform[] uniforms = sstate.getUniforms();

        // retrieve references to the bone matrices
        SkinHost shost = (SkinHost)host;
        _boneMatrices = new Matrix4f[_bones.length];
        for (int ii = 0; ii < _bones.length; ii++) {
            _boneMatrices[ii] = shost.getBoneMatrix(_bones[ii]);
            if (uniforms != null) {
                ((UniformMatrix4f)uniforms[ii]).value = _boneMatrices[ii];
            }
        }
    }

    @Override // documentation inherited
    public void enqueue ()
    {
        // if using a shader, all we have to do is dirty the uniforms
        RenderState[] states = _bbatch.getStates();
        ShaderState sstate = (ShaderState)states[RenderState.SHADER_STATE];
        if (sstate != ShaderState.DISABLED) {
            Uniform[] uniforms = sstate.getUniforms();
            for (Uniform uniform : uniforms) {
                UniformMatrix4f unif = (UniformMatrix4f)uniform;
                uniform.dirty = true;
            }
            sstate.setDirty(true);
            super.enqueue();
            return;
        }

        // get a reference to the vertex array to which we will write
        ArrayState astate = (ArrayState)_bbatch.getStates()[RenderState.ARRAY_STATE];
        astate.setDirty(true);
        ClientArray narray = astate.getNormalArray();
        int vinc = narray.stride/4, vpos = (int)narray.offset/4;

        // get everything in local variables
        float[] vbuf = _vbuf, vertices = _vertices, boneWeights = _boneWeights;
        Matrix4f[] boneMatrices = _boneMatrices;
        int[] boneIndices = _boneIndices;

        // blend and write each vertex to the working buffer
        for (int vidx = 0, bidx = 0; vidx < vertices.length; ) {
            // retrieve the source normal and vertex
            float nx = vertices[vidx++], ny = vertices[vidx++], nz = vertices[vidx++];
            float vx = vertices[vidx++], vy = vertices[vidx++], vz = vertices[vidx++];

            // blend in the normal and vertex as transformed by the indexed bone matrix
            float tvx = 0f, tvy = 0f, tvz = 0f;
            float tnx = 0f, tny = 0f, tnz = 0f;
            for (int ii = 0; ii < 4; ii++) {
                Matrix4f m = boneMatrices[boneIndices[bidx]];
                float weight = boneWeights[bidx++];

                float m00 = m.m00, m10 = m.m10, m20 = m.m20;
                float m01 = m.m01, m11 = m.m11, m21 = m.m21;
                float m02 = m.m02, m12 = m.m12, m22 = m.m22;
                tnx += (m00*nx + m10*ny + m20*nz) * weight;
                tny += (m01*nx + m11*ny + m21*nz) * weight;
                tnz += (m02*nx + m12*ny + m22*nz) * weight;

                tvx += (m00*vx + m10*vy + m20*vz + m.m30) * weight;
                tvy += (m01*vx + m11*vy + m21*vz + m.m31) * weight;
                tvz += (m02*vx + m12*vy + m22*vz + m.m32) * weight;
            }

            // write the blended normal
            vbuf[vpos] = tnx;
            vbuf[vpos + 1] = tny;
            vbuf[vpos + 2] = tnz;

            // and the vertex
            vbuf[vpos + 3] = tvx;
            vbuf[vpos + 4] = tvy;
            vbuf[vpos + 5] = tvz;
            vpos += vinc;
        }

        // copy the working buffer to the vertex buffer all at once
        narray.floatArray.put(_vbuf).rewind();

        // enqueue the batch for rendering
        super.enqueue();
    }

    @Override // documentation inherited
    public Object clone ()
    {
        // depending on whether we are using hardware or software skinning, make a deep(ish) copy
        // of the shader state or array state
        SkinSurface osurface = (SkinSurface)super.clone();
        RenderState[] states = _bbatch.getStates(), ostates = osurface._bbatch.getStates();
        ShaderState sstate = (ShaderState)states[RenderState.SHADER_STATE];
        if (sstate == ShaderState.DISABLED) {
            ostates[RenderState.ARRAY_STATE] =
                copyArrayState((ArrayState)states[RenderState.ARRAY_STATE]);
        } else {
            ostates[RenderState.SHADER_STATE] = copyShaderState(sstate);
        }
        return osurface;
    }

    @Override // documentation inherited
    protected SimpleBatch createBaseBatch (Geometry geom)
    {
        // if we're using a shader, we can create a nondeformable batch
        if (_material.hasSkinPrograms()) {
            return super.createBaseBatch(geom);
        }

        // start with a non-compiled batch
        SimpleBatch batch = geom.createBatch(_ctx.getRenderer(), true);

        // create the arrays to hold the source data
        SkinMesh mesh = (SkinMesh)geom;
        int start = mesh.getStart(), vcount = 1 + mesh.getEnd() - start;
        _vertices = new float[vcount * 3 * 2];
        _boneIndices = new int[vcount * 4];
        _boneWeights = new float[vcount * 4];

        // find the starting positions and strides for each buffer
        ClientArray iarray = mesh.getBoneIndexArray(), warray = mesh.getBoneWeightArray();
        ClientArray narray = mesh.getNormalArray(), varray = mesh.getVertexArray();
        int iinc = iarray.stride/4, ipos = (int)iarray.offset/4 + start*iinc;
        int winc = warray.stride/4, wpos = (int)warray.offset/4 + start*winc;
        int ninc = narray.stride/4, npos = (int)narray.offset/4 + start*ninc;
        int vinc = varray.stride/4, vpos = (int)varray.offset/4 + start*vinc;

        // extract the bone indices, bone weights, normals, and vertices into the arrays
        for (int ii = 0, vidx = 0, bidx = 0; ii < vcount; ii++) {
            _boneIndices[bidx] = (int)iarray.floatArray.get(ipos);
            _boneIndices[bidx + 1] = (int)iarray.floatArray.get(ipos + 1);
            _boneIndices[bidx + 2] = (int)iarray.floatArray.get(ipos + 2);
            _boneIndices[bidx + 3] = (int)iarray.floatArray.get(ipos + 3);
            ipos += iinc;

            _boneWeights[bidx++] = warray.floatArray.get(wpos);
            _boneWeights[bidx++] = warray.floatArray.get(wpos + 1);
            _boneWeights[bidx++] = warray.floatArray.get(wpos + 2);
            _boneWeights[bidx++] = warray.floatArray.get(wpos + 3);
            wpos += winc;

            _vertices[vidx++] = narray.floatArray.get(npos);
            _vertices[vidx++] = narray.floatArray.get(npos + 1);
            _vertices[vidx++] = narray.floatArray.get(npos + 2);
            npos += ninc;

            _vertices[vidx++] = varray.floatArray.get(vpos);
            _vertices[vidx++] = varray.floatArray.get(vpos + 1);
            _vertices[vidx++] = varray.floatArray.get(vpos + 2);
            vpos += vinc;
        }

        // determine the size of the new merged array
        ClientArray[] tcarrays = mesh.getTexCoordArrays();
        ClientArray carray = mesh.getColorArray();
        int vsize = narray.size + varray.size;
        if (tcarrays != null) {
            for (ClientArray tcarray : tcarrays) {
                vsize += tcarray.size;
            }
        }
        if (carray != null) {
            vsize += carray.size;
        }

        // create the merged array buffer and find the source buffer parameters
        FloatBuffer array = BufferUtils.createFloatBuffer(vcount * vsize);
        _vbuf = new float[vcount * vsize];
        int stride = vsize*4, offset = 0;
        ClientArray[] ntcarrays = null;
        int[] tcinc = null, tcpos = null;
        if (tcarrays != null) {
            ntcarrays = new ClientArray[tcarrays.length];
            tcinc = new int[tcarrays.length];
            tcpos = new int[tcarrays.length];
            for (int ii = 0; ii < tcarrays.length; ii++) {
                int size = tcarrays[ii].size;
                ntcarrays[ii] = new ClientArray(size, stride, offset, array);
                offset += size*4;
                tcinc[ii] = tcarrays[ii].stride/4;
                tcpos[ii] = (int)tcarrays[ii].offset/4 + start*tcinc[ii];
            }
        }
        ClientArray ncarray = null;
        int cinc = 0, cpos = 0;
        if (carray != null) {
            ncarray = new ClientArray(carray.size, stride, offset, array);
            offset += carray.size*4;
            cinc = carray.stride/4;
            cpos = (int)carray.offset/4 + start*cinc;
        }
        ClientArray nnarray = new ClientArray(narray.size, stride, offset, array);
        offset += narray.size*4;
        ClientArray nvarray = new ClientArray(varray.size, stride, offset, array);

        // copy the contents of the original arrays into the merged array
        // (just leave empty spaces for the vertices/normals)
        for (int ii = 0, idx = 0; ii < vcount; ii++) {
            if (tcarrays != null) {
                for (int jj = 0; jj < tcarrays.length; jj++) {
                    ClientArray tcarray = tcarrays[jj];
                    for (int kk = 0; kk < tcarray.size; kk++) {
                        _vbuf[idx++] = tcarray.floatArray.get(tcpos[jj] + kk);
                    }
                    tcpos[jj] += tcinc[jj];
                }
            }
            if (carray != null) {
                for (int jj = 0; jj < carray.size; jj++) {
                    _vbuf[idx++] = carray.floatArray.get(cpos + jj);
                }
                cpos += cinc;
            }
            idx += narray.size;
            idx += varray.size;
        }

        // replace the array state
        RenderState[] states = batch.getStates();
        ArrayState astate = (ArrayState)states[RenderState.ARRAY_STATE];
        states[RenderState.ARRAY_STATE] = new ArrayState(
            0, null, ntcarrays, ncarray, nnarray, nvarray,
            astate.getElementArrayBuffer());

        // finally, return the modified batch
        return batch;
    }

    /** The names of the bones. */
    protected String[] _bones;

    /** The source vertex and normal data (if not using a shader). */
    protected float[] _vertices;

    /** The bone indices. */
    protected int[] _boneIndices;

    /** The bone weights. */
    protected float[] _boneWeights;

    /** Holds the skinned vertex data. */
    protected float[] _vbuf;

    /** The matrices of the bone transforms. */
    protected Matrix4f[] _boneMatrices;
}
