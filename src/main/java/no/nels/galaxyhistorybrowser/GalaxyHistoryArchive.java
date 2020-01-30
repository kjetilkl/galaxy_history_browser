/*
 * Objects of this class represents Galaxy History Archives from a specific source (File or URL)
 */
package no.nels.galaxyhistorybrowser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

/**
 *
 * @author kjetikl
 */
public class GalaxyHistoryArchive {
    
    private String archivepath=null;
    private String version=null;
    private Map<String,String> history_metadata=null;
    private List<Map> datasets=null; // raw datasets
    private List<Map> collections=null; // raw collections
    private List<Map> history=null; // processed history
    
    
    /**
     * Creates a new GalaxyHistoryArchive associated with a specific source (either local file or URL)
     * @param archivepath The path to a local archive file or a URL (starting with http:)
     */
    public GalaxyHistoryArchive(String archivepath) {
        this.archivepath=archivepath;
    }
    
    /**
     * Reads relevant data from the Galaxy History file and caches the information in this object for fast access later
     * @param attributes If provided, this can limit the metadata attributes that are read from the history archive
     *                   Each key should refer to a metadata set (either "history", "datasets" or "collections") and the value should be a String[]
     *                   listing the attributes to retrieve for that metadata set              
     */
    public void initialize(Map<String,String[]> attributes) throws Exception {
        version=getExportVersionFromArchive(); // 
        if (version.equals("1")) throw new Exception("This History archive was created with an older version of Galaxy that does not support collections properly");
        if (!version.equals("2")) throw new Exception("Unrecognized history export version: "+version);
        history_metadata=getHistoryAttributesFromArchive((attributes!=null)?attributes.get("history"):null);
        datasets=getDatasetsFromArchive((attributes!=null)?attributes.get("datasets"):null);
        collections=getCollectionsFromArchive((attributes!=null)?attributes.get("collections"):null);
        history=processHistory(); // Create a new easy-to-use structure to represent the entire history    
    }
    
    /** Returns the version number for the History Archive format that the history was exported in */    
    public String getExportVersion() throws Exception {
        if (version==null) version=getExportVersionFromArchive(); // 
        return version;
    }
    
    /** Returns a specific metadata attribute for the history */
    public String getHistoryAttribute(String attribute) throws Exception {
        if (history_metadata==null) history_metadata=getHistoryAttributesFromArchive(null);
        return history_metadata.get(attribute);
    }
    
    /** Returns all the datasets in the archive (not collections) as a list of Maps */
    public List<Map> getDatasets() throws Exception {    
        if (datasets==null) datasets=getDatasetsFromArchive(null);
        return datasets;
    }    
    
    /** Returns the (first) dataset that has the given value for the attribute */
    public Map getDataset(String attribute, Object value) throws Exception {    
        if (datasets==null) datasets=getDatasetsFromArchive(null);
        return getDataset(datasets, attribute, value);
    }        
    
    public List<Map> getCollections() throws Exception {    
        if (collections==null) collections=getCollectionsFromArchive(null);
        return collections;
    } 
    
    /** Returns an easy-to-use representation of the history that includes both regular datasets and collections in anti-chronological order
     *  Note that the datasets do not contain a full set of metadata, but only some preselected attributes
     */
    public List<Map> getHistory() throws Exception {    
        if (history==null) history=processHistory();
        return history;
    }       

    // ---------------------------------------------------------------------------------------    
         
