package life.qbic.portlet.io;

public class NCBITerm implements Comparable<NCBITerm> {

  private String taxID;
  private String sciName;
  private String description;

  public NCBITerm(String taxID, String sciName, String description) {
    super();
    this.taxID = taxID;
    this.sciName = sciName;
    this.description = description;
  }

  public String getTaxID() {
    return taxID;
  }

  public void setTaxID(String taxID) {
    this.taxID = taxID;
  }

  public String getSciName() {
    return sciName;
  }

  public void setSciName(String sciName) {
    this.sciName = sciName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return taxID + " - " + sciName;
  }

  @Override
  public int compareTo(NCBITerm o) {
    return sciName.compareTo(o.sciName);
  }

}
