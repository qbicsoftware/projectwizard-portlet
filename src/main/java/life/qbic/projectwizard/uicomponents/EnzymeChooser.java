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
package life.qbic.projectwizard.uicomponents;

import java.util.List;

import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.VerticalLayout;

import life.qbic.portal.Styles;

public class EnzymeChooser extends VerticalLayout {

  /**
   * 
   */
  private static final long serialVersionUID = 284773693595639437L;
  /**
   * 
   */
  private ComboBox chooser;

  public EnzymeChooser(List<String> options) {
    chooser = new ComboBox();
    chooser.addItems(options);
    chooser.setFilteringMode(FilteringMode.CONTAINS);
//    chooser.setNullSelectionAllowed(false);
    chooser.setStyleName(Styles.boxTheme);
    addComponent(chooser);
    setSpacing(true);
  }

  public boolean chooserSet() {
    return chooser.getValue() != null;
  }

  public boolean isSet() {
    return chooser.getItemIds().contains(chooser.getValue());
  }

  public String getEnzyme() {
    if (chooser.getValue() != null)
      return chooser.getValue().toString();
    else
      return null;
  }

  public void reset() {
    chooser.setValue(chooser.getNullSelectionItemId());
  }
}
