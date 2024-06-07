import java.util.*;
import java.io.*;

public class ContiguousFileSystem {
	//disk drive object
	DiskDrive disk;
	//keeps track of next open entry space in file table
	int filetable_index;
	
	//constructor
	public ContiguousFileSystem() {
		disk = new DiskDrive();
		filetable_index = 0;
	}
	
	//displays a specified file
	public void printFile(String filename) {
		//find the index of the entry in the file table
		int entry = findTableEntry(filename);
		//if file could not be found notify user
		if(entry == -1) {
			System.out.println("File could not be found\n");
			return;
		}
		//length of file
		int length = (int)disk.read(0, (entry + 10));
		//get start block
		int curr = (int)disk.read(0, (entry + 8)) + (int)disk.read(0, (entry + 9)) + 2;
		//print all blocks of file
		for(int i = 0; i < length; i++) {
			//print all bytes in block
			for(int j = 0; j < 512; j++) {
				//print current byte
				System.out.print((char)disk.read(curr, j));
			}
			//move to next block
			curr++;
		}
		System.out.println();
	}
	
	//prints the file table located at block 1 
	public void printFileTable() {
		//display to user to file is empty
		if(filetable_index == 0) {
			System.out.println("File table is empty\n");
			return;
		}
		//print header
		System.out.println("File Name\t" + "Start Block\t" + "Length");
		//print all entries in the file table
		for(int i = 0; i < filetable_index; i += 11) {
			//relative index of entry
			int curr = i;
			//print simulation name of file
			for(int j = curr; j < curr + 8; j++)
				System.out.print((char)disk.read(0, j));
			System.out.print("\t\t");
			//print start block
			System.out.print(disk.read(0, (curr + 8)) + disk.read(0, (curr + 9)) + 2 + "\t\t");
			//print length of file
			System.out.println(disk.read(0, (curr + 10)));
		}
	}
	
	//prints the bitmap located at block 2
	public void printBitMap() {
		int counter = 0;
		//print every value in the bitmap
		for(int i = 0; i < 256; i++) {
			//if 32 bytes have been printed begin new row
			if(counter == 31) {
				System.out.println(disk.read(1, i));
				counter = 0;
			}
			else {
				System.out.print(disk.read(1, i));
				counter++;
			}
		}
		//end bitmap with new line
		System.out.println("\n");
	}
	
	//prints the contents of a specified block
	public void printBlock(int block) {
		for(int i = 0; i < 512; i++) {
			//print current byte
			System.out.println(disk.read(block, i));
		}
		System.out.println();
	}
	
