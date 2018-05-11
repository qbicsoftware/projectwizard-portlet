/*******************************************************************************
 * QBiC Project Wizard enables users to create hierarchical experiments including different study conditions using factorial design.
 * Copyright (C) "2016"  Andreas Friedrich
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package life.qbic.projectwizard.model;

public class MCCPatient {

  private String ID;

  private String Treatment;
  private String Timepoint;

  public MCCPatient(String ID, String Treatment, String Timepoint) {
    this.ID = ID;
    this.Timepoint = Timepoint;
    this.Treatment = Treatment;
  }

  public String getTreatment() {
    return Treatment;
  }

  public void setTimepoint(String tp) {
    Timepoint = tp;
  }

  public String getTimepoint() {
    return Timepoint;
  }

  public void setTreatment(String treat) {
    Treatment = treat;
  }

  public String getID() {
    return ID;
  }

  public void setID(String iD) {
    ID = iD;
  }

}
