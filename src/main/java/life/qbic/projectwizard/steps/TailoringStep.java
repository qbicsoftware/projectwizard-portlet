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


import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import life.qbic.datamodel.samples.AOpenbisSample;
import life.qbic.projectwizard.uicomponents.LabelingMethod;
import life.qbic.projectwizard.uicomponents.SummaryTable;
import life.qbic.portal.Styles;

/**
 * Wizard Step that shows a SummaryTable of the prepared samples and can be used to edit and delete
 * samples
 * 
 * @author Andreas Friedrich
 * 
 */
public class TailoringStep implements WizardStep {

  Logger logger = LogManager.getLogger(TailoringStep.class);
  boolean skip = false;

  private VerticalLayout main;
  private SummaryTable table;

  // Pooling
  private CheckBox poolSelect;

  /**
   * Create a new Experiment Tailoring step
   * 
   * @param name Title of this step
   */
  public TailoringStep(String name, boolean pooling) {
    main = new VerticalLayout();
    main.setSpacing(true);
    main.setMargin(true);
    Label header = new Label(name + " Tailoring");
//    upload = new CheckBox("Prototype: upload " + name + " tsv");
//    upload.addValueChangeListener(new ValueChangeListener() {
//
//      @Override
//      public void valueChange(ValueChangeEvent event) {
//        uploadPanel.setVisible(upload.getValue());
//      }
//    });
//    main.addComponent(ProjectwizardUI.questionize(upload,
//        "Upload a tab-separated values file containing more information about " + name
//            + "s. It needs to contain a column matching it to existing information you put in.",
//        "Upload " + name + " information"));
//    initUpload();
    main.addComponent(Styles.questionize(header,
        "Here you can delete " + name + " that are not part of the"
            + " experiment. You can change the secondary name to something"
            + " more intuitive - experimental variables will be saved in additional columns.",
        name + " Tailoring"));

    if (pooling)
      initPooling(name);
    table = new SummaryTable("Samples");
    table.setVisible(false);
  }

//  private void initUpload() {
//    final Uploader uploader = new Uploader();
//    Upload upload = new Upload("Upload a tsv here", uploader);
//    upload.setButtonCaption("Upload");
//    // Listen for events regarding the success of upload.
//    upload.addFailedListener(uploader);
//    upload.addSucceededListener(uploader);
//    FinishedListener uploadFinListener = new FinishedListener() {
//      /**
//       * 
//       */
//      private static final long serialVersionUID = -8413963075202260180L;
//
//      public void uploadFinished(FinishedEvent event) {
//        String error = uploader.getError();
//        File file = uploader.getFile();
//        table.resetChanges();
//        if (file.getPath().endsWith("up_")) {
//          String msg = "No file selected.";
//          logger.warn(msg);
//          Styles.notification("Something went wrong...", msg, NotificationType.ERROR);
//          if (!file.delete())
//            logger.error(
//                "uploaded metadata file " + file.getAbsolutePath() + " could not be deleted!");
//        } else {
//          if (error == null || error.isEmpty()) {
//            String msg = "Metadata tsv upload successful!";
//            logger.info(msg);
//            Styles.notification("Upload successful", msg, NotificationType.SUCCESS);
//            try {
//              processMetadataUpload(file);
//            } catch (IOException e) {
//              // TODO Auto-generated catch block
//              e.printStackTrace();
//            }
//          } else {
//            logger.error(error);
//            Styles.notification("Something went wrong...", error, NotificationType.ERROR);
//            if (!file.delete())
//              logger
//                  .error("uploaded tmp file " + file.getAbsolutePath() + " could not be deleted!");
//          }
//        }
//      }
//    };
//    upload.addFinishedListener(uploadFinListener);
//  }

//  protected void processMetadataUpload(File tsv) throws IOException {
//    CSVReader reader = new CSVReader(new FileReader(tsv));
//    String error = "";
//    ArrayList<String[]> data = new ArrayList<String[]>();
//    String[] nextLine;
//    int i = 0;
//    while ((nextLine = reader.readNext()) != null) {
//      if (data.isEmpty() || nextLine.length == data.get(0).length) {
//        data.add(nextLine);
//      } else {
//        error = "Wrong number of columns in row " + i
//            + " Please make sure every row fits the header row.";
//        Styles.notification("Parsing Error", error, NotificationType.ERROR);
//      }
//    }
//    reader.close();
//    String[] header = data.get(0);
//    data.remove(0);
//  }

  private void initPooling(String name) {
    poolSelect = new CheckBox();
    poolSelect.setCaption("Pool " + name);
    main.addComponent(Styles.questionize(poolSelect,
        "Select if multiple tissue extracts are pooled into a single sample "
            + "before measurement.",
        "Pooling"));
  }

  public void setSamples(List<AOpenbisSample> samples, LabelingMethod labelingMethod) {
    table.removeAllItems();
    table.initTable(samples, labelingMethod);
    table.setVisible(true);
    table.setPageLength(samples.size());
    main.addComponent(table);
  }

  public List<AOpenbisSample> getSamples() {
    return table.getSamples();
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
    return skip || true;
  }

  @Override
  public boolean onBack() {
    return true;
  }

  public void setSkipStep(boolean b) {
    skip = b;
  }

  public boolean isSkipped() {
    return skip;
  }

  public CheckBox getPoolBox() {
    return poolSelect;
  }

  public boolean pool() {
    return poolSelect != null && poolSelect.getValue();
  }
}
