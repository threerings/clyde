package com.threerings.opengl.model.tools.fbx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.samskivert.util.Logger;
import com.samskivert.util.StringUtil;

import com.google.common.io.Files;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;

import com.lukaseichberg.fbxloader.FBXFile;
import com.lukaseichberg.fbxloader.FBXLoader;
import com.lukaseichberg.fbxloader.FBXNode;

import com.threerings.math.Quaternion;

import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.model.tools.ModelDef;

import static com.threerings.opengl.Log.log;

public class ModelFbxParser extends AbstractFbxParser
{
    /**
     * Parse the model as well as extract any textures in the fbx into the specified directory.
     *
     * @param messages if provided, is populated with a list of import messages.
     */
    public static ModelDef parseModel (InputStream in, File dir, @Nullable List<String> messages)
        throws IOException
    {
        try {
            return new ModelFbxParser().parse(in, dir, messages);
        } catch (IOException ioe) {
            messages.add(ioe.getMessage());
            throw ioe;
        } catch (RuntimeException re) {
            messages.add(re.getMessage());
            throw re;
        }
    }

    /**
     * I'm actually not a fan of having anything be static but until I see a need to subclass
     * this I guess I'll do it to enforce in the API that the actual instance is not
     * thread-safe and will contain much garbage after parsing the model. Therefore, the
     * instance isn't directly instantiable and you access this via the public static method
     * above.
     */
    protected ModelFbxParser () { /* do not instantiate directly */ }

    protected ModelDef parse (InputStream in, File dir, @Nullable List<String> messages)
        throws IOException
    {
        FBXFile fbx = FBXLoader.loadFBXFile("model", in);
        //FbxDumper.Dump(fbx);

        // read and populate the connections
        populateConnections(fbx);

        FBXNode objects = fbx.getRootNode().getChildByName("Objects");

        // pre-populate some objects by id
        populateObjects(objects, "Deformer", "Geometry", "Material", "Texture", "Video");

        ModelDef model = new ModelDef();

        // Go through all the Models, meshes last
        List<FBXNode> models = objects.getChildrenByName("Model");
        Collections.sort(models, new ModelNodeComparator());

        // parse nodes
        Map<FBXNode, String> textures = Maps.newHashMap();
        for (FBXNode node : models) {
            // see what kind of model it is
            Long id = node.getData(0);
            String name = sanitizeName(node.<String>getData(1));
            String type = node.getData(2);
            ModelDef.SpatialDef spat;
            boolean isRoot = false;
            if ("LimbNode".equals(type) || "Null".equals(type)) {
                spat = new ModelDef.NodeDef();

            } else if ("Root".equals(type)) {
                spat = new ModelDef.NodeDef();
                isRoot = true;

            } else if ("Mesh".equals(type)) {
                FBXNode geom = findNodeToDest(id, "Geometry");
                if (geom == null) {
                    log.warning("Unable to find a geometry for a mesh node?", "name", name);
                    continue;
                }
                Long geomId = geom.getData();
                // See if we have skin and material
                FBXNode skin = findNodeToDest(geomId, "Deformer", "Skin"); // ok to not find...
                Long skinId = skin != null ? skin.<Long>getData() : null;
                ModelDef.TriMeshDef mesh = parseMesh(geom, skinId);
                spat = mesh;

                mesh.offsetTranslation = new float[] { 0f, 0f, 0f };
                mesh.offsetRotation = new float[] { 0f, 0f, 0f, 1f };
                mesh.offsetScale = new float[] { 1f, 1f, 1f };

                // then, after we've potentially set-up bone weights, assign the textures
                FBXNode material = findNodeToDest(id, "Material");
                if (material != null) {
                    Long materialId = material.getData();
                    FBXNode texture = findNodeToDest(materialId, "Texture");
                    if (texture != null) {
                        Long textureId = texture.getData();
                        FBXNode video = findNodeToDest(textureId, "Video");
                        String filename = textures.get(video);
                        if (filename == null) {
                            filename = extractTexture(video, dir);
                            if (filename != null) {
                                if (messages != null) messages.add("Extracted " + filename + ".");
                                textures.put(video, filename);
                            }
                        }
                        if (filename != null) mesh.texture = filename;
                    }
                }
                if (mesh.texture != null) {
                    mesh.tag = skinId == null ? ModelConfig.DEFAULT_TAG : ModelConfig.SKINNED_TAG;
                }

            } else {
                log.warning("Unknown node type seen: " + type);
                continue;
            }

            // now we can read some stuff about this spatial
            spat.name = name;
            mapObject(id, spat);

            spat.translation = new float[] { 0f, 0f, 0f };
            spat.rotation = new float[] { 0f, 0f, 0f, 1f };
            spat.scale = new float[] { 1f, 1f, 1f };

            FBXNode props = node.getChildByName("Properties70");
            if (props == null) {
                log.warning("No props for node?", "name", name);
                continue;
            }
            float[] preRot = null;
            for (FBXNode prop : props.getChildrenByName("P")) {
                String pname = prop.getData(0);
                if ("Lcl Translation".equals(pname)) {
                    spat.translation = getFloatTriplet(prop);
                } else if ("Lcl Rotation".equals(pname)) {
                    spat.rotation = getRotation(prop);
                } else if ("Lcl Scaling".equals(pname)) {
                    spat.scale = getFloatTriplet(prop);
                } else if ("PreRotation".equals(pname)) {
                    preRot = getRotation(prop);
//                    if (isRoot) preRotation = new Quaternion(getRotation(prop)).invert();
//                    else log.warning("PreRotation found for non-root node?", "name", name);
                }
            }
            if (preRot != null) {
                log.info("Found pre-rot for a node", "name", name, "prerot", preRot);
                Quaternion result = new Quaternion(preRot).multLocal(new Quaternion(spat.rotation));
                result.get(spat.rotation);
                log.info("Calculated new total rotation", "rot", spat.rotation);
            }
            model.addSpatial(spat);

            //log.info("Found node", "name", spat.name, "type", type, "type", spat.getClass());
        }

        // go through the connections and wire-up any spatials?
        for (Connection conn : connsByDest.values()) {
            if (!"OO".equals(conn.type)) continue;
            Object src = objectsById.get(conn.srcId);
            Object dest = objectsById.get(conn.destId);
            if (src instanceof ModelDef.SpatialDef && dest instanceof ModelDef.SpatialDef) {
                ModelDef.SpatialDef child = (ModelDef.SpatialDef)src;
                if (child.parent == null) child.parent = ((ModelDef.SpatialDef)dest).name;
                else log.warning("Connection has parent assigned already!", "child", child.name);
//            } else {
//                log.warning("unfound conn",
//                    "src", formatObj(src, conn.srcId),
//                    "dest", formatObj(dest, conn.destId));
            }
        }

        return model;
    }

