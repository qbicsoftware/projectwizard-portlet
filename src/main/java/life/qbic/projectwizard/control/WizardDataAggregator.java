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
package life.qbic.projectwizard.control;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vaadin.teemu.wizards.WizardStep;

import life.qbic.datamodel.experiments.ExperimentModel;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.experiments.OpenbisExperiment;
import life.qbic.datamodel.identifiers.SampleCodeFunctions;
import life.qbic.datamodel.persons.PersonType;
import life.qbic.datamodel.samples.AOpenbisSample;
import life.qbic.datamodel.samples.OpenbisBiologicalEntity;
import life.qbic.datamodel.samples.OpenbisBiologicalSample;
import life.qbic.datamodel.samples.OpenbisMHCExtractSample;
import life.qbic.datamodel.samples.OpenbisTestSample;
import life.qbic.expdesign.ParserHelpers;
import life.qbic.expdesign.model.ExperimentalDesignPropertyWrapper;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.portal.portlet.ProjectWizardUI;
import life.qbic.projectwizard.control.WizardController.Steps;
import life.qbic.projectwizard.model.MHCLigandExtractionProtocol;
import life.qbic.projectwizard.model.MSExperimentModel;
import life.qbic.projectwizard.model.RegisteredAnalyteInformation;
import life.qbic.projectwizard.model.TestSampleInformation;
import life.qbic.projectwizard.model.TissueInfo;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Experiment;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import ch.systemsx.cisd.openbis.plugin.query.shared.api.v1.dto.QueryTableModel;
import life.qbic.projectwizard.steps.ConditionInstanceStep;
import life.qbic.projectwizard.steps.EntityStep;
import life.qbic.projectwizard.steps.ExtractionStep;
import life.qbic.projectwizard.steps.ProjectContextStep;
import life.qbic.projectwizard.steps.AnalyteStep;
import life.qbic.xml.manager.StudyXMLParser;
import life.qbic.xml.properties.Property;
import life.qbic.xml.study.Qexperiment;
import life.qbic.xml.study.TechnologyType;

/**
 * Aggregates the data from the wizard needed to create the experimental setup in the form of TSVs
 * 
 * @author Andreas Friedrich
 * 
 */
public class WizardDataAggregator {

  private ProjectContextStep s1;
  private EntityStep s2;
  private ConditionInstanceStep s3;
  private ExtractionStep s5;
  private ConditionInstanceStep s6;
  private AnalyteStep s8;

  private String tsvContent;

  private IOpenBisClient openbis;
  // private XMLParser xmlParser = new XMLParser();
  private Map<String, String> taxMap;
  private Map<String, String> tissueMap;
  private Map<String, Property> factorMap;
  private Map<String, Integer> personMap;
  private int firstFreeExperimentID;
  private int firstFreeEntityID;
  private Map<String, Sample> existingSamples;
  private Map<String, String> oldCodesToNewCodes;
  private String nextBarcode;
  private String firstFreeBarcode;
  private char classChar = 'X';

  // mandatory openBIS fields
  private String spaceCode;
  private String projectCode;
  private List<OpenbisExperiment> experiments;
  private String species;
  private String speciesInfo;
  private String tissue;
  private String specialTissue;
  private List<TestSampleInformation> techTypeInfo = new ArrayList<TestSampleInformation>();

  // info needed to create samples
  private int bioReps;
  private int extractReps;

  private List<List<String>> bioFactors;
  private List<List<String>> extractFactors;
  private boolean inheritEntities;
  private boolean inheritExtracts;

  private List<Sample> openbisEntities;
  private List<AOpenbisSample> entities = new ArrayList<AOpenbisSample>();
  private List<AOpenbisSample> extracts;
  private List<AOpenbisSample> tests;
  private List<AOpenbisSample> extractPools;
  private List<AOpenbisSample> testPools;
  private List<AOpenbisSample> msSamples;
  private List<AOpenbisSample> mhcExtracts;
  private Map<String, Character> classChars;
  private static final Logger logger = LogManager.getLogger(WizardDataAggregator.class);
  private ArrayList<Sample> samples;

  private Map<String, Map<String, Object>> mhcExperimentProtocols;
  private MSExperimentModel fractionationProperties;
  private List<ExperimentType> informativeExpTypes = new ArrayList<ExperimentType>(
      Arrays.asList(ExperimentType.Q_MHC_LIGAND_EXTRACTION, ExperimentType.Q_MS_MEASUREMENT));
  private Experiment expDesignExperiment;
  // private String designExperimentID;
  final private StudyXMLParser parser = new StudyXMLParser();
  private JAXBElement<Qexperiment> oldExpDesign;
  private Set<String> oldFactors;
  private Map<Pair<String, String>, Property> oldFactorsForSamples;

  /**
   * Creates a new WizardDataAggregator
   * 
   * @param steps the steps of the Wizard to extract the data from
   * @param openbis openBIS client connection to query for existing context
   * @param taxMap mapping between taxonomy IDs and species names
   * @param tissueMap mapping of tissue names and labels
   */
  public WizardDataAggregator(Map<Steps, WizardStep> steps, IOpenBisClient openbis,
      Map<String, String> taxMap, Map<String, String> tissueMap, Map<String, Integer> personMap) {
    s1 = (ProjectContextStep) steps.get(Steps.Project_Context);
    s2 = (EntityStep) steps.get(Steps.Entities);
    s3 = (ConditionInstanceStep) steps.get(Steps.Entity_Conditions);
    s5 = (ExtractionStep) steps.get(Steps.Extraction);
    s6 = (ConditionInstanceStep) steps.get(Steps.Extract_Conditions);
    s8 = (AnalyteStep) steps.get(Steps.Test_Samples);

    this.openbis = openbis;
    this.taxMap = taxMap;
    this.personMap = personMap;
    this.tissueMap = tissueMap;
  }

  public String getProjectCode() {
    return projectCode;
  }

