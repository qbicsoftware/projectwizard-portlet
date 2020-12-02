package life.qbic.projectwizard.registration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.operation.IOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.DataSetUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.CreateExperimentsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.ExperimentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.update.ExperimentUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.create.CreateProjectsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.create.ProjectCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.update.ProjectUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.CreateSamplesOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.SampleUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.create.CreateSpacesOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.create.SpaceCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.experiments.OpenbisExperiment;
import life.qbic.datamodel.identifiers.ExperimentCodeFunctions;
import life.qbic.datamodel.persons.OpenbisSpaceUserRole;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
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
public class OpenbisV3CreationController implements IOpenbisCreationController {
  final int RETRY_UNTIL_SECONDS_PASSED = 5;
  final int SPLIT_AT_ENTITY_SIZE = 300;
  private IOpenBisClient openbis;
  private OpenbisV3APIWrapper api;

  private static final Logger logger = LogManager.getLogger(OpenbisV3CreationController.class);
  private String errors;
  private String user;

  public OpenbisV3CreationController(IOpenBisClient openbis, String user,
      OpenbisV3APIWrapper v3API) {
    this.openbis = openbis;
    this.api = v3API;
    this.user = user;
  }

  /**
   * Creates a space in openBIS using v3 API
   * 
   * @param name The name of the space to create
   * @param description space description
   */
  @Override
  public boolean registerSpace(String name, String description,
      HashMap<OpenbisSpaceUserRole, ArrayList<String>> userInfo) {
    SpaceCreation space = new SpaceCreation();
    space.setCode(name);
    space.setDescription(description);

    logger.info("creating space");

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
  @Override
  public boolean registerProject(String space, String name, String description) {
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

    IOperation operation = new CreateProjectsOperation(project);
    return api.handleOperations(operation);
  }

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
  @Override
  public boolean registerExperiment(String space, String project, ExperimentType expType,
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

    IOperation operation = new CreateExperimentsOperation(exp);
    return api.handleOperations(operation);
  }

  @Override
  public boolean registerExperiments(String space, String proj, List<RegisterableExperiment> exps) {
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
    if (expCreations.size() > 0) {
      logger.info("Sending " + expCreations.size() + " new experiments to the V3 API.");
      IOperation operation = new CreateExperimentsOperation(expCreations);
      return api.handleOperations(operation);
    }
    return true;
  }

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
   * less convoluted version
   */
  @Override
  public void registerProjectWithExperimentsAndSamplesBatchWise(
      List<List<ISampleBean>> tsvSampleHierarchy, List<OpenbisExperiment> informativeExperiments,
      String description, boolean isPilot) {
    errors = "";

    RegisterableProject p =
        new RegisterableProject(tsvSampleHierarchy, description, informativeExperiments, isPilot);

    logger.debug("User sending samples: " + user);
    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        List<RegisterableExperiment> exps = p.getExperiments();
        String space = p.getSpace().toUpperCase();
        String project = p.getProjectCode();
        String desc = p.getDescription();

        boolean success = true;

        if (!openbis.projectExists(space, project)) {
          success = registerProject(space, project, desc);
          if (!success) {
            errors = "Project could not be registered.";
            UI.getCurrent().setPollInterval(-1);
          }
        }
        if (success) {
          success = registerExperiments(space, project, exps);
          if (!success) {
            errors = "Experiments could not be registered.";
          }
        }
        if (!success) {
          // experiments were not registered, break registration
          UI.getCurrent().setPollInterval(-1);
          return;
        }
        int i = 0;
        for (List<ISampleBean> level : tsvSampleHierarchy) {
          i++;
          logger.info("registration of level " + i);

          boolean batchSuccess;
          if (level.size() > SPLIT_AT_ENTITY_SIZE) {
            for (List<ISampleBean> batch : splitSamplesIntoBatches(level, SPLIT_AT_ENTITY_SIZE)) {
              batchSuccess = registerSampleBatch(batch);
              if (!batchSuccess) {
                UI.getCurrent().setPollInterval(-1);
                return;
              }
            }
          } else {
            batchSuccess = registerSampleBatch(level);
            if (!batchSuccess) {
              UI.getCurrent().setPollInterval(-1);
              return;
            }
          }
        }
        UI.getCurrent().setPollInterval(-1);
        api.logout();
      }
    });
    t.start();
    UI.getCurrent().setPollInterval(100);
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
  @Override
  public void registerProjectWithExperimentsAndSamplesBatchWise(
      final List<List<ISampleBean>> tsvSampleHierarchy, final String description,
      final List<OpenbisExperiment> informativeExperiments, final ProgressBar bar, final Label info,
      final Runnable ready, Map<String, Map<String, Object>> entitiesToUpdate,
      final boolean isPilot) {
    errors = "";

    RegisterableProject p =
        new RegisterableProject(tsvSampleHierarchy, description, informativeExperiments, isPilot);

    for (String experiment : entitiesToUpdate.keySet()) {
      String expID = ExperimentCodeFunctions.getExperimentIdentifier(p.getSpace(),
          p.getProjectCode(), experiment);
      long modificationTime = openbis.getExperimentById(expID).getModificationDate().getTime();

      updateExperiment(expID, entitiesToUpdate.get(experiment));

      long newModificationTime = modificationTime;
      double TIMEOUT = 10000;

      while (newModificationTime == modificationTime && TIMEOUT > 0) {
        newModificationTime = openbis.getExperimentById(expID).getModificationDate().getTime();
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
        boolean success = true;
        if (!openbis.projectExists(space, project)) {
          success = registerProject(space, project, desc);
          if (!success) {
            errors = "Project could not be registered.";
          }
        }
        if (success) {
          success = registerExperiments(space, project, exps);
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
              batchSuccess = registerSampleBatch(batch);
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
            batchSuccess = registerSampleBatch(level);
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

  public void updateProjectSpace(List<String> projectCodes, String oldSpace, String newSpace) {
    List<ProjectUpdate> updates = new ArrayList<>();
    for (String code : projectCodes) {
      ProjectUpdate p = new ProjectUpdate();
      p.setProjectId(new ProjectIdentifier(oldSpace, code));
      p.setSpaceId(new SpacePermId(newSpace));
      updates.add(p);
    }
    logger.info("updating spaces of projects: " + projectCodes);
    api.updateProjects(updates);
  }

  @Override
  public void updateExperiment(String expID, Map<String, Object> map) {
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

  public Map<String, List<String>> getDatasetPermIDsOfSamples(Set<String> codes) {
    Map<String, List<String>> res = new HashMap<>();
    for (String code : codes) {
      DataSetSearchCriteria criteria = new DataSetSearchCriteria();
      criteria.withOrOperator();
      criteria.withSample().withCode().thatEquals(code);
      // criteria.withType().withCode().thatEquals("MY_SAMPLE_TYPE_CODE");

      DataSetFetchOptions fetchOptions = new DataSetFetchOptions();

      SearchResult<DataSet> result = api.searchDatasets(criteria, fetchOptions);

      List<String> permIds = new ArrayList<>();

      for (DataSet d : result.getObjects()) {
        permIds.add(d.getPermId().getPermId());
      }
      res.put(code, permIds);
    }
    return res;
  }

  public void updateDatasets(List<String> ids, Map<String, Map<String, Object>> idsToProps,
      Map<String, SampleIdentifier> idsToSampleIDs) {
    List<DataSetUpdate> updates = new ArrayList<>();
    for (String permID : ids) {
      DataSetUpdate dsUpdate = new DataSetUpdate();
      dsUpdate.setDataSetId(new DataSetPermId(permID));

      // samples to update?
      if (idsToSampleIDs.containsKey(permID)) {
        dsUpdate.setSampleId(idsToSampleIDs.get(permID));
      }

      // props to update?
      if (idsToProps.containsKey(permID)) {
        Map<String, String> props = new HashMap<>();
        Map<String, Object> map = idsToProps.get(permID);
        for (String key : map.keySet()) {
          props.put(key, map.get(key).toString());
        }
        dsUpdate.setProperties(props);
      }

      updates.add(dsUpdate);
    }
    logger.info("updating dataset metadata for: " + ids);

    api.updateDataSets(updates);
  }

  /**
   * if identifier is found in parent id map, parent samples missing from the values list of the map
   * are removed
   * 
   * @param ids
   * @param idsToProps
   * @param idsToParentIDs
   */
  public void updateSamples(List<SampleIdentifier> ids,
      Map<SampleIdentifier, Map<String, Object>> idsToProps,
      Map<SampleIdentifier, List<SampleIdentifier>> idsToParentIDs) {
    List<SampleUpdate> updates = new ArrayList<>();
    for (SampleIdentifier id : ids) {
      SampleUpdate sampleUpdate = new SampleUpdate();
      sampleUpdate.setSampleId(id);

      if (!openbis.sampleExists(id.getIdentifier())) {
        logger
            .warn("sample " + id.getIdentifier() + " cannot be updated because it does not exist.");
        return;
      }

      // parents to update?
      if (idsToParentIDs.containsKey(id)) {
        System.out.println("updating id " + id);

        List<SampleIdentifier> newParents = idsToParentIDs.get(id);
        System.out.println("parents: " + newParents);
        sampleUpdate.getParentIds()
            .set(newParents.toArray(new SampleIdentifier[newParents.size()]));
        System.out.println(sampleUpdate);
      }

      // props to update?
      if (idsToProps.containsKey(id)) {
        Map<String, String> props = new HashMap<>();
        Map<String, Object> map = idsToProps.get(id);
        for (String key : map.keySet()) {
          props.put(key, map.get(key).toString());
        }
        sampleUpdate.setProperties(props);
      }

      updates.add(sampleUpdate);
    }
    logger.info("updating sample metadata for: " + ids);

    api.updateSamples(updates);
  }

  @Override
  public boolean registerSampleBatch(List<ISampleBean> samples) {
    List<SampleCreation> newSamples = new ArrayList<>();

    if (samples.size() == 0)
      return true;

    for (ISampleBean sample : samples) {
      if (openbis.sampleExists(sample.getCode())) {
        logger.warn(sample.getCode() + " already exists."
            + " Removing this sample from registration process.");
      } else {

        SampleCreation sampleCreation = new SampleCreation();
        String space = sample.getSpace();
        sampleCreation.setTypeId(new EntityTypePermId(sample.getType().toString()));
        sampleCreation.setSpaceId(new SpacePermId(space));

        List<SampleIdentifier> parents = new ArrayList<>();
        for (String parent : sample.getParentIDs()) {
          if (!parent.isEmpty()) {
            parents.add(new SampleIdentifier(space, null, parent));
          }
        }
        if (!parents.isEmpty()) {
          sampleCreation.setParentIds(parents);
        }
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

    IOperation operation = new CreateSamplesOperation(newSamples);
    return api.handleOperations(operation);
  }

  public String getErrors() {
    return errors;
  }

  @Override
  public boolean setupEmptyProject(String space, String project, String description)
      throws JAXBException {
    registerProject(space, project, description);
    StudyXMLParser p = new StudyXMLParser();
    JAXBElement<Qexperiment> res =
        p.createNewDesign(new HashSet<>(), new ArrayList<>(), new HashMap<>(), new HashMap<>());
    String emptyStudy = p.toString(res);

    String exp = project + "_INFO";
    String code = project + "000";

    Map<String, Object> props = new HashMap<>();
    props.put("Q_EXPERIMENTAL_SETUP", emptyStudy);

    boolean success =
        registerExperiment(space, project, ExperimentType.Q_PROJECT_DETAILS, exp, props);
    if (!success) {
      // experiments were not registered, break registration
      errors = "Info experiment could not be registered.";
      logger.error(errors);
      return false;
    }
    ISampleBean infoSample = new TSVSampleBean(code, exp, project, space,
        SampleType.Q_ATTACHMENT_SAMPLE, "", new ArrayList<String>(), new HashMap<String, Object>());
    success = registerSampleBatch(new ArrayList<ISampleBean>(Arrays.asList(infoSample)));
    if (!success) {
      // experiments were not registered, break registration
      errors = "Info sample could not be registered.";
      logger.error(errors);
      return false;
    } else {
      return true;
    }
  }

}
