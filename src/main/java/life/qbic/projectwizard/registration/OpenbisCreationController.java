package life.qbic.projectwizard.registration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.operation.IOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.CreateExperimentsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.ExperimentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.update.ExperimentUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.create.CreateProjectsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.create.ProjectCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.CreateSamplesOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.create.CreateSpacesOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.create.SpaceCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.experiments.OpenbisExperiment;
import life.qbic.datamodel.identifiers.ExperimentCodeFunctions;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.xml.manager.StudyXMLParser;
import life.qbic.xml.study.Qexperiment;


/**
 * Provides methods to register new entities to openBIS using the openBIS Client API. Also performs
 * some simple sanity checks before sending data to openBIS.
 * 
 * @author Andreas Friedrich
 * 
 */
public class OpenbisCreationController {
  final int RETRY_UNTIL_SECONDS_PASSED = 5;
  final int SPLIT_AT_ENTITY_SIZE = 500;
  private IOpenBisClient openbis;
  private OpenbisV3APIWrapper api;
  // private String sessionToken;
  private static final Logger logger = LogManager.getLogger(OpenbisCreationController.class);
  private String errors;

  public OpenbisCreationController(IOpenBisClient openbis, OpenbisV3APIWrapper v3API) {
    this.openbis = openbis;
    this.api = v3API;
  }

  /**
   * Interact with an ingestion service script registered for the openBIS instance
   * 
   * @param ingestionService Name of the ingestions service script registered at openBIS
   * @param params HashMap of String parameter names and their arguments for the ingestion service
   */
  public void openbisGenericIngest(String ingestionService, HashMap<String, Object> params) {
    openbis.ingest("DSS1", ingestionService, params);
  }

  /**
   * Creates a space in openBIS and adds (existing) users with different rights using ingestion
   * scripts on the server
   * 
   * @param name The name of the space to create
   * @param userInfo HashMap of type HashMap<OpenbisSpaceUserRole,ArrayList<String>> containing one
   *        or more users of one or more types
   */
  // public void registerSpace(String name, HashMap<OpenbisSpaceUserRole, ArrayList<String>>
  // userInfo,
  // String user) {
  // Map<String, Object> params = new HashMap<String, Object>();
  // params.put("code", name);
  // params.put("registration_user", user);
  // for (OpenbisSpaceUserRole type : OpenbisSpaceUserRole.values()) {
  // if (userInfo.containsKey(type))
  // params.put(type.toString().toLowerCase(), userInfo.get(type));
  // }
  // // call ingestion service for space creation
  // openbis.ingest("DSS1", "register-space", params);
  // }

  /**
   * Creates a space in openBIS using v3 API
   * 
   * @param name The name of the space to create
   * @param description space description
   */
  public boolean registerSpaceV3(String name, String description) {
    SpaceCreation space = new SpaceCreation();
    space.setCode(name);
    space.setDescription(description);

    logger.info("creating space");

    // List<SpacePermId> permIds = api.createSpaces(sessionToken, Arrays.asList(space));
    // logger.info("space created:");
    // logger.info(permIds);

    IOperation operation = new CreateSpacesOperation(space);
    return api.handleOperations(operation);
  }

  /**
   * Create a project belonging to a space in openBIS using v3 API
   * 
   * @param space Existing space the project to create should resides in
   * @param name Name of the project to create
   * @param description Project description
   * @return false, if the specified space doesn't exist, resulting in failure, true otherwise
   */
  public boolean registerProjectV3(String space, String name, String description) {
    errors = "";
    if (!openbis.spaceExists(space)) {
      errors = space + " does not exist!";
      logger.error(errors);
      return false;
    }
    logger.info("Creating project " + name + " in space " + space);
    if (description == null || description.isEmpty()) {
      description = "Created using the project wizard.";
      logger.warn("No project description input found. Setting standard info.");
    }

    ProjectCreation project = new ProjectCreation();
    project.setCode(name);
    project.setSpaceId(new SpacePermId(space));
    project.setDescription(description);
    // List<ProjectPermId> permIds2 = api.createProjects(sessionToken, Arrays.asList(project));
    // logger.info("created: " + permIds2);

    IOperation operation = new CreateProjectsOperation(project);
    return api.handleOperations(operation);
  }

