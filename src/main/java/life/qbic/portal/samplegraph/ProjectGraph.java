package life.qbic.portal.samplegraph;

import java.util.List;

import com.vaadin.annotations.JavaScript;
import com.vaadin.ui.AbstractJavaScriptComponent;

import life.qbic.datamodel.samples.SampleSummary;

@JavaScript({"vaadin://js/d3.v4.min.js", "graph_connector.js", "vaadin://js/dagre.min.js",
    "vaadin://js/d3-scale-chromatic.v1.min.js"})
public class ProjectGraph extends AbstractJavaScriptComponent {

  @Override
  public ProjectGraphState getState() {
    return (ProjectGraphState) super.getState();
  }

  public void setProject(final List<SampleSummary> list) {
    getState().setProject(list);
  }

  public ProjectGraph(final GraphPage layout, String imagePath) {
    getState().setImagePath(imagePath);

    registerRpc(new NodeClickRpc() {
      public void onCircleClick(String label, List<String> sampleCodes) {
        layout.showDatasetsForSamples(label, sampleCodes);
      }
    });
  }

}
