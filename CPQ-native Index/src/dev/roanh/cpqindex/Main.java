package dev.roanh.cpqindex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.roanh.cpqindex.Index.Block;
import dev.roanh.cpqindex.Index.LabelSequence;
import dev.roanh.gmark.conjunct.cpq.CPQ;
import dev.roanh.gmark.conjunct.cpq.QueryGraphCPQ;
import dev.roanh.gmark.conjunct.cpq.QueryGraphCPQ.Vertex;
import dev.roanh.gmark.core.graph.Predicate;
import dev.roanh.gmark.util.GraphPanel;
import dev.roanh.gmark.util.UniqueGraph;
import dev.roanh.gmark.util.UniqueGraph.GraphNode;
import dev.roanh.gmark.util.Util;

public class Main{

	public static void main(String[] args) throws FileNotFoundException{
		//initialise native bindings
		try{
			loadNatives();
		}catch(IOException | UnsatisfiedLinkError e){
			e.printStackTrace();
			return;
		}
		
		//TODO
		
		File file = new File("robots_fixed.txt");
		try{
			file.createNewFile();
		}catch(IOException e1){
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		PrintStream fs = new PrintStream(file);
		
		try{
			formatIndex(
				Paths.get("C:\\Users\\roanh\\Documents\\2 Thesis\\k-path\\robots_fixed_l2h"),
				Paths.get("C:\\Users\\roanh\\Documents\\2 Thesis\\k-path\\robots_fixed_h2p"),
//				Paths.get("C:\\Users\\roanh\\Documents\\2 Thesis\\k-path\\selfloop_k2_l2h"),
//				Paths.get("C:\\Users\\roanh\\Documents\\2 Thesis\\k-path\\selfloop_k2_h2p"),
				8,//DO NOT FORGET TO UPDATE THE LABEL COUNT!!! 2x
				fs
			);
		}catch(IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
		
		
		//(115,160): [0⁻0, 0⁻2]
		//(115,160), (980,160), (1175,160), (1283,160), (1360,160), (1434,160), (1436,160), (1438,160), (1439,160), (1440,160), (1441,160), (1442,160), (1443,160), (1444,160), (1445,160), (1446,160), (1447,160), (1448,160), (1449,160), (1450,160): [0⁻0, 0⁻2]

		//7721 - 7728
		
//		try{
//			Instant start = Instant.now();
//			Index<Integer> index = new Index<Integer>(readGraph(Paths.get("C:\\Users\\roanh\\Documents\\2 Thesis\\Datasets\\robots.edge")), 2, false);
//			System.out.println("done: " + Duration.between(start, Instant.now()).toString());
//			
//			index.sort();
//			
////			BufferedWriter w = Files.newBufferedWriter(Paths.get("myindex_test.txt"));
////			for(Index<Integer>.Block block : index.getBlocks()){
////				String head = block.getPaths().toString();
////				w.append(head.substring(1, head.length() - 1));
////				w.append(": ");
//////				for(Index<Integer>.LabelSequence s : block.getLabels()){
//////					for(Predicate p : s.getLabels()){
//////						w.append(p.getAlias());
//////					}
//////				}
////				w.append(block.getLabels().stream().map(ls->{
////					String str = "";
////					for(Predicate p : ls.getLabels()){
////						str += p.getAlias();
////					}
////					return str;
////				}).collect(Collectors.toList()).toString());
////				w.newLine();
////			}
////			w.flush();
////			w.close();
//		
//		}catch(IllegalArgumentException | IOException e){
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	private static <T extends Comparable<T>> String labelsToString(Index<T>.LabelSequence labels){
		StringBuilder buf = new StringBuilder();
		for(Predicate label : labels.getLabels()){
			buf.append(label.getAlias());
		}
		return buf.toString();
	}
	
	private static void formatIndex(Path lh, Path hp, int lc, PrintStream outStream) throws IOException{
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
		
//		labels.forEach((l,c)->outStream.println(l + " " + c));
		
		 //(1 + id) * 6 + original
		
		final int pad = 9;
		final int end = 3 + blocks.values().stream().mapToInt(List::size).max().getAsInt() / 2;
//		String[] out = new String[end + labels.values().stream().mapToInt(List::size).max().getAsInt()];
//		for(int i = 0; i < out.length; i++){
//			out[i] = "";
//		}
		
		List<Entry<List<String>, List<String>>> bin = new ArrayList<Entry<List<String>, List<String>>>();
		for(int block = 0; block < blocks.size(); block++){
//			out[0] += block;

			List<String> keys = new ArrayList<String>();
			int c = 2;
			List<Integer> data = blocks.get(block);
			for(int j = 0; j < data.size(); j += 2){
				keys.add("(" + data.get(j) + "," + data.get(j + 1) + ")");
				outStream.print("(" + data.get(j) + "," + data.get(j + 1) + ")");
//				out[c++] += "(" + data.get(j) + "," + data.get(j + 1) + ")";
				if(j == data.size() - 2){
					outStream.print(": ");
				}else{
					outStream.print(", ");
				}
			}
			
			List<String> labs = new ArrayList<String>();
			outStream.print("[");
//			out[end - 1] += "-----";
			c = end;
			labels.get(block).sort(null);
			int ls = 0;
			for(String l : labels.get(block)){
				labs.add(l);
				outStream.print(l);
//				out[c++] += l;
				ls++;
				if(ls != labels.get(block).size()){
					outStream.print(", ");
				}
			}
			outStream.println("]");
			
			bin.add(new SimpleEntry<List<String>, List<String>>(keys, labs));
			
//			for(int i = 0; i < out.length; i++){
//				while(out[i].length() < pad * (block + 1)){
//					out[i] += " ";
//				}
//			}
		}
		
//		ObjectOutputStream obsout = new ObjectOutputStream(new FileOutputStream(new File("robots1.bin")));
//		obsout.writeObject(bin);
//		obsout.flush();
//		obsout.close();
		
//		for(String s : out){
//			outStream.println(s);
//		}
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
