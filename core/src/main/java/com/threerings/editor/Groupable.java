package com.threerings.editor;

import java.util.List;

/**
 * Implemented by editable object that can be grouped into another compatible type.
 * For example, ActionConfig has a subtype ActionConfig.Compound that allows for
 * grouping ActionConfigs. By implementing these methods the editor can allow people
 * to easily adjust a single action config into a group.
 */
public interface Groupable
{
    /**
     * Access any grouped instances of the "same type" that are contained within this instance.
     * Return null, a internal collection (it won't be modified), or view / copy of the instances.
     */
    public List<?> getGrouped ();

    /**
     * Set the contained instances to the values specified, or throw if unable to do so for
     * any reason.
     */
    public void setGrouped (List<?> values)
        throws UnsupportedOperationException;
}