    /**
     * Parse a mesh, which won't have a `name` or `texture` yet assigned.
     * Assumption: The bone nodes have already been parsed and are mapped by id.
     * @param skinId if null, parse a trimesh, otherwise a skinmesh with bone weights.
     */
    protected ModelDef.TriMeshDef parseMesh (FBXNode geom, Long skinId)
    {
        boolean isSkin = skinId != null;
        ModelDef.TriMeshDef mesh = isSkin ? new ModelDef.SkinMeshDef() : new ModelDef.TriMeshDef();
        FBXNode norms = geom.getChildByName("LayerElementNormal");
        FBXNode uvs = geom.getChildByName("LayerElementUV");

        double[] vertices = geom.getChildProperty("Vertices");
        int[] pvi = geom.getChildProperty("PolygonVertexIndex");
        double[] normals = norms.getChildProperty("Normals");
        double[] uvData = uvs.getChildProperty("UV");
        int[] uvIndex = uvs.getChildProperty("UVIndex");
        String normalMappingType = norms.getChildProperty("MappingInformationType");

        ListMultimap<Integer, Integer> verticesLookup = null;
        List<ModelDef.SkinVertex> meshVerts = null;
        if (isSkin) {
            verticesLookup = ArrayListMultimap.create();
            meshVerts = Lists.newArrayList();
        }

        int nidx = 0;
        int uidx = 0;
        float[] defaultTcoords = new float[2];
        for (int ii = 0, nn = pvi.length; ii < nn; ++ii) {
            ModelDef.Vertex vv = isSkin ? new ModelDef.SkinVertex() : new ModelDef.Vertex();
            vv.tcoords = defaultTcoords;
            int idx = pvi[ii];

            if ((idx < 0) != (ii % 3 == 2)) {
                // TODO: Convert polygons to triangles ?
                throw new RuntimeException("Unable to read meshes that aren't triangles!");
            }
            // Handle negative indices (they mark end of polygon, need to be made positive)
            if (idx < 0) idx = ~idx;

            // Set vertex position
            int vi = idx * 3;
            vv.location = new float[] {
                (float)vertices[vi], (float)vertices[vi + 1], (float)vertices[vi + 2]
            };

            // Set normal
            if ("ByPolygonVertex".equals(normalMappingType)) {
                vv.normal = new float[] {
                    (float)normals[nidx], (float)normals[nidx + 1], (float)normals[nidx + 2]
                };
                nidx += 3;

            } else if ("ByVertice".equals(normalMappingType)) {
                vv.normal = new float[] {
                    (float)normals[vi], (float)normals[vi + 1], (float)normals[vi + 2]
                };

            } else {
                log.warning("Unknown normalMappingType " + normalMappingType);
            }

            // Set UV coordinates
            int uvIdx = uvIndex[uidx++];
            if (uvIdx != -1) {
                uvIdx *= 2;
                vv.tcoords = new float[] { (float)uvData[uvIdx], (float)uvData[uvIdx + 1] };
            }

            if (isSkin) {
                verticesLookup.put(idx, meshVerts.size());
                meshVerts.add((ModelDef.SkinVertex)vv);
            } else {
                // Since there are no bone weights, we can add the vertex to the mesh instead
                mesh.addVertex(vv);
            }
        }

        if (isSkin) {
            for (Connection conn : connsByDest.get(skinId)) {
                Long clusterId = conn.srcId;
                FBXNode cluster = findNode(clusterId, "Deformer", "Cluster");
                if (cluster == null) {
                    log.warning("Messed-up deformer cluster?", "cluster", clusterId);
                    continue;
                }
                // Get indices and weights
                FBXNode indexNode = cluster.getChildByName("Indexes");
                if (indexNode == null) {
                    // TODO: A Cluster that uses only Transform / TransformLink. What do we do?
                    //log.info("TODO: Cluster uses Transform??", "id", clusterId);
                    continue;
                }
                int[] indices = indexNode.getData(0);
                double[] weights = cluster.getChildProperty("Weights");

                // Get the bone name from the connected Model node
                String boneName = null;
                for (Connection clusterConn : connsByDest.get(clusterId)) {
                    Object obj = objectsById.get(clusterConn.srcId);
                    if (obj instanceof ModelDef.SpatialDef) {
                        boneName = ((ModelDef.SpatialDef)obj).name;
                    }
                }
                if (boneName == null) log.warning("Couldn't find bone!", "cluster", clusterId);
                else {
                    // Add bone influences
                    for (int ii = 0; ii < indices.length; ++ii) {
                        for (int vi : verticesLookup.get(indices[ii])) {
                            ModelDef.BoneWeight bw = new ModelDef.BoneWeight();
                            bw.bone = boneName;
                            bw.weight = (float)weights[ii];
                            meshVerts.get(vi).addBoneWeight(bw);
                        }
//                        log.info("Added bone weight: " + boneName + " " + weights[ii],
//                                "vertices", verticesLookup.get(indices[ii]));
                    }
                }
            }

            // now that bone weights have been assigned, we can add the vertices
            for (ModelDef.SkinVertex vv : meshVerts) mesh.addVertex(vv);
        }

        return mesh;
    }

