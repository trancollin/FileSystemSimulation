
public class DiskDrive {
	//matrix for storage
	byte[][] storage;
	//variable that tracks how many entries are in file table
	int in_table;
	
	//constructor
	public DiskDrive() {
		//initialize storage matrix
		storage = new byte[256][512];
		//set block 1 and 2 as occupied in bitmap for file table and bitmap
		storage[1][0] = 1;
		storage[1][1] = 1;
		//initialize in_table
		in_table = 0;
	}
	
	//reads byte from storage
	public byte read(int block, int index) {
		return storage[block][index];
	}
	
	//writes byte to storage
	public void write(int block, int index, byte input) {
		storage[block][index] = input;
	}
}
