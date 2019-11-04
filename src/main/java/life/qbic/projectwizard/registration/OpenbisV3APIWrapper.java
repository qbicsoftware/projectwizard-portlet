package life.qbic.projectwizard.registration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.operation.IOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.DataSetUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.IExperimentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.update.ExperimentUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.SynchronousOperationExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.search.ProjectSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.update.ProjectUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.search.SpaceSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyTermFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.IVocabularyTermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.VocabularyTermPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.search.VocabularyTermSearchCriteria;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;

public class OpenbisV3APIWrapper {

  private static final Logger logger = LogManager.getLogger(OpenbisV3APIWrapper.class);

  private IApplicationServerApi adminAPI;
  private IApplicationServerApi API;
  private String adminToken;
  private String userToken;
  private String errors;
  private final int TIMEOUT = 10000;
  private String adminUser;
  private String user;
  private String pw;

  public OpenbisV3APIWrapper(String url, String adminUser, String pw, String user) {
    final String URL = url + "/openbis/openbis" + IApplicationServerApi.SERVICE_URL;

    API = HttpInvokerUtils.createServiceStub(IApplicationServerApi.class, URL, TIMEOUT);
    adminAPI = HttpInvokerUtils.createServiceStub(IApplicationServerApi.class, URL, TIMEOUT);

    this.user = user;
    this.adminUser = adminUser;
    this.pw = pw;
  }


  public SearchResult<Project> getProject(String code) {
    checklogin();
    ProjectSearchCriteria sc = new ProjectSearchCriteria();
    sc.withCode().thatEquals(code);
    return API.searchProjects(userToken, sc, new ProjectFetchOptions());
  }

  public SearchResult<Space> getSpacesForUser() {
    //TODO "make sure user is set correctly"
    logger.warn("make sure user is logged in correctly");
    checklogin();
    return API.searchSpaces(userToken, new SpaceSearchCriteria(), new SpaceFetchOptions());
  }

  public void updateProjects(List<ProjectUpdate> p) {
    checklogin();
    API.updateProjects(userToken, p);
  }

  public void updateExperiments(List<ExperimentUpdate> exps) {
    checklogin();
    API.updateExperiments(userToken, exps);
  }

  public void updateDataSets(List<DataSetUpdate> dSets) {
    checklogin();
    API.updateDataSets(userToken, dSets);
  }

  private void checklogin() {
    if (userToken == null) {
      logger.info("Not logged in to the openBIS V3 API. Logging in as user " + user + ".");
      userToken = API.loginAs(adminUser, pw, user);
      System.out.println(userToken);
    }
    if (adminToken == null) {
      logger.info("Logging in as config user.");
      adminToken = adminAPI.login(adminUser, pw);
      System.out.println(adminToken);
    }
  }

  public void logout() {
    logger.info("Logging out of the openBIS V3 API.");
    API.logout(userToken);
    adminAPI.logout(adminToken);
    adminToken = null;
    userToken = null;
  }

  public boolean handleOperations(IOperation operation) {
    checklogin();
    SynchronousOperationExecutionOptions options = new SynchronousOperationExecutionOptions();
    List<IOperation> ops = Arrays.asList(operation);
    try {
      API.executeOperations(userToken, ops, options);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      if (e.getCause() != null) {
        errors = e.getCause().getMessage();
        if (errors.startsWith("Access denied to object with ProjectIdentifier")) {
          logger.warn("User " + user
              + " could not create project, most likely because they are no power user in openBIS.");
          logger.info("Trying to create project with config user instead.");
          try {
            adminAPI.executeOperations(adminToken, ops, options);
            logger.info("Successful.");
            return true;
          } catch (Exception f) {
            errors = f.getCause().getMessage();
            logger.error(errors);
          }
        } else {
          logger.error(errors);
        }
      } else {
        logger.error(e);
      }
      return false;
    }
  }

  public SearchResult<DataSet> searchDatasets(DataSetSearchCriteria criteria,
      DataSetFetchOptions fetchOptions) {
    checklogin();
    return API.searchDataSets(userToken, criteria, fetchOptions);
  }

  public SearchResult<Space> getSpace(String name) {
    checklogin();
    SpaceSearchCriteria sc = new SpaceSearchCriteria();
    sc.withCode().thatEquals(name);
    return API.searchSpaces(userToken, sc, new SpaceFetchOptions());
  }

