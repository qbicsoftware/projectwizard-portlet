package life.qbic.projectwizard.model;

import java.util.List;

public class MHCTyping {

  private List<String> classOne;
  private List<String> classTwo;

  public MHCTyping(List<String> c1, List<String> c2) {
    classOne = c1;
    classTwo = c2;
  }

  public List<String> getClassOne() {
    return classOne;
  }

  public List<String> getClassTwo() {
    return classTwo;
  }
}
