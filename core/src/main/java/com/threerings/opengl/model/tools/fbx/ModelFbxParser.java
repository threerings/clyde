package com.threerings.opengl.model.tools.fbx;

import java.io.IOException;
import java.io.InputStream;

import com.lukaseichberg.fbxloader.FBXFile;
import com.lukaseichberg.fbxloader.FBXLoader;
import com.lukaseichberg.fbxloader.FBXNode;

import com.threerings.opengl.model.tools.ModelDef;

import static com.threerings.opengl.Log.log;

public class ModelFbxParser
{
    public ModelDef parseModel (InputStream in)
        throws IOException
    {
        ModelDef model = new ModelDef();
        FBXFile fbx = FBXLoader.loadFBXFile("model", in);
        log.info("We do appear to have read an fbx file", "version", fbx.getVersion());
        FbxDumper.Dump(fbx);

        FBXNode root = fbx.getRootNode();
        FBXNode geom = root.getNodeFromPath("Objects/Geometry");
        FBXNode norms = geom.getChildByName("LayerElementNormal");
        double[] vertices = (double[])geom.getChildByName("Vertices").getProperty(0).getData();
        int[] pvi = (int[])geom.getChildByName("PolygonVertexIndex").getProperty(0).getData();
        double[] normals = (double[])norms.getChildByName("Normals").getProperty(0).getData();
        double[] normalsW = (double[])norms.getChildByName("NormalsW").getProperty(0).getData();

        // TODO: fix
        ModelDef.TriMeshDef trimesh = new ModelDef.TriMeshDef();
        trimesh.name = root.getName();
        trimesh.texture = "REPLACE_ME";
        int nidx = 0;
        for (int idx : pvi) {
            ModelDef.Vertex v = new ModelDef.Vertex();
            int vi = idx * 3;
            v.location = new float[] {
                (float)vertices[vi], (float)vertices[vi + 1], (float)vertices[vi + 2] };
            v.tcoords = new float[3];
            v.normal = new float[] {
                (float)normals[nidx++], (float)normals[nidx++], (float)normals[nidx++] };
            trimesh.addVertex(v);
        }
        model.addSpatial(trimesh);

        return model;
    }
}
