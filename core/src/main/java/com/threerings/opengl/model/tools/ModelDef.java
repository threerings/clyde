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

package com.threerings.opengl.model.tools;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.lwjgl.BufferUtils;

import com.google.common.collect.Lists;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ListUtil;
import com.samskivert.util.StringUtil;
import com.samskivert.util.QuickSort;

import com.threerings.math.Box;
import com.threerings.math.FloatMath;
import com.threerings.math.Matrix4f;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.util.ArrayKey;

import com.threerings.opengl.geometry.config.GeometryConfig;
import com.threerings.opengl.geometry.config.GeometryConfig.AttributeArrayConfig;
import com.threerings.opengl.model.CollisionMesh;
import com.threerings.opengl.model.config.ArticulatedConfig;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.model.config.StaticConfig;
import com.threerings.opengl.model.config.StaticSetConfig;
import com.threerings.opengl.renderer.config.ClientArrayConfig;

import static com.threerings.opengl.Log.log;

/**
 * An intermediate representation for models used to store data parsed from
 * XML.
 */
public class ModelDef
{
    /**
     * The abstract base class of the nodes stored in the model definition.
     */
    public static abstract class SpatialDef
    {
        /** The node's name. */
        public String name;

        /** The name of the node's parent. */
        public String parent;

        /** The node's transformation. */
        public float[] translation;
        public float[] rotation;
        public float[] scale;

        /** The definition of the node's parent. */
        public SpatialDef parentDef;

        /** The definitions of the node's children. */
        public ArrayList<SpatialDef> childDefs = new ArrayList<SpatialDef>();

        /**
         * Creates a set of meshes for this node.
         */
        public ModelConfig.MeshSet createMeshes (ModelConfig.Imported config)
        {
            clearTransforms(config);

            // flatten transforms and merge meshes with same properties
            HashMap<Object, TriMeshDef> meshes = new HashMap<Object, TriMeshDef>();
            mergeMeshes(meshes);

            // if there are no meshes, there's no model
            if (meshes.isEmpty()) {
                return null;
            }

            // create visible mesh list
            ArrayList<ModelConfig.VisibleMesh> visible = new ArrayList<ModelConfig.VisibleMesh>();
            CollisionMesh collision = null;
            for (Map.Entry<Object, TriMeshDef> entry : meshes.entrySet()) {
                TriMeshDef mesh = entry.getValue();
                if ("collision".equals(entry.getKey())) {
                    collision = mesh.createCollisionMesh();
                } else {
                    visible.add(mesh.createVisibleMesh(config));
                }
            }

            // if there's no explicit collision mesh, create one from the normal meshes
            if (collision == null) {
                TriMeshDef mesh = new TriMeshDef();
                mergeMeshes(mesh);
                collision = mesh.createCollisionMesh();
            }

            // create and return the set
            return new ModelConfig.MeshSet(
                visible.toArray(new ModelConfig.VisibleMesh[visible.size()]), collision);
        }

        /**
         * Updates the supplied configuration with the contents of this node.
         */
        public void update (ArticulatedConfig config)
        {
            clearTransforms(config);

            // merge skin meshes
            HashMap<Object, SkinMeshDef> meshes = new HashMap<Object, SkinMeshDef>();
            HashSet<String> bones = new HashSet<String>();
            mergeSkinMeshes(meshes, bones);

            // create the transformation hierarchy
            boolean haveCollisionMesh = containsCollisionMesh();
            config.root = createNode(config, haveCollisionMesh);

            // create the skin meshes
            ArrayList<ModelConfig.VisibleMesh> visible = new ArrayList<ModelConfig.VisibleMesh>();
            CollisionMesh collision = null;
            for (Map.Entry<Object, SkinMeshDef> entry : meshes.entrySet()) {
                SkinMeshDef mesh = entry.getValue();
                if ("collision".equals(entry.getKey())) {
                    collision = mesh.createCollisionMesh();
                } else {
                    mesh.createSkinMeshes(config, visible);
                }
            }

            // if there's no explicit collision mesh, create one from the skinned meshes
            if (!haveCollisionMesh && !visible.isEmpty()) {
                TriMeshDef mesh = new TriMeshDef();
                mergeSkinMeshes(mesh);
                collision = mesh.createCollisionMesh();
            }

            // initialize the skin
            config.skin = new ModelConfig.MeshSet(
                visible.toArray(new ModelConfig.VisibleMesh[visible.size()]), collision);

            // update transient fields
            config.initTransientFields();
        }

        /**
         * Clears the transforms according to the supplied config.
         */
        protected void clearTransforms (ModelConfig.Imported config)
        {
            // clear to the global scale value
            clearTransform(config.scale);

            // if the "ignore root transforms" flag is set, clear child transforms
            if (config.ignoreRootTransforms) {
                for (SpatialDef child : childDefs) {
                    child.clearTransform(1f);
                }
            }
        }

