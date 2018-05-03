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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import logging.Log4j2Logger;

import com.vaadin.data.Item;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.DateField;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import componentwrappers.StandardTextField;
import life.qbic.portlet.io.DBVocabularies;
import life.qbic.portlet.model.AOpenbisSample;
import life.qbic.portlet.model.MHCLigandExtractionProtocol;
import life.qbic.portlet.uicomponents.Styles;

public class LigandExtractPanel extends VerticalLayout {

  /**
   * 
   */
  private static final long serialVersionUID = -5539202326542301786L;

  private Map<String, String> antiBodies;

  private Map<Integer, String> tableIdToBarcode;

  private Table extractionExperiments;

  logging.Logger logger = new Log4j2Logger(LigandExtractPanel.class);

  public LigandExtractPanel(DBVocabularies vocabs) {
    this.setCaption("MHC Ligand Extraction");
    // this.vocabs = vocabs;
    this.antiBodies = vocabs.getAntibodiesMap();
    setSpacing(true);

    extractionExperiments = new Table();
    extractionExperiments.setStyleName(Styles.tableTheme);
    extractionExperiments.addContainerProperty("Sample", String.class, null);
    extractionExperiments.addContainerProperty("Mass [mg]", TextField.class, null);
    extractionExperiments.addContainerProperty("Date", DateField.class, null);
    extractionExperiments.addContainerProperty("Antibody 1", ComboBox.class, null);
    extractionExperiments.addContainerProperty("Mass 1 [mg]", TextField.class, null);
    extractionExperiments.addContainerProperty("Antibody 2", ComboBox.class, null);
    extractionExperiments.addContainerProperty("Mass 2 [mg]", TextField.class, null);
    extractionExperiments.addContainerProperty("Antibody 3", ComboBox.class, null);
    extractionExperiments.addContainerProperty("Mass 3 [mg]", TextField.class, null);

    extractionExperiments.setColumnWidth("Mass [mg]", 79);
    extractionExperiments.setColumnWidth("Date", 115);
    extractionExperiments.setColumnWidth("Antibody 1", 108);
    extractionExperiments.setColumnWidth("Antibody 2", 108);
    extractionExperiments.setColumnWidth("Antibody 3", 108);
    extractionExperiments.setColumnWidth("Mass 1 [mg]", 79);
    extractionExperiments.setColumnWidth("Mass 2 [mg]", 79);
    extractionExperiments.setColumnWidth("Mass 3 [mg]", 79);

    extractionExperiments.setColumnHeader("Mass 1 [mg]", "Mass [mg]");
    extractionExperiments.setColumnHeader("Mass 2 [mg]", "Mass [mg]");
    extractionExperiments.setColumnHeader("Mass 3 [mg]", "Mass [mg]");
    extractionExperiments.setColumnHeader("Antibody 1", "Antibody");
    extractionExperiments.setColumnHeader("Antibody 2", "Antibody");
    extractionExperiments.setColumnHeader("Antibody 3", "Antibody");

    addComponent(extractionExperiments);
  }

  public Map<String, Map<String, Object>> getExperimentalProperties() {
    Map<String, Map<String, Object>> res = new HashMap<String, Map<String, Object>>();
    Map<String, MHCLigandExtractionProtocol> infos = getAntibodyInfos();
    for (String key : infos.keySet())
      res.put(key, translateToProperties(infos.get(key)));
    return res;
  }

  private Map<String, Object> translateToProperties(MHCLigandExtractionProtocol prot) {
    Map<String, Object> res = new HashMap<String, Object>();

    try {
      res.put("Q_SAMPLE_MASS", Double.parseDouble(prot.getInputSampleMass()));
    } catch (Exception e) {
      logger.warn("Sample mass not set or not parsable, ignoring.");
    }

    try {
      SimpleDateFormat dateformat = new SimpleDateFormat("dd-MM-yyyy");
      Date date = prot.getPrepDate();
      if (date != null) {
        String dateString = dateformat.format(date);
        res.put("Q_PREPARATION_DATE", dateString);
      }
    } catch (IllegalArgumentException e) {
      logger.warn("No valid preparation date input. Not setting Date for this experiment.");
    }

    List<String> antibodies = prot.getAntibodies();
    for (int i = 1; i <= antibodies.size(); i++) {
      String antibody = antibodies.get(i - 1);
      res.put("Q_MHC_ANTIBODY_COL" + Integer.toString(i), antibody);
      try {
        double mass = Double.parseDouble(prot.getAntiBodyMasses().get(i - 1));
        res.put("Q_MHC_ANTIBODY_MASS_COL" + Integer.toString(i), mass);
      } catch (Exception e) {
        logger.warn("Antibody mass not set or not parsable, ignoring.");
      }

    }
    return res;
  }

  private TextField generateTableIntegerInput() {
    TextField tf = new StandardTextField();
    tf.setImmediate(true);
    tf.setWidth("55px");
    tf.setValidationVisible(true);
    return tf;
  }

  private ComboBox generateTableAntibodyBox() {
    ComboBox b = new ComboBox();
    b.setWidth("100px");
    b.addItems(this.antiBodies.keySet());
    b.setStyleName(Styles.boxTheme);
    return b;
  }

