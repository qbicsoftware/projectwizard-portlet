package life.qbic.portlet.io;

public enum TissueClass {

  Plants("BTO_0001481"), Animals("BTO_0000042"), Fungi("BTO_0001494");
  private final String BTO_TERM;

  private TissueClass(String stringVal) {
    BTO_TERM = stringVal;
  }

  public String toString() {
    return BTO_TERM;
  }

  public static String getEnumByString(String code) {
    for (TissueClass e : TissueClass.values()) {
      if (code == e.BTO_TERM)
        return e.name();
    }
    return null;
  }


}
