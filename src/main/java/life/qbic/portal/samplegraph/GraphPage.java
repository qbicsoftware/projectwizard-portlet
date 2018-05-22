package life.qbic.portal.samplegraph;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.expdesign.model.StructuredExperiment;
import life.qbic.portal.utils.PortalUtils;

public class GraphPage extends VerticalLayout {

  private static final Logger LOGGER = LogManager.getLogger(GraphPage.class);

  private Map<String, ISampleBean> currentSampleMap;
  private ComboBox factorBox;

  private ProjectGraph sampleGraph;


  public GraphPage(Map<String, String> taxMap, Map<String, String> tissueMap) {
    // Map<String, String> reverseTaxMap = taxMap.entrySet().stream()
    // .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    // Map<String, String> reverseTissueMap = tissueMap.entrySet().stream()
    // .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    //
    // parser = new ProjectParser(reverseTaxMap, reverseTissueMap);
  }

  public GraphPage() {
    setSpacing(true);
  }

  private String buildImagePath() {
    StringBuilder pathBuilder = new StringBuilder();

    if (PortalUtils.isLiferayPortlet()) {
      Properties prop = new Properties();
      InputStream in = this.getClass().getClassLoader()
          .getResourceAsStream("WEB-INF/liferay-plugin-package.properties");
      try {
        prop.load(in);
        in.close();
      } catch (IOException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      String portletName = prop.getProperty("name");

      URI location = UI.getCurrent().getPage().getLocation();
      // http
      pathBuilder.append(location.getScheme());
      pathBuilder.append("://");
      // host+port
      pathBuilder.append(location.getAuthority());

      String port = (Integer.toString(location.getPort()));
      if (location.toString().contains(port)) {
        pathBuilder.append(":");
        pathBuilder.append(port);
      }
      pathBuilder.append("/");
      pathBuilder.append(portletName);
    }
    pathBuilder.append("/VAADIN/img/");
    return pathBuilder.toString();
  }

  public void setProjectGraph(StructuredExperiment graph, Map<String, ISampleBean> idsToSamples) {
    this.currentSampleMap = idsToSamples;
    this.factorBox = new ComboBox("Experimental Factor");
    factorBox.setVisible(false);
    addComponent(factorBox);

    if (!graph.getFactorsToSamples().isEmpty()) {
      factorBox.addItems(graph.getFactorsToSamples().keySet());
      factorBox.setVisible(true);
    }

    factorBox.setImmediate(true);
    factorBox.setNullSelectionAllowed(false);
    final GraphPage parent = this;
    factorBox.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        Object factor = factorBox.getValue();
        if (sampleGraph != null)
          parent.removeComponent(sampleGraph);
        sampleGraph = new ProjectGraph(parent, buildImagePath());
        sampleGraph.setSizeFull();
        parent.addComponent(sampleGraph);
        if (factor != null) {
          sampleGraph.setProject(graph.getFactorsToSamples().get(factor));
        }
      }
    });
  }

  public void showDatasetsForSamples(String label, List<String> sampleCodes) {
    Window subWindow = new Window(" " + label + " information");
    subWindow.setWidth("680px");

    VerticalLayout layout = new VerticalLayout();
    layout.setSpacing(true);
    layout.setMargin(true);
    List<ISampleBean> samples = new ArrayList<ISampleBean>();

    for (String code : sampleCodes) {
      ISampleBean s = currentSampleMap.get(code);
      samples.add(s);
    }

    Table haveData = new Table("Samples/Entities");
    haveData.setStyleName(ValoTheme.TABLE_SMALL);
    haveData.addContainerProperty("Sample", String.class, null);
    haveData.addContainerProperty("Secondary Name", String.class, null);
    haveData.addContainerProperty("Lab ID", String.class, null);
    int i = 0;
    for (ISampleBean s : samples) {
      i++;
      List<Object> row = new ArrayList<Object>();
      row.add(s.getCode());
      Map<String, Object> props = s.getMetadata();
      String secName = "";
      if (props.get("Q_SECONDARY_NAME") != null)
        secName = (String) props.get("Q_SECONDARY_NAME");
      row.add(secName);

      String extID = "";
      if (props.get("Q_EXTERNALDB_ID") != null)
        extID = (String) props.get("Q_EXTERNALDB_ID");
      row.add(extID);

      haveData.addItem(row.toArray(new Object[row.size()]), i);
    }

    haveData.setPageLength(samples.size());

    layout.addComponent(haveData);
    Button ok = new Button("Close");
    ok.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        subWindow.close();
      }
    });

    layout.addComponent(ok);

    subWindow.setContent(layout);
    // Center it in the browser window
    subWindow.center();
    subWindow.setModal(true);
    subWindow.setIcon(FontAwesome.FILE);
    subWindow.setResizable(false);

    getUI().addWindow(subWindow);
  }

}
