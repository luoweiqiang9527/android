/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.uibuilder.handlers.motion.attributeEditor;

import com.android.tools.idea.uibuilder.handlers.motion.timeline.MotionSceneModel;
import com.android.tools.idea.uibuilder.handlers.motion.timeline.TimeLineIcons;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

/**
 * Used to show custom attributes
 */
public class CustomAttributePanel extends TagPanel {
  private final JButton myRemoveTagButton = EditorUtils.makeButton(TimeLineIcons.REMOVE_TAG);
  Vector<String> colNames = new Vector<String>(Arrays.asList("Name", "Value"));
  Vector<Vector<Object>> data = new Vector<>();
  DefaultTableModel myTableModel = new DefaultTableModel(data, colNames);
  JTable myTable = new JBTable(myTableModel);
  JBPopupMenu myPopupMenu = new JBPopupMenu("Add Attribute");

  public CustomAttributePanel() {
    myTitle.setText("Custom");

    int pix = JBUI.scale(5);
    setBorder(JBUI.Borders.empty(0, pix));

    myTable.setDefaultRenderer(EditorUtils.AttributesNamesHolder.class, new EditorUtils.AttributesNamesCellRenderer());
    myTable.setDefaultRenderer(String.class, new EditorUtils.AttributesValueCellRenderer());

    myTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     boolean hasFocus,
                                                     int row,
                                                     int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (!isSelected) {
          c.setForeground(column > 0 ? EditorUtils.ourValueColor : EditorUtils.ourNameColor);
        }

        return c;
      }
    });

    EditorUtils.AddRemovePanel addRemovePanel = new EditorUtils.AddRemovePanel();

    myPopupMenu.add(new JMenuItem("test1"));

    addRemovePanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        myPopupMenu.show(e.getComponent(), e.getX(), e.getY());
      }
    });

    GridBagConstraints gbc = new GridBagConstraints();

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.BOTH;
    add(myTitle, gbc);

    gbc.gridx = 1;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.EAST;
    add(myRemoveTagButton, gbc);

    gbc.gridwidth = 2;
    gbc.gridx = 0;
    gbc.weightx = 1;
    gbc.gridy++;
    gbc.fill = GridBagConstraints.BOTH;
    add(myTable, gbc);

    gbc.gridy++;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.WEST;
    add(addRemovePanel, gbc);
  }

  public ActionListener myAddItemAction = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      String s = ((JMenuItem)e.getSource()).getText();
      data.add(new Vector<Object>(Arrays.asList(s, "")));
      myTableModel.fireTableRowsInserted(data.size() - 1, data.size());
    }
  };

  private void setupPopup(MotionSceneModel.CustomAttributes tag) {
    myPopupMenu.removeAll();
    String[] names = tag.getPossibleAttr();
    Set<String> current = tag.getAttributes().keySet();
    for (int i = 0; i < names.length; i++) {
      if (current.contains(names[i])) {
        continue;
      }
      JMenuItem menuItem = new JMenuItem(names[i]);
      menuItem.addActionListener(myAddItemAction);
      myPopupMenu.add(menuItem);
    }
  }

  public void setTag(MotionSceneModel.CustomAttributes tag) {
    HashMap<String, Object> attr = tag.getAttributes();
    data.clear();
    for (String s : attr.keySet()) {
      Vector<Object> v = new Vector<Object>(Arrays.asList(s, attr.get(s)));
      data.add(v);
    }
    myTableModel.fireTableDataChanged();
    setupPopup(tag);
  }
}