        /**
         * Finds the names of all nodes referenced as bones, merges skin meshes with the same
         * properties.
         */
        public void mergeSkinMeshes (HashMap<Object, SkinMeshDef> meshes, HashSet<String> bones)
        {
            for (SpatialDef childDef : childDefs) {
                childDef.mergeSkinMeshes(meshes, bones);
            }
        }

        /**
         * Removes influences from the named bone.
         */
        public void removeBoneWeights (String bone)
        {
            for (SpatialDef childDef : childDefs) {
                childDef.removeBoneWeights(bone);
            }
        }

        /**
         * Merges all of the skin meshes under this node.
         */
        public void mergeSkinMeshes (TriMeshDef mesh)
        {
            for (SpatialDef child : childDefs) {
                child.mergeSkinMeshes(mesh);
            }
        }

        /**
         * Creates an articulation node.
         */
        public abstract ArticulatedConfig.Node createNode (
            ArticulatedConfig config, boolean haveCollisionMesh);

        /**
         * Creates nodes for the children of this one.
         */
        public ArticulatedConfig.Node[] createChildNodes (
            ArticulatedConfig config, boolean haveCollisionMesh)
        {
            ArticulatedConfig.Node[] children = new ArticulatedConfig.Node[childDefs.size()];
            for (int ii = 0; ii < children.length; ii++) {
                children[ii] = childDefs.get(ii).createNode(config, haveCollisionMesh);
            }
            return children;
        }

        /**
         * Merges meshes with the same properties under this node.
         */
        public void mergeMeshes (HashMap<Object, TriMeshDef> meshes)
        {
            for (SpatialDef child : childDefs) {
                child.mergeMeshes(meshes);
            }
        }

        /**
         * Merges all of the meshes under this node.
         */
        public void mergeMeshes (TriMeshDef mesh)
        {
            for (SpatialDef child : childDefs) {
                child.mergeMeshes(mesh);
            }
        }

        /**
         * Returns the transform matrix (which includes the parent matrix).
         */
        public Matrix4f getTransformMatrix ()
        {
            if (_transformMatrix == null) {
                _transformMatrix = createMatrix(translation, rotation, scale);
                if (parentDef != null) {
                    parentDef.getTransformMatrix().mult(_transformMatrix, _transformMatrix);
                }
            }
            return _transformMatrix;
        }

        /**
         * Sets the transform to a simple uniform scale.
         */
        protected void clearTransform (float scale)
        {
            translation = new float[] { 0f, 0f, 0f };
            rotation = new float[] { 0f, 0f, 0f, 1f };
            this.scale = new float[] { scale, scale, scale };
        }

        /**
         * Determines whether this node contains an explicit collision mesh.
         */
        protected boolean containsCollisionMesh ()
        {
            for (SpatialDef child : childDefs) {
                if (child.containsCollisionMesh()) {
                    return true;
                }
            }
            return false;
        }

        /** The lazily initialized transform matrix. */
        protected Matrix4f _transformMatrix;
    }

    /**
     * Represents a node without geometry.
     */
    public static class NodeDef extends SpatialDef
    {
        @Override
        public ArticulatedConfig.Node createNode (
            ArticulatedConfig config, boolean haveCollisionMesh)
        {
            ArticulatedConfig.Node[] children = createChildNodes(config, haveCollisionMesh);
            Transform3D transform = (parentDef == null) ?
                new Transform3D() : createTransform(translation, rotation, scale, config.scale);
            return new ArticulatedConfig.Node(name, transform, children);
        }
    }

    /**
     * Represents a triangle mesh.
     */
    public static class TriMeshDef extends SpatialDef
    {
        /** The geometry offset transform. */
        public float[] offsetTranslation;
        public float[] offsetRotation;
        public float[] offsetScale;

        /** The texture of the mesh, if any. */
        public String texture = "";

        /** The mesh's tag, if any. */
        public String tag;

        /** The vertices of the mesh. */
        public HashArrayList<Vertex> vertices = new HashArrayList<Vertex>();

        /** The triangle indices. */
        public ArrayList<Integer> indices = new ArrayList<Integer>();

        /** Whether or not any of the vertices are non-white. */
        public boolean colored;

        /**
         * Called by the parser to add a vertex to this mesh.
         */
        public void addVertex (Vertex vertex)
        {
            int idx = vertices.indexOf(vertex);
            if (idx != -1) {
                indices.add(idx);
            } else {
                indices.add(vertices.size());
                vertices.add(vertex);
                float[] color = vertex.color;
                colored |= (color != null &&
                    !(color[0] == 1f && color[1] == 1f && color[2] == 1f && color[3] == 1f));
            }
        }

