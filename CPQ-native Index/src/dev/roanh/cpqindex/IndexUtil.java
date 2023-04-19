package dev.roanh.cpqindex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import dev.roanh.gmark.core.graph.Predicate;
import dev.roanh.gmark.util.UniqueGraph;
import dev.roanh.gmark.util.UniqueGraph.GraphNode;
import dev.roanh.gmark.util.Util;

public class IndexUtil{
	
	public static UniqueGraph<Integer, Predicate> readGraph(Path file) throws IOException{
		return readGraph(Files.newInputStream(file));
	}
	
	public static UniqueGraph<Integer, Predicate> readGraph(InputStream in) throws IOException{
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(in))){
			String[] meta = reader.readLine().split(" ");
			int vertices = Integer.parseInt(meta[0]);
			int labelCount = Integer.parseInt(meta[2]);
			List<Predicate> labels = Util.generateLabels(labelCount);
			
			UniqueGraph<Integer, Predicate> graph = new UniqueGraph<Integer, Predicate>();
			for(int i = 0; i < vertices; i++){
				graph.addUniqueNode(i);
			}
			
			GraphNode<Integer, Predicate> last = null;
			String line;
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
