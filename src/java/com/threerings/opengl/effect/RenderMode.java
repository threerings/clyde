//
// $Id$

package com.threerings.opengl.effect;

import java.io.File;
import java.io.FileInputStream;

import org.lwjgl.opengl.GL11;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.BinaryImporter;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.Shallow;

import com.threerings.opengl.model.StaticModel;
import com.threerings.opengl.model.VisibleMesh;
import com.threerings.opengl.model.tools.ModelReader;

import static com.threerings.opengl.Log.*;

/**
 * Determines how particles are rendered.
 */
@EditorTypes({
    RenderMode.Points.class, RenderMode.Lines.class,
    RenderMode.Quads.class, RenderMode.Meshes.class })
public abstract class RenderMode extends DeepObject
    implements Exportable
{
    /**
     * Renders particles as points.
     */
    public static class Points extends RenderMode
    {
        @Override // documentation inherited
        public ParticleGeometry createGeometry (int particles)
        {
            return new ParticleGeometry(GL11.GL_POINTS, particles, 0);
        }
    }

    /**
     * Renders particles as lines or line strips.
     */
    public static class Lines extends RenderMode
    {
        /** The number of segments in each line. */
        @Editable(min=0)
        public int segments;

        /**
         * Copies the segments parameter of the specified quad mode.
         */
        public Lines (Quads quads)
        {
            segments = quads.segments;
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public Lines ()
        {
        }

        @Override // documentation inherited
        public ParticleGeometry createGeometry (int particles)
        {
            return new ParticleGeometry(GL11.GL_LINES, particles, segments);
        }
    }

    /**
     * Renders particles as quads or quad strips.
     */
    public static class Quads extends RenderMode
    {
        /** The number of segments in each line. */
        @Editable(min=0)
        public int segments;

        /**
         * Copies the segments parameter of the specified line mode.
         */
        public Quads (Lines lines)
        {
            segments = lines.segments;
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public Quads ()
        {
        }

        @Override // documentation inherited
        public ParticleGeometry createGeometry (int particles)
        {
            return new ParticleGeometry(GL11.GL_TRIANGLES, particles, segments);
        }
    }

    /**
     * Renders particles as instances of a prototype mesh.
     */
    public static class Meshes extends RenderMode
    {
        /**
         * Sets the path to the prototype mesh to use.
         */
        @Editable(mode="compact", nullable=true)
        @FileConstraints(
            description="m.model_files",
            extensions={".properties", ".dat"},
            directory="model_dir")
        public void setModel (File model)
        {
            if ((_model = model) == null) {
                _mesh = null;
                return;
            }
            try {
                String name = model.getName().toLowerCase();
                StaticModel smodel;
                if (name.endsWith(".properties")) {
                    smodel = (StaticModel)ModelReader.read(model);
                } else {
                    BinaryImporter in = new BinaryImporter(new FileInputStream(model));
                    smodel = (StaticModel)in.readObject();
                }
                _mesh = smodel.getVisibleMeshes()[0];

            } catch (Exception e) {
                log.warning("Failed to load model.", e);
                _mesh = null;
            }
        }

        /**
         * Returns the path of the prototype model.
         */
        @Editable
        public File getModel ()
        {
            return _model;
        }

        @Override // documentation inherited
        public ParticleGeometry createGeometry (int particles)
        {
            if (_mesh != null) {
                return new ParticleGeometry(_mesh, particles);
            } else {
                return new ParticleGeometry(GL11.GL_TRIANGLES, particles, 0);
            }
        }

        /** The path of the prototype model. */
        protected File _model;

        /** The prototype mesh. */
        @Shallow
        protected VisibleMesh _mesh;
    }

    /**
     * Creates geometry object to render the specified number of particles.
     */
    public abstract ParticleGeometry createGeometry (int particles);
}
