package life.qbic.portlet.model;

import java.util.ArrayList;
import java.util.List;

public class MSExperimentModel {

  private List<List<ExperimentModel>> analytesPerStep;
  private List<List<ExperimentModel>> msRunsPerStep;
  private List<ExperimentModel> peptides;
  private ExperimentModel baseAnalytes;

  public MSExperimentModel() {
    this.analytesPerStep = new ArrayList<List<ExperimentModel>>();
    this.msRunsPerStep = new ArrayList<List<ExperimentModel>>();
    this.peptides = new ArrayList<ExperimentModel>();
  }

  public MSExperimentModel(MSExperimentModel base) {
    this.analytesPerStep = new ArrayList<List<ExperimentModel>>(base.analytesPerStep);
    this.msRunsPerStep = new ArrayList<List<ExperimentModel>>(base.msRunsPerStep);
    this.baseAnalytes = new ExperimentModel(base.baseAnalytes);
    this.peptides = new ArrayList<ExperimentModel>(base.peptides);
  }

  public List<ExperimentModel> getLastStepAnalytes() {
    if (analytesPerStep.isEmpty())
      return null;
    else
      return analytesPerStep.get(analytesPerStep.size() - 1);
  }

  public List<ExperimentModel> getLastStepMsRuns() {
    return msRunsPerStep.get(msRunsPerStep.size() - 1);
  }

  public void addAnalyteStepExperiments(List<ExperimentModel> exps) {
    analytesPerStep.add(exps);
  }

  public void addMSRunStepExperiments(List<ExperimentModel> exps) {
    msRunsPerStep.add(exps);
  }

  public List<List<ExperimentModel>> getAnalytes() {
    return analytesPerStep;
  }

  public List<List<ExperimentModel>> getMSRuns() {
    return msRunsPerStep;
  }

  public List<ExperimentModel> getPeptideExperiments() {
    return peptides;
  }

  public void addDigestionExperiment(List<ExperimentModel> exps) {
    this.peptides.addAll(exps);
  }

  public void setBaseAnalytes(ExperimentModel baseAnalytes) {
    this.baseAnalytes = baseAnalytes;
  }

  public ExperimentModel getBaseAnalytes() {
    return baseAnalytes;
  }
}
