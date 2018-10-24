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

import life.qbic.datamodel.experiments.ExperimentModel;
import life.qbic.datamodel.samples.AOpenbisSample;
import life.qbic.datamodel.samples.OpenbisMSSample;
import life.qbic.datamodel.samples.OpenbisTestSample;
import life.qbic.projectwizard.model.MSExperimentModel;
import life.qbic.projectwizard.model.Vocabularies;
import life.qbic.projectwizard.steps.MSAnalyteStep.AnalyteMultiplicationType;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.portal.portlet.ProjectWizardUI;
import life.qbic.xml.properties.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class MSSampleMultiplicationTable extends VerticalLayout {

  /**
   * 
   */
  private static final long serialVersionUID = -2282545855402710972L;
  private List<String> enzymes;
  private AnalyteMultiplicationType type;
  private boolean aboutPeptides;
  private Map<String, AOpenbisSample> tableIdToParent;
  private Map<Object, AOpenbisSample> tableIdToSample;
  private HashMap<String, List<String>> enzymeMap;
  private Table sampleTable;
  private CheckBox poolSamples;

  private static final Logger logger = LogManager.getLogger(MSSampleMultiplicationTable.class);
  private GeneralMSInfoPanel generalFractionMSInfo;


  public MSSampleMultiplicationTable(AnalyteMultiplicationType type, Vocabularies vocabs,
      boolean peptides) {
    setSpacing(true);

    this.type = type;
    this.aboutPeptides = peptides;
    this.enzymes = vocabs.getEnzymes();
    Collections.sort(enzymes);

    setSpacing(true);

    sampleTable = new Table();
    sampleTable.setWidth("775px");
    sampleTable.setCaption("Resulting " + type + "s");
    sampleTable.setStyleName(Styles.tableTheme);
    sampleTable.addContainerProperty("Base Sample", Label.class, null);
    sampleTable.addContainerProperty(type, Label.class, null);
    sampleTable.addContainerProperty(type + " Name", TextField.class, null);
    sampleTable.addContainerProperty(type + " Lab ID", TextField.class, null);
    sampleTable.addContainerProperty("Process", Component.class, null);
    sampleTable.addContainerProperty("Enzyme", Component.class, null);

    sampleTable.setColumnWidth("Base Sample", 110);
    sampleTable.setColumnWidth(type, 65);
    sampleTable.setColumnWidth(type + " Name", 210);
    sampleTable.setColumnWidth(type + " Lab ID", 110);
    sampleTable.setColumnWidth("Process", 130);
    sampleTable.setColumnWidth("Enzyme", 135);
    if (peptides) {
      sampleTable.setColumnCollapsingAllowed(true);
      sampleTable.setColumnCollapsed("Process", true);
      sampleTable.setColumnCollapsed("Enzyme", true);
    }
    addComponent(sampleTable);

    generalFractionMSInfo = new GeneralMSInfoPanel(vocabs, type + " Measurement Details");
    generalFractionMSInfo.setVisible(false);
    addComponent(generalFractionMSInfo);

    poolSamples = new CheckBox("Pool All " + type + "s");
    String info = "Create one pool of all protein " + type
        + " per original sample. They will be digested using the enzyme selected for digestion of single "
        + type + "s (see selection below).";
    if (peptides)
      info = "Create one pool of all peptide " + type
          + " per original sample. They will be measured using the same MS properties used for each single "
          + type + " (see selection below).";
    addComponent(Styles.questionize(poolSamples, info, "Pool All " + type + "s"));

    if (!peptides) {
      // add = new Button();
      // remove = new Button();
      // Styles.iconButton(add, FontAwesome.PLUS_SQUARE);
      // Styles.iconButton(remove, FontAwesome.MINUS_SQUARE);
      initListener();

      // enzymePane = new VerticalLayout();
      // enzymePane.setCaption(type + " Digestion Enzymes");
      // enzymePane.addComponent(c);
      // enzymePane.setVisible(false);
      // addComponent(enzymePane);
      // buttonGrid = new GridLayout(2, 1);
      // buttonGrid.setSpacing(true);
      // buttonGrid.addComponent(add);
      // buttonGrid.addComponent(remove);
      // buttonGrid.setVisible(false);
      // addComponent(buttonGrid);
    }
  }

  private void pasteSelectionToColumn(String propertyName, Object selection) {
    for (Object id : sampleTable.getItemIds()) {
      // should always be ID = 1
      ComboBox b = parseBoxRow(id, propertyName);
      if (selection != null && selection.equals("Custom") && propertyName.equals("Enzyme")) {
        String i = (String) id;
        enzymeMap.put(i, enzymeMap.get(1));
        b.addItem("Custom");
      }
      if (b.isEnabled())// check if this value should be set
        b.setValue(selection);
    }
  }

  public Table getTable() {
    return sampleTable;
  }

  private Object createComplexCellComponent(ComboBox contentBox, String propertyName,
      final Object i) {
    HorizontalLayout complexComponent = new HorizontalLayout();
    complexComponent.setWidth(contentBox.getWidth() + 10, contentBox.getWidthUnits());
    complexComponent.addComponent(contentBox);
    complexComponent.setExpandRatio(contentBox, 1);

    Button copy = new Button();
    Styles.iconButton(copy, FontAwesome.ARROW_CIRCLE_O_DOWN);
    copy.setStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    VerticalLayout vBox = new VerticalLayout();
    vBox.setWidth("15px");
    vBox.addComponent(copy);
    complexComponent.addComponent(vBox);
    complexComponent.setComponentAlignment(vBox, Alignment.BOTTOM_RIGHT);
    copy.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        ComboBox b = parseBoxRow(i, propertyName);
        Object selection = b.getValue();
        pasteSelectionToColumn(propertyName, selection);
      }
    });
    return complexComponent;
  }

  private void initListener() {
    // buttonListener = new Button.ClickListener() {
    //
    // private static final long serialVersionUID = 2240224129259577437L;
    //
    // @Override
    // public void buttonClick(ClickEvent event) {
    // if (event.getButton().equals(add))
    // add();
    // else
    // remove();
    // }
    // };
    // add.addClickListener(buttonListener);
    // remove.addClickListener(buttonListener);
  }

  // public List<String> getEnzymes() {
  // List<String> res = new ArrayList<String>();
  // for (EnzymeChooser c : choosers) {
  // if (c.isSet())
  // res.add(c.getEnzyme());
  // }
  // return res;
  // }

  // private void add() {
  // if (choosers.size() < 4) {
  // EnzymeChooser c = new EnzymeChooser(enzymes);
  // choosers.add(c);
  //
  // removeComponent(buttonGrid);
  //// enzymePane.addComponent(c);
  // addComponent(buttonGrid);
  // }
  // }
  //
  // private void remove() {
  // int size = choosers.size();
  // if (size > 1) {
  // EnzymeChooser last = choosers.get(size - 1);
  // last.reset();
  //// enzymePane.removeComponent(last);
  // choosers.remove(last);
  // }
  // }

  // public void resetInputs() {
  // for (EnzymeChooser c : choosers) {
  // c.reset();
  // }
  // }

  private ComboBox generateTableBox(Collection<String> entries, String width) {
    ComboBox b = new ComboBox();
    b.addItems(entries);
    b.setWidth(width);
    b.setFilteringMode(FilteringMode.CONTAINS);
    b.setStyleName(Styles.boxTheme);
    return b;
  }

  private TextField generateTableTextInput(String width) {
    TextField tf = new TextField();
    tf.setStyleName(Styles.fieldTheme);
    tf.setImmediate(true);
    tf.setWidth(width);
    tf.setValidationVisible(true);
    return tf;
  }

  public void setAnalyteSamples(List<AOpenbisSample> proteins,
      HashMap<Integer, Integer> tableIdToFractions, boolean peptides) {
    sampleTable.removeAllItems();
    tableIdToParent = new HashMap<String, AOpenbisSample>();
    enzymeMap = new HashMap<String, List<String>>();
    int i = 0;
    for (AOpenbisSample s : proteins) {
      i++;
      // multiply by number of fractions
      for (int j = 1; j <= tableIdToFractions.get(i); j++) {
        boolean complexRow = sampleTable.size() == 0; // the first row contains a combobox with
                                                      // added
                                                      // button to copy
        // its selection to the whole column

        String parentID = Integer.toString(i);
        String fractionID = Integer.toString(j);
        String id = parentID + "-" + fractionID;
        tableIdToParent.put(id, s);

        List<Object> row = new ArrayList<Object>();


        Label sample = new Label(s.getQ_SECONDARY_NAME() + "<br>" + s.getQ_EXTERNALDB_ID(),
            Label.CONTENT_XHTML);
        row.add(sample);

        Label num = new Label(fractionID);
        row.add(num);

        TextField secNameInput = generateTableTextInput("200px");
        secNameInput.setValue(s.getQ_SECONDARY_NAME() + " " + type + " #" + fractionID);
        row.add(secNameInput);
        TextField extIdInput = generateTableTextInput("95px");
        row.add(extIdInput);

        ComboBox processBox =
            generateTableBox(new ArrayList<String>(Arrays.asList("None", "Measure")), "95px");
        if (!peptides) {
          processBox.addItem("Digest");
          processBox.addItem("Both");
        }
        processBox.setNullSelectionAllowed(false);
        processBox.select("None");
        if (complexRow)
          row.add(createComplexCellComponent(processBox, "Process", id));
        else
          row.add(processBox);

        processBox.addValueChangeListener(new ValueChangeListener() {

          @Override
          public void valueChange(ValueChangeEvent event) {
            checkFractionMeasured();
          }
        });

        Collections.sort(enzymes);
        ComboBox enzymeBox = generateTableBox(enzymes, "105px");
        enzymeBox.removeAllItems();
        enzymeBox.addItem("[Multiple]");
        enzymeBox.addItems(enzymes);
        enzymeBox.setEnabled(false);
        enzymeBox.setFilteringMode(FilteringMode.CONTAINS);
        if (complexRow)
          row.add(createComplexCellComponent(enzymeBox, "Enzyme", id));
        else
          row.add(enzymeBox);
        final String rowNum = id;
        enzymeBox.addValueChangeListener(new ValueChangeListener() {

          @Override
          public void valueChange(ValueChangeEvent event) {
            Object newVal = enzymeBox.getValue();
            if (newVal.equals("[Multiple]"))
              createEnzymeSelectionWindow(rowNum);
            else if (!newVal.equals("Custom"))
              enzymeBox.removeItem("Custom");
          }
        });

        sampleTable.addItem(row.toArray(new Object[row.size()]), id);

        processBox.addValueChangeListener(new ValueChangeListener() {

          @Override
          public void valueChange(ValueChangeEvent event) {
            String value = (String) processBox.getValue();
            boolean enableEnzyme = value.equals("Digest") || value.equals("Both");
            parseBoxRow(id, "Enzyme").setEnabled(enableEnzyme);
            // boolean enableMS = value.equals("Measure") || value.equals("Both");
            // parseBoxRow(item, "Chr. Type").setEnabled(enableMS);
            // parseBoxRow(item, "LCMS Method").setEnabled(enableMS);
            // parseBoxRow(item, "MS Device").setEnabled(enableMS);
          }
        });
      }
    }
    int pagelength = 0;
    for (int n : tableIdToFractions.values()) {
      pagelength += n;
    }
    sampleTable.setPageLength(pagelength);
    this.setVisible(pagelength > 0);
    checkFractionMeasured();
  }

  protected void createEnzymeSelectionWindow(String row) {
    Window subWindow = new Window(" Enzyme selection");
    subWindow.setWidth("400px");

    VerticalLayout layout = new VerticalLayout();
    layout.setSpacing(true);
    layout.setMargin(true);
    EnzymePanel pan = new EnzymePanel(enzymes);
    Button ok = new Button("Okay.");
    ok.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        List<String> enzymes = pan.getEnzymes();
        ComboBox b = parseBoxRow(row, "Enzyme");
        if (enzymes.isEmpty()) {
          Styles.notification("No enzymes selected", "Please select at least one enzyme!",
              NotificationType.ERROR);
        } else if (enzymes.size() == 1) {
          b.setValue(enzymes.get(0));
          subWindow.close();
        } else {
          b.addItem("Custom");
          b.setValue("Custom");
          enzymeMap.put(row, enzymes);
          subWindow.close();
        }
      }
    });
    layout.addComponent(pan);
    layout.addComponent(ok);

    subWindow.setContent(layout);
    // Center it in the browser window
    subWindow.center();
    subWindow.setModal(true);
    subWindow.setIcon(FontAwesome.FLASK);
    subWindow.setResizable(false);
    ProjectWizardUI ui = (ProjectWizardUI) UI.getCurrent();
    ui.addWindow(subWindow);
  }

  protected void checkFractionMeasured() {
    // boolean digest = false;
    boolean measure = false;
    for (Object i : sampleTable.getItemIds()) {
      ComboBox processBox = parseBoxRow(i, "Process");
      String process = processBox.getValue().toString();
      // digest |= process.equals("Both") || process.equals("Digest");
      measure |= process.equals("Both") || process.equals("Measure") || aboutPeptides;
    }
    // boolean hasSamples = sampleTable.size() > 0;
    // if (!aboutPeptides) {
    // enzymePane.setVisible(digest && hasSamples);
    // buttonGrid.setVisible(digest && hasSamples);
    // }
    generalFractionMSInfo.setVisible(measure);
  }

  private ComboBox parseBoxRow(Object id, String propertyName) {
    Item item = sampleTable.getItem(id);
    Object component = item.getItemProperty(propertyName).getValue();
    if (component instanceof ComboBox)
      return (ComboBox) component;
    else {
      HorizontalLayout h = (HorizontalLayout) component;
      return (ComboBox) h.getComponent(0);
    }
  }

  private TextField parseTextRow(Object id, String propertyName) {
    Item item = sampleTable.getItem(id);
    TextField t = (TextField) item.getItemProperty(propertyName).getValue();
    return t;
  }

  public boolean isValid() {
    boolean res = true;
    String error = "";
    for (Iterator i = sampleTable.getItemIds().iterator(); i.hasNext();) {
      // Get the current item identifier, which is an integer.
      int iid = (Integer) i.next();

      // Now get the actual item from the table.
      // Item item = proteinExperiments.getItem(iid);
      error = "Please fill in the number of " + type + "s (or '0' for none)"; // TODO old?

      String fractions = parseTextRow(iid, type + "s").getValue();
      if (!fractions.isEmpty()) {
        try {
          Integer.parseInt(fractions);
        } catch (NumberFormatException e) {
          res = false;
        }
      } else {
        res = false;
      }
    }

    if (!res) {
      Styles.notification("Missing Input", error, NotificationType.ERROR);
      return false;
    } else
      return true;
  }

  private List<String> getEnzymesFromSampleRow(Object i) {
    if (parseBoxRow(i, "Enzyme").getValue() == null)
      return null;
    else {
      String entry = parseBoxRow(i, "Enzyme").getValue().toString();
      if (entry.equals("Custom"))
        return enzymeMap.get(i);
      else
        return new ArrayList<String>(Arrays.asList(entry));
    }
  }

  public MSExperimentModel getFractionsWithMSProperties(MSExperimentModel model, String sampleType,
      String method) {
    Map<String, ExperimentModel> fractHelper = new HashMap<String, ExperimentModel>();
    List<ExperimentModel> fractionations = new ArrayList<ExperimentModel>();
    List<ExperimentModel> msExperiments = new ArrayList<ExperimentModel>();
    List<ExperimentModel> peptides = new ArrayList<ExperimentModel>();

    Map<String, Object> props = generalFractionMSInfo.getExperimentalProperties();
    // there can be one ms measurement experiment per protein/peptide sample measured (fractions)
    // new ms experiment
    ExperimentModel msExp = new ExperimentModel(0, new ArrayList<AOpenbisSample>());
    msExp.setProperties(props);

    Map<String, List<AOpenbisSample>> peptidesPerDigestion =
        new HashMap<String, List<AOpenbisSample>>();
    // collect samples
    tableIdToSample = new HashMap<Object, AOpenbisSample>();
    for (Object i : sampleTable.getItemIds()) {
      String item = (String) i;
      String[] ids = item.split("-");

      AOpenbisSample parent = tableIdToParent.get(item);

      // new fraction/enrichment cycled sample - this always exists if it's in the table
      String secondaryName = parseTextRow(i, type.toString() + " Name").getValue();
      String extID = parseTextRow(i, type.toString() + " Lab ID").getValue();
      OpenbisTestSample fractionSample =
          new OpenbisTestSample(-2, new ArrayList<AOpenbisSample>(Arrays.asList(parent)),
              sampleType, secondaryName, extID, new ArrayList<Property>(), "");

      ComboBox selection = parseBoxRow(i, "Process");
      String option = selection.getValue().toString();
      if (option.equals("Both") || option.equals("Measure") || sampleType.equals("PEPTIDES")) {
        // new ms sample
        OpenbisMSSample msSample =
            new OpenbisMSSample(-1, new ArrayList<AOpenbisSample>(Arrays.asList(fractionSample)),
                secondaryName + " run", extID + " run", new ArrayList<Property>(), "");
        msExp.addSample(msSample);
        // used to attach wash samples
        tableIdToSample.put(i, msSample);
        // if we have at least one ms measurement, the experiment gets added to the experiment list
        if (msExperiments.isEmpty())
          msExperiments.add(msExp);
      }
      if (option.equals("Both") || option.equals("Digest")) {
        OpenbisTestSample pepSample =
            new OpenbisTestSample(-1, new ArrayList<AOpenbisSample>(Arrays.asList(fractionSample)),
                "PEPTIDES", fractionSample.getQ_SECONDARY_NAME() + " digested",
                fractionSample.getQ_EXTERNALDB_ID(), new ArrayList<Property>(), "");
        List<String> enzymes = getEnzymesFromSampleRow(i);
        String digestion = StringUtils.join(enzymes, ", ");
        if (peptidesPerDigestion.containsKey(digestion))
          peptidesPerDigestion.get(digestion).add(pepSample);
        else
          peptidesPerDigestion.put(digestion,
              new ArrayList<AOpenbisSample>(Arrays.asList(pepSample)));
      }
      if (fractHelper.containsKey(ids[0])) {
        fractHelper.get(ids[0]).addSample(fractionSample);
      } else {
        // there is only one fractionation experiment per fractionation containing all of the
        // resulting samples
        ExperimentModel fractionExp =
            new ExperimentModel(item, new ArrayList<AOpenbisSample>(Arrays.asList(fractionSample)));
        switch (type) {
          case Fraction:
            fractionExp.addProperty("Q_MS_FRACTIONATION_METHOD", method);
            break;
          case Cycle:
            fractionExp.addProperty("Q_MS_ENRICHMENT_METHOD", method);
            break;
          default:
            logger.error("Unknown AnalyteMultiplicationType: " + type);
            break;
        }
        fractHelper.put(ids[0], fractionExp);
        if (sampleType.equals("PEPTIDES"))
          peptides.add(fractionExp);
        else
          fractionations.add(fractionExp);
      }
    }
    // one digestion experiment per unique enzyme set used
    for (String digestion : peptidesPerDigestion.keySet()) {
      ExperimentModel peptideExp = new ExperimentModel("", peptidesPerDigestion.get(digestion));
      peptideExp.addProperty("Q_ADDITIONAL_INFO", "Digestion: " + digestion);
      peptides.add(peptideExp);
    }

    if (fractionations.size() > 0)
      model.addAnalyteStepExperiments(fractionations);
    if (msExperiments.size() > 0)
      model.addMSRunStepExperiments(msExperiments);
    if (peptides.size() > 0)
      model.addDigestionExperiment(peptides);
    if (poolSamples.getValue()) {
      List<AOpenbisSample> pools = new ArrayList<AOpenbisSample>();
      int i = 0;
      for (String id : fractHelper.keySet()) {
        i++;

        List<AOpenbisSample> fractions = fractHelper.get(id).getSamples();
        AOpenbisSample pool = new OpenbisTestSample(1, fractions, sampleType,
            sampleType.toLowerCase() + " pool " + Integer.toString(i), "", new ArrayList<Property>(),
            "");
        pools.add(pool);

        if (sampleType.equals("PEPTIDES")) {
          ExperimentModel msExpPool =
              new ExperimentModel(2, new ArrayList<AOpenbisSample>(Arrays.asList(pool)));
          msExp.setProperties(props);
          model.getLastStepMsRuns().add(msExpPool);
        } else {
          OpenbisTestSample pepSample =
              new OpenbisTestSample(-1, new ArrayList<AOpenbisSample>(Arrays.asList(pool)),
                  "PEPTIDES", pool.getQ_SECONDARY_NAME() + " digested", pool.getQ_EXTERNALDB_ID(),
                  new ArrayList<Property>(), "");
          ExperimentModel peptideExpPool =
              new ExperimentModel(1, new ArrayList<AOpenbisSample>(Arrays.asList(pepSample)));
          model.getPeptideExperiments().add(peptideExpPool);
        }
      }

      if (sampleType.equals("PEPTIDES")) {
        model.getPeptideExperiments().add(new ExperimentModel(3, pools));
      } else {
        model.getLastStepAnalytes().add(new ExperimentModel(3, pools));
      }
    }
    return model;
  }

  public boolean hasDigestions() {
    for (Object i : sampleTable.getItemIds()) {
      ComboBox selection = parseBoxRow(i, "Process");
      String option = selection.getValue().toString();
      if (option.equals("Both") || option.equals("Digest"))
        return true;
    }
    return false;
  }

  public void filterDictionariesByPrefix(String prefix, List<String> dontFilter) {
    generalFractionMSInfo.filterDictionariesByPrefix(prefix, dontFilter);
  }

  public AOpenbisSample getSampleFromRow(Object id) {
    AOpenbisSample s = tableIdToSample.get(id);
    return s;
  }

  public boolean rowIsMeasuredAndNotNull(Object id) {
    if(id==null)
      return false;
    if (aboutPeptides)
      return true;
    ComboBox box = parseBoxRow(id, "Process");
    if (box.getValue() != null) {
      String option = (String) box.getValue();
      return option.equals("Both") || option.equals("Measure");
    } else
      return false;
  }

}

