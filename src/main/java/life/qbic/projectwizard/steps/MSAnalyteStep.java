package life.qbic.projectwizard.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.ValoTheme;

import life.qbic.datamodel.experiments.ExperimentModel;
import life.qbic.datamodel.ms.MSProperties;
import life.qbic.datamodel.samples.AOpenbisSample;
import life.qbic.datamodel.samples.OpenbisMSSample;
import life.qbic.datamodel.samples.OpenbisTestSample;
import life.qbic.portal.portlet.ProjectWizardUI;
import life.qbic.projectwizard.io.DBVocabularies;
import life.qbic.projectwizard.model.MSExperimentModel;
import life.qbic.projectwizard.uicomponents.EnzymePanel;
import life.qbic.projectwizard.uicomponents.MSSampleMultiplicationTable;
import life.qbic.projectwizard.uicomponents.SampleSelectComponent;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;

public class MSAnalyteStep implements WizardStep {

  private VerticalLayout main;
  private static final Logger logger = LogManager.getLogger(MSAnalyteStep.class);

  private OptionGroup analyteOptions = new OptionGroup();
  private Table baseAnalyteSampleTable;
  private MSSampleMultiplicationTable msFractionationTable;
  private MSSampleMultiplicationTable msEnrichmentTable;
  private TextField washRunCount;
  private Table washRuns;
  private boolean selectInfoWasShown;

  private HashMap<Integer, AOpenbisSample> tableIdToAnalyte;
  private Map<Object, AOpenbisSample> tableIdToMSRun;
  private HashMap<Integer, Integer> tableIdToFractions;
  private HashMap<Integer, Integer> tableIdToCycles;
  private HashMap<Integer, List<String>> enzymeMap;
  private TextArea additionalInfo;

  private MSExperimentModel msExperimentModel;
  private String analyte;
  private DBVocabularies vocabs;
  private ComboBox fractionationSelection;
  private ComboBox enrichmentSelection;
  private boolean needsDigestion = false;
  private boolean hasRun = false;
  private MSExperimentModel results;
  private List<String> lcmsMethods;
  private List<String> devices;

  public static enum AnalyteMultiplicationType {
    Fraction, Cycle;
  }

