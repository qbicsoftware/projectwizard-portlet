package life.qbic.projectwizard.uicomponents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import life.qbic.datamodel.persons.PersonType;
import life.qbic.portal.Styles;

public class MissingInfoComponent extends HorizontalLayout {

  private Map<String, List<ComboBox>> catToBoxes;
  private ProjectInformationComponent projectInfoComponent;
  private Map<String, Map<String, String>> catToVocabulary;
  // store mappings to replace in uploaded tsv
  private Map<String, List<String>> currentMappingToCaptions;

  private final Logger logger = LogManager.getLogger(MissingInfoComponent.class);

  public MissingInfoComponent() {
    setSpacing(true);
  }

  public void hideValidBoxes(boolean hide) {
    for (List<ComboBox> list : catToBoxes.values()) {
      for (ComboBox b : list) {
        if (!b.isEnabled()) {
          b.setVisible(!hide);
        }
      }
    }
  }

  public String getVocabularyCodeForValue(Set<String> columnNames, String entry) {
    System.out.println(catToVocabulary.keySet());
//    logger.debug("searching code for value");
//    logger.debug(columnNames);
//    logger.debug(entry);
    Map<String, String> labelToCodeVocabulary = null;
    String label = null;
    for (String colName : columnNames) {
      System.out.println(colName);
      labelToCodeVocabulary = catToVocabulary.get(colName);
//      logger.debug(labelToCodeVocabulary);

      String newLabel = getVocabularyLabelForImportValue(colName, entry);
      if(newLabel==null) {
//        logger.debug(catToBoxes.keySet());
//        logger.debug("catboxes contain "+colName+" for value "+entry+"? "+catToBoxes.containsKey(colName));
      }
      if (newLabel != null) {
        label = newLabel;
      }
    }
//    logger.debug(label);

    if (label == null) {
      Set<String> codes = new HashSet<>(currentMappingToCaptions.keySet());
      for (String code : codes) {
        logger.warn(code);
        logger.warn("checking labeltocodevocab: "+labelToCodeVocabulary);
        if (labelToCodeVocabulary.containsKey(code)) {
          logger.warn("does contain code");
          //TODO removal of changes?
//          logger.debug("REMOVING");
//          logger.debug(code);
          List<String> oldEntries = currentMappingToCaptions.remove(code);//TODO
//          logger.debug("REMOVED");
          String firstHit = oldEntries.get(0);
          for (String colName : columnNames) {
            String newLabel = getVocabularyLabelForImportValue(colName, firstHit);
            if (newLabel != null) {
              label = newLabel;
            }
          }
          if (oldEntries.size() > 1) {
            logger.warn("more than one entry found: " + oldEntries);
            // one entry needed for next category
            currentMappingToCaptions.put(code, new ArrayList<>(Arrays.asList(firstHit)));
          }
        }
      }
    }

    if (labelToCodeVocabulary.containsKey(label)) {
      return labelToCodeVocabulary.get(label);
    }
    return label;
  }

  public String getVocabularyLabelForImportValue(String cat, Object object) {
    if (!catToBoxes.containsKey(cat))
      return null;
    for (ComboBox b : catToBoxes.get(cat))
      if (b.getCaption().equals(object))
        return b.getValue().toString();
    return null;
  }

  public ComboBox getSpaceBox() {
    return projectInfoComponent.getSpaceBox();
  }

  public boolean isValid() {
    boolean boxesValid = true;
    for (List<ComboBox> list : catToBoxes.values()) {
      for (ComboBox b : list) {
        boxesValid &= (b.getValue() != null);
      }
    }
    boolean valid = boxesValid && projectInfoComponent.isValid(false);
    if (valid) {
      lockAllBoxes();
    }
    return valid;
  }

  private void lockAllBoxes() {
    for (List<ComboBox> boxes : catToBoxes.values()) {
      for (ComboBox b : boxes) {
        b.setEnabled(false);
      }
    }
  }

  public String getSpaceCode() {
    return projectInfoComponent.getSpaceCode();
  }

