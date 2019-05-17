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
package life.qbic.projectwizard.steps;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import life.qbic.portal.portlet.ProjectWizardUI;
import life.qbic.projectwizard.io.QuantificationMethodVocabularyParser;
import life.qbic.projectwizard.model.TissueInfo;
import life.qbic.projectwizard.uicomponents.ConditionsPanel;
import life.qbic.projectwizard.uicomponents.LabelingMethod;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.portal.components.OpenbisInfoTextField;

/**
 * Wizard Step to life.qbic.projectwizard.model the extraction of biological samples from entities
 * 
 * @author Andreas Friedrich
 * 
 */
public class ExtractionStep implements WizardStep {

  private boolean skip = false;
  private OptionGroup conditionsSet = new OptionGroup("dummy");

  private VerticalLayout main;

  private ComboBox tissue;
  private ComboBox cellLine;
  private TextField otherTissue;
  private ConditionsPanel c;
  private TextField expName;

  private String emptyFactor = "Other (please specify)";
  private List<String> suggestions = new ArrayList<String>(Arrays.asList("Extraction time",
      "Growth Medium", "Radiation", "Tissue", "Transient expression", "Treatment", emptyFactor));
  private CheckBox isotopes;
  private ComboBox isotopeTypes;

  private OpenbisInfoTextField tissueNum;
  private ComboBox person;
  private Button reloadPeople;

  private OpenbisInfoTextField extractReps;
  private List<LabelingMethod> labelingMethods;