  /**
   * Fetches context information like space and project and computes first unused IDs of samples and
   * context.
   */
  private void prepareBasics() {
    firstFreeExperimentID = 1;
    firstFreeEntityID = 1;
    firstFreeBarcode = "";// TODO cleanup where not needed
    existingSamples = new HashMap<String, Sample>();
    spaceCode = s1.getSpaceCode();
    projectCode = s1.getProjectCode().toUpperCase();

    samples = new ArrayList<Sample>();
    if (openbis.projectExists(spaceCode, projectCode)) {
      samples.addAll(openbis.getSamplesWithParentsAndChildrenOfProjectBySearchService(
          "/" + spaceCode + "/" + projectCode));
    }

    if (!s1.fetchTSVModeSet()) {
      for (Experiment e : openbis.getExperimentsOfProjectByCode(projectCode)) {
        String code = e.getCode();
        String[] split = code.split(projectCode + "E");
        if (code.startsWith(projectCode + "E") && split.length > 1) {
          int num = 0;
          try {
            num = Integer.parseInt(split[1]);
          } catch (Exception e2) {
          }
          if (firstFreeExperimentID <= num)
            firstFreeExperimentID = num + 1;
        }
      }

      for (Sample s : samples) {
        String code = s.getCode();
        existingSamples.put(code, s);
        if (SampleCodeFunctions.isQbicBarcode(code)) {
          if (SampleCodeFunctions.compareSampleCodes(firstFreeBarcode, code) <= 0) {
            firstFreeBarcode = SampleCodeFunctions.incrementSampleCode(code);
          }
        } else if (s.getSampleTypeCode().equals(("Q_BIOLOGICAL_ENTITY"))) {
          int num = Integer.parseInt(s.getCode().split("-")[1]);
          if (num >= firstFreeEntityID)
            firstFreeEntityID = num + 1;
        }
      }
    }
  }

  /**
   * Creates the list of biological entities from the input information collected in the aggregator
   * fields and wizard steps and fetches or creates the associated context
   * 
   * @param map
   * 
   * @return
   * @throws JAXBException
   */
  public List<AOpenbisSample> prepareEntities(Map<Object, Integer> map, boolean copy)
      throws JAXBException {
    prepareBasics();
    this.factorMap = new HashMap<String, Property>();
    experiments = new ArrayList<OpenbisExperiment>();
    species = s2.getSpecies();
    speciesInfo = s2.getSpecialSpecies();
    bioReps = s2.getBioRepAmount();

    // entities are not created new, but parsed from registered ones
    if (inheritEntities) {
      openbisEntities = openbis.getSamplesofExperiment(s1.getExperiment().getID());
      entities = parseEntities(openbisEntities, copy);
      // create new entities and an associated experiment from collected inputs
    } else {
      int personID = -1;
      String person = s2.getPerson();
      Map<String, Object> props = new HashMap<String, Object>();
      if (!s2.getExpNameField().getValue().isEmpty())
        props.put("Q_SECONDARY_NAME", s2.getExpNameField().getValue());
      if (person != null && !person.isEmpty())
        personID = personMap.get(person);
      experiments.add(new OpenbisExperiment(buildExperimentName(),
          ExperimentType.Q_EXPERIMENTAL_DESIGN, personID, props));

      List<List<Property>> valueLists = s3.getFactors();
      bioFactors = createFactorInfo(valueLists);

      entities = buildEntities(map);
    }
    return entities;
  }

  /**
   * Creates the list of biological extracts from the input information collected in the aggregator
   * fields and wizard steps and fetches or creates the associated context
   * 
   * @param map
   * @param copyMode
   * 
   * @return
   * @throws JAXBException
   */
  public List<AOpenbisSample> prepareExtracts(Map<Object, Integer> map, boolean copyMode)
      throws JAXBException {
    // extracts are not created new, but parsed from registered ones
    if (inheritExtracts) {
      if (copyMode) {
        // child experiment of entities
        String expID = existingSamples.get(openbisEntities.get(0).getCode()).getChildren().get(0)
            .getExperimentIdentifierOrNull();
        List<Sample> samples = openbis.getSamplesofExperiment(expID);
        Map<Sample, List<String>> parentMap = getParentMap(samples);
        List<AOpenbisSample> oldExtracts = parseExtracts(samples, parentMap);
        Set<String> entityCodes = new HashSet<String>();
        for (AOpenbisSample e : entities)
          entityCodes.add(e.getCode());
        extracts = new ArrayList<AOpenbisSample>();
        for (AOpenbisSample s : oldExtracts) {
          boolean orphan = true;
          String parentString = s.getParent();
          for (String p : parentString.split(" ")) {
            String newP = oldCodesToNewCodes.get(p);
            if (entityCodes.contains(newP)) {
              orphan = false;
              parentString = parentString.replace(p, newP);
            } else {
              parentString = parentString.replace(p, " ");
            }
          }
          if (!orphan) {
            incrementOrCreateBarcode();
            String newCode = nextBarcode;
            oldCodesToNewCodes.put(s.getCode(), newCode);
            s.setCode(newCode);
            s.setParent(parentString);
            extracts.add(s);
          }
        }
      } else {
        prepareBasics();
        this.factorMap = new HashMap<String, Property>();
        experiments = new ArrayList<OpenbisExperiment>();

        List<Sample> samples = openbis.getSamplesofExperiment(s1.getExperiment().getID());
        extracts = parseExtracts(samples, getParentMap(samples));
      }
      // create new entities and an associated experiment from collected inputs
    } else {
      tissue = s5.getTissue();
      specialTissue = s5.getCellLine();
      if ("Other".equals(tissue))
        specialTissue = s5.getSpecialTissue();
      extractReps = s5.getExtractRepAmount();
      int personID = -1;
      String person = s5.getPerson();
      if (person != null && !person.isEmpty())
        personID = personMap.get(person);
      Map<String, Object> props = new HashMap<String, Object>();
      if (!s5.getExpNameField().getValue().isEmpty())
        props.put("Q_SECONDARY_NAME", s5.getExpNameField().getValue());
      experiments.add(new OpenbisExperiment(buildExperimentName(),
          ExperimentType.Q_SAMPLE_EXTRACTION, personID, props));
      List<List<Property>> valueLists = s6.getFactors();
      extractFactors = createFactorInfo(valueLists);
      // keep track of id letters for different conditions
      classChars = new HashMap<String, Character>();
      extracts = buildExtracts(entities, classChars, map);
    }
    return extracts;
  }


  // TODO move this to openbisclient and remove from here and barcodecontroller
  protected Map<Sample, List<String>> getParentMap(List<Sample> samples) {
    List<String> codes = new ArrayList<String>();
    for (Sample s : samples) {
      codes.add(s.getCode());
    }
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("codes", codes);
    QueryTableModel resTable = openbis.getAggregationService("get-parentmap", params);
    Map<String, List<String>> parentMap = new HashMap<String, List<String>>();

    for (Serializable[] ss : resTable.getRows()) {
      String code = (String) ss[0];
      String parent = (String) ss[1];
      if (parentMap.containsKey(code)) {
        List<String> parents = parentMap.get(code);
        parents.add(parent);
        parentMap.put(code, parents);
      } else {
        parentMap.put(code, new ArrayList<String>(Arrays.asList(parent)));
      }
    }
    Map<Sample, List<String>> res = new HashMap<Sample, List<String>>();
    for (Sample s : samples) {
      List<String> prnts = parentMap.get(s.getCode());
      if (prnts == null)
        prnts = new ArrayList<String>();
      res.put(s, prnts);
    }
    return res;
  }


