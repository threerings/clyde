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

package com.threerings.opengl.gui.config;

import com.google.common.collect.Lists;

import com.samskivert.text.MessageUtil;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.StringUtil;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.DynamicScope;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.FloatMath;
import com.threerings.math.Transform3D;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.opengl.camera.OrbitCameraHandler;
import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.Label.Fit;
import com.threerings.opengl.gui.UIConstants;
import com.threerings.opengl.gui.config.LayoutConfig.Justification;
import com.threerings.opengl.gui.layout.GroupLayout;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.util.GlContext;

/**
 * Contains a component configuration.
 */
@EditorTypes({
    ComponentConfig.Button.class, ComponentConfig.ChatOverlay.class,
    ComponentConfig.CheckBox.class, ComponentConfig.ColorPicker.class,
    ComponentConfig.ComboBox.class, ComponentConfig.Container.class,
    ComponentConfig.FadeLabel.class, ComponentConfig.HTMLView.class, ComponentConfig.Label.class,
    ComponentConfig.List.class, ComponentConfig.PasswordField.class,
    ComponentConfig.RenderableView.class, ComponentConfig.ScrollBar.class,
    ComponentConfig.ScrollPane.class, ComponentConfig.Slider.class,
    ComponentConfig.Spacer.class, ComponentConfig.Spinner.class,
    ComponentConfig.StatusLabel.class, ComponentConfig.TabbedPane.class,
    ComponentConfig.TextArea.class, ComponentConfig.TextEditor.class,
    ComponentConfig.TextField.class, ComponentConfig.ToggleButton.class,
    ComponentConfig.UserInterface.class })