  /**
   * Create a new Extraction step for the wizard
   * 
   * @param tissueMap A map of available tissues (codes and labels)
   * @param cellLinesMap
   */
  public ExtractionStep(Map<String, String> tissueMap, Map<String, String> cellLinesMap,
      Set<String> people) {
    main = new VerticalLayout();
    main.setMargin(true);
    main.setSpacing(true);
    Label header = new Label("Sample Extracts");
    main.addComponent(Styles.questionize(header,
        "Extracts are individual tissue or other samples taken from organisms and used in the experiment. "
            + "You can input (optional) experimental variables, e.g. extraction times or treatments, that differ between different groups "
            + "of extracts.",
        "Sample Extracts"));

    ArrayList<String> tissues = new ArrayList<String>();
    tissues.addAll(tissueMap.keySet());
    Collections.sort(tissues);
    tissue = new ComboBox("Tissue", tissues);
    tissue.setRequired(true);
    tissue.setStyleName(Styles.boxTheme);
    tissue.setFilteringMode(FilteringMode.CONTAINS);
    if (ProjectWizardUI.testMode)
      tissue.setValue("Blood");
    tissueNum = new OpenbisInfoTextField(
        "How many different tissue types are there in this sample extraction?", "", "50px", "2");
    tissueNum.getInnerComponent().setVisible(false);
    tissueNum.getInnerComponent().setEnabled(false);

    expName = new TextField("Experimental Step Name");
    expName.setStyleName(Styles.fieldTheme);
    main.addComponent(expName);

    c = new ConditionsPanel(suggestions, emptyFactor, "Tissue", tissue, true, conditionsSet,
        (TextField) tissueNum.getInnerComponent());
    main.addComponent(c);

    isotopes = new CheckBox("Isotope Labeling");
    isotopes.setImmediate(true);
    main.addComponent(Styles.questionize(isotopes,
        "Are extracted cells labeled by isotope labeling (e.g. for Mass Spectrometry)?",
        "Isotope Labeling"));

    labelingMethods = initLabelingMethods();

    isotopeTypes = new ComboBox();
    isotopeTypes.setVisible(false);
    isotopeTypes.setImmediate(true);
    isotopeTypes.setStyleName(Styles.boxTheme);
    isotopeTypes.setNullSelectionAllowed(false);
    for (LabelingMethod l : labelingMethods)
      isotopeTypes.addItem(l.getName());
    main.addComponent(isotopeTypes);

    isotopes.addValueChangeListener(new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = 6993706766195224898L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        isotopeTypes.setVisible(isotopes.getValue());
      }
    });
    main.addComponent(tissueNum.getInnerComponent());
    main.addComponent(tissue);

    tissue.addValueChangeListener(new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = 1987640360028444299L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        cellLine.setVisible(tissue.getValue().equals("Cell Line"));
        otherTissue.setVisible(tissue.getValue().equals("Other"));
      }
    });
    ArrayList<String> cellLines = new ArrayList<String>();
    cellLines.addAll(cellLinesMap.keySet());
    Collections.sort(cellLines);
    cellLine = new ComboBox("Cell Line", cellLines);
    cellLine.setStyleName(Styles.boxTheme);
    cellLine.setImmediate(true);
    cellLine.setVisible(false);
    cellLine.setFilteringMode(FilteringMode.CONTAINS);
    main.addComponent(cellLine);
    otherTissue = new TextField("Tissue Information");
    otherTissue.setWidth("350px");
    otherTissue.setStyleName(Styles.fieldTheme);
    otherTissue.setVisible(false);
    main.addComponent(otherTissue);

    HorizontalLayout persBoxH = new HorizontalLayout();
    persBoxH.setCaption("Contact Person");
    person = new ComboBox();
    person.addItems(people);
    person.setFilteringMode(FilteringMode.CONTAINS);
    person.setStyleName(Styles.boxTheme);

    reloadPeople = new Button();
    Styles.iconButton(reloadPeople, FontAwesome.REFRESH);
    persBoxH.addComponent(person);
    persBoxH.addComponent(reloadPeople);

    main.addComponent(Styles.questionize(persBoxH,
        "Contact person responsible for tissue extraction.", "Contact Person"));

    extractReps = new OpenbisInfoTextField(
        "Extracted replicates per patient/animal/plant per experimental variable.",
        "Number of extractions per individual defined in the last step. "
            + "Technical replicates are added later!",
        "50px", "1");
    main.addComponent(extractReps.getInnerComponent());
  }

  public ConditionsPanel getCondPanel() {
    return c;
  }

  public OptionGroup conditionsSet() {
    return conditionsSet;
  }

  public String getPerson() {
    if (person.getValue() != null)
      return person.getValue().toString();
    else
      return null;
  }

  public TextField getExpNameField() {
    return expName;
  }

  private List<LabelingMethod> initLabelingMethods() {
    QuantificationMethodVocabularyParser p = new QuantificationMethodVocabularyParser();
    return p.parseQuantificationMethods(new File(ProjectWizardUI.MSLabelingMethods));
  }

  @Override
  public String getCaption() {
    return "Sample Extr.";
  }

  @Override
  public Component getContent() {
    return main;
  }

  @Override
  public boolean onAdvance() {
    if (skip || tissueReady() && replicatesReady() && c.isValid())
      return true;
    else {
      Styles.notification("Information missing", "Please fill in the required fields.",
          NotificationType.ERROR);
      return false;
    }
  }

  private boolean replicatesReady() {
    return !extractReps.getValue().isEmpty();
  }

  private boolean tissueReady() {
    return isTissueFactor() || tissue.getValue() != null;
  }

  @Override
  public boolean onBack() {
    return true;
  }

  public boolean isTissueFactor() {
    // TODO test. was: isEnabled
    return !tissue.isVisible();
  }

  public List<String> getFactors() {
    return c.getConditions();
  }

  public int getExtractRepAmount() {
    return Integer.parseInt(extractReps.getValue());
  }

  public String getTissue() {
    if (tissue.getValue() == null)
      return null;
    else
      return tissue.getValue().toString();
  }

  public String getSpecialTissue() {
    return otherTissue.getValue();
  }

  public String getCellLine() {
    if (cellLine.getValue() != null)
      return (String) cellLine.getValue();
    else
      return "";
  }

  public boolean factorFieldOther(ComboBox source) {
    return emptyFactor.equals(source.getValue());
  }

  public int getTissueAmount() {
    return Integer.parseInt(tissueNum.getValue());
  }

  public void setSkipStep(boolean b) {
    skip = b;
  }

  public boolean isSkipped() {
    return skip;
  }

  public Button getPeopleReloadButton() {
    return reloadPeople;
  }

  public LabelingMethod getLabelingMethod() {
    Object o = isotopeTypes.getValue();
    if (o != null) {
      String type = o.toString();
      for (LabelingMethod l : labelingMethods)
        if (type.equals(l.getName()))
          return l;
      return null;
    } else
      return null;
  }

  public void updatePeople(Set<String> people) {
    String contact = getPerson();
    person.removeAllItems();
    person.addItems(people);
    if (contact != null && !contact.isEmpty())
      person.select(contact);
  }

}
