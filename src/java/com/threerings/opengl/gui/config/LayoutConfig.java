//
// $Id$

package com.threerings.opengl.gui.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.gui.layout.GroupLayout;
import com.threerings.opengl.gui.layout.TableLayout;

@EditorTypes({
    LayoutConfig.Absolute.class, LayoutConfig.Anchor.class,
    LayoutConfig.Border.class, LayoutConfig.HorizontalGroup.class,
    LayoutConfig.VerticalGroup.class, LayoutConfig.Table.class })
public abstract class LayoutConfig extends DeepObject
    implements Exportable
{
    /**
     * The horizontal alignment for table layouts.
     */
    public enum HorizontalAlignment
    {
        LEFT(TableLayout.LEFT),
        CENTER(TableLayout.CENTER),
        RIGHT(TableLayout.RIGHT),
        STRETCH(TableLayout.STRETCH);

        /**
         * Returns the corresponding alignment object.
         */
        public TableLayout.Alignment getAlignment ()
        {
            return _alignment;
        }

        HorizontalAlignment (TableLayout.Alignment alignment)
        {
            _alignment = alignment;
        }

        /** The corresponding alignment object. */
        protected TableLayout.Alignment _alignment;
    }

    /**
     * Vertical alignment for table layouts.
     */
    public enum VerticalAlignment
    {
        TOP(TableLayout.TOP),
        CENTER(TableLayout.CENTER),
        BOTTOM(TableLayout.BOTTOM);

        /**
         * Returns the corresponding alignment object.
         */
        public TableLayout.Alignment getAlignment ()
        {
            return _alignment;
        }

        VerticalAlignment (TableLayout.Alignment alignment)
        {
            _alignment = alignment;
        }

        /** The corresponding alignment object. */
        protected TableLayout.Alignment _alignment;
    }

    /**
     * On-axis group policies.
     */
    public enum OnAxisPolicy
    {
        NONE(GroupLayout.NONE),
        STRETCH(GroupLayout.STRETCH),
        EQUALIZE(GroupLayout.EQUALIZE);

        /**
         * Returns the corresponding group policy.
         */
        public GroupLayout.Policy getPolicy ()
        {
            return _policy;
        }

        OnAxisPolicy (GroupLayout.Policy policy)
        {
            _policy = policy;
        }

        /** The corresponding group policy. */
        protected GroupLayout.Policy _policy;
    }

    /**
     * Off-axis group policies.
     */
    public enum OffAxisPolicy
    {
        NONE(GroupLayout.NONE),
        STRETCH(GroupLayout.STRETCH),
        EQUALIZE(GroupLayout.EQUALIZE),
        CONSTRAIN(GroupLayout.CONSTRAIN);

        /**
         * Returns the corresponding group policy.
         */
        public GroupLayout.Policy getPolicy ()
        {
            return _policy;
        }

        OffAxisPolicy (GroupLayout.Policy policy)
        {
            _policy = policy;
        }

        /** The corresponding group policy. */
        protected GroupLayout.Policy _policy;
    }

    /**
     * The justification options.
     */
    public enum Justification
    {
        CENTER(GroupLayout.CENTER),
        LEFT(GroupLayout.LEFT),
        RIGHT(GroupLayout.RIGHT),
        TOP(GroupLayout.TOP),
        BOTTOM(GroupLayout.BOTTOM);

        /**
         * Returns the corresponding group justification.
         */
        public GroupLayout.Justification getJustification ()
        {
            return _justification;
        }

        Justification (GroupLayout.Justification justification)
        {
            _justification = justification;
        }

        /** The corresponding group justification. */
        protected GroupLayout.Justification _justification;
    }

    /**
     * An absolute layout.
     */
    public static class Absolute extends LayoutConfig
    {
        /**
         * Represents a child of the layout.
         */
        @EditorTypes({ Child.class, SizedChild.class })
        public static class Child extends DeepObject
            implements Exportable
        {
            /** The coordinates of the child. */
            @Editable(hgroup="c")
            public int x, y;

            /** The child component. */
            @Editable(weight=1)
            public ComponentConfig component = new ComponentConfig.Spacer();
        }

        /**
         * Represents a child with a fixed size.
         */
        public static class SizedChild extends Child
        {
            /** The dimensions of the child. */
            @Editable(hgroup="c")
            public int width, height;
        }

        /** Whether or not the coordinates are flipped. */
        @Editable
        public boolean flipped;

        /** The children of this layout. */
        @Editable
        public Child[] children = new Child[0];
    }

    /**
     * An anchor layout.
     */
    public static class Anchor extends LayoutConfig
    {
        /**
         * Represents a child of the layout.
         */
        public static class Child extends DeepObject
            implements Exportable
        {
            /** The proportional location of the anchor on the child. */
            @Editable(step=0.01, hgroup="c")
            public float childX = 0.5f, childY = 0.5f;

            /** The proportional location of the anchor on the parent. */
            @Editable(step=0.01, hgroup="p")
            public float parentX = 0.5f, parentY = 0.5f;

            /** The child component. */
            @Editable
            public ComponentConfig component = new ComponentConfig.Spacer();
        }

        /** The children of this layout. */
        @Editable
        public Child[] children = new Child[0];
    }

    /**
     * A border layout.
     */
    public static class Border extends LayoutConfig
    {
        /** The horizontal gap. */
        @Editable(hgroup="g")
        public int horizontalGap;

        /** The vertical gap. */
        @Editable(hgroup="g")
        public int verticalGap;

        /** The north component. */
        @Editable(nullable=true)
        public ComponentConfig north;

        /** The south component. */
        @Editable(nullable=true)
        public ComponentConfig south;

        /** The east component. */
        @Editable(nullable=true)
        public ComponentConfig east;

        /** The west component. */
        @Editable(nullable=true)
        public ComponentConfig west;

        /** The center component. */
        @Editable(nullable=true)
        public ComponentConfig center;
    }

    /**
     * Base class for {@link HorizontalGroup} and {@link VerticalGroup}.
     */
    public static abstract class Group extends LayoutConfig
    {
        /**
         * Represents a child of the layout.
         */
        public static class Child extends DeepObject
            implements Exportable
        {
            /** Whether or not the component's size is fixed. */
            @Editable(hgroup="f")
            public boolean fixed;

            /** The weight of this child when redistributing space. */
            @Editable(min=0, hgroup="f")
            public int weight = 1;

            /** The child component. */
            @Editable
            public ComponentConfig component = new ComponentConfig.Spacer();
        }

        /** The policy for the main axis. */
        @Editable(hgroup="p")
        public OnAxisPolicy policy = OnAxisPolicy.NONE;

        /** The policy for the off axis. */
        @Editable(hgroup="p")
        public OffAxisPolicy offAxisPolicy = OffAxisPolicy.CONSTRAIN;

        /** The gap between components. */
        @Editable(hgroup="p")
        public int gap = GroupLayout.DEFAULT_GAP;

        /** The justification for the main axis. */
        @Editable(hgroup="j")
        public Justification justification = Justification.CENTER;

        /** The justification on the off axis. */
        @Editable(hgroup="j")
        public Justification offAxisJustification = Justification.CENTER;

        /** The children of this layout. */
        @Editable
        public Child[] children = new Child[0];
    }

    /**
     * A horizontal group layout.
     */
    public static class HorizontalGroup extends Group
    {
    }

    /**
     * A vertical group layout.
     */
    public static class VerticalGroup extends Group
    {
    }

    /**
     * A table layout.
     */
    public static class Table extends LayoutConfig
    {
        /**
         * Represents a child of the layout.
         */
        public static class Child extends DeepObject
            implements Exportable
        {
            /** The child component. */
            @Editable
            public ComponentConfig component = new ComponentConfig.Spacer();
        }

        /** The number of columns in the table. */
        @Editable(min=1, hgroup="c")
        public int columns = 1;

        /** Whether or not to force all rows to be a uniform size. */
        @Editable(hgroup="c")
        public boolean equalRows;

        /** The gap between rows. */
        @Editable(hgroup="g")
        public int rowGap;

        /** The gap between columns. */
        @Editable(hgroup="g")
        public int columnGap;

        /** The horizontal alignment. */
        @Editable(hgroup="a")
        public HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;

        /** The vertical alignment. */
        @Editable(hgroup="a")
        public VerticalAlignment verticalAlignment = VerticalAlignment.TOP;

        /** The children of this layout. */
        @Editable
        public Child[] children = new Child[0];
    }
}
