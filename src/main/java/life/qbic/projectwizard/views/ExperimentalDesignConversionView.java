package life.qbic.projectwizard.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.vaadin.data.Item;
import com.vaadin.ui.Button;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.themes.ValoTheme;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.identifiers.ExperimentCodeFunctions;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.expdesign.ParserHelpers;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.projectwizard.registration.IOpenbisCreationController;
import life.qbic.projectwizard.registration.UpdateProgressBar;
import life.qbic.xml.manager.StudyXMLParser;
import life.qbic.xml.manager.XMLParser;
import life.qbic.xml.properties.Property;
import life.qbic.xml.properties.PropertyType;
import life.qbic.xml.study.Qexperiment;
import life.qbic.xml.study.Qproperty;
import life.qbic.xml.study.TechnologyType;

public class ExperimentalDesignConversionView extends VerticalLayout {

  private Logger logger = LogManager.getLogger(AdminView.class);
  private IOpenBisClient openbis;
  private Table projectTable;
  private Set<String> projectInfoExpsWithDesignXML;
  private Set<String> projectInfoSampsWithDesignXML;
  private Map<String, Set<String>> spaceToProjects;
  private Button convert = new Button("Convert");
  private ProgressBar bar = new ProgressBar();
  private Label info = new Label();
  private IOpenbisCreationController creator;

