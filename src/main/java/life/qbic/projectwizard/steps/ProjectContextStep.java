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
package life.qbic.projectwizard.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.vaadin.teemu.wizards.WizardStep;
import life.qbic.datamodel.experiments.ExperimentBean;
import life.qbic.datamodel.persons.PersonType;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.projectwizard.uicomponents.ProjectInformationComponent;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.portal.components.CustomVisibilityComponent;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Wizard Step to set the Context of the new experiment and sample creation
 * 
 * @author Andreas Friedrich
 * 
 */
public class ProjectContextStep implements WizardStep {

  private VerticalLayout main;

  private ProjectInformationComponent projectInfoComponent;

  List<ExperimentBean> experiments;

  private Table experimentTable;

  private Table samples;

  List<String> contextOptions = new ArrayList<String>(
      Arrays.asList("Add new experiment", "Add sample extraction to existing sample sources",
          "Measure existing extracted samples again", "Create empty sub-project",
          "Download existing sample spreadsheet", "Add similar samples"));
  private CustomVisibilityComponent projectContext;

  private GridLayout grid;

  private CustomVisibilityComponent pilot;
  private CheckBox pilotBox;

  /**
   * Create a new Context Step for the wizard
   * 
   * @param newProjectCode
   */
  public ProjectContextStep(ProjectInformationComponent projSelect) {
    main = new VerticalLayout();
    main.setMargin(true);
    main.setSpacing(true);
    main.setSizeUndefined();

    projectInfoComponent = projSelect;
    projectInfoComponent.setImmediate(true);
    projectInfoComponent.setVisible(true);

    projectContext = new CustomVisibilityComponent(new OptionGroup("", contextOptions));
    projectContext.setVisible(false);

    disableContextOptions();

    experimentTable = new Table("Applicable Experiments");
    experimentTable.setColumnHeader("experiment_type", "Experimental Step");
    experimentTable.setColumnHeader("samples", "Samples");
    experimentTable.setColumnHeader("date", "Date");
    experimentTable.setColumnHeader("code", "Code");
    experimentTable.setStyleName(ValoTheme.TABLE_SMALL);
    experimentTable.setPageLength(1);
    experimentTable
        .setContainerDataSource(new BeanItemContainer<ExperimentBean>(ExperimentBean.class));
    experimentTable.setSelectable(true);
    experimentTable.setVisible(false);

    samples = new Table("Sample Overview");
    samples.setStyleName(ValoTheme.TABLE_SMALL);
    samples.setColumnHeader("code", "Code");
    samples.setColumnHeader("secondaryName", "Secondary Name");
    samples.setVisible(false);
    samples.setPageLength(1);
    samples.setContainerDataSource(new BeanItemContainer<ISampleBean>(ISampleBean.class));

    grid = new GridLayout(2, 4);
    grid.setSpacing(true);
    grid.setMargin(true);
    grid.addComponent(projectInfoComponent, 0, 0);
    Component context = Styles.questionize(projectContext, "If this experiment's organisms or "
        + "tissue extracts are already registered at QBiC from an earlier experiment, you can chose the second "
        + "option (new tissue extracts from old organism) or the third (new measurements from old tissue extracts). "
        + "You can also create a preliminary sub-project and add samples later or "
        + "download existing sample information by choosing the last option.", "Project Context");
    grid.addComponent(context, 0, 1);
    grid.addComponent(experimentTable, 0, 2);
    grid.addComponent(samples, 1, 1, 1, 2);

    pilotBox = new CheckBox("Pilot Project");

    pilot = new CustomVisibilityComponent(pilotBox);
    pilot.setVisible(false);
    grid.addComponent(Styles.questionize(pilot,
        "Select if the samples you want to add belong to a pilot project. "
            + "You can derive non-pilot samples from existing pilot experiments, but not the other way around.",
        "Pilot Experiment"), 0, 3);

    main.addComponent(grid);

    initListeners();
  }

  public boolean isPilot() {
    return pilotBox.getValue();
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
    resetExperiments();
  }

  private boolean descriptionReady() {
    return getDescription() != null && !getDescription().isEmpty();
  }

  private boolean projectReady() {
    return this.projectInfoComponent.projectIsReady();
  }

  public String getProjectCode() {
    return this.projectInfoComponent.getSelectedProject().toUpperCase();
  }

