package life.qbic.projectwizard.io;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.expdesign.io.Command;
import life.qbic.expdesign.io.IExperimentalDesignReader;
import life.qbic.xml.properties.Unit;
import life.qbic.xml.study.TechnologyType;

public abstract class MSDesignReader implements IExperimentalDesignReader {

  protected List<String> mandatoryColumns;
  protected List<String> mandatoryFilled;
  protected List<String> optionalCols;
  protected Map<SampleType, Map<String, String>> headersToTypeCodePerSampletype;
  protected Map<String, Set<String>> parsedCategoriesToValues;

  protected String error;
  protected Map<ExperimentType, List<PreliminaryOpenbisExperiment>> experimentInfos;
  protected List<String> tsvByRows;
  private static final Logger logger = LogManager.getLogger(MSDesignReader.class);
  private Map<String, Command> parsers;

  public Map<ExperimentType, List<PreliminaryOpenbisExperiment>> getExperimentInfos() {
    return experimentInfos;
  }

  public MSDesignReader() {
    parsers = new HashMap<String, Command>();
    parsers.put("Q_PREPARATION_DATE", new Command() {
      @Override
      public String parse(String value) {
        return parseDate(value);
      }
    });
    parsers.put("Q_MEASUREMENT_FINISH_DATE", new Command() {
      @Override
      public String parse(String value) {
        return parseDate(value);
      }
    });
    parsers.put("Q_MS_LCMS_METHOD", new Command() {
      @Override
      public String parse(String value) {
        return parseLCMSMethod(value);
      }
    });
  }

  protected void fillParsedCategoriesToValuesForRow(Map<String, Integer> headerMapping,
      String[] row) {
    addValueForCategory(headerMapping, row, "MS Device");
    addValueForCategory(headerMapping, row, "LC Column");
    addValueForCategory(headerMapping, row, "Sample Cleanup");
    addValueForCategory(headerMapping, row, "Fractionation Type");
    addValueForCategory(headerMapping, row, "Enrichment");
    addValueForCategory(headerMapping, row, "Labeling Type");
    addValueForCategory(headerMapping, row, "Labeling/Derivitisation");
  }

  private void addValueForCategory(Map<String, Integer> headerMapping, String[] row, String cat) {
    if (headerMapping.containsKey(cat)) {
      String val = row[headerMapping.get(cat)];
      if (val != null && !val.isEmpty()) {
        if (parsedCategoriesToValues.containsKey(cat)) {
          parsedCategoriesToValues.get(cat).add(val);
        } else {
          Set<String> set = new HashSet<String>();
          set.add(val);
          parsedCategoriesToValues.put(cat, set);
        }
      }
    }
  }

  public static final String UTF8_BOM = "\uFEFF";

  protected static String removeUTF8BOM(String s) {
    if (s.startsWith(UTF8_BOM)) {
      s = s.substring(1);
    }
    return s;
  }

