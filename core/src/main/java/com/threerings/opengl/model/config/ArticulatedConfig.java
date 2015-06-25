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

package com.threerings.opengl.model.config;

import java.io.IOException;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.TreeSet;

import proguard.annotation.Keep;

import com.samskivert.util.ArrayUtil;
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
import com.threerings.math.Box;
import com.threerings.math.FloatMath;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;
import com.threerings.util.Shallow;

import com.threerings.opengl.model.Articulated;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.CollisionMesh;
import com.threerings.opengl.model.config.ModelConfig.MeshSet;
import com.threerings.opengl.model.config.ModelConfig.VisibleMesh;
import com.threerings.opengl.model.tools.ModelDef;
import com.threerings.opengl.scene.SceneElement.TickPolicy;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Preloadable;

import static com.threerings.opengl.Log.log;

/**
 * An original articulated implementation.
 */
public class ArticulatedConfig extends ModelConfig.Imported
{
    /** The options for billboard rotation about the x axis. */
    public enum BillboardRotationX { ALIGN_TO_VIEW, FACE_VIEWER, NONE };

    /** The options for billboard rotation about the y axis. */
    public enum BillboardRotationY { ALIGN_TO_VIEW, FACE_VIEWER };

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
         * Returns the bounds of the node's geometry.
         */
        public Box getBounds ()
        {
            return Box.EMPTY;
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
         * @param onodes the existing nodes to reuse.
         * @param nnodes the list to contain the new nodes.
         */
        public void getArticulatedNodes (
            GlContext ctx, Scope scope, IdentityHashMap<Node, Articulated.Node> onodes,
            ArrayList<Articulated.Node> nnodes, Transform3D parentWorldTransform,
            Transform3D parentViewTransform)
        {
            Articulated.Node node = onodes.remove(this);
            if (node != null) {
                node.setConfig(this, parentWorldTransform, parentViewTransform);
            } else {
                node = createArticulatedNode(
                    ctx, scope, parentWorldTransform, parentViewTransform);
            }
            nnodes.add(node);
            Transform3D worldTransform = node.getWorldTransform();
            Transform3D viewTransform = node.getViewTransform();
            for (Node child : children) {
                child.getArticulatedNodes(
                    ctx, scope, onodes, nnodes, worldTransform, viewTransform);
            }
        }

        /**
         * Creates a new articulated node.
         */
        protected Articulated.Node createArticulatedNode (
            GlContext ctx, Scope scope, Transform3D parentWorldTransform,
            Transform3D parentViewTransform)
        {
            return new Articulated.Node(
                ctx, scope, this, parentWorldTransform, parentViewTransform);
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

        @Override
        public void getTextures (TreeSet<String> textures)
        {
            super.getTextures(textures);
            if (visible != null) {
                textures.add(visible.texture);
            }
        }

        @Override
        public void getTextureTagPairs (TreeSet<ComparableTuple<String, String>> pairs)
        {
            super.getTextureTagPairs(pairs);
            if (visible != null) {
                pairs.add(new ComparableTuple<String, String>(visible.texture, visible.tag));
            }
        }

        @Override
        public Box getBounds ()
        {
            return (visible == null) ? collision.getBounds() : visible.geometry.getBounds();
        }

        @Override
        protected Articulated.Node createArticulatedNode (
            GlContext ctx, Scope scope, Transform3D parentWorldTransform,
            Transform3D parentViewTransform)
        {
            return new Articulated.MeshNode(
                ctx, scope, this, parentWorldTransform, parentViewTransform);
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

        /** Whether or not to start this animation automatically. */
        @Editable(hgroup="s")
        public boolean startAutomatically;

        /** Whether or not to start this animation after a config update. */
        @Editable(hgroup="s")
        public boolean startOnUpdated;
    }

    /**
     * Represents a transform to apply to a node.
     */
    @EditorTypes({ Billboard.class, Upright.class })
    public abstract class NodeTransform extends DeepObject
        implements Exportable
    {
        /** The name of the node representing the attachment point. */
        @Editable(editor="choice")
        public String node;

        /**
         * Returns the options available for the node field.
         */
        @Keep
        public String[] getNodeOptions ()
        {
            return getNodeNames();
        }

        /**
         * Creates the updater that will apply the node transform.
         */
        public abstract Updater createUpdater (GlContext ctx, Articulated.Node node);
    }

    /**
     * Marker interface for updaters that act on the world transform.
     */
    public interface WorldTransformUpdater extends Updater
    {
    }

    /**
     * Marker interface for updaters that act on the view transform.
     */
    public interface ViewTransformUpdater extends Updater
    {
    }

    /**
     * A billboard transform.
     */
    public class Billboard extends NodeTransform
    {
        /** The x rotation mode. */
        @Editable(hgroup="b")
        public BillboardRotationX rotationX = BillboardRotationX.ALIGN_TO_VIEW;

        /** The y rotation mode. */
        @Editable(hgroup="b")
        public BillboardRotationY rotationY = BillboardRotationY.ALIGN_TO_VIEW;

        @Override
        public Updater createUpdater (GlContext ctx, Articulated.Node node)
        {
            return createBillboardUpdater(
                node, node.getParentViewTransform(),
                node.getLocalTransform(), rotationX, rotationY);
        }
    }

    /**
     * A transform that orients the node vertically with respect to the world coordinate system.
     */
    public class Upright extends NodeTransform
    {
        /** Whether or not the transform incorporates the node's direction. */
        @Editable
        public boolean directional;

        @Override
        public Updater createUpdater (final GlContext ctx, Articulated.Node node)
        {
            final Transform3D pworld = node.getParentWorldTransform();
            final Transform3D local = node.getLocalTransform();
            if (!directional) {
                return new WorldTransformUpdater() {
                    public void update () {
                        ensureRigidOrUniform(local);
                        pworld.extractRotation(local.getRotation()).invertLocal();
                    }
                };
            }
            return new WorldTransformUpdater() {
                public void update () {
                    ensureRigidOrUniform(local);
                    Quaternion lrot = local.getRotation();
                    pworld.extractRotation(lrot).invertLocal();
                    lrot.transformUnitZ(_lup).normalizeLocal();
                    lrot.fromVectors(Vector3f.UNIT_Z, _lup);
                }
                protected Vector3f _lup = new Vector3f();
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

        /** The transform of the model. */
        @Editable(step=0.01)
        public Transform3D transform = new Transform3D();

        /**
         * Returns the options available for the node field.
         */
        @Keep
        public String[] getNodeOptions ()
        {
            return getNodeNames();
        }
    }

    /** The model's tick policy. */
    @Editable(hgroup="t")
    public TickPolicy tickPolicy = TickPolicy.DEFAULT;

    /** Whether or not the model can be completed. */
    @Editable(hgroup="t")
    public boolean completable;

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
     * Creates an updater to apply a billboard transform.
     */
    public static Updater createBillboardUpdater (
        Scope scope, final Transform3D parentViewTransform, final Transform3D localTransform,
        BillboardRotationX rotationX, BillboardRotationY rotationY)
    {
        if (rotationX == BillboardRotationX.ALIGN_TO_VIEW &&
                rotationY == BillboardRotationY.ALIGN_TO_VIEW) {
            return new ViewTransformUpdater() {
                public void update () {
                    ensureRigidOrUniform(localTransform);
                    parentViewTransform.extractRotation(
                        localTransform.getRotation()).invertLocal();
                }
            };
        }
        if (rotationX == BillboardRotationX.NONE &&
                rotationY == BillboardRotationY.ALIGN_TO_VIEW) {
            final Quaternion brot = ScopeUtil.resolve(
                scope, "billboardRotation", Quaternion.IDENTITY);
            return new ViewTransformUpdater() {
                public void update () {
                    ensureRigidOrUniform(localTransform);
                    parentViewTransform.extractRotation(
                        localTransform.getRotation()).invertLocal().multLocal(brot);
                }
            };
        }
        if (rotationX == BillboardRotationX.FACE_VIEWER &&
                rotationY == BillboardRotationY.ALIGN_TO_VIEW) {
            // pivot about the x axis
            return new ViewTransformUpdater() {
                public void update () {
                    ensureRigidOrUniform(localTransform);
                    parentViewTransform.transformPoint(localTransform.getTranslation(), _trans);
                    _rot.fromAngleAxis(FloatMath.atan2(_trans.y, -_trans.z), Vector3f.UNIT_X);
                    parentViewTransform.extractRotation(
                        localTransform.getRotation()).invertLocal().multLocal(_rot);
                }
                protected Vector3f _trans = new Vector3f();
                protected Quaternion _rot = new Quaternion();
            };
        }
        if (rotationX == BillboardRotationX.ALIGN_TO_VIEW &&
                rotationY == BillboardRotationY.FACE_VIEWER) {
            // pivot about the y axis
            return new ViewTransformUpdater() {
                public void update () {
                    ensureRigidOrUniform(localTransform);
                    parentViewTransform.transformPoint(localTransform.getTranslation(), _trans);
                    _rot.fromAngleAxis(FloatMath.atan2(-_trans.x, -_trans.z), Vector3f.UNIT_Y);
                    parentViewTransform.extractRotation(
                        localTransform.getRotation()).invertLocal().multLocal(_rot);
                }
                protected Vector3f _trans = new Vector3f();
                protected Quaternion _rot = new Quaternion();
            };
        }
        if (rotationX == BillboardRotationX.FACE_VIEWER &&
                rotationY == BillboardRotationY.FACE_VIEWER) {
            // pivot about the x and y axes
            return new ViewTransformUpdater() {
                public void update () {
                    ensureRigidOrUniform(localTransform);
                    parentViewTransform.transformPoint(localTransform.getTranslation(), _trans);
                    _rot.fromAnglesXY(
                        FloatMath.atan2(_trans.y, -_trans.z),
                        FloatMath.atan2(-_trans.x, -_trans.z));
                    parentViewTransform.extractRotation(
                        localTransform.getRotation()).invertLocal().multLocal(_rot);
                }
                protected Vector3f _trans = new Vector3f();
                protected Quaternion _rot = new Quaternion();
            };
        }
        // final possibility: no rotation about x, face viewer about y
        final Quaternion brot = ScopeUtil.resolve(
            scope, "billboardRotation", Quaternion.IDENTITY);
        return new ViewTransformUpdater() {
            public void update () {
                ensureRigidOrUniform(localTransform);
                parentViewTransform.transformPoint(localTransform.getTranslation(), _trans);
                _rot.fromAngleAxis(FloatMath.atan2(-_trans.x, -_trans.z), Vector3f.UNIT_Y);
                brot.mult(_rot, _rot);
                parentViewTransform.extractRotation(
                    localTransform.getRotation()).invertLocal().multLocal(_rot);
            }
            protected Vector3f _trans = new Vector3f();
            protected Quaternion _rot = new Quaternion();
        };
    }

    /**
     * Returns the names of the model's nodes in sorted order.
     */
    public String[] getNodeNames ()
    {
        if (root == null) {
            return ArrayUtil.EMPTY_STRING;
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

    @Override
    public void preload (GlContext ctx)
    {
        for (AnimationMapping mapping : animationMappings) {
            new Preloadable.Animation(mapping.animation).preload(ctx);
        }
    }

    @Override
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        if (root == null) {
            return null;
        }
        if (impl instanceof Articulated) {
            ((Articulated)impl).setConfig(ctx, this);
        } else {
            impl = new Articulated(ctx, scope, this);
        }
        return impl;
    }

    @Override
    protected void updateFromSource (ModelDef def)
    {
        if (def == null) {
            root = null;
            skin = null;
        } else {
            def.update(this);
        }
    }

    @Override
    protected void getTextures (TreeSet<String> textures)
    {
        if (root != null) {
            root.getTextures(textures);
        }
        if (skin != null) {
            skin.getTextures(textures);
        }
    }

    @Override
    protected void getTextureTagPairs (TreeSet<ComparableTuple<String, String>> pairs)
    {
        if (root != null) {
            root.getTextureTagPairs(pairs);
        }
        if (skin != null) {
            skin.getTextureTagPairs(pairs);
        }
    }

    /**
     * Ensures that the specified transform is rigid or uniform.
     */
    protected static void ensureRigidOrUniform (Transform3D transform)
    {
        switch (transform.getType()) {
            case Transform3D.IDENTITY:
                transform.promote(Transform3D.UNIFORM);
                break;

            case Transform3D.AFFINE:
            case Transform3D.GENERAL:
                Vector3f trans = transform.getTranslation();
                transform.set(
                    transform.extractTranslation(trans == null ? new Vector3f() : trans),
                    Quaternion.IDENTITY,
                    transform.approximateUniformScale());
                break;
        }
    }
}
