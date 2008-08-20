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
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.export.Importer;
import com.threerings.expr.Scope;
import com.threerings.expr.Updater;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.FloatMath;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
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
    /** The options for billboard rotation about the x axis. */
    public enum BillboardRotationX { ALIGN_TO_VIEW, FACE_VIEWER, NONE };

    /** The options for billboard rotation about the z axis. */
    public enum BillboardRotationZ { ALIGN_TO_VIEW, FACE_VIEWER };

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

        /**
         * (Re)creates the list of articulated nodes for this config.
         *
         * @param onodes if non-null, a list of existing nodes to reuse, if possible.
         * @param nnodes the list to contain the new nodes.
         */
        public void getArticulatedNodes (
            Scope scope, Articulated.Node[] onodes, ArrayList<Articulated.Node> nnodes,
            Transform3D parentViewTransform)
        {
            int idx = nnodes.size();
            Articulated.Node node = (onodes == null || onodes.length <= idx) ? null : onodes[idx];
            if (node != null && node.getClass() == getArticulatedNodeClass()) {
                node.setConfig(this, parentViewTransform);
            } else {
                node = createArticulatedNode(scope, parentViewTransform);
            }
            nnodes.add(node);
            Transform3D viewTransform = node.getViewTransform();
            for (Node child : children) {
                child.getArticulatedNodes(scope, onodes, nnodes, viewTransform);
            }
        }

        /**
         * Returns the articulated node class corresponding to this config class.
         */
        protected Class getArticulatedNodeClass ()
        {
            return Articulated.Node.class;
        }

        /**
         * Creates a new articulated node.
         */
        protected Articulated.Node createArticulatedNode (
            Scope scope, Transform3D parentViewTransform)
        {
            return new Articulated.Node(scope, this, parentViewTransform);
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
        protected Class getArticulatedNodeClass ()
        {
            return Articulated.MeshNode.class;
        }

        @Override // documentation inherited
        protected Articulated.Node createArticulatedNode (
            Scope scope, Transform3D parentViewTransform)
        {
            return new Articulated.MeshNode(scope, this, parentViewTransform);
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
     * Represents a transform to apply to a node.
     */
    @EditorTypes({ Billboard.class })
    public abstract class NodeTransform extends DeepObject
        implements Exportable
    {
        /** The name of the node representing the attachment point. */
        @Editable(editor="choice")
        public String node;

        /**
         * Returns the options available for the node field.
         */
        public String[] getNodeOptions ()
        {
            return getNodeNames();
        }

        /**
         * Creates the updater that will apply the node transform.
         */
        public abstract Updater createUpdater (Articulated.Node node);
    }

    /**
     * A billboard transform.
     */
    public class Billboard extends NodeTransform
    {
        /** The x rotation mode. */
        @Editable(hgroup="b")
        public BillboardRotationX rotationX = BillboardRotationX.ALIGN_TO_VIEW;

        /** The z rotation mode. */
        @Editable(hgroup="b")
        public BillboardRotationZ rotationZ = BillboardRotationZ.ALIGN_TO_VIEW;

        @Override // documentation inherited
        public Updater createUpdater (Articulated.Node node)
        {
            Transform3D viewTransform = node.getViewTransform();
            final Quaternion rotation = viewTransform.getRotation();
            if (rotationX == BillboardRotationX.ALIGN_TO_VIEW &&
                    rotationZ == BillboardRotationZ.ALIGN_TO_VIEW) {
                // simplest case; just clear the rotation
                return createUpdater(rotation, Quaternion.IDENTITY);
            }
            if (rotationX == BillboardRotationX.NONE &&
                    rotationZ == BillboardRotationZ.ALIGN_TO_VIEW) {
                // axial alignment; these all share the same rotation
                return createUpdater(
                    rotation, ScopeUtil.resolve(node, "billboardRotation", Quaternion.IDENTITY));
            }
            final Vector3f translation = viewTransform.getTranslation();
            if (rotationX == BillboardRotationX.FACE_VIEWER &&
                    rotationZ == BillboardRotationZ.ALIGN_TO_VIEW) {
                // pivot about the x axis
                return new Updater() {
                    public void update () {
                        rotation.fromAngleAxis(
                            FloatMath.atan2(translation.y, -translation.z), Vector3f.UNIT_X);
                    }
                };
            }
            if (rotationX == BillboardRotationX.ALIGN_TO_VIEW &&
                    rotationZ == BillboardRotationZ.FACE_VIEWER) {
                // pivot about the y axis
                return new Updater() {
                    public void update () {
                        rotation.fromAngleAxis(
                            FloatMath.atan2(-translation.x, -translation.z), Vector3f.UNIT_Y);
                    }
                };
            }
            if (rotationX == BillboardRotationX.FACE_VIEWER &&
                    rotationZ == BillboardRotationZ.FACE_VIEWER) {
                // pivot about the x and y axes
                return new Updater() {
                    public void update () {
                        rotation.fromAnglesXY(
                            FloatMath.atan2(translation.y, -translation.z),
                            FloatMath.atan2(-translation.x, -translation.z));
                    }
                };
            }
            // final possibility: no rotation about x, face viewer about z
            final Quaternion billboardRotation = ScopeUtil.resolve(
                node, "billboardRotation", Quaternion.IDENTITY);
            return new Updater() {
                public void update () {
                    rotation.fromAngleAxis(
                        FloatMath.atan2(-translation.x, -translation.z), Vector3f.UNIT_Y);
                    billboardRotation.mult(rotation, rotation);
                }
            };
        }

        /**
         * Creates an updater that simply sets the rotation to a referenced value.
         */
        protected Updater createUpdater (final Quaternion rotation, final Quaternion value)
        {
            return new Updater() {
                public void update () {
                    rotation.set(value);
                }
            };
        }
    }

    /**
     * Represents an attached model.
     */
    public class Attachment extends DeepObject
        implements Exportable
    {
        /** The name of the node representing the attachment point. */
        @Editable(editor="choice")
        public String node;

        /** The model to attach to the node. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        /**
         * Returns the options available for the node field.
         */
        public String[] getNodeOptions ()
        {
            return getNodeNames();
        }
    }

    /** The model's animation mappings. */
    @Editable
    public AnimationMapping[] animationMappings = new AnimationMapping[0];

    /** The model's node transforms. */
    @Editable(depends={"source"})
    public NodeTransform[] nodeTransforms = new NodeTransform[0];

    /** The model's attachments. */
    @Editable(depends={"source"})
    public Attachment[] attachments = new Attachment[0];

    /** The root node. */
    @Shallow
    public Node root;

    /** The skin meshes. */
    @Shallow
    public MeshSet skin;

    /**
     * Returns the names of the model's nodes in sorted order.
     */
    public String[] getNodeNames ()
    {
        if (root == null) {
            return new String[0];
        }
        ArrayList<String> names = new ArrayList<String>();
        root.getNames(names);
        QuickSort.sort(names);
        return names.toArray(new String[names.size()]);
    }

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
