package life.qbic.portlet.registration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.experiments.OpenbisExperiment;
import life.qbic.datamodel.samples.ISampleBean;

public class RegisterableProject {

  private String code;
  private String description;
  private String space;
  private List<RegisterableExperiment> experiments;
  private Map<String, ExperimentType> sampleTypesToExpTypes =
      new HashMap<String, ExperimentType>() {
        {
          put("Q_BIOLOGICAL_ENTITY", ExperimentType.Q_EXPERIMENTAL_DESIGN);
          put("Q_BIOLOGICAL_SAMPLE", ExperimentType.Q_SAMPLE_EXTRACTION);
          put("Q_TEST_SAMPLE", ExperimentType.Q_SAMPLE_PREPARATION);
          put("Q_NGS_SINGLE_SAMPLE_RUN", ExperimentType.Q_NGS_SINGLE_SAMPLE_RUN);
          put("Q_MS_RUN", ExperimentType.Q_MS_MEASUREMENT);
          put("Q_MHC_LIGAND_EXTRACT", ExperimentType.Q_MHC_LIGAND_EXTRACTION);
          put("Q_ATTACHMENT_SAMPLE", ExperimentType.Q_PROJECT_DETAILS);
        }
      };
  // these experiment types can't be flagged as "pilot experiments" (i.e. since they are
  // project-wide)
  private final List<String> notPilotable =
      new ArrayList<String>(Arrays.asList("Q_PROJECT_DETAILS"));

  public RegisterableProject(String code, String description, String space,
      List<List<ISampleBean>> samples) {
    this.code = code;
    this.description = description;
    this.space = space;
  }

  public RegisterableProject(List<List<ISampleBean>> tsvSampleHierarchy, String description,
      List<OpenbisExperiment> informativeExperiments, boolean isPilot) {
    this.description = description;
    this.experiments = new ArrayList<RegisterableExperiment>();
    Map<String, OpenbisExperiment> knownExperiments = new HashMap<String, OpenbisExperiment>();
    for (OpenbisExperiment e : informativeExperiments) {
      knownExperiments.put(e.getOpenbisName(), e);
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
            expMap.get(expCode).addSample(s);
          } else {
            // experiment is new, get the metadata, create it and put it into the map
            ExperimentType expType = sampleTypesToExpTypes.get(s.getType());
            Map<String, Object> metadata = new HashMap<String, Object>();
            if (knownExperiments.containsKey(expCode)) {
              metadata = knownExperiments.get(expCode).getMetadata();
            }
            if (!notPilotable.contains(expType.toString()))
              metadata.put("Q_IS_PILOT", isPilot);
            RegisterableExperiment e = new RegisterableExperiment(s.getExperiment(), expType,
                new ArrayList<ISampleBean>(Arrays.asList(s)), metadata);
            this.experiments.add(e);
            expMap.put(expCode, e);
          }
        }
      }
    }
  }
  //
  // public RegisterableProject(List<List<ISampleBean>> tsvSampleHierarchy, String description,
  // String secondaryName, Map<String, Map<String, Object>> mhcExperimentMetadata,
  // List<OpenbisExperiment> informativeExperiments, boolean isPilot) {
  // this.description = description;
  // this.experiments = new ArrayList<RegisterableExperiment>();
  // Map<String, Map<String, Object>> mhcExperiments = new HashMap<String, Map<String, Object>>();
  // if (mhcExperimentMetadata != null) {
  // for (Map<String, Object> entries : mhcExperimentMetadata.values()) {
  // mhcExperiments.put((String) entries.get("Code"), entries);//must be done outside for mhc and ms
  // }
  // }
  // for (Map<String, Object> entries : mhcExperiments.values()) {
  // entries.remove("Code");
  // }
  // Map<String, OpenbisExperiment> knownExperiments = new HashMap<String, OpenbisExperiment>();
  // for (OpenbisExperiment e : informativeExperiments) {
  // knownExperiments.put(e.getOpenbisName(), e);
  // }
  // for (List<ISampleBean> inner : tsvSampleHierarchy) {
  // // needed since we collect some samples that don't have the same experiment now - TODO not the
  // // best place here
  // ISampleBean sa = inner.get(0);
  // this.space = sa.getSpace();
  // this.code = sa.getProject();
  // Map<String, RegisterableExperiment> expMap = new HashMap<String, RegisterableExperiment>();
  // for (ISampleBean s : inner) {
  // String expCode = s.getExperiment();
  // // we know this experiment, add the current sample
  // if (expMap.containsKey(expCode)) {
  // expMap.get(expCode).addSample(s);
  // } else {
  // // experiment is new, get the metadata, create it and put it into the map
  // String expType = sampleTypesToExpTypes.get(s.getType());
  // Map<String, Object> metadata = new HashMap<String, Object>();
  // // if (secondaryName != null && !secondaryName.isEmpty()
  // // && expType.equals("Q_EXPERIMENTAL_DESIGN"))
  // // metadata.put("Q_SECONDARY_NAME", secondaryName);
  // if (expType.equals("Q_MHC_LIGAND_EXTRACTION")) {
  // metadata = mhcExperiments.get(expCode);
  // }
  // if (knownExperiments.containsKey(expCode)) {
  // metadata = knownExperiments.get(expCode).getMetadata();
  // }
  // if (!notPilotable.contains(expType))
  // metadata.put("Q_IS_PILOT", isPilot);
  // RegisterableExperiment e = new RegisterableExperiment(s.getExperiment(), expType,
  // new ArrayList<ISampleBean>(Arrays.asList(s)), metadata);
  // this.experiments.add(e);
  // expMap.put(expCode, e);
  // }
  // }
  // }

  // }

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

  // public void addExperimentFromProperties(Map<String, Object> experimentProperties, String type)
  // {
  // int maxID = 0;
  // for (RegisterableExperiment e : experiments) {
  // String[] splt = e.getCode().split("E");
  // int id = Integer.parseInt(splt[splt.length - 1]);
  // if (id > maxID)
  // maxID = id;
  // }
  // experiments.add(new RegisterableExperiment(code + "E" + Integer.toString(maxID + 1), type,
  // new ArrayList<ISampleBean>(), experimentProperties));
  // }
}