  /**
   * Create a project belonging to a space in openBIS using ingestion scripts on the server
   * 
   * @param space Existing space the project to create should resides in
   * @param name Name of the project to create
   * @param description Project description
   * @return false, if the specified space doesn't exist, resulting in failure, true otherwise
   */
  // public boolean registerProject(String space, String name, String description, String user) {
  // errors = "";
  // if (!openbis.spaceExists(space)) {
  // errors = space + " does not exist!";
  // logger.error(errors);
  // return false;
  // }
  // logger.info("Creating project " + name + " in space " + space);
  // if (description == null || description.isEmpty()) {
  // description = "Created using the project wizard.";
  // logger.warn("No project description input found. Setting standard info.");
  // }
  // Map<String, Object> params = new HashMap<String, Object>();
  // params.put("user", user);
  // params.put("code", name);
  // params.put("space", space);
  // params.put("desc", description);
  // openbis.ingest("DSS1", "register-proj", params);
  // return true;
  // }

  /**
   * Create an experiment belonging to a project (and space) using ingestion scripts on the server
   * 
   * @param space Existing space in openBis
   * @param project Existing project in the space that this experiment will belong to
   * @param expType openBIS experiment type
   * @param name Experiment name
   * @param map Additional properties of the experiment
   * @return false, if the specified project doesn't exist, resulting in failure, true otherwise
   */
  // public boolean registerExperiment(String space, String project, ExperimentType expType,
  // String name, Map<String, Object> map, String user) {
  // logger.info("Creating experiment " + name);
  // errors = "";
  // if (!openbis.projectExists(space, project)) {
  // errors = project + " in " + space + " does not exist.";
  // logger.error(errors);
  // return false;
  // }
  // Map<String, Object> params = new HashMap<String, Object>();
  // params.put("code", name);
  // params.put("type", expType.toString());
  // params.put("project", project);
  // params.put("space", space);
  // params.put("properties", map);
  // params.put("user", user);
  // openbis.ingest("DSS1", "register-exp", params);
  // return true;
  // }

  /**
   * Create an experiment belonging to a project (and space) using v3 api
   * 
   * @param space Existing space in openBis
   * @param project Existing project in the space that this experiment will belong to
   * @param expType openBIS experiment type
   * @param name Experiment name
   * @param map Additional properties of the experiment
   * @return false, if the specified project doesn't exist, resulting in failure, true otherwise
   */
  public boolean registerExperimentV3(String space, String project, ExperimentType expType,
      String name, Map<String, Object> map) {
    logger.info("Creating experiment " + name);
    errors = "";
    if (!openbis.projectExists(space, project)) {
      errors = project + " in " + space + " does not exist.";
      logger.error(errors);
      return false;
    }
    ExperimentCreation exp = new ExperimentCreation();
    exp.setCode(name);
    exp.setProjectId(new ProjectIdentifier(space, project));
    exp.setTypeId(new EntityTypePermId(expType.toString()));
    Map<String, String> props = new HashMap<>();
    for (String key : map.keySet()) {
      props.put(key, map.get(key).toString());
    }
    exp.setProperties(props);

    // List<ExperimentPermId> permIds3 = api.createExperiments(sessionToken, Arrays.asList(exp));
    // logger.info("experiments created: " + permIds3);

    IOperation operation = new CreateExperimentsOperation(exp);
    return api.handleOperations(operation);
  }

  public boolean registerExperimentsV3(String space, String proj,
      List<RegisterableExperiment> exps) {
    List<ExperimentCreation> expCreations = new ArrayList<>();

    for (RegisterableExperiment e : exps) {
      if (!openbis.expExists(space, proj, e.getCode())) {
        ExperimentCreation exp = new ExperimentCreation();
        exp.setCode(e.getCode());
        exp.setProjectId(new ProjectIdentifier(space, proj));
        exp.setTypeId(new EntityTypePermId(e.getType()));
        exp.setProperties(e.getStringProperties());
        expCreations.add(exp);
      }
    }
    if (exps.size() > 0) {
      // List<ExperimentPermId> permIds3 = api.createExperiments(sessionToken, expCreations);
      logger.info("Sending " + expCreations.size() + " new experiments to the V3 API.");
      IOperation operation = new CreateExperimentsOperation(expCreations);
      return api.handleOperations(operation);
    }
    return true;
  }

