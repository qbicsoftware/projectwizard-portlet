/*******************************************************************************
 * QBiC Project Wizard enables users to create hierarchical experiments including different study
 * conditions using factorial design. Copyright (C) "2016" Andreas Friedrich
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package life.qbic.projectwizard.uicomponents;


import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

//
import com.vaadin.ui.CheckBox;

import life.qbic.datamodel.persons.PersonType;
import life.qbic.datamodel.projects.ProjectInfo;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.portal.components.CustomVisibilityComponent;
import life.qbic.portal.components.StandardTextField;

public class ProjectInformationComponent extends VerticalLayout {

  /**
   * 
   */
  private static final long serialVersionUID = 3467663055161160735L;
  private ComboBox spaceBox;
  private CustomVisibilityComponent projectBox;
  private TextField project;
  private Button reloadProjects;
  private TextField projectName;
  private CustomVisibilityComponent personBox;
  private ComboBox piBox;
  private ComboBox contactBox;
  private ComboBox managerBox;
  private Button reloadPeople;

  //
  private CheckBox imgCheck;

  private TextArea projectDescription;

  private ValueChangeListener projectSelectListener;

  public ProjectInformationComponent(List<String> spaces, Set<String> people) {
    setSpacing(true);
    setSizeUndefined();

    Collections.sort(spaces);
    spaceBox = new ComboBox("Project/Space", spaces);
    spaceBox.setStyleName(Styles.boxTheme);
    spaceBox.setNullSelectionAllowed(false);
    spaceBox.setImmediate(true);
    spaceBox.setFilteringMode(FilteringMode.CONTAINS);
    addComponent(Styles.questionize(spaceBox, "Name of the project", "Project Name"));

    imgCheck = new CheckBox("Imaging Support");
    addComponent(imgCheck);

    ComboBox prBox = new ComboBox("Sub-Project");
    prBox.setStyleName(Styles.boxTheme);
    projectBox = new CustomVisibilityComponent(prBox);
    projectBox.setStyleName(Styles.boxTheme);
    projectBox.setImmediate(true);
    addComponent(Styles.questionize(projectBox, "QBiC 5 letter project code", "Project"));

    project = new StandardTextField();
    project.setStyleName(Styles.fieldTheme);
    project.setMaxLength(5);
    project.setWidth("90px");
    project.setEnabled(false);
    project.setValidationVisible(true);

    reloadProjects = new Button();
    Styles.iconButton(reloadProjects, FontAwesome.REFRESH);

    HorizontalLayout proj = new HorizontalLayout();
    proj.setCaption("New Sub-Project");
    proj.addComponent(project);
    proj.addComponent(reloadProjects);
    CustomVisibilityComponent newProj = new CustomVisibilityComponent(proj);

    addComponent(Styles.questionize(newProj,
        "Automatically create an unused QBiC project code or fill in your own. "
            + "The code consists of 5 characters, must start with Q and not contain Y or Z. You can create a random unused code by clicking "
            + FontAwesome.REFRESH.getHtml() + ".",
        "New Sub-Project"));
    projectName = new StandardTextField("Short name");
    projectName.setWidth("200px");
    // expName.setRequired(true);
    projectName.setVisible(false);
    projectName.setInputPrompt("Name of sub project");
    addComponent(projectName);

    HorizontalLayout persBoxH = new HorizontalLayout();
    persBoxH.setCaption("Principal Investigator");
    VerticalLayout persBox = new VerticalLayout();

    piBox = new ComboBox();
    piBox.addItems(people);
    piBox.setFilteringMode(FilteringMode.CONTAINS);
    piBox.setStyleName(Styles.boxTheme);
    contactBox = new ComboBox("Contact Person", people);
    contactBox.setFilteringMode(FilteringMode.CONTAINS);
    contactBox.setStyleName(Styles.boxTheme);
    managerBox = new ComboBox("Project Manager", people);
    managerBox.setFilteringMode(FilteringMode.CONTAINS);
    managerBox.setStyleName(Styles.boxTheme);
    persBox.addComponent(piBox);
    persBox.addComponent(contactBox);
    persBox.addComponent(managerBox);

    reloadPeople = new Button();
    Styles.iconButton(reloadPeople, FontAwesome.REFRESH);
    persBoxH.addComponent(persBox);
    persBoxH.addComponent(reloadPeople);

    personBox = new CustomVisibilityComponent(persBoxH);
    personBox.setVisible(false);
    addComponent(Styles.questionize(personBox,
        "Investigator and contact person of this project. Please contact us if additional people need to be added. Press refresh button to show newly added people.",
        "Contacts"));

    projectDescription = new TextArea("Description");
    projectDescription.setRequired(true);
    projectDescription.setStyleName(Styles.fieldTheme);
    projectDescription.setInputPrompt("Sub-Project description, maximum of 2000 symbols.");
    projectDescription.setWidth("100%");
    projectDescription.setHeight("110px");
    projectDescription.setVisible(false);
    StringLengthValidator lv = new StringLengthValidator(
        "Description is only allowed to contain a maximum of 2000 letters.", 0, 2000, true);
    projectDescription.addValidator(lv);
    projectDescription.setImmediate(true);
    projectDescription.setValidationVisible(true);
    addComponent(projectDescription);
  }

  public void tryEnableCustomProject(String code) {
    boolean choseNewProject = selectionNull();
    if (choseNewProject) {
      project.setValue(code);
    } else {
      project.setValue("");
    }
    project.setEnabled(choseNewProject);
    projectName.setVisible(choseNewProject);
    projectDescription.setVisible(choseNewProject);
    personBox.setVisible(choseNewProject);
  }

  public void updatePeople(Set<String> people) {
    String pi = getPerson(PersonType.Investigator);
    String contact = getPerson(PersonType.Contact);
    String manager = getPerson(PersonType.Manager);
    piBox.removeAllItems();
    contactBox.removeAllItems();
    managerBox.removeAllItems();
    piBox.addItems(people);
    contactBox.addItems(people);
    managerBox.addItems(people);
    if (pi != null && !pi.isEmpty())
      piBox.select(pi);
    if (contact != null && !contact.isEmpty())
      contactBox.select(contact);
    if (manager != null && !manager.isEmpty())
      managerBox.select(manager);
  }

  private boolean selectionNull() {
    return projectBox.getValue() == null;
  }

  public Button getCodeButton() {
    return reloadProjects;
  }

  public ComboBox getProjectBox() {
    return (ComboBox) projectBox.getInnerComponent();
  }

  public TextField getProjectField() {
    return project;
  }

  public Button getProjectReloadButton() {
    return reloadProjects;
  }

  public Button getPeopleReloadButton() {
    return reloadPeople;
  }

  /**
   * Returns either a selected existing project from the combobox or, if it is empty, the value from
   * the textfield. validity of the textfield should be checked elsewhere
   * 
   * @return project code
   */
  public String getSelectedProject() {
    if (selectionNull())
      return project.getValue().toUpperCase();
    else {
      String project = projectBox.getValue().toString();
      if (project.contains(" "))
        // remove alternative name
        project = project.split(" ")[0];
      return project;
    }
  }

  public boolean hasImagingSupport(){
    return this.imgCheck.getValue();
  }

  public String getProjectDescription() {
    return projectDescription.getValue();
  }

  public String getProjectName() {
    return projectName.getValue();
  }

  public void setDescription(String desc) {
    this.projectDescription.setValue(desc);
  }

  public void setProjectName(String name) {
    this.projectName.setValue(name);
  }

  public void addProjects(List<String> projects) {
    ((AbstractSelect) projectBox.getInnerComponent()).addItems(projects);
  }

  public void resetProjects() {
    projectBox.setEnabled(false);
    ((ComboBox) projectBox.getInnerComponent()).removeAllItems();
    project.setEnabled(true);
  }

  public void enableProjectBox(boolean b) {
    projectBox.setEnabled(b);
  }

  public TextField getProjectNameField() {
    return projectName;
  }

  public String getPerson(PersonType type) {
    switch (type) {
      case Manager:
        return (String) managerBox.getValue();
      case Investigator:
        return (String) piBox.getValue();
      case Contact:
        return (String) contactBox.getValue();
      default:
        return null;
    }
  }

  public boolean spaceIsReady() {
    return spaceBox.getValue() != null && !spaceBox.getValue().toString().isEmpty();
  }

  public String getSpaceCode() {
    return (String) this.spaceBox.getValue();
  }

  public ComboBox getSpaceBox() {
    return spaceBox;
  }

  public void addInfoCompleteListener(ValueChangeListener infoCompleteListener) {
    ComboBox b = (ComboBox) projectBox.getInnerComponent();
    b.addValueChangeListener(infoCompleteListener);
    piBox.addValueChangeListener(infoCompleteListener);
    contactBox.addValueChangeListener(infoCompleteListener);
    managerBox.addValueChangeListener(infoCompleteListener);
    projectName.addValueChangeListener(infoCompleteListener);
    project.addValueChangeListener(infoCompleteListener);
    projectDescription.addValueChangeListener(infoCompleteListener);
  }

  public boolean isValid(boolean notify) {
    if (spaceIsReady() && projectIsReady()) {
      if (getProjectBox().isEmpty()) {
        if (projectDescription.isValid() && !projectDescription.isEmpty())
          return true;
        else {
          if (notify)
            Styles.notification("No description", "Please fill in an experiment description.",
                NotificationType.ERROR);
        }
      } else
        return true;
    } else {
      if (notify)
        Styles.notification("No Sub-project selected", "Please select a project and sub-project.",
            NotificationType.ERROR);
    }
    return false;
  }

  public boolean projectIsReady() {
    return !selectionNull() || project.isValid();
    // return !getSelectedProject().toUpperCase().isEmpty();
  }

  public ProjectInfo getProjectInfo() {
    return new ProjectInfo(getSpaceCode(), getSelectedProject(), projectDescription.getValue(),
        getProjectName(), false, getPerson(PersonType.Investigator), getPerson(PersonType.Contact),
        getPerson(PersonType.Manager));
  }

  public void setSpaces(List<String> spaces) {
    Collections.sort(spaces);
    spaceBox.removeAllItems();
    spaceBox.addItems(spaces);
  }
}