  public void setTissueSamples(List<AOpenbisSample> extracts) {
    extractionExperiments.removeAllItems();
    tableIdToBarcode = new HashMap<Integer, String>();
    int i = 0;
    for (AOpenbisSample s : extracts) {
      i++;
      tableIdToBarcode.put(i, s.getCode());

      List<Object> row = new ArrayList<Object>();

      row.add(s.getQ_SECONDARY_NAME());
      row.add(generateTableIntegerInput());
      DateField date = new DateField();
      date.setDateFormat("dd.MM.yy");
      date.setWidth("110px");
      date.setStyleName(Styles.fieldTheme);
      row.add(date);
      row.add(generateTableAntibodyBox());
      row.add(generateTableIntegerInput());
      row.add(generateTableAntibodyBox());
      row.add(generateTableIntegerInput());
      row.add(generateTableAntibodyBox());
      row.add(generateTableIntegerInput());

      extractionExperiments.addItem(row.toArray(new Object[row.size()]), i);
    }
    extractionExperiments.setPageLength(extracts.size());
  }

  public Map<String, MHCLigandExtractionProtocol> getAntibodyInfos() {
    Map<String, MHCLigandExtractionProtocol> res =
        new HashMap<String, MHCLigandExtractionProtocol>();

    for (Iterator i = extractionExperiments.getItemIds().iterator(); i.hasNext();) {
      // Get the current item identifier, which is an integer.
      int iid = (Integer) i.next();

      // Now get the actual item from the table.
      Item item = extractionExperiments.getItem(iid);

      String source = tableIdToBarcode.get(iid);// came from this sample
      TextField mass = (TextField) item.getItemProperty("Mass [mg]").getValue();
      String inputSampleMass = mass.getValue();
      DateField d = (DateField) item.getItemProperty("Date").getValue();
      Date date = d.getValue();
      ComboBox ab1 = (ComboBox) item.getItemProperty("Antibody 1").getValue();
      TextField mass1 = (TextField) item.getItemProperty("Mass 1 [mg]").getValue();
      ComboBox ab2 = (ComboBox) item.getItemProperty("Antibody 2").getValue();
      TextField mass2 = (TextField) item.getItemProperty("Mass 2 [mg]").getValue();
      ComboBox ab3 = (ComboBox) item.getItemProperty("Antibody 3").getValue();
      TextField mass3 = (TextField) item.getItemProperty("Mass 3 [mg]").getValue();
      List<String> antibodies = new ArrayList<String>();
      List<String> antibodyMasses = new ArrayList<String>();

      if (ab1.getValue() != null) {
        antibodies.add(ab1.getValue().toString());
        antibodyMasses.add(mass1.getValue());
      }
      if (ab2.getValue() != null) {
        antibodies.add(ab2.getValue().toString());
        antibodyMasses.add(mass2.getValue());
      }
      if (ab3.getValue() != null) {
        antibodies.add(ab3.getValue().toString());
        antibodyMasses.add(mass3.getValue());
      }

      res.put(source, new MHCLigandExtractionProtocol(inputSampleMass, date, antibodies,
          antibodyMasses));
    }
    return res;
  }

  public boolean isValid() {
    boolean res = true;
    String error = "";
    for (Iterator i = extractionExperiments.getItemIds().iterator(); i.hasNext();) {
      // Get the current item identifier, which is an integer.
      int iid = (Integer) i.next();

      // Now get the actual item from the table.
      Item item = extractionExperiments.getItem(iid);

      TextField mass = (TextField) item.getItemProperty("Mass [mg]").getValue();
      try {
        Integer.parseInt(mass.getValue());
      } catch (NumberFormatException e) {
        res = false;
        error = "Sample mass has to be a number!";
      }
      DateField d = (DateField) item.getItemProperty("Date").getValue();

      if (d.getValue() == null) {
        error = "Please select preparation dates for all samples!";
      }
      ComboBox ab1 = (ComboBox) item.getItemProperty("Antibody 1").getValue();
      TextField mass1 = (TextField) item.getItemProperty("Mass 1 [mg]").getValue();
      ComboBox ab2 = (ComboBox) item.getItemProperty("Antibody 2").getValue();
      TextField mass2 = (TextField) item.getItemProperty("Mass 2 [mg]").getValue();
      ComboBox ab3 = (ComboBox) item.getItemProperty("Antibody 3").getValue();
      TextField mass3 = (TextField) item.getItemProperty("Mass 3 [mg]").getValue();

      String antibodyError = "Please choose at least one antibody and fill in the mass.";

      if (ab1.getValue() != null && !mass1.getValue().isEmpty()) {
        try {
          Integer.parseInt(mass.getValue());
        } catch (NumberFormatException e) {
          res = false;
          error = "Antibody 1 mass has to be a number!";
        }
      } else {
        res = false;
        error = antibodyError;
      }
      if (ab2.getValue() != null && !mass2.getValue().isEmpty()) {
        try {
          Integer.parseInt(mass.getValue());
        } catch (NumberFormatException e) {
          res = false;
          error = "Antibody 2 mass has to be a number!";
        }
      }
      if (ab3.getValue() != null && !mass3.getValue().isEmpty()) {
        try {
          Integer.parseInt(mass.getValue());
        } catch (NumberFormatException e) {
          res = false;
          error = "Antibody 3 mass has to be a number!";
        }
      }
    }

    if (!res) {
      Notification n = new Notification(error);
      n.setStyleName(ValoTheme.NOTIFICATION_CLOSABLE);
      n.setDelayMsec(-1);
      n.show(UI.getCurrent().getPage());
      return false;
    } else
      return true;
  }

}
