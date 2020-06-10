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
package life.qbic.projectwizard.adminviews;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.experiments.OpenbisExperiment;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.expdesign.model.ExperimentalDesignPropertyWrapper;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.projectwizard.control.SampleCounter;
import life.qbic.projectwizard.registration.IOpenbisCreationController;
import life.qbic.xml.manager.StudyXMLParser;
import life.qbic.xml.study.Qexperiment;
import life.qbic.xml.study.TechnologyType;

public class CovidSampleCreation {

  private Logger logger = LogManager.getLogger(CovidSampleCreation.class);

  // private IOpenBisClient openbis;
  private IOpenbisCreationController creator;

  final private StudyXMLParser xmlParser = new StudyXMLParser();

  private List<TechnologyType> techTypes;

  private SampleCounter counter;

  public CovidSampleCreation(IOpenBisClient openbis,
      IOpenbisCreationController creationController) {
    techTypes = new ArrayList<TechnologyType>();
    techTypes.add(new TechnologyType("Transcriptomics"));
    techTypes.add(new TechnologyType("Proteomics"));
    techTypes.add(new TechnologyType("Metabolomics"));
    techTypes.add(new TechnologyType("Imaging"));

    // this.openbis = openbis;
    this.creator = creationController;
    createCovidPatients();
  }

  // T0: Stationäre aufnahme (equals T1 for Tropical Medicine)
  // T2: 2 days
  // T4: 4 days
  // T7: 7 days
  // T14: 14 days or after
  // T28: (potentially needed for Tropical Medicine (Rolf Fendel))
  // TR : 6 months or after recovery
  private List<String> getTimepoints(boolean tropical) {
    List<String> timePoints = new ArrayList<>();
    timePoints.add("T0");
    timePoints.add("T2");
    timePoints.add("T4");
    timePoints.add("T7");
    timePoints.add("T14");
    if (tropical) {
      timePoints.add("T28");
    }
    timePoints.add("TR");
    return timePoints;
  }

  public void createCovidPatients() {
//    techTypes = new ArrayList<TechnologyType>();
//    techTypes.add(new TechnologyType("Genomics"));
//    techTypes.add(new TechnologyType("Transcriptomics"));
//    techTypes.add(new TechnologyType("Proteomics"));
//    techTypes.add(new TechnologyType("Metabolomics"));
//
//    String space = "MFT_COVID-19";

    
//    List<String>  tps = getTimepoints(false);
//    String project = "Q001V";
//
//
//    counter = new SampleCounter(project);
//    createProjectWithTenPatients(space, project, "V", tps, 1, "First 10 Patients for the Covid-19 study.");

//        List<String> tps = getTimepoints(true);
//    String project = "Q001O";
//
//
//    counter = new SampleCounter(project);
//    createProjectWithTenPatients(space, project, "O", tps, 1, "First 10 outpatients for the study at the institute for tropical medicine.");
  }

  private void createProjectWithTenPatients(String space, String projectCode,
      String patientPrefix, List<String> timePoints, int firstPatientNumber, String description) {
    List<List<ISampleBean>> samples = new ArrayList<>();
    List<ISampleBean> patients = new ArrayList<>();

    Map<Pair<String, String>, List<String>> weekLevels = new HashMap<>();

    List<ISampleBean> tissues = new ArrayList<>();
    List<ISampleBean> aliquots = new ArrayList<>();
    List<ISampleBean> measurements = new ArrayList<>();

    for (int i = firstPatientNumber; i < firstPatientNumber + 10; i++) {
      ISampleBean patient = generatePatient(projectCode, patientPrefix, space, i);
      patients.add(patient);
      //TODO buggy?
      for (String tp : timePoints) {
        Pair<String, String> key = new ImmutablePair<String, String>(tp, null);

        List<List<ISampleBean>> samplesForTimepoint =
            createTimepoint(patient, projectCode, space, tp, tissues, aliquots, measurements);

        List<String> ids = new ArrayList<>();
        for (ISampleBean sa : samplesForTimepoint.stream().flatMap(List::stream)
            .collect(Collectors.toList())) {
          ids.add(sa.getCode());
        }
        if (weekLevels.containsKey(key)) {
          weekLevels.get(key).addAll(ids);
        } else {
          weekLevels.put(key, ids);
        }
      }
    }

    Map<String, Map<Pair<String, String>, List<String>>> expDesign = new HashMap<>();
    expDesign.put("timepoint", weekLevels);
    List<OpenbisExperiment> exps = new ArrayList<>();
    OpenbisExperiment designExp = getDesignExperiment(projectCode,
        new ExperimentalDesignPropertyWrapper(expDesign, new HashMap<>()));
    exps.add(designExp);

    String code = projectCode + "000";
    ISampleBean infoSample = new TSVSampleBean(code, designExp.getExperimentCode(), projectCode,
        space, SampleType.Q_ATTACHMENT_SAMPLE, "", new ArrayList<String>(),
        new HashMap<String, Object>());
    samples.add(new ArrayList<ISampleBean>(Arrays.asList(infoSample)));
    samples.add(patients);

    samples.add(tissues);
    samples.add(aliquots);
    samples.add(measurements);

    creator.registerProjectWithExperimentsAndSamplesBatchWise(samples, exps, description, false);
  }