  // public boolean registerExperiments(String space, String proj, List<RegisterableExperiment>
  // exps,
  // String user) {
  // errors = "";
  // int step = 100;
  // int max = RETRY_UNTIL_SECONDS_PASSED * 1000;
  // List<String> codes = new ArrayList<String>();
  // List<String> types = new ArrayList<String>();
  // List<Map<String, Object>> props = new ArrayList<Map<String, Object>>();
  // for (RegisterableExperiment e : exps) {
  // if (!openbis.expExists(space, proj, e.getCode())) {
  // codes.add(e.getCode());
  // types.add(e.getType());
  // props.add(e.getProperties());
  // }
  // }
  // if (codes.size() > 0) {
  // while (!openbis.projectExists(space, proj) && max > 0) {
  // try {
  // Thread.sleep(step);
  // max -= step;
  // } catch (InterruptedException e) {
  // logger.error("thread sleep waiting for experiment creation interruped.");
  // e.printStackTrace();
  // }
  // }
  // logger.info("Creating experiments " + codes);
  // if (!openbis.projectExists(space, proj)) {
  // errors = proj + " in " + space + " does not exist. Not creating experiments.";
  // logger.error(errors);
  // return false;
  // }
  // Map<String, Object> params = new HashMap<String, Object>();
  // params.put("codes", codes);
  // params.put("types", types);
  // params.put("project", proj);
  // params.put("space", space);
  // params.put("properties", props);
  // params.put("user", user);
  // openbis.ingest("DSS1", "register-exp", params);
  // }
  // return true;
  // }

  private List<List<ISampleBean>> splitSamplesIntoBatches(List<ISampleBean> samples,
      int targetSize) {
    List<List<ISampleBean>> res = new ArrayList<List<ISampleBean>>();
    int size = samples.size();
    if (size < targetSize)
      return new ArrayList<List<ISampleBean>>(Arrays.asList(samples));
    for (int i = 0; i < size / targetSize; i++) {
      int from = i * targetSize;
      int to = (i + 1) * targetSize;
      res.add(samples.subList(from, to));
      if (to > size - targetSize && to != size)
        res.add(samples.subList(to, size));
    }
    return res;
  }

