package life.qbic.portlet.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import life.qbic.portlet.model.OntologyTerm;

public class OntologyParser {

  private final String prefix = "http://purl.obolibrary.org/obo/";
  // private OWLOntologyManager manager;
  // private OWLOntology ontology = null;
  private DBManager dbm;
  // private OWLDataFactory df;
  // private OWLReasoner reasoner;
  static Set<String> types = new LinkedHashSet<String>();

  public OntologyParser(DBManager dbm) {
    this.dbm = dbm;
  }

  private static List<String> getSubClassIDs(String xml, String id)
      throws XPathExpressionException, SAXException, IOException, ParserConfigurationException {
    FileInputStream file = new FileInputStream(new File(xml));

    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = builderFactory.newDocumentBuilder();
    Document xmlDocument = builder.parse(file);
    XPath xPath = XPathFactory.newInstance().newXPath();

    String expression = "/Ontology/SubClassOf/Class[2][@IRI='" + id + "']/preceding-sibling::Class";
    NodeList nodeList =
        (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
    List<String> methodIDs = new ArrayList<String>();
    for (int i = 0; i < nodeList.getLength(); i++) {
      String subID = nodeList.item(i).getAttributes().getNamedItem("IRI").getNodeValue();
      methodIDs.add(subID);
    }
    return methodIDs;
  }

  private static OntologyTerm getOntologyTerm(String xml, String id)
      throws SAXException, IOException, XPathExpressionException, ParserConfigurationException {
    // <AnnotationAssertion>
    // <AnnotationProperty IRI="http://purl.obolibrary.org/obo/created_by"/>
    // <IRI>http://purl.obolibrary.org/obo/PRIDE_0000328</IRI>
    // <Literal datatypeIRI="http://www.w3.org/2001/XMLSchema#string">jg</Literal>
    // </AnnotationAssertion>
    // <AnnotationAssertion>
    // <AnnotationProperty IRI="http://purl.obolibrary.org/obo/creation_date"/>
    // <IRI>http://purl.obolibrary.org/obo/PRIDE_0000328</IRI>
    // <Literal
    // datatypeIRI="http://www.w3.org/2001/XMLSchema#string">2011-07-22T09:48:42Z</Literal>
    // </AnnotationAssertion>
    // <AnnotationAssertion>
    // <AnnotationProperty abbreviatedIRI="rdfs:comment"/>
    // <IRI>http://purl.obolibrary.org/obo/PRIDE_0000328</IRI>
    // <Literal datatypeIRI="http://www.w3.org/2001/XMLSchema#string">Reagent used in SILAC
    // labeling.</Literal>
    // </AnnotationAssertion>
    // <AnnotationAssertion>
    // <AnnotationProperty abbreviatedIRI="rdfs:label"/>
    // <IRI>http://purl.obolibrary.org/obo/PRIDE_0000328</IRI>
    // <Literal datatypeIRI="http://www.w3.org/2001/XMLSchema#string">SILAC reagent</Literal>
    // </AnnotationAssertion>
    FileInputStream file = new FileInputStream(new File(xml));

    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = builderFactory.newDocumentBuilder();
    Document xmlDocument = builder.parse(file);
    XPath xPath = XPathFactory.newInstance().newXPath();

    String expression = "/Ontology/AnnotationAssertion//*[contains(., '" + id
        + "')]/preceding-sibling::AnnotationProperty[@abbreviatedIRI='rdfs:label']/following-sibling::Literal";
    NodeList nodeList =
        (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
    String label = nodeList.item(0).getFirstChild().getNodeValue();

    expression = "/Ontology/AnnotationAssertion//*[contains(., '" + id
        + "')]/preceding-sibling::AnnotationProperty[@abbreviatedIRI='rdfs:comment']/following-sibling::Literal";
    nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
    String comment = "";
    if (nodeList.getLength() > 0)
      comment = nodeList.item(0).getFirstChild().getNodeValue();
    return new OntologyTerm(label, comment);
  }

  // <rdf:RDF xmlns="http://purl.obolibrary.org/obo/bto.owl#"
  // xml:base="http://purl.obolibrary.org/obo/bto.owl"
  // xmlns:obo="http://purl.obolibrary.org/obo/"
  // xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
  // xmlns:bto="http://purl.obolibrary.org/obo/bto#"
  // xmlns:owl="http://www.w3.org/2002/07/owl#"
  // xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
  // xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  // xmlns:oboInOwl="http://www.geneontology.org/formats/oboInOwl#">
  // <owl:Class rdf:about="http://purl.obolibrary.org/obo/BTO_0000639">
  // <rdfs:label rdf:datatype="http://www.w3.org/2001/XMLSchema#string">intersegmental
  // muscle</rdfs:label>
  // <rdfs:subClassOf rdf:resource="http://purl.obolibrary.org/obo/BTO_0000887"/>
  // <rdfs:subClassOf>
  // <owl:Restriction>
  // <owl:onProperty rdf:resource="http://purl.obolibrary.org/obo/bto#part_of"/>
  // <owl:someValuesFrom rdf:resource="http://purl.obolibrary.org/obo/BTO_0001266"/>
  // </owl:Restriction>
  // </rdfs:subClassOf>
  // <rdfs:subClassOf>
  // <owl:Restriction>
  // <owl:onProperty rdf:resource="http://purl.obolibrary.org/obo/bto#part_of"/>
  // <owl:someValuesFrom rdf:resource="http://purl.obolibrary.org/obo/BTO_0001368"/>
  // </owl:Restriction>
  // </rdfs:subClassOf>
  // <oboInOwl:id rdf:datatype="http://www.w3.org/2001/XMLSchema#string">BTO:0000639</oboInOwl:id>
  // <oboInOwl:hasOBONamespace
  // rdf:datatype="http://www.w3.org/2001/XMLSchema#string">BrendaTissueOBO</oboInOwl:hasOBONamespace>
  // <obo:IAO_0000115 rdf:datatype="http://www.w3.org/2001/XMLSchema#string">The short
  // intersegmental muscle is located between the prothorax and the mesothorax.</obo:IAO_0000115>
  // </owl:Class>
  // </rdf:RDF>

  private void createInserts(File ontology, List<OntologyEntry> entries,
      List<OntologyRelation> relations)
      throws ParserConfigurationException, XPathExpressionException, SAXException, IOException {
    String prefix = "http://purl.obolibrary.org/obo/";
    FileInputStream file = new FileInputStream(ontology);

    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = builderFactory.newDocumentBuilder();
    Document xmlDocument = builder.parse(file);
    XPath xPath = XPathFactory.newInstance().newXPath();
    // String expression = "/RDF/Class/@about | /RDF/Class/label | /RDF/Class/IAO_0000115 |
    // /RDF/Class/subClassOf/@resource";
    String expression = "/RDF/Class";
    NodeList nodeList =
        (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node cls = nodeList.item(i);
      String id =
          cls.getAttributes().getNamedItem("rdf:about").getTextContent().replace(prefix, "");
      String label = null;
      String desc = "";
      for (int j = 0; j < cls.getChildNodes().getLength(); j++) {
        Node n = cls.getChildNodes().item(j);
        String name = n.getNodeName();
        switch (name) {
          case "rdfs:label":
            label = n.getTextContent();
            break;
          case "rdfs:subClassOf":
            if (n.hasChildNodes()) {
              for (int k = 0; k < n.getChildNodes().getLength(); k++) {
                Node c = n.getChildNodes().item(k);
                if (c.getNodeName().equals("owl:Restriction")) {
                  String to = null;
                  String type = null;
                  for (int l = 0; l < c.getChildNodes().getLength(); l++) {
                    Node rel = c.getChildNodes().item(l);
                    if (rel.getNodeName().equals("owl:onProperty")) {
                      type = rel.getAttributes().getNamedItem("rdf:resource").getTextContent()
                          .replace("http://purl.obolibrary.org/obo/bto#", "");
                    }
                    if (rel.getNodeName().equals("owl:someValuesFrom")) {
                      to = rel.getAttributes().getNamedItem("rdf:resource").getTextContent()
                          .replace(prefix, "");
                    }
                  }
                  relations.add(new OntologyRelation(id, to, type));
                }
              }
            } else {
              String to = n.getAttributes().getNamedItem("rdf:resource").getTextContent()
                  .replace(prefix, "");
              relations.add(new OntologyRelation(id, to, "is_a"));
            }
            break;
          case "obo:IAO_0000115":
            desc = n.getTextContent();
          default:
            break;
        }
      }
      entries.add(new OntologyEntry(id, label, desc));
    }
  }

  private void loadOntologyToDB(File file)
      throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
    // File file3 = new File("/Users/frieda/Downloads/bto.owl");
    List<OntologyEntry> entries = new ArrayList<OntologyEntry>();
    List<OntologyRelation> relations = new ArrayList<OntologyRelation>();
    createInserts(file, entries, relations);
    for (OntologyEntry e : entries) {
      Map<String, Object> entryInserts = new HashMap<String, Object>();
      entryInserts.put("id", e.getId());
      entryInserts.put("name", e.getLabel());
      if (e.getDescription() != null && !e.getDescription().isEmpty())
        entryInserts.put("description", e.getDescription());
      dbm.genericInsertIntoTable("ontology_entry", entryInserts);
    }
    for (OntologyRelation r : relations) {
      Map<String, Object> relationInserts = new HashMap<String, Object>();
      relationInserts.put("child_entry", r.getFrom());
      relationInserts.put("parent_entry", r.getTo());
      relationInserts.put("relation_type", r.getRelationType());
      dbm.genericInsertIntoTable("ontology_relation", relationInserts);
    }
  }

  public static void main(String[] args)
      throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
    System.out.println(getTissuesFor(TissueClass.Plants.toString()));
    // try {
    // // // ontology =
    // // //
    // //
    // manager.loadOntology(IRI.create("file:/Users/frieda/Downloads/pride-ontology-master/pride_cv.owl"));

  }