  /**
   * Creates the list of samples prepared for testing from the input information collected in the
   * aggregator fields and wizard steps and fetches or creates the associated context
   * 
   * @return
   */
  public List<List<AOpenbisSample>> prepareTestSamples() {
    techTypeInfo = s8.getAnalyteInformation();
    if (inheritExtracts) {
      prepareBasics();
      classChars = new HashMap<String, Character>();
      experiments = new ArrayList<OpenbisExperiment>();
    }
    for (TestSampleInformation x : techTypeInfo) {
      int personID = -1;
      String person = x.getPerson();
      if (person != null && !person.isEmpty())
        personID = personMap.get(person);
      OpenbisExperiment exp = new OpenbisExperiment(buildExperimentName(),
          ExperimentType.Q_SAMPLE_PREPARATION, personID, null);// TODO add secondary name here
      experiments.add(exp);
    }
    List<List<AOpenbisSample>> techSortedTests = buildTestSamples(extracts, classChars);
    tests = new ArrayList<AOpenbisSample>();
    for (List<AOpenbisSample> group : techSortedTests)
      tests.addAll(group);
    for (int i = techSortedTests.size() - 1; i > -1; i--) {
      if (!techTypeInfo.get(i).isPooled() && !s8.hasComplexProteinPoolBeforeFractionation())
        techSortedTests.remove(i);
    }
    return techSortedTests;
  }

  /**
   * Creates the list of MHC ligand extract samples prepared for ms from the input information
   * collected in the aggregator fields and wizard steps and fetches or creates the associated
   * context. These are between the test sample and ms sample layer and carry a standard barcode!
   * 
   * @return
   */
  public List<AOpenbisSample> prepareMHCExtractSamplesAndExperiments() {
    mhcExperimentProtocols = s8.getMHCLigandExtractProperties();
    Map<String, MHCLigandExtractionProtocol> antibodyInfos = s8.getAntibodyInfos();

    mhcExtracts = new ArrayList<AOpenbisSample>();

    for (String derivedFrom : mhcExperimentProtocols.keySet()) {
      String expCode = buildExperimentName();
      Map<String, Object> currentProtocol = mhcExperimentProtocols.get(derivedFrom);
      // currentProtocol.put("Code", expCode); //TODO shouldn't be needed
      experiments.add(
          new OpenbisExperiment(expCode, ExperimentType.Q_MHC_LIGAND_EXTRACTION, currentProtocol));

      List<AOpenbisSample> extracts =
          buildMHCExtractSamples(tests, classChars, derivedFrom, antibodyInfos.get(derivedFrom));
      mhcExtracts.addAll(extracts);
    }

    return mhcExtracts;
  }

  // public Map<String, Map<String, Object>> getMHCLigandExtractProperties() {
  // return mhcExperimentProtocols;
  // }

  /**
   * Build and return a list of all possible MHC ligand extracts, using existing test samples and
   * the number of antibody columns used in the experiment.
   * 
   * @param tests prepared ligands (test samples) these MHC ligand extracts will be prepared from
   *        (and attached to)
   * @param classChars Filled map of different class letters used for the tests
   * @return List of lists of AOpenbisSamples containing MHC ligand extract samples
   */
  private List<AOpenbisSample> buildMHCExtractSamples(List<AOpenbisSample> tests,
      Map<String, Character> classChars, String tissueCode, MHCLigandExtractionProtocol protocol) {
    List<AOpenbisSample> mhcExtracts = new ArrayList<AOpenbisSample>();
    int expNum = experiments.size() - 1;
    for (AOpenbisSample s : tests) {
      String type = s.getValueMap().get("Q_SAMPLE_TYPE");
      if (type.equals("CELL_LYSATE")) {
        if (s.getParent().equals(tissueCode)) {
          String secondaryName = s.getQ_SECONDARY_NAME();
          for (String antibody : protocol.getAntibodies()) {
            String[] mhcClasses = protocol.getMHCClass(antibody);
            for (String mhcClass : mhcClasses) {
              if (classChars.containsKey(secondaryName)) { // TODO see other sample creation
                classChar = classChars.get(secondaryName);
              } else {
                classChar = SampleCodeFunctions.incrementUppercase(classChar);
                classChars.put(secondaryName, classChar);
              }
              incrementOrCreateBarcode();
              mhcExtracts.add(new OpenbisMHCExtractSample(nextBarcode, spaceCode,
                  experiments.get(expNum).getExperimentCode(), secondaryName, "", s.getFactors(),
                  antibody, mhcClass, s.getCode(), s.getQ_EXTERNALDB_ID()));
            }
          }
        }
      }
    }
    return mhcExtracts;
  }

  /**
   * Set the list of biological entities (e.g. after filtering it) used in further steps
   * 
   * @param entities
   */
  public void setEntities(List<AOpenbisSample> entities) {
    this.entities = entities;
  }

  /**
   * Set the list of sample extracts (e.g. after filtering it) used in further steps
   * 
   * @param extracts
   */
  public void setExtracts(List<AOpenbisSample> extracts) {
    this.extracts = extracts;
  }

  /**
   * Set the list of test samples
   * 
   * @param tests
   */
  public void setTests(List<AOpenbisSample> tests) {
    this.tests = tests;
  }

  /**
   * Collects conditions as Strings in a list of their instance lists. Also puts the conditions
   * (factors) in a HashMap for later lookup, using value and unit as a key
   * 
   * @param factors List of a list of condition instances (one list per condition)
   * @return List of a list of condition instances (one list per condition) as Strings
   */
  private List<List<String>> createFactorInfo(List<List<Property>> factors) {
    List<List<String>> res = new ArrayList<List<String>>();
    for (List<Property> instances : factors) {
      List<String> factorValues = new ArrayList<String>();
      for (Property f : instances) {
        String name = f.getValue();
        if (f.hasUnit())
          name = name + " " + f.getUnit().getValue();
        factorValues.add(name);
        factorMap.put(name, f);
      }
      res.add(factorValues);
    }
    return res;
  }

  /**
   * Builds an experiment name from the current unused id and increments the id
   * 
   * @return
   */
  private String buildExperimentName() {
    firstFreeExperimentID++;
    return projectCode + "E" + (firstFreeExperimentID - 1);
  }

  /**
   * Generates all permutations of a list of experiment conditions
   * 
   * @param lists Instance lists of different conditions
   * @return List of all possible permutations of the input conditions
   */
  public List<String> generatePermutations(List<List<String>> lists) {
    List<String> res = new ArrayList<String>();
    generatePermutationsHelper(lists, res, 0, "");
    return res;
  }