  /**
   * this is the one normally called!
   * 
   * @param tsvSampleHierarchy
   * @param description
   * @param secondaryName
   * @param bar
   * @param info
   * @param ready
   * @param user
   */
  public void registerProjectWithExperimentsAndSamplesBatchWiseV3(
      final List<List<ISampleBean>> tsvSampleHierarchy, final String description,
      final List<OpenbisExperiment> informativeExperiments, final ProgressBar bar, final Label info,
      final Runnable ready, final String user, Map<String, Map<String, Object>> entitiesToUpdate,
      final boolean isPilot) {
    errors = "";

    RegisterableProject p =
        new RegisterableProject(tsvSampleHierarchy, description, informativeExperiments, isPilot);

    for (String experiment : entitiesToUpdate.keySet()) {
      String expID = ExperimentCodeFunctions.getExperimentIdentifier(p.getSpace(),
          p.getProjectCode(), experiment);
      long modificationTime = openbis.getExperimentById2(expID).get(0).getRegistrationDetails()
          .getModificationDate().getTime();

      updateExperimentV3(expID, entitiesToUpdate.get(experiment));

      long newModificationTime = modificationTime;
      double TIMEOUT = 10000;

      while (newModificationTime == modificationTime && TIMEOUT > 0) {
        newModificationTime = openbis.getExperimentById2(expID).get(0).getRegistrationDetails()
            .getModificationDate().getTime();
        TIMEOUT -= 300;
        try {
          Thread.sleep(300);
        } catch (InterruptedException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }

      }
      if (TIMEOUT < 0) {
        errors = "could not update existing experimental design, not registering samples!";
        logger.error(errors);
        return;
      } else {
        logger.debug("completed update of experimental design successfully");
      }
    }

    logger.debug("User sending samples: " + user);
    Thread t = new Thread(new Runnable() {
      volatile int current = -1;

      @Override
      public void run() {
        info.setCaption("Collecting information");
        UI.getCurrent().access(new UpdateProgressBar(bar, info, 0.01));
        List<RegisterableExperiment> exps = p.getExperiments();
        String space = p.getSpace().toUpperCase();
        String project = p.getProjectCode();
        String desc = p.getDescription();

        int splitSteps = 0;
        // find out which experiments have so many samples they should be sent in multiple packages
        for (RegisterableExperiment exp : exps) {
          splitSteps += exp.getSamples().size() / (SPLIT_AT_ENTITY_SIZE + 1);
        }

        final int todo = tsvSampleHierarchy.size() + splitSteps + 1;
        // bar
        current++;
        double frac = current * 1.0 / todo;
        info.setCaption("Registering Project and Experiments");
        UI.getCurrent().access(new UpdateProgressBar(bar, info, frac));
        boolean success = false;
        if (!openbis.projectExists(space, project)) {
          success = registerProjectV3(space, project, desc);
          if (!success) {
            errors = "Project could not be registered.";
          }
        }
        if (success) {
          success = registerExperimentsV3(space, project, exps);
          if (!success) {
            errors = "Experiments could not be registered.";
          }
        }
        if (!success) {
          // experiments were not registered, break registration
          bar.setVisible(false);
          info.setCaption("An error occured.");
          UI.getCurrent().setPollInterval(-1);
          UI.getCurrent().access(ready);
          return;
        }
        int i = 0;
        for (List<ISampleBean> level : tsvSampleHierarchy) {
          i++;
          logger.info("registration of level " + i);
          info.setCaption("Registering samples");
          info.setValue("");
          current++;
          frac = current * 1.0 / todo;
          UI.getCurrent().access(new UpdateProgressBar(bar, info, frac));
          boolean batchSuccess;
          if (level.size() > SPLIT_AT_ENTITY_SIZE) {
            for (List<ISampleBean> batch : splitSamplesIntoBatches(level, SPLIT_AT_ENTITY_SIZE)) {
              batchSuccess = registerSampleBatchV3(batch);
              if (!batchSuccess) {
                bar.setVisible(false);
                info.setCaption("An error occured.");
                UI.getCurrent().setPollInterval(-1);
                UI.getCurrent().access(ready);
                return;
              }
              current++;
              frac = current * 1.0 / todo;
              UI.getCurrent().access(new UpdateProgressBar(bar, info, frac));
            }
          } else {
            batchSuccess = registerSampleBatchV3(level);
            if (!batchSuccess) {
              bar.setVisible(false);
              info.setCaption("An error occured.");
              UI.getCurrent().setPollInterval(-1);
              UI.getCurrent().access(ready);
              return;
            }
          }
        }
        current++;
        frac = current * 1.0 / todo;
        UI.getCurrent().access(new UpdateProgressBar(bar, info, frac));

        UI.getCurrent().setPollInterval(-1);
        UI.getCurrent().access(ready);
        api.logout();
      }
    });
    t.start();
    UI.getCurrent().setPollInterval(100);
  }

  private void updateExperimentV3(String expID, Map<String, Object> map) {
    ExperimentUpdate exp = new ExperimentUpdate();
    exp.setExperimentId(new ExperimentIdentifier(expID));
    Map<String, String> props = new HashMap<>();
    for (String key : map.keySet()) {
      props.put(key, map.get(key).toString());
    }
    exp.setProperties(props);

    logger.info("updating " + expID);
    // api.updateExperiments(sessionToken, Arrays.asList(exp));
    api.updateExperiments(Arrays.asList(exp));
  }

