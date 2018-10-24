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
package life.qbic.projectwizard.uicomponents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.combobox.FilteringMode;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;

import life.qbic.portal.Styles;
import life.qbic.projectwizard.model.Vocabularies;

public class GeneralMSInfoPanel extends VerticalLayout {

  /**
   * 
   */
  private static final long serialVersionUID = -2282545855402710972L;

  private ComboBox lcmsMethodBox;
  private TextArea lcmsSpecial;
  private ComboBox chromType;
  private ComboBox deviceBox;
  private Vocabularies vocabs;

  OptionGroup conditionsSet;

  public GeneralMSInfoPanel(Vocabularies vocabs, String name) {
    this.setCaption(name);
    this.vocabs = vocabs;

    List<String> chromTypes = new ArrayList<String>(vocabs.getChromTypesMap().keySet());
    Collections.sort(chromTypes);

    deviceBox = new ComboBox("MS Device");
    deviceBox.setFilteringMode(FilteringMode.CONTAINS);
    deviceBox.setStyleName(Styles.boxTheme);
    deviceBox.setWidth("300px");
    chromType = new ComboBox("MS Chromatography Type", chromTypes);
    chromType.setFilteringMode(FilteringMode.CONTAINS);
    chromType.setStyleName(Styles.boxTheme);
    lcmsMethodBox = new ComboBox("MS LCMS Method");
    lcmsMethodBox.setFilteringMode(FilteringMode.CONTAINS);
    lcmsMethodBox.setStyleName(Styles.boxTheme);
    lcmsMethodBox.setWidth("350px");

    lcmsSpecial = new TextArea("LCMS Method Name");
    lcmsSpecial.setStyleName(Styles.areaTheme);
    lcmsSpecial.setVisible(false);
    addComponent(Styles.questionize(deviceBox,
        "The MS device that is used to conduct the experiment.", "MS Device"));
    addComponent(Styles.questionize(chromType,
        "Specifies the kind of chromatography that is coupled to the mass spectrometer.",
        "Chromatography Type"));
    addComponent(Styles.questionize(lcmsMethodBox,
        "Labratory specific parameters for LCMS measurements.", "LCMS Method"));
    addComponent(lcmsSpecial);

    lcmsMethodBox.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        String val = (String) lcmsMethodBox.getValue();
        boolean special = val.equals("SPECIAL_METHOD");
        lcmsSpecial.setVisible(special);
        if (!special)
          lcmsSpecial.setValue("");
      }
    });

    setSpacing(true);
  }

  public Map<String, Object> getExperimentalProperties() {
    Map<String, Object> res = new HashMap<String, Object>();

    if (deviceBox.getValue() != null)
      res.put("Q_MS_DEVICE", vocabs.getDeviceMap().get(deviceBox.getValue()));
    if (lcmsMethodBox.getValue() != null)
      res.put("Q_MS_LCMS_METHOD", lcmsMethodBox.getValue());
    if (chromType.getValue() != null)
      res.put("Q_CHROMATOGRAPHY_TYPE", vocabs.getChromTypesMap().get(chromType.getValue()));
    if (!lcmsSpecial.getValue().isEmpty())
      res.put("Q_MS_LCMS_METHOD_INFO", lcmsSpecial.getValue());

    // res.put("Q_MS_DEVICE", vocabs.getDeviceMap().get(deviceBox.getValue()));
    // res.put("Q_MS_LCMS_METHOD", lcmsMethodBox.getValue());
    // res.put("Q_CHROMATOGRAPHY_TYPE", chromType.getValue());
    // res.put("Q_MS_LCMS_METHOD_INFO", lcmsSpecial.getValue());
    List<String> remove = new ArrayList<String>();
    for (String key : res.keySet()) {
      String val = (String) res.get(key);
      if (val == null || val.isEmpty())
        remove.add(key);
    }
    for (String key : remove)
      res.remove(key);
    return res;
  }

  public boolean isValid() {
    // TODO restrictions?
    // Set<String> uniques = new HashSet<String>();
    // boolean nonEmpty = false;
    // for (EnzymeChooser c : choosers) {
    // uniques.add(c.getEnzyme());
    // nonEmpty |= (!(c.getEnzyme() == null) && !c.getEnzyme().isEmpty());
    // }
    // if (uniques.size() < choosers.size() || !nonEmpty) {
    // Functions.notification("Wrong input", "Please input at least one enzyme and the same enzyme
    // only once.", NotificationType.ERROR);
    // return false;
    // } else
    return true;
  }

  public void filterDictionariesByPrefix(String prefix, List<String> dontFilter) {
    List<String> devices = new ArrayList<String>();
    List<String> lcmsMethods = new ArrayList<String>();
    if (prefix.isEmpty()) {
      devices.addAll(vocabs.getDeviceMap().keySet());
    } else {
      for (String device : vocabs.getDeviceMap().keySet()) {
        if (device.contains("(" + prefix + ")") || dontFilter.contains(device))
          devices.add(device);
      }
    }
    for (String lcmsMethod : vocabs.getLcmsMethods()) {
      if (lcmsMethod.startsWith(prefix) || dontFilter.contains(lcmsMethod))
        lcmsMethods.add(lcmsMethod);
    }
    Collections.sort(devices);
    Collections.sort(lcmsMethods);
    deviceBox.removeAllItems();
    deviceBox.addItems(devices);
    lcmsMethodBox.removeAllItems();
    lcmsMethodBox.addItems(lcmsMethods);
  }

}