        @Override
        public ArticulatedConfig.Node createNode (
            ArticulatedConfig config, boolean haveCollisionMesh)
        {
            // transform by offset matrix
            transformVertices(
                createMatrix(offsetTranslation, offsetRotation, offsetScale, config.scale));

            // create the node with the appropriate meshes
            boolean isCollisionMesh = name.contains("collision");
            return new ArticulatedConfig.MeshNode(
                name,
                createTransform(translation, rotation, scale, config.scale),
                createChildNodes(config, haveCollisionMesh),
                isCollisionMesh ? null : createVisibleMesh(config),
                (haveCollisionMesh && !isCollisionMesh) ? null : createCollisionMesh());
        }

        @Override
        public void mergeMeshes (HashMap<Object, TriMeshDef> meshes)
        {
            // multiply world times offset to form vertex transform
            Matrix4f vtrans = getTransformMatrix().mult(
                createMatrix(offsetTranslation, offsetRotation, offsetScale));

            // transform to flatten
            transformVertices(vtrans);

            // add a new entry if necessary; otherwise, merge with existing
            Object key = getConfigurationKey();
            TriMeshDef omesh = meshes.get(key);
            if (omesh == null) {
                meshes.put(key, this);
            } else {
                omesh.merge(this);
            }

            // merge any children
            super.mergeMeshes(meshes);
        }

        @Override
        public void mergeMeshes (TriMeshDef mesh)
        {
            // merge with existing
            mesh.merge(this);

            // merge any children
            super.mergeMeshes(mesh);
        }

        /**
         * Returns a key representing the configuration of this mesh that will be compared to
         * others in order to determine which meshes can be merged.
         */
        public Object getConfigurationKey ()
        {
            if (name.contains("collision")) {
                return "collision";
            } else {
                return new ArrayKey(getClass(), texture, tag);
            }
        }

        /**
         * Merges another mesh (assumed to have the same configuration, transform, etc.) into this
         * one.
         */
        public void merge (TriMeshDef omesh)
        {
            for (int idx : omesh.indices) {
                addVertex(omesh.vertices.get(idx));
            }
        }

        /**
         * Creates and returns a visible mesh config for this mesh.
         */
        public ModelConfig.VisibleMesh createVisibleMesh (ModelConfig.Imported config)
        {
            // if non-colored, clear out the color information
            if (!colored) {
                for (Vertex vertex : vertices) {
                    vertex.color = null;
                }
            }

            // optimize the vertex order
            optimizeVertexOrder(config.generateTangents);

            // create the base arrays
            AttributeArrayConfig[] vertexAttribArrays = createVertexAttribArrays(config);
            ClientArrayConfig[] texCoordArrays = new ClientArrayConfig[
                vertices.isEmpty() ? 1 : (vertices.get(0).extras.size() + 1)];
            for (int ii = 0; ii < texCoordArrays.length; ii++) {
                texCoordArrays[ii] = new ClientArrayConfig(2);
            }
            ClientArrayConfig colorArray = colored ? new ClientArrayConfig(4) : null;
            ClientArrayConfig normalArray = new ClientArrayConfig(3);
            ClientArrayConfig vertexArray = new ClientArrayConfig(3);

            // put them all in a list
            ArrayList<ClientArrayConfig> arrays = new ArrayList<ClientArrayConfig>();
            if (vertexAttribArrays != null) {
                Collections.addAll(arrays, vertexAttribArrays);
            }
            Collections.addAll(arrays, texCoordArrays);
            if (colorArray != null) {
                arrays.add(colorArray);
            }
            arrays.add(normalArray);
            arrays.add(vertexArray);

            // compute the offsets and stride
            int offset = 0;
            for (ClientArrayConfig array : arrays) {
                array.offset = offset;
                offset += array.getElementBytes();
            }

            // allocate the buffer and update the arrays
            int stride = offset;
            int vsize = stride / 4;
            FloatBuffer vbuf = BufferUtils.createFloatBuffer(vertices.size() * vsize);
            for (ClientArrayConfig array : arrays) {
                array.stride = stride;
                array.floatArray = vbuf;
            }

            // compute the bounds
            Box bounds = new Box(Vector3f.MAX_VALUE, Vector3f.MIN_VALUE);
            for (Vertex vertex : vertices) {
                bounds.addLocal(new Vector3f(vertex.location));
            }
            bounds.expandLocal(
                config.boundsExpansion, config.boundsExpansion, config.boundsExpansion);

            // create and return the mesh
            return new ModelConfig.VisibleMesh(
                texture, StringUtil.isBlank(tag) ? getDefaultTag() : tag,
                createGeometry(
                    bounds, vertexAttribArrays, texCoordArrays,
                    colorArray, normalArray, vertexArray));
        }