  /**
   * recursive helper
   */
  private void generatePermutationsHelper(List<List<String>> lists, List<String> result, int depth,
      String current) {
    String separator = "###";
    if (depth == lists.size()) {
      result.add(current);
      return;
    }
    for (int i = 0; i < lists.get(depth).size(); ++i) {
      if (current.equals(""))
        separator = "";
      generatePermutationsHelper(lists, result, depth + 1,
          current + separator + lists.get(depth).get(i));
    }
  }

  /**
   * Build and return a list of all possible biological entities given their conditions, keep track
   * of conditions in a HashMap for later
   * 
   * @param map
   * 
   * @return List of AOpenbisSamples containing entity samples
   */
  private List<AOpenbisSample> buildEntities(Map<Object, Integer> map) {
    List<AOpenbisSample> entities = new ArrayList<AOpenbisSample>();
    List<List<String>> factorLists = new ArrayList<List<String>>();
    factorLists.addAll(bioFactors);
    List<String> permutations = generatePermutations(factorLists);
    List<List<String>> permLists = new ArrayList<List<String>>();
    for (String concat : permutations) {
      permLists.add(new ArrayList<String>(Arrays.asList(concat.split("###"))));
    }
    int entityNum = firstFreeEntityID;
    int defBioReps = bioReps;
    int permID = 0;
    for (List<String> secondaryNameList : permLists) {
      permID++;
      String secondaryName = nameListToSecondaryName(secondaryNameList);
      if (map.containsKey(permID))
        defBioReps = map.get(permID);
      for (int i = defBioReps; i > 0; i--) {
        List<Property> factors = new ArrayList<Property>();
        for (String name : secondaryNameList) {
          if (factorMap.containsKey(name))
            factors.add(factorMap.get(name));
        }
        if (s2.speciesIsFactor()) {
          for (String factor : secondaryNameList) {
            if (taxMap.containsKey(factor))
              species = factor;
          }
        }
        String taxID = taxMap.get(species);
        entities.add(new OpenbisBiologicalEntity(projectCode + "ENTITY-" + entityNum, spaceCode,
            experiments.get(0).getExperimentCode(), secondaryName, "", factors, taxID, speciesInfo,
            ""));
        entityNum++;
      }
    }
    return entities;
  }

  /**
   * Build and return a list of all possible biological extracts given their conditions, using
   * existing entities. Keep track of condition in a HashMap for later
   * 
   * @param entities Existing (or prepared) biological entity samples these extracts will be
   *        attached to
   * @param classChars Empty map of different class letters used for the identifiers, to keep track
   *        of for test samples
   * @param map
   * @return List of AOpenbisSamples containing extract samples
   */
  private List<AOpenbisSample> buildExtracts(List<AOpenbisSample> entities,
      Map<String, Character> classChars, Map<Object, Integer> map) {
    int expNum = experiments.size() - techTypeInfo.size() - 1;
    List<AOpenbisSample> extracts = new ArrayList<AOpenbisSample>();
    int permID = 0;
    Map<String, TissueInfo> specialTissueInfos = s6.getSpecialTissueMap();
    for (AOpenbisSample e : entities) {
      List<List<String>> factorLists = new ArrayList<List<String>>();
      String secName = e.getQ_SECONDARY_NAME();
      if (secName == null)
        secName = "";
      factorLists.add(new ArrayList<String>(Arrays.asList(secName)));

      factorLists.addAll(extractFactors);
      List<String> permutations = generatePermutations(factorLists);
      List<List<String>> permLists = new ArrayList<List<String>>();
      for (String concat : permutations) {
        permLists.add(new ArrayList<String>(Arrays.asList(concat.split("###"))));
      }
      for (List<String> secondaryNameList : permLists) {
        permID++;
        List<Property> factors = new ArrayList<Property>();
        factors.addAll(e.getFactors());
        for (String name : secondaryNameList)
          for (String element : name.split(";")) {
            element = element.trim();
            if (factorMap.containsKey(element)) {
              if (!factors.contains(factorMap.get(element)))
                factors.add(factorMap.get(element));
            }
          }
        String secondaryName = nameListToSecondaryName(secondaryNameList);
        int defExtrReps = extractReps;
        if (map.containsKey(permID))
          defExtrReps = map.get(permID);
        for (int i = defExtrReps; i > 0; i--) {
          String tissueCode = "";
          if (s5.isTissueFactor()) {
            for (String factorInstance : secondaryNameList) {
              if (specialTissueInfos.containsKey(factorInstance)) {
                TissueInfo info = specialTissueInfos.get(factorInstance);
                tissue = info.getPrimary();
                specialTissue = info.getSpecific();
              }
            }
          }
          tissueCode = tissueMap.get(tissue);
          if (classChars.containsKey(secondaryName)) { // TODO does this seem right to you?
            classChar = classChars.get(secondaryName);
          } else {
            classChar = SampleCodeFunctions.incrementUppercase(classChar);
            classChars.put(secondaryName, classChar);
          }
          List<Property> curFactors = new ArrayList<Property>(factors);
          incrementOrCreateBarcode();
          extracts.add(new OpenbisBiologicalSample(nextBarcode, spaceCode,
              experiments.get(expNum).getExperimentCode(), secondaryName, "", curFactors,
              tissueCode, specialTissue, e.getCode(), e.getQ_EXTERNALDB_ID())); // TODO
          // ext
          // db
          // id
        }
      }
    }
    return extracts;

  }

  private void incrementOrCreateBarcode() {
    if (nextBarcode == null) {
      if (firstFreeBarcode.isEmpty()) {
        classChar = 'A';
        String base = projectCode + SampleCodeFunctions.createCountString(1, 3) + classChar;
        firstFreeBarcode = base + SampleCodeFunctions.checksum(base);
      }
      nextBarcode = firstFreeBarcode;
    } else {
      nextBarcode = SampleCodeFunctions.incrementSampleCode(nextBarcode);
    }
  }

  public List<AOpenbisSample> getTestPools() {
    return testPools;
  }

  public List<AOpenbisSample> createPoolingSamples(Map<String, List<AOpenbisSample>> pools) {
    if (pools.size() > 0) {
      AOpenbisSample dummy = pools.values().iterator().next().get(0);
      boolean extracts = dummy instanceof OpenbisBiologicalSample;
      if (extracts)
        extractPools = new ArrayList<AOpenbisSample>();
      else
        testPools = new ArrayList<AOpenbisSample>();
      String exp = dummy.getValueMap().get("EXPERIMENT");
      List<Property> factors = new ArrayList<Property>();
      for (String secName : pools.keySet()) {
        incrementOrCreateBarcode();
        String parents = "";
        for (AOpenbisSample s : pools.get(secName)) {
          parents += s.getCode() + " ";
        }
        parents = parents.trim();
        if (extracts) {
          extractPools.add(new OpenbisBiologicalSample(nextBarcode, spaceCode, exp, secName, "",
              factors, "Other", "", parents, "")); // TODO ext db id
        } else {
          String type = dummy.getValueMap().get("Q_SAMPLE_TYPE");
          testPools.add(new OpenbisTestSample(nextBarcode, spaceCode, exp, secName, "", factors,
              type, parents, "")); // TODO ext db id
        }
      }
      if (extracts)
        return extractPools;
      else
        return testPools;
    }
    return new ArrayList<AOpenbisSample>();
  }

