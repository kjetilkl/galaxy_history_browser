/*
 */
package no.nels.galaxyhistorybrowser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
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
public class SimpleJSONparserTest {
    
    public SimpleJSONparserTest() {
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

    /**
     * Test of parseJSON method, of class SimpleJSONparser.
     * Tests that a simple JSON list input with elements of different types returns a corresponding List<Object> with those elements
     */
    @Test
    public void testParseJSON_simpleListInput_returnList() throws Exception {
        System.out.println("parseJSON: test that simple JSON list input returns same list");
        String input = "[1,'two',3, 942.3,7.234, null, true,false, \"ok\"]";
        InputStream stream = new ByteArrayInputStream(input.getBytes());
        InputStreamReader streamreader = new InputStreamReader(stream);
        String[] attributes = null;
        SimpleJSONparser instance = new SimpleJSONparser();
        ArrayList expResult = new ArrayList<Object>();
        expResult.add(1);
        expResult.add("two");
        expResult.add(3); 
        expResult.add(942.3); 
        expResult.add(7.234); 
        expResult.add(null);        
        expResult.add(true); 
        expResult.add(false); 
        expResult.add("ok"); 
        Object result = instance.parseJSON(streamreader, attributes);
        assertTrue("Returned value is not a List but rather: "+((result==null)?"NULL":result.getClass()), result instanceof List);
        assertTrue("Returned List value is not correct. Expected \""+expResult+"\" ("+(expResult.getClass())+") but got \""+result+"\" ("+((result==null)?"NULL":result.getClass())+")", Objects.deepEquals(expResult, result));
    }
    
    /**
     * Test of parseJSON method, of class SimpleJSONparser.
     * Tests that a simple JSON object input with fields of different types returns a corresponding HashMap<String,Object> with those fields
     */
    @Test
    public void testParseJSON_simpleObjectInput_returnObject() throws Exception {
        System.out.println("parseJSON: test that simple JSON object input returns same object");
        //String input = "['one','two','three']";
        String input = "{'name':\"John D\", 'age':42, 'salary': 1253.03, 'access': true, 'password':null }";
        InputStream stream = new ByteArrayInputStream(input.getBytes());
        InputStreamReader streamreader = new InputStreamReader(stream);
        String[] attributes = null;
        SimpleJSONparser instance = new SimpleJSONparser();
        HashMap expResult = new HashMap<String,Object>();
        expResult.put("name","John D");
        expResult.put("age",42);
        expResult.put("salary",1253.03);
        expResult.put("access",true);
        expResult.put("password",null);      
        Object result = instance.parseJSON(streamreader, attributes);
        assertTrue("Returned value is not a HashMap but rather: "+((result==null)?"NULL":result.getClass()), result instanceof HashMap);
        assertTrue("Returned HashMap value is not correct. Expected \""+expResult+"\" ("+(expResult.getClass())+") but got \""+result+"\" ("+((result==null)?"NULL":result.getClass())+")", Objects.deepEquals(expResult, result));
    }    
    
    /**
     * Test of parseJSON method, of class SimpleJSONparser.
     * Tests that a complex JSON structure with nested lists and objects returns a corresponding HashMap<String,Object>
     */
    @Test
    public void testParseJSON_complexObjectInput_returnObject() throws Exception {
        System.out.println("parseJSON: test that complex JSON object input returns same object");
        //String input = "['one','two','three']";
        String input = "{"
                + "'name':\"John D\", 'age':42, 'salary': 1253.03, 'access': true, "
                + "'supervisor':{'name':'Karen B','department':'HR'}, "
                + "'students':[32,979,12,'graduates'], "                
                + "'projects':["
                +     "{'id':1,'name':'analysis 1','active':true},"
                +     "{'id':2,'name':'analysis 21','active':false},"
                +     "{'id':3,'name':'analysis 43','active':true}"
                +    "]"
                + "}";
        InputStream stream = new ByteArrayInputStream(input.getBytes());
        InputStreamReader streamreader = new InputStreamReader(stream);
        String[] attributes = null;
        SimpleJSONparser instance = new SimpleJSONparser();
        HashMap<String, Object> supervisor = new HashMap<>(); supervisor.put("name", "Karen B");supervisor.put("department","HR");
        ArrayList<Object> students=new ArrayList<Object>();   students.add(32);students.add(979);students.add(12);students.add("graduates");
        HashMap<String, Object> project1 = new HashMap<>();   project1.put("id",1);project1.put("name","analysis 1"); project1.put("active",true);
        HashMap<String, Object> project2 = new HashMap<>();   project2.put("id",2);project2.put("name","analysis 21"); project2.put("active",false);
        HashMap<String, Object> project3 = new HashMap<>();   project3.put("id",3);project3.put("name","analysis 43"); project3.put("active",true);       
        ArrayList<Object> projects=new ArrayList<Object>();   projects.add(project1);projects.add(project2);projects.add(project3);
        HashMap expResult = new HashMap<String,Object>();
        expResult.put("name","John D");
        expResult.put("age",42);
        expResult.put("salary",1253.03);
        expResult.put("access",true);
        expResult.put("supervisor",supervisor);        
        expResult.put("students",students);
        expResult.put("projects",projects);        
        Object result = instance.parseJSON(streamreader, attributes);
        assertTrue("Returned value is not a HashMap but rather: "+((result==null)?"NULL":result.getClass()), result instanceof HashMap);
        assertTrue("Returned HashMap value is not correct. Expected \""+expResult+"\" ("+(expResult.getClass())+") but got \""+result+"\" ("+((result==null)?"NULL":result.getClass())+")", Objects.deepEquals(expResult, result));
    }      
    
    /**
     * Test of parseJSON method, of class SimpleJSONparser.
     * Tests that a complex JSON structure with nested lists and objects that is filtered on specific attributes returns a corresponding HashMap<String,Object> with only those attributes
     */
    @Test
    public void testParseJSON_complexObjectInputWithFiltering_returnFilteredObject() throws Exception {
        System.out.println("parseJSON: test that complex JSON object input that is filtered on specific attributes returns an object with only those attributes");
        //String input = "['one','two','three']";
        String input = "{"
                + "'name':\"John D\", 'age':42, 'salary': 1253.03, 'access': true, "
                + "'supervisor':{'name':'Karen B','department':'HR'}, "
                + "'students':[32,979,12,'graduates'], "                
                + "'projects':["
                +     "{'id':1,'name':'analysis 1','active':true},"
                +     "{'id':2,'name':'analysis 21','active':false},"
                +     "{'id':3,'name':'analysis 43','active':true}"
                +    "]"
                + "}";
        InputStream stream = new ByteArrayInputStream(input.getBytes());
        InputStreamReader streamreader = new InputStreamReader(stream);
        String[] attributes = new String[]{"name","age","projects","active"};
        SimpleJSONparser instance = new SimpleJSONparser();
        HashMap<String, Object> project1 = new HashMap<>();   project1.put("name","analysis 1"); project1.put("active",true);
        HashMap<String, Object> project2 = new HashMap<>();   project2.put("name","analysis 21"); project2.put("active",false);
        HashMap<String, Object> project3 = new HashMap<>();   project3.put("name","analysis 43"); project3.put("active",true);      
        ArrayList<Object> projects=new ArrayList<Object>();   projects.add(project1);projects.add(project2);projects.add(project3);
        HashMap expResult = new HashMap<String,Object>();
        expResult.put("name","John D");
        expResult.put("age",42);
        expResult.put("projects",projects);        
        Object result = instance.parseJSON(streamreader, attributes);
        assertTrue("Returned value is not a HashMap but rather: "+((result==null)?"NULL":result.getClass()), result instanceof HashMap);
        assertTrue("Returned HashMap value is not correct. Expected \""+expResult+"\" ("+(expResult.getClass())+") but got \""+result+"\" ("+((result==null)?"NULL":result.getClass())+")", Objects.deepEquals(expResult, result));
    }      
    
}
