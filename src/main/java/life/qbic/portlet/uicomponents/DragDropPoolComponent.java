/*******************************************************************************
 * QBiC Project Wizard enables users to create hierarchical experiments including different study
 * conditions using factorial design. Copyright (C) "2016" Andreas Friedrich
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package life.qbic.portlet.uicomponents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import properties.Property;

import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.TableDragMode;
import com.vaadin.ui.themes.ValoTheme;

import life.qbic.portlet.model.AOpenbisSample;
import life.qbic.portlet.uicomponents.Styles;

import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;

import logging.Log4j2Logger;

public class DragDropPoolComponent extends HorizontalSplitPanel {

  /**
   * 
   */
  private static final long serialVersionUID = -7931696318862537094L;
  private VerticalLayout left;
  private TabSheet tableLayout;
  private List<PoolingTable> tables;
  private Button add;
  private Button remove;
  private List<String> factorLabels;
  logging.Logger logger = new Log4j2Logger(DragDropPoolComponent.class);

  private TabSheet samples;
  private Table selectionTable;
  private Table usedTable;

  private Map<Integer, Integer> usedTimes;
  private Map<Integer, AOpenbisSample> map;

  private String name;

  public DragDropPoolComponent(String poolingPrefix) {
    this.name = poolingPrefix;
    usedTimes = new HashMap<Integer, Integer>();
    left = new VerticalLayout();
    left.setSpacing(true);
    left.setWidth("500px");
    tableLayout = new TabSheet();
    tableLayout.setWidth("100%");
    tableLayout.setStyleName(ValoTheme.TABSHEET_FRAMED);
    tables = new ArrayList<PoolingTable>();
    add = new Button("Add Pool");
    remove = new Button("Remove Last Pool");
    initButtons();

    addComponent(left);
    left.addComponent(Styles.questionize(tableLayout,
        "Here you can add pools and fill them with the samples "
            + "from earlier steps (found on the right). One sample can be in multiple pools.",
        "Target Pools"));
    left.addComponent(add);
    left.addComponent(remove);

    samples = new TabSheet();
    initSelectionTables();
    addComponent(Styles.questionize(samples,
        "These are the samples you prepared in the earlier steps. For convenience they are separated in unused samples "
            + "and samples that are already in at least one pool.",
        "Target Pools"));
  }

  private void initButtons() {
    Button.ClickListener l = new Button.ClickListener() {

      /**
       * 
       */
      private static final long serialVersionUID = 5082957673029868123L;

      @Override
      public void buttonClick(ClickEvent event) {
        String src = event.getButton().getCaption();
        if (src.equals("Add Pool"))
          add();
        else
          remove();
      }
    };
    add.addClickListener(l);
    remove.addClickListener(l);
  }

  private void initSelectionTables() {
    selectionTable = new Table();
    selectionTable.setDragMode(TableDragMode.ROW);
    selectionTable.setStyleName(Styles.tableTheme);
    selectionTable.addContainerProperty("ID", Integer.class, null);
    selectionTable.setColumnWidth("ID", 30);
    selectionTable.addContainerProperty("Secondary Name", String.class, null);
    selectionTable.addContainerProperty("Lab ID", String.class, null);
    selectionTable.setPageLength(20);
    selectionTable.setSelectable(true);
    selectionTable.setMultiSelect(true);

    usedTable = new Table();
    usedTable.setDragMode(TableDragMode.ROW);
    usedTable.setStyleName(Styles.tableTheme);
    usedTable.addContainerProperty("ID", Integer.class, null);
    usedTable.setColumnWidth("ID", 30);
    usedTable.addContainerProperty("Secondary Name", String.class, null);
    usedTable.addContainerProperty("Lab ID", String.class, null);
    usedTable.setPageLength(20);
    usedTable.setSelectable(true);
    usedTable.setMultiSelect(true);

    samples.addTab(selectionTable, "Unpooled Samples");
    samples.addTab(usedTable, "Pooled Samples");
  }

  public void initConditionsAndSetSamples(List<AOpenbisSample> samples) {
    map = new HashMap<Integer, AOpenbisSample>();
    factorLabels = new ArrayList<String>();
    List<Property> factors = samples.get(0).getFactors();
    for (int i = 0; i < factors.size(); i++) {
      String l = factors.get(i).getLabel();

      int j = 2;
      while (factorLabels.contains(l)) {
        l = factors.get(i).getLabel() + " (" + Integer.toString(j) + ")";
        j++;
      }
      factorLabels.add(l);
      selectionTable.addContainerProperty(l, String.class, null);
      usedTable.addContainerProperty(l, String.class, null);
    }

    for (int i = 0; i < samples.size(); i++) {
      AOpenbisSample s = samples.get(i);
      int id = i + 1;
      map.put(id, s);

      // The Table item identifier for the row.
      Integer itemId = i;

      // Create the table row.
      List<Object> row = new ArrayList<Object>();
      row.add(id);
      row.add(s.getQ_SECONDARY_NAME());
      row.add(s.getQ_EXTERNALDB_ID());
      for (Property f : s.getFactors()) {
        String v = f.getValue();
        if (f.hasUnit())
          v += " " + f.getUnit();
        row.add(v);
      }
      selectionTable.addItem(row.toArray(new Object[row.size()]), itemId);
    }
    add();
  }

  private void add() {
    String name = "Pool " + (tables.size() + 1);
    PoolingTable p = new PoolingTable(name, samples, usedTimes, factorLabels);
    tables.add(p);
    tableLayout.addTab(p, name);
  }

  private void remove() {
    int size = tables.size();
    if (size > 1) {
      PoolingTable last = tables.get(size - 1);
      tableLayout.removeComponent(last);
      tables.remove(last);
    }
  }

  //
  public Map<String, List<AOpenbisSample>> getPools() {
    Map<String, List<AOpenbisSample>> res = new HashMap<String, List<AOpenbisSample>>();
    for (PoolingTable pt : tables) {
      List<Integer> parentIds = pt.getSampleIDs();
      if (!parentIds.isEmpty()) {
        List<AOpenbisSample> parents = new ArrayList<AOpenbisSample>();
        for (Integer id : parentIds) {
          parents.add(map.get(id));
        }
        res.put(name + pt.getSecondaryName(), parents);
      }
    }
    return res;
  }
}