  public MSAnalyteStep(DBVocabularies vocabs, String analyte) {
    this.analyte = analyte;
    main = new VerticalLayout();
    main.setSpacing(true);
    main.setMargin(true);
    this.vocabs = vocabs;

    additionalInfo = new TextArea("General Information");
    additionalInfo.setStyleName(Styles.areaTheme);
    main.addComponent(additionalInfo);

    String label = "Protein Options";
    String info =
        "Here you can select different fractionation techniques used on the protein samples as well as digest them using different enzymes. "
            + "For samples that are measured, mass spectrometry specific information can be saved.";
    if (analyte.equals("PEPTIDES")) {
      label = "Peptide Options";
      info = "Here you can select different fractionation techniques used on the peptide samples. "
          + "Mass spectrometry specific information about peptide measurements can be saved.";
    }
    Label header = new Label(label);
    main.addComponent(Styles.questionize(header, info, label));

    analyteOptions.addItems(new ArrayList<String>(Arrays.asList("Fractionation", "Enrichment")));
    analyteOptions.setMultiSelect(true);
    main.addComponent(analyteOptions);

    baseAnalyteSampleTable = new Table();
    baseAnalyteSampleTable.setImmediate(true);
    baseAnalyteSampleTable.setStyleName(Styles.tableTheme);
    baseAnalyteSampleTable.addContainerProperty("Sample", Label.class, null);
    baseAnalyteSampleTable.addContainerProperty("Fractions", TextField.class, null);
    baseAnalyteSampleTable.addContainerProperty("Cycles", TextField.class, null);
    baseAnalyteSampleTable.addContainerProperty("Process", Component.class, null);
    baseAnalyteSampleTable.addContainerProperty("Enzyme", Component.class, null);
    baseAnalyteSampleTable.addContainerProperty("Chr. Type", Component.class, null);
    baseAnalyteSampleTable.addContainerProperty("MS Device", Component.class, null);
    baseAnalyteSampleTable.addContainerProperty("LCMS Method", Component.class, null);
    baseAnalyteSampleTable.addContainerProperty("Method Description", TextField.class, null);

    baseAnalyteSampleTable.setColumnWidth("Process", 125);
    baseAnalyteSampleTable.setColumnWidth("Fractions", 71);
    baseAnalyteSampleTable.setColumnWidth("Cycles", 54);
    baseAnalyteSampleTable.setColumnWidth("Chr. Type", 130);
    baseAnalyteSampleTable.setColumnWidth("Enzyme", 135);
    baseAnalyteSampleTable.setColumnWidth("LCMS Method", 150);
    baseAnalyteSampleTable.setColumnWidth("Method Description", 175);
    baseAnalyteSampleTable.setColumnWidth("MS Device", 130);

    // This is where the magic happens. Selection of fractionation enables the fractions column of
    // the main table.
    // Selection of enrichment enables the cycles column of the main table. Additionally, the combo
    // boxes that allow specification of the used methods are enabled or disabled depending on user
    // choice. Changing enrichment cycles or fractions to values larger 1 results in a second table
    // (cycles table or fractionation table) to be initialized containing the resulting fractions or
    // enriched samples.
    analyteOptions.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        Collection<String> test = (Collection<String>) analyteOptions.getValue();
        boolean enrich = false;
        boolean fract = false;
        for (String s : test) {
          if (s.equals("Fractionation")) {
            fract = true;
          }
          if (s.equals("Enrichment")) {
            enrich = true;
          }
        }
        if (!fract) {
          for (Object id : baseAnalyteSampleTable.getItemIds()) {
            TextField b = parseTextRow(baseAnalyteSampleTable, id, "Fractions");
            b.setValue("0");
          }
        }
        if (!enrich) {
          for (Object id : baseAnalyteSampleTable.getItemIds()) {
            TextField b = parseTextRow(baseAnalyteSampleTable, id, "Cycles");
            b.setValue("0");
          }
        }
        enableCol("Fractions", fract);
        enableCol("Cycles", enrich);
        enrichmentSelection.setVisible(enrich);
        fractionationSelection.setVisible(fract);
      }
    });

    if (analyte.equals("PEPTIDES")) {
      baseAnalyteSampleTable.setColumnCollapsingAllowed(true);
      collapseColumn(true, "Enzyme");
    }

    main.addComponent(baseAnalyteSampleTable);

    List<String> fractMethods = vocabs.getFractionationTypes();
    Collections.sort(fractMethods);
    fractionationSelection = new ComboBox("Fractionation Method", fractMethods);
    fractionationSelection.setVisible(false);
    fractionationSelection.setRequired(true);
    fractionationSelection.setStyleName(Styles.boxTheme);
    fractionationSelection.setNullSelectionAllowed(false);
    main.addComponent(fractionationSelection);

    msFractionationTable = new MSSampleMultiplicationTable(AnalyteMultiplicationType.Fraction,
        vocabs, analyte.equals("PEPTIDES"));
    main.addComponent(msFractionationTable);

    List<String> enrichMethods = vocabs.getEnrichmentTypes();
    Collections.sort(enrichMethods);
    enrichmentSelection = new ComboBox("Enrichment Method", enrichMethods);
    enrichmentSelection.setStyleName(Styles.boxTheme);
    enrichmentSelection.setVisible(false);
    enrichmentSelection.setRequired(true);
    enrichmentSelection.setNullSelectionAllowed(false);
    main.addComponent(enrichmentSelection);

    msEnrichmentTable = new MSSampleMultiplicationTable(AnalyteMultiplicationType.Cycle, vocabs,
        analyte.equals("PEPTIDES"));
    main.addComponent(msEnrichmentTable);

    ObjectProperty<Integer> washCount = new ObjectProperty<Integer>(0);
    washRunCount = new TextField("Wash Runs (" + WordUtils.capitalize(analyte) + ")");
    washRunCount.setConverter(new StringToIntegerConverter());
    washRunCount.setWidth("40px");
    washRunCount.setStyleName(Styles.fieldTheme);
    washRunCount.setPropertyDataSource(washCount);

    washRuns = new Table("Wash Runs");
    washRuns.addContainerProperty("Sample Selection", Component.class, null);
    washRuns.addContainerProperty("Name", TextField.class, null);
    washRuns.addContainerProperty("Lab ID", TextField.class, null);
    washRuns.addContainerProperty("LCMS Method", Component.class, null);
    washRuns.setColumnWidth("Sample Selection", 250);
    washRuns.setColumnWidth("Name", 210);
    washRuns.setColumnWidth("Lab ID", 110);
    washRuns.setColumnWidth("LCMS Method", 150);
    washRuns.setVisible(false);

    washRunCount.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        boolean empty = washCount.getValue() == 0;
        washRuns.removeAllItems();
        washRuns.setVisible(!empty);
        for (int i = 1; i <= washCount.getValue(); i++) {
          final int rowID = i;
          List<Object> row = new ArrayList<Object>();

          TextField washName = generateTableTextInput("180px");
          washName.setValue("Wash Run " + Integer.toString(i));
          SampleSelectComponent sampleSelect = new SampleSelectComponent();
          sampleSelect.getSelectButton().addClickListener(new ClickListener() { // *inception sound*

            @Override
            public void buttonClick(ClickEvent event) {
              sampleSelect.reset();
              cancelListeners();
              cancelTableSelections();
              initWashSampleConnectionSelection(sampleSelect, rowID);
            }
          });
          row.add(sampleSelect);
          row.add(washName);
          row.add(generateTableTextInput("95px"));

          ComboBox lcmsMethodBox = generateTableBox(lcmsMethods, "115px");
          lcmsMethodBox.setFilteringMode(FilteringMode.CONTAINS);

          boolean complexRow = i == 1;
          if (complexRow)
            row.add(createComplexCellComponent(washRuns, lcmsMethodBox, "LCMS Method", i));
          else
            row.add(lcmsMethodBox);
          washRuns.addItem(row.toArray(new Object[row.size()]), i);

        }
        washRuns.setPageLength(Math.min(10, washCount.getValue()));
        showSampleSelectInfo();
      }
    });

    main.addComponent(washRunCount);
    main.addComponent(washRuns);
  }

  private void showSampleSelectInfo() {
    if (!selectInfoWasShown) {
      Styles.notification("Sample Selection",
          "You can click on any sample in any of the tables to connect it to this wash run.",
          NotificationType.DEFAULT);
      selectInfoWasShown = true;
    }
  }

  private void enableCol(String colName, boolean enable) {
    for (Object i : baseAnalyteSampleTable.getItemIds()) {
      parseTextRow(baseAnalyteSampleTable, i, colName).setEnabled(enable);
    }
  }

  private void collapseColumn(boolean hide, String colName) {
    baseAnalyteSampleTable.setColumnCollapsed(colName, hide);
  }

  private void initTableListenersForWashSelection(Table t, SampleSelectComponent sampleSelect,
      String infoCol, int washRow) {

    t.addValueChangeListener(new ValueChangeListener() {
      @Override
      public void valueChange(ValueChangeEvent event) {
        boolean mainTable = infoCol.equals("Sample");
        boolean valid = false;
        Object id = t.getValue();
        if (mainTable) {
          valid = rowIsMeasuredAndNotNull(id);
        } else {
          if (t.equals(msEnrichmentTable.getTable()))
            valid = msEnrichmentTable.rowIsMeasuredAndNotNull(id);
          else
            valid = msFractionationTable.rowIsMeasuredAndNotNull(id);
        }
        if (valid) {
          String info = "";
          if (infoCol.equals("Sample")) {
            Label l = parseLabelRow(t, id, "Sample");
            sampleSelect.setSample(l, t, id);
            info = l.getValue();
          } else {
            TextField txt = parseTextRow(t, id, infoCol);
            sampleSelect.setSample(txt, t, id);
            info = txt.getValue();
          }
          String info2 = info;
          if (info.contains("<br>")) {
            String[] splt = info.split("<br>");
            info = splt[0];
            if (splt.length > 1)
              info2 = splt[1];
          }
          parseTextRow(washRuns, washRow, "Name").setValue(info + " wash");
          parseTextRow(washRuns, washRow, "Lab ID").setValue(info2 + " wash");
        } else {
          notifyUnmeasuredSample();
        }
        cancelListeners();
        cancelTableSelections();
      }
    });
    t.setSelectable(true);
  }

  /**
   * called whenever a wash button is clicked
   * 
   * @param sampleSelect
   * @param washRow
   */
  private void initWashSampleConnectionSelection(SampleSelectComponent sampleSelect, int washRow) {
    Table enrichTable = msEnrichmentTable.getTable();
    Table fractTable = msFractionationTable.getTable();
    initTableListenersForWashSelection(enrichTable, sampleSelect, "Cycle Name", washRow);
    initTableListenersForWashSelection(fractTable, sampleSelect, "Fraction Name", washRow);
    initTableListenersForWashSelection(baseAnalyteSampleTable, sampleSelect, "Sample", washRow);
  }

  protected void notifyUnmeasuredSample() {
    Styles.notification("Sample is not measured.",
        "The Sample you selected is not set to be measured. Please change the Process or select a different sample.",
        NotificationType.DEFAULT);
  }

  protected boolean rowIsMeasuredAndNotNull(Object id) {
    if (id == null)
      return false;
    ComboBox box = parseBoxRow(baseAnalyteSampleTable, id, "Process");
    if (box.getValue() != null) {
      String option = (String) box.getValue();
      return option.equals("Both") || option.equals("Measure");
    } else
      return false;
  }

  protected void cancelListeners() {
    List<Table> tables = Arrays.asList(baseAnalyteSampleTable, msFractionationTable.getTable(),
        msEnrichmentTable.getTable());
    for (Table t : tables) {
      Collection<?> listeners = t.getListeners(ValueChangeEvent.class);
      for (Object l : listeners) {
        t.removeValueChangeListener((ValueChangeListener) l);
      }
    }
  }

  protected void cancelTableSelections() {
    List<Table> tables = Arrays.asList(baseAnalyteSampleTable, msFractionationTable.getTable(),
        msEnrichmentTable.getTable());
    for (Table t : tables) {
      t.select(null);
      t.setSelectable(false);
    }
  }

  private TextField generateTableTextInput(String width) {
    TextField tf = new TextField();
    tf.setStyleName(Styles.fieldTheme);
    tf.setImmediate(true);
    tf.setWidth(width);
    tf.setValidationVisible(true);
    return tf;
  }

  public String getAdditionalInfo() {
    return additionalInfo.getValue();
  }

  public boolean hasRun() {
    return hasRun;
  }

  public void setAnalyteSamples(List<AOpenbisSample> analytes,
      Map<String, List<AOpenbisSample>> pools) {
    hasRun = false;
    selectInfoWasShown = false;
    boolean peptides = analyte.equals("PEPTIDES");

    this.msExperimentModel = new MSExperimentModel();
    List<AOpenbisSample> samplesForTable = new ArrayList<AOpenbisSample>();
    samplesForTable.addAll(analytes);
    if (pools != null)
      samplesForTable.addAll(getPoolingSamples(pools));
    baseAnalyteSampleTable.removeAllItems();
    tableIdToAnalyte = new HashMap<Integer, AOpenbisSample>();
    tableIdToFractions = new HashMap<Integer, Integer>();
    tableIdToCycles = new HashMap<Integer, Integer>();
    enzymeMap = new HashMap<Integer, List<String>>();
    int i = 0;
    for (AOpenbisSample s : samplesForTable) {
      i++;
      boolean complexRow = i == 1; // the first row contains a combobox with added button to copy
                                   // its selection to the whole column
      tableIdToAnalyte.put(i, s);
      tableIdToFractions.put(i, 0);
      tableIdToCycles.put(i, 0);

      List<Object> row = new ArrayList<Object>();

      Label sample =
          new Label(s.getQ_SECONDARY_NAME() + "<br>" + s.getQ_EXTERNALDB_ID(), Label.CONTENT_XHTML);

      row.add(sample);
      TextField fractionNumberField = generateTableTextInput("50px");
      fractionNumberField.setEnabled(fractionationSelection.isVisible());
      row.add(fractionNumberField);
      TextField cycleNumberField = generateTableTextInput("50px");
      cycleNumberField.setEnabled(enrichmentSelection.isVisible());
      row.add(cycleNumberField);

      List<String> processOptions = new ArrayList<String>(Arrays.asList("None", "Measure"));
      if (analyte.equals("PROTEINS")) {
        processOptions.add("Digest");
        processOptions.add("Both");
      }
      ComboBox processBox = generateTableBox(processOptions, "95px");

      processBox.setNullSelectionAllowed(false);
      processBox.select("None");
      if (complexRow)
        row.add(createComplexCellComponent(baseAnalyteSampleTable, processBox, "Process", i));
      else
        row.add(processBox);

      List<String> enzymes = vocabs.getEnzymes();
      Collections.sort(enzymes);
      ComboBox enzymeBox = generateTableBox(enzymes, "105px");
      enzymeBox.removeAllItems();
      enzymeBox.addItem("[Multiple]");
      enzymeBox.addItems(enzymes);
      enzymeBox.setEnabled(false);
      enzymeBox.setFilteringMode(FilteringMode.CONTAINS);
      if (complexRow)
        row.add(createComplexCellComponent(baseAnalyteSampleTable, enzymeBox, "Enzyme", i));
      else
        row.add(enzymeBox);
      final int rowNum = i;
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

      List<String> chromTypes = new ArrayList<String>(vocabs.getChromTypesMap().keySet());
      Collections.sort(chromTypes);
      ComboBox chrTypeBox = generateTableBox(chromTypes, "95px");
      chrTypeBox.setEnabled(peptides);
      chrTypeBox.setFilteringMode(FilteringMode.CONTAINS);
      if (complexRow)
        row.add(createComplexCellComponent(baseAnalyteSampleTable, chrTypeBox, "Chr. Type", i));
      else
        row.add(chrTypeBox);

      ComboBox deviceBox = generateTableBox(devices, "100px");
      deviceBox.setEnabled(peptides);
      deviceBox.setFilteringMode(FilteringMode.CONTAINS);
      deviceBox.addValueChangeListener(new ValueChangeListener() {

        @Override
        public void valueChange(ValueChangeEvent event) {

          String device = "";
          if (deviceBox.getValue() != null)
            device = (String) deviceBox.getValue();
          filterLCMSBox(rowNum, device);
        }
      });
      if (complexRow)
        row.add(createComplexCellComponent(baseAnalyteSampleTable, deviceBox, "MS Device", i));
      else
        row.add(deviceBox);

      ComboBox lcmsMethodBox = generateTableBox(lcmsMethods, "115px");
      lcmsMethodBox.setEnabled(peptides);
      lcmsMethodBox.setFilteringMode(FilteringMode.CONTAINS);
      if (complexRow)
        row.add(
            createComplexCellComponent(baseAnalyteSampleTable, lcmsMethodBox, "LCMS Method", i));
      else
        row.add(lcmsMethodBox);

      TextField lcmsSpecialField = generateTableTextInput("165px");
      lcmsSpecialField.setEnabled(false);
      row.add(lcmsSpecialField);

      lcmsMethodBox.addValueChangeListener(new ValueChangeListener() {

        @Override
        public void valueChange(ValueChangeEvent event) {
          String val = (String) lcmsMethodBox.getValue();
          boolean special = "SPECIAL_METHOD".equals(val); // not for null check this way you have to
          lcmsSpecialField.setEnabled(special);
          if (!special)
            lcmsSpecialField.setValue("");
        }
      });

      baseAnalyteSampleTable.addItem(row.toArray(new Object[row.size()]), i);

      fractionNumberField.setValue("0");
      final int item = i;
      fractionNumberField.addValueChangeListener(new ValueChangeListener() {

        @Override
        public void valueChange(ValueChangeEvent event) {
          String value = fractionNumberField.getValue();
          boolean fractionation = StringUtils.isNumeric(value) && Integer.parseInt(value) >= 0;
          if (fractionation) {
            tableIdToFractions.put(item, Integer.parseInt(value));
            msFractionationTable.setAnalyteSamples(samplesForTable, tableIdToFractions,
                analyte.equals("PEPTIDES"));
          }
        }
      });
      cycleNumberField.setValue("0");
      cycleNumberField.addValueChangeListener(new ValueChangeListener() {

        @Override
        public void valueChange(ValueChangeEvent event) {
          String value = cycleNumberField.getValue();
          boolean enrichment = StringUtils.isNumeric(value) && Integer.parseInt(value) >= 0;
          if (enrichment) {
            tableIdToCycles.put(item, Integer.parseInt(value));
            msEnrichmentTable.setAnalyteSamples(samplesForTable, tableIdToCycles,
                analyte.equals("PEPTIDES"));
          }
        }
      });

      processBox.addValueChangeListener(new ValueChangeListener() {

        @Override
        public void valueChange(ValueChangeEvent event) {
          String value = (String) processBox.getValue();
          boolean enableEnzyme = value.equals("Digest") || value.equals("Both");
          boolean enableMS = value.equals("Measure") || value.equals("Both");
          parseBoxRow(baseAnalyteSampleTable, item, "Enzyme").setEnabled(enableEnzyme);
          parseBoxRow(baseAnalyteSampleTable, item, "Chr. Type").setEnabled(enableMS);
          parseBoxRow(baseAnalyteSampleTable, item, "LCMS Method").setEnabled(enableMS);
          parseBoxRow(baseAnalyteSampleTable, item, "MS Device").setEnabled(enableMS);
        }
      });
    }
    baseAnalyteSampleTable.setPageLength(samplesForTable.size());
    msFractionationTable.setAnalyteSamples(samplesForTable, tableIdToFractions,
        analyte.equals("PEPTIDES"));
    msEnrichmentTable.setAnalyteSamples(samplesForTable, tableIdToCycles,
        analyte.equals("PEPTIDES"));
  }

  protected void createEnzymeSelectionWindow(int row) {
    Window subWindow = new Window(" Enzyme selection");
    subWindow.setWidth("400px");

    VerticalLayout layout = new VerticalLayout();
    layout.setSpacing(true);
    layout.setMargin(true);
    EnzymePanel pan = new EnzymePanel(vocabs.getEnzymes());
    Button ok = new Button("Okay.");
    ok.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        List<String> enzymes = pan.getEnzymes();
        ComboBox b = parseBoxRow(baseAnalyteSampleTable, row, "Enzyme");
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

  private Object createComplexCellComponent(Table t, ComboBox contentBox, String propertyName,
      final int rowID) {
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
        ComboBox b = parseBoxRow(t, rowID, propertyName);
        Object selection = b.getValue();
        pasteSelectionToColumn(t, propertyName, selection);
      }
    });
    return complexComponent;
  }

  private List<AOpenbisSample> getPoolingSamples(Map<String, List<AOpenbisSample>> pools) {
    List<AOpenbisSample> res = new ArrayList<AOpenbisSample>();
    if (pools.size() > 0) {
      for (String secName : pools.keySet()) {
        List<AOpenbisSample> parents = new ArrayList<AOpenbisSample>();
        for (AOpenbisSample s : pools.get(secName)) {
          parents.add(s);
        }
        res.add(new OpenbisTestSample(-1, parents, this.analyte, secName, "", // TODO ext db id
            new ArrayList<life.qbic.xml.properties.Property>(), ""));
      }
    }
    return res;
  }

  private ComboBox parseBoxRow(Table table, Object rowID, String propertyName) {
    Item item = table.getItem(rowID);
    Property prop = item.getItemProperty(propertyName);
    if (prop == null)
      return new ComboBox();
    Object component = prop.getValue();
    if (component instanceof ComboBox)
      return (ComboBox) component;
    else {
      HorizontalLayout h = (HorizontalLayout) component;
      return (ComboBox) h.getComponent(0);
    }
  }

  private TextField parseTextRow(Table table, Object id, String propertyName) {
    Item item = table.getItem(id);
    Property prop = item.getItemProperty(propertyName);
    if (prop == null)
      return new TextField();
    TextField t = (TextField) prop.getValue();
    return t;
  }

  protected Label parseLabelRow(Table table, Object rowID, String propertyName) {
    Item item = table.getItem(rowID);
    Property prop = item.getItemProperty(propertyName);
    if (prop == null)
      return new Label();
    Object component = prop.getValue();
    if (component instanceof Label)
      return (Label) component;
    else {
      HorizontalLayout h = (HorizontalLayout) component;
      return (Label) h.getComponent(0);
    }
  }

  private void pasteSelectionToColumn(Table t, String propertyName, Object selection) {
    for (Object id : t.getItemIds()) {
      // should always be ID = 1
      ComboBox b = parseBoxRow(t, id, propertyName);
      if (selection != null && selection.equals("Custom") && propertyName.equals("Enzyme")) {
        Integer i = (int) id;
        enzymeMap.put(i, enzymeMap.get(1));
        b.addItem("Custom");
      }
      if (b.isEnabled())// check if this value should be set
        b.setValue(selection);
    }
  }

  private void filterLCMSBox(Object id, String msDevice) {
    ComboBox b = parseBoxRow(baseAnalyteSampleTable, id, "LCMS Method");
    Object val = b.getValue();
    b.removeAllItems();
    List<String> methods = new ArrayList<String>();
    if (msDevice == null || msDevice.isEmpty() || !msDevice.contains("PCT"))
      methods.addAll(lcmsMethods);
    else {
      msDevice = msDevice.replace(" ", "").toUpperCase();
      for (String m : lcmsMethods) {
        String devType = "notfound";
        try {
          devType = m.split("_")[1];
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        if (msDevice.contains(devType)) {
          methods.add(m);
        }
      }
    }
    b.addItems(methods);
    if (val != null && methods.contains(val))
      b.select(val);
  }

  private ComboBox generateTableBox(Collection<String> entries, String width) {
    ComboBox b = new ComboBox();
    b.addItems(entries);
    b.setWidth(width);
    b.setFilteringMode(FilteringMode.CONTAINS);
    b.setStyleName(Styles.boxTheme);
    return b;
  }

  @Override
  public String getCaption() {
    if (analyte.equals("PEPTIDES"))
      return "Peptide Options";
    else
      return "Protein Options";
  }

  @Override
  public Component getContent() {
    return main;
  }

  @Override
  public boolean onAdvance() {
    return isValid();
  }

  private boolean isValid() {
    Set<String> selected = (Set<String>) analyteOptions.getValue();
    if (selected.contains("Fractionation") && (fractionationSelection.getValue() == null
        || fractionationSelection.getValue().toString().isEmpty())) {
      Styles.notification("Please Select Fractionation",
          "Please select the type of fractionation you want to perform on the samples or deselect 'Fractionation'.",
          NotificationType.ERROR);
      return false;
    }
    if (selected.contains("Enrichment") && (enrichmentSelection.getValue() == null
        || enrichmentSelection.getValue().toString().isEmpty())) {
      Styles.notification("Please Select Enrichment",
          "Please select the type of enrichment you want to perform on the samples or deselect 'Enrichment'.",
          NotificationType.ERROR);
      return false;
    }
    for (Object i : washRuns.getItemIds()) {
      SampleSelectComponent ssc = (SampleSelectComponent) washRuns.getItem(i)
          .getItemProperty("Sample Selection").getValue();
      if (!ssc.isAttachedToSample()) {
        Styles.notification("Please choose sample(s)", "Please select a sample for every wash run.",
            NotificationType.ERROR);
        return false;
      }
    }
    if (analyte.equals("PROTEINS") && needsDigestion) {
      for (Object i : baseAnalyteSampleTable.getItemIds()) {
        ComboBox selection = parseBoxRow(baseAnalyteSampleTable, i, "Process");
        String option = selection.getValue().toString();
        if (option.equals("Both") || option.equals("Digest")) {
          return true;
        }
      }
      if (!msFractionationTable.hasDigestions() && !msEnrichmentTable.hasDigestions()) {
        Styles.notification("Please Select Digestion",
            "Please add at least one process of protein digestion or deselect peptide measurement in a previous step.",
            NotificationType.ERROR);
        return false;
      }
    }
    return true;

  }

  @Override
  public boolean onBack() {
    return true;
  }

  public void createPreliminaryExperiments() {
    String method = "unknown";// TODO this must not reach openbis
    if (fractionationSelection.getValue() != null)
      method = fractionationSelection.getValue().toString();
    this.msExperimentModel =
        msFractionationTable.getFractionsWithMSProperties(this.msExperimentModel, analyte, method);

    method = "unknown";// TODO this must not reach openbis
    if (enrichmentSelection.getValue() != null)
      method = enrichmentSelection.getValue().toString();
    this.msExperimentModel =
        msEnrichmentTable.getFractionsWithMSProperties(this.msExperimentModel, analyte, method);
    hasRun = true;
    this.results = getSamplesWithMSProperties();
  }

  public MSExperimentModel getSamplesWithMSProperties() {
    ExperimentModel baseAnalytes = new ExperimentModel(-5);
    List<ExperimentModel> msExperiments = new ArrayList<ExperimentModel>();
    List<ExperimentModel> peptides = new ArrayList<ExperimentModel>();

    if (analyte.equals("PROTEINS")) {
      for (Object i : baseAnalyteSampleTable.getItemIds()) {

        AOpenbisSample baseAnalyte = tableIdToAnalyte.get(i);
        baseAnalytes.addSample(baseAnalyte);
      }
      msExperimentModel.setBaseAnalytes(baseAnalytes);
    }
    // collect ms samples as well as peptide samples with the same Properties
    Map<MSProperties, List<AOpenbisSample>> msSamplesPerProps =
        new HashMap<MSProperties, List<AOpenbisSample>>();
    Map<String, List<AOpenbisSample>> peptidesPerDigestion =
        new HashMap<String, List<AOpenbisSample>>();
    tableIdToMSRun = new HashMap<Object, AOpenbisSample>();
    for (Object i : baseAnalyteSampleTable.getItemIds()) {
      // String item = Integer.toString((int) i); TODO needed for exp id?
      AOpenbisSample parent = tableIdToAnalyte.get(i);

      ComboBox selection = parseBoxRow(baseAnalyteSampleTable, i, "Process");
      String option = selection.getValue().toString();
      Property x;
      if (option.equals("Both") || option.equals("Measure")) {

        // new ms sample from existing proteins (no fractions/enrichments) or peptides
        OpenbisMSSample msSample =
            new OpenbisMSSample(1, new ArrayList<AOpenbisSample>(Arrays.asList(parent)),
                parent.getQ_SECONDARY_NAME() + " run", parent.getQ_EXTERNALDB_ID() + " run",
                new ArrayList<life.qbic.xml.properties.Property>(), "");
        MSProperties props = getMSPropertiesFromSampleRow(baseAnalyteSampleTable, i);
        if (msSamplesPerProps.containsKey(props))
          msSamplesPerProps.get(props).add(msSample);
        else
          msSamplesPerProps.put(props, new ArrayList<AOpenbisSample>(Arrays.asList(msSample)));
        tableIdToMSRun.put(i, msSample);
      }
      if (option.equals("Both") || option.equals("Digest")) {
        OpenbisTestSample pepSample =
            new OpenbisTestSample(-1, new ArrayList<AOpenbisSample>(Arrays.asList(parent)),
                "PEPTIDES", parent.getQ_SECONDARY_NAME() + " digested", parent.getQ_EXTERNALDB_ID(),
                new ArrayList<life.qbic.xml.properties.Property>(), "");
        List<String> enzymes = getEnzymesFromSampleRow(i);
        String digestion = StringUtils.join(enzymes, ", ");
        if (peptidesPerDigestion.containsKey(digestion))
          peptidesPerDigestion.get(digestion).add(pepSample);
        else
          peptidesPerDigestion.put(digestion,
              new ArrayList<AOpenbisSample>(Arrays.asList(pepSample)));
      }
    }
    int n = 0;
    // one ms experiment per unique property set
    for (MSProperties props : msSamplesPerProps.keySet()) {
      n++;
      ExperimentModel msExp = new ExperimentModel(n, msSamplesPerProps.get(props));
      msExp.setProperties(props.getPropertyMap());
      msExperiments.add(msExp);
    }

    // one digestion experiment per unique enzyme set used
    for (String digestion : peptidesPerDigestion.keySet()) {
      ExperimentModel peptideExp = new ExperimentModel("", peptidesPerDigestion.get(digestion));
      peptideExp.addProperty("Q_ADDITIONAL_INFO", "Digestion: " + digestion);
      peptides.add(peptideExp);
    }

    // add wash runs - must be created after the other ms sample objects (parents)
    ExperimentModel washExp = new ExperimentModel("", new ArrayList<AOpenbisSample>());
    for (Object i : washRuns.getItemIds()) {
      Item item = washRuns.getItem(i);
      SampleSelectComponent ssc =
          (SampleSelectComponent) item.getItemProperty("Sample Selection").getValue();
      AOpenbisSample parent = getWashSampleConnection(ssc);
      String name = parseTextRow(washRuns, i, "Name").getValue();
      String labID = parseTextRow(washRuns, i, "Lab ID").getValue().replace("<br>", "");
      washExp.setProperties(getMSPropertiesFromSampleRow(washRuns, i).getPropertyMap());// TODO more
                                                                                        // than one
                                                                                        // exp
                                                                                        // needed?
      OpenbisMSSample washRun =
          new OpenbisMSSample((int) i, new ArrayList<AOpenbisSample>(Arrays.asList(parent)), name,
              labID, new ArrayList<life.qbic.xml.properties.Property>(), "");
      washExp.addSample(washRun);
    }
    if (washRuns.size() > 0) {
      msExperiments.add(washExp);
    }
    if (msExperiments.size() > 0)
      msExperimentModel.addMSRunStepExperiments(msExperiments);
    if (peptides.size() > 0)
      msExperimentModel.addDigestionExperiment(peptides);
    return msExperimentModel;
  }

  private AOpenbisSample getWashSampleConnection(SampleSelectComponent ssc) {
    Table t = ssc.getTable();
    Object id = ssc.getRowID();
    if (t.equals(msFractionationTable.getTable()))
      return msFractionationTable.getSampleFromRow(id);
    if (t.equals(msEnrichmentTable.getTable()))
      return msEnrichmentTable.getSampleFromRow(id);
    else
      return getSampleFromRow(id);
  }

  private AOpenbisSample getSampleFromRow(Object id) {
    AOpenbisSample s = tableIdToMSRun.get(id);
    return s;
  }

  private MSProperties getMSPropertiesFromSampleRow(Table t, Object i) {
    Object deviceBox = parseBoxRow(t, i, "MS Device").getValue();
    Object lcmsBox = parseBoxRow(t, i, "LCMS Method").getValue();
    Object chromBox = parseBoxRow(t, i, "Chr. Type").getValue();
    String special = parseTextRow(t, i, "Method Description").getValue();
    String device = null;
    String lcms = null;
    String chrom = null;
    if (deviceBox != null) {
      device = vocabs.getDeviceMap().get(deviceBox.toString());
    }
    if (lcmsBox != null) {
      lcms = lcmsBox.toString();
    }
    if (chromBox != null) {
      chrom = vocabs.getChromTypesMap().get(chromBox.toString());
    }
    return new MSProperties(lcms, device, special, chrom);
  }

  private List<String> getEnzymesFromSampleRow(Object i) {
    if (parseBoxRow(baseAnalyteSampleTable, i, "Enzyme").getValue() == null)
      return null;
    else {
      String entry = parseBoxRow(baseAnalyteSampleTable, i, "Enzyme").getValue().toString();
      if (entry.equals("Custom"))
        return enzymeMap.get(i);
      else
        return new ArrayList<String>(Arrays.asList(entry));
    }
  }

  public void setAnalyteSamplesAndExperiments(MSExperimentModel msExperimentModel) {

    List<AOpenbisSample> allSamples = new ArrayList<AOpenbisSample>();
    List<ExperimentModel> source = msExperimentModel.getLastStepAnalytes();
    if (analyte.equals("PEPTIDES"))
      source = msExperimentModel.getPeptideExperiments();
    for (ExperimentModel analytes : source) {
      allSamples.addAll(analytes.getSamples());
    }
    setAnalyteSamples(allSamples, null);// this resets the life.qbic.projectwizard.model, needed for
                                        // pooling
    this.msExperimentModel = new MSExperimentModel(msExperimentModel);
  }

  public void setNeedsDigestion(boolean required) {
    this.needsDigestion = required;
  }

  public MSExperimentModel getResults() {
    return results;
  }

  public void filterDictionariesByPrefix(String prefix, List<String> dontFilter) {
    devices = new ArrayList<String>();
    lcmsMethods = new ArrayList<String>();
    if (prefix.isEmpty()) {
      devices.addAll(vocabs.getDeviceMap().keySet());
    } else {
      for (String device : vocabs.getDeviceMap().keySet()) {
        if (device.contains("(" + prefix + ")") || dontFilter.contains(device))
          devices.add(device);
      }
    }
    for (String lcmsMethod : vocabs.getLcmsMethods()) {
      if (lcmsMethod.startsWith(prefix) || dontFilter.contains(lcmsMethod))
        lcmsMethods.add(lcmsMethod);
    }
    Collections.sort(devices);
    Collections.sort(lcmsMethods);
    msEnrichmentTable.filterDictionariesByPrefix(prefix, dontFilter);
    msFractionationTable.filterDictionariesByPrefix(prefix, dontFilter);
  }

}