        /**
         * Creates and returns a collision mesh object for this mesh.
         */
        public CollisionMesh createCollisionMesh ()
        {
            // get the locations of all the vertices
            Vector3f[] vectors = new Vector3f[indices.size()];
            for (int ii = 0; ii < vectors.length; ii++) {
                Vertex vertex = vertices.get(indices.get(ii));
                vectors[ii] = new Vector3f(vertex.location);
            }
            return new CollisionMesh(vectors);
        }

        /**
         * Creates the attribute arrays.
         */
        protected AttributeArrayConfig[] createVertexAttribArrays (ModelConfig.Imported config)
        {
            return config.generateTangents ?
                new AttributeArrayConfig[] { new AttributeArrayConfig(3, "tangent") } : null;
        }

        /**
         * Returns the default tag for this mesh type.
         */
        protected String getDefaultTag ()
        {
            return ModelConfig.DEFAULT_TAG;
        }

        /**
         * Creates the geometry for this node.
         */
        protected GeometryConfig createGeometry (
            Box bounds, AttributeArrayConfig[] vertexAttribArrays,
            ClientArrayConfig[] texCoordArrays, ClientArrayConfig colorArray,
            ClientArrayConfig normalArray, ClientArrayConfig vertexArray)
        {
            // get the vertex data
            FloatBuffer vbuf = vertexArray.floatArray;
            for (Vertex vertex : vertices) {
                vertex.get(vbuf);
            }
            vbuf.rewind();

            // create the geometry
            return new GeometryConfig.IndexedStored(
                bounds, GeometryConfig.Mode.TRIANGLES, vertexAttribArrays, texCoordArrays,
                colorArray, normalArray, vertexArray, 0, vertices.size() - 1, createIndexBuffer());
        }

        @Override
        protected boolean containsCollisionMesh ()
        {
            return name.contains("collision") || super.containsCollisionMesh();
        }

        /**
         * Transforms all of the mesh's vertices by the specified matrix.
         */
        protected void transformVertices (Matrix4f vtrans)
        {
            // the normal matrix is the inverse transpose of the vertex matrix
            Matrix4f ntrans = vtrans.invertAffine().transposeLocal();

            // transform the vertices and normals
            for (Vertex vertex : vertices) {
                vertex.transform(vtrans, ntrans);
            }
        }

        /** Reorders the vertices to optimize for vertex cache utilization.  Uses the algorithm
         * described in Tom Forsyth's article
         * <a href="http://home.comcast.net/~tom_forsyth/papers/fast_vert_cache_opt.html">
         * Linear-Speed Vertex Cache Optimization</a>.
         */
        protected void optimizeVertexOrder (boolean generateTangents)
        {
            // start by compiling a list of triangles cross-linked with the vertices they use
            // (we use a linked hash set to ensure consistent iteration order for serialization)
            LinkedHashSet<Triangle> triangles = new LinkedHashSet<Triangle>();
            for (int ii = 0, nn = indices.size(); ii < nn; ii += 3) {
                Vertex[] tverts = new Vertex[] {
                    vertices.get(indices.get(ii)),
                    vertices.get(indices.get(ii + 1)),
                    vertices.get(indices.get(ii + 2))
                };
                Triangle triangle = new Triangle(tverts);
                for (Vertex tvert : tverts) {
                    if (tvert.triangles == null) {
                        tvert.triangles = new ArrayList<Triangle>();
                    }
                    tvert.triangles.add(triangle);
                }
                triangles.add(triangle);
            }

            // generate the tangents with that information
            if (generateTangents) {
                for (Vertex vertex : vertices) {
                    vertex.generateTangent();
                }
            }

            // init the scores
            for (Vertex vertex : vertices) {
                vertex.updateScore(Integer.MAX_VALUE);
            }

            // clear the vertices and indices to prepare for readdition
            vertices.clear();
            indices.clear();

            // while there are triangles remaining, keep adding the one with the best score
            // (as determined by its LRU cache position and number of remaining triangles)
            HashArrayList<Vertex> vcache = new HashArrayList<Vertex>();
            while (!triangles.isEmpty()) {
                // first look for triangles in the cache
                Triangle bestTriangle = null;
                float bestScore = -1f;
                for (Vertex vertex : vcache) {
                    for (Triangle triangle : vertex.triangles) {
                        float score = triangle.getScore();
                        if (score > bestScore) {
                            bestTriangle = triangle;
                            bestScore = score;
                        }
                    }
                }

                // if that didn't work, scan the full list
                if (bestTriangle == null) {
                    for (Triangle triangle : triangles) {
                        float score = triangle.getScore();
                        if (score > bestScore) {
                            bestTriangle = triangle;
                            bestScore = score;
                        }
                    }
                }

                // add and update the vertices from the best triangle
                triangles.remove(bestTriangle);
                for (Vertex vertex : bestTriangle.vertices) {
                    addVertex(vertex);
                    vertex.triangles.remove(bestTriangle);
                    vcache.remove(vertex);
                    vcache.add(0, vertex);
                }

                // update the scores of the vertices in the cache
                for (int ii = 0, nn = vcache.size(); ii < nn; ii++) {
                    vcache.get(ii).updateScore(ii);
                }

                // trim the excess (if any) from the end of the cache
                while (vcache.size() > 64) {
                    vcache.remove(vcache.size() - 1);
                }
            }
        }

