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
package life.qbic.projectwizard.control;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
// import org.isatools.isacreator.model.Study;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.validator.CompositeValidator;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload.FinishedEvent;
import com.vaadin.ui.Upload.FinishedListener;
import com.wcs.wcslib.vaadin.widget.multifileupload.ui.AllUploadFinishedHandler;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Experiment;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Project;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.experiments.OpenbisExperiment;
import life.qbic.datamodel.identifiers.ExperimentCodeFunctions;
import life.qbic.datamodel.identifiers.SampleCodeFunctions;
import life.qbic.datamodel.identifiers.TooManySamplesException;
import life.qbic.datamodel.persons.PersonType;
import life.qbic.datamodel.projects.ProjectInfo;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.expdesign.ParserHelpers;
import life.qbic.expdesign.SamplePreparator;
import life.qbic.expdesign.VocabularyValidator;
import life.qbic.expdesign.io.EasyDesignReader;
import life.qbic.expdesign.io.IExperimentalDesignReader;
import life.qbic.expdesign.io.MHCLigandDesignReader;
import life.qbic.expdesign.io.MSDesignReader;
import life.qbic.expdesign.io.QBiCDesignReader;
import life.qbic.expdesign.model.ExperimentalDesignPropertyWrapper;
import life.qbic.expdesign.model.ExperimentalDesignType;
import life.qbic.expdesign.model.SampleSummaryBean;
import life.qbic.expdesign.model.StructuredExperiment;
// import life.qbic.isatab.ISAReader;
// import life.qbic.isatab.ISAStudyInfos;
// import life.qbic.isatab.ISAToQBIC;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.projectwizard.io.DBManager;
import life.qbic.projectwizard.model.MHCTyping;
import life.qbic.projectwizard.model.Vocabularies;
import life.qbic.projectwizard.processes.RegisteredSamplesReadyRunnable;
// import life.qbic.projectwizard.processes.ISAParseReady;
import life.qbic.projectwizard.registration.IOpenbisCreationController;
import life.qbic.projectwizard.uicomponents.MissingInfoComponent;
import life.qbic.projectwizard.uicomponents.ProjectInformationComponent;
import life.qbic.projectwizard.views.ExperimentImportView;
import life.qbic.xml.study.TechnologyType;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.projectwizard.uicomponents.MultiUploadComponent;


public class ExperimentImportController implements IRegistrationController {

  private ExperimentImportView view;
  private final Uploader uploader = new Uploader();
  private IOpenbisCreationController openbisCreator;
  private SamplePreparator prep;
  //
  private ProjectInfo projectInfo;
  private List<Map<String, Object>> metadataList;
  private Map<String, Map<String, Object>> msProperties;
  private Map<String, Map<String, Object>> mhcProperties;
  private Map<String, Map<String, Object>> samplePrepProperties;
  private Map<String, MHCTyping> dnaSampleCodeToMHCType;
  private Map<String, Sample> uniqueIDToSample;
  private List<OpenbisExperiment> complexExperiments;

  private Map<String, String> reverseTaxMap;
  private Map<String, String> reverseTissueMap;
  private MissingInfoComponent questionaire;
  private IOpenBisClient openbis;
  private DBManager dbm;
  private Vocabularies vocabs;
  private Experiment currentDesignExperiment;
  private int firstFreeExperimentID;
  private int firstFreeEntityID;
  private String firstFreeBarcode;
  private String nextBarcode;
  private Map<String, String> uniqueCodeToBarcode;
  private Map<String, String> uniqueNumericIDToUniqueCode;

  private final Logger logger = LogManager.getLogger(ExperimentImportController.class);
  protected String experimentalDesignXML;
  private Map<String, Map<String, Object>> entitiesToUpdate;
  private ArrayList<Sample> currentProjectSamples;
  private ProjectInformationComponent projectInfoComponent;
  private Set<String> currentDesignTypes;
  // protected ISAStudyInfos isaStudyInfos;

  public ExperimentImportController(IOpenbisCreationController creationController,
      Vocabularies vocabularies, IOpenBisClient openbis, DBManager dbm) {
    view = new ExperimentImportView();
    this.dbm = dbm;
    this.questionaire = view.getMissingInfoComponent();
    this.vocabs = vocabularies;
    this.openbis = openbis;
    this.reverseTaxMap = new HashMap<String, String>();
    for (Map.Entry<String, String> entry : vocabs.getTaxMap().entrySet()) {
      this.reverseTaxMap.put(entry.getValue(), entry.getKey());
    }
    this.reverseTissueMap = new HashMap<String, String>();
    for (Map.Entry<String, String> entry : vocabs.getTissueMap().entrySet()) {
      this.reverseTissueMap.put(entry.getValue(), entry.getKey());
    }
    this.openbisCreator = creationController;
  }


  // public void initISAHandler(ISAReader isaParser, File folder) {
  // ComboBox isaStudyBox = view.getISAStudyBox();
  // isaStudyBox.addValueChangeListener(new ValueChangeListener() {
  //
  // @Override
  // public void valueChange(ValueChangeEvent event) {
  // Object study = isaStudyBox.getValue();
  // if (study != null) {
  // isaParser.selectStudyToParse(study.toString());
  // }
  // boolean readSuccess = false;
  // try {
  // prep = new SamplePreparator();
  // readSuccess = prep.processTSV(folder, isaParser, true);
  // } catch (IOException e) {
  // // TODO Auto-generated catch block
  // e.printStackTrace();
  // } catch (JAXBException e) {
  // // TODO Auto-generated catch block
  // e.printStackTrace();
  // }
  // if (readSuccess) {
  // isaStudyInfos = isaParser.getStudyInfos(study.toString());
  // handleImportResults(ExperimentalDesignType.ISA, prep.getSummary());
  // currentDesignTypes = new HashSet<>();
  // currentDesignTypes.addAll(isaStudyInfos.getDesignTypes());
  // projectInfoComponent.setDescription(isaStudyInfos.getDescription());
  // projectInfoComponent.setProjectName(isaStudyInfos.getTitle());
  // // TODO long information, studydesigns
  // } else {
  // String error = prep.getError();
  // Styles.notification("Failed to read ISA format.", error, NotificationType.ERROR);
  // }
  // }
  // });
  // }

  // public void isaPrepComplete(List<Study> studies, String error) {
  // if (error != null) {
  // Styles.notification("Failed to read ISA format.", error, NotificationType.ERROR);
  // view.resetFormatSelection();
  // view.listISAStudies(new ArrayList<Study>());
  // } else {
  // Styles.notification("Upload complete.",
  // "ISA-Tab has been successfully uploaded, please select a study.",
  // NotificationType.SUCCESS);
  // view.listISAStudies(studies);
  // }
  // }