  /**
   * Build and return a list of all possible sample preparations (test samples), using existing
   * extracts.
   * 
   * @param extracts Existing (or prepared) sample extracts these test samples will be attached to
   * @param classChars Filled map of different class letters used for the extracts
   * @return List of lists of AOpenbisSamples containing test samples, sorted by different
   *         technology types
   */
  private List<List<AOpenbisSample>> buildTestSamples(List<AOpenbisSample> extracts,
      Map<String, Character> classChars) {
    List<List<AOpenbisSample>> tests = new ArrayList<List<AOpenbisSample>>();
    for (int j = 0; j < techTypeInfo.size(); j++) {// different technologies
      List<AOpenbisSample> techTests = new ArrayList<AOpenbisSample>();
      int techReps = techTypeInfo.get(j).getReplicates();
      String sampleType = techTypeInfo.get(j).getTechnology();
      int expNum = experiments.size() - techTypeInfo.size() + j;
      for (AOpenbisSample s : extracts) {
        for (int i = techReps; i > 0; i--) {
          String secondaryName = s.getQ_SECONDARY_NAME();
          if (classChars.containsKey(secondaryName)) { // TODO see above
            classChar = classChars.get(secondaryName);
          } else {
            classChar = SampleCodeFunctions.incrementUppercase(classChar);
            classChars.put(secondaryName, classChar);
          }
          incrementOrCreateBarcode();
          techTests.add(new OpenbisTestSample(nextBarcode, spaceCode,
              experiments.get(expNum).getExperimentCode(), secondaryName, "", s.getFactors(),
              sampleType, s.getCode(), s.getQ_EXTERNALDB_ID()));// TODO
          // ext
          // db
          // id
        }
      }
      tests.add(techTests);
    }
    return tests;
  }

  /**
   * parse secondary name from a list of condition permutations
   * 
   * @param secondaryNameList
   * @return
   */
  private String nameListToSecondaryName(List<String> secondaryNameList) {
    String res = secondaryNameList.toString().replace(", ", " ; ");
    return res.substring(1, res.length() - 1);
  }

  /**
   * set flag denoting the inheritance from entities existing in the system
   * 
   * @param inherit
   */
  public void setInheritEntities(boolean inherit) {
    this.inheritEntities = inherit;
  }

  /**
   * set flag denoting the inheritance from extracts existing in the system
   * 
   * @param inherit
   */
  public void setInheritExtracts(boolean inherit) {
    this.inheritExtracts = inherit;
  }

  /**
   * Parse existing entities from the system. They are assumed to be of the same experiment!
   * 
   * @param entities List of biological entities in the form of openBIS Samples
   * @param copy specifies if the samples should be copied (get a new barcode after) parsing
   * @return List of AOpenbisSamples containing entities
   * @throws JAXBException
   */
  private List<AOpenbisSample> parseEntities(List<Sample> entities, boolean copy)
      throws JAXBException {
    oldCodesToNewCodes = new HashMap<String, String>();
    List<AOpenbisSample> res = new ArrayList<AOpenbisSample>();
    String[] eSplit = entities.get(0).getExperimentIdentifierOrNull().split("/");
    String exp = eSplit[eSplit.length - 1];
    int entityNum = firstFreeEntityID;
    for (Sample s : entities) {
      String code = s.getCode();
      if (copy) {
        code = projectCode + "ENTITY-" + entityNum;
        oldCodesToNewCodes.put(s.getCode(), code);
        entityNum++;
      }
      Map<String, String> p = s.getProperties();
      List<Property> factors = new ArrayList<>();

      for (String label : oldFactors) {
        Property f = oldFactorsForSamples.get(new ImmutablePair<>(label, code));
        if (f != null) {
          factors.add(f);
          String name = f.getValue();
          if (f.hasUnit()) {
            name += f.getUnit();
          }
          factorMap.put(name, f);
        }
      }

      // List<Property> factors =
      // xmlParser.getExpFactors(xmlParser.parseXMLString(p.get("Q_PROPERTIES")));
      // for (Property f : factors) {
      // String name = f.getValue() + f.getUnit();
      // factorMap.put(name, f);
      // }
      res.add(new OpenbisBiologicalEntity(code, spaceCode, exp, p.get("Q_SECONDARY_NAME"),
          p.get("Q_ADDITIONAL_INFO"), factors, p.get("Q_NCBI_ORGANISM"),
          p.get("Q_ORGANISM_DETAILED"), p.get("Q_EXTERNALDB_ID")));
    }
    return res;
  }

  /**
   * Parse existing extracts from the system. They are assumed to be of the same experiment!
   * 
   * @param parentMap
   * 
   * @param entities List of biological extracts in the form of openBIS Samples
   * @return List of AOpenbisSamples containing extracts
   * @throws JAXBException
   */
  private List<AOpenbisSample> parseExtracts(List<Sample> extracts,
      Map<Sample, List<String>> childParentsMap) throws JAXBException {
    List<AOpenbisSample> res = new ArrayList<AOpenbisSample>();
    String[] eSplit = extracts.get(0).getExperimentIdentifierOrNull().split("/");
    String exp = eSplit[eSplit.length - 1];
    for (Sample s : extracts) {
      String code = s.getCode();
      Map<String, String> p = s.getProperties();

      List<Property> factors = new ArrayList<>();

      for (String label : oldFactors) {
        Property f = oldFactorsForSamples.get(new ImmutablePair<>(label, code));
        if (f != null) {
          factors.add(f);
          String name = f.getValue();
          if (f.hasUnit()) {
            name += f.getUnit();
          }
          factorMap.put(name, f);
        }
      }
      // List<Property> factors =
      // xmlParser.getExpFactors(xmlParser.parseXMLString(p.get("Q_PROPERTIES")));
      // for (Property f : factors) {
      // String name = f.getValue() + f.getUnit();
      // factorMap.put(name, f);
      // }
      res.add(new OpenbisBiologicalSample(code, spaceCode, exp, p.get("Q_SECONDARY_NAME"),
          p.get("Q_ADDITIONAL_INFO"), factors, p.get("Q_PRIMARY_TISSUE"),
          p.get("Q_TISSUE_DETAILED"), parseParents(s, childParentsMap), p.get("Q_EXTERNALDB_ID")));
    }
    return res;
  }

