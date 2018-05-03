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
package life.qbic.portlet.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import properties.Property;

import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import life.qbic.datamodel.entities.OpenbisTestSample;
import life.qbic.portlet.control.WizardController.Steps;
import life.qbic.portlet.model.AOpenbisSample;
import life.qbic.portlet.model.ExperimentModel;
import life.qbic.portlet.model.MSExperimentModel;
import life.qbic.portlet.uicomponents.DragDropPoolComponent;
import life.qbic.portlet.uicomponents.Styles;
import life.qbic.portlet.uicomponents.Styles.*;
import logging.Log4j2Logger;

public class PoolingStep implements WizardStep {

  private VerticalLayout main;
  private Steps type;
  private DragDropPoolComponent pooling;
  private List<DragDropPoolComponent> poolings;
  private TabSheet instances;
  private MSExperimentModel model;
  logging.Logger logger = new Log4j2Logger(PoolingStep.class);

  public PoolingStep(Steps poolStep) {
    instances = new TabSheet();
    instances.setStyleName(ValoTheme.TABSHEET_FRAMED);
    poolings = new ArrayList<DragDropPoolComponent>();
    this.type = poolStep;
    main = new VerticalLayout();
    main.setSpacing(true);
    main.setMargin(true);
    pooling = new DragDropPoolComponent(getPoolPrefix(poolStep));
    main.addComponent(instances);
  }

  private String getPoolPrefix(Steps poolStep) {
    String name;
    switch (type) {
      case Extract_Pooling:
        name = "Extr-";
        break;
      case Test_Sample_Pooling:
        name = "Prep-";
        break;
      case Protein_Fractionation_Pooling:
        name = "Fraction-";
        break;
      case Peptide_Fractionation_Pooling:
        name = "Fraction-";
        break;
      default:
        name = "";
        break;
    }
    return name;
  }

  @Override
  public String getCaption() {
    String name;
    switch (type) {
      case Extract_Pooling:
        name = "Extr. Pooling";
        break;
      case Test_Sample_Pooling:
        name = "Prep. Pooling";
        break;
      case Protein_Fractionation_Pooling:
        name = "Fract. Pooling";
        break;
      case Peptide_Fractionation_Pooling:
        name = "Fract. Pooling";
        break;
      default:
        name = "";
        break;
    }
    return name;
  }

  @Override
  public Component getContent() {
    return main;
  }

  @Override
  public boolean onAdvance() {
    if (pooling.getPools().isEmpty()) {
      Styles.notification("No pooled samples",
          "Please create at least one pool containing samples or uncheck pooling in the previous step(s).",
          NotificationType.ERROR);
      return false;
    } else
      return true;
  }

  @Override
  public boolean onBack() {
    resetStep();
    return true;
  }

  public void setSamples(List<List<AOpenbisSample>> sampleGroups, Steps poolingType) {
    for (List<AOpenbisSample> group : sampleGroups) {
      String type = group.get(0).getValueMap().get("Q_SAMPLE_TYPE");
      pooling = new DragDropPoolComponent(getPoolPrefix(poolingType));
      pooling.initConditionsAndSetSamples(group);
      poolings.add(pooling);
      instances.addTab(pooling, type);
    }
  }

  public Map<String, List<AOpenbisSample>> getPools() {
    Map<String, List<AOpenbisSample>> res = new HashMap<String, List<AOpenbisSample>>();
    for (DragDropPoolComponent pooling : poolings) {
      res.putAll(pooling.getPools());
    }
    return res;
  }

  public void resetStep() {
    poolings = new ArrayList<DragDropPoolComponent>();
    main.removeComponent(instances);
    instances = new TabSheet();
    main.addComponent(instances);
  }

  // sets Samples from the last analyte level (last step) of the current ms experiment life.qbic.projectwizard.model to be
  // used in the pooling.
  // in the future pooling between samples created in different steps might be considered
  public void setPreliminaryExperiments(MSExperimentModel msExperimentModel, Steps poolingType) {
    this.model = msExperimentModel;
    List<List<AOpenbisSample>> allSamples = new ArrayList<List<AOpenbisSample>>();
    List<AOpenbisSample> currentSamples = new ArrayList<AOpenbisSample>();
    for (ExperimentModel fract : msExperimentModel.getLastStepAnalytes()) {
      currentSamples.addAll(fract.getSamples());
    }
    allSamples.add(currentSamples);
    setSamples(allSamples, poolingType);
  }

  public MSExperimentModel getPreliminarySamples() {
    String analyte = "unknown";
    if (type.equals(Steps.Protein_Fractionation_Pooling))
      analyte = "PROTEINS";
    if (type.equals(Steps.Peptide_Fractionation_Pooling))
      analyte = "PEPTIDES";
    List<AOpenbisSample> samples = new ArrayList<AOpenbisSample>();
    Map<String, List<AOpenbisSample>> pools = getPools();
    for (String name : pools.keySet()) {
      samples.add(new OpenbisTestSample(1, pools.get(name), analyte, name, "",
          new ArrayList<Property>(), ""));
    }
    ExperimentModel exp = new ExperimentModel(1, samples);
    List<ExperimentModel> exps = new ArrayList<ExperimentModel>(Arrays.asList(exp));
    this.model.addAnalyteStepExperiments(exps);
    return model;
  }

}
