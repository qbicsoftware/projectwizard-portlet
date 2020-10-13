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
package life.qbic.projectwizard.processes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.vaadin.server.StreamResource;

import life.qbic.projectwizard.control.Functions;
import life.qbic.projectwizard.steps.FinishStep;
import life.qbic.xml.properties.Property;

public class TSVReadyRunnable implements Runnable {

  private FinishStep layout;
  private Map<String, List<String>> tables;
  private String project;
  private Set<String> factors;
  private Map<Pair<String, String>, Property> factorsForLabelsAndSamples;
  
  private static final Logger logger = LogManager.getLogger(TSVReadyRunnable.class);


  public TSVReadyRunnable(FinishStep layout, Map<String, List<String>> tables, String project,
      Set<String> factors, Map<Pair<String, String>, Property> factorsForLabelsAndSamples) {
    this.layout = layout;
    this.tables = tables;
    this.project = project;
    this.factors = factors;
    this.factorsForLabelsAndSamples = factorsForLabelsAndSamples;
  }

  @Override
  public void run() {
    List<StreamResource> streams = new ArrayList<StreamResource>();
    String ext = "tsv";
    streams.add(Functions.getFileStream(getTSVString(tables.get("Q_BIOLOGICAL_ENTITY")),
        project + "_sample_sources", ext));
    streams.add(Functions.getFileStream(getTSVString(tables.get("Q_BIOLOGICAL_SAMPLE")),
        project + "_sample_extracts", ext));
    if (tables.containsKey("Q_TEST_SAMPLE"))
      streams.add(Functions.getFileStream(getTSVString(tables.get("Q_TEST_SAMPLE")),
          project + "_sample_preparations", ext));
    layout.armButtons(streams);
  }

  private String getTSVString(List<String> table) {
    StringBuilder header = new StringBuilder(table.get(0).replace("\tAttributes", ""));
    StringBuilder tsv = new StringBuilder();
    table.remove(0);

    String xmlStart = "<?xml";
    List<String> labels = new ArrayList<>(factors);
    // header
    
    for(String l : labels) {
    header.append("\tCondition: " + l);
    }

    // data
    for (String row : table) {
      String[] lineSplit = row.split("\t", -1);// doesn't remove trailing whitespaces
      String xml = "";
      String code = lineSplit[0];
      for (String cell : lineSplit) {
        if (cell.startsWith(xmlStart))
          xml = cell;
      }
      row = row.replace("\t" + xml, "");
      StringBuilder line = new StringBuilder("\n" + row);
      
      for(String label : labels) {
        Property f = factorsForLabelsAndSamples.get(new ImmutablePair<>(label, code));
        if(f!=null) {
          line.append("\t" + f.getValue());
          if (f.hasUnit())
            line.append(f.getUnit());
        } else {
          line.append("\t");
        }
      }
      
      tsv.append(line);
    }
    return header.append(tsv).toString();
}

}
