/*
 * Objects of this class represents Galaxy History Archives (tarballs) retrieved from a specific source (File or URL)
 * Initializing the object will parse the metadata in the history archive file and transform it into an internal representation that is more manageable.
 * Several convenience methods in this class allows easy accesss to different attributes of the history 
 * and the contents of individual files within the archive can be accessed directly from InputStreams (for text or binary data) or InputStreamReaders (for text only)
 * 
 */
package no.nels.galaxyhistorybrowser;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

/**
 *
 * @author kjetikl
 */
public class GalaxyHistoryArchive {
    
    private static final HashMap<String,String> MIMETYPES=new HashMap<>(); // a map to convert file suffixes to corresponding MIME types. This is set in static block at the end of this class.
    
    private String archivepath=null; // the local file path or URL of the archive tarball file
    private String version=null;     // the format version of the history archive file. Either 2 (newest format), 1 (older unsupported format) or 0 (not a Galaxy History file) 

    private Map<String,Object> history_metadata=null; // (taken directly from the "history_attrs.txt" JSON file inside the archive)  
    private List<Map> datasets=null;    // raw datasets (taken directly from the "datasets_attrs.txt" JSON file inside the archive)
    private List<Map> collections=null; // raw collections (taken directly from the "collections_attrs.txt" JSON file inside the archive)
    private List<Map> jobs=null;        // raw jobs (taken directly from the "jobs_attrs.txt" JSON file inside the archvie)  
    private Map<String,Object> history=null; // full processed history. This structure includes information from all the four object above.
    
    
    // ----------------------------------------------------------------------------------------------------
    
    /**
     * Creates a new GalaxyHistoryArchive associated with a specific source (either local file or URL)
     * @param archivepath The path to a local archive file or a URL (if starting with http: or https:)
     */
    public GalaxyHistoryArchive(String archivepath) {
        this.archivepath=archivepath;
    }
    
    /**
     * This method will parse all the relevant JSON metadata files inside the history archive file
     * and cache the information in both separate data structures and a full "history" object that 
     * combines information from all the others into a single structure.
     * Note that it is not required to call this method if only a subset of the information about the history is needed.
     * It will then be quicker to just use other accessor methods directly, such as "getHistoryAttribute".
     * 
     * @param attributes If provided, this can limit the metadata attributes that are read from the history archive
     *                   Each key should refer to a metadata set (either "history", "datasets", "collections" or "jobs") and the value should be a String[]
     *                   listing the attributes to retrieve for that metadata set  
     *                   You should not use this parameter unless you known what your are doing.
     */
    private void initialize(Map<String,String[]> attributes) throws IOException {
        String[] datasetAttributes=(attributes!=null)?attributes.get("datasets"):null;
        String[] collectionAttributes=(attributes!=null)?attributes.get("collections"):null;           
        // // if some attribute filters are missing, use sensible defaults instead to limit the amount of information  
        // if (datasetAttributes==null) datasetAttributes=new String[]{"hid","name","extension","file_name","extra_files_path","metadata","dbkey","encoded_id","peek","blurb","visible","create_time","state","job","info"}; 
        // if (collectionAttributes==null) collectionAttributes=new String[]{"hid","encoded_id","element_identifier","element_index","display_name","type","collection","child_collection","elements","hda","state","job","info"};                
        
        if (version==null) version=getExportVersionFromArchive(); // 
        if (version.equals("0")) throw new IOException("This file is probably not a Galaxy history");
        if (version.equals("1")) throw new IOException("This Galaxy history was created with an older version of Galaxy that does not support collections properly");
        if (!version.equals("2")) throw new IOException("Unrecognized history export version: "+version);
        if (history_metadata==null) history_metadata=getHistoryAttributesFromArchive((attributes!=null)?attributes.get("history"):null);
        long size=getHistorySize(); 
        history_metadata.put("history_size",humanReadableSize(size));                
        if (datasets==null) datasets=getDatasetsFromArchive(datasetAttributes);
        if (collections==null) collections=getCollectionsFromArchive(collectionAttributes);
        if (jobs==null) jobs=getJobsFromArchive((attributes!=null)?attributes.get("jobs"):null);
        try { 
            setStatesForDatasets(); // update datasets and collections with their 'state' attributes (and 'job'), which can be found from the jobs
        } 
        catch (IOException ioe) {throw ioe;}    
        catch (Exception ex) {throw new IOException(ex.getMessage(),ex);}   
        history=processHistory(datasetAttributes,collectionAttributes); // Create a new easy-to-use structure to represent the entire history    
    }
    
    /** 
     *  Returns the version number for the history archive format that the history was exported in.
     *  The newest format is "2". The old format "1" does not handle dataset collections properly
     *  and is thus not supported by this library. A return value of "0" means that the file is probably not a Galaxy history file at all.
     *  Although the version numbers are mostly just integers, the method will return a String in case this is changed in the future (e.g. if version "2.1" is introduced)
     *  @return The version number for the archive format used by the history file. Either "2", "1" or "0".
     *  @throws IOException if the history archive file or URL could not be accessed
     */    
    public String getExportVersion() throws IOException {
        if (version==null) version=getExportVersionFromArchive(); // 
        return version;
    }
    
    /** Returns a specific metadata attribute for the history 
     *  @param attribute The name of the metadata attribute, e.g. "name","tags" or "annotation"
     *  @return The value of the metadata attribute (as string)
     *  @throws IOException if the history archive file could not be read or the history metadata file inside the archive could not be processed correctly
     */
    public Object getHistoryAttribute(String attribute) throws IOException {
        if (history_metadata==null) history_metadata=getHistoryAttributesFromArchive(null);
        return history_metadata.get(attribute);
    }
    
    /** Returns all the regular datasets in the history (excluding collections) as a list of Maps
     *  @return A list of dataset objects (represented with Maps)
     *  @throws IOException if the history archive file could not be read or the file inside the archive that contains information about all the datasets could not be processed correctly
     */
    public List<Map> getDatasets() throws IOException {    
        if (datasets==null) datasets=getDatasetsFromArchive(null);
        return datasets;
    }    
    
    /** Returns the dataset that has the given value for the attribute
     *  @param attribute The name of a dataset attribute, e.g. "encoded_id" or "extension"
     *  @param value The value that the dataset should have for the attribute
     *  @return A map representing a dataset that has the given value for the specified attribute. If more than one dataset has this value, the "first one encountered" will be returned
     *  @throws IOException if the history archive file could not be read or the file inside the archive that contains information about all the datasets could not be processed correctly     
     */
    public Map getDataset(String attribute, Object value) throws IOException {    
        if (datasets==null) datasets=getDatasetsFromArchive(null);
        return getDataset(datasets, attribute, value);
    }        
    
