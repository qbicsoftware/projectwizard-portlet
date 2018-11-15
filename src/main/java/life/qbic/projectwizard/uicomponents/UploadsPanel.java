package life.qbic.projectwizard.uicomponents;


import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Upload.FinishedEvent;
import com.vaadin.ui.Upload.FinishedListener;
import com.vaadin.ui.themes.ValoTheme;

import life.qbic.datamodel.attachments.AttachmentConfig;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.projectwizard.io.AttachmentInformation;
import life.qbic.projectwizard.io.AttachmentMover;
import life.qbic.projectwizard.processes.MoveUploadsReadyRunnable;
import life.qbic.projectwizard.registration.OpenbisCreationController;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.VerticalLayout;

public class UploadsPanel extends VerticalLayout {

  /**
   * 
   */
  private static final long serialVersionUID = 6971325287434528738L;

  private static final Logger logger = LogManager.getLogger(UploadsPanel.class);

  private File current;
  private Map<Object, AttachmentInformation> attachments;
  private String space;
  private String project;

  private ComboBox uploadType;
  private UploadComponent upload;
  private TextField fileInfo;
  private Table toUpload;
  private Button commit;

  private OpenBisClient openbis;
  private OpenbisCreationController openbisCreator;
  private Label info;
  private ProgressBar bar;

  private String userID;
  private String tmpFolder;
  
  public UploadsPanel(String tmpFolder, String space, String project, List<String> expOptions,
      String userID, AttachmentConfig attachConfig, OpenBisClient openbis) {
    this.openbisCreator = new OpenbisCreationController(openbis);
    this.openbis = openbis;
    this.tmpFolder = tmpFolder;
    this.userID = userID;
    this.space = space;
    this.project = project;
    attachments = new HashMap<Object, AttachmentInformation>();

    fileInfo = new TextField("Description");
    fileInfo.setStyleName(Styles.fieldTheme);
    fileInfo.setRequired(true);
    fileInfo.setImmediate(true);
    fileInfo.setTextChangeEventMode(TextChangeEventMode.EAGER);
    addComponent(fileInfo);

    uploadType = new ComboBox("Attach to...");
    uploadType.setNullSelectionAllowed(false);
    uploadType.addItems(expOptions);
    uploadType.setValue(expOptions.get(0));
    if (expOptions.size() == 1)
      uploadType.setEnabled(false);
    addComponent(uploadType);

    initUpload(attachConfig.getMaxSize());

    initTable();

    bar = new ProgressBar();
    bar.setVisible(false);
    info = new Label();
    addComponent(bar);
    addComponent(info);

    commit = new Button("Upload all Files");
    addComponent(commit);
    commit.setVisible(false);

    final UploadsPanel view = this;

    final AttachmentMover mover = new AttachmentMover(tmpFolder, attachConfig);
    commit.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        startCommit();
        try {
          mover.moveAttachments(new ArrayList<AttachmentInformation>(getAttachments().values()),
              getBar(), getLabel(), new MoveUploadsReadyRunnable(view));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });

