package dev.roanh.cpqindex;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import dev.roanh.cpqindex.Index.Block;
import dev.roanh.gmark.util.RangeList;

/**
 * Simple record representing a pair of blocks.
 * @author Roan
 * @param first The first block.
 * @param second The second block.
 */
public final record BlockPair(Block first, Block second){
	
	/**
	 * Constructs a new block pair from the given path pair.
	 * @param pair The path pair to get the blocks from.
	 */
	public BlockPair(PathPair pair){
		this(pair.getFirst().getBlock(), pair.getSecond().getBlock());
	}
	
	/**
	 * Reads a previously saved block pair from the given stream.
	 * @param in The stream to read from.
	 * @param blockMap A map of blocks read so far by ID.
	 * @throws IOException When an IOException occurs.
	 */
	public BlockPair(DataInputStream in, RangeList<Block> blockMap) throws IOException{
		this(blockMap.get(in.readInt()), blockMap.get(in.readInt()));
	}
	
	/**
	 * Writes this block pair to the given stream.
	 * @param out The stream to write to.
	 * @throws IOException When an IOException occurs.
	 */
	public void write(DataOutputStream out) throws IOException{
		out.writeInt(first.getId());
		out.writeInt(second.getId());
	}
}