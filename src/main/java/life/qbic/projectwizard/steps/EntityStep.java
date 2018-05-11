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
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import life.qbic.portlet.ProjectWizardUI;
import life.qbic.projectwizard.uicomponents.ConditionsPanel;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.portal.components.OpenbisInfoTextField;

/**
 * Wizard Step to life.qbic.projectwizard.model the biological entities of an experiment
 * 
 * @author Andreas Friedrich
 * 
 */
public class EntityStep implements WizardStep {

  private boolean skip = false;
  private OptionGroup conditionsSet = new OptionGroup("dummy");

  private VerticalLayout main;

  private TextField expName;
  private ComboBox species;
  private TextField specialSpecies;
  private ConditionsPanel c;

  private String emptyFactor = "Other (please specify)";
  private List<String> suggestions = new ArrayList<String>(Arrays.asList("Age", "Genotype",
      "Health State", "Phenotype", "Species", "Treatment", emptyFactor));

  private OpenbisInfoTextField speciesNum;
  private ComboBox person;
  private Button reloadPeople;

  private OpenbisInfoTextField bioReps;

  public ConditionsPanel getCondPanel() {
    return c;
  }

  public OptionGroup isConditionsSet() {
    return conditionsSet;
  }

  /**
   * Create a new Entity step for the wizard
   * 
   * @param speciesMap A map of available species (codes and labels)
   * @param people
   */
  public EntityStep(Map<String, String> speciesMap, Set<String> people) {
    main = new VerticalLayout();
    main.setMargin(true);
    main.setSpacing(true);
    Label header = new Label("Sample Sources");
    main.addComponent(Styles.questionize(header,
        "Sample sources are individual patients, animals or plants that are used in the experiment. "
            + "You can input (optional) experimental variables, e.g. genotypes, that differ between different experimental groups.",
        "Sample Sources"));
    ArrayList<String> openbisSpecies = new ArrayList<String>();
    openbisSpecies.addAll(speciesMap.keySet());
    Collections.sort(openbisSpecies);
    species = new ComboBox("Species", openbisSpecies);
    species.setStyleName(Styles.boxTheme);
    species.setRequired(true);
    species.setFilteringMode(FilteringMode.CONTAINS);
    if (ProjectWizardUI.testMode)
      species.setValue("Homo Sapiens");
    speciesNum = new OpenbisInfoTextField("How many different species are there in this project?",
        "", "50px", "2");
    speciesNum.getInnerComponent().setVisible(false);
    speciesNum.getInnerComponent().setEnabled(false);
    c = new ConditionsPanel(suggestions, emptyFactor, "Species", species, true, conditionsSet,
        (TextField) speciesNum.getInnerComponent());
    expName = new TextField("Experimental Step Name");
    expName.setStyleName(Styles.fieldTheme);
    main.addComponent(expName);
    main.addComponent(c);
    main.addComponent(speciesNum.getInnerComponent());
    main.addComponent(species);

    species.addValueChangeListener(new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = 1987640360028444299L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        specialSpecies.setVisible(species.getValue().equals("Other"));
      }
    });
    specialSpecies = new TextField("Species Information");
    specialSpecies.setStyleName(Styles.fieldTheme);
    specialSpecies.setVisible(false);
    specialSpecies.setWidth("350px");
    main.addComponent(specialSpecies);

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
        "Contact person responsible for patients or sample sources.", "Contact Person"));

    bioReps = new OpenbisInfoTextField(
        "How many identical biological replicates (e.g. animals) per group are there?",
        "Number of (biological) replicates for each group."
            + "Technical replicates are added later!",
        "50px", "1");
    main.addComponent(bioReps.getInnerComponent());
  }

  public TextField getExpNameField() {
    return expName;
  }

  @Override
  public String getCaption() {
    return "Sample Sources";
  }

  @Override
  public Component getContent() {
    return main;
  }

  @Override
  public boolean onAdvance() {
    if (skip || speciesReady() && replicatesReady() && c.isValid())
      return true;
    else {
      Styles.notification("Missing information", "Please fill in the required fields.",
          NotificationType.ERROR);
      return false;
    }
  }

  private boolean replicatesReady() {
    return !bioReps.getValue().isEmpty();
  }

  private boolean speciesReady() {
    return speciesIsFactor()
        || (species.getValue() != null && !species.getValue().toString().isEmpty());
  }

  @Override
  public boolean onBack() {
    return true;
  }

  public boolean speciesIsFactor() {
    //TODO was: isEnabld
    return !species.isVisible();
  }

  public void enableSpeciesFactor(boolean enable) {
    speciesNum.getInnerComponent().setEnabled(enable);
    speciesNum.getInnerComponent().setVisible(enable);
    if (enable)
      species.setValue(null);
  }

  public List<String> getFactors() {
    return c.getConditions();
  }

  public int getBioRepAmount() {
    return Integer.parseInt(bioReps.getValue());
  }

  public String getSpecies() {
    if (species.getValue() == null)
      return null;
    else
      return species.getValue().toString();
  }

  public String getSpecialSpecies() {
    return specialSpecies.getValue();
  }

  public boolean factorFieldOther(ComboBox source) {
    return emptyFactor.equals(source.getValue());
  }

  public int getSpeciesAmount() {
    return Integer.parseInt(speciesNum.getValue());
  }

  public void setSkipStep(boolean b) {
    skip = b;
  }

  public boolean isSkipped() {
    return skip;
  }

  public String getPerson() {
    if (person.getValue() != null)
      return person.getValue().toString();
    else
      return null;
  }

  public Button getPeopleReloadButton() {
    return reloadPeople;
  }

  public void updatePeople(Set<String> people) {
    String contact = getPerson();
    person.removeAllItems();
    person.addItems(people);
    if (contact != null && !contact.isEmpty())
      person.select(contact);
  }

}
