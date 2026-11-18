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

package com.threerings.editor.swing.editors;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.util.SwingUtil;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.config.IdConfig;
import com.threerings.config.ManagedConfig;
import com.threerings.config.NoDependency;
import com.threerings.config.Parameter;
import com.threerings.config.ParameterizedConfig;
import com.threerings.config.Reference;
import com.threerings.config.ReferenceConstraints;
import com.threerings.config.swing.ConfigChooser;
import com.threerings.config.tools.BaseConfigEditor;
import com.threerings.config.util.ReferenceUtil;

import com.threerings.editor.Property;
import com.threerings.editor.swing.PropertyEditor;
import com.threerings.editor.util.PropertyUtil;

import static com.threerings.editor.Log.log;

/**
 * Edits a reference to a config: a button naming the config, which opens a chooser, plus edit
 * and clear buttons. The subclasses differ in how the reference is stored.
 */
public abstract class ConfigReferenceEditor extends PropertyEditor
  implements ActionListener
{
  /**
   * Edits a {@link ConfigReference}, including its arguments.
   */
  public static class ByType extends ConfigReferenceEditor
    implements ChangeListener, ConfigUpdateListener<ManagedConfig>
  {
    @Override
    public void setNoTransfer (boolean noTransfer)
    {
      _noTransfer = noTransfer;
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
      fireStateChanged();
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<ManagedConfig> event)
    {
      update();
    }

    @Override
    public void addNotify ()
    {
      super.addNotify();
      if (_listenee != null) {
        update();
      }
    }

    @Override
    public void removeNotify ()
    {
      super.removeNotify();
      if (_listenee != null) {
        _listenee.removeListener(this);
      }
    }

    @Override
    public String getComponentPath (Component comp, boolean mouse)
    {
      PropertyEditor editor = getNextChildComponent(PropertyEditor.class, comp);
      return editor == null ? "" :
        ("[\"" + editor.getProperty().getName().replace("\"", "\\\"") + "\"]" +
         editor.getComponentPath(comp, mouse));
    }

    @Override
    protected Class<? extends ManagedConfig> getConfigClass ()
    {
      @SuppressWarnings("unchecked")
      Class<? extends ManagedConfig> clazz =
        (Class<? extends ManagedConfig>)_property.getArgumentType(ConfigReference.class);
      return clazz;
    }

    @Override
    protected String getConfigName (Object value)
    {
      return (value == null) ? null : ((ConfigReference<?>)value).getName();
    }

    @Override
    protected Object toValue (String name)
    {
      return new ConfigReference<ManagedConfig>(name);
    }

    @Override
    protected void layoutButtons (JPanel cpanel)
    {
      makeCollapsible(_ctx, getPropertyLabel(), false);
      _content.add(cpanel);
      cpanel.add(new JLabel(_msgs.get("m.config") + ":"));
    }

    @Override
    protected void didInit ()
    {
      super.didInit();
      if (_config != null) {
        _content.add(_arguments = GroupLayout.makeVBox(
          GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        _arguments.setBackground(null);
      }
    }

    @Override
    protected void updateDetails (
      Object value, ManagedConfig config, boolean valid, boolean transfer)
    {
      // make sure we're not listening to anything
      if (_listenee != null) {
        _listenee.removeListener(this);
        _listenee = null;
      }
      ConfigReference<?> ref = (ConfigReference<?>)value;
      if (ref == null) {
        _arguments.removeAll();
        return;
      }
      if (!(config instanceof ParameterizedConfig pconfig) ||
          getProperty().isAnnotationPresent(NoDependency.class)) {
        _arguments.removeAll();
        ref.getArguments().clear();
        return;
      }
      if (!valid) {
        return;
      }

      // store the existing editors mapped by name in case we want to reuse their values
      int ocount = _arguments.getComponentCount();
      HashMap<String, PropertyEditor> oeditors = new HashMap<String, PropertyEditor>();
      if (transfer && !_noTransfer) {
        for (int ii = 0; ii < ocount; ii++) {
          PropertyEditor editor = (PropertyEditor)_arguments.getComponent(ii);
          oeditors.put(editor.getProperty().getName(), editor);
        }
      }

      // scan through the parameters
      int idx = 0;
      Object nargs = ref.getArguments();
      for (Parameter parameter : pconfig.parameters) {
        Property property = parameter.getArgumentProperty(pconfig);
        if (property == null) {
          continue;
        }
        PropertyEditor editor = null;
        if (idx < ocount) {
          // see if we can copy the argument to the new set and reuse the editor
          PropertyEditor oeditor = (PropertyEditor)_arguments.getComponent(idx);
          if (oeditor.getProperty().equals(property)) {
            if (transfer) {
              property.set(nargs, property.get(oeditor.getObject()));
            }
            editor = oeditor;
          } else {
            _arguments.remove(idx);
          }
        }
        if (editor == null) {
          editor = PropertyEditor.createEditor(_ctx, property, _lineage);
          editor.addChangeListener(this);
          _arguments.add(editor, idx);

          // see if we can reuse the value from the previous editor
          PropertyEditor oeditor = oeditors.get(parameter.name);
          if (oeditor != null) {
            Property oproperty = oeditor.getProperty();
            if (property.getGenericType().equals(oproperty.getGenericType())) {
              Object ovalue = oproperty.get(oeditor.getObject());
              if (property.isLegalValue(ovalue)) {
                property.set(nargs, ovalue);
              }
            }
          }
        }
        editor.setObject(nargs);
        idx++;
      }

      // remove the remaining editors
      while (ocount > idx) {
        _arguments.remove(--ocount);
      }
      // we may need to refresh the panel
      SwingUtil.refresh(_arguments);

      // clear out any arguments that don't correspond to parameters
      for (Iterator<String> it = ref.getArguments().keySet().iterator(); it.hasNext(); ) {
        if (pconfig.getParameter(it.next()) == null) {
          it.remove();
        }
      }

      // listen for parameter changes
      (_listenee = pconfig).addListener(this);
    }

    /** Holds the argument panels. */
    protected JPanel _arguments;

    /** The config that we're listening to, if any. */
    protected ParameterizedConfig _listenee;

    /** Should we suppress transferring arguments? */
    protected boolean _noTransfer;
  }

  /**
   * Edits a bare reference marked with {@link Reference}: a config name (String) or the id (int)
   * of an {@link IdConfig}. Also serves Strings using the old "config" editor mode.
   */
  public static class ByAnnotation extends ConfigReferenceEditor
  {
    @Override
    protected Class<? extends ManagedConfig> getConfigClass ()
    {
      Reference ref = _property.getAnnotation(Reference.class);
      if (ref != null) {
        return ref.value();
      }
      ConfigGroup<?>[] groups = _ctx.getConfigManager().getGroups(getMode());
      if (groups.length == 0) {
        log.warning("Missing groups for config editor.", "mode", getMode());
        return null;
      }
      return groups[0].getConfigClass();
    }

    @Override
    protected String getConfigName (Object value)
    {
      if (!(value instanceof Integer id)) {
        return Strings.emptyToNull((String)value);
      }
      if (id == 0) {
        return null;
      }
      Class<? extends ManagedConfig> clazz = getConfigClass();
      ManagedConfig config = (clazz == null)
        ? null
        : _ctx.getConfigManager().getRawConfig(clazz, id);
      return (config == null) ? "#" + id : config.getName(); // a missing id shows in red
    }

    @Override
    protected Object toValue (String name)
    {
      return holdsIds() ? ReferenceUtil.getId(_ctx.getConfigManager(), getConfigClass(), name)
        : name;
    }

    @Override
    protected Object getNullValue ()
    {
      return holdsIds() ? 0 : null;
    }

    @Override
    protected Predicate<? super ManagedConfig> getChooserFilter ()
    {
      // a config can only be referenced by id once it has one
      return holdsIds() ? cfg -> (cfg instanceof IdConfig idcfg) && idcfg.hasConfigId() : null;
    }

    @Override
    protected void layoutButtons (JPanel cpanel)
    {
      String label = getPropertyLabel();
      if (!label.isEmpty()) { // array and list elements have no name
        add(new JLabel(label + ":"));
      }
      add(cpanel);
    }

    @Override
    protected void didInit ()
    {
      super.didInit();
      Class<? extends ManagedConfig> clazz = getConfigClass();
      if (holdsIds() && clazz != null && !IdConfig.class.isAssignableFrom(clazz)) {
        log.warning("@Reference by id needs an IdConfig.",
          "property", _property.getName(), "class", clazz);
      }
    }

    /**
     * Whether we store config ids rather than names.
     */
    protected boolean holdsIds ()
    {
      return ReferenceUtil.holdsIds(_property.getType());
    }
  }

  /**
   * Sets this editor not to carry argument values over when a different config is chosen.
   * Only references with arguments have any to carry.
   */
  public void setNoTransfer (boolean noTransfer)
  {
    // nothing by default
  }

  // documentation inherited from interface ActionListener
  @Override
  public void actionPerformed (ActionEvent event)
  {
    Object source = event.getSource();
    String oname = getConfigName(_property.get(_object));
    Object nvalue;
    if (source == _config) {
      ConfigChooser chooser = getChooser();
      if (chooser == null) {
        return; // no config class: getConfigClass() complained
      }
      chooser.setSelectedConfig(oname);
      if (!chooser.showDialog(this)) {
        return;
      }
      String nname = chooser.getSelectedConfig();
      if (Objects.equals(oname, nname) || (nname == null && !_property.nullable())) {
        return;
      }
      if (nname != null && !validateNewConfig(nname)) {
        return;
      }
      nvalue = (nname == null) ? getNullValue() : toValue(nname);

    } else if (source == _edit) {
      BaseConfigEditor editor = BaseConfigEditor.createEditor(_ctx, getConfigClass(), oname);
      editor.setLocationRelativeTo(this);
      editor.setVisible(true);
      return;

    } else if (source == _clear) {
      nvalue = getNullValue();

    } else {
      super.actionPerformed(event);
      return;
    }
    _property.set(_object, nvalue);
    update(nvalue, true);
    fireStateChanged();
  }

  @Override
  public void update ()
  {
    update(_property.get(_object), false);
  }

  @Override
  public String getEditedFilePath ()
  {
    Class<? extends ManagedConfig> clazz = getConfigClass();
    String name = getConfigName(_property.get(_object));
    if (clazz != null && name != null && _ctx.getConfigManager().isResourceClass(clazz)) {
      File fullfile = _ctx.getResourceManager().getResourceFile(name);
      if (fullfile != null) {
        return fullfile.getAbsolutePath();
      }
    }
    return null;
  }

  /**
   * Returns the class of config we reference, or null if it can't be determined.
   */
  protected abstract Class<? extends ManagedConfig> getConfigClass ();

  /**
   * Returns the name of the config that a stored value references, or null for none.
   */
  protected abstract String getConfigName (Object value);

  /**
   * Returns the value to store for a reference to the named config.
   */
  protected abstract Object toValue (String name);

  /**
   * Returns the value to store for no reference.
   */
  protected Object getNullValue ()
  {
    return null;
  }

  /**
   * Returns an extra filter on what the chooser offers, or null for none.
   */
  protected Predicate<? super ManagedConfig> getChooserFilter ()
  {
    return null;
  }

  /**
   * Adds the panel holding the config buttons, plus anything that precedes them.
   */
  protected abstract void layoutButtons (JPanel cpanel);

  /**
   * Updates anything beyond the config buttons for the current value.
   *
   * @param config the referenced config, or null if there's none or it's missing.
   * @param valid whether the config exists and meets the reference constraints.
   * @param transfer whether to carry over values from a previously chosen config.
   */
  protected void updateDetails (
    Object value, ManagedConfig config, boolean valid, boolean transfer)
  {
    // nothing by default
  }

  /**
   * Validates a newly chosen config, possibly informing the user if it's no good.
   */
  protected boolean validateNewConfig (String name)
  {
    return true;
  }

  @Override
  protected void didInit ()
  {
    if (_property.isConstant()) {
      add(new JLabel(getPropertyLabel() + ":"));
      add(_name = new JLabel(" "));
      return;
    }
    JPanel cpanel = new JPanel();
    cpanel.setBackground(null);
    layoutButtons(cpanel);
    cpanel.add(_config = new JButton(" "));
    _normalColor = _config.getForeground();
    _config.addActionListener(this);
    if (!(_noEdit || getMode().equals("compact"))) {
      cpanel.add(_edit = new JButton(_msgs.get("m.edit")));
      _edit.addActionListener(this);
    }
    if (!_noEdit && _property.nullable()) {
      cpanel.add(_clear = new JButton(_msgs.get("m.clear")));
      _clear.addActionListener(this);
    }
  }

  /**
   * Updates the interface for the specified value.
   *
   * @param transfer whether to carry over values from a previously chosen config.
   */
  protected void update (Object value, boolean transfer)
  {
    String name = getConfigName(value);
    if (_property.isConstant()) {
      _name.setText(name == null ? _msgs.get("m.null_value") : name);
      return;
    }
    if (_clear != null) {
      _clear.setEnabled(name != null);
    }
    if (name == null) {
      if (_edit != null) {
        _edit.setEnabled(false);
      }
      _config.setToolTipText(null);
      _config.setText(_msgs.get("m.null_value"));
      _config.setForeground(_property.nullable() ? _normalColor : Color.red);
      updateDetails(null, null, false, transfer);
      return;
    }
    _config.setToolTipText(name);
    _config.setText(getShortConfigName(name));
    Class<? extends ManagedConfig> clazz = getConfigClass();
    ManagedConfig config = (clazz == null)
      ? null
      : _ctx.getConfigManager().getRawConfig(clazz, name);
    boolean valid = (config != null) && PropertyUtil.getRawConfigPredicate(
        _property.getAnnotation(ReferenceConstraints.class)).apply(config);
    _config.setForeground(valid ? _normalColor : Color.red);
    if (_edit != null) {
      _edit.setEnabled(config != null);
    }
    updateDetails(value, config, valid, transfer);
  }

  /**
   * Returns the chooser, creating it on first use, or null if we have no config class.
   */
  protected ConfigChooser getChooser ()
  {
    if (_chooser == null) {
      ConfigManager cfgmgr = _ctx.getConfigManager();
      Class<? extends ManagedConfig> clazz = getConfigClass();
      if (clazz == null) {
        return null;
      }
      _chooser = ConfigChooser.createInstance(_msgmgr, cfgmgr, clazz,
          _property.getAnnotation(ReferenceConstraints.class));
      // resource configs come from a file chooser, which takes no hidden constraints
      if (!cfgmgr.isResourceClass(clazz)) {
        Predicate<? super ManagedConfig> filter = _ctx.getChoosingFilter(clazz);
        if (filter != null) {
          _chooser.addHiddenConstraint(filter);
        }
        Predicate<? super ManagedConfig> ourFilter = getChooserFilter();
        if (ourFilter != null) {
          _chooser.addHiddenConstraint(ourFilter);
        }
        // a config being edited may not refer to itself
        _chooser.addHiddenConstraint(cfg -> !(_object instanceof ManagedConfig ourCfg) ||
          !Objects.equals(ourCfg.getName(), cfg.getName()) ||
          (ourCfg.getConfigGroup() != cfg.getConfigGroup()));
      }
    }
    return _chooser;
  }

  /** The config button. */
  protected JButton _config;

  /** The config button's color when its config is valid. */
  protected Color _normalColor;

  /** The edit and clear buttons. */
  protected JButton _edit, _clear;

  /** The label for a constant config. */
  protected JLabel _name;

  /** The config chooser, created when first needed. */
  protected ConfigChooser _chooser;

  /** Should we omit the edit button? */
  protected static final boolean _noEdit = Boolean.getBoolean("com.threerings.tool.no_edit");
}