    /** 
     *  Returns an InputStreamReader that reads from a specific file within the archive 
     *  @param filepath The path to a file inside the tarball. Since the method returns a Reader, this should be a text file (not binary)
     */
    public InputStreamReader getInputStreamReaderForFile(String filepath) throws Exception {
        InputStream source=(archivepath.startsWith("http:") || archivepath.startsWith("https:"))?((new URL(archivepath)).openStream()):new FileInputStream(archivepath);
        TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(source));
        TarArchiveEntry currentEntry = tarInput.getNextTarEntry();
        while (currentEntry != null && !currentEntry.getName().equals(filepath)) {
            currentEntry = tarInput.getNextTarEntry(); // You forgot to iterate to the next file
        }
        if (currentEntry!=null && currentEntry.getName().equals(filepath)) {
            if (filepath.endsWith(".gz")) return new InputStreamReader(new GzipCompressorInputStream(tarInput));
            else return new InputStreamReader(tarInput);             
        } else throw new FileNotFoundException("Unable to locate archive file '"+filepath+"'");
    }      
    
    /** 
     *  Returns an InputStream that streams from a specific file within the archive 
     *  @param filepath The path to a file inside the tarball. This could be a text file or a binary file
     */
    public InputStream getInputStreamForFile(String filepath) throws Exception {
        InputStream source=(archivepath.startsWith("http:") || archivepath.startsWith("https:"))?((new URL(archivepath)).openStream()):new FileInputStream(archivepath);
        TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(source));
        TarArchiveEntry currentEntry = tarInput.getNextTarEntry();
        while (currentEntry != null && !currentEntry.getName().equals(filepath)) {
            currentEntry = tarInput.getNextTarEntry(); // You forgot to iterate to the next file
        }
        if (currentEntry!=null && currentEntry.getName().equals(filepath)) {
            if (filepath.endsWith(".gz")) return new GzipCompressorInputStream(tarInput);
            return tarInput;             
        } else throw new FileNotFoundException("Unable to locate archive file '"+filepath+"'");
    }      
    
    /**
     * Reads the contents of a specified JSON file within the archive and returns a representation of the contents consisting of List, Map and Basic Types (String,Integer,Double,Boolean and null)
     * @param filename The path to the JSON file within the archive
     * @param attributes If this list is provided (not null) attribute fields that are not included here will be skipped (note that this also applies to nested attributes)
     * @return Either a List or Map depending on top-most element in the JSON file
     */
    public Object readJSON(String filename, String[] attributes) throws Exception {     
        InputStreamReader reader=getInputStreamReaderForFile(filename);
        SimpleJSONparser parser=new SimpleJSONparser();
        return parser.parseJSON(reader, attributes);            
    }       
    
    // ---------------------------------------------------------------------------------------        
    
    /** 
     * Returns the format version number the archive was exported in 
     */
    private String getExportVersionFromArchive() throws Exception {
        boolean version_file_found=true;
        InputStreamReader reader=null;
        try {
             reader=getInputStreamReaderForFile("export_attrs.txt");
        } catch (FileNotFoundException ffn) {
            version_file_found=false;                       
        }
        if (!version_file_found) {
            try {
                reader=getInputStreamReaderForFile("history_attrs.txt");
            } catch (FileNotFoundException ffn) {
                throw new Exception("The file is probably not a Galaxy History Archive");                      
            }  
            if (reader!=null) return "1"; // the older export format does not have an "export_attrs.txt" file that specifies the version
        }
        SimpleJSONparser parser=new SimpleJSONparser();
        Object result=parser.parseJSON(reader, new String[]{"galaxy_export_version"});            
        if (result instanceof Map && ((Map)result).containsKey("galaxy_export_version")) {
            return ((Map)result).get("galaxy_export_version").toString();
        } else throw new Exception("Unable to determine export format version.");
    }
    

    /**
     * Returns the history's name, annotation, tags (as comma-separated list) creation_time and update_time (just the dates, not the time of day)
     * @return 
     */
    private Map<String,String> getHistoryAttributesFromArchive(String[] attributes) throws Exception {
        InputStreamReader reader=getInputStreamReaderForFile("history_attrs.txt");
        SimpleJSONparser parser=new SimpleJSONparser();
        Object result=parser.parseJSON(reader,attributes); // new String[]{"name","tags","annotation","create_time","update_time"});         
        if (result instanceof Map) {
            Object tags=((Map)result).get("tags"); // format tags as a single comma-separated string
            if (tags instanceof List) {
                String tagslist=splice((List)tags, ",");
                ((Map)result).put("tags", tagslist);                  
            } 
            Object an=((Map)result).get("annotation");
            if (an==null || an.toString().equals("null")) ((Map)result).put("annotation","");
        } else throw new Exception("Unable to read history attributes. Return value was not a map");   
        return (Map<String,String>)result;
    }    
    
    /**
     * Returns information about the regular datasets in the history. 
     * Datasets within collections are included, but not the collections themselves.
     * The dataset is uniquely identified by its "encoded_id" attribute.
     * The "hid" attribute of a dataset is its chronological number in the history.
     * @param attributes If this list is provided (not null), attribute fields that are not included here will be skipped (note that this also applies to nested attributes)
     * @return A List of Maps where each map represents a dataset
     */
    private List<Map> getDatasetsFromArchive(String[] attributes) throws Exception {    
        InputStreamReader reader=getInputStreamReaderForFile("datasets_attrs.txt");
        SimpleJSONparser parser=new SimpleJSONparser();
        Object result=parser.parseJSON(reader,attributes);   
        if (result instanceof List) return (List<Map>)result;
        else throw new Exception("Unable to parse history datasets");
    }  
    
    /**
     * Returns information about the collections in the history. 
     * Datasets within collections are included, but not the collections themselves.
     * @param attributes If this list is provided (not null), attribute fields that are not included here will be skipped (note that this also applies to nested attributes)
     * @return A List of Maps where each map represents a dataset
     */
    private List<Map> getCollectionsFromArchive(String[] attributes) throws Exception {     
        InputStreamReader reader=getInputStreamReaderForFile("collections_attrs.txt");
        SimpleJSONparser parser=new SimpleJSONparser();
        Object result=parser.parseJSON(reader,attributes);                     
        if (result instanceof List) return (List<Map>)result;
        else throw new Exception("Unable to parse history collections: "+result);
    }  
    
    /**
     * Returns a representation of the full history, including both regular datasets and collections, in anti-chronological order (i.e. newest history element is first in list)
     * Each element has a "class" attribute, which can be either "dataset" or a collection type ("list", "paired" or "list:paired").
     * Collections have an attribute called "elements" (List) containing either datasets or other (nested) collections.
     * @return 
     */
    private List<Map> processHistory() throws Exception {
        ArrayList<Map> historylist=new ArrayList<>(); 
        getDatasets();  // force import of datasets.     
        getCollections(); // force import of collections.
        // I want to make slight modifications to the structures so I make copies of the current datasets/collections
        ArrayList<Map> history_datasets=(ArrayList<Map>)deepCopy(datasets, new String[]{"hid","name","extension","file_name","extra_files_path","metadata","dbkey","encoded_id","peek","blurb","visible","create_time"}); 
        ArrayList<Map> history_collections=(ArrayList<Map>)deepCopy(collections, new String[]{"hid","encoded_id","element_identifier","element_index","display_name","type","collection","child_collection","elements","hda"}); 
        
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
        return historylist;
    }
    
    /**
     * Does some processing / restructuring of a collection element (from the history).
     * Subcollections will be processed recursively
     * @param collection
     * @param datasets
     * @throws Exception 
     */
    private void processCollection(Map collection, List<Map> datasets) throws Exception {
        List<Map> elements=(List<Map>)collection.get("elements"); // these are the entries in the collection
        String type=(String)collection.get("type");
        int index=0;
        for (Map element:elements) {
            if (((int)element.get("element_index"))!=index) throw new Exception("List element out of order");
            element.put("name", element.get("element_identifier")); element.remove("element_identifier"); // rename "element_identifier" to "name" for simplicity
            if (type.equals("list") || type.equals("paired")) { // these list types are not nested and can be processed in the same way
                String datasetID=(String)((Map)element.get("hda")).get("encoded_id");
                Map dataset=getDataset(datasets, "encoded_id", datasetID);
                if (dataset==null) throw new Exception("Dataset ["+datasetID+"] not found in datasets list");
                dataset=(Map)deepCopy(dataset, null); // the same dataset can be referenced in many places, but we make individual copies 
                element.put("dataset", dataset); // add the dataset directly as an attribute of this element in the collection    
            } else if (type.equals("list:paired")) {
                 Map contents=(Map)element.get("child_collection");
                 if (contents==null) throw new Exception("Child collection not found!!");
                 element.put("class",contents.get("type")); // The "type" attribute is lifted one level up and called "class" in the parent
                 processCollection(contents, datasets); // process nested collection recursively
                 element.put("collection",contents); element.remove("child_collection"); // rename "child_collection" attribute to "collection" for simplicity
            } else throw new Exception("Unrecognized collection type: "+type); 
            index++;
        }                                    
    }
    
    
    /** 
     * Searches for an object with the given attribute value in a list and returns it
     * @param list The list to search in
     * @param attribute The "key"
     * @param value
     * @return The object (Map) that has the given value for the attribute, or NULL if no such object is found
     */   
    private Map getDataset(List<Map> list, Object attribute, Object value) {
        Map found=null;
        for (Map map:list) {
            Object objectvalue=map.get(attribute);
            // System.err.println("Searching for ["+attribute+"="+value+"]. Found ["+attribute+"="+objectvalue+"]");
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
     * Takes a complex JSON-type object (consisting of potentially nested Maps and Lists) and writes a description of this object into a buffer
     * @param o The object to be formatted
     * @param indent A string specifying intendation. Usually this should be null, unless the method is called recursively
     * @param builder The buffer in which to build the string
     */
    private void formatObjectInBuffer(Object o, String indent, StringBuilder builder) {
        if (indent==null) indent="";
        if (o instanceof java.util.List) {
            for (Object x:((java.util.List)o)) {
                if (x instanceof java.util.List || x instanceof java.util.Map) {builder.append(indent);builder.append("->\n"); formatObjectInBuffer(x, indent+"    ", builder);}
                else {builder.append(indent);builder.append("- "+x+"\n");}
            }
        } else if (o instanceof java.util.Map) {
            for (Object x:((java.util.Map)o).keySet()) {
                Object value=((java.util.Map)o).get(x);
                if (value instanceof java.util.List || value instanceof java.util.Map) {builder.append(indent);builder.append(x+"=>\n"); formatObjectInBuffer(value, indent+"    ", builder);}
                else {builder.append(indent);builder.append(x+":"+value+"\n");}
            }
        }
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
                ((ArrayList)copy).add(deepCopy(item,attributes));
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
        }  else System.err.println("Unexpected occurrence in DeepCopy()");
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

    
}