    /** Returns all the collections in the history as a list of Maps
     *  @return A list of collection objects (represented with Maps)
     *  @throws IOException if the history archive file could not be read or the file inside the archive that contains information about all the collections could not be processed correctly     
     */    
    public List<Map> getCollections() throws IOException {    
        if (collections==null) collections=getCollectionsFromArchive(null);
        return collections;
    } 
    
    /**
     * Returns a list of all the jobs associated with the history
     * @return A list of job objects (represented with Maps)
     * @throws IOException if the history archive file could not be read or the file inside the archive that contains information about all the jobs could not be processed correctly  
     */
    public List<Map> getJobs() throws IOException {    
        if (jobs==null) jobs=getJobsFromArchive(null);
        return jobs;
    }     
    
    /**
     * Returns the job object for the given job ID or NULL if no job with that ID was found
     * @param jobID the "encoded_id" attribute of the jobs
     * @return A map object representing the job
     * @throws IOException if the history archive file could not be read or the file inside the archive that contains information about all the jobs could not be processed correctly  
     */
    public Map<String,Object> getJob(String jobID) throws IOException {    
        if (jobs==null) jobs=getJobsFromArchive(null);
        for (Map job:jobs) {
            String encoded_id=(String)job.get("encoded_id");
            if (jobID.equals(encoded_id)) return job;
        }
        return null;
    }      
    
    /** Returns an easy-to-use representation of the history that includes both regular datasets and collections in anti-chronological order
     *  Note that the datasets do not contain a full set of metadata, but only some preselected attributes
     *  @return A map object representing all the necessary information about the history. It has two keys that references other objects: "metadata" (Map) and "contents" (List of all datasets/collections)
     *  @throws IOException if the history archive file itself could not be read or any of the metadata files inside the archive could not be processed correctly 
     */
    public Map<String,Object> getHistory() throws IOException {    
        if (history==null) initialize(null); // note. Since the initialize() method has been made public. There are now no methods that call it with any other argument than 'null'
        return history;
    }   
    
    
    /** Returns an easy-to-use JSON representation of the history that includes both regular datasets and collections in anti-chronological order
     *  Note that the datasets do not contain a full set of metadata, but only some preselected attributes
     *  @param pretty If TRUE, the JSON string will include indentations and newlines that make it more readable for humans
     *  @return a JSON string representing the history
     *  @throws IOException if the history archive file itself could not be read or any of the metadata files inside the archive could not be processed correctly 
     */
    public String getHistoryAsJSON(boolean pretty) throws IOException {    
        if (history==null) initialize(null);
        try {
            JsonFactory factory=new JsonFactory();
            ObjectMapper mapper = new ObjectMapper(factory);
            if (pretty) mapper.enable(SerializationFeature.INDENT_OUTPUT);            
	    return mapper.writeValueAsString(history); 
        } catch (Exception ex) {
            throw new IOException(ex.getMessage(),ex);
        }
    }     
    
