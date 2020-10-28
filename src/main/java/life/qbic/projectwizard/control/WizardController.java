/*******************************************************************************
 * QBiC Project Wizard enables users to create hierarchical experimental designs including different
 * study conditions using factorial design. Copyright (C) "2016" Andreas Friedrich
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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.bind.JAXBException;
import life.qbic.portal.utils.ConfigurationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.validator.CompositeValidator;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.themes.ValoTheme;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import life.qbic.datamodel.attachments.AttachmentConfig;
import life.qbic.datamodel.experiments.ExperimentBean;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.experiments.OpenbisExperiment;
import life.qbic.datamodel.identifiers.ExperimentCodeFunctions;
import life.qbic.datamodel.persons.PersonType;
import life.qbic.datamodel.samples.AOpenbisSample;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.expdesign.ParserHelpers;
import life.qbic.expdesign.SamplePreparator;
import life.qbic.expdesign.io.QBiCDesignReader;
import life.qbic.expdesign.model.ExperimentalDesignPropertyWrapper;
import life.qbic.expdesign.model.SampleSummaryBean;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.projectwizard.io.DBManager;
import life.qbic.projectwizard.model.MSExperimentModel;
import life.qbic.projectwizard.model.TestSampleInformation;
import life.qbic.projectwizard.model.Vocabularies;
import life.qbic.projectwizard.processes.RegisteredSamplesReadyRunnable;
import life.qbic.projectwizard.processes.RegistrationMode;
import life.qbic.projectwizard.registration.IOpenbisCreationController;
import life.qbic.projectwizard.registration.OmeroAdapter;
import life.qbic.projectwizard.registration.OpenbisV3APIWrapper;
import life.qbic.projectwizard.steps.*;
import life.qbic.projectwizard.uicomponents.ProjectInformationComponent;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.xml.notes.Note;
import life.qbic.xml.properties.Property;
import life.qbic.xml.study.TechnologyType;

/**
 * Controller for the sample/experiment creation wizard
 * 
 * @author Andreas Friedrich
 * 
 */
public class WizardController implements IRegistrationController {

  private IOpenBisClient openbis;
  private IOpenbisCreationController openbisCreator;
  private OmeroAdapter omeroAdapter;
  private Wizard w;
  private String user;
  private Map<Steps, WizardStep> steps;
  private WizardDataAggregator dataAggregator;
  private boolean bioFactorInstancesSet = false;
  private boolean extractFactorInstancesSet = false;
  private boolean extractPoolsSet = false;
  private boolean testPoolsSet = false;
  private boolean copyMode = false;
  private Vocabularies vocabularies;
  private DBManager dbm;
  private FileDownloader tsvDL;
  private List<Note> notes;
  private SamplePreparator prep = new SamplePreparator();
  protected List<String> designExperimentTypes;
  private String newExperimentalDesignXML;

  private Logger logger = LogManager.getLogger(WizardController.class);

  protected Map<String, Map<String, Object>> entitiesToUpdate;
  private OpenbisV3APIWrapper v3API;
  private AttachmentConfig attachmentConfig;

  /**
   * 
   * @param openbis
   * @param omeroAdapter
   * @param v3
   * @param creationController
   * @param dbm
   * @param vocabularies
   * @param attachConfig 
   */

  public WizardController(IOpenBisClient openbis, OmeroAdapter omeroAdapter, OpenbisV3APIWrapper v3,
      IOpenbisCreationController creationController, DBManager dbm, Vocabularies vocabularies, AttachmentConfig attachConfig) {

    this.openbis = openbis;
    this.v3API = v3;
    this.dbm = dbm;
    this.openbisCreator = creationController;
    this.vocabularies = vocabularies;
    this.designExperimentTypes = vocabularies.getExperimentTypes();
    this.omeroAdapter = omeroAdapter;
    this.attachmentConfig = attachConfig;
  }

  // Functions to add steps to the wizard depending on context
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

  private void setRegStep() {
    w.addStep(steps.get(Steps.Registration)); // tsv upload and registration
  }

  private void setInheritEntities() {
    w.addStep(steps.get(Steps.Entity_Tailoring)); // entity negative selection
    w.addStep(steps.get(Steps.Extraction)); // extract first step
    setInheritExtracts();
  }

  private void setInheritExtracts() {
    w.addStep(steps.get(Steps.Extract_Tailoring)); // extracts negative selection
    w.addStep(steps.get(Steps.Test_Samples)); // test samples first step
    setRegStep();
  }

  private void setExtractsPooling() {
    w.addStep(steps.get(Steps.Extract_Pooling)); // pooling step
    setTestStep();
  }

  private void setTestStep() {
    w.addStep(steps.get(Steps.Test_Samples)); // test samples first step
    setRegStep();
  }

  private void setTestsPooling() {
    w.addStep(steps.get(Steps.Test_Sample_Pooling));
    setRegStep();
  }

  private void setTailoringStepsOnly() {
    w.addStep(steps.get(Steps.Entity_Tailoring));
    w.addStep(steps.get(Steps.Extract_Tailoring));
    w.addStep(steps.get(Steps.Test_Samples)); // test samples first step
    setRegStep();
  }

  private void setCreateEntities() {
    w.addStep(steps.get(Steps.Entities)); // entities first step
    setInheritEntities();
  }

  private void setEntityConditions() {
    w.addStep(steps.get(Steps.Entity_Conditions)); // entity conditions
    setInheritEntities();
  }

  private void setExtractConditions() {
    w.addStep(steps.get(Steps.Extract_Conditions)); // extract conditions
    setInheritExtracts();
  }

  private void resetNextSteps() {
    List<WizardStep> steps = w.getSteps();
    List<WizardStep> copy = new ArrayList<WizardStep>();
    copy.addAll(steps);
    boolean isNew = false;
    for (int i = 0; i < copy.size(); i++) {
      WizardStep cur = copy.get(i);
      if (isNew) {
        w.removeStep(cur);
      }
      if (w.isActive(cur))
        isNew = true;
    }
  }

  /**
   * Test is a project has biological entities registered. Used to check availability of context
   * options
   * 
   * @param spaceCode Code of the selected openBIS space
   * @param code Code of the project
   * @return
   */
  public boolean projectHasBioEntities(String spaceCode, String code) {
    if (!openbis.projectExists(spaceCode, code))
      return false;
    for (Experiment experiment : openbis.getExperimentsOfProjectByCode(code)) {
      if (experiment.getType().getCode().equals("Q_EXPERIMENTAL_DESIGN")) {
        if (openbis.getSamplesofExperiment(experiment.getIdentifier().getIdentifier()).size() > 0)
          return true;
      }
    }
    return false;
  }

