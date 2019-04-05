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
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.themes.ValoTheme;

import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Experiment;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Project;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.experiments.OpenbisExperiment;
import life.qbic.datamodel.identifiers.ExperimentCodeFunctions;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.expdesign.ParserHelpers;
import life.qbic.expdesign.model.ExperimentalDesignPropertyWrapper;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.projectwizard.control.IRegistrationController;
import life.qbic.projectwizard.control.SampleCounter;
import life.qbic.projectwizard.model.MCCPatient;
import life.qbic.projectwizard.processes.RegisteredSamplesReadyRunnable;
import life.qbic.projectwizard.registration.OpenbisCreationController;
import life.qbic.projectwizard.views.IRegistrationView;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.portal.components.StandardTextField;
import life.qbic.xml.manager.StudyXMLParser;
import life.qbic.xml.properties.Property;
import life.qbic.xml.study.Qexperiment;
import life.qbic.xml.study.TechnologyType;

public class MCCViewNew extends VerticalLayout
    implements IRegistrationView, IRegistrationController {
  /**
   * 
   */
  private static final long serialVersionUID = 5542816061866018937L;

  private Logger logger = LogManager.getLogger(MCCViewNew.class);

  private IOpenBisClient openbis;
  private OpenbisCreationController creator;
  // private XMLParser p = new XMLParser();
  final private StudyXMLParser xmlParser = new StudyXMLParser();
  private Experiment designExperiment;
  private JAXBElement<Qexperiment> expDesign;
  private String user;
  // view
  private final String mccSpace = "MULTISCALEHCC";
  private final List<String> weeks = new ArrayList<>(
      Arrays.asList("W00", "W02", "W04", "W10", "W18", "W26", "W32", "W40", "W48", "WXX"));
  private List<TechnologyType> techTypes;
  private ComboBox mccProjects;
  private StandardTextField newProject;
  private StandardTextField treatment;
  private StandardTextField patient;
  private Table existingPatients;

  private TabSheet editView;
  private Table samples;
  private Table metaData;

  private ProgressBar bar;
  private Label registerInfo;
  private Button addSamples;

  private List<Sample> entities;
  private List<String> patients;
  private Set<String> cases;
  private SampleCounter counter;

  private String project;

  public MCCViewNew(IOpenBisClient openbis, OpenbisCreationController creationController,
      String user) {
    techTypes = new ArrayList<TechnologyType>();
    techTypes.add(new TechnologyType("Transcriptomics"));
    techTypes.add(new TechnologyType("Proteomics"));
    techTypes.add(new TechnologyType("Metabolomics"));
    techTypes.add(new TechnologyType("Imaging"));

    this.openbis = openbis;
    this.creator = creationController;
    this.user = user;

    this.cases = new HashSet<String>();
    this.patients = new ArrayList<String>();

    mccProjects = new ComboBox("Source Project");
    List<String> projects = new ArrayList<String>();
    for (Project p : openbis.getProjectsOfSpace(mccSpace))
      projects.add(p.getCode());
    mccProjects.addStyleName(Styles.boxTheme);
    mccProjects.addItems(projects);
    mccProjects.setImmediate(true);

    newProject = new StandardTextField("New Project");
    newProject.setImmediate(true);
    newProject.setWidth("80px");

    HorizontalLayout projectTab = new HorizontalLayout();
    projectTab.setSpacing(true);
    projectTab.addComponent(mccProjects);
    projectTab.addComponent(newProject);

    treatment = new StandardTextField("Treatment");
    patient = new StandardTextField("# of new patients");
    patient.setWidth("50px");

    HorizontalLayout paramTab = new HorizontalLayout();
    paramTab.setSpacing(true);
    paramTab.addComponent(treatment);
    paramTab.addComponent(patient);

    existingPatients = new Table("Existing Patients");
    existingPatients.setStyleName(Styles.tableTheme);
    existingPatients.setPageLength(1);

    editView = new TabSheet();
    editView.addStyleName(ValoTheme.TABSHEET_FRAMED);

    samples = new Table("Samples");
    samples.setStyleName(Styles.tableTheme);
    samples.setPageLength(1);

    metaData = new Table();
    metaData.setEditable(true);
    metaData.setStyleName(Styles.tableTheme);

    editView.addTab(samples, "Overview");
    editView.addTab(metaData, "Change Metadata");
    editView.setVisible(false);

    registerInfo = new Label();
    bar = new ProgressBar();
    addSamples = new Button("Add Samples");
    addSamples.setEnabled(false);
    initMCCListeners();
    addComponent(Styles.questionize(projectTab,
        "Samples can only be added if Timepoint, Treatment, Project and Patient Number "
            + "are filled in and they don't already exist in the current project. E.g. you can add a new timepoint for the same patient and "
            + "treatment but not the same timepoint.",
        "Adding new Samples"));
    addComponent(paramTab);
    addComponent(existingPatients);
    addComponent(editView);
    addComponent(registerInfo);
    addComponent(bar);
    addComponent(addSamples);
  }

  private boolean allValid() {
    boolean project = !newProject.isEmpty() || mccProjects.getValue() != null;
    boolean treat = !treatment.isEmpty();
    boolean pat = !patient.isEmpty() && patient.getValue().matches("[0-9]*");
    boolean input = project && treat && pat;
    boolean res = input;
    // not needed since we just specify number of new patients
    // if (input) {
    // S101:W00:*
    // String extID =
    // treatment.getValue().substring(0, 1) + patient.getValue() + ":" + timepoint.getValue();
    // res = !cases.contains(extID);
    // }
    return res;
  }

  private MCCViewNew getView() {
    return this;
  }

  private void findAndSetDesignExperiment(String space, String project) throws JAXBException {
    designExperiment = null;
    String id = ExperimentCodeFunctions.getInfoExperimentID(space, project);
    List<Experiment> exps = openbis.getExperimentById2(id);
    if (exps.isEmpty()) {
      designExperiment = null;
      logger.error("could not find info experiment for project" + project);
    } else {
      Experiment e = exps.get(0);
      if (e.getExperimentTypeCode().equalsIgnoreCase(ExperimentType.Q_PROJECT_DETAILS.name())) {
        designExperiment = e;
        expDesign =
            xmlParser.parseXMLString(designExperiment.getProperties().get("Q_EXPERIMENTAL_SETUP"));
        logger.debug("setting exp design: " + expDesign);
      }
    }
  }

  private void initMCCListeners() {

    ValueChangeListener check = new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = -7015950228583952364L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        addSamples.setEnabled(allValid());
      }
    };
    treatment.addValueChangeListener(check);
    patient.addValueChangeListener(check);

    newProject.addValueChangeListener(new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = -7747648379674835869L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        if (!newProject.isEmpty())
          counter = new SampleCounter(newProject.getValue());
        for (Sample s : openbis.getSamplesOfProject("/" + mccSpace + "/" + newProject.getValue()))
          counter.increment(s);
        addSamples.setEnabled(allValid());
      }
    });

    mccProjects.addValueChangeListener(new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = 354186297920828100L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        projectBoxChanged();
      }
    });

    addSamples.addClickListener(new Button.ClickListener() {
      /**
       * 
       */
      private static final long serialVersionUID = -3356594300480093815L;

      @Override
      public void buttonClick(ClickEvent event) {
        logger.info("Adding Patient.");
        List<List<ISampleBean>> samps = null;
        samps = prepDefaultMCCSamples();
        addSamples.setEnabled(false);
        List<ISampleBean> allSamples =
            samps.stream().flatMap(x -> x.stream()).collect(Collectors.toList());
        ExperimentalDesignPropertyWrapper design =
            ParserHelpers.samplesWithMetadataToExperimentalFactorStructure(allSamples);
        Map<String, Map<String, Object>> entitiesToUpdate =
            new HashMap<String, Map<String, Object>>();

        List<OpenbisExperiment> infoExperiments = new ArrayList<>();
        if (designExperiment != null) {
          entitiesToUpdate.put(designExperiment.getCode(), ParserHelpers.getExperimentalDesignMap(
              designExperiment.getProperties(), design, techTypes, new HashSet<>()));
        } else {
          Map<String, Object> props = new HashMap<>();
          String newDesignXML = "";
          try {
            JAXBElement<Qexperiment> newDesign = xmlParser.createNewDesign(new HashSet<>(),
                techTypes, design.getExperimentalDesign(), design.getProperties());
            newDesignXML = xmlParser.toString(newDesign);
          } catch (JAXBException e) {
            logger.error("could not create new design xml");
            e.printStackTrace();
          }
          String exp = project + "_INFO";
          props.put("Q_EXPERIMENTAL_SETUP", newDesignXML);
          infoExperiments.add(new OpenbisExperiment(exp, ExperimentType.Q_PROJECT_DETAILS, props));
          String code = project + "000";
          String sampleType = "Q_ATTACHMENT_SAMPLE";
          ISampleBean infoSample = new TSVSampleBean(code, exp, project, mccSpace, sampleType, "",
              new ArrayList<String>(), new HashMap<String, Object>());
          samps.add(new ArrayList<ISampleBean>(Arrays.asList(infoSample)));
        }
        logger.debug("exps " + infoExperiments);
        logger.debug("update " + entitiesToUpdate);
        // creator.registerProjectWithExperimentsAndSamplesBatchWise(samps, null, infoExperiments,
        // bar,
        // registerInfo, new RegisteredSamplesReadyRunnable(getView(), getView()), user,
        // entitiesToUpdate, false);
      }
    });
  }

  protected void projectBoxChanged() {
    addSamples.setEnabled(true);
    expDesign = null;
    designExperiment = null;
    if (mccProjects.getValue() == null) {
      newProject.setEnabled(true);
      treatment.setEnabled(true);
      addSamples.setEnabled(false);
    } else {
      newProject.setEnabled(false);
      try {
        findAndSetDesignExperiment(mccSpace, mccProjects.getValue().toString());
      } catch (JAXBException e) {
        e.printStackTrace();
      }
    }
    entities = new ArrayList<Sample>();
    cases = new HashSet<String>();
    if (newProject.getValue().isEmpty())
      counter = new SampleCounter((String) mccProjects.getValue());
    else
      counter = new SampleCounter(newProject.getValue());
    String treatment = "";
    boolean wrongFormat = false;
    for (Sample s : openbis.getSamplesWithParentsAndChildrenOfProjectBySearchService(
        "/" + mccSpace + "/" + (String) mccProjects.getValue())) {
      counter.increment(s);
      String id = s.getProperties().get("Q_EXTERNALDB_ID");
      if (s.getSampleTypeCode().equals("Q_BIOLOGICAL_ENTITY")) {
        entities.add(s);
        patients.add(id);
      } else {
        if (treatment.isEmpty()) {
          List<Property> properties =
              xmlParser.getFactorsAndPropertiesForSampleCode(expDesign, s.getCode());
          for (Property f : properties) {
            if (f.getLabel().equals("treatment")) {
              treatment = f.getValue();
              getView().treatment.setValue(treatment);
              getView().treatment.setEnabled(false);
            }
          }
        }
        try {
          if (id != null) {
            System.out.println(id);
            String prefix = String.join(":", Arrays.copyOfRange(id.split(":"), 0, 2));
            System.out.println(prefix);
            cases.add(prefix);
          }
        } catch (IndexOutOfBoundsException e) {
          wrongFormat = true;
        }
      }
    }
    addSamples.setEnabled(allValid());
    if (wrongFormat) {
      logger
          .warn("MCCView found samples with unexpected/empty External ID. Probably no problem...");
      // Functions.notification("Wrong format",
      // "Project doesn't fit the expected layout. Please choose another project.",
      // NotificationType.ERROR);
      // addSamples.setEnabled(false);
    }
    BeanItemContainer<MCCPatient> c = new BeanItemContainer<MCCPatient>(MCCPatient.class);
    for (String id : cases) {
      String[] idSplit = id.split(":");
      MCCPatient p = new MCCPatient(idSplit[0], treatment, idSplit[1]);
      c.addBean(p);
    }
    existingPatients.setContainerDataSource(c);
    existingPatients.setPageLength(Math.min(10, c.size()));
    existingPatients.sort(new Object[] {"ID", "week"}, new boolean[] {true});
  }

  private TSVSampleBean createSample(String code, String expSuffix, SampleType type, String secondary,
      String extID, String bioType, List<String> parentIDs) {
    Map<String, Object> metadata = new HashMap<>();
    switch (type) {
      case Q_BIOLOGICAL_ENTITY:
        metadata.put("Q_NCBI_ORGANISM", bioType);
        break;
      case Q_BIOLOGICAL_SAMPLE:
        metadata.put("Q_PRIMARY_TISSUE", bioType);
      case Q_TEST_SAMPLE:
        metadata.put("Q_SAMPLE_TYPE", bioType);
      default:
        break;
    }
    metadata.put("Q_EXTERNALDB_ID", extID);
    return new TSVSampleBean(code, project + expSuffix, project, mccSpace, type.toString(), secondary,
        new ArrayList<String>(), metadata);
  }

  private List<List<ISampleBean>> prepDefaultMCCSamples() {
    SampleType t1 = SampleType.Q_BIOLOGICAL_ENTITY;
    SampleType t2 = SampleType.Q_BIOLOGICAL_SAMPLE;
    SampleType t3 = SampleType.Q_TEST_SAMPLE;
    // String timepoint = this.timepoint.getValue();
    String treatInput = this.treatment.getValue();
    String patientInput = this.patient.getValue();
    int numNewPatients = Integer.parseInt(patientInput);
    List<List<ISampleBean>> res = new ArrayList<List<ISampleBean>>();
    List<ISampleBean> patients = new ArrayList<ISampleBean>();
    List<ISampleBean> urine = new ArrayList<ISampleBean>();
    List<ISampleBean> uAliquots = new ArrayList<ISampleBean>();
    List<ISampleBean> liver = new ArrayList<ISampleBean>();
    List<ISampleBean> tumor = new ArrayList<ISampleBean>();
    List<ISampleBean> plasma = new ArrayList<ISampleBean>();
    List<ISampleBean> pAliquots = new ArrayList<ISampleBean>();
    List<ISampleBean> serum = new ArrayList<ISampleBean>();
    List<ISampleBean> sAliquots = new ArrayList<ISampleBean>();
    List<ISampleBean> molecules = new ArrayList<ISampleBean>();
    List<ISampleBean> blood = new ArrayList<ISampleBean>();
    List<ISampleBean> cfDNA = new ArrayList<ISampleBean>();
    List<ISampleBean> DNA = new ArrayList<ISampleBean>();
    List<ISampleBean> RNA = new ArrayList<ISampleBean>();

    project = (String) mccProjects.getValue();
    if (!newProject.isEmpty())
      project = newProject.getValue();

    for (int newPatient : listNewPatients(numNewPatients)) {
      String patientID = project + "ENTITY-" + newPatient;// new parent

      // if (patient.length() < 2)
      // patient = "0" + patient;
      String patientExtID = treatInput.substring(0, 1).toUpperCase() + newPatient;

      // if new patient, add to samples to register
      if (!this.patients.contains(patientExtID)) {
        patients.add(createSample(patientID, "E1", t1, "patient #" + newPatient, patientExtID,
            "9606", new ArrayList<>()));
      }
      List<String> patientIDs = new ArrayList<String>(Arrays.asList(patientID));
      for (String timepoint : weeks) {

        String extIDBase = patientExtID + ":" + timepoint + ":";

        String urineExtIDBase = extIDBase + "U";
        // TODO
        // metadata.put("XML_FACTORS",
        // "treatment: " + treatInput + "; week: evaluation #" + timepoint);
        // metadata.put("Q_PRIMARY_TISSUE", "URINE");
        // metadata.put("Q_EXTERNALDB_ID", urineExtIDBase + "0");
        String urineID = counter.getNewBarcode();
        urine.add(createSample(urineID, "E2", t2, "urine sample", urineExtIDBase + "0", "URINE",
            patientIDs));
        for (int i = 1; i < 5; i++) {
          String ID = counter.getNewBarcode();
          uAliquots.add(createSample(ID, "E3", t2, "urine aliquot #" + i, urineExtIDBase + i,
              "URINE", new ArrayList<String>(Arrays.asList(urineID))));

          // small molecules
          for (int j = 1; j < 2; j++) {
            String suffix = i + ":SM";
            String molID = counter.getNewBarcode();
            molecules.add(createSample(molID, "E12", t3, "urine #" + i + " molecules",
                urineExtIDBase + suffix, "SMALLMOLECULES",
                new ArrayList<String>(Arrays.asList(ID))));
          }
        }

        String plasmaExtIDBase = extIDBase + "P";
        String plasmaID = counter.getNewBarcode();// parent
        plasma.add(createSample(plasmaID, "E4", t2, "plasma sample", plasmaExtIDBase + "0",
            "BLOOD_PLASMA", patientIDs));

        for (int i = 1; i < 5; i++) {
          String ID = counter.getNewBarcode();
          pAliquots.add(createSample(ID, "E5", t2, "plasma aliquot #" + i, plasmaExtIDBase + i,
              "BLOOD_PLASMA", new ArrayList<String>(Arrays.asList(plasmaID))));

          // small molecules
          for (int j = 1; j < 2; j++) {
            String suffix = i + ":SM";
            String molID = counter.getNewBarcode();
            molecules.add(createSample(molID, "E13", t3, "plasma #" + i + " molecules",
                plasmaExtIDBase + suffix, "SMALLMOLECULES",
                new ArrayList<String>(Arrays.asList(ID))));
          }
        }


        String serumExtIDBase = extIDBase + "S";
        String serumID = counter.getNewBarcode();// parent
        serum.add(createSample(serumID, "E6", t2, "serum sample", serumExtIDBase + "0",
            "BLOOD_SERUM", patientIDs));

        for (int i = 1; i < 5; i++) {
          String ID = counter.getNewBarcode();
          sAliquots.add(createSample(ID, "E7", t2, "serum aliquot #" + i, serumExtIDBase + i,
              "BLOOD_SERUM", new ArrayList<String>(Arrays.asList(serumID))));

          // small molecules
          for (int j = 1; j < 2; j++) {
            String suffix = i + ":SM";
            String molID = counter.getNewBarcode();
            molecules.add(createSample(molID, "E14", t3, "serum #" + i + " molecules",
                serumExtIDBase + suffix, "SMALLMOLECULES",
                new ArrayList<String>(Arrays.asList(ID))));
          }
        }

        // TODO
        // String imagingExtBase = extIDBase + "I";
        // for (int i = 1; i < 2; i++) {
        // String ID = counter.getNewBarcode();
        // List<String> parentID = new ArrayList<>(Arrays.asList(ID));
        // metadata.put("Q_EXTERNALDB_ID", imagingExtBase + i);
        // metadata.put("Q_PRIMARY_TISSUE", "HEPATOCELLULAR_CARCINOMA");// TODO huh?
        // liver.add(new TSVSampleBean(ID, project + "E8", project, mccSpace, "Q_BIOLOGICAL_SAMPLE",
        // "imaging", patientIDs, (HashMap<String, Object>) metadata.clone()));
        // }
        String bloodExtBase = extIDBase + "B";
        for (int i = 1; i < 3; i++) {

          String ID = counter.getNewBarcode();
          blood.add(createSample(ID, "E9", t2, "blood sample #" + i, bloodExtBase + i,
              "WHOLE_BLOOD", patientIDs));
          List<String> parentID = new ArrayList<>(Arrays.asList(ID));
          // DNA and cfDNA molecules
          String cfID = counter.getNewBarcode();
          cfDNA.add(createSample(cfID, "E14", t3, "blood #" + i + " cfDNA",
              bloodExtBase + i + ":cfDNA", "CF_DNA", parentID));
          String dnaID = counter.getNewBarcode();
          DNA.add(createSample(dnaID, "E15", t3, "blood #" + i + " DNA", bloodExtBase + i + ":DNA",
              "DNA", parentID));
        }

        if (timepoint.equals("W00") || timepoint.equals("WXX")) {
          String tumorExtBase = extIDBase + "T";
          for (int i = 1; i < 5; i++) {
            String ID = counter.getNewBarcode();
            tumor.add(createSample(ID, "E10", t2, "tumor biopsy #" + i, tumorExtBase + i,
                "HEPATOCELLULAR_CARCINOMA", patientIDs));
            List<String> parentID = new ArrayList<>(Arrays.asList(ID));
            // DNA and RNA molecules
            String rnaID = counter.getNewBarcode();
            RNA.add(createSample(rnaID, "E16", t3, "tumor #" + i + " RNA", tumorExtBase + i + ":RNA",
                "RNA", parentID));
            String dnaID = counter.getNewBarcode();
            DNA.add(createSample(dnaID, "E17", t3, "tumor #" + i + " DNA", tumorExtBase + i + ":DNA",
                "DNA", parentID));

            // metadata.put("Q_EXTERNALDB_ID", tumorExtBase + i);
            // metadata.put("Q_PRIMARY_TISSUE", "HEPATOCELLULAR_CARCINOMA");
            // liver.add(
            // new TSVSampleBean(ID, project + "E10", project, mccSpace, "Q_BIOLOGICAL_SAMPLE",
            // "tumor biopsy #" + i, patientIDs, (HashMap<String, Object>) metadata.clone()));
          }
          String liverExtBase = extIDBase + "L";
          for (int i = 1; i < 3; i++) {

            String ID = counter.getNewBarcode();
            liver.add(createSample(ID, "E11", t2, "liver biopsy #" + i, liverExtBase + i, "LIVER",
                patientIDs));
            List<String> parentID = new ArrayList<>(Arrays.asList(ID));
            // DNA and RNA molecules
            String rnaID = counter.getNewBarcode();
            RNA.add(createSample(rnaID, "E16", t3, "liver #" + i + " RNA", liverExtBase + i + ":RNA",
                "RNA", parentID));
            String dnaID = counter.getNewBarcode();
            DNA.add(createSample(dnaID, "E17", t3, "liver #" + i + " DNA", liverExtBase + i + ":DNA",
                "DNA", parentID));

            // String ID = counter.getNewBarcode();
            // List<String> parentID = new ArrayList<>(Arrays.asList(ID));
            // metadata.put("Q_EXTERNALDB_ID", liverExtBase + i);
            // metadata.put("Q_PRIMARY_TISSUE", "LIVER");
            // liver.add(
            // new TSVSampleBean(ID, project + "E11", project, mccSpace, "Q_BIOLOGICAL_SAMPLE",
            // "liver biopsy #" + i, patientIDs, (HashMap<String, Object>) metadata.clone()));
          }
        }
      }
    }
    List<List<ISampleBean>> dummy =
        new ArrayList<List<ISampleBean>>(Arrays.asList(patients, urine, uAliquots, plasma,
            pAliquots, serum, sAliquots, molecules, blood, tumor, liver, DNA, cfDNA, RNA));
    for (List<ISampleBean> l : dummy) {
      System.out.println(l);
      System.out.println();
      if (l.size() > 0) {
        res.add(l);
      }
    }
    return res;
  }

  private List<Integer> listNewPatients(int numNewPatients) {
    List<Integer> res = new ArrayList<>();

    int id = 101;
    String patient = "S" + id;
    while (cases.contains(patient)) {
      id++;
      patient = "S" + id;
    }
    for (int i = 0; i < numNewPatients; i++) {
      res.add(id);
      id++;
    }

    return res;
  }

  protected List<String> sampsToStrings(List<Sample> children) {
    List<String> res = new ArrayList<String>();
    for (Sample c : children)
      res.add(c.getCode());
    return res;
  }

  @Override
  // TODO handle errors
  public void registrationDone(boolean sqlDown, String errors) {
    logger.info("Registration complete, reloading patient table.");
    Styles.notification("Registration complete!", "Registration of patient complete.",
        NotificationType.SUCCESS);
    projectBoxChanged();
  }

  @Override
  public void performPostRegistrationTasks(boolean success) {}

  @Override
  public String getRegistrationError() {
    return creator.getErrors();
  }
}
