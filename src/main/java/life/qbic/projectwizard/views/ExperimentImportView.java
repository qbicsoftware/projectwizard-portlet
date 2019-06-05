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
package life.qbic.projectwizard.views;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.isatools.isacreator.model.Study;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FileResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;

import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.expdesign.model.ExperimentalDesignType;
import life.qbic.expdesign.model.SampleSummaryBean;
import life.qbic.expdesign.model.StructuredExperiment;
import life.qbic.projectwizard.control.Functions;
import life.qbic.projectwizard.uicomponents.ExperimentSummaryTable;
import life.qbic.projectwizard.uicomponents.MissingInfoComponent;
import life.qbic.projectwizard.uicomponents.ProjectInformationComponent;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.projectwizard.uicomponents.MultiUploadComponent;
import life.qbic.portal.portlet.ProjectWizardUI;
import life.qbic.portal.samplegraph.GraphPage;

public class ExperimentImportView extends VerticalLayout implements IRegistrationView {

  /**
   * 
   */
  private static final long serialVersionUID = 5358966181721590658L;
  private Button register;
  private OptionGroup importOptions;
  private VerticalLayout infos;
  private VerticalLayout isaBox;
  private Upload upload;
  private MultiUploadComponent multiUpload;
  private MissingInfoComponent questionaire;
  private ExperimentSummaryTable summary;
  private Button preview;
  private List<List<ISampleBean>> samples;
  private Label registerInfo;
  private ProgressBar bar;
  private Button downloadTSV;
  private ComboBox isaStudyBox;

  private static final Logger logger = LogManager.getLogger(ExperimentImportView.class);

  public ExperimentImportView() {
    setMargin(true);
    setSpacing(true);

    this.questionaire = new MissingInfoComponent();


    importOptions = new OptionGroup("Import Format");
    importOptions.addItems("QBiC", "Standard", "ISA-Tab (prototype)", "MHC Ligandomics (measured)");

    importOptions.addValueChangeListener(new ValueChangeListener() {
      @Override
      public void valueChange(ValueChangeEvent event) {
        for (Object cl : preview.getListeners(ClickEvent.class))
          preview.removeClickListener((ClickListener) cl);
        Object value = importOptions.getValue();
        enableMultiUpload("ISA-Tab (prototype)".equals(value));
        preview.setVisible("Standard".equals(value));
      }
    });
    infos = new VerticalLayout();
    infos.setCaption("Format Information");

    infos.addComponent(
        Styles.getPopupViewContaining(createTSVDownloadComponent(ExperimentalDesignType.QBIC)));
    infos.addComponent(
        Styles.getPopupViewContaining(createTSVDownloadComponent(ExperimentalDesignType.Standard)));
    infos.addComponent(
        Styles.getPopupViewContaining(createTSVDownloadComponent(ExperimentalDesignType.ISA)));
    infos.addComponent(Styles.getPopupViewContaining(
        createTSVDownloadComponent(ExperimentalDesignType.MHC_Ligands_Finished)));
  }

  protected void enableMultiUpload(boolean enable) {
    isaBox.removeAllComponents();
    if (enable) {

      Window subWindow = new Window(" Upload ISA-Tab");
      subWindow.setWidth("400px");

      VerticalLayout layout = new VerticalLayout();
      layout.setSpacing(true);
      layout.setMargin(true);
      Label info = new Label("Please upload all files belonging to the ISA study.");
      layout.addComponent(info);
      layout.addComponent(multiUpload.getUpload());

      Button ok = new Button("Ok");

      ok.addClickListener(new ClickListener() {
        @Override
        public void buttonClick(ClickEvent event) {
          subWindow.close();
          layout.removeAllComponents();
        }
      });
      layout.addComponent(ok);
      isaStudyBox.setVisible(true);
      isaBox.addComponent(isaStudyBox);
//      String baseDir = VaadinService.getCurrent().getBaseDirectory().getAbsolutePath();
      // src/main/webapp
      Resource res = new ExternalResource(ProjectWizardUI.getPathToVaadinFolder()+"img/isatools.png");
      Image imNotYourC_Pal = new Image(null, res);
      layout.addComponent(imNotYourC_Pal);

      subWindow.setContent(layout);
      // Center it in the browser window
      subWindow.center();
      subWindow.setModal(true);
      subWindow.setIcon(FontAwesome.TABLE);
      subWindow.setResizable(false);
      ProjectWizardUI ui = (ProjectWizardUI) UI.getCurrent();
      ui.addWindow(subWindow);
    } else {
      isaStudyBox.setVisible(false);
      isaBox.addComponent(upload);
    }
  }

  public void initView(Upload upload, MultiUploadComponent multiUpload) {
    HorizontalLayout optionsInfo = new HorizontalLayout();
    optionsInfo.addComponent(importOptions);
    optionsInfo.addComponent(infos);

    // design type selection and info
    addComponent(optionsInfo);

    // file upload component
    this.upload = upload;
    this.multiUpload = multiUpload;
    isaBox = new VerticalLayout();
    isaBox.setSpacing(true);
    addComponent(isaBox);

    isaStudyBox = new ComboBox("Study");
    isaStudyBox.setImmediate(true);
    isaStudyBox.setNullSelectionAllowed(false);

    preview = new Button("Preview Sample Graph");
    preview.setEnabled(false);
    addComponent(preview);
    
    // missing info input layout
    addComponent(questionaire);

    // summary of imortet samples
    summary = new ExperimentSummaryTable();
    summary.setVisible(false);
    addComponent(summary);

    // sample registration button
    register = new Button("Register All");
    register.setVisible(false);
    addComponent(register);

    // registration progress information
    registerInfo = new Label();
    bar = new ProgressBar();
    registerInfo.setVisible(false);
    bar.setVisible(false);
    addComponent(registerInfo);
    addComponent(bar);
  }

