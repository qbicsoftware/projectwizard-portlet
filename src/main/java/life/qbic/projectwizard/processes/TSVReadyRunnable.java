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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import com.vaadin.server.StreamResource;

import life.qbic.projectwizard.control.Functions;
import life.qbic.projectwizard.steps.FinishStep;
import life.qbic.xml.manager.XMLParser;
import life.qbic.xml.properties.Property;

public class TSVReadyRunnable implements Runnable {

  FinishStep layout;
  Map<String, List<String>> tables;
  String project;

  public TSVReadyRunnable(FinishStep layout, Map<String, List<String>> tables, String project) {
    this.layout = layout;
    this.tables = tables;
    this.project = project;
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

  private static String getTSVString(List<String> table) {
    XMLParser p = new XMLParser();

    StringBuilder header = new StringBuilder(table.get(0).replace("\tAttributes", ""));
    StringBuilder tsv = new StringBuilder();
    table.remove(0);

    String xmlStart = "<?xml";
    // header
    List<String> factorLabels = new ArrayList<String>();
    for (String row : table) {
      String[] lineSplit = row.split("\t", -1);// doesn't remove trailing whitespaces
      String xml = "";
      for (String cell : lineSplit) {
        if (cell.startsWith(xmlStart))
          xml = cell;
      }
      List<Property> properties = new ArrayList<Property>();
      if (!xml.equals(xmlStart)) {
        try {
          properties = p.getPropertiesFromXML(xml);
        } catch (JAXBException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        for (Property prop : properties) {
          String label = prop.getLabel();
          if (!factorLabels.contains(label)) {
            factorLabels.add(label);
            switch (prop.getType()) {
              case Factor:
                header.append("\tCondition: " + label);
                break;
              case Property:
                header.append("\tProperty: " + label);
                break;
              default:
                break;
            }
          }
        }
      }
    }

    // data
    for (String row : table) {
      String[] lineSplit = row.split("\t", -1);// doesn't remove trailing whitespaces
      String xml = "";
      for (String cell : lineSplit) {
        if (cell.startsWith(xmlStart))
          xml = cell;
      }
      row = row.replace("\t" + xml, "");
      StringBuilder line = new StringBuilder("\n" + row);
      List<Property> props = new ArrayList<Property>();
      if (!xml.equals(xmlStart)) {
        try {
          props = p.getPropertiesFromXML(xml);
        } catch (JAXBException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        Map<Integer, Property> order = new HashMap<Integer, Property>();
        for (Property f : props) {
          String label = f.getLabel();
          order.put(factorLabels.indexOf(label), f);
        }
        for (int i = 0; i < factorLabels.size(); i++) {
          if (order.containsKey(i)) {
            Property f = order.get(i);
            line.append("\t" + f.getValue());
            if (f.hasUnit())
              line.append(f.getUnit());
          } else {
            line.append("\t");
          }
        }
      } else {
        for (int i = 0; i < factorLabels.size() - 1; i++) {
          line.append("\t");
        }
      }
      tsv.append(line);
    }
    return header.append(tsv).toString();
  }

}
