//
// $Id$

package com.threerings.opengl.model.tools.xml;

import java.io.FileInputStream;
import java.io.IOException;

import org.xml.sax.SAXException;
import org.apache.commons.digester.Digester;

import com.samskivert.xml.SetPropertyFieldsRule;

import com.threerings.opengl.model.tools.ModelDef;

/**
 * Parses XML files containing 3D models.
 */
public class ModelParser
{
    public ModelParser ()
    {
        // create and configure our digester
        _digester = new Digester();

        // add the rules
        String model = "model";
        _digester.addObjectCreate(model, ModelDef.class.getName());
        _digester.addSetNext(model, "setModel", ModelDef.class.getName());

        String tmesh = model + "/triMesh";
        _digester.addObjectCreate(tmesh, ModelDef.TriMeshDef.class.getName());
        _digester.addRule(tmesh, new SetPropertyFieldsRule());
        _digester.addSetNext(tmesh, "addSpatial",
            ModelDef.SpatialDef.class.getName());

        String smesh = model + "/skinMesh";
        _digester.addObjectCreate(smesh,
            ModelDef.SkinMeshDef.class.getName());
        _digester.addRule(smesh, new SetPropertyFieldsRule());
        _digester.addSetNext(smesh, "addSpatial",
            ModelDef.SpatialDef.class.getName());

        String node = model + "/node";
        _digester.addObjectCreate(node, ModelDef.NodeDef.class.getName());
        _digester.addRule(node, new SetPropertyFieldsRule());
        _digester.addSetNext(node, "addSpatial",
            ModelDef.SpatialDef.class.getName());

        String vertex = tmesh + "/vertex", svertex = smesh + "/vertex";
        _digester.addObjectCreate(vertex, ModelDef.Vertex.class.getName());
        _digester.addObjectCreate(svertex,
            ModelDef.SkinVertex.class.getName());
        _digester.addRule(vertex, new SetPropertyFieldsRule());
        _digester.addRule(svertex, new SetPropertyFieldsRule());
        _digester.addSetNext(vertex, "addVertex",
            ModelDef.Vertex.class.getName());
        _digester.addSetNext(svertex, "addVertex",
            ModelDef.Vertex.class.getName());

        String bweight = smesh + "/vertex/boneWeight";
        _digester.addObjectCreate(bweight,
            ModelDef.BoneWeight.class.getName());
        _digester.addRule(bweight, new SetPropertyFieldsRule());
        _digester.addSetNext(bweight, "addBoneWeight",
            ModelDef.BoneWeight.class.getName());
    }

    /**
     * Parses the XML file at the specified path into a model definition.
     */
    public ModelDef parseModel (String path)
        throws IOException, SAXException
    {
        _model = null;
        _digester.push(this);
        _digester.parse(new FileInputStream(path));
        return _model;
    }

    /**
     * Called by the parser once the model is parsed.
     */
    public void setModel (ModelDef model)
    {
        _model = model;
    }

    protected Digester _digester;
    protected ModelDef _model;
}
