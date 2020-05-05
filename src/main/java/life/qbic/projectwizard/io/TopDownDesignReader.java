package life.qbic.projectwizard.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.portal.model.MSRunCollection;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.portal.model.SamplePreparationRun;
import life.qbic.portal.model.PreliminaryOpenbisExperiment;
import life.qbic.xml.study.TechnologyType;

public class TopDownDesignReader extends MSDesignReader {

  private static final Logger logger = LogManager.getLogger(TopDownDesignReader.class);

  public TopDownDesignReader() {
    this.mandatoryColumns = new ArrayList<String>(Arrays.asList("Preparation Date", "MS Run Date",
        "File Name", "MS Device", "Protein Barcode", "Fractionation Type", "Fraction Name"));
    this.mandatoryFilled = new ArrayList<String>(Arrays.asList("MS Device", "Preparation Date",
        "MS Run Date", "File Name", "Protein Barcode", "Sample Cleanup", "LC Column"));
    this.optionalCols = new ArrayList<String>(Arrays.asList("Enrichment", "LCMS Method", "Comment",
        "Labeling Type", "Label", "Middle-Down"));

    headersToTypeCodePerSampletype = new HashMap<>();
    headersToTypeCodePerSampletype.put(SampleType.Q_TEST_SAMPLE, new HashMap<>());
    // headersToTypeCodePerSampletype.put("SampleType.Q_MS_RUN", msRunMetadata);
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
  public List<ISampleBean> readSamples(File file, boolean parseGraph) throws IOException {
    super.initReader();

    BufferedReader reader = new BufferedReader(new FileReader(file));
    ArrayList<String[]> data = new ArrayList<String[]>();
    String next;
    int i = 0;
    // isPilot = false;
    while ((next = reader.readLine()) != null) {
      i++;
      next = removeUTF8BOM(next);
      tsvByRows.add(next);
      String[] nextLine = next.split("\t", -1);// this is needed for trailing tabs
      if (data.isEmpty() || nextLine.length == data.get(0).length) {
        data.add(nextLine);
      } else {
        error = "Wrong number of columns in row " + i;
        reader.close();
        return null;
      }
    }
    reader.close();

    String[] header = data.get(0);
    data.remove(0);
    // find out where the mandatory and other metadata data is
    Map<String, Integer> headerMapping = new HashMap<String, Integer>();
    List<Integer> meta = new ArrayList<Integer>();
    List<Integer> factors = new ArrayList<Integer>();
    List<Integer> loci = new ArrayList<Integer>();
    int numOfLevels = 5;

    ArrayList<String> found = new ArrayList<String>(Arrays.asList(header));
    mandatoryColumns.addAll(mandatoryFilled);
    for (String col : mandatoryColumns) {
      if (!found.contains(col)) {
        error = "Mandatory column " + col + " not found.";
        return null;
      }
    }
    for (i = 0; i < header.length; i++) {
      int position = mandatoryColumns.indexOf(header[i]);
      if (position == -1)
        position = optionalCols.indexOf(header[i]);
      if (position > -1) {
        headerMapping.put(header[i], i);
        meta.add(i);
      } else {
        meta.add(i);
      }
    }
    // create samples
    List<ISampleBean> beans = new ArrayList<>();
    List<List<ISampleBean>> order = new ArrayList<>();
    Map<String, TSVSampleBean> analyteToSample = new HashMap<>();
    Map<SamplePreparationRun, Map<String, Object>> expIDToFracExp = new HashMap<>();
    Map<MSRunCollection, Map<String, Object>> msIDToMSExp = new HashMap<>();

    int rowID = 0;
    int sampleID = 0;
    for (String[] row : data) {
      rowID++;
      boolean special = false;
      if (!special) {
        for (String col : mandatoryFilled) {
          if (row[headerMapping.get(col)].isEmpty()) {
            error = col + " is a mandatory field, but it is not set for row " + rowID + "!";
            return null;
          }
        }
        // mandatory fields that need to be filled to identify sources and samples
        String prepDate = row[headerMapping.get("Preparation Date")];
        // String ligandExtrID = sourceID + "-" + tissue + "-" + prepDate + "-" + antibody;
        String msRunDate = row[headerMapping.get("MS Run Date")];
        String msDevice = row[headerMapping.get("MS Device")];
        String lcCol = row[headerMapping.get("LC Column")];
        String fName = row[headerMapping.get("File Name")];

        String proteinParent = row[headerMapping.get("Protein Barcode")];
        String cleanup = row[headerMapping.get("Sample Cleanup")];
        String comment = "";
        if (headerMapping.containsKey("Comment")) {
          comment = row[headerMapping.get("Comment")];
        }
        String fracType = "";
        String fracName = "";
        
        fillParsedCategoriesToValuesForRow(headerMapping, row);
        
        if (headerMapping.containsKey("Fractionation Type")) {
          fracType = row[headerMapping.get("Fractionation Type")];
          fracName = row[headerMapping.get("Fraction Name")];
        }

        while (order.size() < numOfLevels) {
          order.add(new ArrayList<ISampleBean>());
        }
        // always one new measurement per row
        // chromatography options are stored on the MS level
        // if there is fractionation or enrichment, a new protein experiment and samples are needed
        // this is the case if fractionation type is not empty
        // the number of fractions is taken from the fraction names as well as the source barcode
        // (protein barcode)
        // so all fractions from the same protein sample end up in the same fractionation experiment
        // IF the fractionation/enrichment type is the same
        SamplePreparationRun fracRun = null;
        if (!fracName.isEmpty()) {
          String fracID = proteinParent + "_" + fracType + "_" + fracName;
          fracRun = new SamplePreparationRun(proteinParent, prepDate, fracType, cleanup);
          TSVSampleBean fracSample = analyteToSample.get(fracID);
          if (fracSample == null) {
            sampleID++;
            fracSample = new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_TEST_SAMPLE,
                fracID, fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
            fracSample.addParentID(proteinParent);

            proteinParent = Integer.toString(sampleID);

            fracSample.addProperty("Q_EXTERNALDB_ID", fracID);
            fracSample.addProperty("Q_SAMPLE_TYPE", "PROTEINS");

            order.get(0).add(fracSample);
            analyteToSample.put(fracID, fracSample);

            fracSample.setExperiment(Integer.toString(fracRun.hashCode()));
            Map<String, Object> fracExperimentMetadata = expIDToFracExp.get(fracRun);
            if (fracExperimentMetadata == null) {
              Map<String, Object> metadata = new HashMap<>();
              addFractionationOrEnrichmentToMetadata(metadata, fracType);
              // metadata.put("Q_FRACTIONATION_TYPE", fracType);
              expIDToFracExp.put(fracRun, parsePrepExperimentData(row, headerMapping, metadata));
            } else
              expIDToFracExp.put(fracRun,
                  parsePrepExperimentData(row, headerMapping, fracExperimentMetadata));
          } else {
            proteinParent = fracSample.getCode();
          }
        }
        sampleID++;
        TSVSampleBean msRun = new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_MS_RUN, "",
            fillMetadata(header, row, meta, factors, loci, SampleType.Q_MS_RUN));
        MSRunCollection msRuns = new MSRunCollection(fracRun, msRunDate, msDevice, lcCol);
        msRun.setExperiment(Integer.toString(msRuns.hashCode()));
        Map<String, Object> msExperiment = msIDToMSExp.get(msRuns);
        if (msExperiment == null)
          msIDToMSExp.put(msRuns, parseMSExperimentData(row, headerMapping, new HashMap<>()));
        msRun.addParentID(proteinParent);
        msRun.addProperty("File", fName);
        if(!comment.isEmpty()) {
        msRun.addProperty("Q_ADDITIONAL_INFO", comment);
        }

        order.get(1).add(msRun);
      }
    }
    experimentInfos = new HashMap<>();

    // fractionation experiments
    List<PreliminaryOpenbisExperiment> fracExperiments = new ArrayList<>();
    for (SamplePreparationRun prepRun : expIDToFracExp.keySet()) {
      Map<String, Object> map = expIDToFracExp.get(prepRun);
      // map.put("Code", Integer.toString(prepRun.hashCode()));// used to match samples to their
      // experiments later
      // msExperiments.add(map);
      PreliminaryOpenbisExperiment e =
          new PreliminaryOpenbisExperiment(ExperimentType.Q_SAMPLE_PREPARATION, map);
      e.setCode(Integer.toString(prepRun.hashCode()));
      fracExperiments.add(e);
    }
    experimentInfos.put(ExperimentType.Q_SAMPLE_PREPARATION, fracExperiments);

    // MS experiments
    List<PreliminaryOpenbisExperiment> msExperiments = new ArrayList<>();
    for (MSRunCollection runCollection : msIDToMSExp.keySet()) {
      Map<String, Object> map = msIDToMSExp.get(runCollection);
      // map.put("Code", Integer.toString(runCollection.hashCode()));// used to match samples to
      // their
      // experiments later
      // msExperiments.add(map);
      PreliminaryOpenbisExperiment e =
          new PreliminaryOpenbisExperiment(ExperimentType.Q_MS_MEASUREMENT, map);
      e.setCode(Integer.toString(runCollection.hashCode()));
      msExperiments.add(e);
    }
    experimentInfos.put(ExperimentType.Q_MS_MEASUREMENT, msExperiments);
    for (List<ISampleBean> level : order)
      beans.addAll(level);
    return beans;
  }

  @Override
  public Set<String> getAnalyteSet() {
    return new HashSet<String>(Arrays.asList("PROTEINS"));
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

}
