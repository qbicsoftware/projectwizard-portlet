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
package life.qbic.portlet.views;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;

import parser.XMLParser;
import properties.Property;
import logging.Log4j2Logger;
import main.ProjectwizardUI;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.PropertyType;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataTypeCode;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.portlet.control.Functions;
import life.qbic.portlet.io.DBVocabularies;
import life.qbic.portlet.uicomponents.Styles;
import life.qbic.portlet.uicomponents.UploadComponent;
import life.qbic.portlet.uicomponents.Styles.*;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.SelectedTabChangeEvent;
import com.vaadin.ui.TabSheet.SelectedTabChangeListener;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Upload.FinishedListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Upload.FinishedEvent;

// import au.com.bytecode.opencsv.CSVReader;

public class MetadataUploadView extends VerticalLayout {

  private OptionGroup typeOfData =
      new OptionGroup("Type of Metadata", new ArrayList<String>(Arrays.asList("Samples")));

  private TabSheet sheet;
  private UploadComponent upload;
  private Button send;

  private XMLParser xmlParser = new XMLParser();
  private IOpenBisClient openbis;
  private Map<String, Object> metadata;
  private List<String> customProperties = new ArrayList<String>(
      Arrays.asList("IGNORE (removes column)", "[Experimental Condition]", "[Other Property]"));
  private Map<String, String> propNameToCode;
  private Map<String, Map<String, String>> propToVocabulary;
  private Map<String, Map<String, String>> propToReverseVocabulary;
  private Set<String> allowedSpaces;

  private logging.Logger logger = new Log4j2Logger(MetadataUploadView.class);
  private List<Table> sampleTables;
  private Button reload;

  private Map<String, Sample> codesToSamples;
  private String barcodeColName;

  private List<String> collisions;
  private List<String> codesInTSV;

  private boolean overWriteAllowed = false;

  public MetadataUploadView(IOpenBisClient openbis, DBVocabularies vocabularies,
      boolean overWriteAllowed) {
    allowedSpaces = new HashSet<String>(vocabularies.getSpaces());
    this.overWriteAllowed = overWriteAllowed;
    sheet = new TabSheet();
    sampleTables = new ArrayList<Table>();

    Map<String, String> taxMap = vocabularies.getTaxMap();
    Map<String, String> tissueMap = vocabularies.getTissueMap();

    propToVocabulary = new HashMap<String, Map<String, String>>();
    propToVocabulary.put("Q_NCBI_ORGANISM", taxMap);
    propToVocabulary.put("Q_PRIMARY_TISSUE", tissueMap);

    Map<String, String> reverseTaxMap = new HashMap<String, String>();
    for (Map.Entry<String, String> entry : taxMap.entrySet()) {
      reverseTaxMap.put(entry.getValue(), entry.getKey());
    }
    Map<String, String> reverseTissueMap = new HashMap<String, String>();
    for (Map.Entry<String, String> entry : tissueMap.entrySet()) {
      reverseTissueMap.put(entry.getValue(), entry.getKey());
    }
    propToReverseVocabulary = new HashMap<String, Map<String, String>>();
    propToReverseVocabulary.put("Q_NCBI_ORGANISM", reverseTaxMap);
    propToReverseVocabulary.put("Q_PRIMARY_TISSUE", reverseTissueMap);

    this.openbis = openbis;
    setSpacing(true);
    setMargin(true);
    addComponent(typeOfData);
    upload = new UploadComponent("Upload Metadata (tab-separated)", "Upload",
        ProjectwizardUI.tmpFolder, "meta_", 200000);
    upload.setVisible(false);
    addComponent(upload);
    reload = new Button("Reset columns");
    reload.setVisible(false);
    addComponent(reload);
    reload.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        try {
          parseTSV(upload.getFile());
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });

