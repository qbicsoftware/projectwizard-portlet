package life.qbic.projectwizard.views;

import java.util.List;

public class ConversionReadyRunnable implements Runnable {

  private ExperimentalDesignConversionView view;
  private List<String> spaces;

  public ConversionReadyRunnable(ExperimentalDesignConversionView caller, List<String> spaces) {
    this.view = caller;
    this.spaces = spaces;
  }

  @Override
  public void run() {
    view.conversionFinished(spaces);
  }

}
