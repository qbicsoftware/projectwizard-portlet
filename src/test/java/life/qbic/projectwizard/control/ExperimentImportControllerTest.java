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

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.expdesign.model.ExperimentalDesignType;


public class ExperimentImportControllerTest {

  @Test
  public void testAddBarcodesToTSV() {
    ExperimentImportController controller = new ExperimentImportController();
    List<ISampleBean> runs = new ArrayList<>();
    List<List<ISampleBean>> levels = new ArrayList<>();

    TSVSampleBean msRun1 = new TSVSampleBean("1", SampleType.Q_MS_RUN, "", new HashMap<>());
    msRun1.addProperty("File", "row1");
    TSVSampleBean msRun2 = new TSVSampleBean("2", SampleType.Q_MS_RUN, "", new HashMap<>());
    msRun2.addProperty("File", "row2");
    runs.add(msRun1);
    runs.add(msRun2);

    levels.add(runs);

    List<String> metaboTSV = new ArrayList<>();
    String header = "QBiC Barcode\tSecondary name\tOrganism ID";
    metaboTSV.add(header);
    metaboTSV.add("\trow1\tsample1");
    metaboTSV.add("\trow2\tsample2");

    String withBarcodes =
        controller.addBarcodesToTSV(metaboTSV, levels, ExperimentalDesignType.Metabolomics_LCMS);
    System.out.println(withBarcodes);
    String[] rowSplit = withBarcodes.split("\n");
    assertTrue("adding barcodes to the metabolomics import keeps number of rows equal",
        rowSplit.length == metaboTSV.size());
    assertTrue("adding barcodes to the metabolomics import keeps number of columns equal",
        rowSplit[1].split("\t").length == header.split("\t").length);
    assertTrue("barcodes are added to the front of metabolomics import format",
        rowSplit[1].split("\t")[0].equals(msRun1.getCode()));

    
    // LIGANDOMICS
    List<String> ligandoTSV = new ArrayList<>();
    header = "Filename\tMS Device\tLCMS Method";
    ligandoTSV.add(header);
    ligandoTSV.add("file1.raw\tDevice\tLCMS Method1");
    ligandoTSV.add("file2.raw\tDevice 2\tLCMS Method1");

    msRun1 = new TSVSampleBean("1", SampleType.Q_MS_RUN, "", new HashMap<>());
    msRun1.addProperty("File", "file1.raw");
    msRun2 = new TSVSampleBean("2", SampleType.Q_MS_RUN, "", new HashMap<>());
    msRun2.addProperty("File", "file2.raw");
    runs.clear();
    levels.clear();
    runs.add(msRun1);
    runs.add(msRun2);

    levels.add(runs);

    withBarcodes = controller.addBarcodesToTSV(ligandoTSV, levels,
        ExperimentalDesignType.MHC_Ligands_Finished);
    System.out.println(withBarcodes);
    rowSplit = withBarcodes.split("\n");
    assertTrue("adding barcodes to the ligandomics import keeps number of rows equal",
        rowSplit.length == metaboTSV.size());
    assertTrue("adding barcodes to the ligandomics import adds one column",
        rowSplit[1].split("\t").length - 1 == header.split("\t").length);
    assertTrue("barcodes are added to the front of ligandomics import format",
        rowSplit[1].split("\t")[0].equals(msRun1.getCode()));

    //PROTEOMICS
    List<String> proteoTSV = new ArrayList<>();
    header = "QBiC Barcode\tFile Name\tSecondary Name\tSample Name";
    proteoTSV.add(header);
    proteoTSV.add("\tfile1.raw\tR01\t1");
    proteoTSV.add("\tfile2.raw\tR02\t2");

    msRun1 = new TSVSampleBean("1", SampleType.Q_MS_RUN, "", new HashMap<>());
    msRun1.addProperty("File", "file1.raw");
    msRun2 = new TSVSampleBean("2", SampleType.Q_MS_RUN, "", new HashMap<>());
    msRun2.addProperty("File", "file2.raw");
    runs.clear();
    levels.clear();
    runs.add(msRun1);
    runs.add(msRun2);

    levels.add(runs);

    withBarcodes = controller.addBarcodesToTSV(proteoTSV, levels,
        ExperimentalDesignType.Proteomics_MassSpectrometry);
    
    System.out.println(withBarcodes);
    rowSplit = withBarcodes.split("\n");
    assertTrue("adding barcodes to the proteomics import keeps number of rows equal",
        rowSplit.length == metaboTSV.size());
    assertTrue("adding barcodes to the proteomics import keeps number of columns equal",
        rowSplit[1].split("\t").length == header.split("\t").length);
    assertTrue("barcodes are added to the front of proteomics import format",
        rowSplit[1].split("\t")[0].equals(msRun1.getCode()));
    
    // withBarcodes = controller.addBarcodesToTSV(proteoTSV, levels,
    // ExperimentalDesignType.Proteomics_MassSpectrometry);

    // Set<String> barcodeColumnNames = new HashSet<>(Arrays.asList("QBiC Code", "QBiC Barcode"));
    //
    // String fileNameHeader = "Filename";
    // StringBuilder builder = new StringBuilder(5000);
    // switch (designType) {
    // case Standard:
    // int anltIDPos = -1;
    // int extIDPos = -1;
    // for (String line : tsv) {
    // String[] splt = line.split("\t");
    // if (anltIDPos < 0) {
    // anltIDPos = Arrays.asList(splt).indexOf("Analyte ID");
    // extIDPos = Arrays.asList(splt).indexOf("Extract ID");
    // builder.append("QBiC Code\t" + line + "\n");
    // } else {
    // String extID = splt[anltIDPos];
    // if (extID == null || extID.isEmpty())
    // extID = splt[extIDPos];
    // String code = uniqueCodeToBarcode.get(extID);
    // builder.append(code + "\t" + line + "\n");
    // }
    // }
    // break;
    // case Proteomics_MassSpectrometry:
    // fileNameHeader = "File Name";
    // case Metabolomics_LCMS:
    // fileNameHeader = "Secondary name";
    // case MHC_Ligands_Finished:
    // Map<String, String> fileNameToBarcode = new HashMap<String, String>();
    // for (List<ISampleBean> samples : levels) {
    // for (ISampleBean s : samples) {
    // if (s.getType().equals(SampleType.Q_MS_RUN)) {
    // Map<String, Object> props = s.getMetadata();
    // fileNameToBarcode.put(props.get("File").toString(), s.getCode());
    // props.remove("File");
    // }
    // }
    // }
    // int filePos = -1;
    // boolean colExists = false;
    // for (String line : tsv) {
    // String[] splt = line.split("\t");
    // if (filePos < 0) {
    // colExists = barcodeColumnNames.contains(splt[0]);
    // filePos = Arrays.asList(splt).indexOf(fileNameHeader);
    // if (colExists) {
    // builder.append(line + "\n");
    // } else {
    // builder.append("QBiC Code\t" + line + "\n");
    // }
    // } else {
    // String file = splt[filePos];
    // String code = fileNameToBarcode.get(file);
    // if (colExists) {
    // builder.append(code + line + "\n");
    // } else {
    // builder.append(code + "\t" + line + "\n");
    // }
    // }
    // }
    // default:
    // break;
    // }
    // return builder.toString();
  }

}
