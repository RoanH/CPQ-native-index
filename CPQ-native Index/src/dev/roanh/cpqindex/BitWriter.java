package dev.roanh.cpqindex;

import java.util.StringJoiner;

/**
 * Utility class for writing a sequence of integers to an array
 * while using a variable number of bits for each integer.
 * @author Roan
 */
public class BitWriter{
	/**
	 * The data array being written to.
	 */
	private byte[] data;
	/**
	 * The current byte in the data array being written.
	 */
	private int pos = 0;
	/**
	 * The next bit in the data array to write to.
	 */
	private int sub = 8;
	
	/**
	 * Constructs a new BitWriter with enough space for
	 * at least the requested number of bits.
	 * @param bits The minimum number of bits to allocate
	 *        space for in the data array.
	 */
	public BitWriter(int bits){
		int floor = Math.floorDiv(bits, 8);
		if(floor * 8 != bits){
			floor++;
		}
		
		data = new byte[floor];
	}
	
	/**
	 * Writes the bits of the given integer using
	 * exactly the given number of bits.
	 * @param i The integer to write.
	 * @param bits The number of bit to use to
	 *        write the given integer.
	 */
	public void writeInt(int i, int bits){
		if(sub >= bits){
			//fully fits
			sub -= bits;
			data[pos] |= (i & ((1 << bits) - 1)) << sub;
			if(sub == 0){
				pos++;
				sub = 8;
			}
		}else{
			//need to split
			bits -= sub;
			writeInt((i >>> bits) & ((1 << sub) - 1), sub);
			writeInt(i, bits);
		}
	}
	
	/**
	 * Gets the array containing the written bits.
	 * @return The data array being written to.
	 */
	public byte[] getData(){
		return data;
	}
	
	/**
	 * Constructs a string version of the bits written.
	 * @return A string with the written bits.
	 */
	public String toBinaryString(){
		StringJoiner buf = new StringJoiner(" ");
		for(byte b : data){
			buf.add(String.format("%1$8s", Integer.toBinaryString(Byte.toUnsignedInt(b))).replace(' ', '0'));
		}
		return buf.toString();
	}
}