  public ExperimentalDesignConversionView(IOpenBisClient openbis,
      IOpenbisCreationController registrator) {
    this.openbis = openbis;
    this.creator = registrator;
    spaceToProjects = new HashMap<>();
    projectInfoExpsWithDesignXML = new HashSet<>();
    projectInfoSampsWithDesignXML = new HashSet<>();

    setSpacing(true);
    setMargin(true);

    projectTable = new Table("Projects");
    projectTable.setWidth("350");
    projectTable.setStyleName(ValoTheme.TABLE_SMALL);
    projectTable.addContainerProperty("Space Name", String.class, "");
    projectTable.addContainerProperty("Converted projects", String.class, "");
    projectTable.setColumnWidth("Space Name", 210);
    projectTable.setColumnWidth("Converted projects", 140);
    projectTable.setSelectable(true);
    projectTable.setMultiSelect(true);

    Button loadProjects = new Button("Load Existing Projects");
    addComponent(loadProjects);
    loadProjects.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        initTable();
      }
    });
  }

  protected void initTable() {
    List<Experiment> infoExps =
        openbis.getExperimentsOfType(ExperimentType.Q_PROJECT_DETAILS.name());
    List<Sample> infoSamps = openbis.getSamplesOfType(SampleType.Q_ATTACHMENT_SAMPLE.name());
    List<Project> projects = openbis.listProjects();

    for (Experiment exp : infoExps) {
      if (exp.getProperties().containsKey("Q_EXPERIMENTAL_SETUP")) {
        projectInfoExpsWithDesignXML.add(exp.getIdentifier().getIdentifier());
      }
    }
    for (Sample s : infoSamps) {
      projectInfoSampsWithDesignXML.add(s.getCode());
    }

    for (Project project : projects) {
      String space = project.getSpace().getCode();
      String code = project.getCode();
      if (spaceToProjects.containsKey(space)) {
        spaceToProjects.get(space).add(code);
      } else {
        spaceToProjects.put(space, new HashSet<>(Arrays.asList(code)));
      }
    }

    for (String space : spaceToProjects.keySet()) {
      Set<String> codes = spaceToProjects.get(space);
      int numProjects = codes.size();
      int convertedProjects = 0;
      for (String p : codes) {
        String id = ExperimentCodeFunctions.getInfoExperimentID(space, p);
        if (projectInfoExpsWithDesignXML.contains(id)
            && projectInfoSampsWithDesignXML.contains(p + "000")) {
          convertedProjects++;
        }
      }
      // int percentConverted = 100 * convertedProjects / numProjects;

      List<Object> row = new ArrayList<>();
      row.add(space);
      row.add(Integer.toString(convertedProjects) + "/" + Integer.toString(numProjects));
      // row.add(percentConverted);
      projectTable.addItem(row.toArray(new Object[row.size()]), space);
    }
    Object[] properties = {"Space Name"};
    boolean[] ordering = {true};
    projectTable.sort(properties, ordering);
    addComponent(projectTable);
    addComponent(convert);
    convert.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        if (projectTable.getValue() != null) {
          conversionPressed();
          List<String> spaces = new ArrayList<>((Set<String>) projectTable.getValue());
          List<Set<String>> existingPerSpace = new ArrayList<>();
          for (String space : spaces) {
            Set<String> existing = new HashSet<>();
            for (String project : spaceToProjects.get(space)) {
              String id = ExperimentCodeFunctions.getInfoExperimentID(space, project);
              if (projectInfoExpsWithDesignXML.contains(id)
                  && projectInfoSampsWithDesignXML.contains(project + "000")) {
                existing.add(project);
              }
            }
            existingPerSpace.add(existing);
          }
          convertInBackground(spaces, existingPerSpace);
        }
      }
    });

    addComponent(info);
    addComponent(bar);
    info.setVisible(false);
    bar.setVisible(false);
  }

  private void convertInBackground(List<String> spaces, List<Set<String>> existing) {
    ExperimentalDesignConversionView caller = this;

    Thread t = new Thread(new Runnable() {
      volatile int current = -1;

      @Override
      public void run() {
        int numProjects = 0;
        for (int i = 0; i < spaces.size(); i++) {
          numProjects += spaceToProjects.get(spaces.get(i)).size() - existing.get(i).size();
        }
        info.setCaption("Converting " + numProjects + " projects in " + spaces.size() + " spaces.");
        UI.getCurrent().access(new UpdateProgressBar(bar, info, 0.01));

        final int todo = numProjects;

        for (int i = 0; i < spaces.size(); i++) {
          for (Project pr : openbis.getProjectsOfSpace(spaces.get(i))) {
            String project = pr.getCode();
            if (!existing.get(i).contains(project)) {
              try {
                current++;
                double frac = current * 1.0 / todo;
                UI.getCurrent().access(new UpdateProgressBar(bar, info, frac));
                convertMissingXMLsForProject(spaces.get(i), project);
              } catch (IllegalArgumentException | JAXBException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
            }
          }
        }

        UI.getCurrent().setPollInterval(-1);
        UI.getCurrent().access(new ConversionReadyRunnable(caller, spaces));
      }
    });
    t.start();
    UI.getCurrent().setPollInterval(100);
  }

  private void conversionPressed() {
    projectTable.setSelectable(false);
    convert.setEnabled(false);
    info.setVisible(true);
    bar.setVisible(true);
  }

  public void conversionFinished(List<String> spaces) {
    updateLocalData(spaces);
    projectTable.setSelectable(true);
    convert.setEnabled(true);
    info.setVisible(false);
    bar.setVisible(false);
  }

  private void updateLocalData(List<String> spaces) {
    for (String space : spaces) {
      for (String project : spaceToProjects.get(space)) {
        String id = ExperimentCodeFunctions.getInfoExperimentID(space, project);
        projectInfoExpsWithDesignXML.add(id);
      }
      Item item = projectTable.getItem(space);
      String numProjects = Integer.toString(spaceToProjects.get(space).size());
      String allConverted = numProjects + "/" + numProjects;
      item.getItemProperty("Converted projects").setValue(allConverted);
    }
  }

  private void convertMissingXMLsForProject(String space, String project)
      throws IllegalArgumentException, JAXBException, InterruptedException {
    Set<String> types = new HashSet<>(Arrays.asList("Q_BIOLOGICAL_SAMPLE", "Q_BIOLOGICAL_ENTITY",
        "Q_TEST_SAMPLE", "Q_MHC_LIGAND_EXTRACT"));
    Map<String, Map<Pair<String, String>, List<String>>> expDesign = new HashMap<>();
    Map<String, List<Qproperty>> otherProps = new HashMap<>();
    String TARGET_EXPERIMENT = ExperimentCodeFunctions.getInfoExperimentID(space, project);
    String INFO_SAMPLE_CODE = project + "000";

    List<Sample> samples = openbis.getSamplesOfProject("/" + space + "/" + project);
    Set<TechnologyType> techs = new HashSet<>();
    int size = samples.size();
    logger.info("converting " + project);
    logger.info(size + " samples");
    logger.info("target experiment: " + TARGET_EXPERIMENT);
    Experiment exp = openbis.getExperimentById(TARGET_EXPERIMENT);

    boolean exists = exp != null;
    boolean sampleExists = openbis.sampleExists(INFO_SAMPLE_CODE);

    logger.info("experiment exists: " + exists);
    logger.info("sample exists: " + sampleExists);

    for (Sample s : samples) {
      String type = s.getType().getCode();
      if (type.equals("Q_TEST_SAMPLE")) {
        String analyte = s.getProperties().get("Q_SAMPLE_TYPE");
        TechnologyType tech = ParserHelpers.typeToTechnology.get(analyte);
        if (tech != null)
          techs.add(tech);
      }
      if (types.contains(type)) {
        String code = s.getCode();
        XMLParser par = new XMLParser();
        List<Property> props = par.getAllPropertiesFromXML(s.getProperties().get("Q_PROPERTIES"));
        for (Property p : props) {
          String lab = p.getLabel();
          String val = p.getValue();
          String unit = null;
          if (p.hasUnit())
            unit = p.getUnit().getValue();
          if (p.getType().equals(PropertyType.Factor)) {

            Pair<String, String> valunit = new ImmutablePair<String, String>(val, unit);
            if (expDesign.containsKey(lab)) {
              Map<Pair<String, String>, List<String>> levels = expDesign.get(lab);
              if (levels.containsKey(valunit)) {
                levels.get(valunit).add(code);
              } else {
                levels.put(valunit, new ArrayList<String>(Arrays.asList(code)));
              }
            } else {
              Map<Pair<String, String>, List<String>> newLevel =
                  new HashMap<Pair<String, String>, List<String>>();
              newLevel.put(valunit, new ArrayList<String>(Arrays.asList(code)));
              expDesign.put(lab, newLevel);
            }

          } else {
            Qproperty newProp = null;
            if (p.hasUnit()) {
              newProp = new Qproperty(code, p.getLabel(), p.getValue(), p.getUnit());
            } else {
              newProp = new Qproperty(code, p.getLabel(), p.getValue());
            }
            if (otherProps.containsKey(code)) {
              otherProps.get(code).add(newProp);
            } else {
              otherProps.put(code, new ArrayList<Qproperty>(Arrays.asList(newProp)));
            }
          }
        }
      }
    }
    StudyXMLParser p = new StudyXMLParser();
    List<TechnologyType> techTypes = new ArrayList<>(techs);
    JAXBElement<Qexperiment> res =
        p.createNewDesign(new HashSet<>(), techTypes, expDesign, otherProps);
    String xml = p.toString(res);
    Map<String, Object> props = new HashMap<>();
    String infoCode = project + "_INFO";
    props.put("Q_EXPERIMENTAL_SETUP", xml);
    if (!exists) {
      logger.info("creating new experiment");
      creator.registerExperiment(space, project, ExperimentType.Q_PROJECT_DETAILS, infoCode, props);
    } else {
      logger.info("updating existing experiment");

      creator.updateExperiment(TARGET_EXPERIMENT, props);
    }
    if (!sampleExists) {
      logger.info("registering info sample");
      ISampleBean infoSample = new TSVSampleBean(INFO_SAMPLE_CODE, infoCode, project, space,
          SampleType.Q_ATTACHMENT_SAMPLE, "", new ArrayList<String>(),
          new HashMap<String, Object>());
      creator.registerSampleBatch(new ArrayList<>(Arrays.asList(infoSample)));
    }
    logger.info(project + " done");
  }

}
