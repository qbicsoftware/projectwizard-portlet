package life.qbic.portlet.control;

import java.sql.SQLException;

public interface IRegistrationController {

  void performPostRegistrationTasks(boolean success) throws SQLException;

  String getRegistrationError();

}