  private Component createTSVDownloadComponent(ExperimentalDesignType type) {
    if (type.equals(ExperimentalDesignType.ISA)) {
      VerticalLayout v = new VerticalLayout();
      Label l = new Label("For the ISA specification see:");
      Link link = new Link("http://isa-specs.readthedocs.io/en/latest/isatab.html",
              new ExternalResource("http://isa-specs.readthedocs.io/en/latest/isatab.html"));
   // Open the URL in a new window/tab
      link.setTargetName("_blank");
      v.addComponent(l);
      v.addComponent(link);
      return v;
    } else {
      VerticalLayout v = new VerticalLayout();
      v.setSpacing(true);
      Label l = new Label(type.getDescription());
      l.setWidth("300px");
      v.addComponent(l);
      Button button = new Button("Download Example");
      v.addComponent(button);

      final File example = new File(
          getClass().getClassLoader().getResource("examples/" + type.getFileName()).getFile());
      FileDownloader tsvDL = new FileDownloader(new FileResource(example));
      tsvDL.extend(button);
      return v;
    }
  }

  public Button getRegisterButton() {
    return this.register;
  }

  public void setSummary(List<SampleSummaryBean> beans) {
    summary.setSamples(beans);
    summary.setVisible(true);
  }

  public void setProcessed(List<List<ISampleBean>> processed) {
    samples = processed;
  }

  public void setRegEnabled(boolean b) {
    register.setEnabled(b);
    register.setVisible(b);
  }

  public List<List<ISampleBean>> getSamples() {
    return samples;
  }


  public void registrationDone(boolean sqlDown, String errors) {
    // TODO when adding mysql metadata handle sql down
    if (errors.isEmpty()) {
      logger.info("Sample registration complete!");
      Styles.notification("Registration complete!", "Registration of samples complete.",
          NotificationType.SUCCESS);
      register.setEnabled(false);
      switch (getSelectedDesignOption()) {
        case Standard:
        case MHC_Ligands_Finished:
          downloadTSV.setEnabled(true);
          break;
        default:
          break;
      }
    } else {
      String feedback = "Sample registration could not be completed. Reason: " + errors;
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

  public boolean summaryIsSet() {
    return (summary.size() > 0);
  }

  public void resetAfterUpload() {
    summary.removeAllItems();
    summary.setVisible(false);
    registerInfo.setVisible(false);
    bar.setVisible(false);
    if (downloadTSV != null)
      removeComponent(downloadTSV);
  }

  public ExperimentalDesignType getSelectedDesignOption() {
    if (importOptions.getValue() != null) {
      switch (importOptions.getValue().toString()) {
        case "QBiC":
          return ExperimentalDesignType.QBIC;
        case "Standard":
          return ExperimentalDesignType.Standard;
        case "MHC Ligandomics (preparation)":
          return ExperimentalDesignType.MHC_Ligands_Plan;
        case "MHC Ligandomics (measured)":
          return ExperimentalDesignType.MHC_Ligands_Finished;
        case "ISA-Tab (prototype)":
          return ExperimentalDesignType.ISA;
        default:
          return ExperimentalDesignType.Standard;
      }
    } else
      return null;
  }

  public MissingInfoComponent initMissingInfoComponent(
      ProjectInformationComponent projectInfoComponent,
      Map<String, List<String>> missingCategoryToValues, Map<String, List<String>> catToVocabulary,
      ValueChangeListener missingInfoFilledListener) {
    MissingInfoComponent newQ = new MissingInfoComponent();
    newQ.init(projectInfoComponent, missingCategoryToValues, catToVocabulary,
        missingInfoFilledListener);
    replaceComponent(questionaire, newQ);
    questionaire = newQ;
    return questionaire;
  }

  public MissingInfoComponent getMissingInfoComponent() {
    return questionaire;
  }

  public void setTSVWithBarcodes(String tsvContent, String name) {
    if (downloadTSV != null)
      removeComponent(downloadTSV);
    downloadTSV = new Button("Download Barcodes");
    addComponent(downloadTSV);
    FileDownloader tsvDL = new FileDownloader(Functions.getFileStream(tsvContent, name, "tsv"));
    tsvDL.extend(downloadTSV);
  }

  public void showRegistrationProgress() {
    bar.setVisible(true);
    registerInfo.setVisible(true);
  }

  public void initGraphPreview(StructuredExperiment structuredExperiment,
      Map<String, ISampleBean> idsToSamples) {
    preview.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        Window subWindow = new Window(" Preview Graph");
        subWindow.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        GraphPage g = new GraphPage();
        layout.addComponent(g);
        g.setProjectGraph(structuredExperiment, idsToSamples);
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
        subWindow.setIcon(FontAwesome.CONNECTDEVELOP);
        subWindow.setResizable(false);
        ProjectWizardUI ui = (ProjectWizardUI) UI.getCurrent();
        ui.addWindow(subWindow);
      }
    });
    preview.setEnabled(true);
    preview.setVisible(true);
  }

  public ComboBox getISAStudyBox() {
    return isaStudyBox;
  }

  public void listISAStudies(List<Study> studies) {
    isaStudyBox.setVisible(!studies.isEmpty());
    isaStudyBox.removeAllItems();
    for (Study s : studies)
      isaStudyBox.addItem(s.getStudyId());
    isaStudyBox.setEnabled(!studies.isEmpty());
  }

  public void resetFormatSelection() {
    importOptions.setValue(importOptions.getNullSelectionItemId());
  }

}