  private OpenbisExperiment getDesignExperiment(String project,
      ExperimentalDesignPropertyWrapper wrapper) {
    Map<String, Object> props = new HashMap<>();
    String newDesignXML = "";
    try {
      JAXBElement<Qexperiment> designObject = xmlParser.createNewDesign(new HashSet<>(), techTypes,
          wrapper.getExperimentalDesign(), new HashMap<>());
      newDesignXML = xmlParser.toString(designObject);
    } catch (JAXBException e) {
      logger.error("could not create new design xml");
      e.printStackTrace();
    }
    String exp = project + "_INFO";
    props.put("Q_EXPERIMENTAL_SETUP", newDesignXML);
    return new OpenbisExperiment(exp, ExperimentType.Q_PROJECT_DETAILS, props);
  }

  private List<List<ISampleBean>> createTimepoint(ISampleBean patient, String projectCode,
      String space, String tp, List<ISampleBean> tissues, List<ISampleBean> aliquots,
      List<ISampleBean> measurements) {

    String pharynx_exp_code = projectCode + "E2";
    String pharynx_dna_exp_code = projectCode + "E3";
    String pharynx_rna_exp_code = projectCode + "E4";
    String stool_exp_code = projectCode + "E5";
    String stool_dna_exp_code = projectCode + "E6";
    String stool_rna_exp_code = projectCode + "E7";
    String pbmc_exp_code = projectCode + "E8";
    String pbmc_rna_code = projectCode + "E9";
    String serum_exp_code = projectCode + "E10";
    String serum_ptx_code = projectCode + "E11";
    String serum_mtx_code = projectCode + "E12";
    String plasma_exp_code = projectCode + "E13";
    String plasma_dna_code = projectCode + "E14";
    String pax_exp_code = projectCode + "E15";
    String pax_rna_code = projectCode + "E16";
    String urine_exp_code = projectCode + "E17";

    String idBase = (String) patient.getMetadata().get("Q_EXTERNALDB_ID");

    tp += ":";
    // Rachen
    String sputumBase = idBase + ":RA:" + tp;
    String stoolBase = idBase + ":ST:" + tp;
    String paxBase = idBase + ":PA:" + tp;
    String plasmaBase = idBase + ":PL:" + tp;
    String serumBase = idBase + ":SE:" + tp;
    String pbmcBase = idBase + ":PB:" + tp;
    String urineBase = idBase + ":UR:" + tp;

    ISampleBean sputum =
        generateTissue("Sputum", patient, sputumBase + "A0", pharynx_exp_code, projectCode, space);
    for (int i = 1; i < 11; i++) {
      String extID = sputumBase + "A" + i;
      TSVSampleBean aliquot =
          generateTissue("Sputum", sputum, extID, pharynx_exp_code, projectCode, space);
      aliquot.setSecondaryName("Sputum aliquot " + i);

//      TSVSampleBean dna = generateTestSample("DNA", aliquot, extID + ":DNA", pharynx_dna_exp_code,
//          projectCode, space);
//      TSVSampleBean rna = generateTestSample("RNA", aliquot, extID + ":RNA", pharynx_rna_exp_code,
//          projectCode, space);

      aliquots.add(aliquot);
//      measurements.add(dna);
//      measurements.add(rna);
    }
    // Stuhl
    ISampleBean stool =
        generateTissue("Feces", patient, stoolBase + "A0", stool_exp_code, projectCode, space);
    for (int i = 1; i < 11; i++) {
      String extID = stoolBase + "A" + i;
      TSVSampleBean aliquot =
          generateTissue("Feces", stool, extID, stool_exp_code, projectCode, space);
      aliquot.setSecondaryName("Stool aliquot " + i);

//      TSVSampleBean dna = generateTestSample("DNA", aliquot, extID + ":DNA", stool_dna_exp_code,
//          projectCode, space);
//      TSVSampleBean rna = generateTestSample("RNA", aliquot, extID + ":RNA", stool_rna_exp_code,
//          projectCode, space);

      aliquots.add(aliquot);
//      measurements.add(dna);
//      measurements.add(rna);
    }
    // PBMC
    ISampleBean pbmc =
        generateTissue("PBMC", patient, pbmcBase + "A0", pbmc_exp_code, projectCode, space);
    for (int i = 1; i < 11; i++) {
      String extID = pbmcBase + "A" + i;
      TSVSampleBean aliquot =
          generateTissue("PBMC", pbmc, extID, pbmc_exp_code, projectCode, space);
      aliquot.setSecondaryName("PBMC aliquot " + i);

//      TSVSampleBean rna =
//          generateTestSample("RNA", aliquot, extID + ":RNA", pbmc_rna_code, projectCode, space);

      aliquots.add(aliquot);
//      measurements.add(rna);
    }
    // Serum
    ISampleBean serum = generateTissue("Blood_serum", patient, serumBase + "A0", serum_exp_code,
        projectCode, space);
    for (int i = 1; i < 11; i++) {
      String extID = serumBase + "A" + i;
      TSVSampleBean aliquot =
          generateTissue("Blood_serum", serum, extID, serum_exp_code, projectCode, space);
      aliquot.setSecondaryName("Serum Sarstedt aliquot " + i);

      TSVSampleBean ptx = generateTestSample("PROTEINS", aliquot, extID + ":PTX", serum_ptx_code,
          projectCode, space);
      TSVSampleBean mtx = generateTestSample("SMALLMOLECULES", aliquot, extID + ":MTX",
          serum_mtx_code, projectCode, space);

      aliquots.add(aliquot);
      measurements.add(ptx);
      measurements.add(mtx);
    }
    // Plasma
    ISampleBean plasma = generateTissue("Blood_Plasma", patient, plasmaBase + "A0", plasma_exp_code,
        projectCode, space);
    for (int i = 1; i < 11; i++) {
      String extID = plasmaBase + "A" + i;
      TSVSampleBean aliquot =
          generateTissue("Blood_Plasma", plasma, extID, plasma_exp_code, projectCode, space);
      aliquot.setSecondaryName("Plasma aliquot " + i);

//      TSVSampleBean dna =
//          generateTestSample("DNA", aliquot, extID + ":DNA", plasma_dna_code, projectCode, space);

      aliquots.add(aliquot);
//      measurements.add(dna);
    }
    // Paxgene
    ISampleBean blood =
        generateTissue("Whole_Blood", patient, paxBase + "A0", pax_exp_code, projectCode, space);
    for (int i = 1; i < 11; i++) {
      String extID = paxBase + "A" + i;
      TSVSampleBean aliquot =
          generateTissue("Whole_Blood", blood, extID, pax_exp_code, projectCode, space);
      aliquot.setSecondaryName("PaxGene Blood aliquot " + i);

//      TSVSampleBean rna =
//          generateTestSample("RNA", aliquot, extID + ":RNA", pax_rna_code, projectCode, space);

      aliquots.add(aliquot);
//      measurements.add(rna);
    }
    // Urin
    ISampleBean urine =
        generateTissue("Urine", patient, urineBase + "A0", urine_exp_code, projectCode, space);
    for (int i = 1; i < 11; i++) {
      String extID = urineBase + "A" + i;
      TSVSampleBean aliquot =
          generateTissue("Urine", urine, extID, urine_exp_code, projectCode, space);
      aliquot.setSecondaryName("Urine aliquot " + i);
      aliquots.add(aliquot);
    }
    tissues.add(sputum);
    tissues.add(stool);
    tissues.add(blood);
    tissues.add(urine);
    tissues.add(pbmc);
    tissues.add(plasma);
    tissues.add(serum);

    List<List<ISampleBean>> samples = new ArrayList<>();

    samples.add(tissues);
    samples.add(aliquots);
    samples.add(measurements);

    return samples;
  }

