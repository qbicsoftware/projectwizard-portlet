package life.qbic.projectwizard.registration;

import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;

public class UpdateProgressBar implements Runnable {
  ProgressBar progress;
  Label status;
  double current;

  public UpdateProgressBar(ProgressBar progress, Label status, double current) {
    this.progress = progress;
    this.status = status;
    this.current = current;
  }
  
  @Override
  public void run() {
    progress.setValue(new Float(current));
    if (current < 1.0) {
      status.setValue(((int) (current * 100)) + "% done.");
    } else {
      status.setCaption("Ready");
      status.setValue("Registration complete!");
    }
  }

}
