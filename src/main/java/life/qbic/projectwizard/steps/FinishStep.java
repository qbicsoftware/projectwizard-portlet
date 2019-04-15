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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

import life.qbic.datamodel.attachments.AttachmentConfig;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.portal.portlet.ProjectWizardUI;
import life.qbic.projectwizard.processes.TSVReadyRunnable;
import life.qbic.projectwizard.registration.OpenbisCreationController;
import life.qbic.projectwizard.registration.UpdateProgressBar;
import life.qbic.projectwizard.uicomponents.UploadsPanel;
import life.qbic.xml.manager.StudyXMLParser;
import life.qbic.xml.properties.Property;
import life.qbic.xml.study.Qexperiment;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.portal.utils.PortalUtils;

/**
 * Wizard Step to downloadTSV and upload the TSV file to and from and register samples and context
 * 
 * @author Andreas Friedrich
 * 
 */
public class FinishStep implements WizardStep {

  private VerticalLayout main;
  private Label summary;
  private VerticalLayout downloads;
  private ProgressBar bar;
  private Label info;
  private Button dlEntities;
  private Button dlExtracts;
  private Button dlPreps;
  private CheckBox attach;
  private UploadsPanel uploads;
  private Wizard w;
  private AttachmentConfig attachConfig;
  private Button browserLink;

  private static final Logger logger = LogManager.getLogger(FinishStep.class);
  private List<FileDownloader> downloaders = new ArrayList<FileDownloader>();
  private OpenbisCreationController creator;

