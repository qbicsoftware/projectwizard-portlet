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

  public MissingInfoComponent() {
    setSpacing(true);
  }

  public String getVocabularyLabelForValue(String cat, Object entry) {
    for (ComboBox b : catToBoxes.get(cat))
      if (b.getCaption().equals(entry))
        return b.getValue().toString();
    return null;
  }

//  public Map<String, String> getFilledTypeMap() {
//    Map<String, String> res = new HashMap<String, String>();
//    for (String cat : catToBoxes.keySet()) {
//      for (ComboBox b : catToBoxes.get(cat))
//        res.put(b.getCaption(), b.getValue().toString());
//    }
//    return res;
//  }

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
      Map<String, List<String>> missingCategoryToValues, Map<String, List<String>> catToVocabulary,
      ValueChangeListener infoCompleteListener) {
    VerticalLayout right = new VerticalLayout();
    right.setCaption("Sample information (please complete)");
    this.projectInfoComponent = projectInfoComponent;
    projectInfoComponent.addInfoCompleteListener(infoCompleteListener);
    addComponent(projectInfoComponent);
    addComponent(right);

    catToBoxes = new HashMap<String, List<ComboBox>>();

    for (String cat : missingCategoryToValues.keySet()) {
      List<ComboBox> boxes = new ArrayList<ComboBox>();
      for (String value : missingCategoryToValues.get(cat)) {
        Set<String> vocab = new HashSet<String>(catToVocabulary.get(cat));
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

  public String getProjectSecondaryName() {
    return projectInfoComponent.getSecondaryName();
  }
}
