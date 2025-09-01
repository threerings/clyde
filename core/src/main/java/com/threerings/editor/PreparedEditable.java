package com.threerings.editor;

import com.google.common.base.Function;

/**
 * An interface for objects that wish to lazily construct fields prior to editing.
 *
 * Boy, that sounds so passive, how about: YOU SHOULD USE THIS CLASS.
 * If your class, Foo, has an editable field in it that's a new Bar, that's ridiculous.
 * Because every time your instance is unserialized, anywhere! ANHYWHERE AT ALL!
 * Client/server/development/production, it will create a new instance and then throw it away
 * to be overwritten with the configured value. That's FUCKING STUPID.
 * And from now on, I'm not doing anything that's FUCKING STUPID.
 *
 * Old bad way
 * <code>
 * public class Foo {
 *    @Editable
 *    public Bar bar = new Bar();
 * }
 * </code>
 *
 * New way
 * <code>
 * public class Foo implements PreparedEditable {
 *    @Editable
 *    public Bar bar;
 *
 *    public void prepareInstanceToEdit () {
 *         bar = new Bar();
 *    }
 * }
 * </code>
 *
 * Annoying.
 */
public interface PreparedEditable
{
  public static final Function<Object, Object> PREPARER = new Function<Object, Object>() {
    public Object apply (Object instance)
    {
      if (instance instanceof PreparedEditable) {
        ((PreparedEditable)instance).prepareInstanceToEdit();
      }
      return instance;
    }
  };

  /**
   * Called to prepare this instance to be edited. Can instantiate sub-objects.
   */
  void prepareInstanceToEdit ();
}
