package life.qbic.projectwizard.model;

import java.util.ArrayList;
import java.util.List;

public class OntologyTerm {

  private String label;
  private String comment;
  private List<OntologyTerm> subterms;

  public OntologyTerm(String label, String comment) {
    this.label = label;
    this.comment = comment;
    subterms = new ArrayList<OntologyTerm>();
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public List<OntologyTerm> getSubterms() {
    return subterms;
  }

  public void setSubterms(List<OntologyTerm> subterms) {
    this.subterms = subterms;
  }

  public void addSubterm(OntologyTerm term) {
    this.subterms.add(term);
  }

  public String toString() {
    String res = label + " - " + comment;
    for (OntologyTerm term : subterms) {
      res += "\n->" + term.toString();
    }
    return res;
  }

}
