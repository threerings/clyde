package com.threerings.opengl.model.tools.fbx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.io.Files;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.lukaseichberg.fbxloader.FBXFile;
import com.lukaseichberg.fbxloader.FBXLoader;
import com.lukaseichberg.fbxloader.FBXNode;

import com.threerings.opengl.model.tools.ModelDef;

import static com.threerings.opengl.Log.log;

public class ModelFbxParser
{
    /**
     * Parse the model as well as extract any textures in the fbx into the specified directory.
     *
     * @param messages if provided, is populated with a list of import messages.
     */
    public ModelDef parseModel (InputStream in, File dir, @Nullable List<String> messages)
        throws IOException
    {
        ModelDef model = new ModelDef();
        FBXFile fbx = FBXLoader.loadFBXFile("model", in);
        //FbxDumper.Dump(fbx);

        FBXNode root = fbx.getRootNode();
        List<String> textures = Lists.newArrayList();
        Map<Long, Object> byId = Maps.newHashMap();

        // extract textures to the directory and add names to the list.
        extractTextures(root, dir, byId, textures);

        if (messages != null) {
            for (String texture : textures) {
                messages.add("Imported texture: " + texture);
            }
        }

        // TODO: parse skin mesh

        // parse the meshes
        for (ModelDef.TriMeshDef mesh : parseTriMeshes(root, byId, textures)) model.addSpatial(mesh);

        // parse any nodes found
        for (ModelDef.NodeDef node : parseNodes(root, byId)) model.addSpatial(node);

        return model;
    }

    public Iterable<ModelDef.TriMeshDef> parseTriMeshes (
        FBXNode root, Map<Long, Object> byId, List<String> textures
    ) {
        int numTextures = textures.size();
        FBXNode objects = root.getChildByName("Objects");
        List<ModelDef.TriMeshDef> meshes = Lists.newArrayList();
        for (FBXNode geom : objects.getChildrenByName("Geometry")) {
            ModelDef.TriMeshDef meshdef = parseTriMesh(geom);
            int idx = meshes.size();
            meshdef.name = root.getName() + (idx == 0 ? "" : String.valueOf(idx));
            meshdef.texture = numTextures == 0 ? "unknown" : textures.get(idx % numTextures);
            meshes.add(meshdef);
        }
        return meshes;
    }

    /**
     * Parse a mesh, which won't have a `name` or `texture` yet assigned.
     */
    protected ModelDef.TriMeshDef parseTriMesh (FBXNode geom)
    {
        FBXNode norms = geom.getChildByName("LayerElementNormal");
        FBXNode uvs = geom.getChildByName("LayerElementUV");

        double[] vertices = geom.getChildProperty("Vertices");
        int[] pvi = geom.getChildProperty("PolygonVertexIndex");
        double[] normals = norms.getChildProperty("Normals");
        double[] uvData = uvs.getChildProperty("UV");
        int[] uvIndex = uvs.getChildProperty("UVIndex");
        String normalMappingType = norms.getChildProperty("MappingInformationType");

        ModelDef.TriMeshDef trimesh = new ModelDef.TriMeshDef();

        //trimesh.name = root.getName();
        //trimesh.texture = "unknown_texture";
        trimesh.translation = new float[] { 0f, 0f, 0f };
        trimesh.rotation = new float[] { 0f, 0f, 0f, 1f };
        //    : new float[] { .5f, 0f, 0f, 1f }; // TODO: unhack HACK HACK HACK

        // TODO: is the rotation of the mesh derived from the "PreRotation" in the model properties?
        trimesh.scale = new float[] { 1f, 1f, 1f };
        trimesh.offsetTranslation = new float[] { 0f, 0f, 0f };
        trimesh.offsetRotation = new float[] { 0f, 0f, 0f, 1f };
        trimesh.offsetScale = new float[]{ 1f, 1f, 1f };

        // TODO: Convert polygons to triangles
        boolean warnedPolygons = false;
        int nidx = 0;
        int uidx = 0;
        float[] defaultTcoords = new float[2];
        for (int ii = 0, nn = pvi.length; ii < nn; ++ii) {
            ModelDef.Vertex v = new ModelDef.Vertex();
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

            trimesh.addVertex(v);
        }
        return trimesh;
    }

