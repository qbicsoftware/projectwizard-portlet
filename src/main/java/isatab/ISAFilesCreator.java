package isatab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ISAFilesCreator {

  private final List<String> sectionOrder = new ArrayList<String>(
      Arrays.asList("ONTOLOGY SOURCE REFERENCE", "INVESTIGATION", "INVESTIGATION PUBLICATIONS",
          "INVESTIGATION CONTACTS", "STUDY", "STUDY DESIGN DESCRIPTORS", "STUDY PUBLICATIONS",
          "STUDY FACTORS", "STUDY ASSAYS", "STUDY PROTOCOLS", "STUDY CONTACTS"));

  public static void main(String[] args) {
    List<ISASection> investigationSections = new ArrayList<ISASection>();
    ISASection a = new ISASection("ONTOLOGY SOURCE REFERENCE");
//    String 
    a.addEntry("Term Source Name","");
//        "Term Source File", "Term Source Version", "Term Source Description"));
    ISASection b = new ISASection("INVESTIGATION");
    ISASection c = new ISASection("INVESTIGATION PUBLICATIONS");
    ISASection d = new ISASection("INVESTIGATION CONTACTS");
    ISASection e = new ISASection("STUDY");
    ISASection f = new ISASection("STUDY DESIGN DESCRIPTORS");
    ISASection g = new ISASection("STUDY PUBLICATIONS");
    ISASection h = new ISASection("STUDY FACTORS");
    ISASection i = new ISASection("STUDY ASSAYS");
    ISASection j = new ISASection("STUDY PROTOCOLS");
    ISASection k = new ISASection("STUDY CONTACTS");
  }

  private String getAttributeRowFromList(List<Object> x, int i) {
    return "";
  }

}
