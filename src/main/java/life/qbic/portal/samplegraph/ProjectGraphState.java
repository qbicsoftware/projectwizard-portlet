package life.qbic.portal.samplegraph;

import java.util.List;

import com.vaadin.shared.ui.JavaScriptComponentState;

import life.qbic.datamodel.samples.SampleSummary;

public class ProjectGraphState extends JavaScriptComponentState {

  private List<SampleSummary> project;
  private String imagePath;
  
  public String getImagePath() {
    return imagePath;
  }
  
  public void setImagePath(String imagePath) {
    this.imagePath = imagePath;
  }

  public List<SampleSummary> getProject() {
    return project;
  }

  public void setProject(List<SampleSummary> project) {
    this.project = project;
  }
}