        /**
         * Creates the index buffer for the mesh.
         */
        protected ShortBuffer createIndexBuffer ()
        {
            ShortBuffer ibuf = BufferUtils.createShortBuffer(indices.size());
            for (int idx : indices) {
                ibuf.put((short)idx);
            }
            ibuf.rewind();
            return ibuf;
        }
    }

    /**
     * Represents a triangle mesh whose vertices are influenced by sets of bones.
     */
    public static class SkinMeshDef extends TriMeshDef
    {
        /** The bones that influence this mesh. */
        public HashSet<String> mbones = new HashSet<String>();

        public SkinMeshDef ()
        {
        }

        public SkinMeshDef (SkinMeshDef other)
        {
            texture = other.texture;
            tag = other.tag;
            colored = other.colored;
        }

        @Override
        public void addVertex (Vertex vertex)
        {
            super.addVertex(vertex);
            mbones.addAll(((SkinVertex)vertex).getBones());
        }

        @Override
        public void mergeSkinMeshes (HashMap<Object, SkinMeshDef> meshes, HashSet<String> bones)
        {
            // multiply world times offset to form vertex transform
            Matrix4f vtrans = getTransformMatrix().mult(
                createMatrix(offsetTranslation, offsetRotation, offsetScale));

            // transform to flatten
            transformVertices(vtrans);

            // add a new entry if necessary; otherwise, merge with existing
            Object key = getConfigurationKey();
            SkinMeshDef omesh = meshes.get(key);
            if (omesh == null) {
                meshes.put(key, this);
            } else {
                omesh.merge(this);
            }

            // add all the bones
            bones.addAll(mbones);

            // merge any children
            super.mergeSkinMeshes(meshes, bones);
        }

        @Override
        public void removeBoneWeights (String bone)
        {
            super.removeBoneWeights(bone);
            for (Vertex vertex : vertices) {
                ((SkinVertex)vertex).boneWeights.remove(bone);
            }
        }

        @Override
        public void mergeSkinMeshes (TriMeshDef mesh)
        {
            // merge with existing
            mesh.merge(this);

            // merge any children
            super.mergeSkinMeshes(mesh);
        }

        @Override
        public ArticulatedConfig.Node createNode (
            ArticulatedConfig config, boolean haveCollisionMesh)
        {
            return new ArticulatedConfig.Node(
                name,
                createTransform(translation, rotation, scale, config.scale),
                createChildNodes(config, haveCollisionMesh));
        }

        /**
         * Creates a set of skin meshes based on this definition and adds them to the list
         * provided.
         */
        public void createSkinMeshes (
            ArticulatedConfig config, ArrayList<ModelConfig.VisibleMesh> meshes)
        {
            // copy the vertices into a dummy mesh, adding a mesh to the list when we
            // reach the bone limit (or the end)
            SkinMeshDef mesh = new SkinMeshDef(this);
            for (int ii = 0, nn = indices.size(); ii < nn; ii += 3) {
                SkinVertex s1 = (SkinVertex)vertices.get(indices.get(ii)),
                    s2 = (SkinVertex)vertices.get(indices.get(ii+1)),
                    s3 = (SkinVertex)vertices.get(indices.get(ii+2));
                HashSet<String> tbones = new HashSet<String>();
                tbones.addAll(s1.getBones());
                tbones.addAll(s2.getBones());
                tbones.addAll(s3.getBones());
                HashSet<String> nbones = new HashSet<String>(mesh.mbones);
                nbones.addAll(tbones);
                if (nbones.size() > MAX_BONE_COUNT) {
                    meshes.add(mesh.createVisibleMesh(config));
                    mesh = new SkinMeshDef(this);
                }
                mesh.addVertex(s1);
                mesh.addVertex(s2);
                mesh.addVertex(s3);
            }
            if (!mesh.indices.isEmpty()) {
                meshes.add(mesh.createVisibleMesh(config));
            }
        }

        @Override
        protected AttributeArrayConfig[] createVertexAttribArrays (ModelConfig.Imported config)
        {
            AttributeArrayConfig[] arrays = new AttributeArrayConfig[] {
                new AttributeArrayConfig(4, "boneIndices"),
                new AttributeArrayConfig(4, "boneWeights") };
            AttributeArrayConfig[] sarrays = super.createVertexAttribArrays(config);
            return (sarrays == null) ? arrays : ArrayUtil.concatenate(arrays, sarrays);
        }

        @Override
        protected String getDefaultTag ()
        {
            return ModelConfig.SKINNED_TAG;
        }

