package life.qbic.portal.samplegraph;

import java.util.List;

import com.vaadin.shared.communication.ServerRpc;

public interface NodeClickRpc extends ServerRpc {
//  public void onNodeClick(List<String> entries);
  public void onCircleClick(String label, List<String> sampleCodes);
}