  // public static void printHierarchy(OWLReasoner r, OWLClass clazz, int level, Set<OWLClass>
  // visited,
  // Set<String> tissues) throws OWLException {
  // // Only print satisfiable classes to skip Nothing
  // if (!visited.contains(clazz) && r.isSatisfiable(clazz)) {
  // for (int i = 0; i < level; i++)
  // System.out.print("-");
  // visited.add(clazz);
  // String label = labelFor(clazz, r.getRootOntology());
  // System.out.println(label);
  // tissues.add(label);
  // // Find the children and recurse
  // NodeSet<OWLClass> classes = r.getSubClasses(clazz, false);
  // for (Iterator<OWLClass> iter = classes.entities().iterator(); iter.hasNext();) {
  // OWLClass child = iter.next();
  // printHierarchy(r, child, level + 1, visited, tissues);
  // }
  // }
  // }

  // private static String labelFor(OWLClass clazz, OWLOntology o) {
  // LabelExtractor le = new LabelExtractor();
  //
  // for (Iterator<OWLAnnotation> iter =
  // EntitySearcher.getAnnotations(clazz.getIRI(), o).iterator(); iter.hasNext();) {
  // OWLAnnotation anno = iter.next();
  // String result = anno.accept(le);
  // if (result != null) {
  // return result;
  // }
  // }
  // return clazz.getIRI().toString();
  // }
  //
  public static Set<String> getTissuesFor(String category) {
    Set<OntologyEntry> terms =
        dbm.getDescendantsOfOntologyTerm(new ArrayList<String>(Arrays.asList(category)), true);

    Set<String> tissues = new HashSet<String>();
    for (OntologyEntry t : terms)
      tissues.add(t.getLabel());
    return tissues;
  }
  //
  // static class LabelExtractor implements OWLAnnotationObjectVisitorEx<String> {
  // @Override
  // public String visit(OWLAnnotation annotation) {
  // // types.addAll((Collection<? extends String>) annotation.getProperty().getEntityType());
  // if (annotation.getProperty().isLabel()) {
  // OWLLiteral c = (OWLLiteral) annotation.getValue();
  // return c.getLiteral();
  // }
  // return null;
  // }
  // }

  // public void init() {
  // manager = OWLManager.createOWLOntologyManager();
  // try {
  // // ontology =
  // //
  // manager.loadOntology(IRI.create("file:/Users/frieda/Downloads/pride-ontology-master/pride_cv.owl"));
  // ontology = manager.loadOntology(IRI.create("file:/Users/frieda/Downloads/bto.owl"));
  // } catch (OWLOntologyCreationException e1) {
  // // TODO Auto-generated catch block
  // e1.printStackTrace();
  // }
  //
  // df = OWLManager.getOWLDataFactory();
  // OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
  // reasoner = reasonerFactory.createReasoner(ontology);
  // }
}
