package isatab;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ISASection {

  private String sectionName;
  private Map<String, Object> entries;

  public ISASection(String name) {
    this.sectionName = name;
    entries = new LinkedHashMap<String, Object>();
  }

  public void addEntry(String name, Object metadata) {
    entries.put(name, metadata);
  }

  public Object getEntry(String name) {
    return entries.get(name);
  }

  public List<String> getTabSeparatedSectionTableRows() throws Exception {
    List<String> res = new ArrayList<String>();
    res.add(sectionName);
    for (String name : entries.keySet()) {
      String line = name + "\t";
      Object value = entries.get(name);
      if (value instanceof String)
        line += value;
      else {
        throw new Exception("test");
      }
      res.add(line);
    }
    return res;
  }

}
