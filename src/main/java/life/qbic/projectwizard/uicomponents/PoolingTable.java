/*******************************************************************************
 * QBiC Project Wizard enables users to create hierarchical experiments including different study conditions using factorial design.
 * Copyright (C) "2016"  Andreas Friedrich
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package life.qbic.projectwizard.uicomponents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.event.DataBoundTransferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.event.dd.acceptcriteria.And;
import com.vaadin.event.dd.acceptcriteria.ClientSideCriterion;
import com.vaadin.event.dd.acceptcriteria.Or;
import com.vaadin.event.dd.acceptcriteria.SourceIs;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.ui.AbstractSelect.AcceptItem;
import com.vaadin.ui.Button.ClickEvent;

import life.qbic.portal.Styles;
import life.qbic.portal.components.StandardTextField;

import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.Button;
import com.vaadin.ui.VerticalLayout;

public class PoolingTable extends VerticalLayout {

  /**
   * 
   */
  private static final long serialVersionUID = 2093120065683163087L;
  private StandardTextField secondaryName;
  private Button moveLeft;

  private Table poolTable;
  private HashSet<Integer> poolIDs;
  private Map<Integer, Integer> usedTimes;
  private Table all;
  private Table used;
  private List<String> labels;

  private TabSheet selectionTables;

  public PoolingTable(String name, TabSheet selectionTables, Map<Integer, Integer> usedTimes,
      List<String> factorLabels) {
    this.selectionTables = selectionTables;
    all = (Table) selectionTables.getTab(0).getComponent();
    used = (Table) selectionTables.getTab(1).getComponent();
    labels = factorLabels;

    this.usedTimes = usedTimes;
    setSpacing(true);

    HorizontalLayout tableButtonComponent = new HorizontalLayout();
    tableButtonComponent.setCaption("Sample Pool");
    tableButtonComponent.setSpacing(true);
    secondaryName = new StandardTextField("Secondary Name");
    secondaryName.setValue(name);
    secondaryName.setStyleName(Styles.fieldTheme);
    moveLeft = new Button();
    Styles.iconButton(moveLeft, FontAwesome.ARROW_CIRCLE_LEFT);
    
    moveLeft.addStyleName("large_font_awesome");
    addComponent(secondaryName);
    poolIDs = new HashSet<Integer>();
    poolTable = new Table();
    initTable();
    tableButtonComponent.addComponent(poolTable);
    tableButtonComponent.addComponent(moveLeft);
    addComponent(Styles
        .questionize(
            tableButtonComponent,
            "You can add samples to the active pool by "
                + "selecting them from the right and clicking "+FontAwesome.ARROW_CIRCLE_LEFT.getHtml()+" or by dragging them over with your mouse.",
            "Adding Samples to Pools"));

    initDragAndDrop(new Or(new SourceIs(all), new SourceIs(used)));
    initButtonMover(all, used);
  }

  private Table getActiveTable() {
    return (Table) selectionTables.getSelectedTab();
  }

  private boolean moveSampleToPool(Table source, Object itemId) {
    boolean added = false;
    final Integer id = (Integer) source.getItem(itemId).getItemProperty("ID").getValue();
    String name = (String) source.getItem(itemId).getItemProperty("Secondary Name").getValue();
    String labID = (String) source.getItem(itemId).getItemProperty("Lab ID").getValue();
    final List<Object> row = new ArrayList<Object>();
    row.add(id);
    row.add(name);
    row.add(labID);
    for (String label : labels) {
      String value = (String) source.getItem(itemId).getItemProperty(label).getValue();
      row.add(value);
    }
    if (!poolIDs.contains(id)) {
      added = true;
      poolIDs.add(id);
      Button delete = new Button();
      Styles.iconButton(delete, FontAwesome.UNDO);
      delete.setData(itemId);
      delete.addClickListener(new Button.ClickListener() {
        /**
       * 
       */
        private static final long serialVersionUID = 5414603256990177472L;

        @Override
        public void buttonClick(ClickEvent event) {
          Integer iid = (Integer) event.getButton().getData();
          poolIDs.remove(id);
          poolTable.removeItem(iid);
          resizeTable();
          int newNumUsed = usedTimes.get(id) - 1;
          if (newNumUsed == 0) {
            usedTimes.remove(id);
            used.removeItem(iid);
            all.addItem(row.toArray(), iid);
            all.sort(new Object[] {"ID"}, new boolean[] {true});
          } else
            usedTimes.put(id, newNumUsed);
        }
      });
      poolTable.addItem(new Object[] {id, name, labID, delete}, itemId);
      if (usedTimes.containsKey(id)) {
        int newNumUsed = usedTimes.get(id) + 1;
        usedTimes.put(id, newNumUsed);
      } else {
        usedTimes.put(id, 1);
        all.removeItem(itemId);
        used.addItem(row.toArray(), itemId);
        used.sort(new Object[] {"ID"}, new boolean[] {true});
      }
    }
    return added;
  }

  private void initButtonMover(Table sourceTable, Table usedTable) {
    moveLeft.addClickListener(new Button.ClickListener() {

      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      @Override
      public void buttonClick(ClickEvent event) {
        Table source = getActiveTable();
        Set<Object> ids = (Set<Object>) source.getValue();
        boolean atLeastOneNew = false;
        if (ids.size() > 0) {
          for (Object itemId : ids) {
            atLeastOneNew |= moveSampleToPool(source, itemId);
          }
          resizeTable();
          if (!atLeastOneNew) {
            Notification n = new Notification("Samples are already in this pool.");
            n.setDelayMsec(3000);
            n.show(Page.getCurrent());
          }
        }
      }
    });
  }

  private void initTable() {
    poolTable.setStyleName(Styles.tableTheme);
    resizeTable();
    poolTable.addContainerProperty("ID", Integer.class, null);
    poolTable.addContainerProperty("Secondary Name", String.class, null);
    poolTable.addContainerProperty("Lab ID", String.class, null);
    poolTable.addContainerProperty("Undo", Button.class, null);
  }

  private void initDragAndDrop(final ClientSideCriterion acceptCriterion) {
    poolTable.setDropHandler(new DropHandler() {
      /**
       * 
       */
      private static final long serialVersionUID = 4057757595986300434L;

      @Override
      public void drop(final DragAndDropEvent dropEvent) {
        // criteria verify that this is safe
        final DataBoundTransferable t = (DataBoundTransferable) dropEvent.getTransferable();
        if (!(t.getSourceComponent() instanceof Table)) {
          return;
        }

        Table source = (Table) t.getSourceComponent();
        Object sourceItemId = t.getItemId();
        if (!moveSampleToPool(source, sourceItemId)) {
          Notification n = new Notification("Sample is already in this pool.");
          n.setDelayMsec(3000);
          n.show(Page.getCurrent());
        }
        resizeTable();
      }

      @Override
      public AcceptCriterion getAcceptCriterion() {
        return new And(acceptCriterion, AcceptItem.ALL);
      }
    });
  }

  private void resizeTable() {
    poolTable.setPageLength(poolTable.size() + 1);
  }

  public Table getTable() {
    return poolTable;
  }

  public List<Integer> getSampleIDs() {
    List<Integer> res = new ArrayList<Integer>();
    res.addAll(poolIDs);
    return res;
  }

  public String getSecondaryName() {
    return secondaryName.getValue();
  }
}
