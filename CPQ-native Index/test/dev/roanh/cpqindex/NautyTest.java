package dev.roanh.cpqindex;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class NautyTest{
	
	static{
		try{
			Main.loadNatives();
		}catch(UnsatisfiedLinkError | IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void fullCanon0(){
		assertArrayEquals(new int[]{4, 5, 7, 6, 2, 1, 0, 3}, Nauty.computeCanonSparse(new int[][]{
			new int[]{5},
			new int[]{4},
			new int[]{7, 6},
			new int[]{},
			new int[]{3},
			new int[]{3},
			new int[]{1},
			new int[]{0},
		}, new int[]{-5, -6, 7, -8, 1, 2, 3, -4}));
	}
}
