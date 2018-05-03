package life.qbic.portlet.adminviews;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

import life.qbic.portlet.ProjectWizardUI;
import life.qbic.portlet.io.DBManager;
import life.qbic.portlet.io.NCBITerm;
import life.qbic.portlet.io.OntologyParser;
import life.qbic.portlet.io.TaxonomySearcher;
import life.qbic.ui.Styles;
import life.qbic.ui.Styles.NotificationType;

public class PrototypeView extends VerticalLayout {

  private TextField taxTest;
  private Button search;
  private Label organism;
  private TaxonomySearcher taxSearcher = new TaxonomySearcher();
  private OntologyParser ontoParser;
  private ComboBox tissueBox;

  public PrototypeView(DBManager dbm) {
    this.ontoParser = new OntologyParser(dbm);
    // owlParser.init();
    setMargin(true);
    setSpacing(true);
    taxTest = new TextField("Taxonomy Search");
    taxTest.setStyleName(Styles.fieldTheme);
    search = new Button("Search");
    Button.ClickListener cl = new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        if (taxTest.getValue() != null) {
          try {
            showResults(taxSearcher.getSpecies(taxTest.getValue()));
          } catch (XPathExpressionException | IOException | SAXException
              | ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        } else
          Styles.notification("Species not found.", "No result was found for your search.",
              NotificationType.DEFAULT);
      }

    };
    search.addClickListener(cl);
    addComponent(taxTest);
    addComponent(search);
    organism = new Label("Selected Species");
    organism.setStyleName(Styles.fieldTheme);
    addComponent(organism);
    organism.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        if (organism.getValue().isEmpty())
          resetTissues();
        else {
          String category = "unknown";
          try {
            category = taxSearcher.getKingdomTerm((String) organism.getData());
          } catch (XPathExpressionException | IOException | ParserConfigurationException
              | SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          prepareTissues(category);
        }
      }
    });
    tissueBox = new ComboBox("Tissue");
    tissueBox.setEnabled(false);
    tissueBox.setFilteringMode(FilteringMode.CONTAINS);
    tissueBox.setStyleName(Styles.boxTheme);
    addComponent(tissueBox);
  }

  protected void prepareTissues(String category) {
    resetTissues();
    List<String> tissues = new ArrayList<String>(ontoParser.getTissuesFor(category));
    Collections.sort(tissues);
    tissueBox.addItems(tissues);
    tissueBox.setEnabled(true);
  }

  protected void resetTissues() {
    tissueBox.removeAllItems();
    tissueBox.setEnabled(false);
  }

  protected void showResults(List<NCBITerm> species) {
    String header = " Search Results";

    Window subWindow = new Window(header);
    subWindow.setWidth("380px");

    VerticalLayout layout = new VerticalLayout();
    layout.setSpacing(true);
    layout.setMargin(true);

    Table table = new Table();
    table.setWidth("350px");
    table.setStyleName(Styles.tableTheme);
    table.addContainerProperty("Label", String.class, null);
    table.addContainerProperty("Taxonomy ID", String.class, null);
    table.setSelectable(true);

    for (int i = 0; i < species.size(); i++) {
      List<Object> row = new ArrayList<Object>();
      row.add(species.get(i).getSciName());
      row.add(species.get(i).getTaxID());
      table.addItem(row.toArray(new Object[row.size()]), i);
    }
    table.sort(new Object[] {"Label", "Taxonomy ID"}, new boolean[] {true, true});
    table.setPageLength(Math.min(15, table.size()));
    Button select = new Button("Ok");
    select.setEnabled(false);

    table.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        select.setEnabled(table.getValue() != null);
      }
    });

    select.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        Item item = table.getItem(table.getValue());
        String label = (String) item.getItemProperty("Label").getValue();
        String taxID = (String) item.getItemProperty("Taxonomy ID").getValue();
        subWindow.close();
        organism.setData(taxID);
        organism.setValue(label);
      }
    });

    Button close = new Button("Close");
    close.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        subWindow.close();
      }
    });
    layout.addComponent(table);

    HorizontalLayout buttons = new HorizontalLayout();
    buttons.setSpacing(true);

    buttons.addComponent(close);
    buttons.addComponent(select);
    layout.addComponent(buttons);

    subWindow.setContent(layout);
    // Center it in the browser window
    subWindow.center();
    subWindow.setModal(true);
    subWindow.setIcon(FontAwesome.SEARCH);
    subWindow.setResizable(false);
    ProjectWizardUI ui = (ProjectWizardUI) UI.getCurrent();
    ui.addWindow(subWindow);
  }

}
