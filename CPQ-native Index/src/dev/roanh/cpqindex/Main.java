package dev.roanh.cpqindex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main{

	
	
	
	public static void main(String[] args){
		//initialise native bindings
		try{
			loadNatives();
		}catch(IOException | UnsatisfiedLinkError e){
			e.printStackTrace();
			return;
		}
		
		
		Nauty.runTest();
		
		//TODO
	}
	
	
	/**
	 * Loads the compiled JNI libraries required for nauty.
	 * @throws IOException When an IOException occurs.
	 * @throws UnsatisfiedLinkError When loading a native library fails.
	 */
	public static final void loadNatives() throws IOException, UnsatisfiedLinkError{
		for(Path lib : Files.newDirectoryStream(Paths.get("lib"), Files::isRegularFile)){
			System.out.println("Loading native library: " + lib.getFileName());
			System.load(lib.toAbsolutePath().toString());
		}
	}
}
