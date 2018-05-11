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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Button.ClickListener;

import life.qbic.datamodel.samples.AOpenbisSample;
import life.qbic.portal.Styles;
import life.qbic.projectwizard.control.WizardController.Steps;
import life.qbic.projectwizard.io.DBVocabularies;
import life.qbic.projectwizard.model.MHCLigandExtractionProtocol;
import life.qbic.projectwizard.model.RegisteredAnalyteInformation;
import life.qbic.projectwizard.model.TestSampleInformation;
import life.qbic.projectwizard.uicomponents.LigandExtractPanel;
import life.qbic.projectwizard.uicomponents.MSOptionComponent;
import life.qbic.projectwizard.uicomponents.TechnologiesPanel;
import life.qbic.portal.Styles.NotificationType;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.VerticalLayout;

/**
 * Wizard Step to put in information about the Sample Preparation that leads to a list of Test
 * Samples
 * 
 * @author Andreas Friedrich
 * 
 */
public class TestStep implements WizardStep {

  private VerticalLayout main;
  private TechnologiesPanel techPanel;
  private MSOptionComponent msPanel;
  private LigandExtractPanel mhcLigandPanel;
  private CheckBox noMeasure;
  private DBVocabularies vocabs;
  private boolean containsProteins = false;
  private boolean containsMHCLigands = false;

  private Wizard wizard;

