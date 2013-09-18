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

package com.threerings.opengl.model.tools.xml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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
        _digester.addRule(tmesh, new SetPropertyFieldsRule(false));
        _digester.addSetNext(tmesh, "addSpatial", ModelDef.SpatialDef.class.getName());

        String smesh = model + "/skinMesh";
        _digester.addObjectCreate(smesh, ModelDef.SkinMeshDef.class.getName());
        _digester.addRule(smesh, new SetPropertyFieldsRule(false));
        _digester.addSetNext(smesh, "addSpatial", ModelDef.SpatialDef.class.getName());

        String node = model + "/node";
        _digester.addObjectCreate(node, ModelDef.NodeDef.class.getName());
        _digester.addRule(node, new SetPropertyFieldsRule());
        _digester.addSetNext(node, "addSpatial", ModelDef.SpatialDef.class.getName());

        String vertex = tmesh + "/vertex", svertex = smesh + "/vertex";
        _digester.addObjectCreate(vertex, ModelDef.Vertex.class.getName());
        _digester.addObjectCreate(svertex, ModelDef.SkinVertex.class.getName());
        _digester.addRule(vertex, new SetPropertyFieldsRule());
        _digester.addRule(svertex, new SetPropertyFieldsRule());
        _digester.addSetNext(vertex, "addVertex", ModelDef.Vertex.class.getName());
        _digester.addSetNext(svertex, "addVertex", ModelDef.Vertex.class.getName());

        String extra = "*/extra";
        _digester.addObjectCreate(extra, ModelDef.Extra.class.getName());
        _digester.addRule(extra, new SetPropertyFieldsRule());
        _digester.addSetNext(extra, "addExtra", ModelDef.Extra.class.getName());

        String bweight = smesh + "/vertex/boneWeight";
        _digester.addObjectCreate(bweight, ModelDef.BoneWeight.class.getName());
        _digester.addRule(bweight, new SetPropertyFieldsRule());
        _digester.addSetNext(bweight, "addBoneWeight", ModelDef.BoneWeight.class.getName());
    }

    /**
     * Parses the XML file at the specified path into a model definition.
     */
    public ModelDef parseModel (String path)
        throws IOException, SAXException
    {
        return parseModel(new FileInputStream(path));
    }

    /**
     * Parses the supplied XML stream into a model definition.
     */
    public ModelDef parseModel (InputStream in)
        throws IOException, SAXException
    {
        _model = null;
        _digester.push(this);
        _digester.parse(in);
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
