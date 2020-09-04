/*
 * This is the main "driver" class that will either output the contents of a Galaxy History Archive file
 * to STDOUT (if a history file is given as argument) or start up the GUI to allow interactive browsing.
 */
package no.nels.galaxyhistorybrowser;

import com.fasterxml.jackson.core.JsonParseException;
import java.io.IOException;

/**
 * 
 * @author kjetikl
 */
public class GalaxyHistoryBrowser {
    
    // These fields are used to store command line arguments that are processed below
    static String archiveFile=null; // the (optional) name or URL of the history archive file provided on the command line)
    static String datasetID=null;   // the (optional) identifier for a dataset in the history, provided on the command line
    static String extraFile=null;   // the (optional) path for an "extra file" for a dataset in the history, provided on the command line
    static int start=-1;            // the (optional) start position inside a file when only a subset of the file should be returned
    static int end=-1;              // the (optional) end position inside a file when only a subset of the file should be returned
    static boolean returnVersionOnly=false; // set to TRUE if only the archive format version of the history file should be returned
    static boolean returnMIMEtype=false;    // set to TRUE if only the MIME type of a dataset or file should be returned rather than the full file itself
    static boolean decompress=false;        // set to TRUE if if compressed datasets should be decompressed 
    
    /**
     * Reads a Galaxy History Archive file and outputs the requested content to STDOUT.
     * Which content this is will depend on the given command-line arguments.
     * If no command-line arguments are provided, a GUI interface will be started up instead
     * @param args The first and only argument should be the path to a file (or URL)
     */
    public static void main(String[] args) {    
        if (args==null || args.length<1) GUI.start(null); // no command-line options are provided. Start the GUI interface instead.
        else {
            try {
                processArguments(args); // process arguments and set the static fields in this class
                if (archiveFile==null) throw new IllegalArgumentException("Missing history file");
                GalaxyHistoryArchive history=new GalaxyHistoryArchive(archiveFile);
                
                if (returnVersionOnly) {
                    try {
                        String format=history.getExportVersion();
                        System.out.println(format);
                    } catch (IOException iox) {
                        System.out.println("-1 ("+iox.toString()+")");
                        System.exit(1);
                    }
                    return;
                } else if (returnMIMEtype) {
                    if (datasetID==null) throw new IllegalArgumentException("A dataset ID (and possibly extra file) must be specified when the 'mime' option is used");
                    System.out.println(getMIMEtype(history,datasetID,extraFile,decompress));
                    return;
                }    
                if (datasetID!=null) { // return a dataset file inside the archive rather than the history itself
                    if (extraFile!=null) {
                        history.outputDatasetExtraFile(System.out, datasetID, extraFile, start, end, decompress); // output an extra file associated with the dataset, such as an image file referenced by an HTML dataset
                    } else {
                        history.outputDataset(System.out, datasetID, start, end, decompress); // output the main dataset file
                    }
                } else { // output the whole history as JSON 
                    history.outputHistoryAsJSON(System.out, true);
                }
            } catch (IllegalArgumentException argEx) {
                System.err.println("Argument error: "+argEx.getMessage());
                showUsage();
                System.exit(1);
            } catch (Exception e) { // this handling could probably be better...
                if (!(e instanceof IOException || e instanceof JsonParseException || e.getClass().equals(java.lang.Exception.class))) e.printStackTrace(System.err);
                else System.err.println("ERROR: "+e.toString()); 
                System.exit(1);
            }
        }
    }  
   
    
    private static void showUsage() {
        System.err.println("Usage: java -jar GalaxyHistoryBrowser.jar -history <tarball> [-format] [-dataset <id> [-extra <filepath>] [-start <int> -end <int>] [-mime] [-decompress]]\n");   
        System.err.println("       If only the history option is provided, a JSON representation of the history will be output to STDOUT.");
        System.err.println("       If 'format' option is selected (along with history option), the version format number of the history file will be returned.");
        System.err.println("          2=latest format, 1=older unsupported format, 0=not a Galaxy history file, -1=unable to process file (this is followed by an error message in parentheses).");
        System.err.println("       If the 'dataset' option is provided (along with history), the raw dataset file will be output to STDOUT. The value should be the 'encoded_id' of a dataset.");
        System.err.println("       If the 'extra' option is provided (along with history and dataset), the raw extra file associated with the dataset will be output to STDOUT.");
        System.err.println("       The value of the extra option should be the filepath of the extra file, relative to the location of the main dataset file.");
        System.err.println("       If the 'start' and/or 'end' options are provided, only a portion of the file (dataset or extra file) will be output.");             
        System.err.println("       If the 'mime' option is selected (along with a dataset or extra file), the MIME type of the dataset (or extra file) is returned.");   
        System.err.println("       If the 'decompress' option is selected, datasets (and extra files) with either '.gz' or '.bz2' file suffixes will be output in their decompressed form.");         
        System.err.println("          If this option is used together with the 'mime' option, the MIME type of the decompressed file is output.");          
        
    }
      