  /**
   * this is the one normally called!
   * 
   * @param tsvSampleHierarchy
   * @param description
   * @param secondaryName
   * @param bar
   * @param info
   * @param ready
   * @param user
   */
  // public void registerProjectWithExperimentsAndSamplesBatchWise(
  // final List<List<ISampleBean>> tsvSampleHierarchy, final String description,
  // final List<OpenbisExperiment> informativeExperiments, final ProgressBar bar, final Label info,
  // final Runnable ready, final String user, Map<String, Map<String, Object>> entitiesToUpdate,
  // final boolean isPilot) {
  // errors = "";
  //
  // RegisterableProject p =
  // new RegisterableProject(tsvSampleHierarchy, description, informativeExperiments, isPilot);
  //
  // for (String experiment : entitiesToUpdate.keySet()) {
  // String expID = ExperimentCodeFunctions.getExperimentIdentifier(p.getSpace(),
  // p.getProjectCode(), experiment);
  // long modificationTime = openbis.getExperimentById2(expID).get(0).getRegistrationDetails()
  // .getModificationDate().getTime();
  //
  // HashMap<String, Object> parameters = new HashMap<String, Object>();
  // parameters.put("user", user);
  // parameters.put("identifier", expID);
  // parameters.put("properties", entitiesToUpdate.get(experiment));
  // openbis.triggerIngestionService("update-experiment-metadata", parameters);
  //
  // long newModificationTime = modificationTime;
  // double TIMEOUT = 10000;
  //
  // while (newModificationTime == modificationTime && TIMEOUT > 0) {
  // newModificationTime = openbis.getExperimentById2(expID).get(0).getRegistrationDetails()
  // .getModificationDate().getTime();
  // TIMEOUT -= 300;
  // try {
  // Thread.sleep(300);
  // } catch (InterruptedException e1) {
  // // TODO Auto-generated catch block
  // e1.printStackTrace();
  // }
  //
  // }
  // if (TIMEOUT < 0) {
  // errors = "could not update existing experimental design, not registering samples!";
  // logger.error(errors);
  // return;
  // } else {
  // logger.debug("completed update of experimental design successfully");
  // }
  // }
  //
  // logger.debug("User sending samples: " + user);
  // Thread t = new Thread(new Runnable() {
  // volatile int current = -1;
  //
  // @Override
  // public void run() {
  // info.setCaption("Collecting information");
  // UI.getCurrent().access(new UpdateProgressBar(bar, info, 0.01));
  // List<RegisterableExperiment> exps = p.getExperiments();
  // String space = p.getSpace().toUpperCase();
  // String project = p.getProjectCode();
  // String desc = p.getDescription();
  //
  // int splitSteps = 0;
  // // find out which experiments have so many samples they should be sent in multiple packages
  // for (RegisterableExperiment exp : exps) {
  // splitSteps += exp.getSamples().size() / (SPLIT_AT_ENTITY_SIZE + 1);
  // }
  //
  // // final int todo = exps.size() + splitSteps + 1;// TODO huge number of samples should be
  // // split
  // // into groups
  // final int todo = tsvSampleHierarchy.size() + splitSteps + 1;
  // // of 50 or 100. this needs to be reflected in the progress
  // // bar
  // current++;
  // double frac = current * 1.0 / todo;
  // info.setCaption("Registering Project and Experiments");
  // UI.getCurrent().access(new UpdateProgressBar(bar, info, frac));
  // if (!openbis.projectExists(space, project))
  // registerProject(space, project, desc, user);
  // boolean success = registerExperiments(space, project, exps, user);
  // if (!success) {
  // // experiments were not registered, break registration
  // errors = "Experiments could not be registered.";
  // bar.setVisible(false);
  // info.setCaption("An error occured.");
  // UI.getCurrent().setPollInterval(-1);
  // UI.getCurrent().access(ready);
  // return;
  // }
  //
  // try {
  // Thread.sleep(500);
  // } catch (InterruptedException e) {
  // logger.error("thread sleep waiting for experiment creation interruped.");
  // e.printStackTrace();
  // }
  // // for (RegisterableExperiment exp : exps) { old version!
  // int i = 0;
  // for (List<ISampleBean> level : tsvSampleHierarchy) {
  // i++;
  // logger.info("registration of level " + i);
  // // List<ISampleBean> level = exp.getSamples(); old version!
  // info.setCaption("Registering samples");
  // current++;
  // frac = current * 1.0 / todo;
  // UI.getCurrent().access(new UpdateProgressBar(bar, info, frac));
  // boolean batchSuccess;
  // if (level.size() > SPLIT_AT_ENTITY_SIZE) {
  // for (List<ISampleBean> batch : splitSamplesIntoBatches(level, SPLIT_AT_ENTITY_SIZE)) {
  // batchSuccess = registerSampleBatchInETL(batch, user);
  // if (!batchSuccess) {
  // bar.setVisible(false);
  // info.setCaption("An error occured.");
  // UI.getCurrent().setPollInterval(-1);
  // UI.getCurrent().access(ready);
  // return;
  // }
  // ISampleBean last = batch.get(batch.size() - 1);
  // logger.info("waiting for last batch sample to reach openbis");
  // int step = 100;
  // int max = RETRY_UNTIL_SECONDS_PASSED * 1000;
  // while (!openbis.sampleExists(last.getCode()) && max > 0) {
  // try {
  // Thread.sleep(step);
  // max -= step;
  // } catch (InterruptedException e) {
  // e.printStackTrace();
  // }
  // }
  // current++;
  // frac = current * 1.0 / todo;
  // UI.getCurrent().access(new UpdateProgressBar(bar, info, frac));
  // }
  // } else {
  // batchSuccess = registerSampleBatchInETL(level, user);
  // if (!batchSuccess) {
  // bar.setVisible(false);
  // info.setCaption("An error occured.");
  // UI.getCurrent().setPollInterval(-1);
  // UI.getCurrent().access(ready);
  // return;
  // }
  // }
  // if (level.size() > 0) {
  // ISampleBean last = level.get(level.size() - 1);
  // logger.info("waiting for last sample to reach openbis");
  // int step = 50;
  // int max = RETRY_UNTIL_SECONDS_PASSED * 1000;
  // while (!openbis.sampleExists(last.getCode()) && max > 0) {
  // try {
  // Thread.sleep(step);
  // max -= step;
  // } catch (InterruptedException e) {
  // e.printStackTrace();
  // }
  // }
  // }
  // }
  // current++;
  // frac = current * 1.0 / todo;
  // UI.getCurrent().access(new UpdateProgressBar(bar, info, frac));
  //
  // UI.getCurrent().setPollInterval(-1);
  // UI.getCurrent().access(ready);
  // }
  // });
  // t.start();
  // UI.getCurrent().setPollInterval(100);
  // }