    /** Outputs an easy-to-use JSON representation of the history that includes both regular datasets and collections in anti-chronological order
     *  Note that the datasets do not contain a full set of metadata, but only some preselected attributes
     *  @param outstream The stream that the history should be written to
     *  @param pretty If TRUE, the JSON string will include indentations and newlines that make it more readable for humans
     *  @throws IOException if the history archive file itself could not be read or any of the metadata files inside the archive could not be processed correctly 
     */
    public void outputHistoryAsJSON(OutputStream outstream, boolean pretty) throws IOException {
        if (history==null) initialize(null);    
        try {
            JsonFactory factory=new JsonFactory();
            factory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false); // prevents the mapper from closing the stream after writing first value
            ObjectMapper mapper = new ObjectMapper(factory);
            if (pretty) mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(outstream,history); 
        } catch (Exception ex) {
            throw new IOException(ex.getMessage(),ex);
        }        
    }    
    
    /**
     * Given a filename extension, this method returns the corresponding MIME type, e.g. "fastq" will return "text/plain" and "pdf" will return "application/pdf" 
     * For compressed datafiles with a double file suffix, such as "fastqsanger.gz" or "fasta.bz2",
     * it can either return the compressed MIME type (if decompress option is FALSE) or the MIME type of the decompressed dataset (if decompress is TRUE).
     * 
     * @param ext The file extension. This could either be a single suffix or multiple suffixes separated by dots. Usually, only the last suffix will be considered.
     * @param decompress if this is TRUE and the extension contains a double file suffix, the MIME type of the second to last suffix is returned rather than the MIME type of the last suffix
     * @return The MIME-type corresponding to the extension. If the suffix is unknown, it will return "text/plain" as a default (since most data types in Galaxy are based on plain text, e.g. BED, GFF, FASTA, CSV, SAM)
     */
    public String getMIMEtypeFromExtension(String ext, boolean decompress) {
        if (ext==null || ext.isEmpty()) return "text/plain";
        if (ext.startsWith(".")) ext=ext.substring(1);
        if (ext.equals("tgz")) ext="tar.gz"; // some manual processing here to handle special case...
        String suffix=ext;
        if (ext.contains(".")) {
            String[] parts=ext.split("\\.");
            if (parts.length>=2 && decompress) suffix=parts[parts.length-2]; // use second to last suffix
            else suffix=parts[parts.length-1]; // use last suffix
        }
        if (MIMETYPES.containsKey(suffix)) return MIMETYPES.get(suffix);
        return "text/plain";
    }
    
    /**
     * Given the ID of a dataset, this method returns the corresponding MIME type of the file. 
     * E.g. a dataset with type "fastq" will return "text/plain" and "pdf" will return "application/pdf".
     * @param datasetID The "encoded_id" attribute of the dataset
     * @param decompress If this is set to TRUE and the dataset is a compressed file (GZIP or BZIP2), the MIME type of the decompressed file will be returned rather than the compression MIME type
     * @return the MIME type of the dataset
     */
    public String getMIMEtypeForDataset(String datasetID, boolean decompress) {
        try {
            Map<String,Object> dataset=getDataset("encoded_id",datasetID);
            if (dataset!=null) {
                return getMIMEtypeFromExtension((String)dataset.get("extension"), decompress);
            } else return "text/plain";
        } catch (Exception e) {
            return "text/plain"; //
        }
        
    }
    
    /**
     * Returns an estimated size of the history by summing up the sizes of all the files within the history archive file's 'datasets' subdirectory.
     * The size is returned as a long number, but you can use the humanReadableSize method to convert it into a more convenient representation.
     * @return The size of the history, or -1 if something went wrong
     */
    public long getHistorySize() {
        long size=-1;
        try {
            InputStream source=(archivepath.startsWith("http:") || archivepath.startsWith("https:"))?((new URL(archivepath)).openStream()):new FileInputStream(archivepath);
            TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(source));
            TarArchiveEntry currentEntry = tarInput.getNextTarEntry();
            while (currentEntry != null) {
                if (currentEntry.getName().startsWith("datasets/")) size+=currentEntry.getSize();
                currentEntry = tarInput.getNextTarEntry(); 
            }
        } catch (Exception e) {} // unable to estimate history size. 
        return size;         
    } 
    
    /** Converts a file size measured in bytes into a string more readable by humans.
     *  The conversion assumes that each unit prefix is 1024 times greater than the previous (rather than 1000). This is the same in Galaxy itself.
     *  @param bytes The size of a file in bytes 
     *  @return A human readable size 
     */
    public String humanReadableSize(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %cB", value / 1024.0, ci.current()).replace(",", "."); // convert comma decimal symbol to dot after formatting
    }      
    
    // ---------------------------------------------------------------------------------------    
         
    /** 
     *  Returns an InputStreamReader that reads from a specific file within the archive.
     *  Since the method returns a Reader, this should be a text file (not binary).
     *  This method is mostly used to access the JSON metadata files.
     *  @param filepath The path to a file inside the archive tarball. 
     *  @return An InputStreamReader that allows direct read access to the file
     *  @throws IOException if the history archive file is not readable or the specified file is not found inside the archive
     */
    public InputStreamReader getInputStreamReaderForFile(String filepath) throws IOException {
        InputStream source=(archivepath.startsWith("http:") || archivepath.startsWith("https:"))?((new URL(archivepath)).openStream()):new FileInputStream(archivepath);
        TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(source));
        TarArchiveEntry currentEntry = tarInput.getNextTarEntry();
        while (currentEntry != null && !currentEntry.getName().equals(filepath)) {
            currentEntry = tarInput.getNextTarEntry(); // You forgot to iterate to the next file
        }
        if (currentEntry!=null && currentEntry.getName().equals(filepath)) {
            if (filepath.endsWith(".gz")) return new InputStreamReader(new GzipCompressorInputStream(tarInput));
            else if (filepath.endsWith(".bz2")) return new InputStreamReader(new BZip2CompressorInputStream(tarInput));
            else return new InputStreamReader(tarInput);             
        } else throw new ArchiveFileNotFoundException("Unable to locate archive file '"+filepath+"'");
    }      
    
    /** 
     *  Returns an InputStream that streams from a specific file within the archive.
     *  This file could be a text file or a binary file. This is the standard 
     *  @param filepath The path to a file inside the archive tarball. 
     *  @param decompress If the decompress parameter is TRUE, compressed files inside the archive (with either '.gz' or '.bz2' file suffix) will be decompressed automatically
     *  @return An InputStream that allows direct read access to the file     
     *  @throws IOException if the history archive file is not readable or the specified file is not found inside the archive
     */
    public InputStream getInputStreamForFile(String filepath, boolean decompress) throws IOException {
        InputStream source=(archivepath.startsWith("http:") || archivepath.startsWith("https:"))?((new URL(archivepath)).openStream()):new FileInputStream(archivepath);
        TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(source));
        TarArchiveEntry currentEntry = tarInput.getNextTarEntry();
        while (currentEntry != null && !currentEntry.getName().equals(filepath)) {
            currentEntry = tarInput.getNextTarEntry(); // You forgot to iterate to the next file
        }
        if (currentEntry!=null && currentEntry.getName().equals(filepath)) {
            if (filepath.endsWith(".gz") && decompress) return new GzipCompressorInputStream(tarInput);
            else if (filepath.endsWith(".bz2") && decompress) return new BZip2CompressorInputStream(tarInput);
            return tarInput;             
        } else throw new ArchiveFileNotFoundException("Unable to locate archive file '"+filepath+"'"); // ***
    }      
    
    /**
     * Outputs a full dataset file to an output stream
     * @param outstream The stream to output the dataset to
     * @param datasetID The "encoded_id" attribtute of the dataset to output
     * @param decompress If the decompress parameter is TRUE, compressed files inside the archive (with either '.gz' or '.bz2' file suffix) will be decompressed 
     * @throws IOException if the history archive file itself could not be read or the contents of the archive file could not be properly processed or the dataset with the given ID could not be accessed
     */    
     public void outputDataset(OutputStream outstream, String datasetID, boolean decompress) throws IOException {
         outputDataset(outstream, datasetID, -1, -1, decompress);
     }   
    /**
     * Outputs a selected section of a dataset file to an output stream
     * @param outstream The stream to output the dataset to
     * @param datasetID The "encoded_id" of the dataset to output
     * @param start If start greater than 0 and end is greater than start, then this specifies the start of the section of the file to output
     * @param end If start greater than 0 and end is greater than start, then this specifies the end of the section of the file to output
     * @param decompress If the decompress parameter is TRUE, compressed files inside the archive (with either '.gz' or '.bz2' file suffix) will be decompressed 
     * @throws IOException if the history archive file itself could not be read or the contents of the archive file could not be properly processed or the dataset with the given ID could not be accessed     * 
     */
    public void outputDataset(OutputStream outstream, String datasetID, int start, int end, boolean decompress) throws IOException {
        Map<String,Object> dataset=getDataset("encoded_id", datasetID);
        if (dataset==null) throw new IOException("Dataset with ID ["+datasetID+"] not found");
        String filename=(String)dataset.get("file_name");
        if (filename==null) throw new IOException("Missing filepath for dataset");
        InputStream stream=getInputStreamForFile(filename, decompress);
        if (start>=0 && end>start) { // output just a selected section
            int bytes=end-start+1;
            byte[] buffer=new byte[bytes];
            stream.skip(start); // skip the first 'start' bytes from the buffer
            int length=stream.read(buffer, 0, buffer.length);
            if (length>0) outstream.write(buffer,0,length);
        } else { // output the whole file
            int length=0;
            byte[] buffer=new byte[100000]; // read ~100kb at a time
            while(length>=0) {
                length=stream.read(buffer);
                if (length>0) outstream.write(buffer,0,length);
            }
        }
    }
    
    /**
     * Outputs an extra file associated with a dataset directly to an output stream
     * @param outstream The stream to output the dataset to   
     * @param datasetID The "encoded_id" of the parent dataset
     * @param filename The name/path of the extra file (this could include subdirectory prefixes)
     * @param decompress If the decompress parameter is TRUE, compressed files inside the archive (with either '.gz' or '.bz2' file suffix) will be decompressed
     * @throws IOException if the history archive file itself could not be read or the contents of the archive file could not be properly processed or an extra file with the given path is not associated with a dataset with the given ID
     */    
     public void outputDatasetExtraFile(OutputStream outstream, String datasetID, String filename, boolean decompress) throws IOException {
         outputDatasetExtraFile(outstream, datasetID, filename, -1, -1, decompress);
     }  
     
    /**
     * Outputs a selected section of an extra file associated with a dataset to an output stream
     * @param outstream The stream to output the dataset to    
     * @param datasetID The "encoded_id" of the parent dataset
     * @param filename The name of the extra file (could be a path that includes subdirectory prefix)  
     * @param start If start greater than 0 and end is greater than start, then this specifies the start of the section of the file to output
     * @param end If start greater than 0 and end is greater than start, then this specifies the end of the section of the file to output
     * @param decompress If the decompress parameter is TRUE, compressed files inside the archive (with either '.gz' or '.bz2' file suffix) will be decompressed 
     * @throws IOException if the history archive file itself could not be read or the contents of the archive file could not be properly processed or an extra file with the given path is not associated with a dataset with the given ID
     */
    public void outputDatasetExtraFile(OutputStream outstream, String datasetID,  String filename, int start, int end, boolean decompress) throws IOException {
        Map<String,Object> dataset=getDataset("encoded_id", datasetID);
        if (dataset==null) throw new IOException("Dataset with ID ["+datasetID+"] not found");
        if (filename==null) throw new IOException("Missing filename (path) for dataset");
        String directory=(String)dataset.get("extra_files_path");
        if (directory==null) throw new IOException("Dataset does not include extra files");
        filename=directory+"/"+filename;
        InputStream stream=getInputStreamForFile(filename, decompress);
        if (start>=0 && end>start) { // output just a selected section
            int bytes=end-start+1;
            byte[] buffer=new byte[bytes];
            stream.skip(start); // skip the first 'start' bytes from the buffer
            int length=stream.read(buffer, 0, buffer.length);
            if (length>0) outstream.write(buffer,0,length);
        } else { // output the whole file
            int length=0;
            byte[] buffer=new byte[100000]; // read ~100kb at a time
            while(length>=0) {
                length=stream.read(buffer);
                if (length>0) outstream.write(buffer,0,length);
            }
        }
    }    
    
    /**
     * Reads the contents of a specified JSON file within the history archive file 
     * and returns a representation of the contents consisting of List, Map and Basic Types (String,Integer,Double,Boolean and null)
     * @param filename The path to the JSON file within the archive
     * @param attributes If this list is provided (not null), attribute fields that are not included here will be skipped (note that this also applies to nested attributes)
     * @return Either a List or Map depending on the top-most element in the JSON file
     * @throws IOException if the history archive file itself could not be read, or the specified file inside the archive could not be found or properly parsed as JSON
     */
    public Object readJSON(String filename, String[] attributes) throws IOException {    
        try {
            InputStreamReader reader=getInputStreamReaderForFile(filename);
            SimpleJSONparser parser=new SimpleJSONparser();
            return parser.parseJSON(reader, attributes);   
        } catch (JsonParseException jpe) {
            throw new IOException(jpe.getMessage(), jpe);
        }        
    }       
    
    // --------------------  ONLY PRIVATE METHODS BELOW THIS LINE -------------------------------------------------------------------        
    
    /** 
     * Returns the format version number the archive was exported in (NB: Value is returned as a String)
     * @return 2: if the Galaxy history tarball is the new format
     *         1: if the history file is in the old format
     *         0: if this file is not a Galaxy history (or it unable to determine the format)
     * @throws IOException If something went wrong while processing the file
     */
    private String getExportVersionFromArchive() throws IOException {
        boolean version_file_found=true;
        InputStreamReader reader=null;
        try {
             reader=getInputStreamReaderForFile("export_attrs.txt");
        } catch (ArchiveFileNotFoundException ffn) {
            if (ffn.getMessage().startsWith(""))
            version_file_found=false;                       
        } catch (IOException iox) {
            if (iox.getMessage().equals("Input is not in the .gz format")) return "0";
            else throw iox;
        }
        if (!version_file_found) {
            try {
                reader=getInputStreamReaderForFile("history_attrs.txt"); // The "export_attrs.txt" file is missing, but it could just be an old archive. Check for a file that should be present.
            } catch (ArchiveFileNotFoundException ffn) {
                return "0";                     
            }  
            if (reader!=null) return "1"; // the older export format does not have an "export_attrs.txt" file that specifies the version
        }
        SimpleJSONparser parser=new SimpleJSONparser();
        try {
            Object result=parser.parseJSON(reader, new String[]{"galaxy_export_version"});            
            if (result instanceof Map && ((Map)result).containsKey("galaxy_export_version")) {
                return ((Map)result).get("galaxy_export_version").toString();
            } else  return "0"; // throw new Exception("Unable to determine export format version.");
        } catch (JsonParseException parseEx) {
            return "0"; // throw new Exception("Unable to determine export format version.");
        }
    }
    

    /**
     * Returns the history's metadata attributes as a Map, 
     * including the name, annotation, tags (as comma-separated list), creation_time and update_time
     * @param attributes if a list of attributes is provided, only values for these will be returned. If null, then all will be returned
     * @return 
     * @throws IOException if the history archive file itself could not be read or the 'history_attrs.txt' file inside the archive could not be parsed correctly     * 
     */
    private Map<String,Object> getHistoryAttributesFromArchive(String[] attributes) throws IOException  {
        try {
            InputStreamReader reader=getInputStreamReaderForFile("history_attrs.txt");
            SimpleJSONparser parser=new SimpleJSONparser();            
            Object result=parser.parseJSON(reader,attributes); //          
            if (result instanceof Map) {
//                Object tags=((Map)result).get("tags"); // format tags as a single comma-separated string
//                if (tags instanceof List) {
//                    String tagslist=splice((List)tags, ",");
//                    ((Map)result).put("tags", tagslist);                  
//                } 
                Object an=((Map)result).get("annotation");
                if (an==null || an.toString().equals("null")) ((Map)result).put("annotation",""); // replace annotation with empty string if missing or null
            } else throw new IOException("Unable to read history attributes. Return value from JSON parser was not a map.");   
            return (Map<String,Object>)result;
        } catch (JsonParseException jpe) {
            throw new IOException(jpe.getMessage(), jpe);
        }
    }    
    
    /**
     * Returns information about the regular datasets in the history. 
     * Datasets within collections are included, but not the collections themselves.
     * The dataset is uniquely identified by its "encoded_id" attribute.
     * The "hid" attribute of a dataset is its chronological number in the history.
     * @param attributes If this list is provided (not null), attribute fields that are not included here will be skipped (note that this also applies to nested attributes)
     * @return A List of Maps where each map represents a dataset
     * @throws IOException if the history archive file itself could not be read or the 'datasets_attrs.txt' file inside the archive could not be parsed correctly
     */
    private List<Map> getDatasetsFromArchive(String[] attributes) throws IOException {    
        try {       
            InputStreamReader reader=getInputStreamReaderForFile("datasets_attrs.txt");
            SimpleJSONparser parser=new SimpleJSONparser();
            Object result=parser.parseJSON(reader,attributes);   
            if (result instanceof List) return (List<Map>)result;
            else throw new IOException("Unable to parse history datasets. Return value from JSON parser was not a list.");
        } catch (JsonParseException jpe) {
            throw new IOException(jpe.getMessage(), jpe);
        }
    }  
    
    /**
     * Returns information about the collections in the history. 
     * @param attributes If this list is provided (not null), attribute fields that are not included here will be skipped (note that this also applies to nested attributes)
     * @return A List of Maps where each map represents a dataset
     * @throws IOException if the history archive file itself could not be read or the 'collections_attrs.txt' file inside the archive could not be parsed correctly 
     */
    private List<Map> getCollectionsFromArchive(String[] attributes) throws IOException {  
        try {             
            InputStreamReader reader=getInputStreamReaderForFile("collections_attrs.txt");
            SimpleJSONparser parser=new SimpleJSONparser();
            Object result=parser.parseJSON(reader,attributes);                     
            if (result instanceof List) return (List<Map>)result;
            else throw new IOException("Unable to parse history collections: "+result);
        } catch (JsonParseException jpe) {
            throw new IOException(jpe.getMessage(), jpe);
        }            
    }  
    
    /**
     * Returns information about the jobs in the history. 
     * @param attributes If this list is provided (not null), attribute fields that are not included here will be skipped (note that this also applies to nested attributes)
     * @return A List of Maps where each map represents a jobs
     * @throws IOException if the history archive file itself could not be read or the 'jobs_attrs.txt' file inside the archive could not be parsed correctly 
     */
    private List<Map> getJobsFromArchive(String[] attributes) throws IOException {    
        try {        
            InputStreamReader reader=getInputStreamReaderForFile("jobs_attrs.txt");
            SimpleJSONparser parser=new SimpleJSONparser();
            Object result=parser.parseJSON(reader,attributes);                     
            if (result instanceof List) return (List<Map>)result;
            else throw new IOException("Unable to parse history jobs: "+result);
        } catch (JsonParseException jpe) {
            throw new IOException(jpe.getMessage(), jpe);
        }              
    }      
    
    /**
     * Sets a 'state' attribute for each dataset and collection (for convenience) based on the state of the associated job.
     * It also adds a 'job' attribute to each dataset (but not collections) pointing back to the job. (Collections can contains datasets created by different jobs)
     * @param datasets
     * @param collections
     * @param jobs
     * @throws Exception 
     */
    private void setStatesForDatasets() throws Exception { // 
        for (Map<String,Object> dataset:datasets) {
            String encoded_id=(String)dataset.get("encoded_id");
            String original_id=encoded_id;
            List chain=(List)dataset.get("copied_from_history_dataset_association_id_chain"); // if the dataset is a copy of another, it will not have its own job. Check the job of the original dataset instead
            if (chain!=null && !chain.isEmpty()) original_id=(String)chain.get(chain.size()-1); // last ID in chain refers to the original dataset
            Map<String,Object> job=findJob(jobs,original_id);
            if (job!=null) { // some datasets may not have jobs
                dataset.put("job", job.get("encoded_id")); // add a reference to the job in the dataset
                String state=(String)job.get("state");
                if (state!=null) dataset.put("state",state);
                else {
                    // System.err.println("WARNING:Unable to find state for dataset: "+encoded_id+" ("+dataset.get("name")+")");
                    dataset.put("state","ok"); // Unable to find the state. Just set it to "ok"...
                }
            } else dataset.put("state","ok"); // assume default state is OK if no job exists
        }
        // now do collections...
         for (Map<String,Object> collection:collections) {
            Map<String,Object> contents=(Map<String,Object>)collection.get("collection");
            int[] states=countStatesInCollection(contents); // sideeffect: this will also update state in child collections
            collection.put("state", getStateFromCounts(states));
        }       
    }
    
    /** finds the job associated with the dataset from a list of jobs */
    private Map<String,Object> findJob(List<Map> jobs, String dataset_id) throws IOException {
        for (Map<String,Object> job:jobs) {
            //String jobstate=(String)job.get("state");
            Object output_dataset_mapping=job.get("output_dataset_mapping");
            if (output_dataset_mapping==null) continue;
            if (output_dataset_mapping instanceof Map) {
                for (Object key:((Map)output_dataset_mapping).keySet()) {
                    Object value=((Map)output_dataset_mapping).get(key);
                    if (value instanceof List) {
                        for (Object x:((List)value)) {
                            if (dataset_id.equals(x)) return job; // we found it!!
                        }
                    } else throw new IOException("Archive Format Error: Unexpected value for 'output_dataset_mapping' field. Expected a list but got: "+((value!=null)?value.getClass():"null"));
                }
            } else throw new IOException("Archive Format Error: Unexpected value for 'output_dataset_mapping'. Expected a map but got: "+output_dataset_mapping.getClass());            
        } 
        return null;
    } 
    
    /** Retrieves the (cached) state of the dataset with the given ID. 
     *  Note that information about the state is not included for the datasets by default, but only for their associated jobs.
     *  The method setStatesForDatasets() will go through each dataset and create a new state attribute by examining their job,
     *  so this method has to be run first (or else all calls to this method will simply return null).
     *  
     *  @param the unique ID of the dataset ("encoded_id" attribute)
     *  @return A string representing the state of the dataset, e.g. "ok" or "error". Returns null if the dataset with that ID was not found or it had no state set.
     *  @throws IOException if the history archive file could not be read or the file inside the archive that contains information about all the datasets could not be processed correctly  
     */
    private String getStateForDataset(String id) throws IOException {       
        Map<String,Object> dataset=getDataset("encoded_id", id);
        if (dataset!=null) return (String)dataset.get("state"); 
        else return null;
    }
    
    /**
     * This method is used to derive a state for a collection
     * based on the states of the datasets in the collection.
     * 
     * @param states A count of the different states that the datasets in the collection have
     * @return 
     */
    private String getStateFromCounts(int[] states) {
        int total=0;for (int i=0;i<states.length;i++) total+=states[i];
             if (states[1]>0) return "error";
        else if (states[3]>0) return "running";
        else if (states[2]>0) return "waiting";
        else if (states[5]>0) return "paused";        
        else if (states[4]==total) return "deleted";
        else if (states[6]>0) return "other"; // not a Galaxy state
        else return "ok";    
    }
    
    /**
     * Counts the number of times each state is encountered among datasets in the collection.
     * For nested collections, the count is performed recursively. It assumes that datasets have already been assigned "state" attributes, for instance with setStatesForDatasets()
     * @param collection A collection from the original list of 
     * @return an array with 7 count values for the different states [OK,error,waiting,running,deleted,paused,other]. Waiting includes 'new' and 'queued'. Running includes 'upload'
     */
    private int[] countStatesInCollection(Map<String,Object> collection) throws Exception {
        int[] states=new int[7]; // 
        // Map<String,Object> contents=(Map<String,Object>)collection.get("collection");
        List<Map> elements=(List<Map>)collection.get("elements");
        if (elements==null) throw new Exception("No 'elements' entry in expected collection object ["+collection.get("encoded_id")+"]");
        for (Map<String,Object> element:elements) { // process all the elements in the collection
            String type=(String)element.get("element_type");
            if (type.equals("hda")) {
                Map<String,Object> hda=(Map<String,Object>)element.get("hda");
                String dataset_id=(String)hda.get("encoded_id");
                String state=getStateForDataset(dataset_id);
                if (state!=null) {
                         if (state.equalsIgnoreCase("ok")) states[0]++;
                    else if (state.equalsIgnoreCase("error")) states[1]++;
                    else if (state.equalsIgnoreCase("waiting") || state.equalsIgnoreCase("new") || state.equalsIgnoreCase("queued")) states[2]++;
                    else if (state.equalsIgnoreCase("running") || state.equalsIgnoreCase("upload")) states[3]++;
                    else if (state.equalsIgnoreCase("deleted") || state.equalsIgnoreCase("deleted_new")) states[4]++;
                    else if (state.equalsIgnoreCase("paused")) states[5]++;
                    else states[6]++;
                } else states[6]++; // no state is counted as "other"
            } else if (type.equals("dataset_collection")) { // this would be a nested collection
                Map<String,Object> childcollection=(Map<String,Object>)element.get("child_collection");
                int[] childstates=countStatesInCollection(childcollection);
                // set state on this child collection before propagating to the parent
                element.put("state", getStateFromCounts(childstates));
                for (int i=0;i<childstates.length;i++) states[i]+=childstates[i];
            } else throw new Exception("Encountered unexpected element type while processing collection states: "+type);
        }
        return states;
    }
    
    /**
     * Returns a representation of the full history, including metadata and both regular datasets and collections. 
     * The returned Map object contains two fields "metadata" and "contents", where the metadata object contains information about the history itself,
     * and the contents points to a list containing all the datasets/collections in anti-chronological order (i.e. newest history element is first in list).
     * Each element has a "class" attribute, which can be either "dataset" or a collection type ("list", "paired" or "list:paired").
     * Collections have an attribute called "elements" (List) containing either datasets or other (nested) collections.
     * @param datasetAttributes a list of dataset attributes to keep. If provided (not null), those attributes that are not included in the list will be ignored
     * @param collectionAttributes a list of collection attributes to keep. If provided (not null), those attributes that are not included in the list will be ignored
     * @return a Map object representing the history and all its contents
     * @throws IOException if the history archive file itself could not be read or any of the metadata files inside the archive could not be processed correctly
     */
    private Map<String,Object> processHistory(String[] datasetAttributes, String[] collectionAttributes) throws IOException {
        ArrayList<Map> historylist=new ArrayList<>(); 
        getDatasets();  // force import of datasets if they have not already been read     
        getCollections(); // force import of collections if they have not already been read 
        // I want to make slight modifications to the structures so I make copies of the current datasets/collections
        ArrayList<Map> history_datasets=(ArrayList<Map>)deepCopy(datasets, datasetAttributes); 
        ArrayList<Map> history_collections=(ArrayList<Map>)deepCopy(collections, collectionAttributes); 
        
        // Perform simple pre-processing of datasets to clean them up a bit.
        for (Map<String,Object> dataset:history_datasets) {
            dataset.put("class","dataset");
            dataset.put("size",dataset.get("blurb")); dataset.remove("blurb"); // rename "blurb" to "size" (even though "size" is not really consistent)           
            dataset.put("dbkey", ((Map)dataset.get("metadata")).get("dbkey")); // lift "dbkey" up to top-level instead of having it below "metadata"
            dataset.remove("metadata"); // this map should now be empty so we can remove it (since 'dbkey' is the only metadata attribute included in the list above)      
        }
        // Process collections first. Datasets within the collection are moved out of the datasets list and inserted directly as children of the collections
        for (Map<String,Object> collection:history_collections) {          
            collection.put("name",collection.get("display_name")); collection.remove("display_name"); // rename "display_name" to "name" to make it consistent with regular datasets          
            Map<String,Object> contents=(Map<String,Object>)collection.get("collection");
            collection.put("class",contents.get("type")); // The "type" attribute is lifted one level up and called "class" in the parent
            processCollection(contents, history_datasets); // goes through the collection, processing each element and adds datasets directly to the leaf nodes
            historylist.add(collection);
        }
        // Now add the remaining datasets that were not part of collections. The datasets that should be visible at the top level in the history have the attribute "visible:true"
        for (Map<String,Object> dataset:history_datasets) {
            if ((boolean)dataset.get("visible")) {
                historylist.add(dataset);
            }
        }
        Collections.sort(historylist, new ChronologicalOrderComparator(false)); // sort in anti-chronological order (by HID)
        HashMap<String,Object> fullhistory=new HashMap<>();
        fullhistory.put("metadata", deepCopy(history_metadata, null));
        fullhistory.put("contents",historylist);
        return fullhistory;
    }
    
    /**
     * This method is used to perform additional processing / restructuring of a collection element after it has been created from the "collections_attrs.txt" metadata file.
     * It will add all the datasets that are part of this collection itself 
     * (In the original metadata files the "leaf node" datasets are kept completely separate from the collections themselves and they are only referenced by the collection)
     * Subcollections will be processed recursively by this same method
     * @param collection A collection object
     * @param datasets A list containing all the datasets in the history. The ones that are relevant will be used.
     * @throws IOException if the history archive file itself could not be read or any of the metadata files inside the archive could not be processed correctly
     */
    private void processCollection(Map collection, List<Map> datasets) throws IOException {
        List<Map> elements=(List<Map>)collection.get("elements"); // these are the entries in the collection
        collection.put("collection_size",elements.size()); // add the size as attribute of the collection to ease processing by browser
        String type=(String)collection.get("type");        
        int index=0;
        for (Map element:elements) {
            if (((int)element.get("element_index"))!=index) throw new IOException("Archive Format Error: List element out of order");
            element.put("name", element.get("element_identifier")); element.remove("element_identifier"); // rename "element_identifier" to "name" for simplicity
            if (type.equals("list") || type.equals("paired")) { // these list types are not nested and can be processed in the same way;
                String elementID=(String)element.get("encoded_id");
                String datasetID=(String)((Map)element.get("hda")).get("encoded_id");                
                Map dataset=getDataset(datasets, "encoded_id", datasetID);
                if (dataset==null) throw new IOException("Archive Format Error: Dataset ["+datasetID+"] not found in datasets list");
                dataset=(Map)deepCopy(dataset, null); // the same dataset can be referenced in many places, but we make individual copies 
                dataset.put("element_encoded_id",elementID); // this should be unique 
                element.put("dataset", dataset); // add the dataset directly as an attribute of this element in the collection    
            } else if (type.equals("list:paired")) {
                 Map contents=(Map)element.get("child_collection");
                 if (contents==null) throw new IOException("Archive Format Error: Child collection not found");
                 element.put("class",contents.get("type")); // The "type" attribute is lifted one level up and called "class" in the parent
                 processCollection(contents, datasets); // process nested collection recursively
                 element.put("collection",contents); element.remove("child_collection"); // rename "child_collection" attribute to "collection" for simplicity
            } else throw new IOException("Archive Format Error: Unrecognized collection type: "+type); 
            index++;
        }                                    
    }
    
    
    /** 
     * Searches for a dataset object (Map) with the given attribute value in a list and returns it
     * @param list The list of dataset objects to search in
     * @param attribute The name of the attribute field (map key)
     * @param value The value that the object should have for the attribute
     * @return The object (Map) that has the given value for the attribute, or NULL if no such object is found
     */   
    private Map getDataset(List<Map> list, Object attribute, Object value) {
        Map found=null;
        for (Map map:list) {
            Object objectvalue=map.get(attribute);
            // System.err.println("Searching for ["+attribute+"="+value+"]. Found ["+attribute+"="+objectvalue+"]"); // DEBUG
            if (objectvalue!=null && value.equals(objectvalue)) found=map;
        }
        return found;
    }      

    /** 
     * Converts a list into a String where the elements are separated by the specified character(s) 
     * @param list The list which shall be converted into a String
     * @param separator The character(s) to be used to separate the elements in the list in the resulting string
     * @return a concatenation of all the elements in the list into a single string
     */
    private String splice(List list, String separator) {
        StringBuilder string=new StringBuilder();
        int size=list.size();
        for (int i=0;i<size;i++) {
            string.append(list.get(i));
            if (i<size-1) string.append(separator);
        }
        return string.toString();
    }    

    
    /**
     * Returns a deep copy of the object which consists of nested Maps and Lists
     * @param object A Map or a List (with potentially nested Maps and Lists)
     * @param attributes If provided, only the given attributes will be kept for maps (note that this is applied recursively)
     * @return 
     */
    private Object deepCopy(Object object, String[] attributes) {
        Object copy=null;
        if (object instanceof List) {
            copy=new ArrayList<Map>(((List)object).size());
            for (Object item:((List)object)) {
                if (item instanceof Map || item instanceof List) item=deepCopy(item,attributes);
                ((ArrayList)copy).add(item);
            }
        } else if (object instanceof Map) {
            copy=new HashMap();     
            Set<String> keys=((Map)object).keySet();
            if (attributes!=null) keys.retainAll(Arrays.asList(attributes)); // intersection of keys and specified attributes
            for (String key:keys) {
                Object value=((Map)object).get(key);
                if (value instanceof Map || value instanceof List) value=deepCopy(value,attributes);
                ((HashMap)copy).put(key,value);
            }          
        }  else System.err.println("Unexpected occurrence in DeepCopy(). object is "+object);
        return copy;
    }
    
    /** 
     * Compares the HID of two history elements in order to sort them in numerical order (ascending or descending) 
     */
    private class ChronologicalOrderComparator implements Comparator<Map> {
        private int sign=1;
        
        public ChronologicalOrderComparator(boolean ascending) {
            if (!ascending) sign=-1;
        }
        @Override
        public int compare(Map m1, Map m2) {
            int hid1=(int)m1.get("hid");
            int hid2=(int)m2.get("hid");
            return (hid1-hid2)*sign;
        }       
    }    

    /**
     * A new exception used to signal that the requested file was not found within the archive tarball
     */
    private class ArchiveFileNotFoundException extends FileNotFoundException {
        
        public ArchiveFileNotFoundException(String msg) {
            super(msg);
        }
    }
    
    
    static { // these MIME type mappings are taken from Galaxy's "config/datatypes_conf.xml" file (with a few additions)
        MIMETYPES.put("ab1","application/octet-stream");
        MIMETYPES.put("analyze75","application/xml");
        MIMETYPES.put("anvio_state","application/json");
        MIMETYPES.put("arff","text/plain");
        MIMETYPES.put("asn1","text/plain");
        MIMETYPES.put("asn1-binary","application/octet-stream");
        MIMETYPES.put("bam","application/octet-stream");
        MIMETYPES.put("bcf","application/octet-stream");
        MIMETYPES.put("bcf_uncompressed","application/octet-stream");
        MIMETYPES.put("bigbed","application/octet-stream");
        MIMETYPES.put("bigwig","application/octet-stream");
        MIMETYPES.put("binary","application/octet-stream");
        MIMETYPES.put("biom1","application/json");
        MIMETYPES.put("biom2","application/octet-stream");
        MIMETYPES.put("blastdbd","text/html");
        MIMETYPES.put("blastdbn","text/html");
        MIMETYPES.put("blastdbp","text/html");
        MIMETYPES.put("blastxml","application/xml");
        MIMETYPES.put("bmp","image/bmp");
        MIMETYPES.put("bowtie_base_index","text/html");
        MIMETYPES.put("bowtie_color_index","text/html");
        MIMETYPES.put("bz2","application/x-bzip2");    // ADDED!
        MIMETYPES.put("cisml","application/xml");
        MIMETYPES.put("consensusxml","application/xml");
        MIMETYPES.put("cool","application/octet-stream");
        MIMETYPES.put("cram","application/octet-stream");
        MIMETYPES.put("cxb","application/octet-stream");
        MIMETYPES.put("d3_hierarchy","application/json");
        MIMETYPES.put("dada2_sequencetable","application/text");
        MIMETYPES.put("dada2_uniques","application/text");
        MIMETYPES.put("data","application/octet-stream");
        MIMETYPES.put("data_manager_json","application/json");
        MIMETYPES.put("eps","image/eps");
        MIMETYPES.put("featurexml","application/xml");
        MIMETYPES.put("fphe","text/html");
        MIMETYPES.put("fps","text/html");
        MIMETYPES.put("gafa.sqlite","application/octet-stream");
        MIMETYPES.put("gemini.sqlite","application/octet-stream");
        MIMETYPES.put("geojson","application/json");
        MIMETYPES.put("gif","image/gif");
        MIMETYPES.put("gmaj.zip","application/zip");
        MIMETYPES.put("gz","application/gzip");    // ADDED!        
        MIMETYPES.put("h5","application/octet-stream");
        MIMETYPES.put("h5ad","application/octet-stream");
        MIMETYPES.put("hivtrace","application/json");
        MIMETYPES.put("hlf","application/octet-stream");
        MIMETYPES.put("html","text/html");
        MIMETYPES.put("hyphy_results.json","application/json");
        MIMETYPES.put("idpdb","application/octet-stream");
        MIMETYPES.put("idxml","application/xml");
        MIMETYPES.put("im","image/im");
        MIMETYPES.put("imgt.json","application/json");
        MIMETYPES.put("imzml","application/xml");
        MIMETYPES.put("interprophet_pepxml","application/xml");
        MIMETYPES.put("isa-json","application/isa-tools");
        MIMETYPES.put("isa-tab","application/isa-tools");
        MIMETYPES.put("jpg","image/jpeg");
        MIMETYPES.put("jpeg","image/jpeg");        
        MIMETYPES.put("loom","application/octet-stream");
        MIMETYPES.put("maskinfo-asn1","text/plain");
        MIMETYPES.put("maskinfo-asn1-binary","application/octet-stream");
        MIMETYPES.put("mcool","application/octet-stream");
        MIMETYPES.put("memexml","application/xml");
        MIMETYPES.put("mrxs","image/mirax");
        MIMETYPES.put("mz.sqlite","application/octet-stream");
        MIMETYPES.put("mz5","application/octet-stream");
        MIMETYPES.put("mzdata","application/xml");
        MIMETYPES.put("mzid","application/xml");
        MIMETYPES.put("mzml","application/xml");
        MIMETYPES.put("mzq","application/xml");
        MIMETYPES.put("mzxml","application/xml");
        MIMETYPES.put("neostore","text/html");
        MIMETYPES.put("netcdf","application/octet-stream");
        MIMETYPES.put("nmrml","application/xml");
        MIMETYPES.put("nrrd","image/nrrd");
        MIMETYPES.put("obfs","text/html");
        MIMETYPES.put("obo","text/html");
        MIMETYPES.put("osw","application/octet-stream");
        MIMETYPES.put("owl","text/html");
        MIMETYPES.put("oxlicg","application/octet-stream");
        MIMETYPES.put("oxligl","application/octet-stream");
        MIMETYPES.put("oxling","application/octet-stream");
        MIMETYPES.put("oxliss","application/octet-stream");
        MIMETYPES.put("oxlist","application/octet-stream");
        MIMETYPES.put("oxlits","application/octet-stream");
        MIMETYPES.put("pbm","image/pbm");
        MIMETYPES.put("pcd","image/pcd");
        MIMETYPES.put("pcx","image/pcx");
        MIMETYPES.put("pdf","application/pdf");
        MIMETYPES.put("peptideprophet_pepxml","application/xml");
        MIMETYPES.put("pepxml","application/xml");
        MIMETYPES.put("pgm","image/pgm");
        MIMETYPES.put("png","image/png");
        MIMETYPES.put("pphe","text/html");
        MIMETYPES.put("ppm","image/ppm");
        MIMETYPES.put("pqp","application/octet-stream");
        MIMETYPES.put("probam","application/octet-stream");
        MIMETYPES.put("protxml","application/xml");
        MIMETYPES.put("psd","image/psd");
        MIMETYPES.put("pssm-asn1","text/plain");
        MIMETYPES.put("qcml","application/xml");
        MIMETYPES.put("qname_input_sorted.bam","application/octet-stream");
        MIMETYPES.put("qname_sorted.bam","application/octet-stream");
        MIMETYPES.put("rast","image/rast");
        MIMETYPES.put("raw_pepxml","application/xml");
        MIMETYPES.put("rgb","image/rgb");
        MIMETYPES.put("rna_eps","image/eps");
        MIMETYPES.put("sbml","application/xml");
        MIMETYPES.put("scf","application/octet-stream");
        MIMETYPES.put("sff","application/octet-stream");
        MIMETYPES.put("shp","application/octet-stream");
        MIMETYPES.put("sqlite","application/octet-stream");
        MIMETYPES.put("sra","application/octet-stream");
        MIMETYPES.put("svg","image/svg+xml");
        MIMETYPES.put("svslide","image/sakura");
        MIMETYPES.put("tandem","application/xml");
        MIMETYPES.put("tar","application/x-tar");   // ADDED     
        MIMETYPES.put("thermo.raw","application/octet-stream");
        MIMETYPES.put("tiff","image/tiff");
        MIMETYPES.put("toolshed.gz","multipart/x-gzip");
        MIMETYPES.put("trafoxml","application/xml");
        MIMETYPES.put("traml","application/xml");
        MIMETYPES.put("twobit","application/octet-stream");
        MIMETYPES.put("uniprotxml","application/xml");
        MIMETYPES.put("unsorted.bam","application/octet-stream");
        MIMETYPES.put("vms","image/hamamatsu");
        MIMETYPES.put("xbm","image/xbm");
        MIMETYPES.put("xml","application/xml");
        MIMETYPES.put("xpm","image/xpm");    
        MIMETYPES.put("zip","application/zip");   // ADDED             
    }
    
}