  /**
   * Test is a project has biological extracts registered. Used to check availability of context
   * options
   * 
   * @param spaceCode Code of the selected openBIS space
   * @param code Code of the project
   * @return
   */
  public boolean projectHasExtracts(String spaceCode, String code) {
    if (!openbis.projectExists(spaceCode, code))
      return false;
    for (Experiment e : openbis.getExperimentsOfProjectByCode(code)) {
      if (e.getType().getCode().equals("Q_SAMPLE_EXTRACTION"))
        if (openbis.getSamplesofExperiment(e.getIdentifier().getIdentifier()).size() > 0)
          return true;
    }
    return false;
  }

  public Wizard getWizard() {
    return w;
  }

  private String generateProjectCode() {
    Random r = new Random();
    String res = "";
    while (res.length() < 5 || openbis.getProjectByCode(res) != null) {
      res = "Q";
      for (int i = 1; i < 5; i++) {
        char c = 'Y';
        while (c == 'Y' || c == 'Z')
          c = (char) (r.nextInt(26) + 'A');
        res += c;
      }
    }
    return res;
  }

  public static enum Steps {
    Project_Context, Entities, Entity_Conditions, Entity_Tailoring, Extraction, Extract_Conditions, Extract_Tailoring, Extract_Pooling, Test_Samples, Test_Sample_Pooling, Registration, Finish, Protein_Fractionation, Protein_Fractionation_Pooling, Peptide_Fractionation, Peptide_Fractionation_Pooling;
  }