        @Override
        protected GeometryConfig createGeometry (
            Box bounds, AttributeArrayConfig[] vertexAttribArrays,
            ClientArrayConfig[] texCoordArrays, ClientArrayConfig colorArray,
            ClientArrayConfig normalArray, ClientArrayConfig vertexArray)
        {
            // find the names of all bones influencing the mesh
            String[] bones = mbones.toArray(new String[mbones.size()]);

            // get the vertex data
            FloatBuffer vbuf = vertexArray.floatArray;
            for (Vertex vertex : vertices) {
                ((SkinVertex)vertex).get(vbuf, bones);
            }
            vbuf.rewind();

            // create the geometry
            return new GeometryConfig.SkinnedIndexedStored(
                bounds, GeometryConfig.Mode.TRIANGLES, vertexAttribArrays, texCoordArrays,
                colorArray, normalArray, vertexArray, 0, vertices.size() - 1, createIndexBuffer(),
                bones);
        }
    }

    /**
     * A single vertex in a mesh.
     */
    public static class Vertex
    {
        /** The parameters of the vertex. */
        public float[] location;
        public float[] normal;
        public float[] tcoords;
        public float[] color;

        /** Extra data associated with the vertex. */
        public List<Extra> extras = Lists.newArrayList();

        /** When reordering vertices, the triangles containing the vertex. */
        public ArrayList<Triangle> triangles;

        /** When reordering vertices, the last computed score of the vertex. */
        public float score;

        /** The tangent vector, if computed. */
        public Vector3f tangent;

        /**
         * Called by the parser to add extra data to this vertex.
         */
        public void addExtra (Extra extra)
        {
            extras.add(extra);
        }

        /**
         * Transforms this vertex in-place by the given vertex and normal matrices.
         */
        public void transform (Matrix4f vtrans, Matrix4f ntrans)
        {
            vtrans.transformPointLocal(new Vector3f(location)).get(location);
            ntrans.transformVectorLocal(new Vector3f(normal)).normalizeLocal().get(normal);
        }

        /**
         * Generates the tangent vector for this vertex (after its triangle list has been
         * initialized).
         */
        public void generateTangent ()
        {
            tangent = new Vector3f();
            Vector3f normal = new Vector3f(this.normal), vec = new Vector3f();
            Quaternion rot = new Quaternion();
            for (Triangle triangle : triangles) {
                for (Vertex vertex : triangle.vertices) {
                    if (vertex == this) {
                        continue;
                    }
                    vec.set(
                        vertex.location[0] - location[0],
                        vertex.location[1] - location[1],
                        vertex.location[2] - location[2]).crossLocal(normal);
                    if (vec.length() < FloatMath.EPSILON) {
                        continue;
                    }
                    float angle = FloatMath.atan2(
                        vertex.tcoords[0] - tcoords[0], vertex.tcoords[1] - tcoords[1]);
                    rot.fromAngleAxis(angle, normal).transformLocal(vec).normalizeLocal();
                    tangent.addLocal(vec);
                }
            }
            if (tangent.length() > 0f) {
                tangent.normalizeLocal();
            }
        }

        /**
         * Puts the contents of this vertex into the specified buffer.
         */
        public void get (FloatBuffer vbuf)
        {
            if (tangent != null) {
                tangent.get(vbuf);
            }
            vbuf.put(tcoords);
            for (int ii = 0, nn = extras.size(); ii < nn; ii++) {
                vbuf.put(extras.get(ii).tcoords);
            }
            if (color != null) {
                vbuf.put(color);
            }
            vbuf.put(normal);
            vbuf.put(location);
        }

        /**
         * Updates this vertex's score based on its current position in the simulated cache
         * and the number of remaining triangles that include it.
         */
        public void updateScore (int cacheIdx)
        {
            float pscore;
            if (cacheIdx > 63) {
                pscore = 0f; // outside the cache
            } else if (cacheIdx < 3) {
                pscore = 0.75f; // the three most recent vertices
            } else {
                pscore = FloatMath.pow((63 - cacheIdx) / 60f, 1.5f);
            }
            score = pscore + 2f * FloatMath.pow(triangles.size(), -0.5f);
        }

        @Override
        public int hashCode ()
        {
            return Arrays.hashCode(location) ^ Arrays.hashCode(normal) ^
                Arrays.hashCode(tcoords) ^ Arrays.hashCode(color);
        }

        @Override
        public boolean equals (Object obj)
        {
            Vertex overt = (Vertex)obj;
            return Arrays.equals(location, overt.location) &&
                Arrays.equals(normal, overt.normal) &&
                Arrays.equals(tcoords, overt.tcoords) &&
                Arrays.equals(color, overt.color) &&
                extras.equals(overt.extras);
        }

        @Override
        public String toString ()
        {
            return StringUtil.toString(location);
        }
    }