    // TODO: more can be done here to parse nodes?
    public Iterable<ModelDef.NodeDef> parseNodes (FBXNode root, Map<Long, Object> byId)
    {
        FBXNode objects = root.getChildByName("Objects");
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
            Long longId = (Long)child.getProperty(0).getData();
            node.name = (String)child.getProperty(1).getData();
            String type = (String)child.getProperty(2).getData();
            if (!"LimbNode".equals(type)) {
                log.warning("Seen node type: " + type);
                // "Mesh", "Root"... TODO
            }

            Object oval = nodes.put(node.name, node);
            if (oval != null) log.warning("Two objects of same name?", "name", node.name);

            oval = byId.put(longId, node);
            if (oval != null) log.warning("Two objects of same id?", "id", longId);

            FBXNode props = child.getChildByName("Properties70");
            if (props == null) {
                log.warning("No props for node?", "name", node.name);
                continue;
            }
            for (FBXNode prop : props.getChildrenByName("P")) {
                String pname = (String)prop.getProperty(0).getData();
                //log.info("Found pname", "pname", pname, "node", node.name);
                if ("Lcl Translation".equals(pname)) {
                    node.translation = new float[] {
                        ((Double)prop.getProperty(4).getData()).floatValue(),
                        ((Double)prop.getProperty(5).getData()).floatValue(),
                        ((Double)prop.getProperty(6).getData()).floatValue()
                    };
                } else if ("Lcl Rotation".equals(pname)) {
                    node.rotation = new float[] {
                        ((Double)prop.getProperty(4).getData()).floatValue(),
                        ((Double)prop.getProperty(5).getData()).floatValue(),
                        ((Double)prop.getProperty(6).getData()).floatValue(),
                        1f
                    };
                } else if ("Lcl Scaling".equals(pname)) {
                    node.scale = new float[] {
                        ((Double)prop.getProperty(4).getData()).floatValue(),
                        ((Double)prop.getProperty(5).getData()).floatValue(),
                        ((Double)prop.getProperty(6).getData()).floatValue()
                    };
                }
            }
        }

        // see if we have any attributes
        for (FBXNode attr : objects.getChildrenByName("NodeAttribute")) {
            Long longId = (Long)attr.getProperty(0).getData();
            Object oval = byId.put(longId, attr);
            if (oval != null) log.warning("Two objects of same id?", "id", longId);
            else log.info("Stored attr..." + longId);
        }

        // https://download.autodesk.com/us/fbx/20112/fbx_sdk_help/index.html?url=WS73099cc142f487551fea285e1221e4f9ff8-7fda.htm,topicNumber=d0e6388
        FBXNode connections = root.getChildByName("Connections");
        for (FBXNode conn : connections.getChildrenByName("C")) {
            String type = (String)conn.getProperty(0).getData();
            if (true || "OO".equals(type)) {
                Long srcId = (Long)conn.getProperty(1).getData();
                Long destId = (Long)conn.getProperty(2).getData();
                // child is "source", parent is "destination"
                Object src = byId.get(srcId);
                Object dest = byId.get(destId);
                if (dest instanceof ModelDef.NodeDef) {
                    ModelDef.NodeDef parent = (ModelDef.NodeDef)dest;
                    if (src instanceof ModelDef.NodeDef) {
                        ModelDef.NodeDef child = (ModelDef.NodeDef)src;
                        if (child.parent == null) {
                            child.parent = parent.name;
                            log.info("Added parent!", "child", child.name, "parent", parent.name);
                        } else {
                            log.warning("Oh noes! Child already has a parent defined?",
                                    "child", child.name,
                                    "parent", child.parent,
                                    "newparent", parent.name);
                        }
                    } else if (src instanceof FBXNode) {
                        log.info("We found that some attributes apply to a node",
                                "parent", parent.name);
                        FbxDumper.Dump((FBXNode)src);
                    }
                } else {
                    log.info("Unfound conn", "type", type, "src", src, "dest", dest);
                }
            }
        }

        return nodes.values();
    }

    /**
     * Extract into `dir` any textures found at <em>or below</em> the specified node.
     */
    public void extractTextures (
        FBXNode node, File dir, Map<Long, Object> byId, List<String> filenames
    )
        throws IOException
    {
        // recurse on children
        for (int ii = 0, nn = node.getNumChildren(); ii < nn; ++ii) {
            extractTextures(node.getChild(ii), dir, byId, filenames);
        }

        extractTexture(node, dir, byId, filenames);
    }

    private boolean extractTexture (
        FBXNode node, File dir, Map<Long, Object> byId, List<String> filenames
    )
        throws IOException
    {
        // For now, we look for a node called "Video"
        // - that has a sub-node called "Type" with a "Clip" string property.
        // - that has a "Filename" with a string property
        // - and finally a "Content" with the file data
        if (!"Video".equals(node.getName())) return false;
        FBXNode type = node.getChildByName("Type");
        FBXNode filename = node.getChildByName("Filename");
        FBXNode content = node.getChildByName("Content");
        if (type == null || filename == null || content == null ||
                type.getNumProperties() != 1 ||
                filename.getNumProperties() != 1 ||
                content.getNumProperties() != 1) return false;

        Object data = content.getProperty(0).getData();
        if (!(data instanceof byte[])) return false;

        if (!"Clip".equals(type.getProperty(0).getData())) return false;

        // TODO?
        String fullname = String.valueOf(filename.getProperty(0).getData());
        int foreslash = fullname.lastIndexOf('/');
        int backslash = fullname.lastIndexOf('\\');
        String basename = fullname.substring(1 + Math.max(foreslash, backslash));
        Files.write((byte[])data, new File(dir, basename));
        //log.info("Wrote", "file", basename);
        filenames.add(basename);
        return true;
    }
}