  private ISampleBean generatePatient(String projectCode, String patientPrefix, String space, int patientNumber) {
    String patient_exp_code = projectCode + "E1";

    String secondaryName = "Patient " + patientNumber;
    String code = projectCode + "ENTITY-" + patientNumber;
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("Q_NCBI_ORGANISM", "9606");
    String extID = patientPrefix+String.format("%03d", patientNumber);
    metadata.put("Q_EXTERNALDB_ID", extID);
    return new TSVSampleBean(code, patient_exp_code, projectCode, space,
        SampleType.Q_BIOLOGICAL_ENTITY, secondaryName, new ArrayList<>(), metadata);
  }

  private TSVSampleBean generateTissue(String tissue, ISampleBean parent, String extID,
      String experimentCode, String projectCode, String space) {
    String code = counter.getNewBarcode();
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("Q_PRIMARY_TISSUE", tissue);
    metadata.put("Q_EXTERNALDB_ID", extID);
    return new TSVSampleBean(code, experimentCode, projectCode, space,
        SampleType.Q_BIOLOGICAL_SAMPLE, "", Arrays.asList(parent.getCode()), metadata);
  }

  private TSVSampleBean generateTestSample(String type, ISampleBean parent, String extID,
      String experimentCode, String projectCode, String space) {
    String code = counter.getNewBarcode();
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("Q_SAMPLE_TYPE", type);
    metadata.put("Q_EXTERNALDB_ID", extID);
    return new TSVSampleBean(code, experimentCode, projectCode, space, SampleType.Q_TEST_SAMPLE, "",
        Arrays.asList(parent.getCode()), metadata);
  }

