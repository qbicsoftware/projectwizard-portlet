package life.qbic.projectwizard.uicomponents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    String label = getVocabularyLabelForValue(cat, entry);
    Map<String, String> vocab = catToVocabulary.get(cat);
    if (vocab.containsKey(label)) {
      return vocab.get(label);
    }
    // TODO Auto-generated method stub
    return label;
  }

  public String getVocabularyLabelForValue(String cat, Object object) {
//    System.out.println(catToBoxes.keySet());
//    System.out.println(cat);
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
          b.addValueChangeListener(infoCompleteListener);
          b.setRequiredError("Please find the closest option.");
          b.setRequired(true);
        }
        boxes.add(b);
        right.addComponent(b);
      }
      catToBoxes.put(cat, boxes);
    }
  }

  // public void init(ProjectInformationComponent projectInfoComponent,
  // Map<String, List<String>> missingCategoryToValues, Map<String, Map<String>> catToVocabulary,
  // ValueChangeListener infoCompleteListener) {
  // VerticalLayout right = new VerticalLayout();
  // right.setCaption("Sample information (please complete)");
  // this.projectInfoComponent = projectInfoComponent;
  // this.catToVocabulary = catToVocabulary;
  //
  // projectInfoComponent.addInfoCompleteListener(infoCompleteListener);
  // addComponent(projectInfoComponent);
  // addComponent(right);
  //
  // catToBoxes = new HashMap<String, List<ComboBox>>();
  //
  // for (String cat : missingCategoryToValues.keySet()) {
  // List<ComboBox> boxes = new ArrayList<ComboBox>();
  // for (String value : missingCategoryToValues.get(cat)) {
  // Set<String> vocab = new HashSet<String>(catToVocabulary.get(cat));
  // ComboBox b = new ComboBox(value, vocab);
  // b.setNullSelectionAllowed(false);
  // b.setStyleName(Styles.boxTheme);
  // b.setFilteringMode(FilteringMode.CONTAINS);
  // boolean match = false;
  // for (String vVal : vocab) {
  // if (vVal.equalsIgnoreCase(value)) {
  // match = true;
  // b.setValue(vVal);
  // b.setEnabled(false);
  // break;
  // }
  // }
  // if (!match) {
  // b.addValueChangeListener(infoCompleteListener);
  // b.setRequiredError("Please find the closest option.");
  // b.setRequired(true);
  // }
  // boxes.add(b);
  // right.addComponent(b);
  // }
  // catToBoxes.put(cat, boxes);
  // }
  // }

  public String getProjectSecondaryName() {
    return projectInfoComponent.getProjectName();
  }

}
