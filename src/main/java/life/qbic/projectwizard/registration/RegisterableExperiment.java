package life.qbic.projectwizard.registration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.samples.ISampleBean;

public class RegisterableExperiment {

  private String code;
  private String type;
  private List<ISampleBean> samples;
  private Map<String, Object> properties;

  public RegisterableExperiment(String code, ExperimentType type, List<ISampleBean> samples,
      Map<String, Object> properties) {
    this.code = code;
    this.type = type.toString();
    this.samples = samples;
    this.properties = properties;
  }

  public String getType() {
    return type;
  }

  public String getCode() {
    return code;
  }

  public List<ISampleBean> getSamples() {
    return samples;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public Map<String, String> getStringProperties() {
    Map<String, String> res = new HashMap<>();
    for (String key : properties.keySet()) {
      res.put(key, properties.get(key).toString());
    }
    return res;
  }

  public void addSample(ISampleBean s) {
    samples.add(s);
  }
}