  public boolean registerSampleBatchV3(List<ISampleBean> samples) {
    List<SampleCreation> newSamples = new ArrayList<>();

    // String s = null;
    // String p = null;
    // String e = null;
    if (samples.size() == 0)
      return true;
    // to speed up things only the first sample and its experiment is checked for existence, might
    // lead to errors
    // ISampleBean first = samples.get(0);
    // if (!first.getExperiment().equals(e)) {
    // s = first.getSpace();
    // p = first.getProject();
    // e = first.getExperiment();
    // if (!openbis.expExists(s, p, e)) {
    // errors = e + " not found in " + p + " (" + s + ") Stopping registration of samples.";
    // logger.error(errors + " This will most likely lead to openbis errors or lost samples!");
    // return false;
    // }
    // }

    for (ISampleBean sample : samples) {
      if (openbis.sampleExists(sample.getCode())) {
        logger.warn(sample.getCode() + " already exists."
            + " Removing this sample from registration process.");
      } else {

        SampleCreation sampleCreation = new SampleCreation();
        String space = sample.getSpace();
        sampleCreation.setTypeId(new EntityTypePermId(sample.getType()));
        sampleCreation.setSpaceId(new SpacePermId(space));

        List<SampleIdentifier> parents = new ArrayList<>();
        for (String parent : sample.getParentIDs()) {
          parents.add(new SampleIdentifier(space, null, parent));
        }
        sampleCreation.setParentIds(parents);
        sampleCreation.setExperimentId(new ExperimentIdentifier(
            "/" + space + "/" + sample.getProject() + "/" + sample.getExperiment()));
        sampleCreation.setCode(sample.getCode());


        Map<String, String> props = new HashMap<>();
        if (!sample.getSecondaryName().isEmpty()) {
          props.put("Q_SECONDARY_NAME", sample.getSecondaryName());
        }
        for (String key : sample.getMetadata().keySet()) {
          props.put(key, sample.getMetadata().get(key).toString());
        }
        sampleCreation.setProperties(props);

        newSamples.add(sampleCreation);
      }
    }
    logger.info("Sending " + newSamples.size() + " new samples to V3 API.");
    // List<SamplePermId> permIds4 = api.createSamples(sessionToken, newSamples);
    // logger.info("created: " + permIds4);

    IOperation operation = new CreateSamplesOperation(newSamples);
    return api.handleOperations(operation);
  }