    /**
     * An extra bit of data associated with a vertex.
     */
    public static class Extra
    {
        /** The extra texture coordinates. */
        public float[] tcoords;

        @Override
        public boolean equals (Object other)
        {
            return Arrays.equals(tcoords, ((Extra)other).tcoords);
        }

        @Override
        public int hashCode ()
        {
            return tcoords != null ? Arrays.hashCode(tcoords) : 0;
        }
    }

    /**
     * A vertex with bone influences.
     */
    public static class SkinVertex extends Vertex
    {
        /** The bones influencing the vertex, mapped by name. */
        public HashMap<String, BoneWeight> boneWeights = new HashMap<String, BoneWeight>();

        /**
         * Called by the parser to add a bone weight to this vertex.
         */
        public void addBoneWeight (BoneWeight weight)
        {
            if (weight.weight == 0f) {
                return;
            }
            BoneWeight bweight = boneWeights.get(weight.bone);
            if (bweight != null) {
                bweight.weight += weight.weight;
            } else {
                boneWeights.put(weight.bone, weight);
            }
        }

        /**
         * Returns a set containing the names of the bones influencing this vertex.  If there are
         * more than four, the extra influences will be removed and the weight will be
         * redistributed.
         */
        public Set<String> getBones ()
        {
            // if there are more than four weights, keep only the four greatest and adjust the
            // values such that the total weight remains the same
            if (boneWeights.size() > 4) {
                BoneWeight[] weights = boneWeights.values().toArray(
                    new BoneWeight[boneWeights.size()]);
                QuickSort.rsort(weights);
                float ototal = getTotalWeight();
                for (int ii = 4; ii < weights.length; ii++) {
                    boneWeights.remove(weights[ii].bone);
                }
                float scale = ototal / getTotalWeight();
                for (BoneWeight weight : boneWeights.values()) {
                    weight.weight *= scale;
                }
            }
            return boneWeights.keySet();
        }

        /**
         * Puts the contents of this vertex into the specified buffer.
         *
         * @param bones the list of bones influencing the mesh.
         */
        public void get (FloatBuffer vbuf, String[] bones)
        {
            // if there are more than four weights, limit the result to the four greatest;
            // if there are fewer, pad the values out with zero
            BoneWeight[] weights = boneWeights.values().toArray(
                new BoneWeight[boneWeights.size()]);
            QuickSort.rsort(weights);
            for (int ii = 0; ii < 4; ii++) {
                vbuf.put(ii < weights.length ? ListUtil.indexOf(bones, weights[ii].bone) : 0f);
            }
            for (int ii = 0; ii < 4; ii++) {
                vbuf.put(ii < weights.length ? weights[ii].weight : 0f);
            }
            super.get(vbuf);
        }

        @Override
        public boolean equals (Object obj)
        {
            return super.equals(obj) && boneWeights.equals(((SkinVertex)obj).boneWeights);
        }

        /**
         * Returns the total weight of all influences.
         */
        protected float getTotalWeight ()
        {
            float total = 0f;
            for (BoneWeight weight : boneWeights.values()) {
                total += weight.weight;
            }
            return total;
        }
    }

    /**
     * The influence of a single bone on a vertex.
     */
    public static class BoneWeight
        implements Comparable<BoneWeight>
    {
        /** The name of the influencing bone. */
        public String bone;

        /** The amount of influence. */
        public float weight;

        // documentation inherited from interface Comparable
        public int compareTo (BoneWeight other)
        {
            return Float.compare(weight, other.weight);
        }

        @Override
        public boolean equals (Object other)
        {
            return weight == ((BoneWeight)other).weight;
        }

        @Override
        public int hashCode ()
        {
            return Float.floatToIntBits(weight);
        }
        @Override
        public String toString ()
        {
            return bone + " " + weight;
        }
    }

    /**
     * Creates a {@link Transform3D} object from the supplied arrays.
     */
    public static Transform3D createTransform (
        float[] translation, float[] rotation, float[] scale, float gscale)
    {
        Vector3f trans = new Vector3f(translation).multLocal(gscale);
        Quaternion rot = new Quaternion(rotation);
        if (scale[0] != scale[1] || scale[1] != scale[2]) {
            return new Transform3D(trans, rot, new Vector3f(scale));
        } else if (scale[0] != 1f) {
            return new Transform3D(trans, rot, scale[0]);
        } else if (!(trans.equals(Vector3f.ZERO) && rot.equals(Quaternion.IDENTITY))) {
            return new Transform3D(trans, rot);
        } else {
            return new Transform3D();
        }
    }

    /** The nodes in the model, mapped by name. */
    public HashMap<String, SpatialDef> spatials = new HashMap<String, SpatialDef>();

    /**
     * Called by the parser to add a node to this model.
     */
    public void addSpatial (SpatialDef spatial)
    {
        spatials.put(spatial.name, spatial);
    }

