/*******************************************************************************
 * QBiC Project Wizard enables users to create hierarchical experiments including different study conditions using factorial design.
 * Copyright (C) "2016"  Andreas Friedrich
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package life.qbic.projectwizard.processes;

import java.sql.SQLException;

import life.qbic.projectwizard.control.IRegistrationController;
import life.qbic.projectwizard.views.IRegistrationView;

/**
 * Class implementing the Runnable interface so it can be run and trigger a response in the view after the sample creation thread finishes
 * @author Andreas Friedrich
 *
 */
public class RegisteredSamplesReadyRunnable implements Runnable {

  private IRegistrationView view;
  private IRegistrationController control;

  public RegisteredSamplesReadyRunnable(IRegistrationView view, IRegistrationController control) {
    this.view = view;
    this.control = control;
  }

  @Override
  public void run() {
    boolean sqlDown = false;
    try {
      control.performPostRegistrationTasks(control.getRegistrationError().isEmpty());
    } catch (SQLException e) {
      sqlDown = true;
    }
    view.registrationDone(sqlDown, control.getRegistrationError());
  }
}