    setSpacing(true);
  }

  protected String createMetadataString(AttachmentInformation a) {
    String user = a.getUser();
    String info = a.getInfo().replace("\n", " ");
    String barcode = a.getBarcode();
    return "user=" + user + "\n" + "info=" + info + "\n" + "barcode=" + barcode + "\n" + "type="
        + uploadType.getValue().toString();
  }

  private void initTable() {
    toUpload = new Table("Files to Upload");
    toUpload.setWidth("400px");
    toUpload.addContainerProperty("Info", String.class, null);
    toUpload.addContainerProperty("Context", String.class, null);
    toUpload.addContainerProperty("Remove", Button.class, null);
    addComponent(toUpload);
    toUpload.setVisible(false);
  }

  private String getBarcode() {
    return project + "000";
  }

  public Button getCommitButton() {
    return commit;
  }

  private void tableChanged() {
    boolean notEmpty = toUpload.size() != 0;
    toUpload.setPageLength(toUpload.size() + 1);
    toUpload.setVisible(notEmpty);
    commit.setVisible(notEmpty);
  }

  private void initUpload(int maxSize) {
    upload = new UploadComponent("Select File", "Add File", tmpFolder, userID, maxSize * 1000000);
    if (!new File(tmpFolder).exists()) {
      logger.error("tmp folder " + tmpFolder
          + " does not exist! Create it or set another folder in properties file.");
    }

    upload.getUploadComponent().setEnabled(false);
    fileInfo.addTextChangeListener(new TextChangeListener() {

      @Override
      public void textChange(TextChangeEvent event) {
        upload.getUploadComponent().setEnabled(!event.getText().isEmpty());
      }
    });

    FinishedListener uploadFinListener = new FinishedListener() {
      /**
       * 
       */
      private static final long serialVersionUID = -8413963075202260180L;

      public void uploadFinished(FinishedEvent event) {
        if (upload.wasSuccess()) {
          File file = upload.getFile();
          if (!file.getName().equals("up_")) {
            logger.info("Upload successful");
            current = file;
            int i = file.getName().lastIndexOf('.');
            if (i < 0)
              i = file.getName().length();
            if (fileInfo.getValue() == null || fileInfo.getValue().isEmpty())
              fileInfo.setValue(file.getName().substring(3, i));

            Button delete = new Button();
            delete.setStyleName(ValoTheme.BUTTON_BORDERLESS);
            delete.setIcon(FontAwesome.TRASH_O);
            delete.setWidth("10px");

            Object itemId = toUpload.addItem();

            delete.setData(itemId);
            String secondary = fileInfo.getValue();
            String type = (String) uploadType.getValue();

            Date date = new java.util.Date();
            String timeStamp = new Timestamp(date.getTime()).toString().split(" ")[1]
                .replace(":", "").replace(".", "");

            attachments.put(itemId, new AttachmentInformation(current.getName(), secondary, type,
                userID, getBarcode(), timeStamp));

            fileInfo.setValue("");

            delete.addClickListener(new Button.ClickListener() {
              /**
              * 
              */
              private static final long serialVersionUID = 5414603256990177472L;

              @Override
              public void buttonClick(ClickEvent event) {
                Integer iid = (Integer) event.getButton().getData();
                toUpload.removeItem(iid);
                attachments.remove(iid);
                tableChanged();
              }
            });
            toUpload.getContainerProperty(itemId, "Info").setValue(secondary);
            toUpload.getContainerProperty(itemId, "Context").setValue(type);
            toUpload.getContainerProperty(itemId, "Remove").setValue(delete);
            tableChanged();
            upload.getUploadComponent().setEnabled(false);
          }
        }
      }
    };
    upload.addFinishedListener(uploadFinListener);
    addComponent(upload);
  }

  public Map<Object, AttachmentInformation> getAttachments() {
    return attachments;
  }

  public Label getLabel() {
    return info;
  }

  public ProgressBar getBar() {
    return bar;
  }

  public void startCommit() {
    commit.setEnabled(false);
    bar.setVisible(true);

    String sample = project + "000";
    String experiment = project + "_INFO";
    if (!openbis.sampleExists(sample)) {
      if (!openbis.expExists(space, project, experiment)) {
        openbisCreator.registerExperiment(space, project, ExperimentType.Q_PROJECT_DETAILS, experiment,
            new HashMap<String, Object>(), userID);
        while (!openbis.expExists(space, project, experiment))
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
      }
      List<ISampleBean> samples = new ArrayList<ISampleBean>();
      samples.add(new TSVSampleBean(sample, experiment, project, space, "Q_ATTACHMENT_SAMPLE", "",
          new ArrayList<String>(), new HashMap<String, Object>()));
      openbisCreator.registerSampleBatchInETL(samples, userID);
      double timeoutS = 10.0;
      while (!openbis.sampleExists(sample))
        if (timeoutS <= 0) {
          logger.error("Could not create attachment sample. User has been informed");
          Styles.notification("Upload failed",
              "Upload failed, the data management system might be offline. Please contact us if the problem persists.",
              NotificationType.ERROR);
          break;
        } else {
          try {
            double sleeptimeMS = 100.0;
            Thread.sleep((long) sleeptimeMS);
            timeoutS -= sleeptimeMS / 1000;
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
    }
  }

  public void commitDone() {
    // upload.setVisible(false);
    bar.setVisible(false);
    info.setValue("Successfully moved all files.");
    toUpload.removeAllItems();
    toUpload.setPageLength(1);
  }
}
