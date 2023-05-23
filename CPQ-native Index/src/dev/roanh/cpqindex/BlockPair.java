package dev.roanh.cpqindex;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import dev.roanh.cpqindex.Index.Block;
import dev.roanh.gmark.util.RangeList;

public final record BlockPair(Block first, Block second){
	
	public BlockPair(PathPair pair){
		this(pair.getFirst().getBlock(), pair.getSecond().getBlock());
	}
	
	public BlockPair(DataInputStream in, RangeList<Block> blockMap) throws IOException{
		this(blockMap.get(in.readInt()), blockMap.get(in.readInt()));
	}
	
	public void write(DataOutputStream out) throws IOException{
		out.writeInt(first.getId());
		out.writeInt(second.getId());
	}
}