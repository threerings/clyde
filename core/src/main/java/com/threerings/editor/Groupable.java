package com.threerings.editor;

import java.util.List;

/**
 * Implemented by editable object that can be grouped into another compatible type.
 * For example, ActionConfig has a subtype ActionConfig.Compound that allows for
 * grouping ActionConfigs. By implementing these methods the editor can allow people
 * to easily adjust a single action config into a group.
 */
public interface Groupable<E>
{
  /**
   * Access any grouped instances of the "same type" that are contained within this instance.
   * Return null, a internal collection (it won't be modified), or view / copy of the instances.
   */
  public List<E> getGrouped ();

  /**
   * Set the contained instances to the values specified, or throw if unable to do so for
   * any reason. The supplied List will never be null and will always have at least 1 element.
   */
  public void setGrouped (List<E> values)
    throws UnsupportedOperationException;
}
