//
// $Id$

package com.threerings.opengl.geometry.util;

import java.util.ArrayList;
import java.util.HashMap;

import com.samskivert.util.HashIntMap;

import com.threerings.opengl.geometry.config.PassDescriptor;
import com.threerings.opengl.renderer.BufferObject;
import com.threerings.opengl.renderer.ClientArray;
import com.threerings.opengl.renderer.state.ArrayState;
import com.threerings.opengl.util.GlUtil;

/**
 * Various static methods relating to geometry processing.
 */
public class GeometryUtil
{
    /**
     * Creates a list containing all of the supplied arrays for ease of processing.
     */
    public static ArrayList<ClientArray> createList (
        HashMap<String, ClientArray> vertexAttribArrays, HashIntMap<ClientArray> texCoordArrays,
        ClientArray colorArray, ClientArray normalArray, ClientArray vertexArray)
    {
        ArrayList<ClientArray> list = new ArrayList<ClientArray>();
        list.addAll(vertexAttribArrays.values());
        list.addAll(texCoordArrays.values());
        if (colorArray != null) {
            list.add(colorArray);
        }
        if (normalArray != null) {
            list.add(normalArray);
        }
        list.add(vertexArray);
        return list;
    }

    /**
     * Computes the offsets and stride of the specified interleaved arrays.
     *
     * @return the stride between vertices, in bytes.
     */
    public static int updateOffsetsAndStride (ArrayList<ClientArray> arrays)
    {
        // compute the offsets
        int offset = 0;
        for (ClientArray array : arrays) {
            array.offset = offset;
            offset += array.getElementBytes();
        }

        // bump the stride up to the nearest power of two
        int stride = GlUtil.nextPowerOfTwo(offset);

        // update the arrays with the stride and return it
        for (ClientArray array : arrays) {
            array.stride = stride;
        }
        return stride;
    }

    /**
     * Creates a set of array states for the specified passes.
     *
     * @param vertexAttribArrays maps vertex attribute names to client arrays.
     * @param texCoordArrays maps texture coordinate sets to client arrays.
     * @param colorArray the color client array, if any.
     * @param normalArray the normal client array, if any.
     * @param vertexArray the vertex client array (required).
     * @param elementArrayBuffer the element array buffer, if any.
     */
    public static ArrayState[] createArrayStates (
        HashMap<String, ClientArray> vertexAttribArrays, HashIntMap<ClientArray> texCoordArrays,
        ClientArray colorArray, ClientArray normalArray, ClientArray vertexArray,
        BufferObject elementArrayBuffer, PassDescriptor[] passes)
    {
        ArrayState[] states = new ArrayState[passes.length];
        for (int ii = 0; ii < passes.length; ii++) {
            PassDescriptor pass = passes[ii];
            ClientArray[] attribArrays = new ClientArray[pass.vertexAttribs.length];
            for (int jj = 0; jj < attribArrays.length; jj++) {
                attribArrays[jj] = vertexAttribArrays.get(pass.vertexAttribs[jj]);
            }
            ClientArray[] coordArrays = new ClientArray[pass.texCoordSets.length];
            for (int jj = 0; jj < coordArrays.length; jj++) {
                coordArrays[jj] = texCoordArrays.get(pass.texCoordSets[jj]);
            }
            states[ii] = new ArrayState(
                pass.firstVertexAttribIndex, attribArrays, coordArrays,
                (pass.colors ? colorArray : null), (pass.normals ? normalArray : null),
                vertexArray, elementArrayBuffer);
        }
        return states;
    }
}