  /**
   * Initialize all possible steps in the wizard and the listeners used
   */
  public void init(final String user) {
    this.user = user;
    this.w = new Wizard();
    WizardController control = this;
    w.getFinishButton().setVisible(false);
    w.getFinishButton().setStyleName(ValoTheme.BUTTON_DANGER);
    w.getCancelButton().setStyleName(ValoTheme.BUTTON_DANGER);

    final ProjectInformationComponent projSelection = new ProjectInformationComponent(
        vocabularies.getSpaces(), vocabularies.getPeople().keySet());
    final ProjectContextStep contextStep = new ProjectContextStep(projSelection);
    final EntityStep entStep =
        new EntityStep(vocabularies.getTaxMap(), vocabularies.getPeople().keySet());
    final ConditionInstanceStep entCondInstStep =
        new ConditionInstanceStep(vocabularies.getTaxMap().keySet(), "Species", "Biol. Variables");
    final TailoringStep tailoringStep1 = new TailoringStep("Sample Sources", false);
    final ExtractionStep extrStep = new ExtractionStep(vocabularies.getTissueMap(),
        vocabularies.getCellLinesMap(), vocabularies.getPeople().keySet());
    final ConditionInstanceStep extrCondInstStep = new ConditionInstanceStep(
        vocabularies.getTissueMap().keySet(), "Tissues", "Extr. Variables");
    final TailoringStep tailoringStep2 = new TailoringStep("Sample Extracts", true);
    final AnalyteStep techStep = new AnalyteStep(w, vocabularies);
    final SummaryRegisterStep regStep = new SummaryRegisterStep();
    final PoolingStep poolStep1 = new PoolingStep(Steps.Extract_Pooling);
    final PoolingStep poolStep2 = new PoolingStep(Steps.Test_Sample_Pooling);
    final FinishStep finishStep = new FinishStep(w, attachmentConfig, openbisCreator);

    final MSAnalyteStep protFracStep = new MSAnalyteStep(vocabularies, "PROTEINS");
    final MSAnalyteStep pepFracStep = new MSAnalyteStep(vocabularies, "PEPTIDES");

    steps = new HashMap<Steps, WizardStep>();
    steps.put(Steps.Project_Context, contextStep);
    steps.put(Steps.Entities, entStep);
    steps.put(Steps.Entity_Conditions, entCondInstStep);
    steps.put(Steps.Entity_Tailoring, tailoringStep1);
    steps.put(Steps.Extraction, extrStep);
    steps.put(Steps.Extract_Conditions, extrCondInstStep);
    steps.put(Steps.Extract_Tailoring, tailoringStep2);
    steps.put(Steps.Extract_Pooling, poolStep1);
    steps.put(Steps.Test_Samples, techStep);
    steps.put(Steps.Test_Sample_Pooling, poolStep2);
    steps.put(Steps.Protein_Fractionation, protFracStep);
    steps.put(Steps.Peptide_Fractionation, pepFracStep);
    steps.put(Steps.Registration, regStep);
    steps.put(Steps.Finish, finishStep);

    this.dataAggregator = new WizardDataAggregator(steps, openbis, vocabularies.getTaxMap(),
        vocabularies.getTissueMap(), vocabularies.getPeople());
    // w.addStep(finishStep);
    w.addStep(contextStep);

    FocusListener fListener = new FocusListener() {
      private static final long serialVersionUID = 8721337946386845992L;

      @Override
      public void focus(FocusEvent event) {
        // new project selected...keep generating codes until one is valid
        TextField pr = projSelection.getProjectField();
        if (!pr.isValid() || pr.isEmpty()) {
          projSelection.tryEnableCustomProject(generateProjectCode());
          contextStep.enableEmptyProjectContextOption(true);
          contextStep.enableNewContextOption(true);
          contextStep.makeContextVisible();
        }
      }
    };
    projSelection.getProjectField().addFocusListener(fListener);

    Button.ClickListener projCL = new Button.ClickListener() {

      /**
       * 
       */
      private static final long serialVersionUID = -6646294420820222646L;

      @Override
      public void buttonClick(ClickEvent event) {
        String existingProject = (String) projSelection.getProjectBox().getValue();
        if (existingProject == null || existingProject.isEmpty()) {
          projSelection.tryEnableCustomProject(generateProjectCode());
          contextStep.enableEmptyProjectContextOption(true);
          contextStep.enableNewContextOption(true);
          contextStep.makeContextVisible();
        }
      }
    };
    projSelection.getProjectReloadButton().addClickListener(projCL);

    Button.ClickListener peopleCL = new Button.ClickListener() {

      /**
       * 
       */
      private static final long serialVersionUID = -6646294420820222646L;

      @Override
      public void buttonClick(ClickEvent event) {
        vocabularies.setPeople(dbm.fetchPeople());
        Set<String> people = vocabularies.getPeople().keySet();
        projSelection.updatePeople(people);
        entStep.updatePeople(people);
        extrStep.updatePeople(people);
        techStep.updatePeople(people);
      }
    };
    projSelection.getPeopleReloadButton().addClickListener(peopleCL);
    entStep.getPeopleReloadButton().addClickListener(peopleCL);
    extrStep.getPeopleReloadButton().addClickListener(peopleCL);

    Button.ClickListener cl = new Button.ClickListener() {
      /**
       * 
       */
      private static final long serialVersionUID = -8427457552926464653L;

      @Override
      public void buttonClick(ClickEvent event) {
        String src = event.getButton().getCaption();
        if (src.equals("Register Project")) {
          String desc = contextStep.getDescription();
          String altTitle = contextStep.getExpSecondaryName();
          registerProjectOnly(desc, altTitle, user, regStep);
          w.addStep(steps.get(Steps.Finish));
        }
        if (src.equals("Send Project to QBiC")) {
          String tsv = dataAggregator.getTSVContent();
          String space = contextStep.getSpaceCode();
          String project = contextStep.getProjectCode();
          String altTitle = contextStep.getExpSecondaryName();
          List<Note> notes = new ArrayList<Note>();
          boolean afterMS = w.getSteps().contains(steps.get(Steps.Protein_Fractionation));
          if (afterMS) {
            List<String> infos = new ArrayList<String>();
            String protInfo = protFracStep.getAdditionalInfo();
            if (protInfo != null && !protInfo.isEmpty()) {
              infos.add(protInfo);
            }
            String pepInfo = pepFracStep.getAdditionalInfo();
            if (pepInfo != null && !pepInfo.isEmpty()) {
              infos.add(pepInfo);
            }
            if (!infos.isEmpty()) {
              Date now = new Date();
              SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
              for (String comment : infos) {
                Note note = new Note();
                note.setComment(comment);
                note.setUsername(user);
                note.setTime(ft.format(now));
                notes.add(note);
              }
            }
          }
          sendInquiry(space, project, altTitle, tsv, user, notes);
          Styles.notification("Project inquiry sent.",
              "Your Project inquiry was successfully sent to QBiC. We will contact you.",
              NotificationType.SUCCESS);
        }
        if (src.equals("Register All Samples")) {
          regStep.getRegisterButton().setEnabled(false);
          ProjectContextStep contextStep = (ProjectContextStep) steps.get(Steps.Project_Context);
          String desc = contextStep.getDescription();
          boolean afterMS = w.getSteps().contains(steps.get(Steps.Protein_Fractionation));
          // Additional information set in the protein and/or peptide step(s)
          notes = new ArrayList<Note>();
          if (afterMS) {
            List<String> infos = new ArrayList<String>();
            String protInfo = protFracStep.getAdditionalInfo();
            if (protInfo != null && !protInfo.isEmpty()) {
              infos.add(protInfo);
            }
            String pepInfo = pepFracStep.getAdditionalInfo();
            if (pepInfo != null && !pepInfo.isEmpty()) {
              infos.add(pepInfo);
            }
            if (!infos.isEmpty()) {
              Date now = new Date();
              SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
              for (String comment : infos) {
                Note note = new Note();
                note.setComment(comment);
                note.setUsername(user);
                note.setTime(ft.format(now));
                notes.add(note);
              }
            }
          }
          List<List<ISampleBean>> samples = regStep.getSamples();

          String space = contextStep.getSpaceCode();
          String project = contextStep.getProjectCode();
          String exp = project + "_INFO";
          String code = project + "000";
          boolean pilot = contextStep.isPilot();
          ISampleBean infoSample =
              new TSVSampleBean(code, exp, project, space, SampleType.Q_ATTACHMENT_SAMPLE, "",
                  new ArrayList<String>(), new HashMap<String, Object>());
          samples.add(new ArrayList<ISampleBean>(Arrays.asList(infoSample)));
          List<OpenbisExperiment> informativeExperiments =
              dataAggregator.getExperimentsWithMetadata();
          if (newExperimentalDesignXML != null) {
            logger.debug("set new xml: " + newExperimentalDesignXML);

            Map<String, Object> props = new HashMap<>();
            props.put("Q_EXPERIMENTAL_SETUP", newExperimentalDesignXML);
            informativeExperiments.add(
                new OpenbisExperiment(project + "_INFO", ExperimentType.Q_PROJECT_DETAILS, props));
          }
          openbisCreator.registerProjectWithExperimentsAndSamplesBatchWise(samples, desc,
              informativeExperiments, regStep.getProgressBar(), regStep.getProgressLabel(),
              new RegisteredSamplesReadyRunnable(regStep, control), entitiesToUpdate, pilot);
          w.addStep(steps.get(Steps.Finish));

          boolean imgSupport = contextStep.hasImagingSupport();
          if (imgSupport) {
            List<ISampleBean> imagableSamples = new ArrayList<>();
            for (List<ISampleBean> level : samples) {
              if (!level.isEmpty()
                  && level.get(0).getType().equals(SampleType.Q_BIOLOGICAL_SAMPLE)) {
                imagableSamples.addAll(level);
              }
            }
            omeroAdapter.registerSamples(project, desc, imagableSamples);
          }
        }
      }

      private void sendInquiry(String space, String project, String altTitle, String tsv,
          String user, List<Note> notes) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("project", project);
        parameters.put("space", space);
        parameters.put("user", user);
        parameters.put("project-tsv", tsv);

        String comments = "";
        for (Note n : notes)
          comments += n.getComment() + "\n";
        parameters.put("alt-name", altTitle);
        parameters.put("notes", comments);// TODO do something with this

        openbis.triggerIngestionService("mail-project-inquiry", parameters);
      }

    };
    regStep.getRegisterButton().addClickListener(cl);

