package life.qbic.projectwizard.uicomponents;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;

import com.vaadin.server.FontAwesome;
import com.vaadin.server.StreamVariable;
import com.wcs.wcslib.vaadin.widget.multifileupload.ui.AllUploadFinishedHandler;
import com.wcs.wcslib.vaadin.widget.multifileupload.ui.MultiFileUpload;
import com.wcs.wcslib.vaadin.widget.multifileupload.ui.UploadFinishedHandler;
import com.wcs.wcslib.vaadin.widget.multifileupload.ui.UploadStatePanel;
import com.wcs.wcslib.vaadin.widget.multifileupload.ui.UploadStateWindow;

import life.qbic.portal.portlet.ProjectWizardUI;

public class MultiUploadComponent {

  private static final int FILE_COUNT = 15;
  private MultiFileUpload upload;
  private UploadStateWindow uploadStateWindow = new UploadStateWindow();
  private UploadFinishedHandler uploadFinishedHandler;
  private String creationTimeStamp;
  private File directory;
  
  public MultiUploadComponent() {
    createUploadFinishedHandler();

    addUpload("", true);
  }
  
  private void generateNewTimeStamp() {
    Date date = new java.util.Date();
    creationTimeStamp = new SimpleDateFormat("HHmmssS").format(new Timestamp(date.getTime()));
  }
  
  public File getISAFolder() {
    return directory;
  }
  
  public MultiFileUpload getUpload() {
    return upload;
  }

  private void createUploadFinishedHandler() {
    uploadFinishedHandler = (InputStream stream, String fileName, String mimeType, long length,
        int filesLeftInQueue) -> {
          if(creationTimeStamp==null)
            generateNewTimeStamp();
      
      directory = new File(ProjectWizardUI.tmpFolder, creationTimeStamp);
      directory.mkdirs();
      FileOutputStream fos = null;
      File file = new File(directory, fileName);      
      try {
        fos = new FileOutputStream(file);
        IOUtils.copy(stream, fos);
        System.out.println(file);
      } catch (final java.io.FileNotFoundException e) {
        throw new RuntimeException(e);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } finally {
        try {
          fos.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      
      if(filesLeftInQueue==0)
        creationTimeStamp = null;
      
    };
  }

  private void addUpload(String caption, boolean multiple) {

    upload = new ISAUpload(uploadFinishedHandler, uploadStateWindow, multiple);
    int maxFileSize = 10485760; // 5 MB
    upload.setMaxFileSize(maxFileSize);
    String errorMsgPattern = "File is too big (max = {0}): {2} ({1})";
    upload.setSizeErrorMsgPattern(errorMsgPattern);
    upload.setImmediate(true);
    upload.setCaption(caption);
    upload.setPanelCaption(caption);
    upload.setMaxFileCount(FILE_COUNT);
    upload.getSmartUpload().setUploadButtonCaptions("Upload File", "Select Files");
//    upload.getSmartUpload().setUploadButtonIcon(FontAwesome.UPLOAD);
  }

  private class ISAUpload extends MultiFileUpload {

    public ISAUpload(UploadFinishedHandler handler, UploadStateWindow uploadStateWindow) {
      super(handler, uploadStateWindow, true);
    }

    public ISAUpload(UploadFinishedHandler handler, UploadStateWindow uploadStateWindow,
        boolean multiple) {
      super(handler, uploadStateWindow, multiple);
    }

    @Override
    protected UploadStatePanel createStatePanel(UploadStateWindow uploadStateWindow) {
      return new ISAUploadStatePanel(uploadStateWindow);
    }
  }

  private class ISAUploadStatePanel extends UploadStatePanel {

    public ISAUploadStatePanel(UploadStateWindow window) {
      super(window);
    }

    // hide streaming windows - our files are small
    @Override
    public void streamingStarted(StreamVariable.StreamingStartEvent event) {
      super.streamingStarted(event);
      setVisible(false);
      this.getWindow().refreshVisibility();
    }

  }

  public void setFinishedHandler(AllUploadFinishedHandler handler) {
    upload.setAllUploadFinishedHandler(handler);
  }
}
