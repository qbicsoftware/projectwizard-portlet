package isatab;

import java.util.HashMap;
import java.util.Map;

public class Ontology {

  private Map<Integer, String> ISAOrder;
  private String sourceName;
  private String sourceFile;
  private String sourceVersion;
  private String sourceDescription;

  public Ontology(String sourceName, String sourceFile, String sourceVersion,
      String sourceDescription) {
    this.sourceName = sourceName;
    this.sourceFile = sourceFile;
    this.sourceVersion = sourceVersion;
    this.sourceDescription = sourceDescription;
    ISAOrder = new HashMap<Integer, String>();
    ISAOrder.put(0, sourceName);
    ISAOrder.put(1, sourceFile);
    ISAOrder.put(2, sourceVersion);
    ISAOrder.put(3, sourceDescription);
  }

  public String getISAValueByPosition(int index) {
    return ISAOrder.get(index);
  }

  public String getSourceName() {
    return sourceName;
  }

  public String getSourceFile() {
    return sourceFile;
  }

  public String getSourceVersion() {
    return sourceVersion;
  }

  public String getSourceDescription() {
    return sourceDescription;
  }

}
