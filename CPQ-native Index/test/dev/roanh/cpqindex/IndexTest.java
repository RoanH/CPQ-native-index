package dev.roanh.cpqindex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import dev.roanh.gmark.core.graph.Predicate;
import dev.roanh.gmark.util.UniqueGraph;

public class IndexTest{
	
	@Disabled//TODO
	@Test
	public void robots() throws IOException, ClassNotFoundException{
		UniqueGraph<Integer, Predicate> graph = Main.readGraph(Paths.get("C:\\Users\\roanh\\Documents\\2 Thesis\\Datasets\\robots.edge"));
		
		Index<Integer> index = new Index<Integer>(graph, 2, false);
		index.sort();
		
		ObjectInputStream obsout = new ObjectInputStream(new FileInputStream(new File("robots.bin")));
		List<Entry<List<String>, List<String>>> bin = (List<Entry<List<String>, List<String>>>)obsout.readObject();
		obsout.close();
		
		
		List<Index<Integer>.Block> blocks = index.getBlocks();
		assertEquals(7999, blocks.size());
		
		Iterator<Index<Integer>.Block> iter = blocks.iterator();
		Iterator<Entry<List<String>, List<String>>> real = bin.iterator();
		while(iter.hasNext()){
			
			
			
		}
	}
	
	@Disabled//TODO
	@Test
	public void robotsk1() throws IOException, ClassNotFoundException{
		UniqueGraph<Integer, Predicate> graph = Main.readGraph(Paths.get("C:\\Users\\roanh\\Documents\\2 Thesis\\Datasets\\robots.edge"));
		
		Index<Integer> index = new Index<Integer>(graph, 1, false);
		index.sort();
		
		ObjectInputStream obsout = new ObjectInputStream(new FileInputStream(new File("robots1.bin")));
		List<Entry<List<String>, List<String>>> bin = (List<Entry<List<String>, List<String>>>)obsout.readObject();
		obsout.close();
		
		List<Index<Integer>.Block> blocks = index.getBlocks();
		assertEquals(24, bin.size());
		assertEquals(24, blocks.size());
		
		Iterator<Index<Integer>.Block> iter = blocks.iterator();
		Iterator<Entry<List<String>, List<String>>> real = bin.iterator();
		while(iter.hasNext()){
			Entry<List<String>, List<String>> test = real.next();
			Index<Integer>.Block block = iter.next();
			
			assertEquals(test.getKey().toString(), block.getPaths().toString());
			assertEquals(test.getValue().toString(), block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		}
	}

	@Test
	public void constructionTest(){
		Predicate l0 = new Predicate(0, "0");
		Predicate l1 = new Predicate(1, "1");
		Predicate l2 = new Predicate(2, "2");
		Predicate l3 = new Predicate(3, "3");
		
		UniqueGraph<Integer, Predicate> g = new UniqueGraph<Integer, Predicate>();
		g.addUniqueNode(0);
		g.addUniqueNode(1);
		g.addUniqueNode(2);
		g.addUniqueNode(3);
		g.addUniqueNode(4);
		g.addUniqueNode(5);
		g.addUniqueNode(6);

		g.addUniqueEdge(0, 1, l0);
		g.addUniqueEdge(0, 2, l1);
		g.addUniqueEdge(1, 3, l0);
		g.addUniqueEdge(2, 3, l1);
		g.addUniqueEdge(3, 4, l2);
		g.addUniqueEdge(3, 5, l3);
		g.addUniqueEdge(4, 6, l2);
		g.addUniqueEdge(5, 6, l3);
		
		Index<Integer> index = new Index<Integer>(g, 4, false);
		index.sort();
		
		List<Index<Integer>.Block> blocks = index.getBlocks();
		assertEquals(49, blocks.size());
		Iterator<Index<Integer>.Block> iter = blocks.iterator();
		
		Index<Integer>.Block block = iter.next();
		assertEquals("[(0,0)]", block.getPaths().toString());
		assertEquals("[00⁻, 11⁻, 000⁻0⁻, 001⁻1⁻, 00⁻00⁻, 00⁻11⁻, 110⁻0⁻, 111⁻1⁻, 11⁻00⁻, 11⁻11⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(0,1)]", block.getPaths().toString());
		assertEquals("[0, 000⁻, 00⁻0, 110⁻, 11⁻0]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(0,2)]", block.getPaths().toString());
		assertEquals("[1, 001⁻, 00⁻1, 111⁻, 11⁻1]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(0,3)]", block.getPaths().toString());
		assertEquals("[00, 11, 000⁻0, 001⁻1, 0022⁻, 0033⁻, 00⁻00, 00⁻11, 110⁻0, 111⁻1, 1122⁻, 1133⁻, 11⁻00, 11⁻11]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(0,4)]", block.getPaths().toString());
		assertEquals("[002, 112]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(0,5)]", block.getPaths().toString());
		assertEquals("[003, 113]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(0,6)]", block.getPaths().toString());
		assertEquals("[0022, 0033, 1122, 1133]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(1,0)]", block.getPaths().toString());
		assertEquals("[0⁻, 00⁻0⁻, 01⁻1⁻, 0⁻00⁻, 0⁻11⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(1,1)]", block.getPaths().toString());
		assertEquals("[00⁻, 0⁻0, 00⁻00⁻, 00⁻0⁻0, 01⁻10⁻, 01⁻1⁻0, 022⁻0⁻, 033⁻0⁻, 0⁻000⁻, 0⁻00⁻0, 0⁻110⁻, 0⁻11⁻0]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(1,2)]", block.getPaths().toString());
		assertEquals("[01⁻, 0⁻1, 00⁻01⁻, 00⁻0⁻1, 01⁻11⁻, 01⁻1⁻1, 022⁻1⁻, 033⁻1⁻, 0⁻001⁻, 0⁻00⁻1, 0⁻111⁻, 0⁻11⁻1]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(1,3)]", block.getPaths().toString());
		assertEquals("[0, 00⁻0, 01⁻1, 022⁻, 033⁻, 0⁻00, 0⁻11]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(1,4)]", block.getPaths().toString());
		assertEquals("[02, 00⁻02, 01⁻12, 0222⁻, 022⁻2, 0332⁻, 033⁻2, 0⁻002, 0⁻112]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(1,5)]", block.getPaths().toString());
		assertEquals("[03, 00⁻03, 01⁻13, 0223⁻, 022⁻3, 0333⁻, 033⁻3, 0⁻003, 0⁻113]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(1,6)]", block.getPaths().toString());
		assertEquals("[022, 033]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(2,0)]", block.getPaths().toString());
		assertEquals("[1⁻, 10⁻0⁻, 11⁻1⁻, 1⁻00⁻, 1⁻11⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(2,1)]", block.getPaths().toString());
		assertEquals("[10⁻, 1⁻0, 10⁻00⁻, 10⁻0⁻0, 11⁻10⁻, 11⁻1⁻0, 122⁻0⁻, 133⁻0⁻, 1⁻000⁻, 1⁻00⁻0, 1⁻110⁻, 1⁻11⁻0]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(2,2)]", block.getPaths().toString());
		assertEquals("[11⁻, 1⁻1, 10⁻01⁻, 10⁻0⁻1, 11⁻11⁻, 11⁻1⁻1, 122⁻1⁻, 133⁻1⁻, 1⁻001⁻, 1⁻00⁻1, 1⁻111⁻, 1⁻11⁻1]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(2,3)]", block.getPaths().toString());
		assertEquals("[1, 10⁻0, 11⁻1, 122⁻, 133⁻, 1⁻00, 1⁻11]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(2,4)]", block.getPaths().toString());
		assertEquals("[12, 10⁻02, 11⁻12, 1222⁻, 122⁻2, 1332⁻, 133⁻2, 1⁻002, 1⁻112]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(2,5)]", block.getPaths().toString());
		assertEquals("[13, 10⁻03, 11⁻13, 1223⁻, 122⁻3, 1333⁻, 133⁻3, 1⁻003, 1⁻113]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(2,6)]", block.getPaths().toString());
		assertEquals("[122, 133]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(3,0)]", block.getPaths().toString());
		assertEquals("[0⁻0⁻, 1⁻1⁻, 0⁻00⁻0⁻, 0⁻01⁻1⁻, 0⁻0⁻00⁻, 0⁻0⁻11⁻, 1⁻10⁻0⁻, 1⁻11⁻1⁻, 1⁻1⁻00⁻, 1⁻1⁻11⁻, 22⁻0⁻0⁻, 22⁻1⁻1⁻, 33⁻0⁻0⁻, 33⁻1⁻1⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(3,1)]", block.getPaths().toString());
		assertEquals("[0⁻, 0⁻00⁻, 0⁻0⁻0, 1⁻10⁻, 1⁻1⁻0, 22⁻0⁻, 33⁻0⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(3,2)]", block.getPaths().toString());
		assertEquals("[1⁻, 0⁻01⁻, 0⁻0⁻1, 1⁻11⁻, 1⁻1⁻1, 22⁻1⁻, 33⁻1⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(3,3)]", block.getPaths().toString());
		assertEquals("[0⁻0, 1⁻1, 22⁻, 33⁻, 0⁻00⁻0, 0⁻01⁻1, 0⁻022⁻, 0⁻033⁻, 0⁻0⁻00, 0⁻0⁻11, 1⁻10⁻0, 1⁻11⁻1, 1⁻122⁻, 1⁻133⁻, 1⁻1⁻00, 1⁻1⁻11, 222⁻2⁻, 223⁻3⁻, 22⁻0⁻0, 22⁻1⁻1, 22⁻22⁻, 22⁻33⁻, 332⁻2⁻, 333⁻3⁻, 33⁻0⁻0, 33⁻1⁻1, 33⁻22⁻, 33⁻33⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(3,4)]", block.getPaths().toString());
		assertEquals("[2, 0⁻02, 1⁻12, 222⁻, 22⁻2, 332⁻, 33⁻2]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(3,5)]", block.getPaths().toString());
		assertEquals("[3, 0⁻03, 1⁻13, 223⁻, 22⁻3, 333⁻, 33⁻3]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(3,6)]", block.getPaths().toString());
		assertEquals("[22, 33, 0⁻022, 0⁻033, 1⁻122, 1⁻133, 222⁻2, 223⁻3, 22⁻22, 22⁻33, 332⁻2, 333⁻3, 33⁻22, 33⁻33]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(4,0)]", block.getPaths().toString());
		assertEquals("[2⁻0⁻0⁻, 2⁻1⁻1⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(4,1)]", block.getPaths().toString());
		assertEquals("[2⁻0⁻, 22⁻2⁻0⁻, 23⁻3⁻0⁻, 2⁻0⁻00⁻, 2⁻0⁻0⁻0, 2⁻1⁻10⁻, 2⁻1⁻1⁻0, 2⁻22⁻0⁻, 2⁻33⁻0⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(4,2)]", block.getPaths().toString());
		assertEquals("[2⁻1⁻, 22⁻2⁻1⁻, 23⁻3⁻1⁻, 2⁻0⁻01⁻, 2⁻0⁻0⁻1, 2⁻1⁻11⁻, 2⁻1⁻1⁻1, 2⁻22⁻1⁻, 2⁻33⁻1⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(4,3)]", block.getPaths().toString());
		assertEquals("[2⁻, 22⁻2⁻, 23⁻3⁻, 2⁻0⁻0, 2⁻1⁻1, 2⁻22⁻, 2⁻33⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(4,4)]", block.getPaths().toString());
		assertEquals("[22⁻, 2⁻2, 22⁻22⁻, 22⁻2⁻2, 23⁻32⁻, 23⁻3⁻2, 2⁻0⁻02, 2⁻1⁻12, 2⁻222⁻, 2⁻22⁻2, 2⁻332⁻, 2⁻33⁻2]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(4,5)]", block.getPaths().toString());
		assertEquals("[23⁻, 2⁻3, 22⁻23⁻, 22⁻2⁻3, 23⁻33⁻, 23⁻3⁻3, 2⁻0⁻03, 2⁻1⁻13, 2⁻223⁻, 2⁻22⁻3, 2⁻333⁻, 2⁻33⁻3]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(4,6)]", block.getPaths().toString());
		assertEquals("[2, 22⁻2, 23⁻3, 2⁻22, 2⁻33]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(5,0)]", block.getPaths().toString());
		assertEquals("[3⁻0⁻0⁻, 3⁻1⁻1⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(5,1)]", block.getPaths().toString());
		assertEquals("[3⁻0⁻, 32⁻2⁻0⁻, 33⁻3⁻0⁻, 3⁻0⁻00⁻, 3⁻0⁻0⁻0, 3⁻1⁻10⁻, 3⁻1⁻1⁻0, 3⁻22⁻0⁻, 3⁻33⁻0⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(5,2)]", block.getPaths().toString());
		assertEquals("[3⁻1⁻, 32⁻2⁻1⁻, 33⁻3⁻1⁻, 3⁻0⁻01⁻, 3⁻0⁻0⁻1, 3⁻1⁻11⁻, 3⁻1⁻1⁻1, 3⁻22⁻1⁻, 3⁻33⁻1⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(5,3)]", block.getPaths().toString());
		assertEquals("[3⁻, 32⁻2⁻, 33⁻3⁻, 3⁻0⁻0, 3⁻1⁻1, 3⁻22⁻, 3⁻33⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(5,4)]", block.getPaths().toString());
		assertEquals("[32⁻, 3⁻2, 32⁻22⁻, 32⁻2⁻2, 33⁻32⁻, 33⁻3⁻2, 3⁻0⁻02, 3⁻1⁻12, 3⁻222⁻, 3⁻22⁻2, 3⁻332⁻, 3⁻33⁻2]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(5,5)]", block.getPaths().toString());
		assertEquals("[33⁻, 3⁻3, 32⁻23⁻, 32⁻2⁻3, 33⁻33⁻, 33⁻3⁻3, 3⁻0⁻03, 3⁻1⁻13, 3⁻223⁻, 3⁻22⁻3, 3⁻333⁻, 3⁻33⁻3]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(5,6)]", block.getPaths().toString());
		assertEquals("[3, 32⁻2, 33⁻3, 3⁻22, 3⁻33]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(6,0)]", block.getPaths().toString());
		assertEquals("[2⁻2⁻0⁻0⁻, 2⁻2⁻1⁻1⁻, 3⁻3⁻0⁻0⁻, 3⁻3⁻1⁻1⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(6,1)]", block.getPaths().toString());
		assertEquals("[2⁻2⁻0⁻, 3⁻3⁻0⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(6,2)]", block.getPaths().toString());
		assertEquals("[2⁻2⁻1⁻, 3⁻3⁻1⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(6,3)]", block.getPaths().toString());
		assertEquals("[2⁻2⁻, 3⁻3⁻, 2⁻22⁻2⁻, 2⁻23⁻3⁻, 2⁻2⁻0⁻0, 2⁻2⁻1⁻1, 2⁻2⁻22⁻, 2⁻2⁻33⁻, 3⁻32⁻2⁻, 3⁻33⁻3⁻, 3⁻3⁻0⁻0, 3⁻3⁻1⁻1, 3⁻3⁻22⁻, 3⁻3⁻33⁻]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(6,4)]", block.getPaths().toString());
		assertEquals("[2⁻, 2⁻22⁻, 2⁻2⁻2, 3⁻32⁻, 3⁻3⁻2]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(6,5)]", block.getPaths().toString());
		assertEquals("[3⁻, 2⁻23⁻, 2⁻2⁻3, 3⁻33⁻, 3⁻3⁻3]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
		
		block = iter.next();
		assertEquals("[(6,6)]", block.getPaths().toString());
		assertEquals("[2⁻2, 3⁻3, 2⁻22⁻2, 2⁻23⁻3, 2⁻2⁻22, 2⁻2⁻33, 3⁻32⁻2, 3⁻33⁻3, 3⁻3⁻22, 3⁻3⁻33]", block.getLabels().stream().map(this::labelsToString).collect(Collectors.toList()).toString());
	}
	
	private <T extends Comparable<T>> String labelsToString(Index<T>.LabelSequence labels){
		StringBuilder buf = new StringBuilder();
		for(Predicate label : labels.getLabels()){
			buf.append(label.getAlias());
		}
		return buf.toString();
	}
}
