/*
 *
 */
package no.nels.galaxyhistorybrowser;

/**
 * 
 * @author kjetikl
 */
public class GalaxyHistoryBrowser {
    
    /**
     * Reads a Galaxy History Archive file and outputs the history to STDOUT
     * If no filename is provided, a GUI interface will be started up instead
     * @param args The first and only argument should be the path to a file (or URL)
     */
    public static void main(String[] args) {    
        if (args==null || args.length<1) GUI.start();
        else {
            try {        
                GalaxyHistoryArchive history=new GalaxyHistoryArchive(args[0]);
                history.initialize(null);        
                System.err.println("Galaxy History Archive Format Version: "+history.getExportVersion()+"\n");
                outputStructure(history.getHistory(), null);
            } catch (Exception e) {
                System.err.println(e);
                // e.printStackTrace();
            }
        }
    }
      
    private static void outputStructure(Object o, String indent) {
        if (indent==null) indent="";
        if (o instanceof java.util.List) {
            for (Object x:((java.util.List)o)) {
                if (x instanceof java.util.List || x instanceof java.util.Map) {System.out.print(indent);System.out.println("-> "); outputStructure(x, indent+"    ");}
                else {System.out.print(indent);System.out.println("- "+x);}
            }
        } else if (o instanceof java.util.Map) {
            for (Object x:((java.util.Map)o).keySet()) {
                Object value=((java.util.Map)o).get(x);
                if (value instanceof java.util.List || value instanceof java.util.Map) {System.out.print(indent);System.out.println(x+"=>"); outputStructure(value, indent+"    ");}
                else {System.out.print(indent);System.out.println(x+":"+value);}
            }
        }
    }
    
}
