package dev.roanh.cpqindex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import dev.roanh.gmark.core.graph.Predicate;
import dev.roanh.gmark.util.UniqueGraph;
import dev.roanh.gmark.util.UniqueGraph.GraphNode;
import dev.roanh.gmark.util.Util;

/**
 * Collection of some small index utilities.
 * @author Roan
 */
public class IndexUtil{
	
	/**
	 * Reads a graph from the given file where each line
	 * is expected to contain information for a graph edge
	 * in the format {@code source target label}
	 * @param file The file to read from.
	 * @return The read graph.
	 * @throws IOException When an IOException occurs.
	 */
	public static UniqueGraph<Integer, Predicate> readGraph(Path file) throws IOException{
		return readGraph(Files.newInputStream(file));
	}
	
	/**
	 * Reads a graph from the given plain text input stream
	 * each line  is expected to contain information for a
	 * graph edge in the format {@code source target label}
	 * @param in The input stream to read from (plain text).
	 * @return The read graph.
	 * @throws IOException When an IOException occurs.
	 */
	public static UniqueGraph<Integer, Predicate> readGraph(InputStream in) throws IOException{
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))){
			String line = reader.readLine();
			if(line == null){
				return null;
			}
			
			String[] meta = line.split(" ");
			int vertices = Integer.parseInt(meta[0]);
			int labelCount = Integer.parseInt(meta[2]);
			List<Predicate> labels = Util.generateLabels(labelCount);
			
			UniqueGraph<Integer, Predicate> graph = new UniqueGraph<Integer, Predicate>();
			for(int i = 0; i < vertices; i++){
				graph.addUniqueNode(i);
			}
			
			GraphNode<Integer, Predicate> last = null;
			while((line = reader.readLine()) != null){
				String[] args = line.split(" ");
				if(args.length == 0){
					break;
				}
				
				int src = Integer.parseInt(args[0]);
				int trg = Integer.parseInt(args[1]);
				int lab = Integer.parseInt(args[2]);
				
				if(last == null || last.getID() != src){
					last = graph.getNode(src);
				}
				
				last.addUniqueEdgeTo(trg, labels.get(lab));
			}
			
			return graph;
		}
	}
}