    protected String extractTexture (FBXNode node, File dir)
        throws IOException
    {
        FBXNode type = node.getChildByName("Type");
        FBXNode filename = node.getChildByName("Filename");
        FBXNode content = node.getChildByName("Content");
        if (type == null || filename == null || content == null ||
                type.getNumProperties() != 1 ||
                filename.getNumProperties() != 1 ||
                content.getNumProperties() != 1) return null;

        Object data = content.getData();
        if (!(data instanceof byte[])) return null;

        if (!"Clip".equals(type.getData())) {
            log.warning("Texture type is not clip?", "type", type.getData());
            return null;
        }

        String fullname = filename.getData();
        int foreslash = fullname.lastIndexOf('/');
        int backslash = fullname.lastIndexOf('\\');
        String basename = fullname.substring(1 + Math.max(foreslash, backslash));
        Files.write((byte[])data, new File(dir, basename));
        return basename;
    }
}

/**
 * Compares Mesh nodes after LimbNode or Root nodes.
 */
class ModelNodeComparator implements Comparator<FBXNode>
{
    // from Comparator
    public int compare (FBXNode a, FBXNode b)
    {
        return Ints.compare(nodeOrder(a), nodeOrder(b));
    }

    private int nodeOrder (FBXNode node)
    {
        String type = node.<String>getData(2);
        return "Root".equals(type) ? 0
            : "LimbNode".equals(type) ? 1
            : "Mesh".equals(type) ? 2
            : 3; // unknowns last
    }
}