  public SearchResult<Project> getProjectsOfSpace(String space) {
    checklogin();
    ProjectSearchCriteria sc = new ProjectSearchCriteria();
    sc.withSpace().withCode().thatEquals(space);
    return API.searchProjects(userToken, sc, new ProjectFetchOptions());
  }

  public Experiment getExperimentByID(String expID) {
    checklogin();
    ExperimentIdentifier id = new ExperimentIdentifier(expID);

    ExperimentFetchOptions options = new ExperimentFetchOptions();
    options.withType();
    options.withProperties();

    Map<IExperimentId, Experiment> map = API.getExperiments(userToken, Arrays.asList(id), options);
    return map.get(id);
  }

  public SearchResult<Sample> searchSampleWithCode(String code) {
    checklogin();
    SampleSearchCriteria sc = new SampleSearchCriteria();
    sc.withCode().thatEquals(code);
    SampleFetchOptions options = new SampleFetchOptions();
    options.withExperiment();
    return API.searchSamples(userToken, sc, options);
  }

  public Experiment getExperimentWithSamplesByID(String expID) {
    checklogin();
    ExperimentIdentifier id = new ExperimentIdentifier(expID);

    ExperimentFetchOptions options = new ExperimentFetchOptions();
    options.withType();
    options.withProperties();
    options.withSamples().withProperties();
    options.withSamples().withType();
    options.withRegistrator();

    Map<IExperimentId, Experiment> map = API.getExperiments(userToken, Arrays.asList(id), options);
    return map.get(id);
  }

  public List<Experiment> getExperimentsWithSamplesOfProject(String projectCode) {
    checklogin();
    ExperimentSearchCriteria sc = new ExperimentSearchCriteria();
    sc.withProject().withCode().thatEquals(projectCode);

    ExperimentFetchOptions options = new ExperimentFetchOptions();
    options.withType();
    options.withProperties();
    options.withSamples().withProperties();
    options.withSamples().withType();
    options.withRegistrator();

    SearchResult<Experiment> res = API.searchExperiments(userToken, sc, options);

    return res.getObjects();
  }

  public String translateVocabCode(String code, String vocabulary) {
    checklogin();
    IVocabularyTermId x = new VocabularyTermPermId(code, vocabulary);
    VocabularyTermFetchOptions options = new VocabularyTermFetchOptions();

    Map<IVocabularyTermId, VocabularyTerm> res =
        API.getVocabularyTerms(userToken, Arrays.asList(x), options);
    return res.get(x).getLabel();
  }

  public Map<String, String> getVocabLabelToCode(String vocabulary) {
    checklogin();

    VocabularyTermSearchCriteria vc = new VocabularyTermSearchCriteria();
    vc.withVocabulary().withCode().thatEquals(vocabulary);

    VocabularyTermFetchOptions options = new VocabularyTermFetchOptions();
    SearchResult<VocabularyTerm> searchResult = API.searchVocabularyTerms(userToken, vc, options);

    Map<String, String> res = new HashMap<String, String>();
    for (VocabularyTerm t : searchResult.getObjects()) {
      if (t.getLabel() != null && !t.getLabel().isEmpty()) {
        res.put(t.getLabel(), t.getCode());
      } else {
        res.put(t.getCode(), t.getCode());
      }
    }

    return res;
  }

  public List<Sample> getSamplesOfProjectOfTypes(String projectCode, List<String> typeCodes) {
    checklogin();
    SampleSearchCriteria sc = new SampleSearchCriteria();
    sc.withProject().withCode().thatEquals(projectCode);
    for (String type : typeCodes) {
      sc.withType().withCode().thatEquals(type);
    }

    SampleFetchOptions options = new SampleFetchOptions();
    options.withType();
    options.withProperties();
    options.withRegistrator();

    SearchResult<Sample> res = API.searchSamples(userToken, sc, options);

    return res.getObjects();
  }

  public List<Experiment> getExperimentsOfProject(String projectCode) {
    checklogin();
    ExperimentSearchCriteria sc = new ExperimentSearchCriteria();
    sc.withProject().withCode().thatEquals(projectCode);

    ExperimentFetchOptions options = new ExperimentFetchOptions();
    options.withType();
    options.withProperties();
    options.withRegistrator();

    SearchResult<Experiment> res = API.searchExperiments(userToken, sc, options);

    return res.getObjects();
  }
}
