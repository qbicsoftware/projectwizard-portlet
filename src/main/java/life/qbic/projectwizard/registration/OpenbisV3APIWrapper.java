package life.qbic.projectwizard.registration;

import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.operation.IOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.DataSetUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.update.ExperimentUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.SynchronousOperationExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.update.ProjectUpdate;
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
}
