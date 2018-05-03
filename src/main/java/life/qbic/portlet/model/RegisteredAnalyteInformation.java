package life.qbic.portlet.model;

import java.util.Set;

public class RegisteredAnalyteInformation {

  private Set<String> analytes;
  private boolean measurePeptides;
  private boolean shortGel;
  private String purificationMethod;

  public RegisteredAnalyteInformation(Set<String> analytes, boolean measurePeptides,
      boolean shortGel, String purification) {
    this.analytes = analytes;
    this.measurePeptides = measurePeptides;
    this.shortGel = shortGel;
    this.purificationMethod = purification;
  }

  public Set<String> getAnalytes() {
    return analytes;
  }

  public boolean isMeasurePeptides() {
    return measurePeptides;
  }

  public boolean isShortGel() {
    return shortGel;
  }

  public String getPurificationMethod() {
    return purificationMethod;
  }
}
