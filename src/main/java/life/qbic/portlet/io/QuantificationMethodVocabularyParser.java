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
package life.qbic.portlet.io;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import life.qbic.portlet.uicomponents.LabelingMethod;

public class QuantificationMethodVocabularyParser {

  public List<LabelingMethod> parseQuantificationMethods(File file) {
    Scanner scanner = null;
    try {
      scanner = new Scanner(file);
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    List<LabelingMethod> methods = new ArrayList<LabelingMethod>();
    List<String> reagents = new ArrayList<String>();
    String name = "";
    while (scanner.hasNext()) {
      String line = scanner.nextLine();
      if (!line.startsWith("\t")) {
        if (!name.isEmpty()) {
          methods.add(new LabelingMethod(name, reagents));
          reagents = new ArrayList<String>();
        }
        name = line.trim();
      } else {
        reagents.add(line.trim());
      }
    }
    methods.add(new LabelingMethod(name, reagents));
    scanner.close();
    return methods;
  }

}
