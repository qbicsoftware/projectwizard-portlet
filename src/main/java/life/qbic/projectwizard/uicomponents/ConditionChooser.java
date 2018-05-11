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

import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import life.qbic.portal.Styles;

/**
 * Composite UI component to choose a single condition of an experiment
 * 
 * @author Andreas Friedrich
 * 
 */
public class ConditionChooser extends VerticalLayout {

  private static final long serialVersionUID = 7196121933289471757L;
  private ComboBox chooser;
  private String other;
  private String special;
  private boolean isSpecial;
  private TextField freetext;

  /**
   * Creates a new condition chooser component
   * 
   * @param options List of different possible conditions
   * @param other Name of the "other" condition, which when selected will enable an input field for
   *        free text
   * @param special Name of a "special" condition like species for the entity input, which when
   *        selected will disable the normal species input because there is more than one instance
   * @param nullSelectionAllowed true, if the conditions may be empty
   */
  public ConditionChooser(List<String> options, String other, String special,
      boolean nullSelectionAllowed) {
    isSpecial = false;
    this.other = other;
    this.special = special;
    chooser = new ComboBox("Experimental Variable", options);
    chooser.setStyleName(Styles.boxTheme);
    chooser.setImmediate(true);
    chooser.setNullSelectionAllowed(nullSelectionAllowed);
    addComponent(chooser);
  }

  public void addListener(ValueChangeListener l) {
    this.chooser.addValueChangeListener(l);
  }

  public boolean factorIsSpecial() {
    return isSpecial;
  }

  public void changed() {
    String val = "";
    if (chooser.getValue() != null) {
      val = chooser.getValue().toString();
      if (val.equals(other)) {
        freetext = new TextField();
        freetext.setRequired(true);
        freetext.setStyleName(Styles.fieldTheme);
        RegexpValidator factorLabelValidator = new RegexpValidator("([a-z]+_?[a-z]*)+([a-z]|[0-9]*)",
                "Name must start with a lower case letter and contain only lower case letter words, which can be connected by underscores ('_'). It can end with one or more numbers.");
        freetext.addValidator(factorLabelValidator);
        freetext.setImmediate(true);
        freetext.setValidationVisible(true);
        addComponent(freetext);
      } else {
        if (this.components.contains(freetext))
          removeComponent(freetext);
      }
    } else {
      if (this.components.contains(freetext))
        removeComponent(freetext);
    }
    isSpecial = val.equals(special);
  }

  public boolean chooserSet() {
    return chooser.getValue() != null;
  }

  public boolean isSet() {
    if (chooser.getValue() == null)
      return false;
    else
      return !chooser.getValue().toString().equals(other) || !freetext.getValue().isEmpty();
  }

  public boolean isValid() {
    if (chooser.getValue() == null)
      return true;
    else
      return !chooser.getValue().toString().equals(other) || freetext.isValid();
  }

  public String getCondition() {
    Object val = chooser.getValue();
    if (val == null)
      return null;
    else if (val.toString().equals(other))
      return freetext.getValue();
    else
      return val.toString();
  }

  public Object getBox() {
    return chooser;
  }

  public void reset() {
    chooser.select(chooser.getNullSelectionItemId());
  }
}
