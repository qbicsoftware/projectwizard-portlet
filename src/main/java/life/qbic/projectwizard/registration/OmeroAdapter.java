package life.qbic.projectwizard.registration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.omero.BasicOMEROClient;

public class OmeroAdapter {

  private Logger logger = LogManager.getLogger(OmeroAdapter.class);
  private BasicOMEROClient omero;

  public OmeroAdapter(BasicOMEROClient omero) {
    this.omero = omero;
  }

  public void registerProject(String project, String desc) {
    if (omero == null) {
      logger
          .error("Omero project could not be created, Omero client was not correctly initialized.");
    } else {
      omero.connect();
      omero.createProject(project, desc);
      omero.disconnect();
    }
  }

  public void registerSamples(String project, String desc, List<ISampleBean> samples) {
    if (omero == null) {
      logger
          .error("Omero samples could not be created, Omero client was not correctly initialized.");
    } else {
      omero.connect();
      HashMap<Long, String> projectMap = omero.loadProjects();
      omero.disconnect();
      Set<Map.Entry<Long, String>> set = projectMap.entrySet();
      Iterator<Map.Entry<Long, String>> iterator = set.iterator();
      long omeroProjectId = -1;
      while (iterator.hasNext()) {
        Map.Entry<Long, String> entry = iterator.next();

        if (entry.getValue().equals(project)) {
          omeroProjectId = (Long) entry.getKey();
          break;
        }
      }

      logger.info("omero project id: " + omeroProjectId);

      if (omeroProjectId == -1) {
        omero.connect();
        omeroProjectId = omero.createProject(project, desc);
        omero.disconnect();
      }

      List<ISampleBean> omeroSamples = new ArrayList<>();

      SampleType type = null;
      if (!samples.isEmpty()) {
        type = samples.get(0).getType();

      }
      if (type.equals(SampleType.Q_BIOLOGICAL_SAMPLE)) {
        omeroSamples.addAll(samples);
      }

      omero.connect();
      logger.info("omero samples:");
      for (ISampleBean omeroSample : omeroSamples) {


        logger.info("sample: " + omeroSample.getCode() + " ----%%%%%%%%%");
        logger.info("desc: " + omeroSample.getSecondaryName());

        long dataset_id = omero.createDataset(omeroProjectId, omeroSample.getCode(),
            omeroSample.getSecondaryName());
        logger.info("dataset id: " + dataset_id);
      }
      omero.disconnect();
    }
  }

}
