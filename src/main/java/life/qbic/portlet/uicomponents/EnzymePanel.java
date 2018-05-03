package life.qbic.portlet.uicomponents;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;

import life.qbic.portlet.uicomponents.Styles;

public class EnzymePanel extends VerticalLayout {

  private List<String> enzymes;
  private List<EnzymeChooser> choosers;
  private GridLayout buttonGrid;
  private Button add;
  private Button remove;
  private Button.ClickListener buttonListener;

  public EnzymePanel(List<String> enzymes) {
    this.enzymes = enzymes;
    add = new Button();
    remove = new Button();
    Styles.iconButton(add, FontAwesome.PLUS_SQUARE);
    Styles.iconButton(remove, FontAwesome.MINUS_SQUARE);
    initListener();

    choosers = new ArrayList<EnzymeChooser>();
    EnzymeChooser c = new EnzymeChooser(enzymes);
    choosers.add(c);

    setCaption("Digestion Enzymes");
    addComponent(c);
    buttonGrid = new GridLayout(2, 1);
    buttonGrid.setSpacing(true);
    buttonGrid.addComponent(add);
    buttonGrid.addComponent(remove);
    addComponent(buttonGrid);
    setSpacing(true);
  }

  private void initListener() {
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
    add.addClickListener(buttonListener);
    remove.addClickListener(buttonListener);
  }


  public List<String> getEnzymes() {
    List<String> res = new ArrayList<String>();
    for (EnzymeChooser c : choosers) {
      if (c.isSet())
        res.add(c.getEnzyme());
    }
    return res;
  }

  private void add() {
    if (choosers.size() < 4) {
      EnzymeChooser c = new EnzymeChooser(enzymes);
      choosers.add(c);

      removeComponent(buttonGrid);
      addComponent(c);
      addComponent(buttonGrid);
    }
  }

  private void remove() {
    int size = choosers.size();
    if (size > 1) {
      EnzymeChooser last = choosers.get(size - 1);
      last.reset();
      removeComponent(last);
      choosers.remove(last);
    }
  }

  public void resetInputs() {
    for (EnzymeChooser c : choosers) {
      c.reset();
    }
  }

}
