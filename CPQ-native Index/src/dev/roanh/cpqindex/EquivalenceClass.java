package dev.roanh.cpqindex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import dev.roanh.gmark.core.graph.Predicate;
import dev.roanh.gmark.util.RangeList;
import dev.roanh.gmark.util.UniqueGraph;
import dev.roanh.gmark.util.UniqueGraph.GraphEdge;

public class EquivalenceClass<V extends Comparable<V>>{//TODO possibly move generics to the top class and make the rest inner classes
	private RangeList<List<LabelledPath>> segments;
	private int k;
	
	
	public static void main(String[] args){
		UniqueGraph<Integer, Predicate> g = new UniqueGraph<Integer, Predicate>();
		g.addUniqueNode(0);
		g.addUniqueNode(1);
		g.addUniqueNode(2);
		g.addUniqueNode(3);
		g.addUniqueNode(4);

		Predicate a = new Predicate(0, "a");
		Predicate b = new Predicate(1, "b");
		Predicate c = new Predicate(2, "c");
		
		g.addUniqueEdge(0, 1, c);
		g.addUniqueEdge(1, 2, a);
		g.addUniqueEdge(1, 3, a);
		g.addUniqueEdge(2, 4, b);
		g.addUniqueEdge(3, 4, b);
		
		new EquivalenceClass<Integer>(2).partition(g);
	}
	
	public EquivalenceClass(int k){
		this.k = k;
		segments = new RangeList<List<LabelledPath>>(k, ArrayList::new);
	}
	
	//partition according to k-path-bisimulation
	public void partition(UniqueGraph<V, Predicate> g) throws IllegalArgumentException{
		if(k <= 0){
			throw new IllegalArgumentException("Invalid value of k for bisimulation, has to be 1 or greater.");
		}
		
		//we first compute classes for 1-path-bisimulation
		Map<Pair, LabelledPath> pathMap = new HashMap<Pair, LabelledPath>();
		for(GraphEdge<V, Predicate> edge : g.getEdges()){
			//forward edges S1(u,v) <- all labels on edges between u and v
			pathMap.computeIfAbsent(new Pair(edge.getSource(), edge.getTarget()), LabelledPath::new).addLabel(edge.getData());
			
			//inverse edges S1(v,u) <- all labels on inverse edges between v and u
			pathMap.computeIfAbsent(new Pair(edge.getTarget(), edge.getSource()), LabelledPath::new).addLabel(edge.getData().getInverse());
		}
		
		//sort 1-path
		List<LabelledPath> segOne = segments.get(0);
		pathMap.values().stream().sorted(this::sortOnePath).forEachOrdered(segOne::add);
		
		//assign block IDs
		LabelledPath prev = null;
		int id = 1;
		for(LabelledPath seg : segOne){
			if(prev == null){
				seg.segId = id;
			}else{
				if(seg.labels.equals(prev.labels)){
					if(seg.isLoop() ^ prev.isLoop()){
						id++;
						seg.segId = id;
					}else{
						//is labels and cyclic patterns (loop) are the same the same segment ID is assigned
						seg.segId = id;
						seg.bisimilar = true;
						prev.bisimilar = true;
					}
				}else{
					id++;
					seg.segId = id;
				}
			}
			
			prev = seg;
			//order pushback?
		}
		
		
		
		System.out.println(pathMap);
		
		segOne.forEach(l->System.out.println(l.pair + " / " + l.labels + " / " + l.segId));
		
		
	}
	
	
	
	
	private int sortOnePath(LabelledPath a, LabelledPath b){
		if(a.labels.equals(b.labels)){
			if(a.isLoop()){
				return 1;
			}else if(b.isLoop()){
				return -1;
			}else if(a.pair.src.compareTo(b.pair.src) < 0){
				return 1;
			}else if(a.pair.src.equals(b.pair.src) && a.pair.trg.compareTo(b.pair.trg) < 0){
				return 1;
			}else{
				return -1;
			}
		}else{
			return a.labels.hashCode() - b.labels.hashCode();
		}
	}
	
	
//	inline bool cmpsegvaluepointer_0(const Segment *a, const Segment *b)
//	{
//	    if(a->segvalue==b->segvalue){
//			if(a->path.src==a->path.dst)return true;
//			else if(b->path.src==b->path.dst)return false;
//	        else if(a->path.src<b->path.src)return true;
//	        else if(a->path.src==b->path.src&&a->path.dst<b->path.dst)return true;
//	        else return false;
//		}
//	    else return a->segvalue < b->segvalue;
//	}
	
	
	
	
	
	private final class LabelledPath{
		private final Pair pair;
		private Set<Predicate> labels = new HashSet<Predicate>();
		
		//id stuff
		private int segId;
		
		@Deprecated
		private boolean bisimilar = false;
		
		private LabelledPath(Pair pair){
			this.pair = pair;
		}
		
		public void addLabel(Predicate label){
			labels.add(label);
		}
		
		public boolean isLoop(){
			return pair.isLoop();
		}
	}
	
	private final class Pair{//aka path, aka st-pair, aka pathkey
		private V src;//src,u
		private V trg;//trg,v
		
		private Pair(V src, V trg){
			this.src = src;
			this.trg = trg;
		}
		
		public boolean isLoop(){
			return src.equals(trg);
		}
		
		@Override
		public boolean equals(Object obj){
			if(obj instanceof EquivalenceClass.Pair){
				EquivalenceClass<?>.Pair other = (EquivalenceClass<?>.Pair)obj;
				return src.equals(other.src) && trg.equals(other.trg);
			}else{
				return false;
			}
		}

		@Override
		public int hashCode(){
			return Objects.hash(src, trg);
		}
		
		@Override
		public String toString(){
			return "(" + src + "," + trg + ")";
		}
	}
}
