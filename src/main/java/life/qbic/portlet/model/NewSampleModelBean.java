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
package life.qbic.portlet.model;

/**
 * Bean item representing identifier, secondary name and sample type of samples to visualize in a
 * table etc.
 * 
 * @author Andreas Friedrich
 *
 */
public class NewSampleModelBean {

  private String Code;
  private String Secondary_Name;

  public NewSampleModelBean(String code, String secondaryName, String type) {
    this.Code = code;
    this.Secondary_Name = secondaryName;
  }

  public String getCode() {
    return Code;
  }

  public void setCode(String code) {
    this.Code = code;
  }

  public String getSecondary_Name() {
    return Secondary_Name;
  }

  public void setSecondary_Name(String secondaryName) {
    this.Secondary_Name = secondaryName;
  }


}