    send = new Button("Send to Database");
    send.setEnabled(false);
    initListeners();
  }

  private void initListeners() {
    typeOfData.addValueChangeListener(new ValueChangeListener() {
      @Override
      public void valueChange(ValueChangeEvent event) {
        upload.setVisible(true);
      }
    });
    upload.addFinishedListener(new FinishedListener() {

      @Override
      public void uploadFinished(FinishedEvent event) {
        if (upload.wasSuccess())
          try {
            send.setVisible(parseTSV(upload.getFile()));
            reload.setVisible(true);
          } catch (IOException e) {
            e.printStackTrace();
          }
      }
    });
    send.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        try {
          ingestTable();
          Styles.notification("Done!", "Your metadata was sent to the Database.",
              NotificationType.SUCCESS);
          sheet.removeComponent(getActiveTable());
        } catch (Exception e) {
          e.printStackTrace();
          Styles.notification("Something went wrong!",
              "Sorry, your metadata could not be registered. Please contact a delevoper.",
              NotificationType.ERROR);
        }
      }
    });
  }

  public Table getActiveTable() {
    return (Table) sheet.getSelectedTab();
  }

  protected void ingestTable() throws IllegalArgumentException, JAXBException {
    Table sampleTable = getActiveTable();
    metadata = new HashMap<String, Object>();
    List<String> types = new ArrayList<String>();
    List<Property> conditions = new ArrayList<Property>();
    List<String> codes = new ArrayList<String>();
    for (Object col : sampleTable.getContainerPropertyIds()) {
      String attribute = getSelectedProperty(col);
      if (!attribute.equals("Properties -->")) {
        String unit = null;
        properties.PropertyType propType = null;
        if (attribute.startsWith("Condition: "))
          propType = properties.PropertyType.Factor;
        if (attribute.startsWith("Property: "))
          propType = properties.PropertyType.Property;
        if (propType != null) {
          attribute = attribute.replace("Condition: ", "").replace("Property: ", "");
          Property prop = null;
          if (attribute.contains("[") && attribute.contains("]")) {
            unit = parseUnit(attribute);
            attribute = attribute.replace(" [" + unit + "]", "");
            prop = new Property(attribute, "", properties.Unit.valueOf(unit), propType);
          } else
            prop = new Property(attribute, "", propType);
          conditions.add(prop);
          attribute = prop.toString();
        } else {
          if (propNameToCode.containsKey(attribute))
            attribute = propNameToCode.get(attribute);
          types.add(attribute);
        }
        Map<String, String> curTypeMap = new HashMap<String, String>();
        for (Object row : sampleTable.getItemIds()) {
          int id = (int) row;
          if (id != -1) {
            String bc = getBarcodeInRow(id);
            String val = parseLabelCell(id, col);
            // if (propType != null) {
            // val = propType + ": " + val;
            // if (unit != null)
            // val = val + " [" + unit + "]";
            // }
            if (!codes.contains(bc))
              codes.add(bc);
            if (propToVocabulary.containsKey(attribute))
              val = propToVocabulary.get(attribute).get(val);
            curTypeMap.put(bc, val);
          }
        }
        metadata.put(attribute, curTypeMap);
      }
    }
    Map<String, String> xmlPropertyMap = new HashMap<String, String>();
    for (String code : codes) {
      List<Property> newFactors = new ArrayList<Property>();
      String existingXML = codesToSamples.get(code).getProperties().get("Q_PROPERTIES");
      List<Property> oldFactors = xmlParser.getExpFactorsFromXML(existingXML);
      for (Property condition : conditions) {
        for (Property f : oldFactors) {
          Property test = null;
          if (f.hasUnit())
            test = new Property(f.getLabel(), "", f.getUnit(), f.getType());
          else
            test = new Property(f.getLabel(), "", f.getType());
          if (!conditions.contains(test)) {
            newFactors.add(f);
          }
        }
        Map<String, String> condMap = (HashMap<String, String>) metadata.get(condition.toString());
        Property prop = parseProperty(condition, condMap.get(code));
        newFactors.add(prop);
      }
      if (!newFactors.isEmpty())
        // xmlPropertyMap.put(code, xmlParser.toString(xmlParser.createXMLFromFactors(newFactors)));
        xmlPropertyMap.put(code, xmlParser.toString(xmlParser.createXMLFromProperties(newFactors)));
    }
    for (Property condition : conditions) {
      metadata.remove(condition.getLabel());
    }
    if (!xmlPropertyMap.isEmpty()) {
      types.add("Q_PROPERTIES");
      metadata.put("Q_PROPERTIES", xmlPropertyMap);
    }
    metadata.put("identifiers", codes);
    metadata.put("types", types);
    logger.info("Ingesting metadata");
    openbis.ingest("DSS1", "update-sample-metadata", metadata);
  }

  private Property parseProperty(Property propWithOutVal, String value) {
    if (propWithOutVal.hasUnit())
      return new Property(propWithOutVal.getLabel(), value, propWithOutVal.getUnit(),
          propWithOutVal.getType());
    else
      return new Property(propWithOutVal.getLabel(), value, propWithOutVal.getType());
  }

  protected boolean parseTSV(File file) throws IOException {
    for (Table t : sampleTables) {
      t.removeAllItems();
      sheet.removeComponent(t);
    }
    removeComponent(sheet);
    addComponent(sheet);
    sheet.addSelectedTabChangeListener(new SelectedTabChangeListener() {

      @Override
      public void selectedTabChange(SelectedTabChangeEvent event) {
        reactToTableChange();
      }
    });

    sampleTables.clear();
    CSVParser parser =
        new CSVParserBuilder().withIgnoreQuotations(true).withSeparator('\t').build();
    CSVReader reader = new CSVReaderBuilder(new FileReader(file)).withCSVParser(parser).build();

    String error = "";
    ArrayList<String[]> data = new ArrayList<String[]>();
    String[] nextLine;
    int rowID = 0;
    while ((nextLine = reader.readNext()) != null) {
      rowID++;
      if (data.isEmpty() || nextLine.length == data.get(0).length) {
        data.add(nextLine);
      } else {
        error = "Wrong number of columns in row " + rowID
            + " Please make sure every row fits the header row.";
        Styles.notification("Parsing Error", error, NotificationType.ERROR);
        reader.close();
        return false;
      }
    }
    reader.close();
    String[] header = data.get(0);
    data.remove(0);
    int barcodeCol = -1;
    String projectCode = "";
    for (int j = 0; j < header.length; j++) {
      String word = data.get(0)[j];
      if ((Functions.isQbicBarcode(word) || word.contains("ENTITY-")) && barcodeCol == -1) {
        barcodeCol = j;
        barcodeColName = header[barcodeCol];
        projectCode = word.substring(0, 5);
      }
    }
    if (barcodeCol == -1) {
      error =
          "No barcode column found. Make sure one column contains QBiC Barcodes to map your information to existing samples!";
      Styles.notification("File Incomplete", error, NotificationType.ERROR);
      return false;
    }
    if (barcodeCol != 0) {
      header[barcodeCol] = header[0];
      header[0] = barcodeColName;
      for (String[] d : data) {
        String bc = d[barcodeCol];
        d[barcodeCol] = d[0];
        d[0] = bc;
      }
      barcodeCol = 0;
    }
    List<Sample> projectSamples =
        openbis.getSamplesWithParentsAndChildrenOfProjectBySearchService(projectCode);
    codesToSamples = new HashMap<String, Sample>();
    Map<String, List<String>> sampleTypeToAttributes = new HashMap<String, List<String>>();
    Map<String, DataTypeCode> propertyToType = new HashMap<String, DataTypeCode>();
    for (Sample s : projectSamples) {
      String space = s.getSpaceCode();
      // don't add samples the user should not be able to see
      if (allowedSpaces.contains(space))
        codesToSamples.put(s.getCode(), s);
    }
    propNameToCode = new HashMap<String, String>();
    codesInTSV = new ArrayList<String>();
    for (int i = 0; i < data.size(); i++) {
      String bc = data.get(i)[barcodeCol];
      if (!codesToSamples.containsKey(bc)) {
        // if samples don't exist or user doesn't have rights to see them, show error
        Styles.notification("Sample not found!",
            "Sample with code " + bc + " was not found in the Database.", NotificationType.ERROR);
        return false;
      }
      String type = codesToSamples.get(bc).getSampleTypeCode();
      if (!sampleTypeToAttributes.containsKey(type)) {
        List<PropertyType> props =
            openbis.listPropertiesForType(openbis.getSampleTypeByString(type));
        List<String> propertyNames = new ArrayList<String>();
        for (PropertyType p : props) {
          String propName = p.getLabel();
          String propCode = p.getCode();
          propNameToCode.put(propName, propCode);
          DataTypeCode dataType = p.getDataType();
          switch (dataType) {
            case CONTROLLEDVOCABULARY:// TODO properties without mapping?
              if (propToVocabulary.containsKey(propCode)) {
                propertyToType.put(propName, dataType);
                propertyNames.add(propName);
              }
              break;
            case MATERIAL:
              break;
            case TIMESTAMP:
              break;
            case XML:
              break;
            default:
              propertyToType.put(propName, dataType);
              propertyNames.add(propName);
          }
        }
        sampleTypeToAttributes.put(type, propertyNames);
      }
      codesInTSV.add(bc);
    }

    for (String type : sampleTypeToAttributes.keySet()) {
      Set<String> options = new HashSet<String>();
      options.addAll(customProperties);
      options.addAll(sampleTypeToAttributes.get(type));
      // options.removeAll(hiddenProperties);
      Table sampleTable = new Table(type + " Samples");
      sampleTable.setWidth("100%");
      sampleTable.setStyleName(Styles.tableTheme);
      sampleTable.addContainerProperty(header[barcodeCol], String.class, null);
      for (int i = 0; i < header.length; i++) {
        if (i != barcodeCol) {
          sampleTable.addContainerProperty(header[i], Component.class, null);
        }
      }
      List<Object> row = new ArrayList<Object>();
      row.add("Properties -->");
      for (int i = 1; i < header.length; i++) {
        // if (i != barcodeCol) {
        String headline = header[i];
        ComboBox attributeOptions = new ComboBox("", options);
        attributeOptions.setStyleName(Styles.boxTheme);
        attributeOptions.setImmediate(true);
        attributeOptions.setInputPrompt("<Select Attribute>");
        attributeOptions.setWidth("100%");
        attributeOptions.setFilteringMode(FilteringMode.CONTAINS);
        attributeOptions.setNullSelectionAllowed(false);
        attributeOptions.addValueChangeListener(new ValueChangeListener() {

          @Override
          public void valueChange(ValueChangeEvent event) {
            List<Object> toRemove = new ArrayList<Object>();
            for (Object item : attributeOptions.getItemIds()) {
              String val = item.toString();
              if (val.startsWith("Condition: ") || val.startsWith("Property: "))
                if (!attributeOptions.getValue().equals(item))
                  toRemove.add(item);
            }
            for (Object item : toRemove) {
              attributeOptions.removeItem(item);
            }
            if (attributeOptions.getValue() != null) {
              String selectedProperty = (String) attributeOptions.getValue();
              if (selectedProperty.equals("[Experimental Condition]")
                  || selectedProperty.equals("[Other Property]"))
                createConditionWindow(attributeOptions);
              else {
                if (selectedProperty.equals("IGNORE (removes column)")) {
                  sampleTable.removeContainerProperty(headline);
                  reactToTableChange();
                } else {
                  DataTypeCode dType = propertyToType.get(selectedProperty);
                  if (dType != null) {
                    switch (dType) {
                      case CONTROLLEDVOCABULARY:
                        createVocabularySelectWindow(attributeOptions, selectedProperty,
                            collectLabelsInCol(headline));// TODO ?
                        break;
                      case REAL:
                      case INTEGER:
                        checkForNumberConsistency(headline, dType);
                        reactToTableChange();
                        break;
                      default:
                        reactToTableChange();
                        break;
                    }
                  } else {
                    reactToTableChange();
                  }
                }
              }
            }
          }
        });
        row.add(attributeOptions);
        // } else {
        // row.add("Properties -->");
        // }
      }
      sampleTable.addItem(row.toArray(), -1);
      for (int i = 0; i < codesInTSV.size(); i++) {
        String thisType = codesToSamples.get(codesInTSV.get(i)).getSampleTypeCode();
        if (thisType.equals(type)) {
          row = new ArrayList<Object>();
          row.add(codesInTSV.get(i));
          for (int j = 0; j < header.length; j++) {
            if (j != barcodeCol) {
              row.add(new Label(data.get(i)[j]));
            }
          }
          sampleTable.addItem(row.toArray(), i);
        }
      }
      sheet.addTab(sampleTable);
      sampleTables.add(sampleTable);
      sampleTable.setPageLength(Math.min(20, sampleTable.size()));
      styleTable(sampleTable);
      reactToTableChange();
    }
    addComponent(send);
    return true;
  }

  protected List<Label> collectLabelsInCol(String headline) {
    Table sampleTable = getActiveTable();
    List<Label> res = new ArrayList<Label>();
    for (Object itemID : sampleTable.getItemIds()) {
      int id = (int) itemID;
      if (id > -1) {
        Item item = sampleTable.getItem(id);
        Label l = (Label) item.getItemProperty(headline).getValue();
        res.add(l);
      }
    }
    return res;
  }

  protected void checkForNumberConsistency(String headline, DataTypeCode dType) {
    boolean consistent = true;
    boolean needsDelimiterChange = false;
    String moreInfo = "Not a number.";
    String barcode = "";
    for (Object item : getActiveTable().getItemIds()) {
      int id = (int) item;
      if (id != -1) {
        String val = parseLabelCell(id, headline);
        if (!val.isEmpty()) {
          if (dType.equals(DataTypeCode.INTEGER)) {
            try {
              Integer.parseInt(val);
            } catch (NumberFormatException e) {
              consistent = false;
            }
          }
          if (dType.equals(DataTypeCode.REAL)) {
            // try normal parse
            try {
              Double.parseDouble(val);
            } catch (NumberFormatException e) {
              // normal parse unsuccessful, check for different delimiter
              NumberFormat format = NumberFormat.getInstance(Locale.GERMANY);
              try {
                format.parse(val);
                // worked, needs different delimiter
                needsDelimiterChange = true;
              } catch (ParseException e1) {
                // didn't work, not a double value
                barcode = getBarcodeInRow(id);
                consistent = false;
              }
            }
          }
        }
      }
    }
    if (consistent) {
      if (needsDelimiterChange)
        createDelimiterChangeDialogue(headline);
    } else {
      createNotRightTypeDialogue(headline, moreInfo, barcode);
    }
  }

  private void createNotRightTypeDialogue(String headline, String moreInfo, String barcode) {
    Window subWindow = new Window(" Wrong data type!");
    subWindow.setWidth("400px");

    VerticalLayout layout = new VerticalLayout();
    layout.setSpacing(true);
    layout.setMargin(true);
    Label preInfo = new Label("Data of barcode " + barcode
        + " in this column doesn't fit the attribute type. " + moreInfo);
    layout.addComponent(preInfo);
    Button ok = new Button("Ignore Column.");
    Button no = new Button("Select different attribute.");
    ok.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        getActiveTable().removeContainerProperty(headline);
        subWindow.close();
      }
    });
    no.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        resetAttribute(headline);
        subWindow.close();
      }
    });
    layout.addComponent(ok);
    layout.addComponent(no);

    subWindow.setContent(layout);
    // Center it in the browser window
    subWindow.center();
    subWindow.setModal(true);
    subWindow.setIcon(FontAwesome.BOLT);
    subWindow.setResizable(false);
    ProjectwizardUI ui = (ProjectwizardUI) UI.getCurrent();
    ui.addWindow(subWindow);
  }

  protected void resetAttribute(String headline) {
    Item item = getActiveTable().getItem(-1);
    Object cell = item.getItemProperty(headline).getValue();
    ComboBox c = ((ComboBox) cell);
    c.setNullSelectionAllowed(true);
    c.select(null);
    c.setNullSelectionAllowed(false);
  }
  //
  // public static void main(String[] args) {
  // int infoMaxLength = 21;
  // String res = "ID01-spleen-150114-L243_TUE39";
  // System.out.println(res.substring(0, Math.min(res.length(), infoMaxLength)));
  //
  // RegexpValidator factorLabelValidator = new RegexpValidator("([a-z]+_?[a-z]*)+([a-z]|[0-9]*)",
  // "Name must start with a lower case letter and contain only lower case letters, numbers or
  // underscores ('_'). Underscores must be followed by a letter.");
  // List<String> tests = new ArrayList<String>(Arrays.asList("a", "a_1", "abc_abc_de1",
  // "isotope12c_or13c", "abc", "abc1", "abc_1", "abc_asd1_x", "abc1_abc2"));
  // for (String x : tests)
  // System.out.println(x + ": " + factorLabelValidator.isValid(x));
  // }

  private void createDelimiterChangeDialogue(String headline) {
    Window subWindow = new Window(" Unexpected number format");
    subWindow.setWidth("400px");

    VerticalLayout layout = new VerticalLayout();
    layout.setSpacing(true);
    layout.setMargin(true);
    Label preInfo = new Label("The decimal delimiter of this type needs to be replaced with '.'.");
    layout.addComponent(preInfo);
    Button ok = new Button("Change numbers in this column.");
    Button no = new Button("Select different attribute.");
    ok.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        changeDelimiterInCol(headline);
        subWindow.close();
      }
    });
    no.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        resetAttribute(headline);
        subWindow.close();
      }
    });
    layout.addComponent(ok);
    layout.addComponent(no);

    subWindow.setContent(layout);
    // Center it in the browser window
    subWindow.center();
    subWindow.setModal(true);
    subWindow.setIcon(FontAwesome.QUESTION);
    subWindow.setResizable(false);
    ProjectwizardUI ui = (ProjectwizardUI) UI.getCurrent();
    ui.addWindow(subWindow);
  }

  protected void changeDelimiterInCol(String headline) {
    for (Object item : getActiveTable().getItemIds()) {
      int id = (int) item;
      if (id != -1) {
        String val = parseLabelCell(id, headline);
        writeLabelCell(id, headline, val.replace(",", "."));
      }
    }
  }

  private void reactToTableChange() {
    collisions = new ArrayList<String>();
    Table t = getActiveTable();
    t.setCellStyleGenerator(t.getCellStyleGenerator());
    showStatus();
  }

  private void styleTable(Table table) {
    // Set cell style generator
    table.setCellStyleGenerator(new Table.CellStyleGenerator() {

      @Override
      public String getStyle(Table source, Object itemId, Object propertyId) {
        String type = null;
        if (propertyId != null) {
          // barcode col
          if (propertyId.equals(barcodeColName))
            return "blue-hue1";
          // combobox col
          type = getSelectedProperty(propertyId);
        }
        // not set yet
        if (type == null)
          return "red-hue";
        else {
          // check for data in openbis that would be overwritten
          String collision = getCollisionOrNull(propertyId, itemId);
          if (collision != null) {
            collisions.add(collision);
            return "yellow-hue";
          } else
            return "blue-hue1";
        }
      }
    });
  }

  // make findable for table styler
  private void fillCollisionsList() {
    Table sampleTable = getActiveTable();
    for (Object propertyId : sampleTable.getContainerPropertyIds()) {
      for (Object itemId : sampleTable.getItemIds()) {
        String type = getSelectedProperty(propertyId);
        // type set
        if (type != null && !propertyId.equals(barcodeColName)) {
          // check for data in openbis that would be overwritten
          String collision = getCollisionOrNull(propertyId, itemId);
          if (collision != null) {
            collisions.add(collision);
          }
        }
      }
    }
  }

  private void showStatus() {
    boolean ready = true;
    for (Object colName : getActiveTable().getContainerPropertyIds()) {
      String selected = getSelectedProperty(colName);
      ready &= selected != null && !selected.isEmpty();
    }
    if (ready) {
      fillCollisionsList();
      if (collisions.size() > 0) {
        Window subWindow = new Window(" Collisions found!");
        subWindow.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        Label preInfo = new Label("The following entries exist and would need to be overwritten:");
        layout.addComponent(preInfo);
        TextArea tf = new TextArea();
        tf.setWidth("350px");
        tf.setValue(StringUtils.join(collisions, ""));
        tf.setStyleName(Styles.areaTheme);
        layout.addComponent(tf);
        String overwriteInfo =
            "In order to keep your data safe, you are not allowed to overwrite existing information by default. "
                + "You can either remove the columns in question (choose 'ignore column') or contact QBiC.";
        if (overWriteAllowed)
          overwriteInfo = "You can either remove the columns in question (choose 'ignore column') "
              + "before sending it to the Database or overwrite the metadata.";
        Label info = new Label(overwriteInfo);
        Button ok = new Button("Got it!");
        ok.addClickListener(new ClickListener() {

          @Override
          public void buttonClick(ClickEvent event) {
            subWindow.close();
          }
        });
        layout.addComponent(info);
        layout.addComponent(ok);

        subWindow.setContent(layout);
        // Center it in the browser window
        subWindow.center();
        subWindow.setModal(true);
        subWindow.setIcon(FontAwesome.BOLT);
        subWindow.setResizable(false);
        ProjectwizardUI ui = (ProjectwizardUI) UI.getCurrent();
        ui.addWindow(subWindow);
      } else {
        Styles.notification("No collisions found!",
            "You can update the metadata in our database without overwriting something. To do so press 'Send to Database'",
            NotificationType.DEFAULT);
      }
      send.setEnabled(collisions.isEmpty() || overWriteAllowed);
    } else
      send.setEnabled(false);
  }

  protected void createVocabularySelectWindow(ComboBox selected, String propName,
      List<Label> entries) {
    Window subWindow = new Window(" " + propName);
    subWindow.setWidth("300px");

    VerticalLayout layout = new VerticalLayout();
    layout.setMargin(true);
    layout.setSpacing(true);

    // create combobox per unique value for this column to find a mapping to the source vocabulary
    Map<String, String> entriesToVocabValues = new HashMap<String, String>();
    Set<String> uniqueEntries = new HashSet<String>();
    // keep these old values in case user chooses different property afterwards
    List<String> oldEntries = new ArrayList<String>();
    ValueChangeListener resetSelectionListener = new ValueChangeListener() {
      @Override
      public void valueChange(ValueChangeEvent event) {
        // reset labels to what they were before
        for (int i = 0; i < entries.size(); i++) {
          entries.get(i).setValue(oldEntries.get(i));
        }
        // remove reset listener, it won't be needed until a vocabulary field is selected again
        selected.removeValueChangeListener(this);
      }
    };
    selected.addValueChangeListener(resetSelectionListener);

    List<ComboBox> boxes = new ArrayList<ComboBox>();
    Set<String> vocabOptions = propToVocabulary.get(propNameToCode.get(propName)).keySet();
    for (Label l : entries) {
      String val = l.getValue();
      oldEntries.add(val);
      if (!uniqueEntries.contains(val)) {
        ComboBox b = new ComboBox(val);
        b.addItems(vocabOptions);
        b.setNullSelectionAllowed(false);
        b.setStyleName(Styles.boxTheme);
        b.setFilteringMode(FilteringMode.CONTAINS);
        layout.addComponent(b);
        boxes.add(b);
        uniqueEntries.add(val);
        val = StringUtils.capitalize(val);
        if (vocabOptions.contains(val)) {
          b.setValue(val);
          b.setEnabled(false);
        }
      }
    }
    Button send = new Button("Ok");
    send.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        boolean valid = true;
        for (ComboBox b : boxes) {
          if (b.getValue() != null) {
            String newVal = b.getValue().toString();
            entriesToVocabValues.put(b.getCaption(), newVal);
          } else
            valid = false;
        }
        if (valid) {
          for (Label l : entries) {
            l.setValue(entriesToVocabValues.get(l.getValue()));
          }
          subWindow.close();

          // check for collisions now that values have changed
          reactToTableChange();
        } else {
          String error = "Please select a value for each entry.";
          Styles.notification("Missing Input", error, NotificationType.DEFAULT);
        }
      }
    });
    layout.addComponent(send);

    subWindow.setContent(layout);
    // Center it in the browser window
    subWindow.center();
    subWindow.setModal(true);
    subWindow.setIcon(FontAwesome.FLASK);
    subWindow.setResizable(false);

    ProjectwizardUI ui = (ProjectwizardUI) UI.getCurrent();
    ui.addWindow(subWindow);
  }

  protected void createConditionWindow(ComboBox selectionBox) {
    String val = (String) selectionBox.getValue();

    // val.equals("[Experimental Condition]")
    String header = " Experimental Condition Name";
    String prefix = "Condition";
    Resource icon = FontAwesome.FLASK;

    if (val.equals("[Other Property]")) {
      header = " Property Name";
      prefix = "Property";
      icon = FontAwesome.FILE_TEXT;
    }
    final String category = prefix;

    Window subWindow = new Window(header);
    subWindow.setWidth("300px");

    VerticalLayout layout = new VerticalLayout();
    layout.setSpacing(true);
    layout.setMargin(true);
    TextField label = new TextField();
    label.setRequired(true);
    label.setStyleName(Styles.fieldTheme);
    RegexpValidator factorLabelValidator = new RegexpValidator("([a-z]+_?[a-z]*)+([a-z]|[0-9]*)",
        "Name must start with a lower case letter and contain only lower case letter words, which can be connected by underscores ('_'). It can end with one or more numbers.");
    label.addValidator(factorLabelValidator);
    label.setValidationVisible(true);
    label.setImmediate(true);

    ComboBox unitSelect = new ComboBox("Unit");
    unitSelect.setNullSelectionAllowed(false);
    unitSelect.addItems(properties.Unit.values());
    String nullItem = "[None]";
    unitSelect.addItem(nullItem);
    unitSelect.select(nullItem);
    unitSelect.setStyleName(Styles.boxTheme);
    unitSelect.setImmediate(true);

    Button send = new Button("Ok");
    send.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        if (label.isValid()) {
          String unit = "";
          if (!unitSelect.getValue().equals(nullItem))
            unit = " [" + unitSelect.getValue() + "]";
          String name = category + ": " + label.getValue() + unit;
          selectionBox.addItem(name);
          selectionBox.select(name);
          subWindow.close();
        } else {
          String error = "Please input a name for this " + category + ".";
          if (!label.isEmpty())
            error = factorLabelValidator.getErrorMessage();
          Styles.notification("Missing Input", error, NotificationType.DEFAULT);
        }
      }
    });
    layout.addComponent(label);
    layout.addComponent(unitSelect);
    layout.addComponent(send);

    subWindow.setContent(layout);
    // Center it in the browser window
    subWindow.center();
    subWindow.setModal(true);
    subWindow.setIcon(icon);
    subWindow.setResizable(false);
    ProjectwizardUI ui = (ProjectwizardUI) UI.getCurrent();
    ui.addWindow(subWindow);
  }

  private String parseLabelCell(int id, Object propertyId) {
    Item item = getActiveTable().getItem(id);
    Label l = (Label) item.getItemProperty(propertyId).getValue();
    return l.getValue();
  }

  private void writeLabelCell(int id, Object propertyId, String text) {
    Item item = getActiveTable().getItem(id);
    Label l = (Label) item.getItemProperty(propertyId).getValue();
    l.setValue(text);
  }

  private String getBarcodeInRow(int id) {
    Item item = getActiveTable().getItem(id);
    String bc = (String) item.getItemProperty(barcodeColName).getValue();
    return bc;
  }

  private String parseUnit(String label) {
    if (!label.contains("]") && !label.contains("["))
      return null;
    label = label.substring(label.indexOf("[") + 1);
    label = label.substring(0, label.indexOf("]"));
    return label;
  }

  protected String getCollisionOrNull(Object propertyId, Object itemId) {
    String typeName = getSelectedProperty(propertyId);
    String typeCode = propNameToCode.get(typeName);
    String res = null;

    int id = (int) itemId;
    if (id != -1) {
      String val = parseLabelCell(id, propertyId);
      String barcode = getBarcodeInRow(id);
      String openbisVal = "";
      Map<String, String> props = codesToSamples.get(barcode).getProperties();
      properties.PropertyType propType = null;
      if (typeName.startsWith("Condition: "))
        propType = properties.PropertyType.Factor;
      if (typeName.startsWith("Property: "))
        propType = properties.PropertyType.Property;
      if (propType != null) {
        typeName = typeName.replace("Condition: ", "").replace("Property: ", "");
        String unit = null;
        if (typeName.contains("[") && typeName.contains("]")) {
          unit = parseUnit(typeName);
          val = val + " " + unit;
          typeName = typeName.replace(" [" + unit + "]", "");
        }
        openbisVal = parseXMLConditionValue(props.get("Q_PROPERTIES"), typeName, propType);
      } else
        openbisVal = props.get(typeCode);
      if (propToReverseVocabulary.containsKey(typeCode))
        openbisVal = propToReverseVocabulary.get(typeCode).get(openbisVal);

      boolean empty = openbisVal == null || openbisVal.isEmpty() || val == null || val.isEmpty();
      boolean same = val != null && val.equals(openbisVal);
      boolean collision = (!empty && !same);
      if (collision) {
        res = barcode + ": " + openbisVal + " --> " + val + "\n";
      }
    }
    return res;
  }

  private String parseXMLConditionValue(String xml, String label, properties.PropertyType type) {
    List<Property> props = new ArrayList<Property>();
    try {
      props = xmlParser.getAllPropertiesFromXML(xml);
    } catch (JAXBException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    String res = "";
    for (Property f : props) {
      if (f.getLabel().equals(label) && f.getType().equals(type)) {
        res = f.getValue();
        if (f.hasUnit())
          res += " " + f.getUnit();
      }
    }
    return res;
  }

  protected String getSelectedProperty(Object propertyId) {
    Item item = getActiveTable().getItem(-1);
    Object cell = item.getItemProperty(propertyId).getValue();
    if (cell instanceof ComboBox)
      return (String) ((ComboBox) cell).getValue();
    else
      return cell.toString();
  }

}
