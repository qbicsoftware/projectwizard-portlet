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
package life.qbic.projectwizard.model;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import life.qbic.projectwizard.uicomponents.LigandExtractPanel;

public class MHCLigandExtractionProtocol {

  private Map<String, String[]> antibodyToMHCClass = new HashMap<String, String[]>() {
    /**
     * 
     */
    private static final long serialVersionUID = 1442236401110900500L;

    {
      put("MAE", new String[] {"MHC_CLASS_I", "MHC_CLASS_II"});
      put("L243", new String[] {"MHC_CLASS_II"});
      put("L243_TUE39", new String[] {"MHC_CLASS_II"});
      put("BB7.2", new String[] {"MHC_CLASS_I"});
      put("B1.23.2", new String[] {"MHC_CLASS_I"});
      put("TUE39", new String[] {"MHC_CLASS_II"});
      put("W6-32", new String[] {"MHC_CLASS_I"});
      put("GAPA3", new String[] {"MHC_CLASS_I"});
      // Mouse MHC (H-2)
      put("B22.249", new String[] {"MHC_CLASS_I"});// H2-Db
      put("Y3", new String[] {"MHC_CLASS_I"});// H2-Kb
      put("M5.144.15.2", new String[] {"MHC_CLASS_II"});// H2-Ab
    };
  };

  private String inputSampleMass;
  private List<String> antibodies;
  private List<String> antibodyMasses;
  private Date preparationDate;

  Logger logger = LogManager.getLogger(MHCLigandExtractionProtocol.class);

  public MHCLigandExtractionProtocol(String inputSampleMass, Date preparationDate,
      List<String> antibodies, List<String> antibodyMasses) {
    this.inputSampleMass = inputSampleMass;
    this.antibodies = antibodies;
    this.antibodyMasses = antibodyMasses;
    this.preparationDate = preparationDate;
  }

  public String[] getMHCClass(String antibody) {
    if (antibodyToMHCClass.containsKey(antibody))
      return antibodyToMHCClass.get(antibody);
    else {
      logger.error(antibody + " is an unknown antibody. Returning 'null' as MHC Class");
      return null;
    }
  }

  public Date getPrepDate() {
    return preparationDate;
  }

  public String getInputSampleMass() {
    return inputSampleMass;
  }

  public List<String> getAntibodies() {
    return antibodies;
  }

  public List<String> getAntiBodyMasses() {
    return antibodyMasses;
  }

}
