/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.controller.harvester; 

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.skife.csv.SimpleReader;

import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;



/**
 * An implementation of FileHarvestJob that can be used for any CSV file harvest.
 */
class CsvFileHarvestJob implements FileHarvestJob {

    /**
     * Contains constant constructor inputs for CsvFileHarvestJob
     * @author mbarbieri
     */
    public enum JobType {
        GRANT("csvGrant", "granttemplate.csv", "CSVtoRDFgrant.sh", "Grant", "Imported Grants", "No new grants were imported.", new String[] {"http://vivoweb.org/ontology/core#Grant"}),
        PERSON("csvPerson", "persontemplate.csv", "CSVtoRDFperson.sh", "Person", "Imported Persons", "No new persons were imported.", new String[] {"http://xmlns.com/foaf/0.1/Person"});

        public final String httpParameterName;
        private final String templateFileName;
        private final String scriptFileName;
        private final String friendlyName;
        private final String linkHeader;
        private final String noNewDataMessage;
        private final String[] rdfTypesForLinks;

        /**
         * Determines if there is a JobType with the specified HTTP parameter name.
         * @param httpParameterName the HTTP parameter name to look for a Job Type for
         * @return true if there is such a JobType, false otherwise
         */
        public static boolean containsTypeWithHttpParameterName(String httpParameterName) {
            return (getByHttpParameterName(httpParameterName) != null);
        }

        /**
         * Returns the JobType with the specified HTTP parameter name.  This is essentially a string identifier for the job type.  This
         * method accepts nulls, returning null in that case.
         * @param httpParameterName the HTTP parameter name to find the job for
         * @return the JobType with the specified HTTP parameter name, or null if there is none or httpParameterName was null
         */
        public static JobType getByHttpParameterName(String httpParameterName) {
            JobType returnValue = null;

            if(httpParameterName != null) {
                JobType[] values = JobType.values();
                for(JobType jobType : values) {
                    if(jobType.httpParameterName.equalsIgnoreCase(httpParameterName)) {
                        returnValue = jobType;
                        break;
                    }
                }
            }
            return returnValue;
        }
        
        private JobType(String httpParameterName, String templateFileName, String scriptFileName, String friendlyName, String linkHeader, String noNewDataMessage, String[] rdfTypesForLinks) {
            this.httpParameterName = httpParameterName;
            this.templateFileName = templateFileName;
            this.scriptFileName = scriptFileName;
            this.friendlyName = friendlyName;
            this.linkHeader = linkHeader;
            this.noNewDataMessage = noNewDataMessage;
            this.rdfTypesForLinks = Arrays.copyOf(rdfTypesForLinks, rdfTypesForLinks.length);
        }
        
        private CsvFileHarvestJob constructCsvFileHarvestJob(VitroRequest vreq, String namespace) {
            return new CsvFileHarvestJob(vreq, this.templateFileName, this.scriptFileName, namespace, this.friendlyName, this.linkHeader, this.noNewDataMessage, this.rdfTypesForLinks);
        }
    }

    
    /**
     * Logger.
     */
    private static final Log log = LogFactory.getLog(CsvFileHarvestJob.class);

    /**
     * The HTTP request.
     */
    private VitroRequest vreq;

    /**
     * The template file against which uploaded CSV files will be validated.
     */
    private File templateFile;

    /**
     * The script which will be run after needed replacements are made.
     */
    private File scriptFile;

    /**
     * The namespace to be used for the harvest.
     */
    @SuppressWarnings("unused")
    private final String namespace;

    /**
     * A name for the type of data being imported.  For example "Grant" or "Person".
     */
    private final String friendlyName;

    /**
     * A heading to be shown above the area where links to profiles of newly-harvested entities are listed.
     */
    private final String linkHeader;

    /**
     * The message to show to the user if there are no newly-harvested entities to show them.
     */
    private final String noNewDataMessage;

    /**
     * An array of rdf:type values which will be used for links.
     */
    private final String[] rdfTypesForLinks;
    
