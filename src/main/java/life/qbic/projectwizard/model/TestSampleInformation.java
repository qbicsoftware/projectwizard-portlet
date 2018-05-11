package life.qbic.projectwizard.model;

public class TestSampleInformation {

  private String technology;
  private boolean pool;
  private int replicates;
  private String person;

  public TestSampleInformation(String tech, boolean pool, int reps, String person) {
    this.replicates = reps;
    this.pool = pool;
    this.person = person;
    this.technology = tech;
  }

  public String getTechnology() {
    return technology;
  }

  public boolean isPooled() {
    return pool;
  }

  public int getReplicates() {
    return replicates;
  }

  public String getPerson() {
    return person;
  }

  @Override
  public String toString() {
    String res = technology;
    if (pool)
      res += " (pooled)";
    res += "\n" + Integer.toString(replicates) + " replicates";
    res += "\n" + person;
    return res;
  }

}