  /**
   * Parse existing test samples from the system
   * 
   * @param tests List of test samples in the form of openBIS Samples
   * @param parentMap
   * @return List of AOpenbisSamples containing test samples
   * @throws JAXBException
   */
  private List<AOpenbisSample> parseTestSamples(List<Sample> tests,
      Map<Sample, List<String>> childToParentsMap) throws JAXBException {
    List<AOpenbisSample> res = new ArrayList<AOpenbisSample>();
    for (Sample s : tests) {
      String code = s.getCode();
      String[] eSplit = s.getExperimentIdentifierOrNull().split("/");
      Map<String, String> p = s.getProperties();
      List<Property> factors = new ArrayList<>();

      for (String label : oldFactors) {
        Property f = oldFactorsForSamples.get(new ImmutablePair<>(label, code));
        if (f != null) {
          factors.add(f);
          String name = f.getValue();
          if (f.hasUnit()) {
            name += f.getUnit();
          }
          factorMap.put(name, f);
        }
      }
      // List<Property> factors =
      // xmlParser.getExpFactors(xmlParser.parseXMLString(p.get("Q_PROPERTIES")));
      // for (Property f : factors) {
      // String name = f.getValue() + f.getUnit();
      // factorMap.put(name, f);
      // }
      res.add(new OpenbisTestSample(code, spaceCode, eSplit[eSplit.length - 1],
          p.get("Q_SECONDARY_NAME"), p.get("Q_ADDITIONAL_INFO"), factors, p.get("Q_SAMPLE_TYPE"),
          parseParents(s, childToParentsMap), p.get("Q_EXTERNALDB_ID")));
    }
    return res;
  }

  /**
   * Get the parents of a sample give its code and return them space delimited so they can be added
   * to a tsv
   * 
   * @param parentMap
   * 
   * @param code
   * @return
   */
  private String parseParents(Sample sample, Map<Sample, List<String>> childParentsMap) {
    if (childParentsMap != null && childParentsMap.containsKey(sample))
      return StringUtils.join(childParentsMap.get(sample), " ");
    else {
      List<String> codes = new ArrayList<String>();
      for (Sample s : sample.getParents())
        codes.add(s.getCode());
      return StringUtils.join(codes, " ");
    }
  }

//  /**
//   * Copy a list of samples, used by the copy context function
//   * 
//   * @param samples
//   * @param copies
//   * @return
//   */
//  private List<AOpenbisSample> copySamples(List<AOpenbisSample> samples,
//      Map<String, String> copies) {
//    String newExp = buildExperimentName();
//    String type = samples.get(0).getValueMap().get("SAMPLE TYPE");
//    ExperimentType eType = ExperimentType.Q_EXPERIMENTAL_DESIGN;
//    if (type.equals("Q_BIOLOGICAL_ENTITY"))
//      eType = ExperimentType.Q_EXPERIMENTAL_DESIGN;
//    else if (type.equals("Q_BIOLOGICAL_SAMPLE"))
//      eType = ExperimentType.Q_SAMPLE_EXTRACTION;
//    else if (type.equals("Q_TEST_SAMPLE"))
//      eType = ExperimentType.Q_SAMPLE_PREPARATION;
//    else
//      logger.error("Unexpected type: " + type);
//    experiments.add(new OpenbisExperiment(newExp, eType, -1, null));// TODO secondary name?
//
//    for (AOpenbisSample s : samples) {
//      s.setExperiment(newExp);
//      String code = s.getCode();
//      String newCode = code;
//      if (s instanceof OpenbisBiologicalEntity) {
//        newCode = projectCode + "ENTITY-" + firstFreeEntityID;
//        firstFreeEntityID++;
//      } else {
//        if (nextBarcode == null) {
//          // classChar = 'A';
//          // nextBarcode =
//          // projectCode + Functions.createCountString(firstFreeBarcodeID, 3) + classChar;
//          // nextBarcode = nextBarcode + Functions.checksum(nextBarcode);
//          nextBarcode = firstFreeBarcode;
//        } else {
//          nextBarcode = SampleCodeFunctions.incrementSampleCode(nextBarcode);
//        }
//        newCode = nextBarcode;
//      }
//      copies.put(code, newCode);
//      s.setCode(newCode);
//      String p = s.getParent();
//      // change parent if parent was copied
//      if (p != null && p.length() > 0)
//        if (copies.containsKey(p))
//          s.setParent(copies.get(p));
//    }
//    return samples;
//  }

  /**
   * Gets all samples that are one level higher in the sample hierarchy of an attached experiment
   * than a given list of samples
   * 
   * @param originals
   * @return
   */
  private List<Sample> getUpperSamples(List<Sample> originals) {
    for (Sample s : originals) {
      List<Sample> parents = openbis.getParentsBySearchService(s.getCode());
      if (parents.size() > 0) {
        return openbis.getSamplesofExperiment(parents.get(0).getExperimentIdentifierOrNull());
      }
    }
    return null;
  }

//  /**
//   * Gets all samples that are one level lower in the sample hierarchy of an attached experiment
//   * than a given list of samples
//   * 
//   * @param originals
//   * @return
//   */
//  private List<Sample> getLowerSamples(List<Sample> originals) {
//    for (Sample s : originals) {
//      List<Sample> children = openbis.getChildrenSamples(s);
//      if (children.size() > 0) {
//        return openbis.getSamplesofExperiment(children.get(0).getExperimentIdentifierOrNull());
//      }
//    }
//    return null;
//  }