    /**
     * The session ID of this user session.
     */
    private final String sessionId;

    
    public static CsvFileHarvestJob createJob(JobType jobType, VitroRequest vreq, String namespace) {
        return jobType.constructCsvFileHarvestJob(vreq, namespace);
    }
    
    /**
     * Constructor.
     * @param templateFileName just the name of the template file.  The directory is assumed to be standard.
     */
    private CsvFileHarvestJob(VitroRequest vreq, String templateFileName, String scriptFileName, String namespace, String friendlyName, String linkHeader, String noNewDataMessage, String[] rdfTypesForLinks) {
        this.vreq = vreq;
        this.templateFile = new File(getTemplateFileDirectory() + templateFileName);
        this.scriptFile = new File(getScriptFileDirectory() + scriptFileName);
        this.namespace = namespace;
        this.friendlyName = friendlyName;
        this.linkHeader = linkHeader;
        this.noNewDataMessage = noNewDataMessage;
        this.rdfTypesForLinks = Arrays.copyOf(rdfTypesForLinks, rdfTypesForLinks.length);
        
        this.sessionId = this.vreq.getSession().getId();
    }

    /**
     * Gets the path to the directory containing the template files.
     * @return the path to the directory containing the template files
     */
    private String getTemplateFileDirectory() {
        String harvesterPath = TestFileController.getHarvesterPath();
        String pathToTemplateFiles = harvesterPath + TestFileController.PATH_TO_TEMPLATE_FILES;
        return pathToTemplateFiles;
    }

    /**
     * Gets the path to the directory containing the script files.
     * @return the path to the directory containing the script files
     */
    private String getScriptFileDirectory() {
        String harvesterPath = TestFileController.getHarvesterPath();
        String pathToScriptFiles = harvesterPath + TestFileController.PATH_TO_HARVESTER_SCRIPTS;
        return pathToScriptFiles;
    }


    
    private boolean[] getLinesEndingInComma(File file) throws IOException {
        ArrayList<Boolean> linesEndingInCommaList = new ArrayList<Boolean>();
        
        BufferedReader reader = new BufferedReader(new FileReader(file));
        
        for(String line = reader.readLine(); line != null; line = reader.readLine()) {
            boolean lineEndsInComma = line.endsWith(",");
            linesEndingInCommaList.add(lineEndsInComma);
        }
        reader.close();
        
        boolean[] linesEndingInComma = new boolean[linesEndingInCommaList.size()];
        for(int i = 0; i < linesEndingInComma.length; i++) {
            linesEndingInComma[i] = linesEndingInCommaList.get(i);
        }
        return linesEndingInComma;
    }
    
    
    
    @Override
    @SuppressWarnings("rawtypes")
    public String validateUpload(File file) {
        try {
            SimpleReader reader = new SimpleReader();

            List templateCsv = reader.parse(this.templateFile);
            String[] templateFirstLine = (String[])templateCsv.get(0);

            //if a line ends in a comma (absolutely a comma, no whitespace), SimpleReader will not consider the part after the comma to be a blank section.
            List csv = reader.parse(file);
            boolean[] linesEndingInComma = getLinesEndingInComma(file);

            int length = csv.size();

            if(length == 0)
                return "No data in file";

            for(int i = 0; i < length; i++) {
                String[] line = (String[])csv.get(i);
                boolean endsInComma = linesEndingInComma[i];
                if(i == 0) {
                    String errorMessage = validateCsvFirstLine(templateFirstLine, line);
                    if(errorMessage != null)
                        return errorMessage;
                }
                else if(line.length != 0) {
                    int actualLineLength = line.length + (endsInComma ? 1 : 0);
                    if(actualLineLength != templateFirstLine.length) {
                        return "Mismatch in number of entries in row " + i + ": expected " + templateFirstLine.length + ", found " + actualLineLength;
                    }
                }
            }

        } catch (IOException e) {
            log.error(e, e);
            return e.getMessage();
        }
        return null;
    }

