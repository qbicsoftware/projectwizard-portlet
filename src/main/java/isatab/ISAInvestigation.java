package isatab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ISAInvestigation {

  // Identifier String A identifier or an accession number provided by a repository. This SHOULD be
  // locally unique.
  // Title String A concise name given to the investigation.
  // Description String A textual description of the investigation.
  // Submission Date Representation of a ISO8601 date The date on which the investigation was
  // reported to the repository.
  // Public Release Date Representation of a ISO8601 date The date on which the investigation was
  // released publicly.
  // Publications A list of Publication A list of Publications relating to the investigation.
  // Contacts A list of Contact A list of Contacts relating to the investigation.

  private String identifier;
  private String title;
  private String description;
  private Date submissionDate;
  private Date releaseDate;
  private List<String> publications;
  private List<String> x;

}
