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

import java.io.*;
import java.util.*;

class OBOParser {
  private BufferedReader in;
  private String buffer;
  private Map<String, Term> id2term = new HashMap<String, Term>();

  private class Term {
    String id;
    String name;
    String def;
    Set<String> children = new HashSet<String>();
    Set<String> is_a = new HashSet<String>();

    int depth() {
      int min_child = 0;
      for (String p : is_a) {
        Term parent = id2term.get(p);
        if (parent == null) {
          System.err.println("Cannot get " + p);
          continue;
        }
        int n2 = parent.depth();
        if (min_child == 0 || n2 < min_child)
          min_child = n2;
      }
      return 1 + min_child;
    }

    public String toString() {
      return id + "\t" + name + "\t" + is_a;
    }
  }

  private Set<String> getAllDescendantById(String id) {
    Set<String> set = new HashSet<String>();
    set.add(id);
    Term t = id2term.get(id);
    for (String c : t.children) {
      set.addAll(getAllDescendantById(c));
    }
    return set;
  }


  private Term getTermById(String id, boolean create) {
    Term t = this.id2term.get(id);
    if (t == null && create) {
      t = new Term();
      t.id = id;
      t.name = id;
      t.def = id;
      this.id2term.put(id, t);
    }
    return t;
  }

  private static String nocomment(String s) {
    int excl = s.indexOf('!');
    if (excl != -1)
      s = s.substring(0, excl);
    return s.trim();
  }

  private String next() throws IOException {
    if (buffer != null) {
      String s = buffer;
      buffer = null;
      return s;
    }
    return in.readLine();
  }

  private void parseTerm() throws IOException {
    Term t = null;
    String line;
    while ((line = next()) != null) {
      if (line.startsWith("[")) {
        this.buffer = line;
        break;
      }
      int colon = line.indexOf(':');
      if (colon == -1)
        continue;
      if (line.startsWith("id:") && t == null) {
        t = getTermById(line.substring(colon + 1).trim(), true);
        continue;
      }
      if (t == null)
        continue;
      if (line.startsWith("name:")) {
        t.name = nocomment(line.substring(colon + 1));
        continue;
      } else if (line.startsWith("def:")) {
        t.def = nocomment(line.substring(colon + 1));
        continue;
      } else if (line.startsWith("is_a:")) {
        String rel = nocomment(line.substring(colon + 1));
        t.is_a.add(rel);
        Term parent = getTermById(rel, true);
        parent.children.add(t.id);
        continue;
      }
    }
  }

  private void parse() throws IOException {
    in = new BufferedReader(new FileReader(new File("/Users/frieda/Downloads/pride_cv.obo")));
    String line;
    while ((line = next()) != null) {
      if (line.equals("[Term]"))
        parseTerm();
    }
    in.close();
  }

  public static void main(String args[]) throws IOException {
    OBOParser app = new OBOParser();
    app.parse();
    int level = 1;
    boolean found = true;
    while (found) {
      found = false;
      for (Term t : app.id2term.values()) {
        if (t.depth() == level) {
          System.out.println("" + level + "\t" + t);
          found = true;
        }
      }
      level++;
    }
    for (String id : app.getAllDescendantById("PRIDE:0000309")) {
      System.out.println(app.id2term.get(id));
    }
  }
}
