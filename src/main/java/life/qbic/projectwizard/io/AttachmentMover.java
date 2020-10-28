package life.qbic.projectwizard.io;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import life.qbic.datamodel.attachments.AttachmentConfig;
import life.qbic.projectwizard.processes.MoveUploadsReadyRunnable;
import life.qbic.projectwizard.registration.UpdateProgressBar;

/**
 * Provides methods to move uploaded attachments to the datamover folder on the portal and create
 * marker files to start movement to the DSS.
 * 
 * @author Andreas Friedrich
 * 
 */
public class AttachmentMover {

  private AttachmentConfig config;
  private String tmpFolder;
  private Logger logger = LogManager.getLogger(AttachmentMover.class);

  /**
   * Create a new AttachmentMover
   * 
   * @param tmpFolder temp folder for file uploads
   */
  public AttachmentMover(String tmpFolder, AttachmentConfig attachmentConfig) {
    this.tmpFolder = tmpFolder;
    this.config = attachmentConfig;
  }

  public void createAttachmentFromStringMoveAndMarker(String content, String fileName,
      String description, String type, String user, String barcode) {
    Date date = new java.util.Date();
    String timeStamp =
        new Timestamp(date.getTime()).toString().split(" ")[1].replace(":", "").replace(".", "");

    AttachmentInformation attachment =
        new AttachmentInformation(fileName, description, type, user, barcode, timeStamp);
    Sardine sardine = SardineFactory.begin(config.getUser(), config.getPass());

    String folder = attachment.getFolder();
    byte[] data = content.getBytes(StandardCharsets.UTF_8);
    try {
      sardine.createDirectory(config.getUri() + timeStamp);

      sardine.put(config.getUri() + folder + "/" + attachment.getFileName(), data);

      byte[] metadata = createMetadataString(attachment).getBytes();
      sardine.put(config.getUri() + folder + "/" + "metadata.txt", metadata);

      Thread.sleep(1000);

      createMarker(sardine, folder, attachment.getFileName());

    } catch (Exception e1) {
      e1.printStackTrace();
      try {
        sardine.shutdown();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void moveAttachment(Sardine sardine, AttachmentInformation attachment) {
    byte[] data;
    try {
      sardine.createDirectory(config.getUri() + attachment.getFolder());

      data = FileUtils.readFileToByteArray(new File(tmpFolder + attachment.getName()));
      sardine.put(config.getUri() + attachment.getFolder() + "/" + attachment.getFileName(), data);

      byte[] metadata = createMetadataString(attachment).getBytes();
      sardine.put(config.getUri() + attachment.getFolder() + "/" + "metadata.txt", metadata);

    } catch (Exception e1) {
      e1.printStackTrace();
      try {
        sardine.shutdown();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Moves attachments to the datamover folder.
   * 
   * @param attachments List of names and other infos for each attachment
   * @param moveUploadsReadyRunnable
   * @param object2
   * @param object
   * @return
   * @throws IOException
   */
  public void moveAttachments(final List<AttachmentInformation> attachments, final ProgressBar bar,
      final Label info, final MoveUploadsReadyRunnable ready) throws IOException {
    final Sardine sardine = SardineFactory.begin(config.getUser(), config.getPass());
    ready.setSardine(sardine);
    final int todo = attachments.size();
    if (todo > 0) {
      Thread t = new Thread(new Runnable() {
        volatile int current = 0;

        @Override
        public void run() {
          for (AttachmentInformation a : attachments) {
            current++;
            double frac = current * 1.0 / todo;
            UI.getCurrent().access(new UpdateProgressBar(bar, info, frac));
            moveAttachment(sardine, a);
          }
          UI.getCurrent().access(ready);
          UI.getCurrent().setPollInterval(-1);
          try {
            createMarkers(attachments);
          } catch (InterruptedException e) {
            e.printStackTrace();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });
      t.start();
      UI.getCurrent().setPollInterval(100);
    } else {
      UI.getCurrent().access(ready);
    }
  }

  protected String createMetadataString(AttachmentInformation a) {
    String user = a.getUser();
    String info = a.getInfo().replace("\n", " ");
    String barcode = a.getBarcode();
    String type = a.getType();
    return "user=" + user + "\n" + "info=" + info + "\n" + "barcode=" + barcode + "\n" + "type="
        + type;
  }

  private void createMarker(Sardine sardine, String folder, String fileName)
      throws InterruptedException, IOException {
    String prefix = ".MARKER_is_finished_";
    String datafile = folder + "/" + fileName;
    byte[] data = new byte[0];
    int maxTries = 2;
    while (maxTries > 0 && !sardine.exists(config.getUri() + datafile)) {
      maxTries--;
      Thread.sleep(100);
    }
    if (!sardine.exists(config.getUri() + datafile)) {
      logger.error("Did not create marker file because folder " + datafile
          + " was not found on the server.");
      logger.error("Killing Sardine process.");
      sardine.shutdown();
    } else {
      // data = FileUtils.readFileToByteArray(marker);
      sardine.put(config.getUri() + prefix + folder, data);
    }
  }

  private void createMarkers(List<AttachmentInformation> attachments)
      throws InterruptedException, IOException {
    Sardine sardine = SardineFactory.begin(config.getUser(), config.getPass());
    for (AttachmentInformation a : attachments) {
      createMarker(sardine, a.getFolder(), a.getFileName());
    }
    sardine.shutdown();
  }
}
