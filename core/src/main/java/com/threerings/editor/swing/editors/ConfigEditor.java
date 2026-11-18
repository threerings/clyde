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

import java.util.Objects;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.google.common.base.Predicate;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ManagedConfig;
import com.threerings.config.Reference;
import com.threerings.config.ReferenceConstraints;
import com.threerings.config.swing.ConfigChooser;
import com.threerings.config.tools.BaseConfigEditor;

import com.threerings.editor.swing.PropertyEditor;
import com.threerings.editor.util.PropertyUtil;

import static com.threerings.editor.Log.log;

/**
 * Edits a bare config name: a String marked with {@link Reference} (or the old "config" editor
 * mode). Picks with the same chooser as {@link ConfigReferenceEditor}, minus the arguments.
 */
public class ConfigEditor extends PropertyEditor
  implements ActionListener
{
  @Override
  public void actionPerformed (ActionEvent event)
  {
    String ovalue = (String)_property.get(_object);
    String nvalue;
    Object source = event.getSource();
    if (source == _config) {
      ConfigChooser chooser = getChooser();
      chooser.setSelectedConfig(ovalue);
      if (!chooser.showDialog(this)) {
        return;
      }
      nvalue = chooser.getSelectedConfig();
      if (nvalue == null && !_property.nullable()) {
        return;
      }

    } else if (source == _edit) {
      BaseConfigEditor editor = BaseConfigEditor.createEditor(_ctx, _clazz, ovalue);
      editor.setLocationRelativeTo(this);
      editor.setVisible(true);
      return;

    } else if (source == _clear) {
      nvalue = null;

    } else {
      super.actionPerformed(event);
      return;
    }
    if (!Objects.equals(ovalue, nvalue)) {
      _property.set(_object, nvalue);
      update();
      fireStateChanged();
    }
  }

  @Override
  public void update ()
  {
    String value = (String)_property.get(_object);
    if (_property.isConstant()) {
      _name.setText(value == null ? _msgs.get("m.null_value") : value);
      return;
    }
    if (_config == null) {
      return; // no config type: didInit already complained
    }
    if (_edit != null) {
      _edit.setEnabled(value != null);
    }
    if (_clear != null) {
      _clear.setEnabled(value != null);
    }
    if (value == null) {
      _config.setToolTipText(null);
      _config.setText(_msgs.get("m.null_value"));
      _config.setForeground(_property.nullable() ? _normalColor : Color.red);
      return;
    }
    _config.setToolTipText(value);
    _config.setText(getShortConfigName(value));
    ManagedConfig config = _ctx.getConfigManager().getRawConfig(_clazz, value);
    boolean valid = (config != null) && PropertyUtil.getRawConfigPredicate(
        _property.getAnnotation(ReferenceConstraints.class)).apply(config);
    _config.setForeground(valid ? _normalColor : Color.red);
  }

  @Override
  protected void didInit ()
  {
    add(new JLabel(getPropertyLabel() + ":"));
    if (_property.isConstant()) {
      add(_name = new JLabel(" "));
      return;
    }
    Reference ref = _property.getAnnotation(Reference.class);
    if (ref != null) {
      _clazz = ref.value();
    } else {
      ConfigGroup<?>[] groups = _ctx.getConfigManager().getGroups(getMode()); // old way
      if (groups.length == 0) {
        log.warning("Missing groups for config editor.", "mode", getMode());
        return;
      }
      _clazz = groups[0].getConfigClass();
    }

    JPanel cpanel = new JPanel();
    cpanel.setBackground(null);
    add(cpanel);
    cpanel.add(_config = new JButton(" "));
    _normalColor = _config.getForeground();
    _config.addActionListener(this);
    if (!(ConfigReferenceEditor._noEdit || getMode().equals("compact"))) {
      cpanel.add(_edit = new JButton(_msgs.get("m.edit")));
      _edit.addActionListener(this);
    }
    if (!ConfigReferenceEditor._noEdit && _property.nullable()) {
      cpanel.add(_clear = new JButton(_msgs.get("m.clear")));
      _clear.addActionListener(this);
    }
  }

  /**
   * Returns the chooser, creating it on first use.
   */
  protected ConfigChooser getChooser ()
  {
    if (_chooser == null) {
      ConfigManager cfgmgr = _ctx.getConfigManager();
      _chooser = ConfigChooser.createInstance(_msgmgr, cfgmgr, _clazz,
          _property.getAnnotation(ReferenceConstraints.class));
      // resource configs come from a file chooser, which takes no hidden constraints
      if (!cfgmgr.isResourceClass(_clazz)) {
        Predicate<? super ManagedConfig> filter = _ctx.getChoosingFilter(_clazz);
        if (filter != null) {
          _chooser.addHiddenConstraint(filter);
        }
        // a config being edited may not refer to itself
        _chooser.addHiddenConstraint(cfg -> !(_object instanceof ManagedConfig ourCfg) ||
          !Objects.equals(ourCfg.getName(), cfg.getName()) ||
          (ourCfg.getConfigGroup() != cfg.getConfigGroup()));
      }
    }
    return _chooser;
  }

  /** The config class we pick from. */
  protected Class<? extends ManagedConfig> _clazz;

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
}
