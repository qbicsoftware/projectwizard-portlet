package life.qbic.projectwizard.uicomponents;


import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.FinishedEvent;
import com.vaadin.ui.Upload.FinishedListener;
import com.vaadin.ui.Upload.StartedEvent;
import com.vaadin.ui.Upload.StartedListener;
import com.vaadin.ui.Upload.SucceededEvent;

import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;

import com.vaadin.ui.VerticalLayout;

public class UploadComponent extends VerticalLayout
    implements Upload.SucceededListener, Upload.FailedListener, Upload.Receiver,
    Upload.ProgressListener, Upload.FinishedListener, StartedListener {

  /**
   * 
   */
  private static final long serialVersionUID = -3228100993055350771L;
  protected Upload upload;
  protected String directory;
  protected String user;
  protected File file;
  protected long maxSize; // In bytes. 100Kb = 100000
  protected ProgressBar progressIndicator; // May be null
  protected boolean cancelled = false;
  protected Long contentLength;
  protected Button cancelProcessing;
  protected HorizontalLayout processingLayout;

  private static final Logger logger = LogManager.getLogger(UploadComponent.class);
  private boolean success;

  public UploadComponent(String fieldCaption, String buttonCaption, String directoryParam,
      String user, int maxSize) {
    upload = new Upload(fieldCaption, null);
    this.user = user;
    this.addComponent(upload);
    this.maxSize = maxSize;
    upload.setReceiver(this);
    this.directory = directoryParam;
    // this.targetPrefix = targetPrefix;
    upload.setButtonCaption(buttonCaption);
    upload.addSucceededListener(this);
    upload.addFailedListener(this);
    upload.addProgressListener(this);
    upload.addFinishedListener(this);
    upload.addStartedListener(this);

    processingLayout = new HorizontalLayout();
    processingLayout.setSpacing(true);
    processingLayout.setVisible(false);
    this.addComponent(processingLayout);

    progressIndicator = new ProgressBar();
    progressIndicator.setWidth("100%");
    processingLayout.addComponent(progressIndicator);

    cancelProcessing = new Button("cancel", new Button.ClickListener() {
      /**
       * 
       */
      private static final long serialVersionUID = -6812538024652192780L;

      @Override
      public void buttonClick(ClickEvent event) {
        cancelled = true;
        upload.interruptUpload();
      }
    });

    cancelProcessing.setStyleName("small");
    processingLayout.addComponent(cancelProcessing);
  }

  @Override
  public OutputStream receiveUpload(String filename, String MIMEType) {
    filename = FilenameUtils.getName(filename);
    if (filename.isEmpty()) {
      upload.interruptUpload();
    }
    FileOutputStream fos = null;
    Date date = new java.util.Date();
    String timeStamp = new SimpleDateFormat("HHmmssS").format(new Timestamp(date.getTime()));
    file = new File(directory, user + "_" + timeStamp + "_" + filename);
    try {
      fos = new FileOutputStream(file);
    } catch (final java.io.FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    return fos; // Return the output stream to write to
  }

  public Upload getUploadComponent() {
    return upload;
  }

  @Override
  public void updateProgress(long readBytes, long contentLength) {
    this.contentLength = contentLength;
    if (maxSize < contentLength) {
      upload.interruptUpload();
      return;
    }

    processingLayout.setVisible(true);
    progressIndicator.setValue(new Float(readBytes / (float) contentLength));
  }

  @Override
  public void uploadFinished(FinishedEvent event) {
    processingLayout.setVisible(false);
  }


  @Override
  public void uploadFailed(FailedEvent event) {
    processingLayout.setVisible(false);
    if (event.getFilename().isEmpty()) {
      Styles.notification("No file selected", "Please select a file before adding it.",
          NotificationType.ERROR);

      logger.info("Upload was cancelled due to no file selected.");
    } else if (contentLength != null && maxSize < contentLength) {
      Styles.notification("File too large", "Your file is " + contentLength / 1000
          + "Kb long. Maximum file size is " + maxSize / 1000 + "Kb", NotificationType.ERROR);

      logger.info("Upload was cancelled due to file exceeding size limit.");
    } else if (cancelled) {
      // Nothing to do...
    } else {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      if (event.getReason() != null)
        event.getReason().printStackTrace(pw);
      logger.error("Upload cancelled due to error.");
      Styles.notification("There was a problem uploading your file.", "", NotificationType.ERROR);
    }

    try {
      file.delete();
    } catch (Exception e) {
      // Silent exception. If we can't delete the file, it's not big problem. May the file did not
      // even exist.
    }
  }

  public String getDirectory() {
    return directory;
  }

  public File getFile() {
    return file;
  }

  @Override
  public void uploadSucceeded(SucceededEvent event) {
    success = true;
  }

  public void addFinishedListener(FinishedListener listener) {
    upload.addFinishedListener(listener);
  }

  public boolean wasSuccess() {
    return success;
  }

  @Override
  public void uploadStarted(StartedEvent event) {
    success = false;
  }

}
