/*
 */
package no.nels.galaxyhistorybrowser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kjetikl
 */
public class GalaxyHistoryArchiveTest {
    
    public GalaxyHistoryArchiveTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

//    /**
//     * Test of initialize method, of class GalaxyHistoryArchive.
//     */
//    @Test
//    public void testInitialize() throws Exception {
//        System.out.println("initialize");
//        Map attributes = null;
//        GalaxyHistoryArchive instance = null;
//        instance.initialize(attributes);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getExportVersion method, of class GalaxyHistoryArchive.
//     */
//    @Test
//    public void testGetExportVersion() throws Exception {
//        System.out.println("getExportVersion");
//        GalaxyHistoryArchive instance = null;
//        String expResult = "";
//        String result = instance.getExportVersion();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getHistoryAttribute method, of class GalaxyHistoryArchive.
//     */
//    @Test
//    public void testGetHistoryAttribute() throws Exception {
//        System.out.println("getHistoryAttribute");
//        String attribute = "";
//        GalaxyHistoryArchive instance = null;
//        String expResult = "";
//        String result = instance.getHistoryAttribute(attribute);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDatasets method, of class GalaxyHistoryArchive.
//     */
//    @Test
//    public void testGetDatasets() throws Exception {
//        System.out.println("getDatasets");
//        GalaxyHistoryArchive instance = null;
//        List<Map> expResult = null;
//        List<Map> result = instance.getDatasets();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDataset method, of class GalaxyHistoryArchive.
//     */
//    @Test
//    public void testGetDataset() throws Exception {
//        System.out.println("getDataset");
//        String attribute = "";
//        Object value = null;
//        GalaxyHistoryArchive instance = null;
//        Map expResult = null;
//        Map result = instance.getDataset(attribute, value);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getCollections method, of class GalaxyHistoryArchive.
//     */
//    @Test
//    public void testGetCollections() throws Exception {
//        System.out.println("getCollections");
//        GalaxyHistoryArchive instance = null;
//        List<Map> expResult = null;
//        List<Map> result = instance.getCollections();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getJobs method, of class GalaxyHistoryArchive.
//     */
//    @Test
//    public void testGetJobs() throws Exception {
//        System.out.println("getJobs");
//        GalaxyHistoryArchive instance = null;
//        List<Map> expResult = null;
//        List<Map> result = instance.getJobs();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getJob method, of class GalaxyHistoryArchive.
//     */
//    @Test
//    public void testGetJob() throws Exception {
//        System.out.println("getJob");
//        String jobID = "";
//        GalaxyHistoryArchive instance = null;
//        Map<String, Object> expResult = null;
//        Map<String, Object> result = instance.getJob(jobID);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getHistory method, of class GalaxyHistoryArchive.
//     */
//    @Test
//    public void testGetHistory() throws Exception {
//        System.out.println("getHistory");
//        GalaxyHistoryArchive instance = null;
//        Map<String, Object> expResult = null;
//        Map<String, Object> result = instance.getHistory();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getHistoryAsJSON method, of class GalaxyHistoryArchive.
//     */
//    @Test
//    public void testGetHistoryAsJSON() throws Exception {
//        System.out.println("getHistoryAsJSON");
//        boolean pretty = false;
//        GalaxyHistoryArchive instance = null;
//        String expResult = "";
//        String result = instance.getHistoryAsJSON(pretty);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of outputHistoryAsJSON method, of class GalaxyHistoryArchive.
//     */
//    @Test
//    public void testOutputHistoryAsJSON() throws Exception {
//        System.out.println("outputHistoryAsJSON");
//        OutputStream outstream = null;
//        boolean pretty = false;
//        GalaxyHistoryArchive instance = null;
//        instance.outputHistoryAsJSON(outstream, pretty);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of getMIMEtypeFromExtension method, of class GalaxyHistoryArchive.
     */
    @Test
    public void testGetMIMEtypeFromExtension() {
        System.out.println("getMIMEtypeFromExtension");
        GalaxyHistoryArchive instance = new GalaxyHistoryArchive("");
        assertEquals("text/plain", instance.getMIMEtypeFromExtension("fastq", false)); 
        assertEquals("text/plain", instance.getMIMEtypeFromExtension("fastq", true)); // decompress option should have no effect on single suffixes
        assertEquals("image/jpeg", instance.getMIMEtypeFromExtension("jpeg", true));
        assertEquals("image/jpeg", instance.getMIMEtypeFromExtension("jpg", true));
        assertEquals("image/png", instance.getMIMEtypeFromExtension("png", true));
        assertEquals("application/pdf", instance.getMIMEtypeFromExtension(".pdf", false));
        assertEquals("application/pdf", instance.getMIMEtypeFromExtension(".txt.pdf", false));
        assertEquals("text/plain", instance.getMIMEtypeFromExtension(".txt.pdf", true)); // this case not normal, but it is chosen design
        assertEquals("application/octet-stream", instance.getMIMEtypeFromExtension("bam", false));    
        assertEquals("application/gzip", instance.getMIMEtypeFromExtension("gz", false)); //
        assertEquals("application/gzip", instance.getMIMEtypeFromExtension("fastq.gz", false)); //
        assertEquals("text/plain", instance.getMIMEtypeFromExtension("fastq.gz", true)); // decompressed format
        assertEquals("application/x-bzip2", instance.getMIMEtypeFromExtension("bz2", false)); //
        assertEquals("application/x-bzip2", instance.getMIMEtypeFromExtension("fastq.bz2", false)); //
        assertEquals("text/plain", instance.getMIMEtypeFromExtension("fastq.bz2", true)); // decompressed format        
        assertEquals("application/gzip", instance.getMIMEtypeFromExtension("tar.gz", false)); // 
        assertEquals("application/x-tar", instance.getMIMEtypeFromExtension("tar.gz", true)); // 
        assertEquals("application/gzip", instance.getMIMEtypeFromExtension("tgz", false)); //
        assertEquals("application/x-tar", instance.getMIMEtypeFromExtension("tgz", true)); //            
        assertEquals("application/zip", instance.getMIMEtypeFromExtension("zip", false));
        assertEquals("application/zip", instance.getMIMEtypeFromExtension("zip", true)); // ZIP format is not "decompressable" here
        assertEquals("text/plain", instance.getMIMEtypeFromExtension(".txt.zip", true)); // contrived example but chosen design                
    }

//    /**
//     * Test of getInputStreamReaderForFile method, of class GalaxyHistoryArchive.
//     */
//    @Test
//    public void testGetInputStreamReaderForFile() throws Exception {
//        System.out.println("getInputStreamReaderForFile");
//        String filepath = "";
//        GalaxyHistoryArchive instance = null;
//        InputStreamReader expResult = null;
//        InputStreamReader result = instance.getInputStreamReaderForFile(filepath);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getInputStreamForFile method, of class GalaxyHistoryArchive.
//     */
//    @Test
//    public void testGetInputStreamForFile() throws Exception {
//        System.out.println("getInputStreamForFile");
//        String filepath = "";
//        GalaxyHistoryArchive instance = null;
//        InputStream expResult = null;
//        InputStream result = instance.getInputStreamForFile(filepath);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of outputDataset method, of class GalaxyHistoryArchive.
//     */
//    @Test
//    public void testOutputDataset_String() throws Exception {
//        System.out.println("outputDataset");
//        String datasetID = "";
//        GalaxyHistoryArchive instance = null;
//        instance.outputDataset(datasetID);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of outputDataset method, of class GalaxyHistoryArchive.
//     */
//    @Test
//    public void testOutputDataset_3args() throws Exception {
//        System.out.println("outputDataset");
//        String datasetID = "";
//        int start = 0;
//        int end = 0;
//        GalaxyHistoryArchive instance = null;
//        instance.outputDataset(datasetID, start, end);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of outputDatasetExtraFile method, of class GalaxyHistoryArchive.
//     */
//    @Test
//    public void testOutputDatasetExtraFile_String_String() throws Exception {
//        System.out.println("outputDatasetExtraFile");
//        String datasetID = "";
//        String filename = "";
//        GalaxyHistoryArchive instance = null;
//        instance.outputDatasetExtraFile(datasetID, filename);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of outputDatasetExtraFile method, of class GalaxyHistoryArchive.
//     */
//    @Test
//    public void testOutputDatasetExtraFile_4args() throws Exception {
//        System.out.println("outputDatasetExtraFile");
//        String datasetID = "";
//        String filename = "";
//        int start = 0;
//        int end = 0;
//        GalaxyHistoryArchive instance = null;
//        instance.outputDatasetExtraFile(datasetID, filename, start, end);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of readJSON method, of class GalaxyHistoryArchive.
//     */
//    @Test
//    public void testReadJSON() throws Exception {
//        System.out.println("readJSON");
//        String filename = "";
//        String[] attributes = null;
//        GalaxyHistoryArchive instance = null;
//        Object expResult = null;
//        Object result = instance.readJSON(filename, attributes);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
    
}
