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
package life.qbic.portlet.uicomponents;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;

import life.qbic.portlet.uicomponents.Styles;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;

/**
 * Composite UI component for inputting an arbitrary number of experimental conditions
 * 
 * @author Andreas Friedrich
 * 
 */
public class ConditionsPanel extends HorizontalLayout {

  private static final long serialVersionUID = -1578503116738309380L;
  List<String> options;
  String other;
  String special;
  ComboBox specialField;
  TextField specialNumField;
  boolean nullSelectionAllowed;
  List<ConditionChooser> choosers;
  ValueChangeListener conditionChangeListener;
  Button.ClickListener buttonListener;
  HorizontalLayout buttonGridComp;
  GridLayout buttonGrid;
  Button add;
  Button remove;

  OptionGroup conditionsSet;

  /**
   * Create a new Conditions Panel component to select experimental conditions
   * 
   * @param options List of different possible conditions
   * @param other Name of the "other" condition, which when selected will enable an input field for
   *        free text
   * @param special Name of a "special" condition like species for the entity input, which when
   *        selected will disable the normal species input because there is more than one instance
   * @param specialField the ComboBox containing the options of the special field, will be disabled
   *        when special is chosen as a condition
   * @param nullSelectionAllowed true, if the conditions may be empty
   * @param conditionsSet (empty) option group that makes it possible to listen to the conditions
   *        inside this component from the outside
   * @param specialNumField
   */
  public ConditionsPanel(List<String> options, String other, String special, ComboBox specialField,
      boolean nullSelectionAllowed, OptionGroup conditionsSet, TextField specialNumField) {
    this.specialField = specialField;
    this.specialNumField = specialNumField;
    this.options = options;
    this.other = other;
    this.special = special;
    this.nullSelectionAllowed = nullSelectionAllowed;

    this.conditionsSet = conditionsSet;
    this.conditionsSet.addItem("set");

    initListener();

    choosers = new ArrayList<ConditionChooser>();
    ConditionChooser c = new ConditionChooser(options, other, special, nullSelectionAllowed);
    c.addListener(conditionChangeListener);
    choosers.add(c);
    addComponent(c);

    buttonGrid = new GridLayout(1, 2);
    buttonGrid.setSpacing(true);
    add = new Button();
    remove = new Button();
    Styles.iconButton(add, FontAwesome.PLUS_SQUARE);
    Styles.iconButton(remove, FontAwesome.MINUS_SQUARE);
    buttonGrid.addComponent(add);
    buttonGrid.addComponent(remove);
    add.addClickListener(buttonListener);
    remove.addClickListener(buttonListener);
    buttonGridComp = new HorizontalLayout();
    buttonGridComp.addComponent(buttonGrid);
    buttonGridComp =
        Styles.questionize(buttonGridComp,
            "Choose (optional) experimental variables for this level of your experiment. "
                + "You can add or remove variables using " + FontAwesome.PLUS_SQUARE.getHtml()
                + " and " + FontAwesome.MINUS_SQUARE.getHtml() + ".", "Experimental Variables");
    addComponent(buttonGridComp);
    setSpacing(true);
  }

  private void initListener() {
    conditionChangeListener = new ValueChangeListener() {

      private static final long serialVersionUID = 1521218807043139513L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        ComboBox source = (ComboBox) event.getProperty();
        boolean special = false;
        conditionsSet.setValue(null);
        for (ConditionChooser c : choosers) {
          if (c.getBox().equals(source)) {
            c.changed();
            if (c.chooserSet())
              conditionsSet.select("set");
          }
          special |= c.factorIsSpecial();
        }
        specialField.setRequired(!special);
        //TODO test this, was enabled
        specialField.setVisible(!special);
        specialNumField.setEnabled(special);
        specialNumField.setVisible(special);
      }
    };

    buttonListener = new Button.ClickListener() {

      private static final long serialVersionUID = 2240224129259577437L;

      @Override
      public void buttonClick(ClickEvent event) {
        if (event.getButton().equals(add))
          add();
        else
          remove();
      }

    };
  }

  public List<String> getConditions() {
    List<String> res = new ArrayList<String>();
    for (ConditionChooser c : choosers) {
      if (c.isSet())
        res.add(c.getCondition());
    }
    return res;
  }

  public boolean isValid() {
    boolean res = true;
    for (ConditionChooser c : choosers) {
      res &= c.isValid();
    }
    return res;
  }

  public void changed() {
    for (ConditionChooser c : choosers)
      c.changed();
  }

  private void add() {
    ConditionChooser c = new ConditionChooser(options, other, special, nullSelectionAllowed);
    c.addListener(conditionChangeListener);
    choosers.add(c);

    removeComponent(buttonGridComp);
    addComponent(c);
    addComponent(buttonGridComp);
  }

  private void remove() {
    int size = choosers.size();
    if (size > 1) {
      ConditionChooser last = choosers.get(size - 1);
      last.reset();
      removeComponent(last);
      choosers.remove(last);
    }
  }

}
