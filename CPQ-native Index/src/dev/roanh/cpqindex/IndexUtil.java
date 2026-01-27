/*
 * CPQ-native Index: A graph database index with native support for CPQs.
 * Copyright (C) 2023  Roan Hofland (roan@roanh.dev).  All rights reserved.
 * GitHub Repository: https://github.com/RoanH/CPQ-native-index
 *
 * CPQ-native Index is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CPQ-native Index is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dev.roanh.cpqindex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import dev.roanh.gmark.type.schema.Predicate;
import dev.roanh.gmark.util.graph.generic.UniqueGraph;
import dev.roanh.gmark.util.graph.generic.UniqueGraph.GraphNode;
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
	 * Reads a graph from the given file where each line
	 * is expected to contain information for a graph edge
	 * in the format {@code source target label} and only
	 * keeps edges whose label is in the given allowed set.
	 * @param file The file to read from.
	 * @param allowedLabels The allowed label IDs, or null to allow all.
	 * @return The read graph.
	 * @throws IOException When an IOException occurs.
	 */
	public static UniqueGraph<Integer, Predicate> readGraph(Path file, Set<Integer> allowedLabels) throws IOException{
		return readGraph(Files.newInputStream(file), allowedLabels);
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
		return readGraph(in, null);
	}
	
	/**
	 * Reads a graph from the given plain text input stream
	 * each line  is expected to contain information for a
	 * graph edge in the format {@code source target label}
	 * and only keeps edges whose label is in the given allowed set.
	 * @param in The input stream to read from (plain text).
	 * @param allowedLabels The allowed label IDs, or null to allow all.
	 * @return The read graph.
	 * @throws IOException When an IOException occurs.
	 */
	public static UniqueGraph<Integer, Predicate> readGraph(InputStream in, Set<Integer> allowedLabels) throws IOException{
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
				
				if(allowedLabels != null && !allowedLabels.contains(lab)){
					continue;
				}
				
				if(last == null || last.getID() != src){
					last = graph.getNode(src);
				}
				
				last.addUniqueEdgeTo(trg, labels.get(lab));
			}
			
			return graph;
		}
	}
}
