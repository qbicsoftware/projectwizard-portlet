package isatab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ISAOntologyModel {

  private final String ISA_HEADER = "ONTOLOGY SOURCE REFERENCE";
  private final List<String> labelsInOrder = new ArrayList<String>(Arrays.asList("Term Source Name",
      "Term Source File", "Term Source Version", "Term Source Description"));
  private List<Ontology> ontologies;

  public ISAOntologyModel() {
    super();
    this.ontologies = new ArrayList<Ontology>();
  }

  public void addOntology(Ontology o) {
    this.ontologies.add(o);
  }

  public void addOntologies(List<Ontology> ontologies) {
    this.ontologies.addAll(ontologies);
  }

  public List<Ontology> getOntologies() {
    return this.ontologies;
  }

  public List<String> getISASection() {
    List<String> res = new ArrayList<String>(Arrays.asList(ISA_HEADER));
    for (int i = 0; i < labelsInOrder.size(); i++) {
      String label = labelsInOrder.get(i);
      StringBuilder row = new StringBuilder(label);
      for (Ontology o : ontologies) {
        row.append("\t").append(o.getISAValueByPosition(i));
      }
      res.add(row.toString());
    }
    return res;
  }


}
