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
package life.qbic.portlet.uicomponents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import properties.Property;
import properties.PropertyType;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.Validator;
import com.vaadin.data.validator.NullValidator;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;

import life.qbic.portlet.uicomponents.Styles;
import logging.Log4j2Logger;

/**
 * Composite UI component to input values of Property instances and their units
 * 
 * @author Andreas Friedrich
 * 
 */
public class ConditionPropertyPanel extends VerticalLayout {

  private static final long serialVersionUID = 3320102983685470217L;
  private String condition;
  private OptionGroup type;
  private TextArea values;
  private ComboBox unitField;
  private logging.Logger logger = new Log4j2Logger(ConditionPropertyPanel.class);

  /**
   * Create a new Condition Property Panel
   * 
   * @param condition The name of the condition selected
   * @param units An EnumSet of units (e.g. SI units)
   */
  public ConditionPropertyPanel(String condition, EnumSet<properties.Unit> units) {
    this.condition = condition;
    type = new OptionGroup("", new ArrayList<String>(Arrays.asList("Continuous", "Categorical")));
    // type = new CustomVisibilityComponent(new OptionGroup("", new
    // ArrayList<String>(Arrays.asList("Continuous", "Categorical"))));

    values = new TextArea("Values");
    values.setWidth("300px");
    values.setInputPrompt("Please input different occurrences of the condition " + condition + ",\n"
        + "one per row.");
    Validator v = new PropertyValuesValidator();
    values.addValidator(v);
    values.setValidationVisible(true);
    values.setImmediate(true);
    values.setRequired(true);
    values.setRequiredError("Please input at least one condition.");

    unitField = new ComboBox("Unit", units);
    unitField.setStyleName(Styles.boxTheme);
    unitField.setEnabled(false);
    unitField.setVisible(false);
    unitField.setNullSelectionAllowed(false);
    NullValidator uv =
        new NullValidator("If the condition is continuous, a unit must be selected.", false);
    unitField.addValidator(uv);
    unitField.setValidationVisible(false);

    initListener();
    addComponent(values);
    addComponent(Styles.questionize(type,
        "Continuous variables can be measured and have units, "
            + "e.g. discrete time points, categorical variables don't, e.g. different genotypes.",
        "Variable Type"));
    addComponent(unitField);
    setSpacing(true);
  }

  private void initListener() {
    ValueChangeListener typeListener = new ValueChangeListener() {

      /**
       * 
       */
      private static final long serialVersionUID = -6989982426500636013L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        boolean on = type != null && type.getValue().toString().equals("Continuous");
        unitField.setEnabled(on);
        unitField.setVisible(on);
        unitField.setValidationVisible(on);
        if (!on)
          unitField.select(unitField.getNullSelectionItemId());
      }
    };
    type.addValueChangeListener(typeListener);
  }

  /**
   * Returns all conditions with their units as a list
   * 
   * @return
   */
  public List<Property> getFactors() {
    logger.debug("debugging input of conditions: ");
    List<Property> res = new ArrayList<Property>();
    properties.Unit unit = null;
    if (unitField.getValue() != null)
      unit = (properties.Unit) unitField.getValue();
    logger.debug("whole string:>" + values.getValue() + "<end");
    for (String val : values.getValue().split("\n")) {
      if (unit != null)
        res.add(new Property(condition.toLowerCase().replace(" ", "_"), val, unit,
            PropertyType.Factor));
      else
        res.add(new Property(condition.toLowerCase().replace(" ", "_"), val, PropertyType.Factor));
    }
    logger.debug("resulting list: " + res);
    return res;
  }

  public boolean unitSet() {
    return unitField.getValue() != null;
  }

  public boolean isContinuous() {
    return type.getValue() != null && type.getValue().toString().equals("Continuous");
  }

  public ComboBox getUnitsBox() {
    return unitField;
  }

  public TextArea getInputArea() {
    return values;
  }

  public OptionGroup getUnitTypeSelect() {
    return type;
  }

  public boolean isValid() {
    return values.isValid();
  }

  public String getException() {
    String err = "";
    String val = values.getValue();
    if (val.isEmpty())
      err = "Please input different occurrences of the condition " + condition + ",\n"
          + "one per row.";
    else if (val.contains(";") || val.contains(":"))
      err = "Please don't use the symbols ';' or ':'";
    else if (isContinuous() && !unitSet())
      err = "If the condition is continuous, a unit must be selected.";
    return err;
  }

  public class PropertyValuesValidator implements Validator {

    @Override
    public void validate(Object value) throws InvalidValueException {
      String val = (String) value;
      if (val != null && !val.isEmpty())
        // throw new InvalidValueException("Please fill in values.");
        if (val.contains(":") || val.contains(";"))
        throw new InvalidValueException("Please don't use the symbols ';' or ':'");
    }
  }
}
