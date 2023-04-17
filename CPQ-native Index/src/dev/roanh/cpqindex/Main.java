package dev.roanh.cpqindex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import dev.roanh.gmark.conjunct.cpq.CPQ;
import dev.roanh.gmark.conjunct.cpq.QueryGraphCPQ;
import dev.roanh.gmark.conjunct.cpq.QueryGraphCPQ.Vertex;
import dev.roanh.gmark.core.graph.Predicate;
import dev.roanh.gmark.util.GraphPanel;
import dev.roanh.gmark.util.UniqueGraph;
import dev.roanh.gmark.util.UniqueGraph.GraphNode;
import dev.roanh.gmark.util.Util;

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
		
//		try{
//			formatIndex(
//				Paths.get("C:\\Users\\roanh\\Documents\\2 Thesis\\k-path\\(aa^bb)(cc^dd)\\chain_k4_l2h"),
//				Paths.get("C:\\Users\\roanh\\Documents\\2 Thesis\\k-path\\(aa^bb)(cc^dd)\\chain_k4_h2p"),
//				8//DO NOT FORGET TO UPDATE THE LABEL COUNT!!! 2x
//			);
//		}catch(IOException e){
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
//		CPQ cpq = CPQ.parse("(a◦b)∩(a◦c)");
//		
//		
//		CanonForm canon = new CanonForm(cpq);
//		System.out.println(canon.toStringCanon());
//		System.out.println(Arrays.toString(canon.toBinaryCanon()));
//		for(byte b : canon.toBinaryCanon()){
//			System.out.print(String.format("%1$8s", Integer.toBinaryString(Byte.toUnsignedInt(b))).replace(' ', '0') + " ");
//		}
//		System.out.println();
//		System.out.println(canon.toBase64Canon());
		
		
		try{
			Index<Integer> index = new Index<Integer>(readGraph(Paths.get("C:\\Users\\roanh\\Documents\\2 Thesis\\Datasets\\robots.edge")), 2, true);
			System.out.println("done");
			
			
		
		}catch(IllegalArgumentException | IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static UniqueGraph<Integer, Predicate> readGraph(Path file) throws IOException{
		try(Stream<String> stream = Files.lines(file, StandardCharsets.UTF_8)){
			Iterator<String> iter = stream.iterator();
			String[] meta = iter.next().split(" ");
			int vertices = Integer.parseInt(meta[0]);
			int labelCount = Integer.parseInt(meta[2]);
			List<Predicate> labels = Util.generateLabels(labelCount);
			
			UniqueGraph<Integer, Predicate> graph = new UniqueGraph<Integer, Predicate>();
			for(int i = 0; i < vertices; i++){
				graph.addUniqueNode(i);
			}
			
			GraphNode<Integer, Predicate> last = null;
			while(iter.hasNext()){
				String[] args = iter.next().split(" ");
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
//					label = label.isEmpty() ? String.valueOf(minor) : (minor + "." + label);
					label = label.isEmpty() ? (minor >= lc / 2 ? (minor - lc / 2 + "⁻") : String.valueOf(minor)) : ((minor >= lc / 2 ? (minor - lc / 2 + "⁻") : String.valueOf(minor)) + label);
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
				System.out.print("(" + data.get(j) + "," + data.get(j + 1) + "): ");
				out[c++] += "(" + data.get(j) + "," + data.get(j + 1) + ")";
			}
			
			System.out.print("[");
			out[end - 1] += "-----";
			c = end;
			labels.get(block).sort(null);
			for(String l : labels.get(block)){
				System.out.print(l + ", ");
				out[c++] += l;
			}
			System.out.println("]");
			
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
