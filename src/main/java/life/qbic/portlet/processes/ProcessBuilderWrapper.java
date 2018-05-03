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
package life.qbic.portlet.processes;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;


class ProcessBuilderWrapper {
    private StringWriter infos;
    private StringWriter errors;
    private int status;
 
    public ProcessBuilderWrapper(File directory, List<String> command, String path) throws Exception {
        infos = new StringWriter();
        errors = new StringWriter();
        ProcessBuilder pb = new ProcessBuilder(command);
        if(path != null) pb.environment().put("PATH", path);
        if(directory != null) pb.directory(directory);
        Process process = pb.start();
        StreamBoozer seInfo = new StreamBoozer(process.getInputStream(), new PrintWriter(infos, true));
        StreamBoozer seError = new StreamBoozer(process.getErrorStream(), new PrintWriter(errors, true));
        seInfo.start();
        seError.start();
        status = process.waitFor();
        seInfo.join();
        seError.join();
    }
 
    public ProcessBuilderWrapper(List<String> command) throws Exception {
        this(null, command, null);
    }
    
    public ProcessBuilderWrapper(List<String> command, String path) throws Exception {
    	this(null, command, path);
    }
 
    public String getErrors() {
        return errors.toString();
    }
 
    public String getInfos() {
        return infos.toString();
    }
 
    public int getStatus() {
        return status;
    }   
}
 
    class StreamBoozer extends Thread {
        private InputStream in;
        private PrintWriter pw;
 
        StreamBoozer(InputStream in, PrintWriter pw) {
            this.in = in;
            this.pw = pw;
        }
 
        @Override
        public void run() {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(in));
                String line = null;
                while ( (line = br.readLine()) != null) {
                    pw.println(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }