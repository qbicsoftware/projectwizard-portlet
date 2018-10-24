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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Table;
import com.vaadin.ui.Upload.FinishedEvent;
import com.vaadin.ui.Upload.FinishedListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.VerticalLayout;

import life.qbic.portal.portlet.ProjectWizardUI;
import life.qbic.projectwizard.io.AttachmentInformation;
import life.qbic.portal.Styles;
import life.qbic.portal.components.StandardTextField;
public class WizardUploadsPanel extends VerticalLayout {

  /**
   * 
   */
  private static final long serialVersionUID = 6971325287434528738L;

  private static final Logger logger = LogManager.getLogger(WizardUploadsPanel.class);

  private File current;
  private Map<Object, AttachmentInformation> attachments;
  private String project;

  private ComboBox context;
  // private Upload upload;
  private UploadComponent upload;
  private StandardTextField fileInfo;
  private Button add;
  private Table toUpload;
  private Button commit;

  private Label info;
  private ProgressBar bar;

  private String userID;

  public WizardUploadsPanel(String project, List<String> expOptions, String userID, int uploadSize) {
    setCaption("Upload Planning Files");
    this.userID = userID;
    this.project = project;
    attachments = new HashMap<Object, AttachmentInformation>();

    initUpload(uploadSize);

    fileInfo = new StandardTextField("Description");
    addComponent(fileInfo);
    fileInfo.setVisible(false);

    context = new ComboBox("Attach to...");
    context.setVisible(false);
    context.setNullSelectionAllowed(false);
    context.setStyleName(Styles.boxTheme);
    context.addItems(expOptions);
    context.addItem("General Infos");
    addComponent(context);


    add = new Button("Add");
    addComponent(add);
    add.setVisible(false);

    initTable();

    bar = new ProgressBar();
    bar.setVisible(false);
    info = new Label();
    addComponent(bar);
    addComponent(info);

    commit = new Button("Commit Files");
    addComponent(commit);
    commit.setVisible(false);

    setSpacing(true);
    addListeners();
  }

  private void initTable() {
    toUpload = new Table("Files to Upload");
    toUpload.setStyleName(Styles.tableTheme);
    toUpload.setWidth("400px");
    toUpload.addContainerProperty("Info", String.class, null);
    toUpload.addContainerProperty("Context", String.class, null);
    toUpload.addContainerProperty("Remove", Button.class, null);
    addComponent(toUpload);
    toUpload.setVisible(false);
  }

  public void resetContext() {
    context.setValue("General Infos");
  }

  private void addListeners() {

    context.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        add.setVisible(current != null);
      }
    });

    add.addClickListener(new ClickListener() {

      @SuppressWarnings("unchecked")
      @Override
      public void buttonClick(ClickEvent event) {
        Button delete = new Button();
        Styles.iconButton(delete, FontAwesome.UNDO);

        Object itemId = toUpload.addItem();

        delete.setData(itemId);
        String secondary = fileInfo.getValue();
        String cntxt = (String) context.getValue();
        attachments.put(itemId, new AttachmentInformation(current.getName(), secondary, userID,
            getBarcode(cntxt), cntxt, cntxt));

        delete.addClickListener(new Button.ClickListener() {
          /**
         * 
         */
          private static final long serialVersionUID = 5414603256990177472L;

          @Override
          public void buttonClick(ClickEvent event) {
            Integer iid = (Integer) event.getButton().getData();
            toUpload.removeItem(iid);
            attachments.remove(iid);
            tableChanged();
          }
        });
        toUpload.getContainerProperty(itemId, "Info").setValue(secondary);
        toUpload.getContainerProperty(itemId, "Context").setValue(cntxt);
        toUpload.getContainerProperty(itemId, "Remove").setValue(delete);
        tableChanged();
        add.setVisible(false);
        fileInfo.setVisible(false);
        context.setVisible(false);
      }

      private String getBarcode(String choice) {
        if (choice.equals("General Infos"))
          return project + "000";
        else
          return project + choice.split(" ")[0];
      }
    });
  }

  public Button getCommitButton() {
    return commit;
  }

  private void tableChanged() {
    boolean notEmpty = toUpload.size() != 0;
    toUpload.setPageLength(toUpload.size() + 1);
    toUpload.setVisible(notEmpty);
    commit.setVisible(notEmpty);
  }

  private void initUpload(int maxSize) {
    upload =
        new UploadComponent("Upload experiment information here", "Upload",
            ProjectWizardUI.tmpFolder, "up_", maxSize * 1000000);
    FinishedListener uploadFinListener = new FinishedListener() {
      /**
       * 
       */
      private static final long serialVersionUID = -8413963075202260180L;

      public void uploadFinished(FinishedEvent event) {
        if (upload.wasSuccess()) {
          File file = upload.getFile();
          logger.info("Upload successful");
          current = file;
          context.setVisible(true);
          if (context.getValue() != null)
            add.setVisible(true);
          fileInfo.setVisible(true);
          int i = file.getName().lastIndexOf('.');
          if (i < 0)
            i = file.getName().length();
          fileInfo.setValue(file.getName().substring(3, i));
        }
      }
    };
    upload.addFinishedListener(uploadFinListener);
    addComponent(upload);
  }

  public Map<Object, AttachmentInformation> getAttachments() {
    return attachments;
  }

  public Label getLabel() {
    return info;
  }

  public ProgressBar getBar() {
    return bar;
  }

  public void startCommit() {
    commit.setEnabled(false);
    bar.setVisible(true);
  }

  public void commitDone() {
    upload.setVisible(false);
    bar.setVisible(false);
    add.setVisible(false);
    info.setValue("Successfully moved all files.");
  }
}
