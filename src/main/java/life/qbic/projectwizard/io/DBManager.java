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
package life.qbic.projectwizard.io;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DBManager {
  private DBConfig config;

  Logger logger = LogManager.getLogger(DBManager.class);

  public DBManager(DBConfig config) {
    this.config = config;
  }

  private void logout(Connection conn) {
    try {
      if (conn != null)
        conn.close();
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private Connection login() {
    String DB_URL = "jdbc:mariadb://" + config.getHostname() + ":" + config.getPort() + "/"
        + config.getSql_database();

    Connection conn = null;

    try {
      Class.forName("org.mariadb.jdbc.Driver");
      conn = DriverManager.getConnection(DB_URL, config.getUsername(), config.getPassword());
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return conn;
  }

  public String getProjectName(String projectIdentifier) {
    String sql = "SELECT short_title from projects WHERE openbis_project_identifier = ?";
    String res = "";
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setString(1, projectIdentifier);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        res = rs.getString(1);
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } catch (NullPointerException n) {
      logger.error("Could not reach SQL database, resuming without project names.");
    }
    logout(conn);
    return res;
  }

  public int isProjectInDB(String projectIdentifier) {
    logger.info("Looking for project " + projectIdentifier + " in the DB");
    String sql = "SELECT * from projects WHERE openbis_project_identifier = ?";
    int res = -1;
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setString(1, projectIdentifier);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        res = rs.getInt("id");
        logger.info("project found!");
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    }
    logout(conn);
    return res;
  }

  public int addProjectToDB(String projectIdentifier, String projectName) {
    int exists = isProjectInDB(projectIdentifier);
    if (exists < 0) {
      logger.info("Trying to add project " + projectIdentifier + " to the person DB");
      String sql = "INSERT INTO projects (openbis_project_identifier, short_title) VALUES(?, ?)";
      Connection conn = login();
      try (PreparedStatement statement =
          conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        statement.setString(1, projectIdentifier);
        statement.setString(2, projectName);
        statement.execute();
        ResultSet rs = statement.getGeneratedKeys();
        if (rs.next()) {
          logout(conn);
          logger.info("Successful.");
          return rs.getInt(1);
        }
      } catch (SQLException e) {
        logger.error("SQL operation unsuccessful: " + e.getMessage());
        e.printStackTrace();
      }
      logout(conn);
      return -1;
    }
    return exists;
  }

  private boolean hasPersonRoleInProject(int personID, int projectID, String role) {
    logger.info("Checking if person already has this role in the project.");
    String sql =
        "SELECT * from projects_persons WHERE person_id = ? AND project_id = ? and project_role = ?";
    boolean res = false;
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setInt(1, personID);
      statement.setInt(2, projectID);
      statement.setString(3, role);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        res = true;
        logger.info("person already has this role!");
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    }
    logout(conn);
    return res;
  }

  public void addPersonToProject(int projectID, int personID, String role) {
    if (!hasPersonRoleInProject(personID, projectID, role)) {
      logger.info("Trying to add person with role " + role + " to a project.");
      String sql =
          "INSERT INTO projects_persons (project_id, person_id, project_role) VALUES(?, ?, ?)";
      Connection conn = login();
      try (PreparedStatement statement =
          conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        statement.setInt(1, projectID);
        statement.setInt(2, personID);
        statement.setString(3, role);
        statement.execute();
        logger.info("Successful.");
      } catch (SQLException e) {
        logger.error("SQL operation unsuccessful: " + e.getMessage());
        e.printStackTrace();
      }
      logout(conn);
    }
  }

  /**
   * returns a map of principal investigator first+last names along with the pi_id. only returns
   * active investigators
   * 
   * @return
   */
  private Map<String, Integer> getPersonsWithIDs() {
    String sql = "SELECT id, first_name, family_name FROM persons WHERE active = 1";
    Map<String, Integer> res = new HashMap<String, Integer>();
    Connection conn = login();
    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        int pi_id = rs.getInt("id");
        String first = rs.getString("first_name");
        String last = rs.getString("family_name");
        res.put(first + " " + last, pi_id);
      }
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    logout(conn);
    return res;
  }

  public boolean genericInsertIntoTable(String table, Map<String, Object> values) {
    List<String> keys = new ArrayList<String>(values.keySet());
    String key_string = String.join(", ", keys);
    String[] ar = new String[keys.size()];
    for (int i = 0; i < ar.length; i++) {
      ar[i] = "?";
    }
    String val_string = String.join(", ", ar);
    String sql = "INSERT INTO " + table + " (" + key_string + ") VALUES(" + val_string + ")";
    // return false;
    Connection conn = login();
    try (
        PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      int i = 0;
      for (String key : keys) {
        i++;
        Object val = values.get(key);
        if (val instanceof String)
          statement.setString(i, (String) val);
        if (val instanceof Integer)
          statement.setInt(i, (int) val);
      }
      boolean res = statement.execute();
      logout(conn);
      return res;
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
    }
    logout(conn);
    return false;
  }

  public int addExperimentToDB(String id) {
    int exists = isExpInDB(id);
    if (exists < 0) {
      String sql = "INSERT INTO experiments (openbis_experiment_identifier) VALUES(?)";
      Connection conn = login();
      try (PreparedStatement statement =
          conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        statement.setString(1, id);
        statement.execute();
        ResultSet rs = statement.getGeneratedKeys();
        if (rs.next()) {
          logout(conn);
          return rs.getInt(1);
        }
      } catch (SQLException e) {
        logger.error("Was trying to add experiment " + id + " to the person DB");
        logger.error("SQL operation unsuccessful: " + e.getMessage());
      }
      logout(conn);
      return -1;
    }
    logger.info("added experiment do mysql db");
    return exists;
  }

  private int isExpInDB(String id) {
    logger.info("Looking for experiment " + id + " in the DB");
    String sql = "SELECT * from experiments WHERE openbis_experiment_identifier = ?";
    int res = -1;
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setString(1, id);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        logger.info("experiment found!");
        res = rs.getInt("id");
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    }
    logout(conn);
    return res;
  }

  public void addPersonToExperiment(int expID, int personID, String role) {
    if (expID == 0 || personID == 0)
      return;

    if (!hasPersonRoleInExperiment(personID, expID, role)) {
      logger.info("Trying to add person with role " + role + " to an experiment.");
      String sql =
          "INSERT INTO experiments_persons (experiment_id, person_id, experiment_role) VALUES(?, ?, ?)";
      Connection conn = login();
      try (PreparedStatement statement =
          conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        statement.setInt(1, expID);
        statement.setInt(2, personID);
        statement.setString(3, role);
        statement.execute();
        logger.info("Successful.");
      } catch (SQLException e) {
        logger.error("SQL operation unsuccessful: " + e.getMessage());
        e.printStackTrace();
      }
      logout(conn);
    }
  }

  private boolean hasPersonRoleInExperiment(int personID, int expID, String role) {
    logger.info("Checking if person already has this role in the experiment.");
    String sql =
        "SELECT * from experiments_persons WHERE person_id = ? AND experiment_id = ? and experiment_role = ?";
    boolean res = false;
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setInt(1, personID);
      statement.setInt(2, expID);
      statement.setString(3, role);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        res = true;
        logger.info("person already has this role!");
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    }
    logout(conn);
    return res;
  }

  private void endQuery(Connection c, PreparedStatement p) {
    if (p != null)
      try {
        p.close();
      } catch (Exception e) {
        logger.error("PreparedStatement close problem");
      }
    if (c != null)
      try {
        logout(c);
      } catch (Exception e) {
        logger.error("Database Connection close problem");
      }
  }

  public Map<String, Integer> fetchPeople() {
    Map<String, Integer> map = new HashMap<String, Integer>();
    try {
      map = getPersonsWithIDs();
    } catch (NullPointerException e) {
      map.put("No Connection", -1);
    }
    return map;
  }

  public Set<String> getFullTissueSet() {
    Set<String> res = new HashSet<String>();
    String sql = "SELECT name FROM ontology_entry;";
    Connection conn = login();
    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        res.add(rs.getString(1));
      }
      // statement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    logout(conn);
    System.out.println(res.size());
    return res;
  }

  private boolean saveOldDescription(String projectIdentifier) {
    String sql = "SELECT * from projects WHERE openbis_project_identifier = ?";
    int id = -1;
    String oldDescription = "";
    String oldTitle = "";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setString(1, projectIdentifier);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        id = rs.getInt("id");
        oldDescription = rs.getString("long_description");
        oldTitle = rs.getString("short_title");
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    }
    Date date = new Date();
    Timestamp timestamp = new Timestamp(date.getTime());
    sql =
        "INSERT INTO projects_history (project_id, timestamp, long_description, short_title) VALUES(?, ?, ?, ?)";
    statement = null;
    int res = -1;
    try {
      statement = conn.prepareStatement(sql);
      statement.setInt(1, id);
      statement.setTimestamp(2, timestamp);
      statement.setString(3, oldDescription);
      statement.setString(4, oldTitle);
      statement.execute();
      res = statement.getUpdateCount();
      logger.info("Successful.");
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res != -1;
  }

  public boolean changeLongProjectDescription(String projectIdentifier, String description) {
    logger.info("Adding long description of project " + projectIdentifier);
    boolean saved = saveOldDescription(projectIdentifier);
    if (!saved)
      logger.warn("Could not save old project description to database!");
    String sql = "UPDATE projects SET long_description = ? WHERE openbis_project_identifier = ?";
    Connection conn = login();
    PreparedStatement statement = null;
    int res = -1;
    try {
      statement = conn.prepareStatement(sql);
      statement.setString(1, description);
      statement.setString(2, projectIdentifier);
      statement.execute();
      res = statement.getUpdateCount();
      logger.info("Successful.");
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res != -1;
  }

  public boolean findTissueInOntology(String tissue) {
    String sql = "SELECT * FROM ontology_entry WHERE name = ?;";
    Connection conn = login();
    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      statement.setString(1, tissue);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        return true;
      }
      // statement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    logout(conn);
    return false;
  }

  public Set<OntologyEntry> getDescendantsOfOntologyTerm(List<String> terms, boolean all) {
    List<String> parentsToSearch = new ArrayList<String>();
    parentsToSearch.addAll(terms);
    Set<OntologyEntry> res = new HashSet<OntologyEntry>();
    String[] ar = new String[parentsToSearch.size()];
    for (int i = 0; i < ar.length; i++) {
      ar[i] = "?";
    }
    String val_string = String.join(", ", ar);
    String sql =
        "SELECT ontology_entry.*, ontology_relation.* FROM ontology_entry, ontology_relation "
            + "WHERE ontology_relation.parent_entry IN (" + val_string + ") "
            + "AND ontology_relation.child_entry = ontology_entry.id;";
    Connection conn = login();
    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      int i = 0;
      for (String term : parentsToSearch) {
        i++;
        statement.setString(i, term);
      }
      parentsToSearch.clear();
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String term = rs.getString("ontology_entry.id");
        String label = rs.getString("ontology_entry.name");
        String desc = rs.getString("ontology_entry.description");
        if (all)
          parentsToSearch.add(term);
        res.add(new OntologyEntry(term, label, desc));
      }
      // statement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    logout(conn);
    if (!parentsToSearch.isEmpty())
      res.addAll(getDescendantsOfOntologyTerm(parentsToSearch, all));
    return res;
  }

}
