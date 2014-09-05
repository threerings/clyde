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

package com.threerings.editor.swing.editors;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.threerings.config.ParameterizedConfig;

import com.threerings.editor.PathProperty;

/**
 * An editor for an array of paramater paths.
 */
public class PathTableArrayListEditor extends TableArrayListEditor
{
    @Override
    protected void didInit ()
    {
        super.didInit();
        _table.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent (JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column)
            {
                super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
                Color color = isSelected ? table.getSelectionForeground() : table.getForeground();
                if (value instanceof String) {
                    String p = (String)value;
                    Object root = getRootObject();
                    if (((root instanceof ParameterizedConfig) &&
                            ((ParameterizedConfig)root).isInvalidParameterPath(p))
                            ||
                            (PathProperty.createPath(_ctx.getConfigManager(), root, p) == null)) {
                        color = isSelected ? Color.red.brighter() : Color.red;
                    }
                }
                setForeground(color);
                return this;
            }
        });
    }
}
