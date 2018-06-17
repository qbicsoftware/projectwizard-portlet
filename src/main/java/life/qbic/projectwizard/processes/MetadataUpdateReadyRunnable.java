package life.qbic.projectwizard.processes;

import life.qbic.projectwizard.views.MetadataUploadView;

public class MetadataUpdateReadyRunnable implements Runnable {

  MetadataUploadView view;

  public MetadataUpdateReadyRunnable(MetadataUploadView view) {
    this.view = view;
  }

  @Override
  public void run() {
    try {
      view.ingestionComplete();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
