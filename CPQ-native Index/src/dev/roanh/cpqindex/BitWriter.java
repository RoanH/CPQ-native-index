package dev.roanh.cpqindex;

import java.util.StringJoiner;

public class BitWriter{
	private byte[] data;
	private int pos = 0;
	private int sub = 8;
	
	public BitWriter(int bits){
		int floor = Math.floorDiv(bits, 8);
		if(floor * 8 != bits){
			floor++;
		}
		
		data = new byte[floor];
	}
	
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
			int most = (i >>> (bits - sub)) & ((1 << sub) - 1);
			bits -= sub;
			writeInt(most, sub);
			writeInt(i, bits);
		}
	}
	
	public byte[] getData(){
		return data;
	}
	
	public String toBinaryString(){
		StringJoiner buf = new StringJoiner(" ");
		for(byte b : data){
			buf.add(String.format("%1$8s", Integer.toBinaryString(Byte.toUnsignedInt(b))).replace(' ', '0'));
		}
		return buf.toString();
	}
}
