package life.qbic.projectwizard.model;

public class TissueInfo {

  private String primary;
  private String specific;

  public TissueInfo(String primary, String other) {
    super();
    this.primary = primary;
    this.specific = other;
  }

  public String getPrimary() {
    return primary;
  }
  
  public String getSpecific() {
    return specific;
  }

}
