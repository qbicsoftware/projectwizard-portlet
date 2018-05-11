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
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.VerticalLayout;

import life.qbic.projectwizard.control.WizardController.Steps;
import life.qbic.projectwizard.io.DBVocabularies;
import life.qbic.projectwizard.steps.MSAnalyteStep;
import life.qbic.portal.Styles;

public class MSOptionComponent extends VerticalLayout {

  /**
   * 
   */
  private static final long serialVersionUID = 6966022066367510739L;

  private CheckBox proteinPooling;
  private CheckBox shortGel;
  private CheckBox purification;
  private CheckBox measurePeptides;
  private ComboBox purificationMethods;

  private static final Logger logger = LogManager.getLogger(MSOptionComponent.class);

  public MSOptionComponent(DBVocabularies vocabs) {
    this.setCaption("MS Experiment Options");
    setSpacing(true);
    proteinPooling = new CheckBox("Pool Before Protein Fractionation/Enrichment");
    measurePeptides = new CheckBox("Measure Peptides");
    shortGel = new CheckBox("Use Short Gel");
    purification = new CheckBox("Protein Purification");

    addComponent(purification);
    purificationMethods = new ComboBox("Purification Method");
    purificationMethods.setNullSelectionAllowed(false);
    purificationMethods.setStyleName(Styles.boxTheme);
    purificationMethods.setVisible(false);
    List<String> methods =
        new ArrayList<String>(vocabs.getProteinPurificationMethodsMap().values());
    Collections.sort(methods);
    purificationMethods.addItems(methods);
    addComponent(purificationMethods);

    purification.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        purificationMethods.setVisible(purification.getValue());
      }
    });
    addComponent(shortGel);
    addComponent(proteinPooling);
    addComponent(measurePeptides);
  }

  public List<WizardStep> getNextMSSteps(Map<life.qbic.projectwizard.control.WizardController.Steps, WizardStep> steps) {
    boolean poolProteins = proteinPooling.getValue();
    boolean peps = measurePeptides.getValue();
    List<WizardStep> res = new ArrayList<WizardStep>();
    if (poolProteins) {
      res.add(steps.get(Steps.Test_Sample_Pooling));
    }
    MSAnalyteStep f1 = (MSAnalyteStep) steps.get(Steps.Protein_Fractionation);
    res.add(f1);
    if (peps) {
      MSAnalyteStep f2 = (MSAnalyteStep) steps.get(Steps.Peptide_Fractionation);
      f1.setNeedsDigestion(true);
      res.add(f2);
    } else {
      f1.setNeedsDigestion(false);
    }
    res.add(steps.get(Steps.Registration));
    return res;
  }

  public boolean usesShortGel() {
    return shortGel.getValue();
  }

  public boolean usesPurification() {
    return purification.getValue() && purificationMethods.getValue() != null;
  }

  public String getPurificationMethod() {
    return purificationMethods.getValue().toString();
  }

  public boolean isValid() {
    return true;
  }

  public void addMSListener(ValueChangeListener msExpChangedListener) {
    proteinPooling.addValueChangeListener(msExpChangedListener);
    measurePeptides.addValueChangeListener(msExpChangedListener);
  }

  public boolean hasProteinPoolBeforeFractionation() {
    return proteinPooling.getValue();
  }

  public void selectMeasurePeptides(boolean select) {
    measurePeptides.setValue(select);
  }

  public void selectUseShortGel(boolean select) {
    shortGel.setValue(select);
  }

  public void selectProteinPurification(String option) {
    if(option.isEmpty()) {
      purification.setValue(false);
      purificationMethods.setNullSelectionAllowed(true);
      purificationMethods.setValue(purificationMethods.getNullSelectionItemId());
      purificationMethods.setNullSelectionAllowed(false);
    } else {
      purification.setValue(true);
      purificationMethods.setValue(option);
    }
  }

}