    /**
     * Space selection listener
     */
    ValueChangeListener spaceSelectListener = new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = -7487587994432604593L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        contextStep.resetProjects();
        String space = contextStep.getSpaceCode();
        if (space != null) {
          List<String> projects = new ArrayList<String>();
          for (Project p : openbis.getProjectsOfSpace(space)) {
            String code = p.getCode();
            String name = dbm.getProjectName("/" + space + "/" + code);
            if (name != null && name.length() > 0) {
              if (name.length() >= 80)
                name = name.substring(0, 80) + "...";
              code += " (" + name + ")";
            }
            projects.add(code);
          }
          contextStep.setProjectCodes(projects);
          List<String> dontFilter = new ArrayList<String>(Arrays.asList("SPECIAL_METHOD"));
          if (space.equals("PROTEOME_CENTER_TUEBINGEN")) {
            protFracStep.filterDictionariesByPrefix("PCT", dontFilter);
            pepFracStep.filterDictionariesByPrefix("PCT", dontFilter);
          } else if (space.endsWith("MPC")) {
            protFracStep.filterDictionariesByPrefix("MPC", dontFilter);
            pepFracStep.filterDictionariesByPrefix("MPC", dontFilter);
          } else {
            protFracStep.filterDictionariesByPrefix("", dontFilter);
            pepFracStep.filterDictionariesByPrefix("", dontFilter);
          }
        }
      }

    };
    contextStep.getSpaceBox().addValueChangeListener(spaceSelectListener);

    /**
     * Project selection listener
     */

    ValueChangeListener projectSelectListener = new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = -443162343850159312L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        contextStep.resetExperiments();
        updateContextOptions(projSelection, contextStep);
      }

    };
    contextStep.getProjectBox().addValueChangeListener(projectSelectListener);

    /**
     * Experiment selection listener
     */

    ValueChangeListener expSelectListener = new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = 1931780520075315462L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        contextStep.resetSamples();
        contextStep.makePilotBoxVisible(false);
        OptionGroup projectContext = contextStep.getProjectContext();
        String context = (String) projectContext.getValue();
        List<String> contextOptions = contextStep.getContextOptions();
        ExperimentBean exp = contextStep.getExperiment();
        if (exp != null) {
          if (exp.isPilot() && !contextOptions.get(4).equals(context)) {
            contextStep.selectPilot();
          }
          List<ISampleBean> beans = new ArrayList<ISampleBean>();
          for (Sample s : openbis.getSamplesofExperiment(exp.getID())) {
            Map<String, String> props = s.getProperties();
            beans.add(new TSVSampleBean(s.getCode(), exp.getCode(), contextStep.getProjectCode(),
                s.getSpace().getCode(), SampleType.valueOf(s.getType().getCode()),
                props.get("Q_SECONDARY_NAME"), Arrays.asList(), new HashMap<>()));
          }
          contextStep.setSamples(beans);
        }
      }

    };
    contextStep.getExperimentTable().addValueChangeListener(expSelectListener);

    /**
     * Project context (radio buttons) listener
     */

    ValueChangeListener projectContextListener = new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = 5972535836592118817L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        copyMode = false;
        if (contextStep.getProjectContext().getValue() != null) {
          contextStep.makePilotBoxVisible(false);
          resetNextSteps();
          OptionGroup projectContext = contextStep.getProjectContext();
          String context = (String) projectContext.getValue();
          List<String> contextOptions = contextStep.getContextOptions();

          List<ExperimentBean> experiments = contextStep.getExperiments();
          List<ExperimentBean> beans = new ArrayList<ExperimentBean>();
          // inherit from bio entities
          if (contextOptions.get(1).equals(context)) {
            for (ExperimentBean b : experiments) {
              if (b.getExperiment_type().equals(ExperimentType.Q_EXPERIMENTAL_DESIGN.toString()))
                beans.add(b);
            }
            setInheritEntities();
            dataAggregator.setInheritEntities(true);
            dataAggregator.setInheritExtracts(false);
          }
          // inherit from sample extraction
          if (contextOptions.get(2).equals(context)) {
            for (ExperimentBean b : experiments) {
              if (b.getExperiment_type().equals(ExperimentType.Q_SAMPLE_EXTRACTION.toString()))
                beans.add(b);
            }
            setInheritExtracts();
            dataAggregator.setInheritEntities(false);
            dataAggregator.setInheritExtracts(true);
          }
          // new context
          if (contextOptions.get(0).equals(context)) {
            contextStep.hideExperiments();
            setCreateEntities();
            contextStep.makePilotBoxVisible(true);
            dataAggregator.setInheritEntities(false);
            dataAggregator.setInheritExtracts(false);
          }
          regStep.setRegistrationMode(RegistrationMode.RegisterSamples);
          if (contextOptions.get(3).equals(context)) {
            setRegStep();
            regStep.setRegistrationMode(RegistrationMode.RegisterEmptyProject);
          }
          // copy design context
          if (contextOptions.get(5).equals(context)) {
            for (ExperimentBean b : experiments) {
              if (b.getExperiment_type().equals(ExperimentType.Q_EXPERIMENTAL_DESIGN.toString()))
                beans.add(b);
            }
            copyMode = true;
            setTailoringStepsOnly();
            dataAggregator.setInheritEntities(true);
            dataAggregator.setInheritExtracts(true);
          }
          // read only tsv creation
          if (contextOptions.get(4).equals(context)) {
            for (ExperimentBean b : experiments) {
              if (b.getExperiment_type().equals(ExperimentType.Q_EXPERIMENTAL_DESIGN.toString()))
                beans.add(b);
            }
            setRegStep();
            regStep.setRegistrationMode(RegistrationMode.DownloadTSV);
          }
          if (beans.size() > 0)
            contextStep.showExperiments(beans);
        }
      }
    };
    contextStep.getProjectContext().addValueChangeListener(projectContextListener);

    /**
     * Listeners for pooling samples
     */
    ValueChangeListener poolingListener = new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = 2393762547426343668L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        resetNextSteps();
        if (tailoringStep2.pool()) {
          setExtractsPooling();
        } else {
          setTestStep();
        }
      }
    };
    tailoringStep2.getPoolBox().addValueChangeListener(poolingListener);

    ValueChangeListener testPoolListener = new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = 2393762547426343668L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        resetNextSteps();
        if (techStep.hasPools()) {
          setTestsPooling();
        } else {
          setRegStep();
        }
      }
    };

    ValueChangeListener proteinListener = new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = -7329476869277381974L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        boolean containsProteins = false;
        for (TestSampleInformation i : techStep.getAnalyteInformation()) {
          String tech = i.getTechnology();
          containsProteins |= tech.equals("PROTEINS");
        }
        if (containsProteins) {
          // TODO probably not needed
          // dataAggregator.prepareTestSamples();
          // techStep.setProteinPreps(dataAggregator.getTests());
        }
      }
    };

    techStep.initTestStep(testPoolListener, proteinListener, peopleCL, steps);

    ValueChangeListener noMeasureListener = new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = 2393762547426343668L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        techStep.changeTechPanel();
      }
    };
    techStep.getNotMeasured().addValueChangeListener(noMeasureListener);

    /**
     * Listeners for entity and extract conditions
     */
    ValueChangeListener entityConditionSetListener = new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = 2393762547426343668L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        resetNextSteps();
        if (entStep.isConditionsSet().getValue() != null) {
          setEntityConditions();
        } else {
          setInheritEntities();
        }
      }
    };
    entStep.isConditionsSet().addValueChangeListener(entityConditionSetListener);

    ValueChangeListener extractConditionSetListener = new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = 4879458823482873630L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        resetNextSteps();
        if (extrStep.conditionsSet().getValue() != null) {
          setExtractConditions();
        } else {
          setInheritExtracts();
        }
      }
    };
    extrStep.conditionsSet().addValueChangeListener(extractConditionSetListener);

    TextField f = contextStep.getProjectCodeField();
    CompositeValidator vd = new CompositeValidator();
    RegexpValidator p = new RegexpValidator("Q[A-Xa-x0-9]{4}",
        "Project must have length of 5, start with Q and not contain Y or Z");
    vd.addValidator(p);
    vd.addValidator(new ProjectNameValidator(openbis));
    f.addValidator(vd);
    f.setImmediate(true);
    f.setValidationVisible(true);

    WizardProgressListener wl = new WizardProgressListener() {

      @Override
      public void wizardCompleted(WizardCompletedEvent event) {}

      @Override
      public void wizardCancelled(WizardCancelledEvent event) {}

      @Override
      public void stepSetChanged(WizardStepSetChangedEvent event) {}

      /**
       * Reactions to step changes in the wizard
       */
      @Override
      public void activeStepChanged(WizardStepActivationEvent event) {
        // Context Step
        if (event.getActivatedStep().equals(contextStep)) {
          // contextStep.allowNext(false);
          regStep.enableDownloads(false);
        }
        // Entity Setup Step
        if (event.getActivatedStep().equals(entStep)) {
          bioFactorInstancesSet = false;
        }
        // Entity Condition Instances Step
        if (event.getActivatedStep().equals(entCondInstStep)) {
          reloadConditionsPreviewTable(entCondInstStep, Integer.toString(entStep.getBioRepAmount()),
              new ArrayList<AOpenbisSample>());
          if (!bioFactorInstancesSet) {
            if (entStep.speciesIsFactor()) {
              entCondInstStep.initOptionsFactorComponent(entStep.getSpeciesAmount(),
                  entStep.isInfectionStudy(), new HashSet<String>(), "N/A", "N/A", "N/A", "N/A");
            }
            entCondInstStep.initFactorFields(entStep.getFactors());
            initConditionListener(entCondInstStep, Integer.toString(entStep.getBioRepAmount()),
                new ArrayList<AOpenbisSample>());
            bioFactorInstancesSet = true;
          }
        }
        // Negative Selection of Entities
        if (event.getActivatedStep().equals(tailoringStep1)) {
          try {
            tailoringStep1.setSamples(dataAggregator.prepareEntities(
                entCondInstStep.getPreSelection(), copyMode, entStep.isInfectionStudy()), null);
          } catch (JAXBException e) {
            e.printStackTrace();
          }
        }
        // Extract Setup Step
        if (event.getActivatedStep().equals(extrStep)) {
          dataAggregator.setEntities(tailoringStep1.getSamples());
          extractFactorInstancesSet = false;
        }
        // Extract Factor Instances Step
        if (event.getActivatedStep().equals(extrCondInstStep)) {
          reloadConditionsPreviewTable(extrCondInstStep,
              Integer.toString(extrStep.getExtractRepAmount()), dataAggregator.getEntities());
          if (!extractFactorInstancesSet) {
            if (extrStep.isTissueFactor()) {
              extrCondInstStep.initOptionsFactorComponent(extrStep.getTissueAmount(), false,
                  vocabularies.getCellLinesMap().keySet(), "Cell Line", "Other", "Cell Line",
                  "Other");
            }
            extrCondInstStep.initFactorFields(extrStep.getFactors());
            initConditionListener(extrCondInstStep,
                Integer.toString(extrStep.getExtractRepAmount()), dataAggregator.getEntities());
            extractFactorInstancesSet = true;
          }
        }
        // Negative Selection of Extracts
        if (event.getActivatedStep().equals(tailoringStep2)) {
          extractPoolsSet = false;
          if (copyMode) {
            // set entities if some were removed or changed in the last step
            dataAggregator.setEntities(tailoringStep1.getSamples());

          }
          try {
            tailoringStep2.setSamples(
                dataAggregator.prepareExtracts(extrCondInstStep.getPreSelection(), copyMode),
                extrStep.getLabelingMethod());
          } catch (JAXBException e) {
            e.printStackTrace();
          }
        }
        // Extract Pool Step
        if (event.getActivatedStep().equals(poolStep1)) {
          dataAggregator.resetExtracts();
          if (!extractPoolsSet) {
            poolStep1.setSamples(
                new ArrayList<List<AOpenbisSample>>(Arrays.asList(tailoringStep2.getSamples())),
                Steps.Extract_Pooling);
            extractPoolsSet = true;
          }
        }
        // Test Setup Step
        if (event.getActivatedStep().equals(techStep)) {
          // dataAggregator.setHasFractionationExperiment(false);
          testPoolsSet = false;// we have to reset this in the case someone goes back from pooling
          List<AOpenbisSample> extracts = tailoringStep2.getSamples();
          techStep.setTissueExtracts(extracts);
          List<AOpenbisSample> all = new ArrayList<AOpenbisSample>();
          all.addAll(extracts);
          all.addAll(dataAggregator.createPoolingSamples(poolStep1.getPools()));
          dataAggregator.setExtracts(all);
          if (copyMode)
            techStep.setAnalyteInputs(dataAggregator.getBaseAnalyteInformation());
        }
        // Test Pool Step
        if (event.getActivatedStep().equals(poolStep2)) {
          if (!testPoolsSet) {// if we come from the analyte step the pools are reset, if we come
                              // back from the next step they are not
            poolStep2.setSamples(dataAggregator.prepareTestSamples(), Steps.Test_Sample_Pooling);
            testPoolsSet = true;
          }
        }
        // Protein Fractionation
        if (event.getActivatedStep().equals(protFracStep)) {
          // List<AOpenbisSample> analytes = new ArrayList<AOpenbisSample>();

          if (!testPoolsSet) {// if pools aren't set at this point then there was no pooling
                              // selected before
            dataAggregator.prepareTestSamples();// we reset the analyte samples in case we come from
                                                // the next step and prepare them anew
          }
          // we forward testsamples and potential pools directly to the fractionation step to sort
          // them out
          // they don't get barcodes either for now, in case we need to recreate them
          List<AOpenbisSample> proteins =
              filterForProperties(dataAggregator.getTests(), "Q_SAMPLE_TYPE", "PROTEINS");
          Map<String, List<AOpenbisSample>> pools = poolStep2.getPools();
          for (String pool : pools.keySet()) {
            List<AOpenbisSample> filtered =
                filterForProperties(pools.get(pool), "Q_SAMPLE_TYPE", "PROTEINS");
            if (filtered.isEmpty())
              pools.remove(pool);
            else
              pools.put(pool, filtered);
          }
          protFracStep.setAnalyteSamples(proteins, pools);
        }
        // Peptide Fractionation
        if (event.getActivatedStep().equals(pepFracStep)) {
          if (!protFracStep.hasRun()) {
            protFracStep.createPreliminaryExperiments();
          }
          pepFracStep.setAnalyteSamplesAndExperiments(protFracStep.getResults());
        }
        // TSV and Registration Step
        if (event.getActivatedStep().equals(regStep)) {
          regStep.enableDownloads(false);
          // Test samples were filled out
          if (w.getSteps().contains(steps.get(Steps.Test_Samples))) {
            boolean afterMS = w.getSteps().contains(steps.get(Steps.Protein_Fractionation));
            if (!testPoolsSet && !afterMS)
              dataAggregator.prepareTestSamples();
            if (techStep.hasMHCLigands())
              dataAggregator.prepareMHCExtractSamplesAndExperiments();
            List<AOpenbisSample> all = new ArrayList<AOpenbisSample>();
            if (!afterMS) {
              all.addAll(dataAggregator.getTests());
              // all.addAll(
              dataAggregator.createPoolingSamples(poolStep2.getPools());
              dataAggregator.setTests(all);
            }
            if (containsFractionation()) {
              dataAggregator
                  .setFractionationExperimentsProperties(getFractionationPropertiesFromLastStep());
              dataAggregator.createFractionationSamplesAndExperiments();
            }
            createTSV();
            try {
              prep.processTSV(dataAggregator.getTSV(), new QBiCDesignReader(), false);
            } catch (IOException | JAXBException e) {
              e.printStackTrace();
            }
            armDownloadButtons(regStep.getDownloadButton());
            List<SampleSummaryBean> summaries = prep.getSummary();
            Map<String, String> taxMap = new HashMap<String, String>();
            for (Map.Entry<String, String> entry : vocabularies.getTaxMap().entrySet())
              taxMap.put(entry.getValue(), entry.getKey());
            for (SampleSummaryBean s : summaries) {
              List<String> translations = new ArrayList<String>();
              for (String id : s.getSampleContent().split(", ")) {
                String translation = taxMap.get(id);
                if (translation != null)
                  translations.add(translation);
                else
                  translations.add(id);
              }
              s.setSampleContent(String.join(", ", translations));
            }
            ExperimentalDesignPropertyWrapper preliminaryDesign =
                prep.getExperimentalDesignProperties();
            List<TechnologyType> techTypes = prep.getTechnologyTypes();
            entitiesToUpdate = dataAggregator.getEntitiesToUpdate(preliminaryDesign, techTypes);
            newExperimentalDesignXML = null;
            if (entitiesToUpdate.isEmpty()) {
              try {
                newExperimentalDesignXML = ParserHelpers.createDesignXML(preliminaryDesign,
                    techTypes, new HashSet<String>());
              } catch (JAXBException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
            }

            regStep.setSummary(summaries);
            regStep.setProcessed(prep.getProcessed());
          }
          regStep.testRegEnabled();
          // Write TSV mode
          if (contextStep.fetchTSVModeSet()) {
            try {
              dataAggregator.parseAll();
            } catch (JAXBException e1) {
              e1.printStackTrace();
            }
            createTSV();
            try {
              prep.processTSV(dataAggregator.getTSV(), new QBiCDesignReader(), false);
            } catch (IOException | JAXBException e) {
              e.printStackTrace();
            }
            armDownloadButtons(regStep.getDownloadButton());
            List<SampleSummaryBean> summaries = prep.getSummary();
            Map<String, String> taxMap = new HashMap<String, String>();
            for (Map.Entry<String, String> entry : vocabularies.getTaxMap().entrySet())
              taxMap.put(entry.getValue(), entry.getKey());
            for (SampleSummaryBean s : summaries) {
              String translation = taxMap.get(s.getSampleContent());
              if (translation != null)
                s.setSampleContent(translation);
            }
            regStep.setSummary(summaries);
          }
        }
        if (event.getActivatedStep().equals(finishStep)) {
          ProjectContextStep context = (ProjectContextStep) steps.get(Steps.Project_Context);
          String space = context.getSpaceCode();
          String proj = context.getProjectCode();
          int timeout = 20;
          while (!openbis.projectExists(space, proj) && timeout > 0) {
            logger.error(
                "Project " + proj + " (" + space + ") " + "could not be found after registration.");
            logger.debug("timeout in " + timeout);
            timeout--;
            try {
              Thread.sleep(500);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
          Project project = openbis.getProjectByIdentifier("/" + space + "/" + proj);
          Map<String, List<Sample>> samplesByExperiment = new HashMap<String, List<Sample>>();
          for (Sample sample : openbis
              .getSamplesOfProject(project.getIdentifier().getIdentifier())) {
            if (sample.getExperiment() != null) {
              String expCode = sample.getExperiment().getCode();
              if (samplesByExperiment.containsKey(expCode)) {
                List<Sample> samples = samplesByExperiment.get(expCode);
                samples.add(sample);
                samplesByExperiment.put(expCode, samples);
              } else {
                List<Sample> samples = new ArrayList<Sample>(Arrays.asList(sample));
                samplesByExperiment.put(expCode, samples);
              }
            } else {
              logger.warn("No experiment found for sample " + sample.getCode());
            }
          }
          String designExpID = ExperimentCodeFunctions.getInfoExperimentID(space, proj);
          finishStep.setExperimentInfos(space, proj, designExpID, project.getDescription(),
              samplesByExperiment, openbis);
        }
      }

      private MSExperimentModel getFractionationPropertiesFromLastStep() {
        WizardStep lastInput = w.getSteps().get(w.getSteps().size() - 2);// last step is
                                                                         // registration itself
        if (lastInput instanceof PoolingStep) {
          return ((PoolingStep) lastInput).getPreliminarySamples();
        } else if (lastInput instanceof MSAnalyteStep) {
          MSAnalyteStep last = (MSAnalyteStep) lastInput;
          last.createPreliminaryExperiments();
          return last.getResults();
        } else {
          logger.error(
              "Tried to fetch fractionation properties from wizard but the last step was neither of type Pooling or Fractionation. Step in question is: "
                  + lastInput.toString());
          logger.error("Wizard likely stopped working before registration. User was " + user);
          Styles.notification("Error",
              "Sorry, something went wrong. Please notify a QBiC contact person.",
              NotificationType.ERROR);
          return null;
        }
      }

      private boolean containsFractionation() {
        List<Steps> relevant = new ArrayList<Steps>(
            Arrays.asList(Steps.Peptide_Fractionation, Steps.Peptide_Fractionation_Pooling,
                Steps.Protein_Fractionation, Steps.Protein_Fractionation_Pooling));
        boolean res = false;
        for (Steps s : relevant) {
          res |= w.getSteps().contains(steps.get(s));
        }
        return res;
      }
    };
    w.addListener(wl);
  }

  protected List<AOpenbisSample> filterForProperties(List<AOpenbisSample> samples, String property,
      String value) {
    List<AOpenbisSample> res = new ArrayList<AOpenbisSample>();
    for (AOpenbisSample sample : samples) {
      if (value.equals(sample.getValueMap().get(property)))
        res.add(sample);
    }
    return res;
  }

  protected void createTSV() {
    try {
      dataAggregator.createTSV();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }

  protected void initConditionListener(final ConditionInstanceStep step, final String amount,
      final List<AOpenbisSample> previousLevel) {

    ValueChangeListener listener = new ValueChangeListener() {
      /**
         * 
         */
      private static final long serialVersionUID = 7925081983580407077L;

      public void valueChange(ValueChangeEvent event) {
        reloadConditionsPreviewTable(step, amount, previousLevel);
      }
    };
    step.attachListener(listener);
  }

  protected void reloadConditionsPreviewTable(ConditionInstanceStep step, String amount,
      List<AOpenbisSample> previousLevel) {
    if (step.validInput()) {
      if (previousLevel.isEmpty()) {
        EntityStep entStep = (EntityStep) steps.get(Steps.Entities);
        step.buildTable(preparePreviewPermutations(step.getFactors(), entStep.isInfectionStudy()),
            amount);
      } else {
        step.buildTable(preparePreviewPermutations(step.getFactors(), previousLevel), amount);
      }
    } else {
      step.destroyTable();
    }

  }

  private void updateContextOptions(ProjectInformationComponent projSelection,
      ProjectContextStep contextStep) {
    // disable everything
    contextStep.disableContextOptions();

    // inputs to check
    String space = (String) contextStep.getSpaceBox().getValue();
    String existingProject = contextStep.getProjectCode();
    // String existingProject = (String) projSelection.getProjectBox().getValue();

    if (space != null && !space.isEmpty()) {
      // space is set
      if (existingProject != null && !existingProject.isEmpty()) {
        // known project selected, will deactivate generation
        projSelection.tryEnableCustomProject("");
        contextStep.enableNewContextOption(true);
        contextStep.makeContextVisible();
        boolean hasBioEntities = projectHasBioEntities(space, existingProject);
        boolean hasExtracts = projectHasExtracts(space, existingProject);
        contextStep.enableExtractContextOption(hasBioEntities);
        contextStep.enableMeasureContextOption(hasExtracts);
        contextStep.enableTSVWriteContextOption(hasBioEntities);
        contextStep.enableCopyContextOption(hasBioEntities);

        List<ExperimentBean> beans = new ArrayList<ExperimentBean>();
        logger.debug("set design null");
        dataAggregator.setExistingExpDesignExperiment(null, null);

        // long start = System.currentTimeMillis();

        List<ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment> experiments =
            v3API.getExperimentsWithSamplesOfProject(existingProject);
        String designExpID = ExperimentCodeFunctions.getInfoExperimentID(space, existingProject);

        // TimeUtils.logElapsedTime(start);

        for (ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment e : experiments) {
          String type = e.getType().getCode();
          String id = e.getIdentifier().getIdentifier();
          if (id.equals(designExpID)) {
            logger.info("setting design experiment");
            dataAggregator.setExistingExpDesignExperiment(e.getCode(), e.getProperties());
          }
          if (designExperimentTypes.contains(type)) {
            Date date = e.getRegistrationDate();
            SimpleDateFormat dt1 = new SimpleDateFormat("yy-MM-dd");
            String dt = "";
            if (date != null)
              dt = dt1.format(date);
            boolean pilot = false;
            if (e.getProperties().get("Q_IS_PILOT") != null)
              pilot = Boolean.parseBoolean(e.getProperties().get("Q_IS_PILOT"));
            int numOfSamples = e.getSamples().size();
            beans.add(new ExperimentBean(id, type, Integer.toString(numOfSamples), dt, pilot));
          }
        }
        // old
        // for (Experiment e : openbis.getExperimentsOfProjectByCode(existingProject)) {
        // String designExpID = ExperimentCodeFunctions.getInfoExperimentID(space, existingProject);
        // String type = e.getExperimentTypeCode();
        // String id = e.getIdentifier();
        // if (id.equals(designExpID)) {
        // logger.info("setting design experiment");
        // dataAggregator.setExistingExpDesignExperiment(e);
        // }
        // if (designExperimentTypes.contains(type)) {
        // Date date = e.getRegistrationDetails().getRegistrationDate();
        // SimpleDateFormat dt1 = new SimpleDateFormat("yy-MM-dd");
        // String dt = "";
        // if (date != null)
        // dt = dt1.format(date);
        // boolean pilot = false;
        // if (e.getProperties().get("Q_IS_PILOT") != null)
        // pilot = Boolean.parseBoolean(e.getProperties().get("Q_IS_PILOT"));
        // int numOfSamples = openbis.getSamplesofExperiment(e.getIdentifier()).size();
        // beans.add(new ExperimentBean(e.getIdentifier(), e.getExperimentTypeCode(),
        // Integer.toString(numOfSamples), dt, pilot));
        // }
        // }
        // TimeUtils.logElapsedTime(start);
        contextStep.setExperiments(beans);
      } else {
        // can create new project
        projSelection.getProjectField().setEnabled(true);
      }
    }
  }

  /**
   * Prepare all condition permutations for the user to set the amounts when conditions from a
   * previous tier are included
   * 
   * @param factorLists
   * @param previousTier Samples of the previous tier
   * @return
   */
  public List<String> preparePreviewPermutations(List<List<Property>> factorLists,
      List<AOpenbisSample> previousTier) {
    List<String> permutations = new ArrayList<String>();
    for (AOpenbisSample e : previousTier) {
      List<List<String>> res = new ArrayList<List<String>>();
      String secName = e.getQ_SECONDARY_NAME();
      if (secName == null)
        secName = "";
      String condKey = "(" + e.getCode().split("-")[1] + ") " + secName;
      res.add(new ArrayList<String>(Arrays.asList(condKey)));
      for (List<Property> instances : factorLists) {
        List<String> factorValues = new ArrayList<String>();
        for (Property f : instances) {
          String name = f.getValue();
          if (f.hasUnit())
            name = name + " " + f.getUnit().getValue();
          factorValues.add(name);
        }
        res.add(factorValues);
      }
      permutations.addAll(dataAggregator.generatePermutations(res, false));
    }
    return permutations;
  }

  /**
   * Prepare all condition permutations for the user to set the amounts
   * 
   * @param factorLists
   * @return
   */
  public List<String> preparePreviewPermutations(List<List<Property>> factorLists,
      boolean infectionStudy) {
    List<List<String>> res = new ArrayList<List<String>>();
    for (List<Property> instances : factorLists) {
      List<String> factorValues = new ArrayList<String>();
      for (Property f : instances) {
        String name = f.getValue();
        if (f.hasUnit())
          name = name + " " + f.getUnit().getValue();
        factorValues.add(name);
      }
      res.add(factorValues);
    }
    List<String> permutations = dataAggregator.generatePermutations(res, infectionStudy);
    return permutations;
  }

  protected void armDownloadButtons(Button tsv) {
    StreamResource tsvStream =
        getTSVStream(dataAggregator.getTSVContent(), dataAggregator.getTSVName());
    if (tsvDL == null) {
      tsvDL = new FileDownloader(tsvStream);
      tsvDL.extend(tsv);
    } else
      tsvDL.setFileDownloadResource(tsvStream);
  }

  public StreamResource getGraphStream(final String content, String name) {
    StreamResource resource = new StreamResource(new StreamResource.StreamSource() {
      @Override
      public InputStream getStream() {
        try {
          InputStream is = new ByteArrayInputStream(content.getBytes());
          return is;
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }
      }
    }, String.format("%s.graphml", name));
    return resource;
  }

  private void registerProjectOnly(String desc, String altTitle, String user,
      SummaryRegisterStep regStep) {
    ProjectContextStep context = (ProjectContextStep) steps.get(Steps.Project_Context);
    String space = context.getSpaceCode();
    String code = context.getProjectCode();

    //
    boolean imgSupport = context.hasImagingSupport();

    logger.info("project: " + code + " xxxxxxx");
    logger.info("desc: " + desc);
    logger.info("img: " + imgSupport);

    if (imgSupport) {
      omeroAdapter.registerProject(code, desc);
    }

    boolean success = false;
    try {
      success = openbisCreator.setupEmptyProject(space, code, desc);
    } catch (JAXBException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    if (!success) {
      Styles.notification("An error occured when trying to register the new project.",
          openbisCreator.getErrors(), NotificationType.ERROR);
      return;
    }
    // will register people to the db and send a success message
    boolean sqlDown = false;
    try {
      performPostRegistrationTasks(success);
    } catch (SQLException e) {
      sqlDown = true;
    }
    regStep.registrationDone(sqlDown, getRegistrationError());
    // Functions.notification("Success", "Project was registered!",
    // NotificationType.SUCCESS);
  }

  @Override
  public String getRegistrationError() {
    return openbisCreator.getErrors();
  }

  public StreamResource getTSVStream(final String content, String name) {
    StreamResource resource = new StreamResource(new StreamResource.StreamSource() {
      @Override
      public InputStream getStream() {
        try {
          InputStream is = new ByteArrayInputStream(content.getBytes());
          return is;
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }
      }
    }, String.format("%s.tsv", name));
    return resource;
  }

  private void writeNoteToOpenbis(String id, Note note) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("id", id);
    params.put("user", note.getUsername());
    params.put("comment", note.getComment());
    params.put("time", note.getTime());
    openbis.ingest("DSS1", "add-to-xml-note", params);
  }

  @Override
  public void performPostRegistrationTasks(boolean success) throws SQLException {
    if (success) {
      ProjectContextStep contextStep = (ProjectContextStep) steps.get(Steps.Project_Context);
      String projectIdentifier =
          "/" + contextStep.getSpaceCode() + "/" + contextStep.getProjectCode();
      String projectName = contextStep.getExpSecondaryName();
      List<OpenbisExperiment> exps = dataAggregator.getExperiments();
      if (exps == null)
        exps = new ArrayList<OpenbisExperiment>();
      int investigatorID = -1;
      int contactID = -1;
      int managerID = -1;
      if (!contextStep.getPerson(PersonType.Investigator).equals(""))
        investigatorID =
            vocabularies.getPeople().get(contextStep.getPerson(PersonType.Investigator));
      if (!contextStep.getPerson(PersonType.Manager).equals(""))
        managerID = vocabularies.getPeople().get(contextStep.getPerson(PersonType.Manager));
      if (!contextStep.getPerson(PersonType.Contact).equals(""))
        contactID = vocabularies.getPeople().get(contextStep.getPerson(PersonType.Contact));

      logger.info("Registration complete!");
      for (OpenbisExperiment e : exps) {
        if (e.getType().equals(ExperimentType.Q_EXPERIMENTAL_DESIGN)) {
          String id = projectIdentifier + "/" + e.getExperimentCode();
          for (Note n : notes) {
            writeNoteToOpenbis(id, n);
          }
        }
      }
      int projectID = dbm.addProjectToDB(projectIdentifier, projectName);
      if (investigatorID != -1)
        dbm.addPersonToProject(projectID, investigatorID, "PI");
      if (contactID != -1)
        dbm.addPersonToProject(projectID, contactID, "Contact");
      if (managerID != -1)
        dbm.addPersonToProject(projectID, managerID, "Manager");
      for (OpenbisExperiment e : exps) {
        String identifier = projectIdentifier + "/" + e.getExperimentCode();
        int expID = dbm.addExperimentToDB(identifier);
        if (e.getPersonID() > -1) {
          int person = e.getPersonID();
          dbm.addPersonToExperiment(expID, person, "Contact");
        }
      }
    } else {
      // nothing for now
    }
  }

  public void resetSpaces() {
    List<String> spaces = openbis.getUserSpaces(user);
    ((ProjectContextStep) steps.get(Steps.Project_Context)).setSpaces(spaces);
  }
}
