package life.qbic.portlet.io;

public class OntologyEntry {

  private String id;
  private String label;
  private String description;

  public OntologyEntry(String id, String label, String description) {
    super();
    this.id = id;
    this.label = label;
    this.description = description;
  }

  public String getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public String getDescription() {
    return description;
  }
}
