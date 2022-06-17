import java.lang.Object;
import java.util.Collection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import com.thoughtworks.qdox.*;
import com.thoughtworks.qdox.model.*;
 
class extractor {
    public static void main(String[] args) {
    	// define qdox entrypoint
        JavaProjectBuilder parser = new JavaProjectBuilder();

		// load source files
		String dir = "../target_APK/";
		String appName = args[0] + "/";
		System.out.println("INFO  - Starting pattern-based method extraction ...");
		System.out.println("INFO  - Loading "+ appName + " app ...");
        File sourceFolder = new File(dir + appName);
        parser.addSourceTree(sourceFolder);
        Collection<JavaClass> classes = parser.getClasses();

		try{

	        // create output file for signatures
	        String fileOutName = "../target_APK/" + appName + "signatures_pattern.txt";
	        File fileOut = new File(fileOutName);
	        FileWriter fw = new FileWriter(fileOutName);

       		System.out.println("INFO  - Fetching methods ...");

		
			// iter over classes
	        for (JavaClass cls: classes) {
	        	String pkgName = cls.getPackage().getName();
	        	String clsName = cls.getName();
	        	// iter over class methods
	        	for (JavaMethod met: cls.getMethods()) {
	        		String metName = met.getName();
					String metNameEsc = metName;
					
	        		// escape underscore if present
	        		if (metName.contains("_")) {
	        			metNameEsc = metName.replace("_", "_1");
	        		}
	    			String sig;

	    			// build method signature if native
	        		if (met.isNative() && met.isPublic()) {
	        			sig = "Java";
	        			sig = sig + "_" + pkgName.replace(".", "_");
	        			sig = sig + "_" + clsName;
	        			sig = sig + "_" + metNameEsc;

	        			// append original name to build unique native methods set 
	        			// (remove from this set the ones statically defined)
	        			sig = sig + " ";
						sig = sig  + met.getReturns().getValue() + ":";
	        			for (JavaParameter param: met.getParameters()) {
	        				sig = sig + param.getType().getValue() + ",";
	        			}
	        			sig = sig + "\n";
	        			

						// append to output file
						fw.append(sig);
						
					}
	        			
	        			
        		}
	        		//System.out.println(met.getName() + "(" + met.isNative() + ")");
        	}

			System.out.println("INFO  - printed methods to file " + fileOutName.substring(3));
	        
        	fw.close();    
        	
        } catch (IOException ioe) {
			System.err.println("IOException: " + ioe.getMessage());
		}
    }
}
