/*
 */
package no.nels.galaxyhistorybrowser;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses JSON from an InputStream and returns a representation of the structure
 * The returned object can either be a Map ("JSON object") or a List (with nested child entries being a combination of Maps, Lists and Basic types (String,Integer,Double,Boolean or NULL)).
 * @author kjetikl
 */
public class SimpleJSONparser {
    
    /**
     * 
     * @param streamreader
     * @param attributes If this list is provided (not null), JSON object attribute fields that are not included here will be skipped (note that this also applies to nested attributes)
     * @return Either a List or Map ("JSON object")
     * @throws IOException
     * @throws JsonParseException 
     */
    public Object parseJSON(InputStreamReader streamreader, String[] attributes) throws IOException, JsonParseException{
        JsonFactory factory = new JsonFactory();
        JsonParser  parser  = factory.createParser(streamreader); 
        Object result=null;
        Set<String> limitToFields=null;
        if (attributes!=null && attributes.length>0) {
            limitToFields=new HashSet<>(attributes.length);
            limitToFields.addAll(Arrays.asList(attributes));
        }
        while(!parser.isClosed()){
            JsonToken jsonToken = parser.nextToken();
            if (jsonToken==null)  {parser.close();continue;}
            if (jsonToken.equals(JsonToken.START_OBJECT)) {
                result=parseJSONobject(parser, limitToFields);
            } else if (jsonToken.equals(JsonToken.START_ARRAY)) {
                result=parseJSONlist(parser, limitToFields);
            } else throw new IOException("Unexpected JSON token: "+jsonToken.toString()); 
        } 
        return result;
    }
    
    private Map<String,Object> parseJSONobject(JsonParser parser, Set<String> limitToFields) throws IOException, JsonParseException{
        Map<String,Object> map=new HashMap<>();
        String field=null;
        while(!parser.isClosed()){
            JsonToken jsonToken = parser.nextToken();
            if (jsonToken.equals(JsonToken.END_OBJECT)) return map;            
            if (!jsonToken.equals(JsonToken.FIELD_NAME)) throw new JsonParseException(parser, "Missing field name in map");
            field=parser.getCurrentName();
            jsonToken = parser.nextToken();            
            if (jsonToken.equals(JsonToken.START_OBJECT)) {
                if (limitToFields==null || limitToFields.contains(field)) {
                    Map<String,Object> value=parseJSONobject(parser,limitToFields);
                    map.put(field,value);
                } else parser.skipChildren();
            } else if (jsonToken.equals(JsonToken.START_ARRAY)) {
                if (limitToFields==null || limitToFields.contains(field)) {                
                    List<Object> sublist=parseJSONlist(parser, limitToFields);
                    map.put(field,sublist);
                 } else parser.skipChildren();                   
            } else if (jsonToken.equals(JsonToken.VALUE_STRING)) {
                if (limitToFields==null || limitToFields.contains(field)) map.put(field,parser.getValueAsString());
            } else if (jsonToken.equals(JsonToken.VALUE_NUMBER_INT)) {
                if (limitToFields==null || limitToFields.contains(field)) map.put(field,parser.getValueAsInt());
            } else if (jsonToken.equals(JsonToken.VALUE_NUMBER_FLOAT)) {
                if (limitToFields==null || limitToFields.contains(field)) map.put(field,parser.getValueAsDouble());
            } else if (jsonToken.equals(JsonToken.VALUE_TRUE) || jsonToken.equals(JsonToken.VALUE_FALSE)) {
                if (limitToFields==null || limitToFields.contains(field)) map.put(field,parser.getValueAsBoolean());
            } else if (jsonToken.equals(JsonToken.VALUE_NULL)) {
                if (limitToFields==null || limitToFields.contains(field)) map.put(field,null);
            } else throw new IOException("Unexpected JSON token: "+jsonToken.toString()); 
        } 
        throw new JsonParseException(parser, "Unexpected end of map");      
    }
    
    private List<Object> parseJSONlist(JsonParser parser, Set<String> limitToFields) throws IOException, JsonParseException{
        List<Object> list=new ArrayList<>();
        while(!parser.isClosed()){
            JsonToken jsonToken = parser.nextToken();
            if (jsonToken.equals(JsonToken.START_OBJECT)) {
                Map<String,Object> object=parseJSONobject(parser, limitToFields);
                list.add(object);
            } else if (jsonToken.equals(JsonToken.START_ARRAY)) {
                List<Object> sublist=parseJSONlist(parser, limitToFields);
                list.add(sublist);
            } else if (jsonToken.equals(JsonToken.END_ARRAY)) {
                return list;
            } else if (jsonToken.equals(JsonToken.VALUE_STRING)) {
                list.add(parser.getValueAsString());
            } else if (jsonToken.equals(JsonToken.VALUE_NUMBER_INT)) {
                list.add(parser.getValueAsInt());
            } else if (jsonToken.equals(JsonToken.VALUE_NUMBER_FLOAT)) {
                list.add(parser.getValueAsDouble());
            } else if (jsonToken.equals(JsonToken.VALUE_TRUE) || jsonToken.equals(JsonToken.VALUE_FALSE))  {
                list.add(parser.getValueAsBoolean());
            } else if (jsonToken.equals(JsonToken.VALUE_NULL))  {
                list.add(null);
            } else throw new JsonParseException(parser, "Unexpected JSON token: "+jsonToken.toString()); 
        } 
        throw new JsonParseException(parser, "Unexpected end of list");
    }        
    
    
}