    /**
     * Returns the MIME type of a dataset or extra file inside the history
     * If extrafilepath is NULL, the MIME type of the main file associated with the dataset is returned.
     * If extrafilepath is not NULL, the MIME type of this extra file associated with the dataset is returned.
     * If the file is compressed, e.g. "fastq.gz" it can return either the MIME type of the compressed file ("application/gzip") or the uncompressed file ("text/plain")
     * 
     * @param history a GalaxyHistoryArchive with the processed history 
     * @param dataset The identifier for a dataset inside the history
     * @param extrafilepath (Optional) the path to an extra file associated with the dataset. The "extra_files_path_#" subdirectory prefix will be derived from the dataset and should not be included.
     * @param decompress If TRUE and the dataset/file is compressed, the MIME type of the original uncompressed file will be returned rather than the MIME type of the compression format
     * @return 
     */
    private static String getMIMEtype(GalaxyHistoryArchive history, String dataset, String extrafilepath, boolean decompress) {
        if (extrafilepath!=null) {
            int p=extrafilepath.indexOf('.');
            String ext=extrafilepath.substring((p>0)?p:0); // get file suffix from extra file
            return history.getMIMEtypeFromExtension(ext, decompress);
        } else {
            return history.getMIMEtypeForDataset(dataset, decompress);
        }
    }
    
    
    /**
     * Parses the command-line arguments and sets the static fields in this class to hold them
     * @param args The command-line arguments received by the main(String[] args) method
     * @throws IllegalArgumentException 
     */
    private static void processArguments(String[] args) throws IllegalArgumentException {
        int current=0;
        while(current<args.length) {
           if (args[current].equals("-history")) {
               if (current+1==args.length) throw new IllegalArgumentException("Missing file for history option");
               else archiveFile=stripQuotes(args[current+1]);
               current+=2;
           } else if (args[current].equals("-dataset")) {
               if (current+1==args.length) throw new IllegalArgumentException("Missing ID for dataset option");
               else datasetID=stripQuotes(args[current+1]);
               current+=2;
           } else if (args[current].equals("-extra")) {
               if (current+1==args.length) throw new IllegalArgumentException("Missing file for extra dataset");
               else extraFile=stripQuotes(args[current+1]);
               current+=2;
           } else if (args[current].equals("-start")) {
               if (current+1==args.length) throw new IllegalArgumentException("Missing value for 'start' option");
               else try {
                   start=Integer.parseInt(args[current+1]);
                   if (start<0) throw new NumberFormatException();
               } catch (NumberFormatException e) {
                   throw new IllegalArgumentException("Value for 'start' option must be a positive integer");
               }
               current+=2;               
           } else if (args[current].equals("-end")) {
               if (current+1==args.length) throw new IllegalArgumentException("Missing value for 'end' option");
               else try {
                   end=Integer.parseInt(args[current+1]);
                   if (end<0 || end<=start) throw new NumberFormatException();
               } catch (NumberFormatException e) {
                   throw new IllegalArgumentException("Value for 'end' option must be a positive integer greater than start");
               }
               current+=2;               
           } else if (args[current].equals("-format") || args[current].equals("-version")) {
               returnVersionOnly=true;
               current+=1;
           } else if (args[current].equals("-mime")) {
               returnMIMEtype=true;
               current+=1;
           } else if (args[current].equals("-decompress")) {
               decompress=true;
               current+=1;
           } else throw new IllegalArgumentException("Unrecognized option: "+args[current]);

        }
        if (end>0 && start<0) start=0; // if only 'end' is specified then start is assumed to be 0
        if (start>=0 && end<=start) throw new IllegalArgumentException("The 'end' must be greater than 'start'");     
    }
 
    /**
     * Removes double or single quotes around a string
     * @param string
     * @return The argument string without quotes
     */
    private static String stripQuotes(String string) {
        if (string.startsWith("\"") && string.endsWith("\"")) string=string.substring(1,string.length()-1);
        if (string.startsWith("'") && string.endsWith("'")) string=string.substring(1,string.length()-1);
        return string;
    }
}
