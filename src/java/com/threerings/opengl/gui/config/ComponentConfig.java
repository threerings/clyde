//
// $Id$

package com.threerings.opengl.gui.config;

import com.samskivert.util.StringUtil;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;

import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.Label.Fit;
import com.threerings.opengl.gui.UIConstants;
import com.threerings.opengl.gui.config.LayoutConfig.Justification;
import com.threerings.opengl.gui.layout.GroupLayout;
import com.threerings.opengl.util.GlContext;

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

        @Override // documentation inherited
        protected Component maybeRecreate (GlContext ctx, Scope scope, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.Label.class) ?
                comp : new com.threerings.opengl.gui.Label("");
        }

        @Override // documentation inherited
        protected void configure (GlContext ctx, Scope scope, Component comp)
        {
            super.configure(ctx, scope, comp);
            com.threerings.opengl.gui.Label label = (com.threerings.opengl.gui.Label)comp;
            label.setIcon(icon == null ? null : icon.getIcon(ctx));

        }
    }

    /**
     * A button.
     */
    public static class Button extends Label
    {
        /** The action to fire when the button is pressed. */
        @Editable(hgroup="a")
        public String action = "";

        @Override // documentation inherited
        protected Component maybeRecreate (GlContext ctx, Scope scope, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.Button.class) ?
                comp : new com.threerings.opengl.gui.Button("");
        }
    }

    /**
     * A toggle button.
     */
    public static class ToggleButton extends Button
    {
        /** Whether or not the button is selected. */
        @Editable(hgroup="a")
        public boolean selected;

        @Override // documentation inherited
        protected Component maybeRecreate (GlContext ctx, Scope scope, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.ToggleButton.class) ?
                comp : new com.threerings.opengl.gui.ToggleButton("");
        }
    }

    /**
     * A check box.
     */
    public static class CheckBox extends ToggleButton
    {
        @Override // documentation inherited
        protected Component maybeRecreate (GlContext ctx, Scope scope, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.CheckBox.class) ?
                comp : new com.threerings.opengl.gui.CheckBox("");
        }
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

        @Override // documentation inherited
        protected Component maybeRecreate (GlContext ctx, Scope scope, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.ComboBox.class) ?
                comp : new com.threerings.opengl.gui.ComboBox();
        }
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

        @Override // documentation inherited
        protected Component maybeRecreate (GlContext ctx, Scope scope, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.List.class) ?
                comp : new com.threerings.opengl.gui.List();
        }
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

        @Override // documentation inherited
        protected Component maybeRecreate (GlContext ctx, Scope scope, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.TextField.class) ?
                comp : new com.threerings.opengl.gui.TextField();
        }
    }

    /**
     * A password field.
     */
    public static class PasswordField extends TextField
    {
        @Override // documentation inherited
        protected Component maybeRecreate (GlContext ctx, Scope scope, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.PasswordField.class) ?
                comp : new com.threerings.opengl.gui.PasswordField();
        }
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

        @Override // documentation inherited
        protected Component maybeRecreate (GlContext ctx, Scope scope, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.TextArea.class) ?
                comp : new com.threerings.opengl.gui.TextArea();
        }
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

        @Override // documentation inherited
        protected Component maybeRecreate (GlContext ctx, Scope scope, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.TabbedPane.class) ?
                comp : new com.threerings.opengl.gui.TabbedPane();
        }
    }

    /**
     * A spacer.
     */
    public static class Spacer extends ComponentConfig
    {
        @Override // documentation inherited
        protected Component maybeRecreate (GlContext ctx, Scope scope, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.Spacer.class) ?
                comp : new com.threerings.opengl.gui.Spacer();
        }
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

        @Override // documentation inherited
        protected Component maybeRecreate (GlContext ctx, Scope scope, Component comp)
        {
            return new com.threerings.opengl.gui.Slider(
                orientation.getConstant(), model.createBoundedRangeModel());
        }
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

        @Override // documentation inherited
        protected Component maybeRecreate (GlContext ctx, Scope scope, Component comp)
        {
            return null;
        }
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

        @Override // documentation inherited
        protected Component maybeRecreate (GlContext ctx, Scope scope, Component comp)
        {
            return new com.threerings.opengl.gui.ScrollBar(
                orientation.getConstant(), model.createBoundedRangeModel());
        }
    }

    /**
     * A container.
     */
    public static class Container extends ComponentConfig
    {
        /** The layout of the container. */
        @Editable
        public LayoutConfig layout = new LayoutConfig.Absolute();

        @Override // documentation inherited
        protected Component maybeRecreate (GlContext ctx, Scope scope, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.Container.class) ?
                comp : new com.threerings.opengl.gui.Container();
        }
    }

    /**
     * A config-based user interface component.
     */
    public static class UserInterface extends ComponentConfig
    {
        /** The user interface reference. */
        @Editable(nullable=true)
        public ConfigReference<UserInterfaceConfig> userInterface;

        @Override // documentation inherited
        protected Component maybeRecreate (GlContext ctx, Scope scope, Component comp)
        {
            return null;
        }
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

    /**
     * Creates or updates a component for this configuration.
     *
     * @param scope the component's expression scope.
     * @param comp an existing component to reuse, if possible.
     * @return either a reference to the existing component (if reused) or a new component.
     */
    public Component getComponent (GlContext ctx, Scope scope, Component comp)
    {
        comp = maybeRecreate(ctx, scope, comp);
        configure(ctx, scope, comp);
        return comp;
    }

    /**
     * Recreates the component if the supplied component doesn't match the configuration.
     */
    protected abstract Component maybeRecreate (GlContext ctx, Scope scope, Component comp);

    /**
     * Configures the specified component.
     */
    protected void configure (GlContext ctx, Scope scope, Component comp)
    {
        comp.setAlpha(alpha);
        comp.setEnabled(enabled);
        comp.setVisible(visible);
        if (!StringUtil.isBlank(tooltipText)) {
            comp.setTooltipText(tooltipText);
        }
        comp.setTooltipRelativeToMouse(tooltipRelativeToMouse);
        comp.setStyleConfig(style);
        if (preferredSize != null) {
            comp.setPreferredSize(preferredSize.createDimension());
        }
    }

    /**
     * Returns the class of the specified object, or <code>null</code> if the reference is null.
     */
    protected static Class getClass (Object object)
    {
        return (object == null) ? null : object.getClass();
    }
}
