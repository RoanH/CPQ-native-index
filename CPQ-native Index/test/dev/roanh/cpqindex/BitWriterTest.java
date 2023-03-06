package dev.roanh.cpqindex;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class BitWriterTest{

	@Test
	public void longTest(){
		BitWriter w = new BitWriter(64);

		assertEquals("00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000000", w.toBinaryString());
		assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 0}, w.getData());

		w.writeInt(0b110, 3);
		assertEquals("11000000 00000000 00000000 00000000 00000000 00000000 00000000 00000000", w.toBinaryString());
		assertArrayEquals(new byte[]{-64, 0, 0, 0, 0, 0, 0, 0}, w.getData());

		w.writeInt(0b1110, 3);
		assertEquals("11011000 00000000 00000000 00000000 00000000 00000000 00000000 00000000", w.toBinaryString());
		assertArrayEquals(new byte[]{-40, 0, 0, 0, 0, 0, 0, 0}, w.getData());
		
		w.writeInt(0b1111, 4);
		assertEquals("11011011 11000000 00000000 00000000 00000000 00000000 00000000 00000000", w.toBinaryString());
		assertArrayEquals(new byte[]{-37, -64, 0, 0, 0, 0, 0, 0}, w.getData());
		
		w.writeInt(0b01111111, 8);
		assertEquals("11011011 11011111 11000000 00000000 00000000 00000000 00000000 00000000", w.toBinaryString());
		assertArrayEquals(new byte[]{-37, -33, -64, 0, 0, 0, 0, 0}, w.getData());
		
		w.writeInt(0b10111111_11111111_11111111_11111101, 32);
		assertEquals("11011011 11011111 11101111 11111111 11111111 11111111 01000000 00000000", w.toBinaryString());
		assertArrayEquals(new byte[]{-37, -33, -17, -1, -1, -1, 64, 0}, w.getData());
		
		w.writeInt(0, 4);
		assertEquals("11011011 11011111 11101111 11111111 11111111 11111111 01000000 00000000", w.toBinaryString());
		assertArrayEquals(new byte[]{-37, -33, -17, -1, -1, -1, 64, 0}, w.getData());
		
		w.writeInt(1, 1);
		assertEquals("11011011 11011111 11101111 11111111 11111111 11111111 01000010 00000000", w.toBinaryString());
		assertArrayEquals(new byte[]{-37, -33, -17, -1, -1, -1, 66, 0}, w.getData());
	}
}
