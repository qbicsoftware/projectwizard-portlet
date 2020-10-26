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
  // store mappings in case users make more than one change. for each mapping
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

  public String getVocabularyCodeForValue(String cat, String entry) {
    // {Precellys=[Proteomic]
    // xxxPRECELLYS
    // yyySample Preparation
    System.out.println(currentMappingToCaptions);
    System.out.println("xxx" + entry);
    System.out.println("yyy" + cat);
    Map<String, String> labelToCodeVocabulary = catToVocabulary.get(cat);

    String label = getVocabularyLabelForImportValue(cat, entry);

    if (label == null) {
      Set<String> codes = new HashSet<>(currentMappingToCaptions.keySet());
      for (String code : codes) {
        if (labelToCodeVocabulary.containsKey(code)) {
          List<String> oldEntries = currentMappingToCaptions.remove(code);
          String firstHit = oldEntries.get(0);
          label = getVocabularyLabelForImportValue(cat, firstHit);
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
    for (List<ComboBox> list : catToBoxes.values())
      for (ComboBox b : list)
        boxesValid &= (b.getValue() != null);
    return boxesValid && projectInfoComponent.isValid(false);
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
                if (currentMappingToCaptions.containsKey(newValue)) {
                  currentMappingToCaptions.get(newValue).add(b.getCaption());
                } else {
                  currentMappingToCaptions.put(newValue,
                      new ArrayList<>(Collections.singletonList(b.getCaption())));
                }
                b.setEnabled(false);
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
    Map<String, String> replacements = new HashMap<>();
    for (String vocabValue : currentMappingToCaptions.keySet()) {
      for (String userInput : currentMappingToCaptions.get(vocabValue)) {
        replacements.put(userInput, vocabValue);
      }
    }
    return replacements;
  }

}
