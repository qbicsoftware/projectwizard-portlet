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
package life.qbic.projectwizard.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.identifiers.ExperimentCodeFunctions;
import life.qbic.datamodel.persons.OpenbisSpaceUserRole;
import life.qbic.expdesign.ParserHelpers;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.projectwizard.adminviews.MCCView;
import life.qbic.projectwizard.model.Vocabularies;
import life.qbic.projectwizard.registration.OpenbisCreationController;
import life.qbic.xml.manager.StudyXMLParser;
import life.qbic.xml.manager.XMLParser;
import life.qbic.xml.properties.Property;
import life.qbic.xml.properties.PropertyType;
import life.qbic.xml.study.Qexperiment;
import life.qbic.xml.study.Qproperty;
import life.qbic.xml.study.TechnologyType;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;

import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.ui.Button;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.themes.ValoTheme;

import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Project;

public class AdminView extends VerticalLayout {

  /**
   * 
   */
  private static final long serialVersionUID = -1713715806593305379L;

  IOpenBisClient openbis;
  OpenbisCreationController registrator;
  String user;

  private TabSheet tabs;
  // space
  private TextField space;
  private TextArea users;
  private Button createSpace;
  // mcc patients
  private MCCView addMultiScale;
  private ExperimentalDesignConversionView conversionView;

  // edit data

  // upload metainfo
  // private MetadataUploadView metadataUpload;

  // logger
  private Logger logger = LogManager.getLogger(AdminView.class);
  
  public AdminView(IOpenBisClient openbis, Vocabularies vocabularies,
      OpenbisCreationController creationController, String user) {
    this.user = user;
    this.registrator = creationController;
    this.openbis = openbis;
    tabs = new TabSheet();
    tabs.setStyleName(ValoTheme.TABSHEET_FRAMED);

    VerticalLayout spaceView = new VerticalLayout();
    spaceView.setSpacing(true);
    spaceView.setMargin(true);
    space = new TextField("Space");
    space.setWidth("300px");
    space.setStyleName(Styles.fieldTheme);
    space.addValidator(new RegexpValidator("[-\\w]+",
        "Spaces are only allowed to contain alphanumeric characters, underscore (_) and minus (-)."));
    space.setValidationVisible(true);
    users = new TextArea("Users");
    users.setStyleName(Styles.areaTheme);
    users.setWidth("100px");
    users.setHeight("100px");
    createSpace = new Button("Create Space");
    spaceView.addComponent(space);
    spaceView.addComponent(Styles.questionize(users,
        "Users must exist in openBIS, otherwise the space cannot be created!",
        "Add Users to Space"));
    spaceView.addComponent(createSpace);
    tabs.addTab(spaceView, "Create Space");

    // METADATA
    // metadataUpload = new MetadataUploadView(openbis, vocabularies);
    // tabs.addTab(metadataUpload, "Update Metadata");

    // MULTISCALE
    addMultiScale = new MCCView(openbis, creationController, user);
    addMultiScale.setSpacing(true);
    addMultiScale.setMargin(true);   

    tabs.addTab(addMultiScale, "Add Multiscale Samples");
    
    // Convert Projects to new experimental design
    conversionView = new ExperimentalDesignConversionView(openbis);
    tabs.addTab(conversionView, "Project Migration");

//     tabs.addTab(new PrototypeView(), "Prototypes");

    addComponent(tabs);
    initButtons();
  }

  private void initButtons() {
    createSpace.addClickListener(new Button.ClickListener() {

      /**
       * 
       */
      private static final long serialVersionUID = -6870391592753359641L;

      @Override
      public void buttonClick(ClickEvent event) {
        space.validate();
        if (space.isValid()) {
          createSpace.setEnabled(false);
          String space = getSpace().toUpperCase();
          if (!openbis.spaceExists(space)) {
            HashMap<OpenbisSpaceUserRole, ArrayList<String>> roleInfos =
                new HashMap<OpenbisSpaceUserRole, ArrayList<String>>();
            if (getUsers().size() > 0)
              roleInfos.put(OpenbisSpaceUserRole.USER, getUsers());
            registrator.registerSpace(space, roleInfos, user);
            // wait few seconds, then check for a maximum of timeout seconds, if space was created
            int timeout = 5;
            int wait = 2;
            try {
              Thread.sleep(wait * 1000);
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            while (timeout > 0 && !openbis.spaceExists(space)) {
              timeout -= 1;
              try {
                Thread.sleep(1000);
              } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
            }
            if (openbis.spaceExists(space)) {
              Styles.notification("Space created", "The space " + space + " has been created!",
                  NotificationType.SUCCESS);
              resetSpaceTab();
            } else {
              Styles.notification("Problem creating space",
                  "There seems to have been a problem while creating the space. Do the specified users already exist in openbis? If not, create them.",
                  NotificationType.ERROR);
            }
            createSpace.setEnabled(true);
          }
        }
      }
    });
  }

  protected void resetSpaceTab() {
    users.setValue("");
    space.setValue("");
  }

  public String getSpace() {
    return space.getValue();
  }

  public ArrayList<String> getUsers() {
    if (!users.getValue().trim().equals(""))
      return new ArrayList<String>(Arrays.asList(users.getValue().split("\n")));
    else
      return new ArrayList<String>();
  }

  public Button getCreateSpace() {
    return createSpace;
  }

}