  public FinishStep(final Wizard w, AttachmentConfig attachmentConfig, OpenbisCreationController creator) {
    this.w = w;
    this.attachConfig = attachmentConfig;
    this.creator = creator;

    main = new VerticalLayout();
    main.setMargin(true);
    main.setSpacing(true);
    Label header = new Label("Summary and File Upload");
    main.addComponent(Styles.questionize(header,
        "Here you can download spreadsheets of the samples in your experiment "
            + "and upload informative files belonging to your project, e.g. treatment information. "
            + "It might take a few minutes for your files to show up in our project browser.",
        "Last Step"));
    summary = new Label();
    summary.setContentMode(ContentMode.PREFORMATTED);
    Panel summaryPane = new Panel();
    summaryPane.setContent(summary);
    summaryPane.setWidth("550px");
    main.addComponent(summaryPane);

    downloads = new VerticalLayout();
    downloads.setCaption("Download Spreadsheets:");
    downloads.setSpacing(true);
    dlEntities = new Button("Sample Sources");
    dlExtracts = new Button("Sample Extracts");
    dlPreps = new Button("Sample Preparations");
    dlEntities.setEnabled(false);
    dlExtracts.setEnabled(false);
    dlPreps.setEnabled(false);
    downloads.addComponent(dlEntities);
    downloads.addComponent(dlExtracts);
    downloads.addComponent(dlPreps);

    this.bar = new ProgressBar();
    this.info = new Label();
    info.setCaption("Preparing Spreadsheets");
    main.addComponent(bar);
    main.addComponent(info);
    main.addComponent(downloads);

    browserLink = new Button("Show in Project Browser");
    main.addComponent(browserLink);

    attach = new CheckBox("Upload Additional Files");
    // attach.setVisible(false);
    attach.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        uploads.setVisible(attach.getValue());
        w.getFinishButton().setVisible(!attach.getValue());
      }
    });
    main.addComponent(Styles.questionize(attach,
        "Upload one or more small files pertaining to the experimental design of this project.",
        "Upload Attachments"));
  }

  public void fileCommitDone() {
    uploads.commitDone();
    logger.info("Moving of files to Datamover folder complete!");
    Styles
        .notification("Upload complete",
            "Registration of files complete. It might take a few minutes for your files to show up in the navigator. \n"
                + "You can end the project creation by clicking 'Finish'.",
            NotificationType.SUCCESS);
    w.getFinishButton().setVisible(true);
  }

  public void setExperimentInfos(String space, String proj, String designExpID, String desc,
      Map<String, List<Sample>> samplesByExperiment, IOpenBisClient openbis) {
    boolean empty = samplesByExperiment.isEmpty();
    for (Object listener : browserLink.getListeners(ClickEvent.class))
      browserLink.removeClickListener((ClickListener) listener);
    browserLink.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        String host = UI.getCurrent().getPage().getLocation().getHost();
        String path =
            UI.getCurrent().getPage().getLocation().getPath().replace("creator", "browser");
        String url = "http://" + host + "/" + path + "#!project//" + space + "/" + proj;
        UI.getCurrent().getPage().setLocation(url);
      }
    });

    int entitieNum = 0;
    int samplesNum = 0;
    List<String> ids = new ArrayList<String>();
    for (String exp : samplesByExperiment.keySet()) {
      List<Sample> samps = samplesByExperiment.get(exp);
      for (Sample s : samps)
        ids.add(s.getIdentifier().getIdentifier());
      int amount = samps.size();
      String sampleType = samps.get(0).getType().getCode();
      switch (sampleType) {
        case "Q_BIOLOGICAL_ENTITY":
          entitieNum += amount;
          break;
        case "Q_BIOLOGICAL_SAMPLE":
          samplesNum += amount;
          break;
        case "Q_TEST_SAMPLE":
          samplesNum += amount;
          break;
        default:
          break;
      }
    }
    String amountInfo = "\ndoes not have samples for now.";
    if (!empty) {
      amountInfo = "\nnow has " + entitieNum + " Sample Sources and " + samplesNum + " samples.";
    }
    summary.setValue("Your Experimental Design was registered. Project " + proj + amountInfo + " \n"
        + "Project description: " + desc.substring(0, Math.min(desc.length(), 60)) + "...");
    w.getFinishButton().setVisible(true);

    initUpload(space, proj, openbis);
    if (!empty) {
      downloads.setVisible(true);
      prepareSpreadsheets(
          new ArrayList<String>(
              Arrays.asList("Q_BIOLOGICAL_ENTITY", "Q_BIOLOGICAL_SAMPLE", "Q_TEST_SAMPLE")),
          ids.size(), space, proj, designExpID, openbis);
    } else {
      bar.setVisible(false);
      info.setVisible(false);
      downloads.setVisible(false);
    }
  }

  private void prepareSpreadsheets(List<String> sampleTypes, int numSamples, String space,
      final String project, String designExpID, IOpenBisClient openbis) {

    FinishStep layout = this;
    bar.setVisible(true);
    info.setVisible(true);

    int todo = 3;
    Thread t = new Thread(new Runnable() {
      volatile int current = 0;

      @Override
      public void run() {
        updateProgressBar(current, todo, bar, info);

        while (openbis.getSamplesOfProject("/" + space + "/" + project).size() < numSamples) {
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
        logger.debug("designexpID " + designExpID);
        //List<Experiment> exps = openbis.getExperimentById2(designExpID);
//        Experiment exp = openbis.getExperimentById(designExpID); TODO remove
        StudyXMLParser parser = new StudyXMLParser();
        Set<String> factors = new HashSet<>();
        Map<Pair<String, String>, Property> factorsForLabelsAndSamples = new HashMap<>();

//        if (!exps.isEmpty()) {
          String xml = "";//exp.getProperties().get("Q_EXPERIMENTAL_SETUP"); TODO remove
          try {
            JAXBElement<Qexperiment> expDesign = parser.parseXMLString(xml);
            factors.addAll(parser.getFactorLabels(expDesign));
            factorsForLabelsAndSamples = parser.getFactorsForLabelsAndSamples(expDesign);
          } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
//        }

        Map<String, List<String>> tables = new HashMap<String, List<String>>();
        for (String type : sampleTypes) {
          tables.put(type, openbis.getProjectTSV(project, type));
          current++;
          updateProgressBar(current, todo, bar, info);
        }

        UI.getCurrent().setPollInterval(-1);
        UI.getCurrent().access(
            new TSVReadyRunnable(layout, tables, project, factors, factorsForLabelsAndSamples));
      }
    });
    t.start();
    UI.getCurrent().setPollInterval(100);
  }

  private void updateProgressBar(int current, int todo, ProgressBar bar, Label info) {
    double frac = current * 1.0 / todo;
    UI.getCurrent().access(new UpdateProgressBar(bar, info, frac));
  }

  public void armButtons(List<StreamResource> streams) {
    armDownloadButton(dlEntities, streams.get(0), 1);
    armDownloadButton(dlExtracts, streams.get(1), 2);
    if (streams.size() > 2)
      armDownloadButton(dlPreps, streams.get(2), 3);
  }

  protected void armDownloadButton(Button b, StreamResource stream, int dlnum) {
    if (downloaders.size() < dlnum) {
      FileDownloader dl = new FileDownloader(stream);
      dl.extend(b);
      downloaders.add(dl);
    } else
      downloaders.get(dlnum - 1).setFileDownloadResource(stream);
    b.setEnabled(true);
  }

  private void initUpload(String space, String project, IOpenBisClient openbis) {
    if (uploads != null)
      main.removeComponent(uploads);
    String userID = "admin";
    if (PortalUtils.isLiferayPortlet())
      try {
        userID = PortalUtils.getUser().getScreenName();
      } catch (Exception e) {
        logger.error(e.getMessage());
        logger.error("Could not contact Liferay for User screen name.");
      }

    this.uploads = new UploadsPanel(space, project,
        new ArrayList<String>(Arrays.asList("Experimental Design")), userID, attachConfig,
        (OpenBisClient) openbis, creator);// TODO this cast is not safe in dev mode when openbis is down
    this.uploads.setVisible(false);
    main.addComponent(uploads);
  }

  @Override
  public String getCaption() {
    return "Summary";
  }

  @Override
  public Component getContent() {
    return main;
  }

  @Override
  public boolean onAdvance() {
    return true;
  }

  @Override
  public boolean onBack() {
    return true;
  }

  public void enableDownloads(boolean b) {
    downloads.setEnabled(b);
  }

}
