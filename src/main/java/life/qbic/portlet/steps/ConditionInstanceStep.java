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
package life.qbic.portlet.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import properties.Property;
import properties.PropertyType;
import componentwrappers.CustomVisibilityComponent;
import componentwrappers.StandardTextField;
import life.qbic.portlet.model.TissueInfo;
import life.qbic.portlet.uicomponents.ConditionPropertyPanel;
import life.qbic.portlet.uicomponents.FactorOptionBox;
import life.qbic.portlet.uicomponents.Styles;
import life.qbic.portlet.uicomponents.Styles.*;

import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.ErrorMessage;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Wizard Step to select what experimental conditions can be distinguished at a step of the
 * experimental design
 * 
 * @author Andreas Friedrich
 * 
 */
public class ConditionInstanceStep implements WizardStep {

  private boolean skip = false;

  private VerticalLayout main;
  private TabSheet instances;

  private List<ConditionPropertyPanel> factorInstances;
  private List<FactorOptionBox> optionInstances;
  private Set<String> options;
  private String optionName;
  private String stepName;
  private CustomVisibilityComponent previewFrame;
  private Table preview;
  private Map<Object, Integer> permutations;

  private Map<String, TissueInfo> specialTissueInfos;

  /**
   * Create a new Condition Step for the wizard
   * 
   * @param options Set of different conditions available e.g. different species
   * @param optionName Title of the options selection e.g. Species
   * @param stepName Title of this step e.g. Extraction Conditions
   */
  public ConditionInstanceStep(Set<String> options, String optionName, String stepName) {
    main = new VerticalLayout();
    main.setSpacing(true);
    main.setMargin(true);
    instances = new TabSheet();
    instances.setStyleName(ValoTheme.TABSHEET_FRAMED);
    main.addComponent(instances);
    preview = new Table();
    preview.addContainerProperty("Factors", String.class, null);
    preview.addContainerProperty("Samples", TextField.class, null);
    preview.setStyleName(Styles.tableTheme);
    VerticalLayout frame = new VerticalLayout();
    frame.setCaption("Preview of Combinations");
    frame.addComponent(preview);
    previewFrame = new CustomVisibilityComponent(frame);
    previewFrame.setVisible(false);

    main.addComponent(Styles.questionize(previewFrame,
        "This table shows a preview of the number and type of samples "
            + "that will be created in the next step. You can change the number of samples for each "
            + "group of samples. For example, if one variable combination is not in the experiment at all, set it to 0.",
        "Preview"));

    this.options = options;
    this.optionName = optionName;
    this.stepName = stepName;
    factorInstances = new ArrayList<ConditionPropertyPanel>();
    optionInstances = new ArrayList<FactorOptionBox>();
    permutations = new LinkedHashMap<Object, Integer>();
  }

  public void destroyTable() {
    previewFrame.setVisible(false);
    preview.removeAllItems();
  }

  public void buildTable(List<String> permutations, String startAmount) {
    preview.removeAllItems();
    int i = 0;
    for (String s : permutations) {
      s = s.replace("###", " ; ");
      i++;
      Integer itemId = new Integer(i);
      TextField tf = new StandardTextField();
      tf.setValue(startAmount);
      preview.addItem(new Object[] {s, tf}, itemId);
    }
    preview.setPageLength(preview.size());
    previewFrame.setVisible(true);
  }

  @Override
  public String getCaption() {
    return stepName;
  }

  public void initFactorFields(List<String> factors) {
    for (int i = 0; i < factors.size(); i++) {
      String f = factors.get(i);
      if (!optionName.contains(f)) {
        EnumSet<properties.Unit> units = EnumSet.noneOf(properties.Unit.class);
        units.addAll(Arrays.asList(properties.Unit.values()));
        ConditionPropertyPanel a = new ConditionPropertyPanel(f, units);
        a.setMargin(true);
        factorInstances.add(a);
        instances.addTab(a, f);
      }
    }
  }

  public void initOptionsFactorComponent(int amount, Set<String> altOptions, String altBoxCaption,
      String altFieldCaption, String altBoxKeyword, String altFieldKeyword) {
    VerticalLayout optionBox = new VerticalLayout();
    for (int i = 1; i <= amount; i++) {
      FactorOptionBox b = new FactorOptionBox(options, altOptions, optionName + " " + i,
          altBoxCaption, altFieldCaption, altBoxKeyword, altFieldKeyword);
      b.setStyleName(Styles.boxTheme);
      optionInstances.add(b);
      optionBox.addComponent(b);
    }
    HorizontalLayout helpBox = Styles.questionize(optionBox,
        "To continue, fill in the different " + optionName + " in this experiment.", optionName);
    helpBox.setMargin(true);
    instances.addTab(helpBox, optionName);
  }

  @Override
  public Component getContent() {
    return main;
  }

  @Override
  public boolean onAdvance() {
    boolean valid = validInput();
    if (!valid) {
      String error = "";
      for (ConditionPropertyPanel p : factorInstances) {
        if (p.getException() != "")
          error = p.getException();
      }
      if(error.isEmpty())
        error = "Please add all missing values.";
      Styles.notification("Wrong input", error, NotificationType.ERROR);
    }
    return skip || valid;
  }

  public boolean validInput() {
    boolean def = optionInstances.size() > 0 || factorInstances.size() > 0;
    for (FactorOptionBox b : optionInstances)
      def &= b.isValid();
    for (ConditionPropertyPanel p : factorInstances) {
      def &= p.isValid();
      if (p.isContinuous())
        def &= p.unitSet();
    }
    return def;
  }

  @Override
  public boolean onBack() {
    resetFactorFields();
    return true;
  }

  public void attachListener(ValueChangeListener l) {
    for (FactorOptionBox b : optionInstances)
      b.addValueChangeListener(l);
    for (ConditionPropertyPanel p : factorInstances) {
      p.getUnitTypeSelect().addValueChangeListener(l);
      p.getUnitsBox().addValueChangeListener(l);
      p.getInputArea().addValueChangeListener(l);
    }
  }

  public List<List<Property>> getFactors() {
    List<List<Property>> res = new ArrayList<List<Property>>();
    for (ConditionPropertyPanel a : this.factorInstances) {
      res.add(a.getFactors());
    }
    if (this.optionInstances.size() > 0) {
      List<Property> source = new ArrayList<Property>();
      specialTissueInfos = new HashMap<String, TissueInfo>();
      for (FactorOptionBox b : this.optionInstances) {
        String val = (String) b.getValue();
        source.add(new Property(optionName.toLowerCase(), val, PropertyType.Factor));
        specialTissueInfos.put(val, b.getInfoComponent());
      }
      res.add(source);
    }
    return res;
  }

  private void resetFactorFields() {
    instances.removeAllComponents();
    factorInstances = new ArrayList<ConditionPropertyPanel>();
    optionInstances = new ArrayList<FactorOptionBox>();
  }

  public void setSkipStep(boolean b) {
    skip = b;
  }

  public boolean isSkipped() {
    return skip;
  }

  public Map<Object, Integer> getPreSelection() {
    permutations = new LinkedHashMap<Object, Integer>();
    for (Object id : preview.getItemIds()) {
      int amount = parseAmount(preview.getItem(id).getItemProperty("Samples").getValue());
      permutations.put(id, amount);
    }
    return permutations;
  }

  private int parseAmount(Object o) {
    String val = ((TextField) o).getValue();
    if (val.isEmpty())
      return 0;
    else
      return Integer.parseInt(val);
  }

  public Map<String, TissueInfo> getSpecialTissueMap() {
    return specialTissueInfos;
  }
}