  /**
   * Creates a tab separated values file of the context created by the wizard, given that samples
   * have been prepared in the aggregator class
   * 
   * @return
   * @throws FileNotFoundException
   * @throws UnsupportedEncodingException
   */
  public String createTSV() throws FileNotFoundException, UnsupportedEncodingException {
    List<AOpenbisSample> samples = new ArrayList<AOpenbisSample>();
    samples.addAll(entities);
    samples.addAll(extracts);
    samples.addAll(tests);
    if (testPools != null)
      samples.addAll(testPools);
    if (msSamples != null)
      samples.addAll(msSamples);// TODO test
    if (mhcExtracts != null)
      samples.addAll(mhcExtracts);
    List<String> rows = new ArrayList<String>();

    List<String> header = new ArrayList<String>(Arrays.asList("SAMPLE TYPE", "SPACE", "EXPERIMENT",
        "Q_SECONDARY_NAME", "PARENT", "Q_PRIMARY_TISSUE", "Q_TISSUE_DETAILED", "Q_ADDITIONAL_INFO",
        "Q_NCBI_ORGANISM", "Q_SAMPLE_TYPE", "Q_EXTERNALDB_ID"));
    // TODO current assumption: tests should have more or an equal number of xml entries than
    // ancestors, because they inherit their entries
    int factorRowSize = 0;
    AOpenbisSample a = samples.get(0);
    for (AOpenbisSample b : samples) {
      if (factorRowSize < b.getFactors().size()) {
        factorRowSize = b.getFactors().size();
        a = b;
      }
    }
    String description = s1.getDescription();
    String secondaryName = s1.getExpSecondaryName();
    String investigator = s1.getPerson(PersonType.Investigator);
    String contact = s1.getPerson(PersonType.Contact);
    String manager = s1.getPerson(PersonType.Manager);

    String result = "";
    description = description.replace("\n", "\n#");
    secondaryName = secondaryName.replace("\n", " - ");
    result += "#PROJECT_DESCRIPTION=" + description + "\n";
    result += "#ALTERNATIVE_NAME=" + secondaryName + "\n";
    if (s1.isPilot())
      result += "#PILOT PROJECT\n";
    result += "#INVESTIGATOR=" + investigator + "\n";
    result += "#CONTACT=" + contact + "\n";
    result += "#MANAGER=" + manager + "\n";

    // TODO reuse this in the refactored version, it's not stupid
    if (experiments != null) {
      for (OpenbisExperiment e : experiments) {
        if (informativeExpTypes.contains(e.getType()) || e.containsProperties()) {
          result += e.getPropertiesString() + "\n";
        }
      }
    }
    // Map<String, Object> msProps = s8.getProteinMSExperimentProperties(); TODO might need this for
    // non-fractionation experiments
    // if (msProps != null)
    // result += addExperimentInfoLine(msProps, "Q_MS_MEASUREMENT") + "\n";
    if (mhcExperimentProtocols != null) {
      header.add("Q_ANTIBODY");
      header.add("Q_MHC_CLASS");
    }

    String headerLine = "Identifier";
    for (String col : header)
      headerLine += "\t" + col;

    for (Property f : a.getFactors()) {
      String label = f.getLabel();
      switch (f.getType()) {
        case Factor:
          headerLine += "\tCondition: " + label;
          break;
        case Property:
          headerLine += "\tProperty: " + label;
          break;
        default:
          break;
      }
    }
    for (AOpenbisSample s : samples) {
      String code = s.getCode();
      if (isEntity(code) || SampleCodeFunctions.isQbicBarcode(code)
          || SampleCodeFunctions.isMeasurementOfBarcode(code, s.getValueMap().get("SAMPLE TYPE"))) {
        Map<String, String> data = s.getValueMap();
        String row = s.getCode();
        List<String> factors = s.getFactorStringsWithoutLabel();
        for (String col : header) {
          String val = "";
          if (data.containsKey(col))
            val = data.get(col);
          if (val == null)
            val = "";
          row += "\t" + val;
        }
        for (int i = 0; i < factors.size(); i++)
          row += "\t" + factors.get(i);
        for (int i = factors.size(); i < factorRowSize; i++) {
          row += "\t";
        }
        rows.add(row);
      } else {
        logger.warn(
            code + " will be ignored, it is not a valid QBiC barcode registerable by the wizard.");
      }
    }
    result += headerLine + "\n";
    for (String line : rows) {
      result += line + "\n";
    }
    this.tsvContent = result;
    return result;
  }

  // TODO should be parsed from the tsv?
  public List<OpenbisExperiment> getExperimentsWithMetadata() {
    List<OpenbisExperiment> res = new ArrayList<OpenbisExperiment>();
    for (OpenbisExperiment e : experiments) {
      if (informativeExpTypes.contains(e.getType()) || e.containsProperties()) {
        res.add(e);
      }
    }
    return res;
  }

  private static boolean isEntity(String code) {
    String pattern = "Q[A-Z0-9]{4}ENTITY-[0-9]+";
    return code.matches(pattern);
  }

  public File getTSV() throws FileNotFoundException, UnsupportedEncodingException {
    String file = ProjectWizardUI.tmpFolder + "tmp_" + getTSVName() + ".tsv";
    PrintWriter writer = new PrintWriter(file, "UTF-8");
    for (String line : tsvContent.split("\n")) {
      writer.println(line);
    }
    writer.close();
    return new File(file);
  }

  public String getTSVName() {
    return spaceCode + "_" + projectCode;
  }

  public String getTSVContent() {
    return tsvContent;
  }

  public List<AOpenbisSample> getEntities() {
    return entities;
  }

  public void parseAll() throws JAXBException {
    prepareBasics();
    factorMap = new HashMap<String, Property>();

    List<Sample> openbisEntities = new ArrayList<Sample>();
    List<Sample> openbisExtracts = new ArrayList<Sample>();
    List<Sample> openbisTests = new ArrayList<Sample>();

    List<Sample> allSamples =
        openbis.getSamplesWithParentsAndChildrenOfProjectBySearchService(projectCode);
    for (Sample sa : allSamples) {
      String type = sa.getSampleTypeCode();
      switch (type) {
        case "Q_BIOLOGICAL_ENTITY":
          openbisEntities.add(sa);
          break;
        case "Q_BIOLOGICAL_SAMPLE":
          openbisExtracts.add(sa);
          break;
        case "Q_TEST_SAMPLE":
          openbisTests.add(sa);
          break;
        default:
          break;
      }
    }
    entities = parseEntities(openbisEntities, false);
    extracts = parseExtracts(openbisExtracts, null);
    tests = parseTestSamples(openbisTests, null);
  }

  public List<AOpenbisSample> getTests() {
    return tests;
  }

  public void resetExtracts() {
    extracts = new ArrayList<AOpenbisSample>();
  }

  public List<OpenbisExperiment> getExperiments() {
    return experiments;
  }

  public void setFractionationExperimentsProperties(
      MSExperimentModel fractionationPropertiesFromLastStep) {
    this.fractionationProperties = fractionationPropertiesFromLastStep;
  }