  public String getPerson(PersonType type) {
    String res = projectInfoComponent.getPerson(type);
    if (res == null)
      res = "";
    return res;
  }

  public void setProjectCodes(List<String> projects) {
    projectInfoComponent.addProjects(projects);
    projectInfoComponent.enableProjectBox(true);
    projectInfoComponent.setVisible(true);
  }

  public void resetProjects() {
    projectInfoComponent.resetProjects();
  }

  public String getProjectCode() {
    return this.projectInfoComponent.getSelectedProject().toUpperCase();
  }

  public ComboBox getProjectBox() {
    return projectInfoComponent.getProjectBox();
  }

  public TextField getProjectCodeField() {
    return projectInfoComponent.getProjectField();
  }

  public String getDescription() {
    return projectInfoComponent.getProjectDescription();
  }

  public void tryEnableCustomProject(String code) {
    projectInfoComponent.tryEnableCustomProject(code);
  }

  public void init(ProjectInformationComponent projectInfoComponent,
      Map<String, List<String>> missingCategoryToValues,
      Map<String, Map<String, String>> catToVocabulary, ValueChangeListener infoCompleteListener) {
    VerticalLayout right = new VerticalLayout();

    this.projectInfoComponent = projectInfoComponent;
    this.catToVocabulary = catToVocabulary;
    this.currentMappingToCaptions = new HashMap<>();

    projectInfoComponent.addInfoCompleteListener(infoCompleteListener);
    addComponent(projectInfoComponent);
    addComponent(right);
    right.setCaption("Experiment information (please complete)");

    catToBoxes = new HashMap<String, List<ComboBox>>();
    this.catToVocabulary = catToVocabulary;
    for (String cat : missingCategoryToValues.keySet()) {

      List<ComboBox> boxes = new ArrayList<ComboBox>();
      for (String value : missingCategoryToValues.get(cat)) {

        Set<String> vocab = new HashSet<String>(catToVocabulary.get(cat).keySet());
        ComboBox b = new ComboBox(value, vocab);
        b.setNullSelectionAllowed(false);
        b.setStyleName(Styles.boxTheme);
        b.setFilteringMode(FilteringMode.CONTAINS);
        boolean match = false;
        for (String vVal : vocab) {
          if (vVal.equalsIgnoreCase(value)) {
            match = true;
            b.setValue(vVal);
            b.setEnabled(false);
            break;
          }
        }
        if (!match) {
          ValueChangeListener changeHistoryListener = new ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
              if (b.getValue() != null) {
                String newValue = b.getValue().toString();
                logger.warn("val change: "+newValue);
                if (currentMappingToCaptions.containsKey(newValue)) {
                  logger.warn("key exists");
                  currentMappingToCaptions.get(newValue).add(b.getCaption());
                } else {
                  logger.warn("new key");
                  currentMappingToCaptions.put(newValue,
                      new ArrayList<>(Collections.singletonList(b.getCaption())));
                }
                logger.warn(currentMappingToCaptions);
              }
            }
          };

          b.addValueChangeListener(infoCompleteListener);
          b.addValueChangeListener(changeHistoryListener);
          b.setRequiredError("Please find the closest option.");
          b.setRequired(true);
        }
        boxes.add(b);
        right.addComponent(b);
      }
      catToBoxes.put(cat, boxes);
    }
  }

  public String getProjectSecondaryName() {
    return projectInfoComponent.getProjectName();
  }

  public boolean hasImagingSupport() {
    if (projectInfoComponent != null) {
      return projectInfoComponent.hasImagingSupport();
    }
    return false;
  }

  public Map<String, String> getMetadataReplacements() {
    logger.warn("mapping to captions: "+currentMappingToCaptions);
    Map<String, String> replacements = new HashMap<>();
    for (String vocabValue : currentMappingToCaptions.keySet()) {
      for (String userInput : currentMappingToCaptions.get(vocabValue)) {
        replacements.put(userInput, vocabValue);
      }
    }
    return replacements;
  }

}
