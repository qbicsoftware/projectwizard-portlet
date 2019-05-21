package life.qbic.projectwizard.registration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.experiments.OpenbisExperiment;
import life.qbic.datamodel.identifiers.SampleCodeFunctions;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;

public class RegisterableProject {

  private String code;
  private String description;
  private String space;
  private List<RegisterableExperiment> experiments;

  // these experiment types can't be flagged as "pilot experiments" (i.e. since they are
  // project-wide)
  private final List<ExperimentType> notPilotable =
      new ArrayList<>(Arrays.asList(ExperimentType.Q_PROJECT_DETAILS));

  public RegisterableProject(List<List<ISampleBean>> tsvSampleHierarchy, String description,
      List<OpenbisExperiment> informativeExperiments, boolean isPilot) {
    this.description = description;
    this.experiments = new ArrayList<RegisterableExperiment>();
    Map<String, OpenbisExperiment> knownExperiments = new HashMap<String, OpenbisExperiment>();
    for (OpenbisExperiment e : informativeExperiments) {
      knownExperiments.put(e.getExperimentCode(), e);
    }
    for (List<ISampleBean> inner : tsvSampleHierarchy) {
      // needed since we collect some samples that don't have the same experiment now - TODO not the
      // best place here
      if (!inner.isEmpty()) {
        ISampleBean sa = inner.get(0);
        this.space = sa.getSpace();
        this.code = sa.getProject();
        Map<String, RegisterableExperiment> expMap = new HashMap<String, RegisterableExperiment>();
        for (ISampleBean s : inner) {
          String expCode = s.getExperiment();
          // we know this experiment, add the current sample
          if (expMap.containsKey(expCode)) {
            RegisterableExperiment exp = expMap.get(expCode);
            exp.addSample(s);
          } else {
            ExperimentType expType = null;
            Map<String, Object> metadata = new HashMap<String, Object>();
            if (knownExperiments.containsKey(expCode)) {
              OpenbisExperiment exp = knownExperiments.get(expCode);
              expType = exp.getType();
              metadata = exp.getMetadata();
            } else {
              // experiment is new, get the metadata, create it and put it into the map
              expType = SampleCodeFunctions.sampleTypesToExpTypes.get(s.getType());
            }

            if (!notPilotable.contains(expType)) {
              metadata.put("Q_IS_PILOT", isPilot);
            }
            RegisterableExperiment e = new RegisterableExperiment(s.getExperiment(), expType,
                new ArrayList<ISampleBean>(Arrays.asList(s)), metadata);
            this.experiments.add(e);
            expMap.put(expCode, e);
          }
        }
      }
    }
  }

  public String getSpace() {
    return space;
  }

  public String getProjectCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  public List<RegisterableExperiment> getExperiments() {
    return experiments;
  }

}
