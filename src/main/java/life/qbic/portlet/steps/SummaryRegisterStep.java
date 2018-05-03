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
package life.qbic.portlet.steps;

import java.util.Iterator;
import java.util.List;

import logging.Log4j2Logger;
import main.SampleSummaryBean;

import org.vaadin.hene.flexibleoptiongroup.FlexibleOptionGroup;
import org.vaadin.hene.flexibleoptiongroup.FlexibleOptionGroupItemComponent;
import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.VerticalLayout;

import componentwrappers.CustomVisibilityComponent;
import life.qbic.portlet.model.ISampleBean;
import life.qbic.portlet.processes.RegistrationMode;
import life.qbic.portlet.uicomponents.ExperimentSummaryTable;
import life.qbic.portlet.uicomponents.Styles;
import life.qbic.portlet.uicomponents.Styles.*;
import life.qbic.portlet.views.IRegistrationView;

/**
 * Wizard Step to downloadTSV and upload the TSV file to and from and register samples and context
 * 
 * @author Andreas Friedrich
 * 
 */
public class SummaryRegisterStep implements WizardStep, IRegistrationView {

  private VerticalLayout main;
  private Button downloadTSV;
  private Button register;
  private ExperimentSummaryTable summary;
  private List<List<ISampleBean>> samples;
  private Label registerInfo;
  private ProgressBar bar;
  private CustomVisibilityComponent summaryComponent;
  private logging.Logger logger = new Log4j2Logger(SummaryRegisterStep.class);
  private boolean registrationComplete = false;
  private FlexibleOptionGroup optionGroup;
  private VerticalLayout optionLayout;
  private RegistrationMode registrationMode;
  private final String paidOption = "I am aware of all costs associated with this project, "
      + "since I previously signed a corresponding agreement "
      + "and I agree to pay all charges upon receival of the "
      + "invoice. This submission is binding.";
  private final String freeOption = "I understand that this experimental design draft will "
      + "now be submitted for QBiC review. I thereby request "
      + "a consultancy meeting. There are no costs associated " + "with this submission.";

  public SummaryRegisterStep() {
    main = new VerticalLayout();
    main.setMargin(true);
    main.setSpacing(true);
    Label header = new Label("Sample Registration");
    main.addComponent(Styles.questionize(header,
        "Here you can download a spreadsheet of the samples in your experiment "
            + "and register your project in the database. "
            + "Registering samples may take a few seconds.",
        "Sample Registration"));

    summary = new ExperimentSummaryTable();

    summaryComponent = new CustomVisibilityComponent(Styles.questionize(summary,
        "This is a summary of samples for Sample Sources/Patients, Tissue Extracts and "
            + "samples that will be measured.",
        "Experiment Summary"));
    summaryComponent.setVisible(false);
    main.addComponent(summaryComponent.getInnerComponent());

    downloadTSV = new Button("Download Spreadsheet");
    downloadTSV.setEnabled(false);
    HorizontalLayout tsvInfo = new HorizontalLayout();
    tsvInfo.addComponent(downloadTSV);
    main.addComponent(Styles.questionize(tsvInfo,
        "You can download a technical spreadsheet to register your samples at a later time instead. More informative spreadsheets are available in the next step.",
        "TSV Download"));

    Container cont = new IndexedContainer();
    cont.addContainerProperty("caption", String.class, "");
    cont.getContainerProperty(cont.addItem(), "caption").setValue(paidOption);
    cont.getContainerProperty(cont.addItem(), "caption").setValue(freeOption);
    optionGroup = new FlexibleOptionGroup(cont);
    optionGroup.setItemCaptionPropertyId("caption");

    optionLayout = new VerticalLayout();
    Iterator<FlexibleOptionGroupItemComponent> iter;
    iter = optionGroup.getItemComponentIterator();
    while (iter.hasNext()) {
      FlexibleOptionGroupItemComponent fogItemComponent = iter.next();
      Label caption = new Label(fogItemComponent.getCaption());
      caption.setWidth("400px");
      optionLayout.addComponent(new HorizontalLayout(fogItemComponent, caption));
    }

    main.addComponent(optionLayout);
    optionGroup.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        testRegEnabled();
      }
    });

    register = new Button("Register All Samples");
    register.setEnabled(false);
    main.addComponent(register);

    registerInfo = new Label();
    bar = new ProgressBar();
    main.addComponent(registerInfo);
    main.addComponent(bar);
  }

  public void setSummary(List<SampleSummaryBean> summaries) {
    summary.setSamples(summaries);
    summaryComponent.setVisible(true);
    enableDownloads(true);
  }

  public void enableDownloads(boolean enabled) {
    downloadTSV.setEnabled(enabled);
  }

  public Button getDownloadButton() {
    return this.downloadTSV;
  }

  @Override
  public String getCaption() {
    return "Registration";
  }

  @Override
  public Component getContent() {
    return main;
  }

  @Override
  public boolean onAdvance() {
    return registrationComplete();
  }

  private boolean registrationComplete() {
    return registrationComplete;
  }

  @Override
  public boolean onBack() {
    return true;
  }

  public Button getRegisterButton() {
    return this.register;
  }

  public void setProcessed(List<List<ISampleBean>> processed) {
    samples = processed;
  }

  public void testRegEnabled() {
    boolean optionChosen = optionGroup.getValue() != null;
    register
        .setEnabled(optionChosen || registrationMode.equals(RegistrationMode.RegisterEmptyProject));
    if (optionChosen) {
      if (registrationReady()) {
        if (optionGroup.getValue().equals(1))
          register.setCaption("Register All Samples");
        if (optionGroup.getValue().equals(2))
          register.setCaption("Send Project to QBiC");
      }
    }
  }

  public List<List<ISampleBean>> getSamples() {
    return samples;
  }

  public void registrationDone(boolean sqlDown, String error) {
    if (error.isEmpty()) {
      if (sqlDown) {
        logger.warn(
            "Project registered, but didn't add information to SQL database due to missing SQL connection. User notified.");
        Styles.notification("Registration complete.",
            "Registration of samples complete. Unfortunately investigators and contacts could not be added to the database.",
            NotificationType.DEFAULT);
      } else {
        Styles.notification("Registration complete!",
            "Registration of project complete. Press 'next' for additional options.",
            NotificationType.SUCCESS);
      }
      if (registrationMode.equals(RegistrationMode.RegisterSamples))
        optionGroup.setEnabled(false);
      register.setEnabled(false);
      registrationComplete = true;
    } else {
      String feedback = "Sample registration could not be completed. Reason: " + error;
      logger.error(feedback);
      Styles.notification("Registration failed!", feedback, NotificationType.ERROR);
    }
  }

  public ProgressBar getProgressBar() {
    return bar;
  }

  public Label getProgressLabel() {
    return registerInfo;
  }

  private boolean registrationReady() {
    return (summary.size() > 0 || registrationMode.equals(RegistrationMode.RegisterEmptyProject));
  }

  public void resetSummary() {
    summary.removeAllItems();
  }

  public void setRegistrationMode(RegistrationMode mode) {
    this.registrationMode = mode;
    switch (mode) {
      case RegisterEmptyProject:
        optionLayout.setVisible(false);
        register.setCaption("Register Project");
        break;
      case RegisterSamples:
        optionLayout.setVisible(true);
        register.setCaption("Register All Samples");
        break;
      case DownloadTSV:
        optionLayout.setVisible(false);
        break;
      default:
        logger.error("Unknown registration mode: " + mode);
        break;
    }
  }

}