    /**
     * Updates the supplied configuration with the model data in this definition.
     */
    public void update (StaticConfig config)
    {
        config.meshes = createRootNode().createMeshes(config);
    }

    /**
     * Updates the supplied configuration with the model data in this definition.
     */
    public void update (StaticSetConfig config)
    {
        // resolve parent/child references and find top-level children
        ArrayList<SpatialDef> tops = resolveReferences();

        // create and map a mesh set for each top-level child
        config.meshes = new TreeMap<String, ModelConfig.MeshSet>();
        for (SpatialDef top : tops) {
            ModelConfig.MeshSet meshes = top.createMeshes(config);
            if (meshes != null) {
                config.meshes.put(top.name, meshes);
            }
        }
    }

    /**
     * Updates the supplied configuration with the model data in this definition.
     */
    public void update (ArticulatedConfig config)
    {
        createRootNode().update(config);
    }

    /**
     * Resolves the node references and returns a root node containing the hierarchy.
     */
    protected NodeDef createRootNode ()
    {
        // resolve parent references and find top-level children
        ArrayList<SpatialDef> tops = resolveReferences();

        // create a top-level node to hold the entire model
        NodeDef node = new NodeDef();
        node.name = "%ROOT%";
        node.childDefs = tops;
        for (SpatialDef top : tops) {
            top.parentDef = node;
        }
        return node;
    }

    /**
     * Resolves the parent/child references and returns a list of the top-level children.
     */
    protected ArrayList<SpatialDef> resolveReferences ()
    {
        ArrayList<SpatialDef> tops = new ArrayList<SpatialDef>();
        for (SpatialDef spatial : spatials.values()) {
            if (spatial.parent == null) {
                tops.add(spatial);
            } else {
                spatial.parentDef = spatials.get(spatial.parent);
                if (spatial.parentDef != null) {
                    spatial.parentDef.childDefs.add(spatial);
                } else {
                    log.warning("Missing parent node [node=" + spatial.name + ", parent=" +
                        spatial.parent + "].");
                    tops.add(spatial);
                }
            }
        }
        return tops;
    }

    /**
     * Creates and returns a matrix containing the described transform.
     */
    protected static Matrix4f createMatrix (
        float[] translation, float[] rotation, float[] scale)
    {
        return createMatrix(translation, rotation, scale, 1f);
    }

    /**
     * Creates and returns a matrix containing the described transform.
     */
    protected static Matrix4f createMatrix (
        float[] translation, float[] rotation, float[] scale, float gscale)
    {
        Matrix4f matrix = new Matrix4f();
        return matrix.setToTransform(
            new Vector3f(translation).multLocal(gscale), new Quaternion(rotation),
            new Vector3f(scale).multLocal(gscale));
    }

    /**
     * Represents a triangle for processing purposes.
     */
    protected static class Triangle
    {
        /** The vertices of the triangle. */
        public Vertex[] vertices;

        public Triangle (Vertex[] vertices)
        {
            this.vertices = vertices;
        }

        /**
         * Returns the score of the triangle, which is simply the sum of the scores of its
         * vertices.
         */
        public float getScore ()
        {
            return vertices[0].score + vertices[1].score + vertices[2].score;
        }
    }

    /**
     * Accelerates {@link ArrayList#indexOf}, {@link ArrayList#contains}, and
     * {@link ArrayList#remove} using an internal hash map (assumes that all elements of the list
     * are unique and non-null).
     */
    protected static class HashArrayList<E> extends ArrayList<E>
    {
        @Override
        public boolean add (E element)
        {
            add(size(), element);
            return true;
        }

        @Override
        public void add (int idx, E element)
        {
            super.add(idx, element);
            remapFrom(idx);
        }

        @Override
        public E remove (int idx)
        {
            E element = super.remove(idx);
            _indices.remove(element);
            remapFrom(idx);
            return element;
        }

        @Override
        public void clear ()
        {
            super.clear();
            _indices.clear();
        }

        @Override
        public int indexOf (Object obj)
        {
            Integer idx = _indices.get(obj);
            return (idx == null ? -1 : idx);
        }

        @Override
        public boolean contains (Object obj)
        {
            return _indices.containsKey(obj);
        }

        @Override
        public boolean remove (Object obj)
        {
            Integer idx = _indices.remove(obj);
            if (idx != null) {
                super.remove(idx);
                return true;
            } else {
                return false;
            }
        }

        protected void remapFrom (int idx)
        {
            for (int ii = idx, nn = size(); ii < nn; ii++) {
                _indices.put(get(ii), ii);
            }
        }

        /** Maps elements to their indices in the list. */
        protected HashMap<Object, Integer> _indices = new HashMap<Object, Integer>();
    }

    /** The maximum number of bones that may influence a single mesh. */
    protected static final int MAX_BONE_COUNT = 31;
}
