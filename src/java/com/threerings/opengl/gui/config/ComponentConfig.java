//
// $Id$

package com.threerings.opengl.gui.config;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.gui.Label.Fit;
import com.threerings.opengl.gui.UIConstants;
import com.threerings.opengl.gui.config.LayoutConfig.Justification;
import com.threerings.opengl.gui.layout.GroupLayout;

/**
 * Contains a component configuration.
 */
@EditorTypes({
    ComponentConfig.Button.class, ComponentConfig.CheckBox.class,
    ComponentConfig.ComboBox.class, ComponentConfig.Container.class,
    ComponentConfig.Label.class, ComponentConfig.List.class,
    ComponentConfig.PasswordField.class, ComponentConfig.ScrollBar.class,
    ComponentConfig.ScrollPane.class, ComponentConfig.Slider.class,
    ComponentConfig.Spacer.class, ComponentConfig.TabbedPane.class,
    ComponentConfig.TextArea.class, ComponentConfig.TextField.class,
    ComponentConfig.ToggleButton.class, ComponentConfig.UserInterface.class })
public abstract class ComponentConfig extends DeepObject
    implements Exportable
{
    /** Available label orientations. */
    public enum Orientation
    {
        HORIZONTAL(UIConstants.HORIZONTAL),
        VERTICAL(UIConstants.VERTICAL);

        /**
         * Returns the corresponding UI constant.
         */
        public int getConstant ()
        {
            return _constant;
        }

        Orientation (int constant)
        {
            _constant = constant;
        }

        /** The corresponding UI constant. */
        protected int _constant;
    }

    /**
     * A label.
     */
    public static class Label extends ComponentConfig
    {
        /** The label's icon, if any. */
        @Editable(nullable=true)
        public IconConfig icon;

        /** The label's text. */
        @Editable(hgroup="t")
        public String text = "";

        /** The gap between icon and text. */
        @Editable(hgroup="t")
        public int iconTextGap = 3;

        /** The label orientation. */
        @Editable(hgroup="o")
        public Orientation orientation = Orientation.HORIZONTAL;

        /** Determines how to fit overlong text in the label. */
        @Editable(hgroup="o")
        public Fit fit = Fit.WRAP;
    }

    /**
     * A button.
     */
    public static class Button extends Label
    {
        /** The action to fire when the button is pressed. */
        @Editable(hgroup="a")
        public String action = "";
    }

    /**
     * A toggle button.
     */
    public static class ToggleButton extends Button
    {
        /** Whether or not the button is selected. */
        @Editable(hgroup="a")
        public boolean selected;
    }

    /**
     * A check box.
     */
    public static class CheckBox extends ToggleButton
    {
    }

    /**
     * A combo box.
     */
    public static class ComboBox extends ComponentConfig
    {
        /**
         * A single item in the list.
         */
        @EditorTypes({ StringItem.class, IconItem.class })
        public static abstract class Item extends DeepObject
            implements Exportable
        {
        }

        /**
         * A string item.
         */
        public static class StringItem extends Item
        {
            /** The text of the item. */
            @Editable
            public String text = "";
        }

        /**
         * An icon item.
         */
        public static class IconItem extends Item
        {
            /** The item icon. */
            @Editable(nullable=true)
            public IconConfig icon;
        }

        /** The items available for selection. */
        @Editable
        public Item[] items = new Item[0];

        /** The index of the selected item. */
        @Editable(min=0)
        public int selected;
    }

    /**
     * A list.
     */
    public static class List extends ComponentConfig
    {
        /** The items available for selection. */
        @Editable
        public String[] items = new String[0];

        /** The index of the selected item. */
        @Editable(min=0)
        public int selected;
    }

    /**
     * Base class for text components.
     */
    public static abstract class TextComponent extends ComponentConfig
    {
        /** The text in the component. */
        @Editable(hgroup="t")
        public String text = "";
    }

    /**
     * A text field.
     */
    public static class TextField extends TextComponent
    {
        /** The maximum length of the field (or zero for unlimited). */
        @Editable(min=0, hgroup="t")
        public int maxLength;
    }

    /**
     * A password field.
     */
    public static class PasswordField extends TextField
    {
    }

    /**
     * A text area.
     */
    public static class TextArea extends ComponentConfig
    {
        /** The text in the component. */
        @Editable(hgroup="t")
        public String text = "";

        /** The area's preferred width, or zero for none. */
        @Editable(min=0, hgroup="t")
        public int preferredWidth;
    }

    /**
     * A tabbed pane.
     */
    public static class TabbedPane extends ComponentConfig
    {
        /**
         * A single tab.
         */
        public static class Tab extends DeepObject
            implements Exportable
        {
            /** The tab title. */
            @Editable(hgroup="t")
            public String title = "";

            /** Whether or not the tab has a close button. */
            @Editable(hgroup="t")
            public boolean hasClose;

            /** The tab component. */
            @Editable
            public ComponentConfig component = new ComponentConfig.Spacer();
        }

        /** The tab alignment. */
        @Editable(hgroup="t")
        public Justification tabAlignment = Justification.LEFT;

        /** The tab gap. */
        @Editable(hgroup="t")
        public int gap = GroupLayout.DEFAULT_GAP;

        /** The tabs. */
        @Editable
        public Tab[] tabs = new Tab[0];
    }

    /**
     * A spacer.
     */
    public static class Spacer extends ComponentConfig
    {
    }

    /**
     * A slider.
     */
    public static class Slider extends ComponentConfig
    {
        /** The slider's orientation. */
        @Editable
        public Orientation orientation = Orientation.HORIZONTAL;

        /** The slider's model. */
        @Editable
        public BoundedRangeModelConfig model = new BoundedRangeModelConfig();
    }

    /**
     * A scroll pane.
     */
    public static class ScrollPane extends ComponentConfig
    {
        /** Whether or not to allow vertical scrolling. */
        @Editable(hgroup="v")
        public boolean vertical = true;

        /** Whether or not to allow horizontal scrolling. */
        @Editable(hgroup="v")
        public boolean horizontal;

        /** The snap value. */
        @Editable(hgroup="s")
        public int snap;

        /** Whether or not to always show the scrollbar. */
        @Editable(hgroup="s")
        public boolean showScrollbarAlways = true;

        /** The style for the viewport, if non-default. */
        @Editable(nullable=true)
        public ConfigReference<StyleConfig> viewportStyle;

        /** The child component. */
        @Editable
        public ComponentConfig child = new Spacer();
    }

    /**
     * A scroll bar.
     */
    public static class ScrollBar extends ComponentConfig
    {
        /** The scroll bar's orientation. */
        @Editable
        public Orientation orientation = Orientation.HORIZONTAL;

        /** The scroll bar's model. */
        @Editable
        public BoundedRangeModelConfig model = new BoundedRangeModelConfig();
    }

    /**
     * A container.
     */
    public static class Container extends ComponentConfig
    {
        /** The layout of the container. */
        @Editable
        public LayoutConfig layout = new LayoutConfig.Absolute();
    }

    /**
     * A config-based user interface component.
     */
    public static class UserInterface extends ComponentConfig
    {
        /** The user interface reference. */
        @Editable(nullable=true)
        public ConfigReference<UserInterfaceConfig> userInterface;
    }

    /** The component alpha value. */
    @Editable(min=0, max=1, step=0.01, weight=1, hgroup="c")
    public float alpha = 1f;

    /** Whether or not the component is enabled. */
    @Editable(weight=1, hgroup="c")
    public boolean enabled = true;

    /** Whether or not the component is visible. */
    @Editable(weight=1, hgroup="c")
    public boolean visible = true;

    /** The text for the component's tooltip. */
    @Editable(weight=1, hgroup="t")
    public String tooltipText = "";

    /** Whether or not the tooltip is relative to the mouse cursor. */
    @Editable(weight=1, hgroup="t")
    public boolean tooltipRelativeToMouse;

    /** The component's style, if non-default. */
    @Editable(weight=1, nullable=true)
    public ConfigReference<StyleConfig> style;

    /** The preferred size, if non-default. */
    @Editable(weight=1, nullable=true)
    public DimensionConfig preferredSize;
}