  /**
   * Create a new Sample Preparation step for the wizard
   * 
   * @param sampleTypes Available list of sample types, e.g. Proteins, RNA etc.
   */
  public TestStep(Wizard wizard, DBVocabularies vocabs) {
    this.wizard = wizard;
    this.vocabs = vocabs;
    main = new VerticalLayout();
    main.setMargin(true);
    main.setSpacing(true);
    main.setSizeUndefined();
    Label header = new Label("Analysis Method");
    main.addComponent(Styles.questionize(header,
        "Here you can specify what kind of material is extracted from the samples for measurement, how many measurements (techn. replicates) are taken per sample "
            + "and if there is pooling for some or all of the technologies used.",
        "Analysis Method"));
    noMeasure = new CheckBox("No further preparation of samples?");
    main.addComponent(Styles.questionize(noMeasure,
        "Check if no DNA etc. is extracted and measured, for example in tissue imaging.",
        "No Sample Preparation"));

    noMeasure.addValueChangeListener(new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = 2393762547426343668L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        changeTechPanel();
      }
    });
  }

  @Override
  public String getCaption() {
    return "Analysis Method";
  }

  public CheckBox getNotMeasured() {
    return noMeasure;
  }

  public void changeTechPanel() {
    if (noMeasure.getValue())
      techPanel.resetInputs();
    techPanel.setEnabled(!noMeasure.getValue());
  }

  @Override
  public Component getContent() {
    return main;
  }

  @Override
  public boolean onAdvance() {
    if (techPanel.isValid() || noMeasure.getValue()) {
      if (containsProteins) {
        if (msPanel.isValid()) {
          return true;
        } else
          return false;
      } else
        return true;
    } else {
      Styles.notification("Missing information",
          "Please input at least one analyte and the number of replicates.",
          NotificationType.ERROR);
      return false;
    }
  }

  private void replaceWizardSteps(List<WizardStep> nextMSSteps) {
    resetNextSteps(hasPools());
    for (WizardStep step : nextMSSteps)
      wizard.addStep(step);
  }

  @Override
  public boolean onBack() {
    return true;
  }

  public List<TestSampleInformation> getAnalyteInformation() {
    return techPanel.getTechInfo();
  }

  public boolean hasProteins() {
    return containsProteins;
  }

  public boolean hasPools() {
    return techPanel.poolingSet();
  }

  public void initTestStep(ValueChangeListener testPoolListener,
      ValueChangeListener outerProteinListener, ClickListener refreshPeopleListener,
      Map<Steps, WizardStep> steps) {
    ValueChangeListener proteinListener = new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = -2885684670741817840L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        containsProteins = false;
        for (TestSampleInformation i : getAnalyteInformation()) {
          String tech = i.getTechnology();
          containsProteins |= tech.equals("PROTEINS");
        }
        msPanel.setVisible(containsProteins);
        if (!containsProteins) {
          resetNextSteps(hasPools());
          wizard.addStep(steps.get(Steps.Registration));
        } else {
          replaceWizardSteps(msPanel.getNextMSSteps(steps));
        }
      }
    };

    ValueChangeListener mhcLigandListener = new ValueChangeListener() {

      /**
         * 
         */
      private static final long serialVersionUID = -2883346670741817840L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        containsMHCLigands = false;
        for (TestSampleInformation i : getAnalyteInformation()) {
          containsMHCLigands |= i.getTechnology().equals("CELL_LYSATE");
        }
        mhcLigandPanel.setVisible(containsMHCLigands);
      }
    };
    techPanel = new TechnologiesPanel(vocabs.getAnalyteTypes(), vocabs.getPeople().keySet(),
        new OptionGroup(""), testPoolListener,
        new ArrayList<ValueChangeListener>(Arrays.asList(outerProteinListener, proteinListener)),
        mhcLigandListener, refreshPeopleListener);
    main.addComponent(techPanel);
    main.addComponent(new Label("<hr />", Label.CONTENT_XHTML));
    msPanel = new MSOptionComponent(vocabs);
    msPanel.setVisible(false);
    ValueChangeListener msExpChangedListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        replaceWizardSteps(msPanel.getNextMSSteps(steps));
      }
    };

    msPanel.addMSListener(msExpChangedListener);

    main.addComponent(msPanel);

    mhcLigandPanel = new LigandExtractPanel(vocabs);
    mhcLigandPanel.setVisible(false);

    main.addComponent(mhcLigandPanel);
  }

  public void setTissueExtracts(List<AOpenbisSample> extracts) {
    mhcLigandPanel.setTissueSamples(extracts);
  }

  public Map<String, MHCLigandExtractionProtocol> getAntibodyInfos() {
    return mhcLigandPanel.getAntibodyInfos();
  }

  public Map<String, Map<String, Object>> getMHCLigandExtractProperties() {
    if (containsMHCLigands)
      return mhcLigandPanel.getExperimentalProperties();
    else
      return null;
  }

  public boolean hasMHCLigands() {
    return containsMHCLigands;
  }

  public Map<String, Object> getProteinPreparationInformation() {
    Map<String, Object> res = new HashMap<String, Object>();
    if (msPanel.usesPurification())
      res.put("Q_MS_PURIFICATION_METHOD", msPanel.getPurificationMethod());
    if (msPanel.usesShortGel())
      res.put("Q_ADDITIONAL_INFO", "Short Gel");
    return res;
  }

  private void resetNextSteps(boolean pool) {
    List<WizardStep> steps = wizard.getSteps();
    List<WizardStep> copy = new ArrayList<WizardStep>();
    copy.addAll(steps);
    boolean isNew = false;
    for (int i = 0; i < copy.size(); i++) {
      WizardStep cur = copy.get(i);
      boolean keep = pool && cur instanceof PoolingStep && !hasProteins();
      if (isNew && !keep) {
        wizard.removeStep(cur);
      }
      if (cur.equals(this))
        isNew = true;
    }
  }

  public boolean hasComplexProteinPoolBeforeFractionation() {
    return msPanel.hasProteinPoolBeforeFractionation();
  }

  public void setAnalyteInputs(RegisteredAnalyteInformation infos) {
    for (String analyte : infos.getAnalytes()) {
      techPanel.select(analyte);
    }
    msPanel.selectMeasurePeptides(infos.isMeasurePeptides());
    msPanel.selectUseShortGel(infos.isShortGel());
    msPanel.selectProteinPurification(infos.getPurificationMethod());
  }

  public void updatePeople(Set<String> people) {
    techPanel.updatePeople(people);
  }
}
