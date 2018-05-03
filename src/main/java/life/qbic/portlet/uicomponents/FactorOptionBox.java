package life.qbic.portlet.uicomponents;

import java.util.Set;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import life.qbic.portlet.model.TissueInfo;

public class FactorOptionBox extends VerticalLayout {

  private ComboBox optionBox;
  private ComboBox alternativeBox;
  private TextField alternativeField;
  private String altFieldKeyword;
  private String altBoxKeyword;

  public FactorOptionBox(Set<String> options, Set<String> altOptions, String optionCaption,
      String altBoxCaption, String altFieldCaption, String altBoxKeyword, String altFieldKeyword) {
    this.altFieldKeyword = altFieldKeyword;
    this.altBoxKeyword = altBoxKeyword;
    optionBox = new ComboBox(optionCaption, options);
    optionBox.setStyleName(Styles.boxTheme);
    optionBox.setImmediate(true);
    optionBox.setRequired(true);
    optionBox.setFilteringMode(FilteringMode.CONTAINS);
    addComponent(optionBox);

    optionBox.addValueChangeListener(new ValueChangeListener() {
      /**
       * 
       */
      private static final long serialVersionUID = 1987640360028444299L;

      @Override
      public void valueChange(ValueChangeEvent event) {
        alternativeBox.setVisible(optionBox.getValue().equals(altBoxKeyword));
        alternativeField.setVisible(optionBox.getValue().equals(altFieldKeyword));
      }
    });

    alternativeBox = new ComboBox(altBoxCaption, altOptions);
    alternativeBox.setStyleName(Styles.boxTheme);
    alternativeBox.setImmediate(true);
    alternativeBox.setVisible(false);
    alternativeBox.setFilteringMode(FilteringMode.CONTAINS);
    addComponent(alternativeBox);
    alternativeField = new TextField(altFieldCaption);
    alternativeField.setWidth("350px");
    alternativeField.setStyleName(Styles.fieldTheme);
    alternativeField.setVisible(false);
    addComponent(alternativeField);
  }

  public boolean isValid() {
    if (optionBox.getValue() != null) {
      String val = optionBox.getValue().toString();
      if (val.equals(altFieldKeyword))
        return !alternativeField.getValue().isEmpty();
      else
        return !val.isEmpty();
    }
    return false;
  }

  public String getValue() {
    String val = optionBox.getValue().toString();
    if (val.equals(altFieldKeyword))
      return alternativeField.getValue();
    else if (val.equals(altBoxKeyword))
      if (alternativeBox.getValue() != null)
        return alternativeBox.getValue().toString();
    return val;
  }

  public void addValueChangeListener(ValueChangeListener l) {
    optionBox.addValueChangeListener(l);
    alternativeBox.addValueChangeListener(l);
    alternativeField.addValueChangeListener(l);
  }

  public TissueInfo getInfoComponent() {
    String first = optionBox.getValue().toString();
    if (first.equals(getValue()))
      return new TissueInfo(first, "");
    else
      return new TissueInfo(first, getValue());
  }

}