  public void init(final String user, final String isaConfigPath) {
    ExperimentImportController control = this;
    Upload upload = new Upload("Upload your file here", uploader);
    MultiUploadComponent multiUpload = new MultiUploadComponent();
    // final ExperimentImportController controller = this;

    final AllUploadFinishedHandler allUploadFinishedHandler = new AllUploadFinishedHandler() {

      @Override
      public void finished() {
        Thread t = new Thread(new Runnable() {

          @Override
          public void run() {
            File folder = multiUpload.getISAFolder();
            // ISAReader isaParser = new ISAReader(isaConfigPath, new ISAToQBIC());
            String error = null;
            // List<Study> studies = new ArrayList<Study>();
            try {
              // studies = isaParser.listStudies(folder);
              // error = isaParser.getError();
            } catch (NullPointerException e) {
              error = "Investigation file not found or not the right format.";
            }
            if (error == null)
              // initISAHandler(isaParser, folder);

              // UI.getCurrent().access(new ISAParseReady(controller, studies, error));
              UI.getCurrent().setPollInterval(-1);
          }
        });
        UI.getCurrent().setPollInterval(10);
        t.start();

      }
    };
    multiUpload.setFinishedHandler(allUploadFinishedHandler);

    view.initView(upload, multiUpload);
    upload.setButtonCaption("Upload");
    // Listen for events regarding the success of upload.
    upload.addFailedListener(uploader);
    upload.addSucceededListener(uploader);
    FinishedListener uploadFinListener = new FinishedListener() {
      /**
       * 
       */
      private static final long serialVersionUID = -8413963075202260180L;

      public void uploadFinished(FinishedEvent event) {
        currentDesignTypes = new HashSet<>();
        String uploadError = uploader.getError();
        File file = uploader.getFile();
        view.resetAfterUpload();
        if (file.getPath().endsWith("up_")) {
          String msg = "No file selected.";
          logger.warn(msg);
          Styles.notification("Failed to read file.", msg, NotificationType.ERROR);
          if (!file.delete())
            logger.error("uploaded tmp file " + file.getAbsolutePath() + " could not be deleted!");
        } else {
          if (uploadError == null || uploadError.isEmpty()) {
            String msg = "Upload successful!";
            logger.info(msg);
            try {
              view.setRegEnabled(false);
              prep = new SamplePreparator();
              Map<String, Set<String>> experimentTypeVocabularies =
                  new HashMap<String, Set<String>>();
              experimentTypeVocabularies.put("Q_ANTIBODY", vocabs.getAntibodiesMap().keySet());
              experimentTypeVocabularies.put("Q_CHROMATOGRAPHY_TYPE",
                  new HashSet<String>(vocabs.getChromTypesMap().values()));
              experimentTypeVocabularies.put("Q_MS_DEVICE",
                  new HashSet<String>(vocabs.getDeviceMap().values()));
              experimentTypeVocabularies.put("Q_MS_LCMS_METHOD",
                  new HashSet<String>(vocabs.getLcmsMethods()));

              VocabularyValidator validator = new VocabularyValidator(experimentTypeVocabularies);

              IExperimentalDesignReader reader = null;
              boolean parseGraph = true;
              boolean experimentVocabCorrectionAllowed = false;
              switch (getImportType()) {
                case QBIC:
                  reader = new QBiCDesignReader();
                  break;
                case Standard:
                  reader = new EasyDesignReader();
                  break;
                case MHC_Ligands_Finished:
                  reader = new MHCLigandDesignReader();
                  break;
                case Proteomics_MassSpectrometry:
                  experimentVocabCorrectionAllowed = true;
                  reader = new MSDesignReader();
                default:
                  break;
              }

              boolean readSuccess = prep.processTSV(file, reader, parseGraph);
              boolean vocabValid = false;
              if (readSuccess) {
                samplePrepProperties = prep.getSpecialExperimentsOfTypeOrNull(
                    ExperimentType.Q_SAMPLE_PREPARATION.toString());
                msProperties = prep
                    .getSpecialExperimentsOfTypeOrNull(ExperimentType.Q_MS_MEASUREMENT.toString());
                mhcProperties = prep.getSpecialExperimentsOfTypeOrNull(
                    ExperimentType.Q_MHC_LIGAND_EXTRACTION.toString());
                metadataList = new ArrayList<Map<String, Object>>();
                if (samplePrepProperties != null)
                  metadataList.addAll(samplePrepProperties.values());
                if (msProperties != null)
                  metadataList.addAll(msProperties.values());
                if (mhcProperties != null)
                  metadataList.addAll(mhcProperties.values());
                vocabValid = validator.validateExperimentMetadata(metadataList);
              }
              if (readSuccess && (vocabValid || experimentVocabCorrectionAllowed)) {
                List<SampleSummaryBean> summaries = prep.getSummary();
                for (SampleSummaryBean s : summaries) {
                  String translation = reverseTaxMap.get(s.getFullSampleContent());
                  if (translation != null)
                    s.setSampleContent(translation);
                }
                Styles.notification("Upload successful",
                    "Experiment was successfully uploaded and read.", NotificationType.SUCCESS);
                handleImportResults(summaries);

              } else {
                if (!readSuccess) {
                  String error = prep.getError();
                  Styles.notification("Failed to read file.", error, NotificationType.ERROR);
                } else {
                  String error = validator.getError();
                  Styles.notification("Failed to process file.", error, NotificationType.ERROR);
                }
                if (!file.delete())
                  logger.error(
                      "uploaded tmp file " + file.getAbsolutePath() + " could not be deleted!");
              }
            } catch (IOException | JAXBException e) {
              e.printStackTrace();
            }
          } else {
            // view.showError(error);
            Styles.notification("Failed to upload file.", uploadError, NotificationType.ERROR);
            if (!file.delete())
              logger
                  .error("uploaded tmp file " + file.getAbsolutePath() + " could not be deleted!");
          }
        }
      }
    };
    upload.addFinishedListener(uploadFinListener);
    // view.initUpload(upload);

    Button.ClickListener cl = new Button.ClickListener() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      /**
       * 
       */

      @Override
      public void buttonClick(ClickEvent event) {
        String src = event.getButton().getCaption();
        if (src.equals("Register All")) {
          List<List<ISampleBean>> samples = view.getSamples();
          view.getRegisterButton().setEnabled(false);
          view.showRegistrationProgress();
          // collect experiment information
          complexExperiments = new ArrayList<OpenbisExperiment>();
          if (entitiesToUpdate.isEmpty()) {
            complexExperiments.add(prepareXMLPropertyForNewExperiment(samples));
          }
          String space = projectInfo.getSpace();
          String project = projectInfo.getProjectCode();
          String infoExpCode = project + "_INFO";
          String code = project + "000";
          ISampleBean infoSample =
              new TSVSampleBean(code, infoExpCode, project, space, SampleType.Q_ATTACHMENT_SAMPLE,
                  "", new ArrayList<String>(), new HashMap<String, Object>());
          samples.add(new ArrayList<ISampleBean>(Arrays.asList(infoSample)));

          samplePrepProperties = fixSamplePrepProperties(samplePrepProperties);

          complexExperiments.addAll(
              collectComplexExperiments(samplePrepProperties, ExperimentType.Q_SAMPLE_PREPARATION));
          complexExperiments
              .addAll(collectComplexExperiments(msProperties, ExperimentType.Q_MS_MEASUREMENT));
          complexExperiments.addAll(
              collectComplexExperiments(mhcProperties, ExperimentType.Q_MHC_LIGAND_EXTRACTION));

          if (experimentalDesignXML != null) {
            logger.debug("set new xml: >" + experimentalDesignXML + "<");

            Map<String, Object> props = new HashMap<>();
            props.put("Q_EXPERIMENTAL_SETUP", experimentalDesignXML);
            complexExperiments
                .add(new OpenbisExperiment(infoExpCode, ExperimentType.Q_PROJECT_DETAILS, props));
          }
          List<String> tsv = prep.getOriginalTSV();
          switch (getImportType()) {
            case Standard:
            case MHC_Ligands_Finished:
            case Proteomics_MassSpectrometry:
              String tsvContent = addBarcodesToTSV(tsv, view.getSamples(), getImportType());
              view.setTSVWithBarcodes(tsvContent,
                  uploader.getFileNameWithoutExtension() + "_with_barcodes");
              break;
            default:
              break;
          }
          openbisCreator.registerProjectWithExperimentsAndSamplesBatchWise(samples,
              projectInfo.getDescription(), complexExperiments, view.getProgressBar(),
              view.getProgressLabel(), new RegisteredSamplesReadyRunnable(view, control),
              entitiesToUpdate, projectInfo.isPilot());

        }
      }

      private Map<String, Map<String, Object>> fixSamplePrepProperties(
          Map<String, Map<String, Object>> samplePrepProperties) {
        Map<String, Map<String, Object>> res = new HashMap<>();
        for (String exp : samplePrepProperties.keySet()) {
          Map<String, Object> props = samplePrepProperties.get(exp);
          String digestionInfo = "";
          if (props.containsKey("Q_DIGESTION_METHOD")) {
            digestionInfo = (String) props.get("Q_DIGESTION_METHOD");
            // TODO translate to humanly readable version
          }
          if (props.containsKey("Q_DIGESTION_ENZYMES")) {
            List<String> enzymes = (List<String>) props.get("Q_DIGESTION_ENZYMES");
            String enzymesString = String.join(", ", enzymes);
            if (!digestionInfo.isEmpty()) {
              digestionInfo += ": ";
            }
            digestionInfo += enzymesString;
          }
          if (!digestionInfo.isEmpty()) {
            props.put("Q_DIGESTION_METHOD", digestionInfo);
          }
          props.remove("Q_DIGESTION_ENZYMES");
          res.put(exp, props);
        }
        return res;
      }

      private Collection<? extends OpenbisExperiment> collectComplexExperiments(
          Map<String, Map<String, Object>> propsMap, ExperimentType type) {
        List<OpenbisExperiment> res = new ArrayList<OpenbisExperiment>();
        if (propsMap != null) {
          for (String code : propsMap.keySet()) {
            res.add(new OpenbisExperiment(code, type, propsMap.get(code)));
          }
        }
        return res;
      }
    };
    view.getRegisterButton().addClickListener(cl);
  }

  // TODO this should be done while the samples are read
  protected OpenbisExperiment prepareXMLPropertyForNewExperiment(List<List<ISampleBean>> samples) {
    ExperimentType designExpType = ExperimentType.Q_PROJECT_DETAILS;
    List<ISampleBean> lst = samples.stream().flatMap(x -> x.stream()).collect(Collectors.toList());
    String experiment = null;
    for (ISampleBean s : lst) {
      String project = s.getProject();
      experiment = project + "_INFO";
      break;
    }

    Map<String, Object> propsMap = new HashMap<>();
    propsMap.put("Q_EXPERIMENTAL_SETUP", experimentalDesignXML);
    return new OpenbisExperiment(experiment, designExpType, propsMap);
  }


  protected void handleImportResults(List<SampleSummaryBean> summaries) {
    switch (getImportType()) {
      // Standard hierarchic QBiC design
      case QBIC:
        view.setSummary(summaries);
        view.setProcessed(prep.getProcessed());
        view.setRegEnabled(true);
        projectInfo = prep.getProjectInfo();
        findFirstExistingDesignExperimentCodeOrNull(projectInfo.getSpace(),
            projectInfo.getProjectCode());
        prepDesignXML(prep.getTechnologyTypes());
        break;
      // Standard non-hierarchic design without QBiC specific keywords
      case Standard:
        prepareCompletionDialog();
        StructuredExperiment nodes = prep.getSampleGraph();
        if (!nodes.getFactorsToSamples().keySet().isEmpty())
          view.initGraphPreview(nodes, prep.getIDsToSamples());
        break;
      case ISA:
        prepareCompletionDialog();
        Map<String, ISampleBean> idsToSamples = prep.getIDsToSamples();
        view.initGraphPreview(prep.getSampleGraph(), idsToSamples);
        // // MHC Ligands that have already been measured (Filenames exist)
      case MHC_Ligands_Finished:
      case Proteomics_MassSpectrometry:
        prepareCompletionDialog();
        break;
      default:
        logger.error("Error parsing tsv: " + prep.getError());
        Styles.notification("Failed to read file.", prep.getError(), NotificationType.ERROR);
        break;
    }
  }

  private void prepDesignXML(List<TechnologyType> techTypes) {
    Map<String, String> uniqueNumericToBarcode = new HashMap<>();
    for (String num : uniqueNumericIDToUniqueCode.keySet()) {
      uniqueNumericToBarcode.put(num,
          uniqueCodeToBarcode.get(uniqueNumericIDToUniqueCode.get(num)));
    }

    // create Experimental Design XML, first reset both values
    experimentalDesignXML = null;
    entitiesToUpdate = new HashMap<String, Map<String, Object>>();

    ExperimentalDesignPropertyWrapper importedDesignProperties =
        prep.getExperimentalDesignProperties();
    if (uniqueCodeToBarcode != null) {
      Map<String, String> codeMap = uniqueCodeToBarcode;
      if (!uniqueNumericIDToUniqueCode.isEmpty()) {
        codeMap = uniqueNumericToBarcode;
      }
      ParserHelpers.translateIdentifiersInExperimentalDesign(codeMap, importedDesignProperties);
    }
    if (currentDesignExperiment != null) {
      Map<String, String> currentProps = currentDesignExperiment.getProperties();
      Map<String, Object> map = ParserHelpers.getExperimentalDesignMap(currentProps,
          importedDesignProperties, techTypes, currentDesignTypes);
      final String SETUP_PROPERTY_CODE = "Q_EXPERIMENTAL_SETUP";
      String oldXML = currentProps.get(SETUP_PROPERTY_CODE);
      if (!map.get(SETUP_PROPERTY_CODE).equals(oldXML)) {
        logger.info("update of experimental design needed");
        entitiesToUpdate.put(currentDesignExperiment.getCode(), map);
      } else {
        logger.info("no update of existing experimental design needed");
      }
    } else {
      try {
        logger.info("creating new experimental design");
        experimentalDesignXML =
            ParserHelpers.createDesignXML(importedDesignProperties, techTypes, currentDesignTypes);
      } catch (JAXBException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  private void findFirstExistingDesignExperimentCodeOrNull(String space, String project) {
    String expID = ExperimentCodeFunctions.getInfoExperimentID(space, project);
    List<Experiment> experiments = openbis.getExperimentById2(expID);
    // reset
    currentDesignExperiment = null;
    for (Experiment e : experiments) {
      currentDesignExperiment = e;
    }
  }

  private void prepareCompletionDialog() {
    Map<String, Map<String, String>> catToVocabulary = new HashMap<>();
    catToVocabulary.put("Species", vocabs.getTaxMap());
    catToVocabulary.put("Tissue", vocabs.getTissueMap());
    Map<String, String> analytesMap = new HashMap<>();
    for (String a : vocabs.getAnalyteTypes()) {
      analytesMap.put(a, a);
    }
    catToVocabulary.put("Analyte", analytesMap);

    Map<String, List<String>> parsedCategoryToValues = new HashMap<>();

    // allow users to correct
    if (getImportType().equals(ExperimentalDesignType.Proteomics_MassSpectrometry)) {

      // Map<String, Set<String>> pretransformedProperties = new HashMap<>();
      //
      // vocabValid = validator.transformAndValidateExperimentMetadata(metadataList,
      // pretransformedProperties);
      logger.info("Before replacement");
      logger.info(metadataList);

      //// Fractionation Type : Q_MS_FRACTIONATION_METHOD : Q_MS_FRACTIONATION_PROTOCOLS
      Map<String, String> fracMap = new HashMap<>();
      for (String method : vocabs.getFractionationTypes()) {
        fracMap.put(method, method);
      }
      catToVocabulary.put("Fractionation Type", fracMap);
      //// Enrichment Method : Q_MS_ENRICHMENT_METHOD : Q_MS_ENRICHMENT_PROTOCOLS
      Map<String, String> enrichMap = new HashMap<>();
      for (String method : vocabs.getEnrichmentTypes()) {
        enrichMap.put(method, method);
      }
      catToVocabulary.put("Enrichment Method", enrichMap);
      //// Digestion Enzyme : (Q_DIGESTION_METHOD) : Q_DIGESTION_PROTEASES
      Map<String, String> enzymeMap = new HashMap<>();
      for (String e : vocabs.getEnzymes()) {
        enzymeMap.put(e, e);
      }
      catToVocabulary.put("Digestion Enzyme", enzymeMap);
      //// LC Column : Q_CHROMATOGRAPHY_TYPE : Q_CHROMATOGRAPHY_TYPES
      catToVocabulary.put("LC Column", vocabs.getChromTypesMap());
      //// LCMS Method : Q_MS_LCMS_METHOD : Q_MS_LCMS_METHODS
      Map<String, String> lcmsMap = new HashMap<>();
      for (String method : vocabs.getLcmsMethods()) {
        lcmsMap.put(method, method);
      }
      catToVocabulary.put("LCMS Method", lcmsMap);
      //// MC Device : Q_MS_DEVICE : Q_MS_DEVICES
      catToVocabulary.put("MS Device", vocabs.getDeviceMap());
      //// Sample Cleanup (peptide) : Q_PROTEIN_PURIFICATION_METHODS
      //// Sample Cleanup (protein) : Q_PROTEIN_PURIFICATION_METHODS
      //// TODO
      //// Expression System : Q_EXPRESSION_SYSTEM = Q_NCBI_TAXONOMY
      catToVocabulary.put("Expression System", vocabs.getTaxMap());
      //// Digestion Method : (Q_DIGESTION_METHOD) : Q_DIGESTION_PROTOCOL
      catToVocabulary.put("Digestion Method", vocabs.getDigestionMethodsMap());
      //// Labeling Type : Q_LABELING_METHOD : Q_LABELING_TYPES
      Map<String, String> labelingMap = new HashMap<>();
      for (String label : vocabs.getLabelingTypes()) {
        labelingMap.put(label, label);
      }
      catToVocabulary.put("Labeling Type", labelingMap);
      //// Sample Preparation
      Map<String, String> samplePrepMap = vocabs.getSamplePreparationMethods();
      catToVocabulary.put("Sample Preparation", samplePrepMap);

      parsedCategoryToValues = prep.getParsedCategoriesToValues(
          new ArrayList<String>(Arrays.asList("Expression System", "LC Column", "MS Device",
              "Fractionation Type", "Enrichment Method", "Labeling Type", "LCMS Method",
              "Digestion Method", "Digestion Enzyme", "Sample Preparation", "Species", "Tissue")));
    }

    if (!parsedCategoryToValues.containsKey("Species"))
      parsedCategoryToValues.put("Species", new ArrayList<String>(prep.getSpeciesSet()));
    if (!parsedCategoryToValues.containsKey("Analyte"))
      parsedCategoryToValues.put("Analyte", new ArrayList<String>(prep.getAnalyteSet()));
    if (!parsedCategoryToValues.containsKey("Tissue"))
      parsedCategoryToValues.put("Tissue", new ArrayList<String>(prep.getTissueSet()));

    initMissingInfoListener(parsedCategoryToValues, catToVocabulary);
  }

  protected String addBarcodesToTSV(List<String> tsv, List<List<ISampleBean>> levels,
      ExperimentalDesignType designType) {
    Set<String> barcodeColumnNames = new HashSet<>(Arrays.asList("QBiC Code", "QBiC Barcode"));
    logger.info("adding barcodes to tsv");
    logger.info("design type: " + designType);
    String fileNameHeader = "Filename";
    StringBuilder builder = new StringBuilder(5000);
    switch (designType) {
      case Standard:
        int anltIDPos = -1;
        int extIDPos = -1;
        for (String line : tsv) {
          String[] splt = line.split("\t");
          if (anltIDPos < 0) {
            anltIDPos = Arrays.asList(splt).indexOf("Analyte ID");
            extIDPos = Arrays.asList(splt).indexOf("Extract ID");
            builder.append("QBiC Code\t" + line + "\n");
          } else {
            String extID = splt[anltIDPos];
            if (extID == null || extID.isEmpty())
              extID = splt[extIDPos];
            String code = uniqueCodeToBarcode.get(extID);
            builder.append(code + "\t" + line + "\n");
          }
        }
        break;
      case Proteomics_MassSpectrometry:
        fileNameHeader = "File Name";
      case MHC_Ligands_Finished:
        Map<String, String> fileNameToBarcode = new HashMap<String, String>();
        for (List<ISampleBean> samples : levels) {
          for (ISampleBean s : samples) {
            if (s.getType().equals(SampleType.Q_MS_RUN)) {
              Map<String, Object> props = s.getMetadata();
              fileNameToBarcode.put(props.get("File").toString(), s.getCode());
              props.remove("File");
            }
          }
        }
        int filePos = -1;
        for (String line : tsv) {
          String[] splt = line.split("\t");
          boolean colExists = barcodeColumnNames.contains(splt[0]);
          if (filePos < 0) {
            filePos = Arrays.asList(splt).indexOf(fileNameHeader);
            if (colExists) {
              builder.append(line + "\n");
            } else {
              builder.append("QBiC Code\t" + line + "\n");
            }
          } else {
            String file = splt[filePos];
            String code = fileNameToBarcode.get(file);
            if (colExists) {
              builder.append(code + line + "\n");
            } else {
              builder.append(code + "\t" + line + "\n");
            }
          }
        }
      default:
        break;
    }
    System.out.println(builder.toString());
    return builder.toString();
  }

  protected void initMissingInfoListener(Map<String, List<String>> parsedCategoryToValues,
      Map<String, Map<String, String>> catToVocabulary) {
    uniqueCodeToBarcode = new HashMap<>();
    uniqueNumericIDToUniqueCode = new HashMap<>();

    projectInfoComponent =
        new ProjectInformationComponent(vocabs.getSpaces(), vocabs.getPeople().keySet());

    ValueChangeListener missingInfoFilledListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        boolean overflow = false;
        boolean infoComplete = questionaire.isValid();
        boolean samplesToRegister = false;
        if (infoComplete) {

          // change summary information
          List<SampleSummaryBean> summaries = prep.getSummary();
          for (SampleSummaryBean b : summaries) {
            String cat = "";
            if (b.getSampleType().contains("Source"))
              cat = "Species";
            else if (b.getSampleType().contains("Sample Extract"))
              cat = "Tissue";
            else if (b.getSampleType().contains("Preparations"))
              cat = "Analyte";
            if (parsedCategoryToValues.containsKey(cat)) {
              String val = b.getFullSampleContent();
              List<String> newVal = new ArrayList<String>();
              for (String v : val.split(", ")) {
                v = v.trim();
                String translation = questionaire.getVocabularyLabelForValue(cat, v);
                if (translation == null)
                  translation = reverseTissueMap.get(v);
                if (translation == null)
                  translation = v;
                newVal.add(translation);
              }
              b.setSampleContent(StringUtils.join(newVal, ", "));
            }
          }
          // TODO enable for all types
          if (getImportType().equals(ExperimentalDesignType.Proteomics_MassSpectrometry)) {
            Map<String, Set<String>> keyToFields = new HashMap<>();

            // sample prep experiments
            keyToFields.put("Q_SAMPLE_PREPARATION_METHOD",
                new HashSet<>(Arrays.asList("Sample Preparation")));
            keyToFields.put("Q_MS_FRACTIONATION_METHOD",
                new HashSet<>(Arrays.asList("Fractionation Type")));
            keyToFields.put("Q_MS_ENRICHMENT_METHOD",
                new HashSet<>(Arrays.asList("Enrichment Method")));
            keyToFields.put("Q_LABELING_METHOD", new HashSet<>(Arrays.asList("Labeling Type")));
            keyToFields.put("Q_DIGESTION_METHOD", new HashSet<>(Arrays.asList("Digestion Method")));
            keyToFields.put("Q_DIGESTION_ENZYMES",
                new HashSet<>(Arrays.asList("Digestion Enzyme")));
            // ms experiments
            keyToFields.put("Q_MS_DEVICE", new HashSet<>(Arrays.asList("MS Device")));
            keyToFields.put("Q_CHROMATOGRAPHY_TYPE", new HashSet<>(Arrays.asList("LC Column")));
            keyToFields.put("Q_MS_LCMS_METHOD", new HashSet<>(Arrays.asList("LCMS Method")));
            // keyToFields.put("Q_MS_PURIFICATION_METHOD",
            // new HashSet<>(Arrays.asList("Sample Cleanup (protein)")));
            // keyToFields.put("Q_MS_PURIFICATION_METHOD",
            // new HashSet<>(Arrays.asList("Sample Cleanup (peptide)")));

            for (Map<String, Object> props : metadataList) {
              Map<String, String> newProps = new HashMap<>();
              for (String key : props.keySet()) {
                if (keyToFields.containsKey(key)) {
                  for (String vocabCode : keyToFields.get(key)) {
                    if (props.get(key) instanceof String) {
                      String entry = (String) props.get(key);
                      // String newLabel = questionaire.getVocabularyLabelForValue(val, entry);//
                      // TODO?
                      // System.out.println(newLabel);
                      String newVal = questionaire.getVocabularyCodeForValue(vocabCode, entry);
                      if (newVal != null) {
                        props.put(key, newVal);
                      }
                    } else if (props.get(key) instanceof List<?>) {
                      List<String> newPropList = new ArrayList<>();
                      List<String> propList = (List<String>) (List<?>) props.get(key);
                      for (String entry : propList) {
                        String newEntry = questionaire.getVocabularyCodeForValue(vocabCode, entry);
                        if (newEntry != null) {
                          newPropList.add(newEntry);
                        } else {
                          newPropList.add(entry);
                        }
                      }
                      props.put(key, newPropList);
                    }
                  }
                }
              }
              for (String newProp : newProps.keySet()) {
                props.put(newProp, newProps.get(newProp));
              }
            }
            logger.info("after replacement:");
            logger.info(metadataList);
          }
          view.setSummary(summaries);
          projectInfo = projectInfoComponent.getProjectInfo();
          String space = questionaire.getSpaceCode();
          String project = questionaire.getProjectCode();
          try {
            countExistingOpenbisEntities(space, project);
          } catch (TooManySamplesException e1) {
            // TODO Auto-generated catch block
            overflow = true;
          }

          int entityNum = firstFreeEntityID;
          nextBarcode = null;

          List<List<ISampleBean>> processed = prep.getProcessed();
          Set<String> msCodes = new HashSet<String>();
          dnaSampleCodeToMHCType = new HashMap<String, MHCTyping>();
          Map<String, String> specialExpToExpCode = new HashMap<String, String>();
          Set<TechnologyType> techTypes = new HashSet<>();
          techTypes.addAll(prep.getTechnologyTypes());

          for (List<ISampleBean> level : processed) {
            SampleType type = level.get(0).getType();
            String exp = "";
            if (!type.equals(SampleType.Q_MS_RUN) && !type.equals(SampleType.Q_MHC_LIGAND_EXTRACT))
              exp = getNextExperiment(project);// TODO
            // list of existing samples to be removed before registration
            List<ISampleBean> existing = new ArrayList<ISampleBean>();
            for (ISampleBean b : level) {
              TSVSampleBean t = (TSVSampleBean) b;

              // TODO do this in the parser
              Object extID = t.getMetadata().get("Q_EXTERNALDB_ID");
              boolean noExtID = extID == null || ((String) extID).isEmpty();
              if (t.getType().equals(SampleType.Q_MS_RUN) && noExtID) {
                t.getMetadata().put("Q_EXTERNALDB_ID", t.getMetadata().get("Q_EXTERNALDB_ID"));
              }

              String uniqueID = createUniqueIDFromSampleMetadata(t);

              if (uniqueIDToSample.containsKey(uniqueID)) {
                existing.add(t);
                uniqueCodeToBarcode.put(uniqueID, uniqueIDToSample.get(uniqueID).getCode());
                uniqueNumericIDToUniqueCode.put(t.getCode(), uniqueID);
              } else {
                t.setProject(project);
                t.setSpace(space);
                String code = "";
                Map<String, Object> props = t.getMetadata();
                switch (t.getType()) {
                  case Q_BIOLOGICAL_ENTITY:
                    code = project + "ENTITY-" + entityNum;
                    String newVal = questionaire.getVocabularyLabelForValue("Species",
                        props.get("Q_NCBI_ORGANISM"));
                    props.put("Q_NCBI_ORGANISM", vocabs.getTaxMap().get(newVal));

                    if (props.containsKey("Q_EXPRESSION_SYSTEM")) {
                      if (!props.get("Q_EXPRESSION_SYSTEM").equals("")) {
                        System.out.println("XXXX");
                        System.out.println("expression system contained");
                        String newExprVal = questionaire.getVocabularyLabelForValue(
                            "Expression System", props.get("Q_EXPRESSION_SYSTEM"));
                        props.put("Q_EXPRESSION_SYSTEM", vocabs.getTaxMap().get(newExprVal));
                      }
                    }
                    entityNum++;
                    break;
                  case Q_BIOLOGICAL_SAMPLE:
                    try {
                      incrementOrCreateBarcode(project);
                    } catch (TooManySamplesException e) {
                      overflow = true;
                    }
                    code = nextBarcode;

                    newVal = questionaire.getVocabularyLabelForValue("Tissue",
                        props.get("Q_PRIMARY_TISSUE"));

                    props.put("Q_PRIMARY_TISSUE", vocabs.getTissueMap().get(newVal));
                    break;
                  case Q_TEST_SAMPLE:
                    try {
                      incrementOrCreateBarcode(project);
                    } catch (TooManySamplesException e) {
                      overflow = true;
                    }
                    code = nextBarcode;
                    if (!specialExpToExpCode.containsKey(t.getExperiment())) {
                      specialExpToExpCode.put(t.getExperiment(), getNextExperiment(project));
                    }
                    exp = specialExpToExpCode.get(t.getExperiment());

                    newVal = questionaire.getVocabularyLabelForValue("Analyte",
                        props.get("Q_SAMPLE_TYPE"));
                    props.put("Q_SAMPLE_TYPE", newVal);
                    // TODO check if this is not too unspecific for ligandomics
                    TechnologyType ttype = ParserHelpers.typeToTechnology.get(newVal);
                    if (ttype != null) {
                      techTypes.add(ParserHelpers.typeToTechnology.get(newVal));
                    }
                    if (getImportType().equals(ExperimentalDesignType.MHC_Ligands_Finished)) {
                      if ("DNA".equals(newVal)) {
                        List<String> c1 = (List<String>) props.get("MHC_I");
                        List<String> c2 = (List<String>) props.get("MHC_II");
                        dnaSampleCodeToMHCType.put(code, new MHCTyping(c1, c2));
                        props.remove("MHC_I");
                        props.remove("MHC_II");
                      }
                    }
                    break;
                  case Q_MHC_LIGAND_EXTRACT:
                    try {
                      incrementOrCreateBarcode(project);
                    } catch (TooManySamplesException e) {
                      overflow = true;
                    }
                    code = nextBarcode;
                    if (!specialExpToExpCode.containsKey(t.getExperiment())) {
                      specialExpToExpCode.put(t.getExperiment(), getNextExperiment(project));
                    }
                    exp = specialExpToExpCode.get(t.getExperiment());
                    break;
                  case Q_MS_RUN:
                    // get ms experiment to connect it correctly
                    if (!specialExpToExpCode.containsKey(t.getExperiment())) {
                      specialExpToExpCode.put(t.getExperiment(), getNextExperiment(project));
                    }
                    exp = specialExpToExpCode.get(t.getExperiment());
                    // get parent sample for code
                    String parentID = t.getParentIDs().get(0);
                    if (!uniqueCodeToBarcode.containsKey(parentID)) {
                      parentID = uniqueNumericIDToUniqueCode.get(parentID);
                    }
                    String parentCode = uniqueCodeToBarcode.get(parentID);

                    int msRun = 1;
                    code = "";
                    while (code.isEmpty() || msCodes.contains(code)) {
                      code = "MS" + Integer.toString(msRun) + parentCode;
                      msRun++;
                    }
                    msCodes.add(code);
                    break;
                  case Q_ATTACHMENT_SAMPLE:
                    break;
                  case Q_BMI_GENERIC_IMAGING_RUN:
                    break;
                  case Q_EDDA_BENCHMARK:
                    break;
                  case Q_EXT_MS_QUALITYCONTROL_RUN:
                    break;
                  case Q_EXT_NGS_QUALITYCONTROL_RUN:
                    break;
                  case Q_FASTA:
                    break;
                  case Q_HT_QPCR_RUN:
                    break;
                  case Q_MICROARRAY_RUN:
                    break;
                  case Q_NGS_EPITOPES:
                    break;
                  case Q_NGS_FLOWCELL_RUN:
                    break;
                  case Q_NGS_HLATYPING:
                    break;
                  case Q_NGS_IMMUNE_MONITORING:
                    break;
                  case Q_NGS_IONTORRENT_RUN:
                    break;
                  case Q_NGS_MAPPING:
                    break;
                  case Q_NGS_MTB_DIAGNOSIS_RUN:
                    break;
                  case Q_NGS_READ_MATCH_ALIGNMENT_RUN:
                    break;
                  case Q_NGS_SINGLE_SAMPLE_RUN:
                    break;
                  case Q_NGS_VARIANT_CALLING:
                    break;
                  case Q_VACCINE_CONSTRUCT:
                    break;
                  case Q_WF_MA_QUALITYCONTROL_RUN:
                    break;
                  case Q_WF_MS_INDIVIDUALIZED_PROTEOME_RUN:
                    break;
                  case Q_WF_MS_LIGANDOMICS_ID_RUN:
                    break;
                  case Q_WF_MS_LIGANDOMICS_QC_RUN:
                    break;
                  case Q_WF_MS_MAXQUANT_RUN:
                    break;
                  case Q_WF_MS_PEPTIDEID_RUN:
                    break;
                  case Q_WF_MS_QUALITYCONTROL_RUN:
                    break;
                  case Q_WF_NGS_16S_TAXONOMIC_PROFILING:
                    break;
                  case Q_WF_NGS_EPITOPE_PREDICTION_RUN:
                    break;
                  case Q_WF_NGS_HLATYPING_RUN:
                    break;
                  case Q_WF_NGS_MAPPING_RUN:
                    break;
                  case Q_WF_NGS_QUALITYCONTROL_RUN:
                    break;
                  case Q_WF_NGS_RNA_EXPRESSION_ANALYSIS_RUN:
                    break;
                  case Q_WF_NGS_SHRNA_COUNTING_RUN:
                    break;
                  case Q_WF_NGS_VARIANT_ANNOTATION_RUN:
                    break;
                  case Q_WF_NGS_VARIANT_CALLING_RUN:
                    break;
                  default:
                    break;
                }
                String numericID = t.getCode();
                uniqueNumericIDToUniqueCode.put(numericID, uniqueID);
                t.setExperiment(exp);
                t.setCode(code);
                uniqueCodeToBarcode.put(uniqueID, code);
                List<String> parents = t.getParentIDs();
                t.setParents(new ArrayList<ISampleBean>());
                List<String> newParents = new ArrayList<String>();
                for (String parentID : parents) {
                  if (!uniqueCodeToBarcode.containsKey(parentID)
                      && uniqueNumericIDToUniqueCode.containsKey(parentID)) {
                    parentID = uniqueNumericIDToUniqueCode.get(parentID);
                  }
                  if (uniqueCodeToBarcode.containsKey(parentID))
                    newParents.add(uniqueCodeToBarcode.get(parentID));
                  else
                    logger.warn(
                        "Parent could not be translated, because no id to code mapping was found for id "
                            + parentID);
                }
                for (String p : newParents) {
                  t.addParentID(p);
                }
              }
            }
            // techTypes.addAll(techTypes);
            // remove existing samples from registration process
            level.removeAll(existing);
            samplesToRegister |= !level.isEmpty();
          }

          // create Experimental Design XML
          List<TechnologyType> types = new ArrayList<>(techTypes);
          prepDesignXML(types);

          fixSpecialExperiments(specialExpToExpCode);
          view.setProcessed(processed);
        }
        view.setRegEnabled(infoComplete && samplesToRegister && !overflow);
        if (infoComplete) {
          if (!samplesToRegister) {
            Styles.notification("Samples already exist.",
                "Every Analyte ID was already found in existing samples of this project.",
                NotificationType.DEFAULT);
          }
          if (overflow) {
            Styles.notification("Too many samples.",
                "This experiment exceeds the maximum number of samples for one project.",
                NotificationType.ERROR);
          }
        }
      }

      private void fixSpecialExperiments(Map<String, String> specialExpToExpCode) {
        Set<String> codes = new HashSet<String>();
        if (samplePrepProperties != null) {
          codes.addAll(samplePrepProperties.keySet());
          for (String code : codes) {
            samplePrepProperties.put(specialExpToExpCode.get(code), samplePrepProperties.get(code));
            samplePrepProperties.remove(code);
          }
        }
        codes.clear();
        if (mhcProperties != null) {
          codes.addAll(mhcProperties.keySet());
          for (String code : codes) {
            mhcProperties.put(specialExpToExpCode.get(code), mhcProperties.get(code));
            mhcProperties.remove(code);
          }
        }
        codes.clear();
        if (msProperties != null) {
          codes.addAll(msProperties.keySet());
          for (String code : codes) {
            msProperties.put(specialExpToExpCode.get(code), msProperties.get(code));
            msProperties.remove(code);
          }
        }
      }
    };
    questionaire = view.initMissingInfoComponent(projectInfoComponent, parsedCategoryToValues,
        catToVocabulary, missingInfoFilledListener);
    // view.addComponent(questionaire);

    // add project code validators
    TextField f = projectInfoComponent.getProjectField();
    CompositeValidator vd = new CompositeValidator();
    RegexpValidator p = new RegexpValidator("Q[A-Xa-x0-9]{4}",
        "Project must have length of 5, start with Q and not contain Y or Z");
    vd.addValidator(p);
    vd.addValidator(new ProjectNameValidator(openbis));
    f.addValidator(vd);
    f.setImmediate(true);
    f.setValidationVisible(true);

    FocusListener fListener = new FocusListener() {
      private static final long serialVersionUID = 8721337946386845992L;

      @Override
      public void focus(FocusEvent event) {
        TextField pr = projectInfoComponent.getProjectField();
        if (!pr.isValid() || pr.isEmpty()) {
          // new project selected...keep generating codes until one is valid
          projectInfoComponent.tryEnableCustomProject(generateUnusedProjectCode());
        }
      }
    };
    projectInfoComponent.getProjectField().addFocusListener(fListener);

    Button.ClickListener projCL = new Button.ClickListener() {

      /**
       * 
       */
      private static final long serialVersionUID = -6646294420820222646L;

      @Override
      public void buttonClick(ClickEvent event) {
        String existingProject = (String) projectInfoComponent.getProjectBox().getValue();
        if (existingProject == null || existingProject.isEmpty()) {
          projectInfoComponent.tryEnableCustomProject(generateUnusedProjectCode());
        }
      }
    };
    projectInfoComponent.getProjectReloadButton().addClickListener(projCL);

    questionaire.getSpaceBox().addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        questionaire.resetProjects();
        String space = questionaire.getSpaceCode();
        if (space != null) {
          List<String> projects = new ArrayList<String>();
          for (Project p : openbis.getProjectsOfSpace(space)) {
            String code = p.getCode();
            // String name = dbm.getProjectName("/" + space + "/" + code);
            // if (name != null && name.length() > 0) {
            // if (name.length() >= 80)
            // name = name.substring(0, 80) + "...";
            // code += " (" + name + ")";
            // }
            projects.add(code);
          }
          questionaire.setProjectCodes(projects);
        }
      }
    });
  }

  private String getNextExperiment(String project) {
    String res = project + "E" + firstFreeExperimentID;
    firstFreeExperimentID++;
    return res;
  }

  private String generateUnusedProjectCode() {
    Random r = new Random();
    String res = "";
    while (res.length() < 5 || openbis.getProjectByCode(res) != null) {
      res = "Q";
      for (int i = 1; i < 5; i++) {
        char c = 'Y';
        while (c == 'Y' || c == 'Z')
          c = (char) (r.nextInt(26) + 'A');
        res += c;
      }
    }
    return res;
  }

  /**
   * Fetches context information like space and project and computes first unused IDs of samples and
   * context. Also fills a map of existing secondary names and samples used later not to re-register
   * 
   * @throws TooManySamplesException
   */
  // TODO
  private void countExistingOpenbisEntities(String space, String project)
      throws TooManySamplesException {
    uniqueIDToSample = new HashMap<String, Sample>();
    firstFreeExperimentID = 1;
    firstFreeEntityID = 1;
    firstFreeBarcode = "";// TODO cleanup where not needed
    currentProjectSamples = new ArrayList<Sample>();
    if (openbis.projectExists(space, project)) {
      currentProjectSamples.addAll(openbis
          .getSamplesWithParentsAndChildrenOfProjectBySearchService("/" + space + "/" + project));
    }
    List<Experiment> experiments = openbis.getExperimentsOfProjectByCode(project);
    for (Experiment e : experiments) {
      String code = e.getCode();
      if (code.equals(project + "_INFO")) {
        currentDesignExperiment = e;
      }
      String[] split = code.split(project + "E");
      if (code.startsWith(project + "E") && split.length > 1) {
        int num = Integer.MAX_VALUE;
        try {
          num = Integer.parseInt(split[1]);
        } catch (Exception e2) {
        }
        if (firstFreeExperimentID <= num) {
          firstFreeExperimentID = num + 1;
        }
      }
    }
    for (Sample s : currentProjectSamples) {
      String code = s.getCode();
      // collect existing samples by their external id, can create import type-specific unique ids
      // instead
      TSVSampleBean converted = new TSVSampleBean(code, SampleType.valueOf(s.getSampleTypeCode()),
          s.getProperties().get("Q_SECONDARY_NAME"), new HashMap<>(s.getProperties()));
      String uniqueSampleID = createUniqueIDFromSampleMetadata(converted);
      boolean emptyID = uniqueSampleID == null || uniqueSampleID.isEmpty();
      if (!emptyID && uniqueIDToSample.containsKey(uniqueSampleID)) {
        logger.warn(uniqueSampleID
            + " was found as a unique id for multiple existing samples. This might"
            + " lead to inconsistencies if new samples are to be attached to this external id.");
      }
      uniqueIDToSample.put(uniqueSampleID, s);
      if (SampleCodeFunctions.isQbicBarcode(code)) {
        if (SampleCodeFunctions.compareSampleCodes(firstFreeBarcode, code) <= 0) {
          firstFreeBarcode = SampleCodeFunctions.incrementSampleCode(code);
          String firstBarcode = project + "001A" + SampleCodeFunctions.checksum(project + "001A");
          if (firstBarcode.equals(firstFreeBarcode))
            throw new TooManySamplesException();
        }
      } else if (s.getSampleTypeCode().equals(("Q_BIOLOGICAL_ENTITY"))) {
        int num = Integer.parseInt(s.getCode().split("-")[1]);
        if (num >= firstFreeEntityID)
          firstFreeEntityID = num + 1;
      }
    }
  }

  private String createUniqueIDFromSampleMetadata(ISampleBean b) {
    String id = (String) b.getMetadata().get("Q_EXTERNALDB_ID");
    if (getImportType().equals(ExperimentalDesignType.Proteomics_MassSpectrometry)) {
      // MS import format may contain same name for protein and digested peptide samples, both at
      // the base, pooled and fraction/cycle level
      // thus, we add the sample type to the external db id in order to create unique sample ids:
      if (b.getType().equals(SampleType.Q_TEST_SAMPLE)) {
        id = id + b.getMetadata().get("Q_SAMPLE_TYPE");
      }
      return id;
    } else {
      return id;
    }
  }


  private void incrementOrCreateBarcode(String project) throws TooManySamplesException {
    String firstBarcode = project + "001A" + SampleCodeFunctions.checksum(project + "001A");
    if (nextBarcode == null) {
      if (firstFreeBarcode.isEmpty()) {
        String base = project + SampleCodeFunctions.createCountString(1, 3) + "A";
        firstFreeBarcode = base + SampleCodeFunctions.checksum(base);
      }
      nextBarcode = firstFreeBarcode;
    } else {
      nextBarcode = SampleCodeFunctions.incrementSampleCode(nextBarcode);
      if (nextBarcode.equals(firstBarcode))
        throw new TooManySamplesException();
    }
  }

  protected ExperimentalDesignType getImportType() {
    return view.getSelectedDesignOption();
  }

  @Override
  public void performPostRegistrationTasks(boolean success) {
    if (success) {
      String space = null;
      String project = null;
      String projectName = null;

      if (getImportType().equals(ExperimentalDesignType.QBIC)) {
        projectName = projectInfo.getSecondaryName();
        ISampleBean s = view.getSamples().get(0).get(0);
        project = s.getProject();
        space = s.getSpace();
      } else {
        space = questionaire.getSpaceCode();
        project = questionaire.getProjectCode();
        projectName = questionaire.getProjectSecondaryName();
        String id = "/" + space + "/" + project;
        addPeopleAndProjectToDB(id, projectName);
        // if (getImportType().equals(ExperimentalDesignType.ISA)) {
        // String protocol = isaStudyInfos.getProtocol();
        // dbm.changeLongProjectDescription(id, protocol);
        // }
      }

      // TODO
      // for (OpenbisExperiment e : exps) {
      // if (e.getType().equals(ExperimentType.Q_EXPERIMENTAL_DESIGN)) {
      // String id = projectIdentifier + "/" + e.getOpenbisName();
      // for (Note n : notes) {
      // writeNoteToOpenbis(id, n);
      // }
      // }
      // }
      logger.info("Performing post registration tasks");
      if (dnaSampleCodeToMHCType != null)
        registerHLATypings(space);
      // TODO
      // for (OpenbisExperiment e : exps) {
      // String identifier = projectIdentifier + "/" + e.getOpenbisName();
      // int expID = dbm.addExperimentToDB(identifier);
      // if (e.getPersonID() > -1) {
      // int person = e.getPersonID();
      // dbm.addPersonToExperiment(expID, person, "Contact");
      // }
      // }
    }
  }

  private void addPeopleAndProjectToDB(String projectIdentifier, String projectName) {
    int projectID = dbm.addProjectToDB(projectIdentifier, projectName);
    int investigatorID = -1;
    int contactID = -1;
    int managerID = -1;
    Map<String, Integer> people = vocabs.getPeople();
    if (!questionaire.getPerson(PersonType.Investigator).equals(""))
      investigatorID = people.get(questionaire.getPerson(PersonType.Investigator));
    if (!questionaire.getPerson(PersonType.Manager).equals(""))
      managerID = people.get(questionaire.getPerson(PersonType.Manager));
    if (!questionaire.getPerson(PersonType.Contact).equals(""))
      contactID = people.get(questionaire.getPerson(PersonType.Contact));
    if (investigatorID != -1)
      dbm.addPersonToProject(projectID, investigatorID, "PI");
    if (contactID != -1)
      dbm.addPersonToProject(projectID, contactID, "Contact");
    if (managerID != -1)
      dbm.addPersonToProject(projectID, managerID, "Manager");
  }

  private void registerHLATypings(String space) {
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("dropbox", "QBiC-register-hlatyping");
    for (String code : dnaSampleCodeToMHCType.keySet()) {
      props.put("filename", space + "_" + code + "_1.alleles");
      List<String> content = dnaSampleCodeToMHCType.get(code).getClassOne();
      props.put("content", content);
      openbis.triggerIngestionService("create-register-textfile", props);

      props.put("filename", space + "_" + code + "_2.alleles");
      content = dnaSampleCodeToMHCType.get(code).getClassTwo();
      props.put("content", content);
      openbis.triggerIngestionService("create-register-textfile", props);
    }
  }

  public Component getView() {
    return view;
  }

  @Override
  public String getRegistrationError() {
    return openbisCreator.getErrors();
  }
}
