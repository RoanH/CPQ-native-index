package dev.roanh.cpqindex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.roanh.gmark.conjunct.cpq.CPQ;
import dev.roanh.gmark.util.UniqueGraph;
import dev.roanh.gmark.util.UniqueGraph.GraphEdge;

public class EquivalenceClass{//TODO possibly move generics to the top class and make the rest inner classes

	
	
	
	
	
	//partition according to k-path-bisimulation
	public static <V, E extends Comparable<E>> void partition(UniqueGraph<V, E> g, int k) throws IllegalArgumentException{
		if(k <= 0){
			throw new IllegalArgumentException("Invalid value of k for bisimulation, has to be 1 or greater.");
		}
		
		List<LabelledPath<V>> segmentsOne = new ArrayList<LabelledPath<V>>();
		Map<Pair<V>, LabelledPath<V>> pathMap = new HashMap<Pair<V>, LabelledPath<V>>();
		
		//we first compute classes for 1-path-bisimulation
		for(GraphEdge<V, E> edge : g.getEdges()){
			
			//the (u,v) key to index the data structures
			Pair<V> key = new Pair<V>(edge);
			
			
			
			
			
			
			
			
			
			
		}
		
		
	}
	
	
	
	
	
	
	
	
	
	
	private static final class LabelledPath<V>{
		private Pair<V> pair;
		
		
	}
	
	private static final class Pair<V>{//aka path, aka st-pair, aka pathkey
		private V u;//src
		private V v;//trg
		
		private Pair(GraphEdge<V, ?> edge){
			u = edge.getSource();
			v = edge.getTarget();
		}
		
		@Override
		public boolean equals(Object obj){
			if(obj instanceof Pair<?>){
				Pair<?> other = (Pair<?>)obj;
				return u.equals(other.u) && v.equals(other.v);
			}else{
				return false;
			}
		}

		@Override
		public int hashCode(){
			return Objects.hash(u, v);
		}
	}
}
