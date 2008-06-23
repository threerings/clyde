//
// $Id$

package com.threerings.opengl.model.tools;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.lwjgl.BufferUtils;

import com.samskivert.util.ListUtil;
import com.samskivert.util.PropertiesUtil;
import com.samskivert.util.StringUtil;
import com.samskivert.util.QuickSort;

import com.threerings.math.Box;
import com.threerings.math.FloatMath;
import com.threerings.math.Matrix4f;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.model.ArticulatedModel;
import com.threerings.opengl.model.ArticulatedModel.Node;
import com.threerings.opengl.model.CollisionMesh;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.SkinMesh;
import com.threerings.opengl.model.StaticModel;
import com.threerings.opengl.model.VisibleMesh;
import com.threerings.opengl.util.GlUtil;

import static com.threerings.opengl.Log.*;

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

        /** Whether or not this node is used as a bone. */
        public boolean bone;

        /** Whether or not this node is used as an attachment point. */
        public boolean point;

        /**
         * Creates a model containing the meshes in this node.
         */
        public Model createModel (HashMap<String, SpatialDef> spatials, Properties props)
        {
            // read the global scale value
            float gscale = Float.parseFloat(props.getProperty("scale", "0.01"));
            clearTransform(gscale);

            // if the "ignore root transforms" flag is set, clear child transforms
            if (Boolean.parseBoolean(props.getProperty("ignore_root_transforms"))) {
                for (SpatialDef child : childDefs) {
                    child.clearTransform(1f);
                }
            }

            // use the type property to determine what kind of model to create
            return "articulated".equals(props.getProperty("type")) ?
                createArticulatedModel(spatials, props) : createStaticModel(props);
        }

        /**
         * Creates an articulated model (where the transformation hierarchy is preserved).
         */
        public ArticulatedModel createArticulatedModel (
            HashMap<String, SpatialDef> spatials, Properties props)
        {
            // merge skin meshes
            HashMap<Object, SkinMeshDef> meshes = new HashMap<Object, SkinMeshDef>();
            HashSet<String> bones = new HashSet<String>();
            mergeSkinMeshes(meshes, bones, props);

            // flag bone nodes, remove influences from missing ones
            for (String bone : bones) {
                SpatialDef bdef = spatials.get(bone);
                if (bdef == null) {
                    log.warning("Missing referenced bone [name=" + bone + "].");
                    removeBoneWeights(bone);
                } else {
                    bdef.bone = true;
                }
            }

            // flag attachment points
            String[] points = StringUtil.parseStringArray(
                props.getProperty("attachment_points", ""));
            for (String point : points) {
                SpatialDef pdef = spatials.get(point);
                if (pdef == null) {
                    log.warning("Missing attachment point [name=" + point + "].");
                } else {
                    pdef.point = true;
                }
            }

            // create the transformation hierarchy
            ArticulatedModel model = new ArticulatedModel(props);
            boolean haveCollisionMesh = containsCollisionMesh();
            Node root = createNode(model, scale[0], haveCollisionMesh);

            // create the skin meshes
            ArrayList<SkinMesh> smeshes = new ArrayList<SkinMesh>();
            CollisionMesh cmesh = null;
            for (Map.Entry<Object, SkinMeshDef> entry : meshes.entrySet()) {
                SkinMeshDef mesh = entry.getValue();
                if ("collision".equals(entry.getKey())) {
                    cmesh = mesh.createCollisionMesh();
                } else {
                    mesh.createSkinMeshes(smeshes);
                }
            }

            // if there's no explicit collision mesh, create one from the skinned meshes
            if (!haveCollisionMesh && !smeshes.isEmpty()) {
                TriMeshDef mesh = new TriMeshDef();
                mergeSkinMeshes(mesh);
                cmesh = mesh.createCollisionMesh();
            }

            model.setData(root, smeshes.toArray(new SkinMesh[smeshes.size()]), cmesh);
            return (root == null) ? null : model;
        }

        /**
         * Finds the names of all nodes referenced as bones, merges skin meshes with the same
         * properties.
         */
        public void mergeSkinMeshes (
            HashMap<Object, SkinMeshDef> meshes, HashSet<String> bones, Properties props)
        {
            for (SpatialDef childDef : childDefs) {
                childDef.mergeSkinMeshes(meshes, bones, props);
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
        public abstract Node createNode (
            ArticulatedModel model, float gscale, boolean haveCollisionMesh);

        /**
         * Creates nodes for the children of this one.
         */
        public Node[] createChildNodes (
            ArticulatedModel model, float gscale, boolean haveCollisionMesh)
        {
            ArrayList<Node> children = new ArrayList<Node>();
            for (SpatialDef childDef : childDefs) {
                Node child = childDef.createNode(model, gscale, haveCollisionMesh);
                if (child != null) {
                    children.add(child);
                }
            }
            return children.toArray(new Node[children.size()]);
        }

        /**
         * Creates a static model (where all meshes share the same transform).
         */
        public StaticModel createStaticModel (Properties props)
        {
            // flatten transforms and merge meshes with same properties
            HashMap<Object, TriMeshDef> meshes = new HashMap<Object, TriMeshDef>();
            mergeMeshes(meshes, props);

            // if there are no meshes, there's no model
            if (meshes.isEmpty()) {
                return null;
            }

            // create visible mesh list
            ArrayList<VisibleMesh> vmeshes = new ArrayList<VisibleMesh>();
            CollisionMesh cmesh = null;
            for (Map.Entry<Object, TriMeshDef> entry : meshes.entrySet()) {
                TriMeshDef mesh = entry.getValue();
                if ("collision".equals(entry.getKey())) {
                    cmesh = mesh.createCollisionMesh();
                } else {
                    vmeshes.add(mesh.createVisibleMesh());
                }
            }

            // if there's no explicit collision mesh, create one from the normal meshes
            if (cmesh == null) {
                TriMeshDef mesh = new TriMeshDef();
                mergeMeshes(mesh);
                cmesh = mesh.createCollisionMesh();
            }

            // create and return the model
            return new StaticModel(
                props, vmeshes.toArray(new VisibleMesh[vmeshes.size()]), cmesh);
        }

        /**
         * Merges meshes with the same properties under this node.
         */
        public void mergeMeshes (HashMap<Object, TriMeshDef> meshes, Properties props)
        {
            for (SpatialDef child : childDefs) {
                child.mergeMeshes(meshes, props);
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
        @Override // documentation inherited
        public Node createNode (ArticulatedModel model, float gscale, boolean haveCollisionMesh)
        {
            Node[] children = createChildNodes(model, gscale, haveCollisionMesh);
            Transform3D transform = (parentDef == null) ?
                new Transform3D() : createTransform(translation, rotation, scale, gscale);
            return (children.length == 0 && !(bone || point)) ?
                null : model.createNode(name, bone, transform, children);
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

        /** Whether or not the mesh allows back face culling. */
        public boolean solid;

        /** The texture of the mesh, if any. */
        public String texture;

        /** Whether or not the mesh is (partially) transparent. */
        public boolean transparent;

        /** The vertices of the mesh. */
        public HashArrayList<Vertex> vertices = new HashArrayList<Vertex>();

        /** The triangle indices. */
        public ArrayList<Integer> indices = new ArrayList<Integer>();

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
            }
        }

        @Override // documentation inherited
        public Node createNode (ArticulatedModel model, float gscale, boolean haveCollisionMesh)
        {
            // transform by offset matrix
            transformVertices(
                createMatrix(offsetTranslation, offsetRotation, offsetScale, gscale));

            // create the node with the appropriate meshes
            boolean isCollisionMesh = name.contains("collision");
            return model.createNode(
                name,
                bone,
                createTransform(translation, rotation, scale, gscale),
                createChildNodes(model, gscale, haveCollisionMesh),
                isCollisionMesh ? null : createVisibleMesh(),
                (haveCollisionMesh && !isCollisionMesh) ? null : createCollisionMesh());
        }

        @Override // documentation inherited
        public void mergeMeshes (HashMap<Object, TriMeshDef> meshes, Properties props)
        {
            // multiply world times offset to form vertex transform
            Matrix4f vtrans = getTransformMatrix().mult(
                createMatrix(offsetTranslation, offsetRotation, offsetScale));

            // transform to flatten
            transformVertices(vtrans);

            // add a new entry if necessary; otherwise, merge with existing
            Object key = getConfigurationKey(props);
            TriMeshDef omesh = meshes.get(key);
            if (omesh == null) {
                meshes.put(key, this);
            } else {
                omesh.merge(this);
            }

            // merge any children
            super.mergeMeshes(meshes, props);
        }

        @Override // documentation inherited
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
        public Object getConfigurationKey (Properties props)
        {
            if (name.contains("collision")) {
                return "collision";
            } else {
                return GlUtil.createKey(getClass(), solid, texture, transparent,
                    PropertiesUtil.getSubProperties(props, name));
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
         * Creates and returns a model mesh object for this mesh.
         */
        public VisibleMesh createVisibleMesh ()
        {
            // optimize the vertex order
            optimizeVertexOrder();

            // populate the vertex buffer
            Box bounds = new Box(Vector3f.MAX_VALUE, Vector3f.MIN_VALUE);
            FloatBuffer vbuf = BufferUtils.createFloatBuffer(vertices.size() * 8);
            for (Vertex vertex : vertices) {
                vertex.get(vbuf);
                bounds.addLocal(new Vector3f(vertex.location));
            }
            vbuf.rewind();

            // create and return the mesh
            return new VisibleMesh(texture, solid, bounds, vbuf, createIndexBuffer());
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

        @Override // documentation inherited
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
        protected void optimizeVertexOrder ()
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
            solid = other.solid;
            texture = other.texture;
            transparent = other.transparent;
        }

        @Override // documentation inherited
        public void addVertex (Vertex vertex)
        {
            super.addVertex(vertex);
            mbones.addAll(((SkinVertex)vertex).getBones());
        }

        @Override // documentation inherited
        public void mergeSkinMeshes (
            HashMap<Object, SkinMeshDef> meshes, HashSet<String> bones, Properties props)
        {
            // multiply world times offset to form vertex transform
            Matrix4f vtrans = getTransformMatrix().mult(
                createMatrix(offsetTranslation, offsetRotation, offsetScale));

            // transform to flatten
            transformVertices(vtrans);

            // add a new entry if necessary; otherwise, merge with existing
            Object key = getConfigurationKey(props);
            SkinMeshDef omesh = meshes.get(key);
            if (omesh == null) {
                meshes.put(key, this);
            } else {
                omesh.merge(this);
            }

            // add all the bones
            bones.addAll(mbones);

            // merge any children
            super.mergeSkinMeshes(meshes, bones, props);
        }

        @Override // documentation inherited
        public void removeBoneWeights (String bone)
        {
            super.removeBoneWeights(bone);
            for (Vertex vertex : vertices) {
                ((SkinVertex)vertex).boneWeights.remove(bone);
            }
        }

        @Override // documentation inherited
        public void mergeSkinMeshes (TriMeshDef mesh)
        {
            // merge with existing
            mesh.merge(this);

            // merge any children
            super.mergeSkinMeshes(mesh);
        }

        @Override // documentation inherited
        public Node createNode (ArticulatedModel model, float gscale, boolean haveCollisionMesh)
        {
            Node[] children = createChildNodes(model, gscale, haveCollisionMesh);
            return (children.length == 0 && !bone) ? null :
                model.createNode(
                    name, bone, createTransform(translation, rotation, scale, gscale), children);
        }

        /**
         * Creates a set of skin meshes based on this definition and adds them to the list
         * provided.
         */
        public void createSkinMeshes (ArrayList<SkinMesh> smeshes)
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
                if (nbones.size() > SkinMesh.MAX_BONE_COUNT) {
                    smeshes.add((SkinMesh)mesh.createVisibleMesh());
                    mesh = new SkinMeshDef(this);
                }
                mesh.addVertex(s1);
                mesh.addVertex(s2);
                mesh.addVertex(s3);
            }
            if (!mesh.indices.isEmpty()) {
                smeshes.add((SkinMesh)mesh.createVisibleMesh());
            }
        }

        @Override // documentation inherited
        public VisibleMesh createVisibleMesh ()
        {
            // optimize the vertex order
            optimizeVertexOrder();

            // find the names of all bones influencing the mesh
            String[] bones = mbones.toArray(new String[mbones.size()]);

            // populate the vertex buffer
            FloatBuffer vbuf = BufferUtils.createFloatBuffer(vertices.size() * 16);
            Box bounds = new Box(Vector3f.MAX_VALUE, Vector3f.MIN_VALUE);
            for (Vertex vertex : vertices) {
                ((SkinVertex)vertex).get(vbuf, bones);
                bounds.addLocal(new Vector3f(vertex.location));
            }
            vbuf.rewind();

            // create and return the mesh
            return new SkinMesh(texture, solid, bounds, vbuf, createIndexBuffer(), bones);
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

        /** When reordering vertices, the triangles containing the vertex. */
        public ArrayList<Triangle> triangles;

        /** When reordering vertices, the last computed score of the vertex. */
        public float score;

        /**
         * Transforms this vertex in-place by the given vertex and normal matrices.
         */
        public void transform (Matrix4f vtrans, Matrix4f ntrans)
        {
            vtrans.transformPointLocal(new Vector3f(location)).get(location);
            ntrans.transformVectorLocal(new Vector3f(normal)).normalizeLocal().get(normal);
        }

        /**
         * Puts the contents of this vertex into the specified buffer.
         */
        public void get (FloatBuffer vbuf)
        {
            vbuf.put(tcoords);
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

        @Override // documentation inherited
        public int hashCode ()
        {
            return Arrays.hashCode(location) ^ Arrays.hashCode(normal) ^ Arrays.hashCode(tcoords);
        }

        @Override // documentation inherited
        public boolean equals (Object obj)
        {
            Vertex overt = (Vertex)obj;
            return Arrays.equals(location, overt.location) &&
                Arrays.equals(normal, overt.normal) &&
                Arrays.equals(tcoords, overt.tcoords);
        }

        @Override // documentation inherited
        public String toString ()
        {
            return StringUtil.toString(location);
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
            vbuf.put(tcoords);
            vbuf.put(normal);
            vbuf.put(location);
        }

        @Override // documentation inherited
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

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return weight == ((BoneWeight)other).weight;
        }

        @Override // documentation inherited
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
     * Builds a {@link Model} from this definition and the supplied properties.
     */
    public Model createModel (Properties props)
    {
        // resolve parent references and find top-level children
        ArrayList<SpatialDef> tops = resolveReferences();

        // create a top-level node to hold the entire model
        NodeDef node = new NodeDef();
        node.childDefs = tops;
        for (SpatialDef top : tops) {
            top.parentDef = node;
        }

        // create and return the model
        return node.createModel(spatials, props);
    }

    /**
     * Builds a set of {@link Model}s (mapped by name) from this definition and the supplied
     * properties.
     */
    public HashMap<String, Model> createModelSet (Properties props)
    {
        // resolve parent/child references and find top-level children
        ArrayList<SpatialDef> tops = resolveReferences();

        // create and map a model for each top-level child
        HashMap<String, Model> models = new HashMap<String, Model>();
        for (SpatialDef top : tops) {
            Properties tprops = new Properties();
            tprops.putAll(props);
            tprops.putAll(PropertiesUtil.getSubProperties(props, top.name));
            Model model = top.createModel(spatials, tprops);
            if (model != null) {
                models.put(top.name, model);
            }
        }
        return models;
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
        @Override // documentation inherited
        public boolean add (E element)
        {
            add(size(), element);
            return true;
        }

        @Override // documentation inherited
        public void add (int idx, E element)
        {
            super.add(idx, element);
            remapFrom(idx);
        }

        @Override // documentation inherited
        public E remove (int idx)
        {
            E element = super.remove(idx);
            _indices.remove(element);
            remapFrom(idx);
            return element;
        }

        @Override // documentation inherited
        public void clear ()
        {
            super.clear();
            _indices.clear();
        }

        @Override // documentation inherited
        public int indexOf (Object obj)
        {
            Integer idx = _indices.get(obj);
            return (idx == null ? -1 : idx);
        }

        @Override // documentation inherited
        public boolean contains (Object obj)
        {
            return _indices.containsKey(obj);
        }

        @Override // documentation inherited
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
}
