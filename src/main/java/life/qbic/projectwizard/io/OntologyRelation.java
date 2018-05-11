package life.qbic.projectwizard.io;

public class OntologyRelation {

  private String from;
  private String to;
  private String relationType;


  public OntologyRelation(String from, String to, String relationType) {
    super();
    this.from = from;
    this.to = to;
    this.relationType = relationType;
  }

  public String getFrom() {
    return from;
  }

  public String getTo() {
    return to;
  }

  public String getRelationType() {
    return relationType;
  }

  public String toString() {
    return from + " " + relationType + " " + to;
  }

}
