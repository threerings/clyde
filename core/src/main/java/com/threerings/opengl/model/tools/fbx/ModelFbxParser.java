package com.threerings.opengl.model.tools.fbx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.samskivert.util.Logger;

import com.google.common.io.Files;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


import com.lukaseichberg.fbxloader.FBXFile;
import com.lukaseichberg.fbxloader.FBXLoader;
import com.lukaseichberg.fbxloader.FBXNode;

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
        return new ModelFbxParser().parse(in, dir, messages);
    }

    /**
     * I'm actually not a fan of having anything be static but until I see a need to subclass
     * this I guess I'll do it to enforce in the API that the actual instance is not
     * thread-safe and will contain much garbage after parsing the model. Therefore, the
     * instance isn't directly instantiable and you access this via the public static method
     * above.
     */
    protected ModelFbxParser () { /* do not instantiate directly */ }

    protected final List<String> textures = Lists.newArrayList();

    protected ModelDef parse (InputStream in, File dir, @Nullable List<String> messages)
        throws IOException
    {
        ModelDef model = new ModelDef();
        FBXFile fbx = FBXLoader.loadFBXFile("model", in);
        //FbxDumper.Dump(fbx);
        root = fbx.getRootNode();
        objects = root.getChildByName("Objects");

        // extract textures to the directory and add names to the list.
        for (FBXNode vid : objects.getChildrenByName("Video")) {
            extractTexture(vid, dir);
        }

        // read and populate the connections
        populateConnections();

        // parse nodes
        Map<String, ModelDef.NodeDef> nodes = parseNodes();

        // skin meshes?
        parseMeshes(nodes, model);

//        // parse the meshes
//        for (ModelDef.TriMeshDef mesh : parseTriMeshes()) model.addSpatial(mesh);
//
        // parse / dump everything
        // checkMore();

        // add the nodes last? // does it matter?
        for (ModelDef.NodeDef node : nodes.values()) model.addSpatial(node);

        // go through the connections and wire-up any spatials?
        for (Connection conn : connsByDest.values()) {
            if (!"OO".equals(conn.type)) continue;
            Object src = objectsById.get(conn.srcId);
            Object dest = objectsById.get(conn.destId);
            if (src instanceof ModelDef.SpatialDef && dest instanceof ModelDef.SpatialDef) {
                ModelDef.SpatialDef child = (ModelDef.SpatialDef)src;
                if (child.parent != null) {
                    log.warning("Connection has parent assigned already!", "child", child.name);
                } else {
                    child.parent = ((ModelDef.SpatialDef)dest).name;
//                    log.info("Wire up", "child", child.name, "parent", child.parent);
//                    if (dest instanceof ModelDef.NodeDef &&
//                            child instanceof ModelDef.TriMeshDef) {
//                        ModelDef.NodeDef node = (ModelDef.NodeDef)dest;
//                        child.rotation = node.rotation;
//                        child.translation = node.translation;
//                        child.scale = node.scale;
//                        log.warning("==== COPYING attrs");
////                        node.translation = new float[] { 0f, 0f, 0f };
////                        node.rotation = new float[] { 0f, 0f, 0f, 1f };
////                        node.scale = new float[] { 1f, 1f, 1f };
//                    }
                }

//            } else {
//                log.warning("unfound conn",
//                    "src", formatObj(src, conn.srcId),
//                    "dest", formatObj(dest, conn.destId));
            }
        }

        // Add any messages
        if (messages != null) {
            // if we imported textures, note it
            for (String texture : textures) {
                messages.add("Imported texture: " + texture);
            }
        }

        return model;
    }

    protected ModelDef.TriMeshDef parseTriMesh (FBXNode geom)
    {
        ModelDef.TriMeshDef mesh = new ModelDef.TriMeshDef();
        addAll(mesh, parseMesh(geom, mesh, null));
        return mesh;
    }

    protected void addAll (ModelDef.TriMeshDef mesh, List<? extends ModelDef.Vertex> vertices)
    {
        for (ModelDef.Vertex v : vertices) mesh.addVertex(v);
    }

    /**
     * Parse a mesh, which won't have a `name` or `texture` yet assigned.
     */
    protected <V extends ModelDef.Vertex> List<V> parseMesh (
        FBXNode geom, ModelDef.TriMeshDef mesh, ListMultimap<Integer, Integer> verticesLookup)
    {
        FBXNode norms = geom.getChildByName("LayerElementNormal");
        FBXNode uvs = geom.getChildByName("LayerElementUV");

        double[] vertices = geom.getChildProperty("Vertices");
        int[] pvi = geom.getChildProperty("PolygonVertexIndex");
        double[] normals = norms.getChildProperty("Normals");
        double[] uvData = uvs.getChildProperty("UV");
        int[] uvIndex = uvs.getChildProperty("UVIndex");
        String normalMappingType = norms.getChildProperty("MappingInformationType");

        //trimesh.name = root.getName();
        //trimesh.texture = "unknown_texture";
        mesh.translation = new float[] { 0f, 0f, 0f };
        mesh.rotation = new float[] { 0f, 0f, 0f, 1f };
             //new float[] { .5f, 0f, 0f, 1f }; // TODO: unhack HACK HACK HACK

        // TODO: is the rotation of the mesh derived from the "PreRotation" in the model properties?
        mesh.scale = new float[] { 1f, 1f, 1f };
        mesh.offsetTranslation = new float[] { 0f, 0f, 0f };
        mesh.offsetRotation = new float[] { 0f, 0f, 0f, 1f };
        mesh.offsetScale = new float[]{ 1f, 1f, 1f };

        mapObject(geom, mesh);
        List<V> meshVerts = Lists.newArrayList();

        // TODO: Convert polygons to triangles ?
        boolean warnedPolygons = false;
        int nidx = 0;
        int uidx = 0;
        float[] defaultTcoords = new float[2];
        for (int ii = 0, nn = pvi.length; ii < nn; ++ii) {
            @SuppressWarnings("unchecked")
            V v = mesh instanceof ModelDef.SkinMeshDef
                ? (V)new ModelDef.SkinVertex() : (V)new ModelDef.Vertex();
            v.tcoords = defaultTcoords;
            int idx = pvi[ii];

            // TEMP?
            if (!warnedPolygons && (idx < 0) != (ii % 3 == 2)) {
                log.warning("We need to be importing triangles! This appears to be not!");
                warnedPolygons = true;
            }
            // Handle negative indices (they mark end of polygon, need to be made positive)
            if (idx < 0) idx = ~idx;

            // Set vertex position
            int vi = idx * 3;
            v.location = new float[] {
                (float)vertices[vi], (float)vertices[vi + 1], (float)vertices[vi + 2]
            };

            // Set normal
            if ("ByPolygonVertex".equals(normalMappingType)) {
                v.normal = new float[] {
                    (float)normals[nidx], (float)normals[nidx + 1], (float)normals[nidx + 2]
                };
                nidx += 3;

            } else if ("ByVertice".equals(normalMappingType)) {
                v.normal = new float[] {
                    (float)normals[vi], (float)normals[vi + 1], (float)normals[vi + 2]
                };

            } else {
                log.warning("Unknown normalMappingType " + normalMappingType);
            }

            // Set UV coordinates
            int uvIdx = uvIndex[uidx++];
            if (uvIdx != -1) {
                uvIdx *= 2;
                v.tcoords = new float[] {
                    (float)uvData[uvIdx], (float)uvData[uvIdx + 1]
                };
            }

            if (verticesLookup != null) verticesLookup.put(idx, meshVerts.size());
            meshVerts.add(v);
        }
        return meshVerts;
    }

    // TODO: more can be done here to parse nodes?
    protected Map<String, ModelDef.NodeDef> parseNodes ()
    {
        Map<String, ModelDef.NodeDef> nodes = Maps.newHashMap();
        float[] defaultTranslation = new float[3];
        float[] defaultRotation = new float[] { 0f, 0f, 0f, 1f };
        float[] defaultScale = new float[] { 1f, 1f, 1f };
        for (FBXNode child : objects.getChildrenByName("Model")) {
            ModelDef.NodeDef node = new ModelDef.NodeDef();
            node.translation = defaultTranslation;
            node.rotation = defaultRotation;
            node.scale = defaultScale;

            if (3 != child.getNumProperties()) log.warning("Node with non-3 props?");
            // prop 0 is id
            Long id = child.getData(0);
            mapObject(id, node);
            node.name = child.getData(1);
            Object oval = nodes.put(node.name, node);
            if (oval != null) log.warning("Two nodes of same name?", "name", node.name);

            String type = child.getData(2);
            if (!"LimbNode".equals(type)) {
                //log.warning("Seen node type: " + type, "name", node.name);
                //continue;
                // "Mesh", "Root"... TODO
            }

            FBXNode props = child.getChildByName("Properties70");
            if (props == null) {
                log.warning("No props for node?", "name", node.name);
                continue;
            }
            //log.info("Defining node", "node", node.name, "id", id);
            for (FBXNode prop : props.getChildrenByName("P")) {
                String pname = prop.getData(0);
                Object pvalue;
                if ("Lcl Translation".equals(pname)) {
                    pvalue = node.translation = getFloatTriplet(prop);
                } else if ("Lcl Rotation".equals(pname)) {
                    pvalue = node.rotation = getRotation(prop);
                } else if ("Lcl Scaling".equals(pname)) {
                    pvalue = node.scale = getFloatTriplet(prop);
                } else continue;
                //} else pvalue = "{" + (prop.getNumProperties() - 4) + "}";
                //log.info("  Found prop on node", "prop", pname, "node", node.name, "value", pvalue);
            }
        }
        return nodes;
    }

    protected boolean extractTexture (FBXNode node, File dir)
        throws IOException
    {
        FBXNode type = node.getChildByName("Type");
        FBXNode filename = node.getChildByName("Filename");
        FBXNode content = node.getChildByName("Content");
        if (type == null || filename == null || content == null ||
                type.getNumProperties() != 1 ||
                filename.getNumProperties() != 1 ||
                content.getNumProperties() != 1) return false;

        Object data = content.getProperty(0).getData();
        if (!(data instanceof byte[])) return false;

        if (!"Clip".equals(type.getProperty(0).getData())) {
            log.warning("Texture type is not clip?", "type", type.getProperty(0).getData());
            return false;
        }

        // TODO?
        String fullname = String.valueOf(filename.getProperty(0).getData());
        int foreslash = fullname.lastIndexOf('/');
        int backslash = fullname.lastIndexOf('\\');
        String basename = fullname.substring(1 + Math.max(foreslash, backslash));
        Files.write((byte[])data, new File(dir, basename));
        //log.info("Wrote", "file", basename);
        textures.add(basename);
        mapObject(node, basename);
        return true;
    }

    protected void parseMeshes (Map<String, ModelDef.NodeDef> nodes, ModelDef model) {

        // First pass: collect all deformers and their relationships
        Map<Long, FBXNode> skinDeformers = Maps.newHashMap();
        Map<Long, FBXNode> clusters = Maps.newHashMap();
        Map<Long, FBXNode> geoms = Maps.newHashMap();
        int numTextures = textures.size();
        int textureIdx = 0; // TODO: this should be extracted from connections

        // find all the geoms
        for (FBXNode geom : objects.getChildrenByName("Geometry")) {
            geoms.put(geom.<Long>getData(0), geom);
        }

        // find the deformers
        for (FBXNode deformer : objects.getChildrenByName("Deformer")) {
            Long id = deformer.getData(0);
            String type = deformer.getData(2);

            if ("Skin".equals(type)) {
                skinDeformers.put(id, deformer);
//                log.info("skin deformer...",
//                        "indexes", deformer.getChildProperty("Indexes"),
//                        "blendweights", deformer.getChildProperty("BlendWeights"));
            } else if ("Cluster".equals(type)) {
                clusters.put(id, deformer);
            } else {
                log.info("Another kind of deformer spotted: " + type);
            }
        }

        // Process each skin deformer
        for (Map.Entry<Long, FBXNode> entry : skinDeformers.entrySet()) {
            FBXNode skinDeformer = entry.getValue();
            ModelDef.SkinMeshDef skinMesh = new ModelDef.SkinMeshDef();
            // find the geom that goes with it...
            FBXNode geom = null;
            Long geomId = null;
            for (Connection conn : connsBySrc.get(entry.getKey())) {
                if (geom != null) log.warning("uh oh?");
                geomId = conn.destId;
                // Remove it from our list of geometries, the leftovers will become skinmeshes
                geom = geoms.remove(geomId);
            }
            if (geom == null) {
                log.warning("Could not find geom for skin deformer... skipping? SHIT!");
                continue;
            }
            ListMultimap<Integer, Integer> verticesLookup = ArrayListMultimap.create();
            List<ModelDef.SkinVertex> vertices = parseMesh(geom, skinMesh, verticesLookup);

            for (Connection conn : connsByDest.get(entry.getKey())) {
                //log.info("Checking dests for the skin: " + conn);
                FBXNode cluster = clusters.get(conn.srcId);
                if (cluster == null) {
                    log.warning("Couldn't find source cluster for conn?", "conn.srcId", conn.srcId);
                    continue;
                }
                // Get indices and weights
                FBXNode indexNode = cluster.getChildByName("Indexes");
                if (indexNode == null) {
//                    log.warning("Missing indexes for bone?", "cluster", cluster.getFullName(),
//                            "id", cluster.getData(0));
                    // TODO: A Cluster that uses only Transform / TransformLink. What do we do?
                    continue;
                }
                int[] indices = indexNode.getData(0);
                double[] weights = cluster.getChildProperty("Weights");

                // Get the bone name from the connected Model node
                String boneName = findConnectedBoneName(cluster);

                if (boneName != null) {
                    // Add bone influences
                    for (int i = 0; i < indices.length; i++) {
                        for (int vi : verticesLookup.get(indices[i])) {
                            ModelDef.BoneWeight bw = new ModelDef.BoneWeight();
                            bw.bone = boneName;
                            bw.weight = (float)weights[i];
                            vertices.get(vi).addBoneWeight(bw);
                        }
//                        log.info("Added bone weight: " +
//                                indices[i] + "/" + vertices.size() + " " +
//                                boneName + " " + weights[i],
//                                "vertices", verticesLookup.get(indices[i]));
                    }
                } else log.warning("Couldn't find bone name", "bone", boneName);

//                // Get transform matrices if present
//                double[] transform = cluster.getChildProperty("Transform");
//                if (transform != null) {
//                    // Convert transform matrix to our format
//                    // Note: You might need to adjust this based on your coordinate system
//                    float[] matrix = new float[16];
//                    for (int i = 0; i < 16; i++) {
//                        matrix[i] = (float)transform[i];
//                    }
//                    log.info("We are trying to figure out the matrix transform for the bone or something",
//                            "bone", boneName, "matrix", transform);
//                }
            }

            addAll(skinMesh, vertices);
            skinMesh.name = root.getName() + ((textureIdx == 0) ? "" : String.valueOf(textureIdx));
            skinMesh.texture = numTextures == 0 ? "unknown" : textures.get(textureIdx++ % numTextures);

            // Let's look up and down at the connections to the geometry
            // connections: the mesh node is a child of the geometry mesh

            // TODO : FIX THIS! Proper connections
//            log.info("Removing node? " + skinMesh.name);
//            nodes.remove(skinMesh.name);

            model.addSpatial(skinMesh);
        }

        // any remaining geoms get parsed as plain trimeshes ?? ?? ?? ??
        for (FBXNode geom : geoms.values()) {
            //log.info("Parsing leftover trimesh? " + geom);
            ModelDef.TriMeshDef mesh = parseTriMesh(geom);
            mesh.name = root.getName() + ((textureIdx == 0) ? "" : String.valueOf(textureIdx));
            mesh.texture = numTextures == 0 ? "unknown" : textures.get(textureIdx++ % numTextures);
            model.addSpatial(mesh);
        }
    }

    protected String findConnectedBoneName (FBXNode cluster) {
        Long clusterId = cluster.getData(0);
        for (Connection conn : connsByDest.get(clusterId)) {
            Object obj = objectsById.get(conn.srcId);
            if (obj instanceof ModelDef.NodeDef) {
                return ((ModelDef.NodeDef)obj).name;
            }
        }
        log.warning("Couldn't find bone name for cluster " + clusterId);
        return null;
    }
}