public abstract class ComponentConfig extends DeepObject
    implements Exportable
{
    /** Are we highlighted in the editor? */
    @DeepOmit
    public transient boolean editorHighlight;

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
        protected final int _constant;
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

        /** The rotation for the text. */
        @Editable(hgroup="t")
        public int textRotation;

        /** The label orientation. */
        @Editable(hgroup="o")
        public Orientation orientation = Orientation.HORIZONTAL;

        /** Determines how to fit overlong text in the label. */
        @Editable(hgroup="o")
        public Fit fit = Fit.WRAP;

        /** The label's preferred width, or zero for none. */
        @Editable(min=0, hgroup="o")
        public int preferredWidth;

        @Override
        public void invalidate ()
        {
            if (icon != null) {
                icon.invalidate();
            }
        }

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.Label.class) ?
                comp : new com.threerings.opengl.gui.Label(ctx, "");
        }

        @Override
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.Label label = (com.threerings.opengl.gui.Label)comp;
            if (shouldSetIcon(ctx, comp)) {
                label.setIcon(icon == null ? null : icon.getIcon(ctx));
            }
            label.setText(getMessage(msgs, text));
            label.setIconTextGap(iconTextGap);
            label.setTextRotation(textRotation);
            label.setOrientation(orientation.getConstant());
            label.setFit(fit);
            label.setPreferredWidth(preferredWidth);
        }

        /**
         * Determines whether we should configure the label's icon.
         */
        protected boolean shouldSetIcon (GlContext ctx, Component comp)
        {
            return true;
        }
    }

    /**
     * A status label.
     */
    public static class StatusLabel extends Label
    {
        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.StatusLabel.class) ?
                comp : new com.threerings.opengl.gui.StatusLabel(ctx);
        }
    }

    /**
     * A fade label.
     */
    public static class FadeLabel extends Label
    {
        /** The label's line fade time. */
        @Editable(min=0)
        public int lineFadeTime;

        @Override
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            ((com.threerings.opengl.gui.FadeLabel)comp).setLineFadeTime(lineFadeTime);
        }

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.FadeLabel.class) ?
                comp : new com.threerings.opengl.gui.FadeLabel(ctx, 0);
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

        /** The argument associated with the action. */
        @Editable(hgroup="a", nullable=true)
        public Object argument;

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.Button.class) ?
                comp : new com.threerings.opengl.gui.Button(ctx, "");
        }

        @Override
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            ((com.threerings.opengl.gui.Button)comp).setAction(action);
            ((com.threerings.opengl.gui.Button)comp).setArgument(argument);
        }

        @Override
        protected boolean shouldSetIcon (GlContext ctx, Component comp)
        {
            StyleConfig config = comp.getStyleConfigs()[Component.DEFAULT];
            StyleConfig.Original original = (config == null) ? null : config.getOriginal(ctx);
            return (original == null || original.icon == null);
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

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.ToggleButton.class) ?
                comp : new com.threerings.opengl.gui.ToggleButton(ctx, "");
        }

        @Override
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            ((com.threerings.opengl.gui.ToggleButton)comp).setSelected(selected);
        }
    }

    /**
     * A check box.
     */
    public static class CheckBox extends ToggleButton
    {
        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.CheckBox.class) ?
                comp : new com.threerings.opengl.gui.CheckBox(ctx, "");
        }

        @Override
        protected boolean shouldSetIcon (GlContext ctx, Component comp)
        {
            return false; // icon is determined by the style
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
            /**
             * Returns the object corresponding to this item.
             */
            public abstract Object getObject (GlContext ctx, MessageBundle msgs);

            /**
             * Invalidates any cached data.
             */
            public void invalidate ()
            {
                // nothing by default
            }
        }

        /**
         * A string item.
         */
        public static class StringItem extends Item
        {
            /** The text of the item. */
            @Editable
            public String text = "";

            @Override
            public Object getObject (GlContext ctx, MessageBundle msgs)
            {
                return getMessage(msgs, text);
            }
        }

        /**
         * An icon item.
         */
        public static class IconItem extends Item
        {
            /** The item icon. */
            @Editable(nullable=true)
            public IconConfig icon;

            @Override
            public Object getObject (GlContext ctx, MessageBundle msgs)
            {
                return (icon == null) ? null : icon.getIcon(ctx);
            }

            @Override
            public void invalidate ()
            {
                if (icon != null) {
                    icon.invalidate();
                }
            }
        }

        /** The items available for selection. */
        @Editable
        public Item[] items = new Item[0];

        /** The dimensions of the popup menu. */
        @Editable(min=0, hgroup="s")
        public int rows = 8, columns = 1;

        /** The index of the selected item. */
        @Editable(min=0, hgroup="s")
        public int selected;

        @Override
        public void invalidate ()
        {
            for (Item item : items) {
                item.invalidate();
            }
        }

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.ComboBox.class) ?
                comp : new com.threerings.opengl.gui.ComboBox<Object>(ctx);
        }

        @Override
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            @SuppressWarnings("unchecked") // it'll be all right
            com.threerings.opengl.gui.ComboBox<Object> box =
                (com.threerings.opengl.gui.ComboBox<Object>)comp;
            int nn = items.length;
            java.util.List<Object> objects = Lists.newArrayListWithCapacity(nn);
            for (int ii = 0; ii < nn; ii++) {
                objects.add(items[ii].getObject(ctx, msgs));
            }
            box.setItems(objects);
            box.setPreferredDimensions(rows, columns);
            box.setSelectedIndex(selected);
        }
    }

    /**
     * A list.
     */
    public static class List extends ComponentConfig
    {
        /** The items available for selection. */
        @Editable
        public String[] items = ArrayUtil.EMPTY_STRING;

        /** The index of the selected item. */
        @Editable(min=0)
        public int selected;

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.List.class) ?
                comp : new com.threerings.opengl.gui.List(ctx);
        }

        @Override
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.List list = (com.threerings.opengl.gui.List)comp;
            Object[] values = new Object[items.length];
            for (int ii = 0; ii < items.length; ii++) {
                values[ii] = getMessage(msgs, items[ii]);
            }
            list.setValues(values);
            list.setSelected(selected < values.length ? values[selected] : null);
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

        @Override
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            ((com.threerings.opengl.gui.TextComponent)comp).setText(getMessage(msgs, text));
        }
    }

    /**
     * A text field.
     */
    public static class TextField extends TextComponent
    {
        /** The maximum length of the field (or zero for unlimited). */
        @Editable(min=0, hgroup="t")
        public int maxLength;

        @Editable(hgroup="t")
        public String placeholder = "";

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.TextField.class) ?
                comp : new com.threerings.opengl.gui.TextField(ctx);
        }

        @Override
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            com.threerings.opengl.gui.EditableTextComponent ecomp =
                (com.threerings.opengl.gui.EditableTextComponent)comp;
            // setMaxLength must be called before setText
            ecomp.setMaxLength(maxLength);
            ecomp.setPlaceholder(getMessage(msgs, placeholder));
            super.configure(ctx, scope, msgs, comp);
        }
    }

    /**
     * A multi-line text editor.
     */
    public static class TextEditor extends TextField
    {
        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.TextEditor.class) ?
                comp : new com.threerings.opengl.gui.TextEditor(ctx);
        }
    }

    /**
     * A password field.
     */
    public static class PasswordField extends TextField
    {
        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.PasswordField.class) ?
                comp : new com.threerings.opengl.gui.PasswordField(ctx);
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

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.TextArea.class) ?
                comp : new com.threerings.opengl.gui.TextArea(ctx);
        }

        @Override
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.TextArea area = (com.threerings.opengl.gui.TextArea)comp;
            area.setPreferredWidth(preferredWidth);
            area.setText(getMessage(msgs, text));
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

            /** An optional override style for the tab button. */
            @Editable(nullable=true)
            public ConfigReference<StyleConfig> styleOverride;

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

        /** The selected tab. */
        @Editable(min=0, hgroup="t")
        public int selected;

        /** The style for the tabs, if non-default. */
        @Editable(nullable=true)
        public ConfigReference<StyleConfig> tabStyle;

        /** The tabs. */
        @Editable
        public Tab[] tabs = new Tab[0];

        @Override
        public void invalidate ()
        {
            for (Tab tab : tabs) {
                tab.component.invalidate();
            }
        }

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.TabbedPane.class) ?
                comp : new com.threerings.opengl.gui.TabbedPane(ctx);
        }

        @Override
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.TabbedPane pane = (com.threerings.opengl.gui.TabbedPane)comp;
            Component[] otabs = new Component[pane.getTabCount()];
            for (int ii = 0; ii < otabs.length; ii++) {
                otabs[ii] = pane.getTab(ii);
            }
            pane.removeAllTabs();
            pane.setTabAlignment(tabAlignment.getJustification());
            pane.setGap(gap);
            for (int ii = 0; ii < tabs.length; ii++) {
                Tab tab = tabs[ii];
                Component tcomp = (ii < otabs.length) ? otabs[ii] : null;
                pane.addTab(
                    getMessage(msgs, tab.title),
                    tab.component.getComponent(ctx, scope, msgs, tcomp),
                    tab.hasClose,
                    tab.styleOverride == null ? tabStyle : tab.styleOverride);
            }
            if (selected < tabs.length) {
                pane.setSelectedIndex(selected);
            }
        }
    }

    /**
     * A spacer.
     */
    public static class Spacer extends ComponentConfig
    {
        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.Spacer.class) ?
                comp : new com.threerings.opengl.gui.Spacer(ctx);
        }
    }

    public static class Spinner extends ComponentConfig
    {
        /** The style for the editor, if non-default. */
        @Editable(nullable=true)
        public ConfigReference<StyleConfig> editorStyle;

        /** The style for the next button, if non-default. */
        @Editable(nullable=true)
        public ConfigReference<StyleConfig> nextStyle;

        /** The style for the previous button, if non-default. */
        @Editable(nullable=true)
        public ConfigReference<StyleConfig> previousStyle;

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.Spinner.class) ?
                comp : new com.threerings.opengl.gui.Spinner(ctx);
        }

        @Override
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.Spinner spinner = (com.threerings.opengl.gui.Spinner)comp;
            spinner.setEditorStyleConfig(editorStyle);
            spinner.setNextStyleConfig(nextStyle);
            spinner.setPreviousStyleConfig(previousStyle);
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

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return new com.threerings.opengl.gui.Slider(
                ctx, orientation.getConstant(), model.createBoundedRangeModel());
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

        /** If we show only buttons. */
        @Editable(hgroup="v")
        public boolean buttons;

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

        @Override
        public void invalidate ()
        {
            child.invalidate();
        }

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            _ochild = (comp instanceof com.threerings.opengl.gui.ScrollPane) ?
                ((com.threerings.opengl.gui.ScrollPane)comp).getChild() : null;
            if (_ochild != null) {
                _ochild.getParent().remove(_ochild);
            }
            // Don't initialize the scroll pane here, do it after configuration.
            return new com.threerings.opengl.gui.ScrollPane(ctx);
        }

        @Override
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.ScrollPane pane = (com.threerings.opengl.gui.ScrollPane)comp;
            pane.init(child.getComponent(ctx, scope, msgs, _ochild),
                vertical, horizontal, snap, buttons);
            pane.setShowScrollbarAlways(showScrollbarAlways);
            pane.setViewportStyleConfig(viewportStyle);
        }

        /** The cached original child of this scroll pane, if any. */
        @DeepOmit
        protected transient Component _ochild;
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

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return new com.threerings.opengl.gui.ScrollBar(
                ctx, orientation.getConstant(), model.createBoundedRangeModel());
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

        @Override
        public void invalidate ()
        {
            layout.invalidate();
        }

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.Container.class) ?
                comp : new com.threerings.opengl.gui.Container(ctx);
        }

        @Override
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            layout.configure(ctx, scope, msgs, (com.threerings.opengl.gui.Container)comp);
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

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.UserInterface.class) ?
                comp : new com.threerings.opengl.gui.UserInterface(ctx);
        }

        @Override
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.UserInterface ui =
                (com.threerings.opengl.gui.UserInterface)comp;
            ui.getScope().setParentScope(scope);
            ui.setConfig(userInterface);
            ScopeUtil.call(scope, "registerComponents", ui.getTagged());
        }
    }

    /**
     * An HTML view.
     */
    public static class HTMLView extends ComponentConfig
    {
        /** The view's stylesheet. */
        @Editable
        public String stylesheet = "";

        /** The contents of the view. */
        @Editable
        public String contents = "";

        /** Whether or not the view should be antialiased. */
        @Editable
        public boolean antialias = true;

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.text.HTMLView.class) ?
                comp : new com.threerings.opengl.gui.text.HTMLView(ctx);
        }

        @Override
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.text.HTMLView view =
                (com.threerings.opengl.gui.text.HTMLView)comp;
            view.setAntialiased(antialias);
            view.setStyleSheet(stylesheet);
            view.setContents(contents);
        }
    }

    /**
     * An embedded 3D view.
     */
    public static class RenderableView extends ComponentConfig
    {
        /** Whether or not this is a static view. */
        @Editable(hgroup="v")
        public boolean staticView;

        /** The name of a node representing the view location. */
        @Editable(hgroup="v")
        public String viewNode = "";

        /** The vertical field of view. */
        @Editable(min=0.0, max=180.0, scale=Math.PI/180.0, hgroup="f")
        public float fov = FloatMath.PI/3f;

        /** The distance to the near clip plane. */
        @Editable(min=0.0, step=0.01, hgroup="f")
        public float near = 1f;

        /** The distance to the far clip plane. */
        @Editable(min=0.0, step=0.01, hgroup="f")
        public float far = 100f;

        /** The camera azimuth. */
        @Editable(min=-180.0, max=+180.0, scale=Math.PI/180.0, hgroup="c")
        public float azimuth;

        /** The camera elevation. */
        @Editable(min=-90.0, max=+90.0, scale=Math.PI/180.0, hgroup="c")
        public float elevation = FloatMath.PI / 4f;

        /** The camera distance. */
        @Editable(min=0.0, step=0.01, hgroup="c")
        public float distance = 10f;

        /** If we attempt to use the hints to size. */
        @Editable
        public boolean usePreferredSizeHints = false;

        /** A set of models to include in the view. */
        @Editable
        public ViewModel[] models = new ViewModel[0];

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.RenderableView.class) ?
                comp : new com.threerings.opengl.gui.RenderableView(ctx);
        }

        @Override
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.RenderableView view =
                (com.threerings.opengl.gui.RenderableView)comp;

            // set the view scope's parent
            DynamicScope vscope = view.getScope();
            vscope.setParentScope(scope);

            // set the camera parameters
            OrbitCameraHandler camhand = (OrbitCameraHandler)view.getCameraHandler();
            camhand.setPerspective(fov, near, far);
            camhand.getCoords().set(azimuth, elevation, distance);
            view.setViewNode(viewNode);

            // make static if specified and ensure that it will be validated
            view.setStatic(staticView);
            view.setUsePreferredSizeHints(usePreferredSizeHints);
            view.invalidate();

            // create/reconfigure the models as necessary
            Model[] omodels = view.getConfigModels();
            Model[] nmodels = new Model[models.length];
            view.setConfigModels(nmodels);
            for (int ii = 0; ii < nmodels.length; ii++) {
                Model model = (ii < omodels.length) ? omodels[ii] : new Model(ctx);
                nmodels[ii] = model;
                ViewModel vmodel = models[ii];
                model.setParentScope(vscope);
                model.setConfig(vmodel.model);
                model.getLocalTransform().set(vmodel.transform);
            }
            for (int ii = nmodels.length; ii < omodels.length; ii++) {
                omodels[ii].dispose();
            }
        }
    }

    /**
     * Allows the selection of a color from a colorization class.
     */
    public static class ColorPicker extends ComponentConfig
    {
        /** The name of the colorization class. */
        @Editable(hgroup="c")
        public String colorClass = "player";

        /** Whether or not to limit the options to starter colors. */
        @Editable(hgroup="c")
        public boolean starters;

        /** The index of the initially selected color. */
        @Editable(min=0)
        public int selected;

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.ColorPicker.class) ?
                comp : new com.threerings.opengl.gui.ColorPicker(ctx);
        }

        @Override
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.ColorPicker picker =
                (com.threerings.opengl.gui.ColorPicker)comp;

            // configure the component
            picker.setColorClass(colorClass, starters);
            picker.setSelectedIndex(selected);
        }
    }

    /**
     * Displays chat as an overlay.
     */
    public static class ChatOverlay extends ComponentConfig
    {
        /** The message bundle to use. */
        @Editable
        public String bundle = "chat";

        /** The area's preferred width, or zero for none. */
        @Editable(min=0, hgroup="p")
        public int preferredWidth;

        /** The color to use for system info messages. */
        @Editable(hgroup="p")
        public Color4f infoColor = new Color4f(Color4f.YELLOW);

        /** The color to use for system feedback messages. */
        @Editable(hgroup="i")
        public Color4f feedbackColor = new Color4f(Color4f.GREEN);

        /** The color to use for system attention messages. */
        @Editable(hgroup="i")
        public Color4f attentionColor = new Color4f(Color4f.RED);

        @Override
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.ChatOverlay.class) ?
                comp : new com.threerings.opengl.gui.ChatOverlay(ctx);
        }

        @Override
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.ChatOverlay overlay =
                (com.threerings.opengl.gui.ChatOverlay)comp;

            // configure the component
            overlay.setBundle(StringUtil.isBlank(bundle) ? MessageManager.GLOBAL_BUNDLE : bundle);
            overlay.setPreferredWidth(preferredWidth);
            overlay.setSystemColors(infoColor, feedbackColor, attentionColor);
        }
    }

    /**
     * Represents a model to include in a {@link RenderableView}.
     */
    public static class ViewModel extends DeepObject
        implements Exportable
    {
        /** The model reference. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        /** The transform of the model. */
        @Editable(step=0.01)
        public Transform3D transform = new Transform3D();
    }

    /** Whether or not the component is enabled. */
    @Editable(weight=1, hgroup="f")
    public boolean enabled = true;

    /** Whether or not the component is visible. */
    @Editable(weight=1, hgroup="f")
    public boolean visible = true;

    /** Whether or not the component is hoverable. */
    @Editable(weight=1, hgroup="f")
    public boolean hoverable = true;

    /** The text for the component's tooltip. */
    @Editable(weight=1, hgroup="t")
    public String tooltipText = "";

    /** Whether or not the tooltip is relative to the mouse cursor. */
    @Editable(weight=1, hgroup="t")
    public boolean tooltipRelativeToMouse;

    /** The component alpha value. */
    @Editable(min=0, max=1, step=0.01, weight=1, hgroup="a")
    public float alpha = 1f;

    /** The component's tag. */
    @Editable(weight=1, hgroup="a")
    public String tag = "";

    /** The component's style, if non-default. */
    @Editable(weight=1, nullable=true)
    public ConfigReference<StyleConfig> style;

    /** The preferred size, if non-default. */
    @Editable(weight=1, nullable=true)
    public DimensionConfig preferredSize;

    @Override
    public Object copy (Object dest, Object outer)
    {
        ComponentConfig result = (ComponentConfig)super.copy(dest, outer);
        result.editorHighlight = this.editorHighlight;
        return result;
    }

    /**
     * Creates or updates a component for this configuration.
     *
     * @param scope the component's expression scope.
     * @param comp an existing component to reuse, if possible.
     * @return either a reference to the existing component (if reused) or a new component.
     */
    public Component getComponent (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
    {
        comp = maybeRecreate(ctx, scope, msgs, comp);
        configure(ctx, scope, msgs, comp);
        if (!StringUtil.isBlank(tag)) {
            ScopeUtil.call(scope, "registerComponent", tag, comp);
        }
        ScopeUtil.call(scope, "editorHighlight", comp, editorHighlight);
        return comp;
    }

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        // nothing by default
    }

    /**
     * Recreates the component if the supplied component doesn't match the configuration.
     */
    protected abstract Component maybeRecreate (
        GlContext ctx, Scope scope, MessageBundle msgs, Component comp);

    /**
     * Configures the specified component.
     */
    protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
    {
        comp.setAlpha(alpha);
        comp.setEnabled(enabled);
        comp.setVisible(visible);
        comp.setHoverable(hoverable);
        comp.setTooltipText(
            StringUtil.isBlank(tooltipText) ? null : getMessage(msgs, tooltipText));
        comp.setTooltipRelativeToMouse(tooltipRelativeToMouse);
        comp.setStyleConfig(style);
        if (preferredSize != null) {
            comp.setPreferredSize(preferredSize.createDimension());
        }
    }

    /**
     * Returns the class of the specified object, or <code>null</code> if the reference is null.
     */
    protected static Class<?> getClass (Object object)
    {
        return (object == null) ? null : object.getClass();
    }

    /**
     * Returns the translation for the supplied text if one exists.
     */
    protected static String getMessage (MessageBundle msgs, String text)
    {
        // TODO: update all our UIs such that everything is fucking translated and if you
        // don't want it translated you prepend it with ~
        //return msgs.xlate(text);

        if (text.startsWith("~") // taint character
                || (text.startsWith(MessageUtil.QUAL_PREFIX) &&
                    text.contains(MessageUtil.QUAL_SEP))
                || text.contains("|")) { // this is the compound key separator, there's no constant
            return msgs.xlate(text);
        }
        return msgs.exists(text) ? msgs.get(text) : text;
    }
}