  protected boolean containsDeglycChem(String enyzmeString) {
    Set<String> chems = parseDigestionEnzymes(enyzmeString);
    for (String chem : chems) {
      if (deglycChems.contains(chem.toLowerCase())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Reads in a TSV file containing openBIS samples that should be registered. Returns a List of
   * TSVSampleBeans containing all the necessary information to register each sample with its meta
   * information to openBIS, given that the types and parents exist.
   * 
   * @param file
   * @return ArrayList of TSVSampleBeans
   * @throws IOException
   */
  public abstract List<ISampleBean> readSamples(File file, boolean parseGraph) throws IOException;

  // TODO
  protected void addFractionationOrEnrichmentToMetadata(Map<String, Object> metadata,
      String fracType) {
    // if (!fracType.isEmpty()) {
    // metadata.put("Fractionation_Enrichment_Placeholder", fracType);
    // }
  }

  protected Set<String> parseDigestionEnzymes(String csEnzymes) {
    Set<String> res = new HashSet<>();
    for (String e : csEnzymes.split(";")) {
      res.add(e.trim());
    }
    return res;
  }

  protected Map<String, Object> parsePrepExperimentData(String[] row,
      Map<String, Integer> headerMapping, Map<String, Object> metadata) {
    if (headerMapping.containsKey("Enrichment")) {
      String val = row[headerMapping.get("Enrichment")];
      if (!val.isEmpty()) {
        metadata.put("Q_MS_ENRICHMENT_METHOD_DETAILED", val);
      }
    }
    String val = row[headerMapping.get("Fractionation Type")];
    if (!val.isEmpty()) {
      metadata.put("Q_MS_FRACTIONATION_METHOD", val);
    }
    String prepDate = row[headerMapping.get("Preparation Date")];
    if (!prepDate.isEmpty()) {
      metadata.put("Q_PREPARATION_DATE", parseDate(prepDate));
    }
    if (headerMapping.containsKey("Labeling Type")) {
      String label = row[headerMapping.get("Labeling Type")];
      if (!label.isEmpty()) {
        metadata.put("Q_LABELING_METHOD", label);
      }
    }
    if (headerMapping.containsKey("Labeling/Derivitisation")) {
      String label = row[headerMapping.get("Labeling/Derivitisation")];
      if (!label.isEmpty()) {
        metadata.put("Q_LABELING_METHOD", label);
      }
    }
    if (headerMapping.containsKey("Sample Cleanup")) {
      val = row[headerMapping.get("Sample Cleanup")];
      if (!val.isEmpty()) {
        metadata.put("Q_MS_PURIFICATION_METHOD", val);
      }
    }
    if (headerMapping.get("Digestion") != null) {
      String enzymes = row[headerMapping.get("Digestion")];
      if (!enzymes.isEmpty()) {
        metadata.put("Q_DIGESTION_METHOD", new ArrayList<>(parseDigestionEnzymes(enzymes)));
      }
    }
    return metadata;
  }

  protected Map<String, Object> parseMSExperimentData(String[] row,
      Map<String, Integer> headerMapping, HashMap<String, Object> metadata) {
    Map<String, String> designMap = new HashMap<String, String>();
    designMap.put("MS Run Date", "Q_MEASUREMENT_FINISH_DATE");
    designMap.put("Share", "Q_EXTRACT_SHARE");
    designMap.put("MS Device", "Q_MS_DEVICE");
    designMap.put("LCMS Method", "Q_MS_LCMS_METHOD_INFO");
    // designMap.put("Comment", "Q_ADDITIONAL_INFO");
    designMap.put("LC Column", "Q_CHROMATOGRAPHY_TYPE");
    designMap.put("Middle-Down", "Q_MS_MIDDLE_DOWN_INFORMATION");
    metadata.put("Q_CURRENT_STATUS", "FINISHED");
    for (String col : designMap.keySet()) {
      String val = "";
      String openbisType = designMap.get(col);
      if (headerMapping.containsKey(col)) {
        val = row[headerMapping.get(col)];
        if (parsers.containsKey(openbisType)) {
          val = parsers.get(openbisType).parse(val);
        }
      }
      if (val != null && !val.isEmpty()) {
        metadata.put(openbisType, val);
      }
    }
    metadata.put("Q_MS_LCMS_METHOD", "SPECIAL_METHOD");
    return metadata;
  }

  protected String parseLCMSMethod(String value) {
    return value;
  }

  protected Object parseBoolean(String value) {
    return value.equals("1");
  }

  protected String parseDate(String value) {
    SimpleDateFormat parser = new SimpleDateFormat("yyMMdd");
    try {
      Date date = parser.parse(value);
      SimpleDateFormat dateformat = new SimpleDateFormat("dd-MM-yyyy");
      if (date != null) {
        return dateformat.format(date);
      }
    } catch (IllegalArgumentException e) {
      logger.warn("No valid preparation date input. Not setting Date for this experiment.");
    } catch (ParseException e) {
      logger.warn("No valid preparation date input. Not setting Date for this experiment.");
    }
    return "";
  }

  protected HashMap<String, Object> fillMetadata(String[] header, String[] data, List<Integer> meta,
      List<Integer> factors, List<Integer> loci, SampleType type) {
    Map<String, String> headersToOpenbisCode = headersToTypeCodePerSampletype.get(type);
    HashMap<String, Object> res = new HashMap<>();
    if (headersToOpenbisCode != null) {
      for (int i : meta) {
        String label = header[i];
        if (!data[i].isEmpty() && headersToOpenbisCode.containsKey(label)) {
          String propertyCode = headersToOpenbisCode.get(label);
          Object val = data[i];
          if (parsers.containsKey(propertyCode))
            val = parsers.get(propertyCode).parse(data[i]);
          res.put(propertyCode, val);
        }
      }
    }
    if (factors.size() > 0) {
      String fRes = "";
      for (int i : factors) {
        if (!data[i].isEmpty()) {
          String values = unitCheck(data[i]);
          fRes += parseXMLPartLabel(header[i]) + ": " + values + ";";
        }
      }
      // remove trailing ";"
      fRes = fRes.substring(0, Math.max(1, fRes.length()) - 1);
      res.put("XML_FACTORS", fRes);
    }
    if (loci.size() > 0) {
      String lRes = "";
      for (int i : loci) {
        if (!data[i].isEmpty()) {
          lRes += parseXMLPartLabel(header[i]) + ": " + data[i] + ";";
        }
      }
      // remove trailing ";"
      lRes = lRes.substring(0, Math.max(1, lRes.length()) - 1);
      res.put("XML_LOCI", lRes);
    }
    return res;
  }

  protected String unitCheck(String string) {
    String[] split = string.split(":");
    if (split.length > 2)
      return string.replace(":", " -");
    if (split.length == 2) {
      String unit = split[1].trim();
      for (Unit u : Unit.values()) {
        if (u.getValue().equals(unit))
          return string;
      }
      return string.replace(":", " -");
    }
    return string;
  }

  private String parseXMLPartLabel(String colName) {
    return colName.split(": ")[1];
  }

  public String getError() {
    if (error != null)
      logger.error(error);
    else
      logger.info("Parsing of experimental design successful.");
    return error;
  }

  @Override
  public abstract Set<String> getAnalyteSet();

  @Override
  public List<String> getTSVByRows() {
    return tsvByRows;
  }

  @Override
  // TODO
  public int countEntities(File file) throws IOException {
    return 0;
  }

  @Override
  public List<TechnologyType> getTechnologyTypes() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<String, List<String>> getParsedCategoriesToValues(List<String> header) {
    Map<String, List<String>> res = new HashMap<>();
    for (String cat : header) {
      if (parsedCategoriesToValues.containsKey(cat)) {
        res.put(cat, new ArrayList<>(parsedCategoriesToValues.get(cat)));
      } else {
        logger.warn(cat + " not found");
      }
    }
    return res;
  }

  public void initReader() {
    error = null;
    parsedCategoriesToValues = new HashMap<>();
    tsvByRows = new ArrayList<String>();
  }
}