  // private void findAndSetDesignExperiment(String space, String project) throws JAXBException {
  // designExperiment = null;
  // System.out.println("searching design experiment");
  // String id = ExperimentCodeFunctions.getInfoExperimentID(space, project);
  // List<Experiment> exps = openbis.getExperimentById2(id);
  // if (exps.isEmpty()) {
  // designExperiment = null;
  // logger.error("could not find info experiment for project" + project);
  // } else {
  // Experiment e = exps.get(0);
  // if (e.getExperimentTypeCode().equalsIgnoreCase(ExperimentType.Q_PROJECT_DETAILS.name())) {
  // designExperiment = e;
  // expDesign =
  // xmlParser.parseXMLString(designExperiment.getProperties().get("Q_EXPERIMENTAL_SETUP"));
  // logger.debug("setting exp design: " + expDesign);
  // }
  // }
  // }
  //
  // private TSVSampleBean createSample(String code, String expSuffix, SampleType sType,
  // String secondary, String extID, String type, List<String> parentIDs) {
  // if (expSuffix.contains(project)) {
  // expSuffix = expSuffix.replace(project, "");
  // }
  // Map<String, Object> metadata = new HashMap<>();
  // switch (sType) {
  // case Q_BIOLOGICAL_ENTITY:
  // metadata.put("Q_NCBI_ORGANISM", type);
  // break;
  // case Q_BIOLOGICAL_SAMPLE:
  // metadata.put("Q_PRIMARY_TISSUE", type);
  // break;
  // case Q_TEST_SAMPLE:
  // metadata.put("Q_SAMPLE_TYPE", type);
  // break;
  // case Q_BMI_GENERIC_IMAGING_RUN:
  // break;
  // default:
  // break;
  // }
  // metadata.put("Q_EXTERNALDB_ID", extID);
  // return new TSVSampleBean(code, project + expSuffix, project, mccSpace, sType, secondary,
  // parentIDs, metadata);
  // }

  public String getRegistrationError() {
    return creator.getErrors();
  }
}
