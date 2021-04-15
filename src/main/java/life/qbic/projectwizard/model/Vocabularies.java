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
package life.qbic.projectwizard.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import life.qbic.portal.portlet.ProjectWizardUI;
import life.qbic.projectwizard.io.QuantificationMethodVocabularyParser;
import life.qbic.projectwizard.uicomponents.LabelingMethod;


public class Vocabularies {

  private Map<String, String> taxMap;
  private Map<String, String> tissueMap;
  private Map<String, String> msDeviceMap;
  private Map<String, String> cellLinesMap;
  private Map<String, String> proteinPurificationMethods;
  private List<String> measureTypes;
  private List<String> spaces;
  private Map<String, Integer> investigators;
  private List<String> experimentTypes;
  private List<String> enzymes;
  private Map<String, String> antibodies;
  private List<String> msProtocols;
  private List<String> lcmsMethods;
  private Map<String, String> chromTypes;
  private List<String> fractionationTypes;
  private List<String> enrichmentTypes;

  private Map<String, String> cultureMedia;
  private List<String> harvestingMethods;
  private List<String> lysisTypes;
  private Map<String, String> lcDeviceMap;
  private List<String> lcDetectionMethods;
  private List<String> msIonModes;

  private List<String> labelingTypes;
  private Map<String, String> samplePreparations;
  private Map<String, String> digestionMethods;
  private List<LabelingMethod> labelingMethods;
  private List<String> isotopeLabels;

  public Vocabularies() {
    QuantificationMethodVocabularyParser p = new QuantificationMethodVocabularyParser();
    this.labelingMethods =
        p.parseQuantificationMethods(new File(ProjectWizardUI.MSLabelingMethods));
    List<String> labels = new ArrayList<>();
    for (LabelingMethod m : labelingMethods) {
      labels.addAll(m.getReagents());
    }
    Collections.sort(labels);
    labels.add("Mix");
    this.isotopeLabels = labels;
  }
  
  public void setTaxMap(Map<String, String> taxMap) {
    this.taxMap = taxMap;
  }

  public void setTissueMap(Map<String, String> tissueMap) {
    this.tissueMap = tissueMap;
  }

  public void setMSDeviceMap(Map<String, String> deviceMap) {
    this.msDeviceMap = deviceMap;
  }

  public void setCellLinesMap(Map<String, String> cellLinesMap) {
    this.cellLinesMap = cellLinesMap;
  }

  public void setProteinPurificationMethods(Map<String, String> proteinPurificationMethods) {
    this.proteinPurificationMethods = proteinPurificationMethods;
  }

  public void setMeasureTypes(List<String> measureTypes) {
    this.measureTypes = measureTypes;
  }

  public void setInvestigators(Map<String, Integer> investigators) {
    this.investigators = investigators;
  }

  public void setExperimentTypes(List<String> experimentTypes) {
    this.experimentTypes = experimentTypes;
  }

  public void setEnzymes(Map<String, String> enzymeMap) {
    this.enzymes = new ArrayList<String>();
    enzymes.addAll(enzymeMap.keySet());
  }

  public void setAntibodies(Map<String, String> antibodies) {
    this.antibodies = antibodies;
  }

  public void setMsProtocols(List<String> msProtocols) {
    this.msProtocols = msProtocols;
  }

  public void setLcmsMethods(List<String> lcmsMethods) {
    this.lcmsMethods = lcmsMethods;
  }

  public void setChromTypes(Map<String, String> chromTypes) {
    this.chromTypes = chromTypes;
  }

  public void setFractionationTypes(List<String> fractionationTypes) {
    this.fractionationTypes = fractionationTypes;
  }

  public void setEnrichmentTypes(List<String> enrichmentTypes) {
    this.enrichmentTypes = enrichmentTypes;
  }

  public void setCultureMedia(Map<String, String> cultureMedia) {
    this.cultureMedia = cultureMedia;
  }

  public void setHarvestingMethods(List<String> harvestingMethods) {
    this.harvestingMethods = harvestingMethods;
  }

  public void setLysisTypes(List<String> lysisTypes) {
    this.lysisTypes = lysisTypes;
  }

  public void setLCDeviceMap(Map<String, String> lcDeviceMap) {
    this.lcDeviceMap = lcDeviceMap;
  }

  public void setLCDetectionMethods(List<String> lcDetectionMethods) {
    this.lcDetectionMethods = lcDetectionMethods;
  }

  public void setMSIonModes(List<String> msIonModes) {
    this.msIonModes = msIonModes;
  }

  public void setLabelingTypes(List<String> labelingTypes) {
    this.labelingTypes = labelingTypes;
  }

  public void setSamplePreparations(Map<String, String> samplePreparations) {
    this.samplePreparations = samplePreparations;
  }

  public void setDigestionMethods(Map<String, String> digestionMethods) {
    this.digestionMethods = digestionMethods;
  }

  public void setLabelingMethods(List<LabelingMethod> labelingMethods) {
    this.labelingMethods = labelingMethods;
  }

  public void setIsotopeLabels(List<String> isotopeLabels) {
    this.isotopeLabels = isotopeLabels;
  }

  public List<String> getLabelingTypes() {
    return labelingTypes;
  }

  public Map<String, String> getSamplePreparationMethods() {
    return samplePreparations;
  }

  public List<String> getFractionationTypes() {
    return fractionationTypes;
  }

  public List<String> getEnrichmentTypes() {
    return enrichmentTypes;
  }

  public Map<String, String> getProteinPurificationMethodsMap() {
    return proteinPurificationMethods;
  }

  public Map<String, String> getCellLinesMap() {
    return cellLinesMap;
  }

  public Map<String, String> getTaxMap() {
    return taxMap;
  }

  public Map<String, String> getTissueMap() {
    return tissueMap;
  }

  public Map<String, String> getMSDeviceMap() {
    return msDeviceMap;
  }

  public List<String> getAnalyteTypes() {
    return measureTypes;
  }

  public List<String> getSpaces() {
    return spaces;
  }

  public Map<String, Integer> getPeople() {
    return investigators;
  }

  public List<String> getExperimentTypes() {
    return experimentTypes;
  }

  public List<String> getEnzymes() {
    return enzymes;
  }

  public List<String> getMsProtocols() {
    return msProtocols;
  }

  public List<String> getLcmsMethods() {
    return lcmsMethods;
  }

  public Map<String, String> getChromTypesMap() {
    return chromTypes;
  }

  public Map<String, String> getAntibodiesMap() {
    return antibodies;
  }

  public void setPeople(Map<String, Integer> people) {
    this.investigators = people;
  }

  public void setSpaces(List<String> userSpaces) {
    this.spaces = userSpaces;
  }

  public Map<String, String> getDigestionMethodsMap() {
    return digestionMethods;
  }

  public List<String> getAllIsotopeLabels() {
    return isotopeLabels;
  }

  public List<LabelingMethod> getLabelingMethods() {
    return labelingMethods;
  }

  public Map<String, String> getCultureMedia() {
    return cultureMedia;
  }

  public List<String> getHarvestingMethods() {
    return harvestingMethods;
  }

  public List<String> getLysisTypes() {
    return lysisTypes;
  }

  public Map<String, String> getLCDeviceMap() {
    return lcDeviceMap;
  }

  public List<String> getLCDetectionMethods() {
    return lcDetectionMethods;
  }

  public List<String> getMSIonModes() {
    return msIonModes;
  }

}
