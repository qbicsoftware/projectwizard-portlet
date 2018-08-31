/*******************************************************************************
 * QBiC Project Wizard enables users to create hierarchical experiments including different study
 * conditions using factorial design. Copyright (C) "2016" Andreas Friedrich
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package life.qbic.projectwizard.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import life.qbic.datamodel.identifiers.SampleCodeFunctions;

public class SampleCounter {

  private int entityID;
  // private int barcodeID;
  private int expID;
  private String barcode;
  private String project;
  Logger logger = LogManager.getLogger(SampleCounter.class);
  private Set<String> knownAtypical = new HashSet<String>();

  public SampleCounter(List<Sample> samples) {
    this(samples.get(0).getCode().substring(0, 5));
    for (Sample s : samples)
      increment(s);
  }

  public SampleCounter(String project) {
    entityID = 0;
    expID = 0;
    barcode = "";
    // barcodeID = 1;
    this.project = project;
  }

  // TODO later updates (after initialization)
  public void increment(Sample s) {
    String code = s.getCode();
    String experiment = s.getExperimentIdentifierOrNull();
    try {
      String exp = experiment.split(project + "E")[1];
      int expNum = Integer.parseInt(exp);
      if (expNum > expID)
        expID = expNum;
    } catch (Exception e) {
      if (!knownAtypical.contains(experiment)) {
        knownAtypical.add(experiment);
        logger.warn("While counting existing experiments in project " + project
            + " unfamiliar experiment identifier " + experiment + " was found.");
      }
    }
    if (SampleCodeFunctions.isQbicBarcode(code)) {
      // previously registered samples...
      List<String> specialCases = new ArrayList<String>(Arrays.asList("QMSHS001X3", "QMSHS002XB",
          "QMSHS003XJ", "QMSHS004XR", "QMSHS005X1", "QMSHS006X9"));
      if (code.startsWith("QMSHS999W"))
        barcode = "QMSHS006X9";
      if (!specialCases.contains(code)) {
        if (SampleCodeFunctions.compareSampleCodes(code, barcode) > 0)
          barcode = code;
      }
    } else if (s.getSampleTypeCode().equals(("Q_BIOLOGICAL_ENTITY"))) {
      int num = Integer.parseInt(s.getCode().split("-")[1]);
      if (num >= entityID)
        entityID = num;
    }
  }

  public String getNewExperiment() {
    expID++;
    return project + "E" + expID;
  }

  public String getNewEntity() {
    entityID++;
    return project + "ENTITY-" + Integer.toString(entityID);
  }

  public String getNewBarcode() {
    if (barcode == null || barcode.isEmpty()) {
      barcode = project + "001A";
      barcode = barcode + SampleCodeFunctions.checksum(barcode);
    }
    barcode = SampleCodeFunctions.incrementSampleCode(barcode);
    return barcode;
  }

}
