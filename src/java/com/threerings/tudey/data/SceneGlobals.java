//
// $Id$

package com.threerings.tudey.data;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.math.Transform3D;
import com.threerings.util.DeepObject;

import com.threerings.opengl.model.config.ModelConfig;

/**
 * Contains the global scene properties.
 */
public class SceneGlobals extends DeepObject
    implements Exportable
{
    /**
     * A model to render as part of the scene's environment.
     */
    public static class EnvironmentModel extends DeepObject
        implements Exportable
    {
        /** The model reference. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        /** The transform of the model. */
        @Editable(step=0.01)
        public Transform3D transform = new Transform3D();
    }

    /** The models to render in the environment. */
    @Editable
    public EnvironmentModel[] environmentModels = new EnvironmentModel[0];
}
