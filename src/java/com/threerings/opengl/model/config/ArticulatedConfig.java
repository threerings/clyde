//
// $Id$

package com.threerings.opengl.model.config;

import java.io.IOException;

import java.util.ArrayList;
import java.util.TreeSet;

import com.samskivert.util.ComparableTuple;
import com.samskivert.util.QuickSort;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.export.Importer;
import com.threerings.expr.Scope;
import com.threerings.math.Transform3D;
import com.threerings.util.DeepObject;
import com.threerings.util.Shallow;

import com.threerings.opengl.mod.Articulated;
import com.threerings.opengl.mod.Model;
import com.threerings.opengl.model.CollisionMesh;
import com.threerings.opengl.model.config.ModelConfig.MeshSet;
import com.threerings.opengl.model.config.ModelConfig.VisibleMesh;
import com.threerings.opengl.model.tools.ModelDef;
import com.threerings.opengl.util.GlContext;

/**
 * An original articulated implementation.
 */
public class ArticulatedConfig extends ModelConfig.Imported
{
    /**
     * A node within an {@link Articulated} model.
     */
    public static class Node extends DeepObject
        implements Exportable
    {
        /** The name of the node. */
        public String name;

        /** The initial transform of the node. */
        public Transform3D transform;

        /** The children of the node. */
        public Node[] children;

        /** The inverse of the reference space transform. */
        public transient Transform3D invRefTransform;

        public Node (String name, Transform3D transform, Node[] children)
        {
            this.name = name;
            this.transform = transform;
            this.children = children;
        }

        public Node ()
        {
        }

        /**
         * Populates the supplied list with the names of all nodes.
         */
        public void getNames (ArrayList<String> names)
        {
            names.add(name);
            for (Node child : children) {
                child.getNames(names);
            }
        }

        /**
         * Populates the supplied set with the names of all textures.
         */
        public void getTextures (TreeSet<String> textures)
        {
            for (Node child : children) {
                child.getTextures(textures);
            }
        }

        /**
         * Populates the supplied set with the names of all texture/tag pairs.
         */
        public void getTextureTagPairs (TreeSet<ComparableTuple<String, String>> pairs)
        {
            for (Node child : children) {
                child.getTextureTagPairs(pairs);
            }
        }

        /**
         * Creates an articulated node for this config.
         *
         * @param node an existing node to reuse, if possible.
         * @return either a reference to the existing node (if reused), or a new node.
         */
        public Articulated.Node getArticulatedNode (
            GlContext ctx, Scope scope, Articulated.Node node)
        {
            // require an exact class match
            if (node != null && node.getClass() == Articulated.Node.class) {
                node.setConfig(this);
            } else {
                node = new Articulated.Node(ctx, scope, this);
            }
            return node;
        }

        /**
         * Updates the nodes' reference transforms.
         */
        public void updateRefTransforms (Transform3D parentRefTransform)
        {
            Transform3D refTransform = parentRefTransform.compose(transform);
            invRefTransform = refTransform.invert();
            for (Node child : children) {
                child.updateRefTransforms(refTransform);
            }
        }
    }

    /**
     * A node containing a mesh.
     */
    public static class MeshNode extends Node
    {
        /** The node's visible mesh. */
        public VisibleMesh visible;

        /** The collision mesh. */
        public CollisionMesh collision;

        public MeshNode (
            String name, Transform3D transform, Node[] children,
            VisibleMesh visible, CollisionMesh collision)
        {
            super(name, transform, children);
            this.visible = visible;
            this.collision = collision;
        }

        public MeshNode ()
        {
        }

        @Override // documentation inherited
        public void getTextures (TreeSet<String> textures)
        {
            super.getTextures(textures);
            if (visible != null) {
                textures.add(visible.texture);
            }
        }

        @Override // documentation inherited
        public void getTextureTagPairs (TreeSet<ComparableTuple<String, String>> pairs)
        {
            super.getTextureTagPairs(pairs);
            if (visible != null) {
                pairs.add(new ComparableTuple<String, String>(visible.texture, visible.tag));
            }
        }

        @Override // documentation inherited
        public Articulated.Node getArticulatedNode (
            GlContext ctx, Scope scope, Articulated.Node node)
        {
            if (node instanceof Articulated.MeshNode) {
                node.setConfig(this);
            } else {
                node = new Articulated.MeshNode(ctx, scope, this);
            }
            return node;
        }
    }

    /**
     * A named animation reference.
     */
    public static class AnimationMapping extends DeepObject
        implements Exportable
    {
        /** The name of the reference. */
        @Editable
        public String name = "";

        /** The animation associated with the name. */
        @Editable(nullable=true)
        public ConfigReference<AnimationConfig> animation;
    }

    /**
     * Represents an attachment point on an articulated model.
     */
    public class AttachmentPoint extends DeepObject
        implements Exportable
    {
        /** The name of the node representing the attachment point. */
        @Editable(editor="choice")
        public String node;

        /** The model to attach to the node. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> attachment;

        /**
         * Returns the options available for the node field.
         */
        public String[] getNodeOptions ()
        {
            if (root == null) {
                return new String[0];
            }
            ArrayList<String> names = new ArrayList<String>();
            root.getNames(names);
            QuickSort.sort(names);
            return names.toArray(new String[names.size()]);
        }
    }

    /** The model's animation mappings. */
    @Editable
    public AnimationMapping[] animationMappings = new AnimationMapping[0];

    /** The model's attachment points. */
    @Editable(depends={"source"})
    public AttachmentPoint[] attachmentPoints = new AttachmentPoint[0];

    /** The root node. */
    @Shallow
    public Node root;

    /** The skin meshes. */
    @Shallow
    public MeshSet skin;

    /**
     * Reads the fields of this object.
     */
    public void readFields (Importer in)
        throws IOException
    {
        in.defaultReadFields();
        initTransientFields();
    }

    /**
     * Initializes the transient fields of the objects after construction or deserialization.
     */
    public void initTransientFields ()
    {
        if (root != null) {
            root.updateRefTransforms(new Transform3D());
        }
    }

    @Override // documentation inherited
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        if (root == null) {
            return null;
        }
        if (impl instanceof Articulated) {
            ((Articulated)impl).setConfig(this);
        } else {
            impl = new Articulated(ctx, scope, this);
        }
        return impl;
    }

    @Override // documentation inherited
    protected void updateFromSource (ModelDef def)
    {
        if (def == null) {
            root = null;
            skin = null;
        } else {
            def.update(this);
        }
    }

    @Override // documentation inherited
    protected void getTextures (TreeSet<String> textures)
    {
        if (root != null) {
            root.getTextures(textures);
        }
        if (skin != null) {
            skin.getTextures(textures);
        }
    }

    @Override // documentation inherited
    protected void getTextureTagPairs (TreeSet<ComparableTuple<String, String>> pairs)
    {
        if (root != null) {
            root.getTextureTagPairs(pairs);
        }
        if (skin != null) {
            skin.getTextureTagPairs(pairs);
        }
    }
}