  // public boolean registerSampleBatchInETL(List<ISampleBean> samples, String user) {
  // String s = null;
  // String p = null;
  // String e = null;
  // if (samples.size() == 0)
  // return true;
  // // to speed up things only the first sample and its experiment is checked for existence, might
  // // lead to errors
  // ISampleBean first = samples.get(0);
  // if (!first.getExperiment().equals(e)) {
  // s = first.getSpace();
  // p = first.getProject();
  // e = first.getExperiment();
  // if (!openbis.expExists(s, p, e)) {
  // errors = e + " not found in " + p + " (" + s + ") Stopping registration of samples.";
  // logger.error(errors + " This will most likely lead to openbis errors or lost samples!");
  // return false;
  // }
  // }
  // Map<String, Object> params = new HashMap<String, Object>();
  // params.put("user", user);
  //
  // for (ISampleBean sample : samples) {
  // if (openbis.sampleExists(sample.getCode())) {
  // logger.warn(sample.getCode() + " already exists in " + p
  // + " Removing this sample from registration process.");
  // } else {
  // String space = sample.getSpace();
  // String project = sample.getProject();
  // String exp = sample.getExperiment();
  // List<String> parents = sample.getParentIDs();
  // Map<String, Object> map = new HashMap<String, Object>();
  // map.put("code", sample.getCode());
  // map.put("space", space);
  // map.put("project", project);
  // map.put("experiment", exp);
  // map.put("type", sample.getType());
  // if (!sample.getSecondaryName().isEmpty()) {
  // map.put("Q_SECONDARY_NAME", sample.getSecondaryName());
  // }
  // if (!parents.isEmpty())
  // map.put("parents", parents);
  // map.put("metadata", sample.getMetadata());
  // params.put(sample.getCode(), map);
  // }
  // }
  // logger.info("Sending batch of new samples to Ingestion Service.");
  // openbis.ingest("DSS1", "register-sample-batch", params);
  // return true;
  // }

  public String getErrors() {
    return errors;
  }


  public boolean setupEmptyProjectV3(String space, String project, String description)
      throws JAXBException {
    registerProjectV3(space, project, description);
    StudyXMLParser p = new StudyXMLParser();
    JAXBElement<Qexperiment> res =
        p.createNewDesign(new HashSet<>(), new ArrayList<>(), new HashMap<>(), new HashMap<>());
    String emptyStudy = p.toString(res);

    String exp = project + "_INFO";
    String code = project + "000";

    Map<String, Object> props = new HashMap<>();
    props.put("Q_EXPERIMENTAL_SETUP", emptyStudy);

    boolean success =
        registerExperimentV3(space, project, ExperimentType.Q_PROJECT_DETAILS, exp, props);
    if (!success) {
      // experiments were not registered, break registration
      errors = "Info experiment could not be registered.";
      logger.error(errors);
      return false;
    }
    ISampleBean infoSample = new TSVSampleBean(code, exp, project, space, "Q_ATTACHMENT_SAMPLE", "",
        new ArrayList<String>(), new HashMap<String, Object>());
    success = registerSampleBatchV3(new ArrayList<ISampleBean>(Arrays.asList(infoSample)));
    if (!success) {
      // experiments were not registered, break registration
      errors = "Info sample could not be registered.";
      logger.error(errors);
      return false;
    } else {
      return true;
    }
  }

  // public boolean setupEmptyProject(String space, String project, String description, String user)
  // throws JAXBException {
  // registerProject(space, project, description, user);
  // StudyXMLParser p = new StudyXMLParser();
  // JAXBElement<Qexperiment> res =
  // p.createNewDesign(new HashSet<>(), new ArrayList<>(), new HashMap<>(), new HashMap<>());
  // String emptyStudy = p.toString(res);
  //
  // String exp = project + "_INFO";
  // String code = project + "000";
  //
  // Map<String, Object> props = new HashMap<>();
  // props.put("Q_EXPERIMENTAL_SETUP", emptyStudy);
  //
  // boolean success =
  // registerExperiment(space, project, ExperimentType.Q_PROJECT_DETAILS, exp, props, user);
  // if (!success) {
  // // experiments were not registered, break registration
  // errors = "Info experiment could not be registered.";
  // logger.error(errors);
  // return false;
  // }
  // ISampleBean infoSample = new TSVSampleBean(code, exp, project, space, "Q_ATTACHMENT_SAMPLE",
  // "",
  // new ArrayList<String>(), new HashMap<String, Object>());
  // success = registerSampleBatchInETL(new ArrayList<ISampleBean>(Arrays.asList(infoSample)),
  // user);
  // if (!success) {
  // // experiments were not registered, break registration
  // errors = "Info sample could not be registered.";
  // logger.error(errors);
  // return false;
  // } else {
  // return true;
  // }
  // }

}