    /**
     * Makes sure that the first line of the CSV file is identical to the first line of the template file.  This is
     * assuming we are expecting all user CSV files to contain an initial header line.  If this is not the case, then
     * this method is unnecessary.
     * @param templateFirstLine the parsed-out contents of the first line of the template file
     * @param line the parsed-out contents of the first line of the input file
     * @return an error message if the two lines don't match, or null if they do
     */
    private String validateCsvFirstLine(String[] templateFirstLine, String[] line) {
        String errorMessage = "File header does not match template";
        if(line.length != templateFirstLine.length) {
            //return errorMessage + ": " + "file header columns = " + line.length + ", template columns = " + templateFirstLine.length;
            String errorMsg = "";
            errorMsg += "file header items: ";
            for(int i = 0; i < line.length; i++) {
                errorMsg += line[i] + ", ";
            }
            errorMsg += "template items: ";
            for(int i = 0; i < templateFirstLine.length; i++) {
                errorMsg += templateFirstLine[i] + ", ";
            }
            return errorMsg;
        }
        for(int i = 0; i < line.length; i++)
        {
            if(!line[i].equals(templateFirstLine[i]))
                return errorMessage + ": file header column " + (i + 1) + " = " + line[i] + ", template column " + (i + 1) + " = " + templateFirstLine[i];
        }
        return null;
    }

    @Override
    public String getScript()
    {
        File scriptTemplate = this.scriptFile;

        String scriptTemplateContents = readScriptTemplate(scriptTemplate);
        String replacements = performScriptTemplateReplacements(scriptTemplateContents);
        return replacements;
    }


    private String performScriptTemplateReplacements(String scriptTemplateContents) {
        String replacements = scriptTemplateContents;

        String workingDirectory = TestFileController.getHarvesterPath();
        String fileDirectory = TestFileController.getUploadPath(vreq);
        String harvestedDataPath = getHarvestedDataPath();

        replacements = replacements.replace("${WORKING_DIRECTORY}", workingDirectory);
        replacements = replacements.replace("${UPLOADS_FOLDER}", fileDirectory);
        replacements = replacements.replace("${HARVESTED_DATA_PATH}", harvestedDataPath);

        return replacements;
    }


    private String readScriptTemplate(File scriptTemplate) {
        String scriptTemplateContents = null;
        BufferedReader reader = null;
        try {
            int fileSize = (int)(scriptTemplate.length());
            char[] buffer = new char[fileSize];
            reader = new BufferedReader(new FileReader(scriptTemplate), fileSize);
            reader.read(buffer);
            scriptTemplateContents = new String(buffer);
        } catch (IOException e) {
            log.error(e, e);
        } finally {
            try {
                if(reader != null)
                    reader.close();
            } catch(IOException e) {
                log.error(e, e);
            }
        }

        return scriptTemplateContents;
    }

    private String getHarvestedDataPath() {
        return TestFileController.getFileHarvestRootPath() + "harvested-data/csv/" + this.sessionId + "/";
    }

    @Override
    public String getAdditionsFilePath() {
        return getHarvestedDataPath() + "additions.rdf.xml";
    }

    @Override
    public String getPageHeader() {
        return "Harvest " + this.friendlyName + " data from CSV file(s)";
    }

    @Override
    public String getLinkHeader() {
        return this.linkHeader;
    }

    @Override
    public String getTemplateFilePath() {
        return this.templateFile.getPath();
    }

    @Override
    public String[] getRdfTypesForLinks() {
        return Arrays.copyOf(this.rdfTypesForLinks, this.rdfTypesForLinks.length);
    }

    @Override
    public String getTemplateDownloadHelp() {
        return "Click here to download a template file to assist you with harvesting the data.";
    }

    @Override
    public String getTemplateFillInHelp() {
        String newline = "\n";
        String help = "";
        help += "<p>A CSV, or <b>C</b>omma-<b>S</b>eparated <b>V</b>alues file, is a method of storing tabular data in plain text.  The first line of a CSV file contains header information, while each subsequent line contains a data record.</p>" + newline;
        help += "<p>The template we provide contains only the header, which you will then fill in accordingly.  For example, if the template contains the text \"firstName,lastName\", then you might add two more lines, \"John,Doe\" and \"Jane,Public\".</p>" + newline;
        return help;
    }

    @Override
    public String getNoNewDataMessage() {
        return this.noNewDataMessage;
    }
    
}


