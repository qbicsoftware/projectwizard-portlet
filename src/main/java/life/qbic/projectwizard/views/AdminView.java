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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import life.qbic.datamodel.persons.OpenbisSpaceUserRole;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.projectwizard.adminviews.MCCView;
import life.qbic.projectwizard.io.DBVocabularies;
import life.qbic.projectwizard.registration.OpenbisCreationController;
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

  // edit data

  // upload metainfo
  // private MetadataUploadView metadataUpload;

  // logger
  private Logger logger = LogManager.getLogger(AdminView.class);

  public AdminView(IOpenBisClient openbis, DBVocabularies vocabularies,
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

    // tabs.addTab(new PrototypeView(), "Prototypes");

    addComponent(tabs);

    initButtons();
//    System.out.println("testing");
//    try {
//      test();
//    } catch (IllegalArgumentException | JAXBException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }
  }

//  private void test() throws IllegalArgumentException, JAXBException {
//    Map<String, Map<Pair<String,String>, List<String>>> expDesign =
//        new HashMap<String, Map<Pair<String,String>, List<String>>>();
//    Map<String, List<Property>> otherProps = new HashMap<String, List<Property>>();
//    Set<String> types = new HashSet<String>(Arrays.asList("Q_BIOLOGICAL_SAMPLE",
//        "Q_BIOLOGICAL_ENTITY", "Q_TEST_SAMPLE", "Q_MHC_LIGAND_EXTRACT"));
//    System.out.println("before fetch");
//    List<Sample> samples = openbis.getSamplesOfProject("/MULTISCALEHCC/QMSHS");
//    int size = samples.size();
//    System.out.println(size);
//    int currentPercent = 0;
//    int current = 0;
//    for (Sample s : samples) {
//      if (types.contains(s.getSampleTypeCode())) {
//        current++;
//        int newPercent = current * 100 / size;
//        if (currentPercent != newPercent)
//          System.out.println(newPercent);
//        currentPercent = newPercent;
//
//        String code = s.getCode();
//        XMLParser par = new XMLParser();
//        List<Property> props = par.getAllPropertiesFromXML(s.getProperties().get("Q_PROPERTIES"));
//        for (Property p : props) {
//          if (p.getType().equals(PropertyType.Factor)) {
//            String lab = p.getLabel();
//            String val = p.getValue();
//            String unit = "";
//            if (p.hasUnit())
//              unit = p.getUnit().getValue();
//            Pair<String,String> valunit = new ImmutablePair<String,String>(val, unit);
//            if (expDesign.containsKey(lab)) {
//              Map<Pair<String,String>, List<String>> levels = expDesign.get(lab);
//              if (levels.containsKey(valunit)) {
//                levels.get(valunit).add(code);
//              } else {
//                levels.put(valunit, new ArrayList<String>(Arrays.asList(code)));
//              }
//            } else {
//              Map<Pair<String,String>, List<String>> newLevel = new HashMap<Pair<String,String>, List<String>>();
//              newLevel.put(valunit, new ArrayList<String>(Arrays.asList(code)));
//              expDesign.put(lab, newLevel);
//            }
//
//          } else {
//            if (otherProps.containsKey(code)) {
//              otherProps.get(code).add(p);
//            } else {
//              otherProps.put(code, new ArrayList<Property>(Arrays.asList(p)));
//            }
//          }
//        }
//      }
//    }
//    NewXMLParser p = new NewXMLParser();
//    JAXBElement<Qexperiment> res = p.createNewDesign(
//        new ArrayList<String>(Arrays.asList("Genomics", "Ligandomics")), expDesign, otherProps);
//    String xml = p.toString(res);
//    try {
//      File file = new File("/Users/frieda/Desktop/qmshs.xml");
//      FileWriter fileWriter = new FileWriter(file);
//      fileWriter.write(xml);
//      fileWriter.flush();
//      fileWriter.close();
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
//    System.out.println("done");
//  }

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
