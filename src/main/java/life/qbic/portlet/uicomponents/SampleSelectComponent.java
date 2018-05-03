package life.qbic.portlet.uicomponents;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class SampleSelectComponent extends HorizontalLayout {

  private Button select;
  private Table table;
  private Object rowID;
  private Label label;
  private AbstractComponent infoComponent;
  private ValueChangeListener listener;

  public SampleSelectComponent() {
    label = new Label("", Label.CONTENT_XHTML);
    setWidth(label.getWidth() - 5, label.getWidthUnits());
    addComponent(label);
    setExpandRatio(label, 1);

    select = new Button();
    Styles.iconButton(select, FontAwesome.PLUS_CIRCLE);
    select.setStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    VerticalLayout vBox = new VerticalLayout();
    vBox.setWidth("15px");
    vBox.addComponent(select);
    addComponent(vBox);
    setComponentAlignment(vBox, Alignment.BOTTOM_RIGHT);
  }

  public void setSample(AbstractComponent infoComponent, Table t, Object rowID) {
    Styles.iconButton(select, FontAwesome.TIMES_CIRCLE);
    if (infoComponent instanceof Label) {
      Label input = (Label) infoComponent;
      label.setValue(input.getValue());
    }
    if (infoComponent instanceof TextField) {
      TextField input = (TextField) infoComponent;
      label.setValue(input.getValue());
      listener = new ValueChangeListener() {
        @Override
        public void valueChange(ValueChangeEvent event) {
          label.setValue(input.getValue());
        }
      };
      input.addValueChangeListener(listener);
    }
    table = t;
    this.rowID = rowID;
    this.infoComponent = infoComponent;
  }

  private void removeSampleListener() {
    if (infoComponent instanceof TextField) {
      TextField input = (TextField) infoComponent;
      input.removeValueChangeListener(listener);
    }
  }

  public Table getTable() {
    return table;
  }
  
  public Object getRowID() {
    return rowID;
  }

  public Button getSelectButton() {
    return select;
  }

  public void reset() {
    Styles.iconButton(select, FontAwesome.QUESTION_CIRCLE);
    if (this.infoComponent != null) {
      removeSampleListener();
      label.setValue("");
    }
  }

  public boolean isAttachedToSample() {
    return infoComponent != null;
  }

}
