package dev.roanh.cpqindex;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import dev.roanh.gmark.conjunct.cpq.CPQ;
import dev.roanh.gmark.conjunct.cpq.QueryGraphCPQ;
import dev.roanh.gmark.conjunct.cpq.QueryGraphCPQ.Vertex;
import dev.roanh.gmark.core.graph.Predicate;
import dev.roanh.gmark.util.GraphPanel;
import dev.roanh.gmark.util.UniqueGraph;

public class Main{
	private static Predicate l1 = new Predicate(1, "a");
	private static Predicate l2 = new Predicate(2, "b");
	private static Predicate l3 = new Predicate(3, "c");
	private static Predicate l4 = new Predicate(4, "d");

	public static void main(String[] args){
		//initialise native bindings
		try{
			loadNatives();
		}catch(IOException | UnsatisfiedLinkError e){
			e.printStackTrace();
			return;
		}
		
		//TODO
		
//		CPQ cpq = CPQ.parse("(a◦b)∩(a◦c)∩id∩a");
//		
//		cpq = CPQ.intersect(CPQ.labels(l1, l2), CPQ.labels(l1, l2));
//		cpq = CPQ.parse("((((0⁻ ∩ id) ∩ 0) ∩ ((0◦0)◦1⁻)) ∩ (((1⁻◦1) ∩ (1⁻◦1)) ∩ 0))");
		
		
		
//		GraphPanel.show(cpq);
//		GraphPanel.show(cpq.toQueryGraph().computeCore());
		
//		CanonForm canon = new CanonForm(cpq);
//		System.out.println(canon.toStringCanon());
//		System.out.println(Arrays.toString(canon.toBinaryCanon()));
//		for(byte b : canon.toBinaryCanon()){
//			System.out.print(String.format("%1$8s", Integer.toBinaryString(Byte.toUnsignedInt(b))).replace(' ', '0') + " ");
//		}
//		System.out.println();
//		System.out.println(canon.toBase64Canon());
		
		try{
			Thread.sleep(20000);
			System.out.println("START");
			Instant start = Instant.now();//advogato, robots
			Index index = new Index(IndexUtil.readGraph(Paths.get("C:\\Users\\roanh\\Documents\\2 Thesis\\Datasets\\robots.edge")), 2, true, false, 8);//cores, labels
			System.out.println("done: " + Duration.between(start, Instant.now()).toString());
		}catch(Exception e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		findRandomCore();
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
