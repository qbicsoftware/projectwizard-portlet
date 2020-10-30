package life.qbic.projectwizard.control;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Validates vocabulary values of parsed designs
 * 
 * @author Andreas Friedrich
 *
 */
public class VocabularyValidator {

  private final Logger logger = LogManager.getLogger(VocabularyValidator.class);

  private Map<String, Set<String>> vocabulariesForProperties;
  private String error = "";

  /**
   * 
   * @param vocabulariesForProperties A map containing a set of vocabulary values for each possible
   *        property name (not vocabulary name!)
   */
  public VocabularyValidator(Map<String, Set<String>> vocabulariesForProperties) {
    this.vocabulariesForProperties = vocabulariesForProperties;
  }

  public String getError() {
    return error;
  }

  /**
   * Given a List of metadata maps - denoting experiments
   * 
   * @param metadataList
   * @param pretransformedProperties
   * @return
   */
  public boolean transformAndValidateExperimentMetadata(List<Map<String, Object>> metadataList,
      Map<String, Set<String>> pretransformedProperties) {

    for (Map<String, Object> experimentProperties : metadataList) {
      Set<String> props = new HashSet<>();
      props.addAll(experimentProperties.keySet());
      for (String propertyName : props) {
        if (pretransformedProperties.containsKey(propertyName)) {
          boolean success = transformProperty(propertyName, experimentProperties,
              pretransformedProperties.get(propertyName));
          if (!success) {
            return false;
          }
          experimentProperties.remove(propertyName);
        }
      }
    }

    for (Map<String, Object> experimentProperties : metadataList) {
      Set<String> props = new HashSet<>();
      props.addAll(experimentProperties.keySet());
      for (String propertyName : props) {
        if (vocabulariesForProperties.containsKey(propertyName)) {
          Set<String> set = vocabulariesForProperties.get(propertyName);
          Object valueObject = experimentProperties.get(propertyName);
          if (valueObject instanceof List) {
            List<String> values = (List<String>) valueObject;
            for (String val : values) {
              if (!set.contains(val.toUpperCase()) && !set.contains(val)) {
                logger.debug(val.toUpperCase());
                setErrorMessage(val, propertyName, set);
                return false;
              }
            }
            String newVal = String.join(", ", values);
            experimentProperties.put(propertyName, newVal);
          } else {
            String value = valueObject.toString();
            if (!set.contains(value.toUpperCase()) && !set.contains(value)) {
              logger.debug(value.toUpperCase());
              setErrorMessage(value, propertyName, set);
              return false;
            }
          }
        }
      }
    }
    return true;
  }

  /**
   * Given a property Name denoting an umbrella term for more than one vocabulary, a Map of
   * experiment properties + values and a set of possible vocabularies for this property, tries to
   * find the relevant vocabulary and replaces the property name by the correct one
   * 
   * @param propertyName The property name for the experiment property that could be part of more
   *        than one vocabulary
   * @param experimentProperties Map of experiment properties
   * @param possibleVocabularizedPropertyNames Set of possible property names that point to possibly
   *        valid vocabularies for the experiment property value in question
   * @return
   */
  private boolean transformProperty(String propertyName, Map<String, Object> experimentProperties,
      Set<String> possibleVocabularizedPropertyNames) {
    Object valueObject = experimentProperties.get(propertyName);
    if (valueObject instanceof String) {
      String value = (String) valueObject;
      for (String vocabName : possibleVocabularizedPropertyNames) {
        Set<String> set = vocabulariesForProperties.get(vocabName);
        if (set.contains(value.toUpperCase()) || set.contains(value)) {
          experimentProperties.put(vocabName, value);
          return true;
        }
      }
      error = "Property " + value + " is not a valid value for either of these categories: "
          + possibleVocabularizedPropertyNames;
      return false;
    } else {
      logger.error("value for " + propertyName + " not a string. was: " + valueObject);
      return false;
    }
  }

  private void setErrorMessage(String property, String propertyName, Set<String> vocabulary) {
    error = "Property " + property + " is not a valid value for " + propertyName;
    error += "\nValid values: " + vocabulary;
  }



}