	//writes file to disk
	public void writeFile(String real_fn, String simulation_fn) throws IOException {
		//create file object
		File file = new File(real_fn);
		//stop if file will use more than 10 blocks
		if(file.length() > 5120) {
			System.out.println("File is too large");
			return;
		}
		//get number of blocks file will use
		int blocks = (int)Math.ceil(file.length()/512.0);		
		//starting block
		int start = 0;
		//indicates if suitable starting block is found
		boolean start_found = false;
		//find contiguous open blocks to fit file
		for(int i = 2; i < 256 - blocks + 1; i++) {
			start = i;
			//range to search for empty contiguous blocks from potential start block
			int range = i + blocks;
			for(int k = i; k < range; k++) {
				//if a block in the range is not empty check next potential start block
				if(disk.read(1, k) == 1)
					break;
				//suitable start block has been found
				if(k == range - 1) {
					start_found = true;
					break;
				}
			}
			//if suitable start block found, stop searching
			if(start_found == true)
				break;
		}
		
		//if not enough contiguous blocks have been found print there is no room
		if(start_found == false) {
			System.out.println("Not enough storage available");
			return;
		}
		//else insert the file into the disk
		else {
			//create temp byte array with all bytes of file
			byte[] temp = new byte[(int)file.length()];
			FileInputStream fs = new FileInputStream(file);
			fs.read(temp);
			
			//copy bytes from temp array to blocks
			int block_num = start;
			int index = 0;
			for(int i = 0; i < temp.length; i++) {
				//write byte from temp to disk
				disk.write(block_num, index, temp[i]);
				//if end of block is reached, move to next block
				if(index == 511) {
					index = 0;
					block_num++;
				}
				//increment index
				else
					index++;
			}
			//mark blocks in bitmap as occupied
			for(int i = start; i < start + blocks; i++)
				disk.write(1, i, (byte)1);
			
			/*
			 * begin writing file entry to file table
			 * each entry is 11 bytes in length:
			 * 		8 for name
			 * 		2 for start block
			 * 		1 for length
			 */
			//write file name to file table
			for(int i = 0; i < 8; i++) {
				//if file name is not long enough to fill entry, write 0 instead
				if(i >= simulation_fn.length()) {
					disk.write(0, filetable_index, (byte)0);
					filetable_index++;
				}
				//write characters of the file name to the file table
				else {
					disk.write(0, filetable_index, (byte)simulation_fn.charAt(i));
					filetable_index++;
				}
			}
			
			/*
			 * write start block to file table
			 * start blocks are stored as two separate indexes since 
			 *		total amount of blocks > 127 (range of byte)
			 * 2 is subtracted from the stored value to make the range of possible blocks 2 - 256
			 */
			//if the start block is greater than 127, the difference of start - 127 - 2 is stored
				//in the latter index
			if(start > 127) {
				disk.write(0, filetable_index, (byte)127);
				filetable_index++;
				disk.write(0, filetable_index, (byte)(start - 129));
			}
			//otherwise, 0 is stored in the latter index
			else {
				disk.write(0, filetable_index, (byte)(start - 2));
				filetable_index++;
				disk.write(0, filetable_index, (byte)0);
			}
			filetable_index++;
			//store length of file in final byte
			disk.write(0, filetable_index, (byte)blocks);
			filetable_index++;
			disk.in_table++;
		}
	}
	
	//outputs a file from the simulation to the real system
	public void generateFile(String simulation_fn, String real_fn) throws IOException {
		//create new file if not created
		File file = new File(real_fn);
		file.createNewFile();
		//create file output stream
		FileOutputStream fos = new FileOutputStream(real_fn);
		//find entry of file in table
		int entry = findTableEntry(simulation_fn);
		//length of file
		int length = (int)disk.read(0, (entry + 10));
		//get start block
		int curr = (int)disk.read(0, (entry + 8)) + (int)disk.read(0, (entry + 9)) + 2;
		//write all blocks to file
		for(int i = 0; i < length; i++) {
			//write all bytes from block
			for(int j = 0; j < 512; j++) {
				fos.write(disk.read(curr, j));
			}
			//move to next block
			curr++;
		}
		fos.close();
	}
	
	//removes a file from the simulation
	public void deleteFile(String filename) {
		//get file entry in table
		int entry = findTableEntry(filename);
		//length of file
		int length = (int)disk.read(0, (entry + 10));
		//get start block
		int start = (int)disk.read(0, (entry + 8)) + (int)disk.read(0, (entry + 9)) + 2;
		//erase entry from byte table 
		for(int i = 0; i < 11; i++)
			disk.write(0, (entry + i), (byte)0);
		//shift down entries
		int table_index = entry + 11;
		int shift = 11;
		while(disk.read(0, table_index) != 0) {
			for(int i = 0; i < 11; i++) {
				disk.write(0, table_index - 11, disk.read(0, table_index));
				table_index++;
			}
		}
		
		//decrement disk variables
		filetable_index -= 11;
		disk.in_table--;
		
		//reset bitmap bits
		for(int i = 0; i < length; i++)
			disk.write(1, start + i, (byte)0);
		//reset appropriate blocks
		for(int i = 0; i < length; i++) {
			for(int j = 0; j < 512; j++) {
				disk.write(start, j, (byte)0);
			}
			start++;
		}
	}
	
	//used by other functions to find an entry in the file table
	public int findTableEntry(String filename) {
		for(int i = 0; i < filetable_index; i += 11) {
			int curr = i;
			String temp = "";
			for(int j = curr; j < curr + 8; j++) {
				if(disk.read(0, j) != 0)
					temp += (char)disk.read(0, j);
			}
			if(temp.equals(filename))
				return curr;
		}
		//file not in system
		return -1;
	}
}
