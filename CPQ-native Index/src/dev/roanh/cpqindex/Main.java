package dev.roanh.cpqindex;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

import dev.roanh.gmark.conjunct.cpq.CPQ;
import dev.roanh.gmark.conjunct.cpq.QueryGraphCPQ;
import dev.roanh.gmark.conjunct.cpq.QueryGraphCPQ.Vertex;
import dev.roanh.gmark.core.graph.Predicate;
import dev.roanh.gmark.util.GraphPanel;
import dev.roanh.gmark.util.UniqueGraph;

public class Main{

	public static void main(String[] args){
		//initialise native bindings
		try{
			loadNatives();
		}catch(IOException | UnsatisfiedLinkError e){
			e.printStackTrace();
			return;
		}
		
		//TODO
		
		try{
			Thread.sleep(10000);
			System.out.println("START");
			Instant start = Instant.now();//advogato, robots
			Index index = new Index(IndexUtil.readGraph(Paths.get("C:\\Users\\roanh\\Documents\\2 Thesis\\Datasets\\robots.edge")), 2, true, false, 7);//cores, labels
			System.out.println("done: " + Duration.between(start, Instant.now()).toString());
		}catch(Exception e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Small testing subroutine to find random CPQ cores.
	 */
	public static void findRandomCore(){
		while(true){
			CPQ q = CPQ.generateRandomCPQ(10, 2);
			System.out.println("Testing: " + q);
			QueryGraphCPQ g = q.toQueryGraph();
			System.out.println("computing core...");
			UniqueGraph<Vertex, Predicate> core = g.computeCore().toUniqueGraph();
			if(core.getEdgeCount() != g.getEdgeCount()){
				GraphPanel.show(g);
				GraphPanel.show(core, g::getVertexLabel, Predicate::getAlias);
				return;
			}
		}
	}
	
	/**
	 * Loads the compiled JNI libraries required for nauty.
	 * @throws IOException When an IOException occurs.
	 * @throws UnsatisfiedLinkError When loading a native library fails.
	 */
	public static final void loadNatives() throws IOException, UnsatisfiedLinkError{
		try(DirectoryStream<Path> libs = Files.newDirectoryStream(Paths.get("lib"), Files::isRegularFile)){
			for(Path lib : libs){
				if(lib.getFileName() != null && !lib.getFileName().toString().endsWith(".jar")){
					System.out.println("Loading native library: " + lib.getFileName());
					System.load(lib.toAbsolutePath().toString());
				}
			}
		}
	}
}
