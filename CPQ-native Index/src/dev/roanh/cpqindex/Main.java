package dev.roanh.cpqindex;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
			formatIndex(
				Paths.get("C:\\Users\\roanh\\Documents\\2 Thesis\\k-path\\0(1 i id)\\selfloop_k2_l2h"),
				Paths.get("C:\\Users\\roanh\\Documents\\2 Thesis\\k-path\\0(1 i id)\\selfloop_k2_h2p"),
				8
			);
		}catch(IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void formatIndex(Path lh, Path hp, int lc) throws IOException{
		//read path blocks
		Map<Integer, List<Integer>> blocks = new HashMap<Integer, List<Integer>>();
		List<String> lines = Files.readAllLines(hp);
		for(int i = 1; i < lines.size(); i++){
			if(!lines.get(i).isEmpty()){
				String[] args = lines.get(i).split(" ");
				List<Integer> data = new ArrayList<Integer>();
				for(int j = 1; j < args.length; j++){
					data.add(Integer.parseInt(args[j]));
				}
				blocks.put(i - 1, data);
			}
		}
		
		//reverse from blocks to labels
		Map<Integer, List<String>> labels = new HashMap<Integer, List<String>>();
		lines = Files.readAllLines(lh);
		for(int i = 1; i < lines.size(); i++){
			if(!lines.get(i).isEmpty()){
				String[] args = lines.get(i).split(" ");
				
				String label = "";
				int num = Integer.parseInt(args[0]);
				do{
					int minor = num % lc;
					label = label.isEmpty() ? String.valueOf(minor) : (minor + "." + label);
					num = ((num - minor) / lc) - 1;
				}while(num >= 0);
				
				for(int j = 1; j < args.length; j++){
					labels.computeIfAbsent(Integer.parseInt(args[j]), k->new ArrayList<String>()).add(label);
				}
			}
		}
		
		labels.forEach((l,c)->System.out.println(l + " " + c));
		
		 //(1 + id) * 6 + original
		
		final int pad = 9;
		final int end = 3 + blocks.values().stream().mapToInt(List::size).max().getAsInt() / 2;
		String[] out = new String[end + labels.values().stream().mapToInt(List::size).max().getAsInt()];
		for(int i = 0; i < out.length; i++){
			out[i] = "";
		}
		
		for(int block = 0; block < blocks.size(); block++){
			out[0] += block;
			
			int c = 2;
			List<Integer> data = blocks.get(block);
			for(int j = 0; j < data.size(); j += 2){
				out[c++] += "(" + data.get(j) + "," + data.get(j + 1) + ")";
			}
			
			out[end - 1] += "-----";
			c = end;
			for(String l : labels.get(block)){
				out[c++] += l;
			}
			
			for(int i = 0; i < out.length; i++){
				while(out[i].length() < pad * (block + 1)){
					out[i] += " ";
				}
			}
		}
		
		for(String s : out){
			System.out.println(s);
		}
	}
	
	/**
	 * Small testing subroutine to find random CPQ cores.
	 */
	public static void findRandomCore(){
		while(true){
			QueryGraphCPQ g = CPQ.generateRandomCPQ(10, 2).toQueryGraph();
			UniqueGraph<Vertex, Predicate> core = g.computeCore();
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
				if(!lib.getFileName().toString().endsWith(".jar")){
					System.out.println("Loading native library: " + lib.getFileName());
					System.load(lib.toAbsolutePath().toString());
				}
			}
		}
	}
}
