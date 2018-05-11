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
package life.qbic.projectwizard.uicomponents;


import java.util.ArrayList;
import java.util.List;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import life.qbic.datamodel.samples.AOpenbisSample;
import life.qbic.portal.Styles;
import life.qbic.portal.components.StandardTextField;
import life.qbic.xml.properties.Property;
import life.qbic.xml.properties.PropertyType;

/**
 * Table to summarize prepared samples, remove them or adapt their secondary names
 * 
 * @author Andreas Friedrich
 * 
 */
public class SummaryTable extends VerticalLayout {

  /**
   * 
   */
  private static final long serialVersionUID = 3220178619365365177L;
  private Table table;
  // private Map<String, AOpenbisSample> map;
  private String name;
  private boolean isotopes = false;
  private LabelingMethod labelingMethod;
  private HorizontalLayout deleteNames;
  private List<String> addedCols;

  public SummaryTable(String name) {
    setSpacing(true);
    this.name = name;
    table = new Table(name);
    Button clearSecondary = new Button("Remove Secondary Names");
    clearSecondary.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        removeColEntries("Secondary Name");
      }
    });
    Button clearExternal = new Button("Remove External IDs");
    clearExternal.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        removeColEntries("External DB ID");
      }
    });
    deleteNames = new HorizontalLayout();
    deleteNames.setSpacing(true);
    deleteNames.addComponent(clearSecondary);
    deleteNames.addComponent(clearExternal);
  }

  public List<AOpenbisSample> getSamples() {
    List<AOpenbisSample> res = new ArrayList<AOpenbisSample>();
    for (Object id : table.getItemIds()) {
      // String key = (String) ((TextField) table.getItem(id).getItemProperty("Secondary
      // Name").getValue()).getData();
      AOpenbisSample s = (AOpenbisSample) id;
      String secName = parseTextField("Secondary Name", id);
      s.setQ_SECONDARY_NAME(secName);
      if (secName == null)
        secName = "";

      String extID = parseTextField("External DB ID", id);
      s.setQ_EXTERNALDB_ID(extID);
      if (extID == null)
        extID = "";

      if (!secName.equals("DELETED")) {
        if (isotopes) {
          String method = labelingMethod.getName();
          String value = parseComboLabel(method, id);
          if (value != null)
            s.addFactor(new Property(method.toLowerCase(), value, PropertyType.Factor));
        }
        res.add(s);
      }
    }
    return res;
  }

  private String parseComboLabel(String colname, Object id) {
    return (String) ((ComboBox) table.getItem(id).getItemProperty(colname).getValue()).getValue();
  }

  private String parseTextField(String colname, Object id) {
    return ((TextField) table.getItem(id).getItemProperty(colname).getValue()).getValue();
  }

  public void setPageLength(int size) {
    table.setPageLength(size);
  }

  public void removeAllItems() {
    removeAllComponents();
    // map = new HashMap<String, AOpenbisSample>();
    table = new Table(name);
    addComponent(table);
    addComponent(Styles.questionize(deleteNames,
        "If you don't want to keep any of the proposed secondary names you can use this button to delete all of them.",
        "Clear Secondary Names"));
  }

  private void removeColEntries(String colName) {
    for (Object id : table.getItemIds()) {
      TextField tf = (TextField) table.getItem(id).getItemProperty(colName).getValue();
      if (!tf.getValue().equals("DELETED"))
        tf.setValue("");
    }
  }

  public void initTable(List<AOpenbisSample> samples, LabelingMethod labelingMethod) {
    if (labelingMethod != null) {
      this.labelingMethod = labelingMethod;
      isotopes = true;
    }
    table.setStyleName(Styles.tableTheme);
    // table.addContainerProperty("ID", String.class, null);
    // table.setColumnWidth("ID", 35);
    table.addContainerProperty("Secondary Name", TextField.class, null);
    table.addContainerProperty("External DB ID", TextField.class, null);
    table.setColumnWidth("External DB ID", 106);
    table.setImmediate(true);
    table.setCaption(samples.size() + " " + name);

    if (isotopes)
      table.addContainerProperty(labelingMethod.getName(), ComboBox.class, null);

    List<String> factorLabels = new ArrayList<String>();
    int maxCols = 0;
    AOpenbisSample mostInformative = samples.get(0);
    for (AOpenbisSample s : samples) {
      int size = s.getFactors().size();
      if (size > maxCols) {
        maxCols = size;
        mostInformative = s;
      }
    }
    List<Property> factors = mostInformative.getFactors();
    for (int i = 0; i < factors.size(); i++) {
      String l = factors.get(i).getLabel();

      int j = 2;
      while (factorLabels.contains(l)) {
        l = factors.get(i).getLabel() + " (" + Integer.toString(j) + ")";
        j++;
      }
      factorLabels.add(l);
      table.addContainerProperty(l, String.class, null);
    }

    table.addContainerProperty("Customize", Button.class, null);
    table.setColumnWidth("Customize", 85);

    List<String> reagents = null;
    if (isotopes)
      reagents = labelingMethod.getReagents();
    int i = -1;
    for (AOpenbisSample s : samples) {
      i++;
      // AOpenbisSample s = samples.get(i);
      // Obje id = Integer.toString(i);
      // map.put(id, s);

      // The Table item identifier for the row.
      // Integer itemId = new Integer(i);

      // Create a button and handle its click.
      Button delete = new Button();
      Styles.iconButton(delete, FontAwesome.TRASH_O);
      // delete.setWidth("15px");
      // delete.setHeight("30px");
      delete.setData(s);
      delete.addClickListener(new Button.ClickListener() {
        /**
         * 
         */
        private static final long serialVersionUID = 5414603256990177472L;

        @Override
        public void buttonClick(ClickEvent event) {
          Button b = event.getButton();
          Object iid = b.getData();
          TextField secNameField =
              (TextField) table.getItem(iid).getItemProperty("Secondary Name").getValue();
          TextField extIDField =
              (TextField) table.getItem(iid).getItemProperty("External DB ID").getValue();
          if (secNameField.getValue().equals("DELETED")) {
            secNameField.setReadOnly(false);
            extIDField.setReadOnly(false);

            // String id = (String) table.getItem(iid).getItemProperty("ID").getValue();
            secNameField.setValue(s.getQ_SECONDARY_NAME());
            extIDField.setValue(s.getQ_EXTERNALDB_ID());

            b.setIcon(FontAwesome.TRASH_O);
          } else {
            secNameField.setValue("DELETED");
            secNameField.setReadOnly(true);
            extIDField.setValue("DELETED");
            extIDField.setReadOnly(true);
            b.setIcon(FontAwesome.UNDO);
          }
        }
      });

      // Create the table row.
      List<Object> row = new ArrayList<Object>();

      TextField secNameField = new StandardTextField();
      secNameField.setImmediate(true);
      String secName = "";
      if (s.getQ_SECONDARY_NAME() != null)
        secName = s.getQ_SECONDARY_NAME();
      secNameField.setValue(secName);
      row.add(secNameField);

      TextField extIDField = new StandardTextField();
      extIDField.setWidth("95px");
      extIDField.setImmediate(true);
      String extID = "";
      if (s.getQ_EXTERNALDB_ID() != null)
        extID = s.getQ_EXTERNALDB_ID();
      extIDField.setValue(extID);
      row.add(extIDField);

      if (isotopes) {
        ComboBox cb = new ComboBox();
        cb.setImmediate(true);
        cb.addItems(reagents);
        cb.select(reagents.get(i % reagents.size()));
        row.add(cb);
      }
      int missing = maxCols - s.getFactors().size();
      for (Property f : s.getFactors()) {
        String v = f.getValue();
        if (f.hasUnit())
          v += " " + f.getUnit();
        row.add(v);
      }
      for (int j = 0; j < missing; j++)
        row.add("");
      row.add(delete);
      table.addItem(row.toArray(new Object[row.size()]), s);
    }
  }

  public void resetChanges() {
    for (String col : addedCols) {
      table.removeContainerProperty(col);
    }
  }
}