  public boolean hasImagingSupport() {
    return this.projectInfoComponent.hasImagingSupport();
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

  private void initListeners() {
    projectInfoComponent.getCodeButton().addClickListener(new Button.ClickListener() {

      /**
       * 
       */
      private static final long serialVersionUID = 7932750235689517217L;

      @Override
      public void buttonClick(ClickEvent event) {
        makeContextVisible();
      }
    });
  }

  public List<ExperimentBean> getExperiments() {
    return experiments;
  }

  public void disableContextOptions() {
    for (int i = 0; i < contextOptions.size(); i++)
      projectContext.setItemEnabled(contextOptions.get(i), false);
  }

  public void resetContext() {
    projectContext.select(projectContext.getNullSelectionItemId());
  }

  public void resetExperiments() {
    hideExperiments();
    resetContext();
  }

  public void resetSamples() {
    samples.removeAllItems();
    samples.setVisible(false);
  }

  public void setExperiments(List<ExperimentBean> beans) {
    experiments = beans;
  }

  public void hideExperiments() {
    experimentTable.setVisible(false);
    experimentTable.removeAllItems();
    resetSamples();
  }

  public void showExperiments(List<ExperimentBean> beans) {
    BeanItemContainer<ExperimentBean> c =
        new BeanItemContainer<ExperimentBean>(ExperimentBean.class);
    c.addAll(beans);
    experimentTable.setContainerDataSource(c,
        Arrays.asList("experiment_type", "samples", "date", "code"));
    if (c.size() == 1)
      experimentTable.select(c.getIdByIndex(0));
    experimentTable.setPageLength(Math.min(10, c.size()));
    experimentTable.setVisible(true);
  }

  public void setSamples(List<ISampleBean> beans) {
    BeanItemContainer<ISampleBean> c = new BeanItemContainer<ISampleBean>(ISampleBean.class);
    c.addAll(beans);
    samples.setPageLength(Math.min(beans.size(), 10));
    samples.setContainerDataSource(c, Arrays.asList("code", "secondaryName"));
    samples.setVisible(true);
  }

  public void enableNewContextOption(boolean enable) {
    projectContext.setItemEnabled(contextOptions.get(0), enable);
  }

  public void enableExtractContextOption(boolean enable) {
    projectContext.setItemEnabled(contextOptions.get(1), enable);
  }

  public void enableMeasureContextOption(boolean enable) {
    projectContext.setItemEnabled(contextOptions.get(2), enable);
  }

  public void enableEmptyProjectContextOption(boolean enable) {
    projectContext.setItemEnabled(contextOptions.get(3), enable);
  }

  public void enableTSVWriteContextOption(boolean enable) {
    projectContext.setItemEnabled(contextOptions.get(4), enable);
  }

  public void enableCopyContextOption(boolean enable) {
    projectContext.setItemEnabled(contextOptions.get(5), enable);
  }

  public List<String> getContextOptions() {
    return contextOptions;
  }

  public OptionGroup getProjectContext() {
    return (OptionGroup) projectContext.getInnerComponent();
  }

  @Override
  public String getCaption() {
    return "Project";
  }

  @Override
  public Component getContent() {
    return main;
  }

  @Override
  public boolean onAdvance() {
    if (spaceReady() && projectReady()) {
      if (inherit() || readOnly() || copy())
        if (expSelected())
          return true;
        else
          Styles.notification("No experiment selected", "Please select an existing experiment.",
              NotificationType.ERROR);
      else {
        if (getProjectBox().isEmpty())
          if (descriptionReady())
            return true;
          else
            Styles.notification("No description", "Please fill in an experiment description.",
                NotificationType.ERROR);
        else
          return true;
      }
    } else
      Styles.notification("No sub-project selected",
          "Please select a project and subproject or create a new one.", NotificationType.ERROR);
    return false;
  }

  private boolean expSelected() {
    return (getSamples().size() > 0);
  }

  private boolean inherit() {
    String context = (String) projectContext.getValue();
    return (contextOptions.get(1).equals(context) || contextOptions.get(2).equals(context));
  }

  private boolean copy() {
    String context = (String) projectContext.getValue();
    return contextOptions.get(5).equals(context);
  }

  private boolean readOnly() {
    return false;
    // String context = (String) projectContext.getValue();
    // return contextOptions.get(4).equals(context);
  }

  private boolean spaceReady() {
    return projectInfoComponent.spaceIsReady();
  }

  @Override
  public boolean onBack() {
    return true;
  }

  public String getSpaceCode() {
    return projectInfoComponent.getSpaceCode();
  }

  public ComboBox getSpaceBox() {
    return projectInfoComponent.getSpaceBox();
  }

  public Table getExperimentTable() {
    return experimentTable;
  }

  public ExperimentBean getExperiment() {
    return (ExperimentBean) experimentTable.getValue();
  }

  public List<ISampleBean> getSamples() {
    List<ISampleBean> res = new ArrayList<ISampleBean>();
    samples.setSelectable(true);
    samples.setMultiSelect(true);
    samples.setValue(samples.getItemIds());
    res.addAll((Collection<? extends ISampleBean>) samples.getValue());
    samples.setMultiSelect(false);
    samples.setSelectable(false);
    return res;
  }

  public boolean copyModeSet() {
    String context = (String) projectContext.getValue();
    return contextOptions.get(3).equals(context);
  }

  public boolean fetchTSVModeSet() {
    String context = (String) projectContext.getValue();
    return contextOptions.get(4).equals(context);
  }

  public boolean emptyProjectModeSet() {
    String context = (String) projectContext.getValue();
    return contextOptions.get(3).equals(context);
  }

  public boolean expSecondaryNameSet() {
    TextField expName = projectInfoComponent.getProjectNameField();
    return expName != null && !expName.isEmpty();
  }

  public String getExpSecondaryName() {
    return projectInfoComponent.getProjectNameField().getValue();
  }

  public void makePilotBoxVisible(boolean b) {
    pilotBox.setValue(false);
    pilot.setVisible(b);
  }

  public void makeContextVisible() {
    projectContext.setVisible(true);
  }

  public void selectPilot() {
    makePilotBoxVisible(true);
    pilotBox.setValue(true);
  }

  public void setSpaces(List<String> spaces) {
    projectInfoComponent.setSpaces(spaces);
  }

}