  /**
   * creates fractionation samples (proteins and or peptides) as well as samples for the attached
   * Mass Spectrometry runs should be called before creating the project spreadsheet (tsv)
   */
  public void createFractionationSamplesAndExperiments() {
    List<AOpenbisSample> res = new ArrayList<AOpenbisSample>();

    ExperimentModel exp = fractionationProperties.getBaseAnalytes();
    Map<String, Object> proteinPrepProps = s8.getProteinPreparationInformation();
    if (proteinPrepProps != null) {
      for (String key : proteinPrepProps.keySet()) {
        exp.addProperty(key, proteinPrepProps.get(key));
      }
    }
    String eName = buildExperimentName();
    experiments.add(
        new OpenbisExperiment(eName, ExperimentType.Q_SAMPLE_PREPARATION, exp.getProperties()));
    for (AOpenbisSample s : exp.getSamples()) {
      s.setExperiment(eName);
      s.setSpace(spaceCode);
      s.setSampleType("Q_TEST_SAMPLE");
      String parents = "";
      if (s.getParents() == null)
        parents = s.getParent();
      else {
        for (AOpenbisSample p : s.getParents()) {
          parents += p.getCode() + " ";
        }
        parents = parents.trim();

      }
      s.setParent(parents);
      if (s.getCode() == null) {
        incrementOrCreateBarcode();
        s.setCode(nextBarcode);
      }
      this.testPools = new ArrayList<AOpenbisSample>();
      if (!tests.contains(s))
        this.testPools.add(s);
    }

    for (List<ExperimentModel> fe : fractionationProperties.getAnalytes()) {
      for (ExperimentModel e : fe) {
        String expName = buildExperimentName();
        experiments.add(
            new OpenbisExperiment(expName, ExperimentType.Q_SAMPLE_PREPARATION, e.getProperties()));
        for (AOpenbisSample s : e.getSamples()) {
          s.setExperiment(expName);
          s.setSpace(spaceCode);
          s.setSampleType("Q_TEST_SAMPLE");
          String parents = "";
          for (AOpenbisSample p : s.getParents()) {
            parents += p.getCode() + " ";
          }
          parents = parents.trim();
          s.setParent(parents);
          if (s.getCode() == null) {
            incrementOrCreateBarcode();
            s.setCode(nextBarcode);
          }
        }
        res.addAll(e.getSamples());
      }
    }
    for (ExperimentModel e : fractionationProperties.getPeptideExperiments()) {
      String expName = buildExperimentName();
      experiments.add(
          new OpenbisExperiment(expName, ExperimentType.Q_SAMPLE_PREPARATION, e.getProperties()));
      for (AOpenbisSample s : e.getSamples()) {
        s.setExperiment(expName);
        s.setSpace(spaceCode);
        s.setSampleType("Q_TEST_SAMPLE");
        String parents = "";
        if (s.getParents() == null)
          parents = s.getParent();
        else {
          for (AOpenbisSample p : s.getParents()) {
            parents += p.getCode() + " ";
          }
          parents = parents.trim();
        }
        s.setParent(parents);
        if (s.getCode() == null) {
          incrementOrCreateBarcode();
          s.setCode(nextBarcode);
        }
      }
      res.addAll(e.getSamples());
    }
    for (List<ExperimentModel> fe : fractionationProperties.getMSRuns()) {
      for (ExperimentModel e : fe) {
        String expName = buildExperimentName();
        experiments.add(
            new OpenbisExperiment(expName, ExperimentType.Q_MS_MEASUREMENT, e.getProperties()));
        for (AOpenbisSample s : e.getSamples()) {
          s.setExperiment(expName);
          s.setSpace(spaceCode);
          s.setSampleType("Q_MS_RUN");
          String parents = "";
          for (AOpenbisSample p : s.getParents()) {
            parents += p.getCode() + " ";
          }
          parents = parents.trim();
          s.setParent(parents);
          if (parents.startsWith("MS")) {// wash runs
            if (s.getCode() == null) {
              incrementOrCreateBarcode();
              s.setCode(nextBarcode);
            }
          } else {
            s.setCode("MS" + parents);
          }
        }
        res.addAll(e.getSamples());
      }
    }
    msSamples = res;
  }

  public RegisteredAnalyteInformation getBaseAnalyteInformation() {
    // TODO replicates?
    Map<String, List<Sample>> infos = new HashMap<String, List<Sample>>();
    for (Sample s : existingSamples.values()) {
      String type = s.getSampleTypeCode();
      if (type.equals("Q_TEST_SAMPLE")) {
        Map<String, String> props = s.getProperties();
        String analyte = props.get("Q_SAMPLE_TYPE");
        if (infos.containsKey(analyte)) {
          infos.get(analyte).add(s);
        } else {
          infos.put(analyte, new ArrayList<Sample>(Arrays.asList(s)));
        }
      }
    }
    boolean measurePeptides = false;
    boolean shortGel = false;
    String purificationMethod = "";
    if (infos.containsKey("PROTEINS")) {
      Sample first = infos.get("PROTEINS").get(0);
      String expID = first.getExperimentIdentifierOrNull();
      for (Experiment e : openbis.getExperimentsOfProjectByCode(first.getCode().substring(0, 5))) {
        if (e.getIdentifier().equals(expID)) {
          Map<String, String> props = e.getProperties();
          if (props.containsKey("Q_MS_PURIFICATION_METHOD"))
            purificationMethod = props.get("Q_MS_PURIFICATION_METHOD");
          if (props.containsKey("Q_ADDITIONAL_INFORMATION"))
            shortGel = props.get("Q_ADDITIONAL_INFORMATION").contains("Short Gel");
          break;
        }
      }
      if (infos.containsKey("PEPTIDES")) {
        for (Sample s : infos.get("PEPTIDES")) {
          for (Sample p : s.getParents()) {
            if (p.getSampleTypeCode().equals("Q_TEST_SAMPLE")
                && p.getProperties().get("Q_SAMPLE_TYPE").equals("PROTEINS")) {
              measurePeptides = true;
              break;
            }
          }
          if (measurePeptides)
            break;
        }
        infos.remove("PEPTIDES");
      }
    }
    RegisteredAnalyteInformation res = new RegisteredAnalyteInformation(infos.keySet(),
        measurePeptides, shortGel, purificationMethod);
    return res;
  }

  public void setExistingExpDesignExperiment(Experiment e) {
    expDesignExperiment = e;
    if (e == null) {
      oldExpDesign = null;
      oldFactors = null;
      oldFactorsForSamples = null;
    } else {
      try {
        oldExpDesign = parser.parseXMLString(e.getProperties().get("Q_EXPERIMENTAL_SETUP"));
        oldFactors = parser.getFactorLabels(oldExpDesign);
        oldFactorsForSamples = parser.getFactorsForLabelsAndSamples(oldExpDesign);
      } catch (JAXBException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
    }
  }

  public Map<String, Map<String, Object>> getEntitiesToUpdate(
      ExperimentalDesignPropertyWrapper importedDesignProperties, List<TechnologyType> techTypes) {
    Map<String, Map<String, Object>> res = new HashMap<>();
    if (expDesignExperiment != null) {
      Map<String, String> currentProps = expDesignExperiment.getProperties();
      Map<String, Object> map =
          ParserHelpers.getExperimentalDesignMap(currentProps, importedDesignProperties, techTypes, new HashSet<>());
      final String SETUP_PROPERTY_CODE = "Q_EXPERIMENTAL_SETUP";
      String oldXML = currentProps.get(SETUP_PROPERTY_CODE);
      if (!map.get(SETUP_PROPERTY_CODE).equals(oldXML)) {
        logger.info("update of experimental design needed");
        res.put(expDesignExperiment.getCode(), map);
      } else {
        logger.info("no update of existing experimental design needed");
      }
    }
    return res;
  }
}
