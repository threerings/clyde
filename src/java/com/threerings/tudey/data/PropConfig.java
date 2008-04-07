//
// $Id$

package com.threerings.tudey.data;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Contains information about a prop.
 */
public class PropConfig extends SceneElementConfig
{
    /**
     * Contains a model to attach to the prop.
     */
    public static class Attachment
    {
        /** The model to attach. */
        public String model;

        /** The point at which to attach the model. */
        public String point;

        public Attachment (String model, String point)
        {
            this.model = model;
            this.point = point;
        }
    }

    /** The prop model. */
    public String model;

    /** The prop model variant. */
    public String variant;

    /** The radius of the prop's bounding cylinder. */
    public float radius;

    /** The height of the prop's bounding cylinder. */
    public float height;

    /** Whether or not actors can pass through the prop. */
    public boolean passable;

    /** Whether or not bullets can pass through the prop. */
    public boolean penetrable;

    /** Model attachments. */
    public Attachment[] attachments;

    /**
     * Returns the configuration of the named prop.
     */
    public static PropConfig getConfig (String name)
    {
        return _configs.get(name);
    }

    /**
     * Returns the configurations of all props.
     */
    public static Collection<PropConfig> getConfigs ()
    {
        return _configs.values();
    }

    @Override // documentation inherited
    public void getResources (Set<SceneResource> results)
    {
        super.getResources(results);
        results.add(new SceneResource.Model(model, variant));
        for (Attachment attachment : attachments) {
            results.add(new SceneResource.Model(attachment.model));
        }
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        model = getProperty("model", "world/prop/" + name + "/model");
        variant = getProperty("variant");
        radius = getProperty("radius", 0.5f);
        height = getProperty("height", 1f);
        passable = getProperty("passable", false);
        penetrable = passable || getProperty("penetrable", false);

        String[] attaches = getProperty("attachments", new String[0]);
        attachments = new Attachment[attaches.length];
        for (int ii = 0; ii < attachments.length; ii++) {
            String name = attaches[ii];
            attachments[ii] = new Attachment(
                name, getProperty(name + ".attach", (String)null));
        }
    }

    /** Maps prop names to their configurations. */
    protected static Map<String, PropConfig> _configs =
        loadConfigs(PropConfig.class, "world/prop/list.txt");
}
