import java.util.*;
import java.io.*;

public class ChainedFileSystem {
	//disk drive object
	DiskDrive disk;
	//keeps track of next open entry space in file table
	int filetable_index;
	
	//constructor
	public ChainedFileSystem() {
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
		//get start block
		int curr = (int)disk.read(0, (entry + 8)) + (int)disk.read(0, (entry + 9)) + 2;
		boolean file_completed = false;
		//print blocks until chain ends
		while(!file_completed) {
			//print data of file in block
			for(int i = 0; i < 510; i ++) {
				System.out.print((char)disk.read(curr, i));
			}
			//if chain is over, file is completed
			if(disk.read(curr, 510) == 0 && disk.read(curr, 511) == 0) {
				System.out.println();
				file_completed = true;
			}
			//jump to next chained block
			else
				curr = disk.read(curr, 510) + disk.read(curr, 511) + 2;
		}
	}
	
	//prints the file table located at block 1 
	public void printFileTable() {
		//display if file is empty to user 
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
			//print bytes
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
		//print all bytes in block
		for(int i = 0; i < 512; i++) {
			System.out.println(disk.read(block, i));
		}
		System.out.println();
	}
	
	//writes file to disk
	public void writeFile(String real_fn, String simulation_fn) throws IOException {
		//create file object
		File file = new File(real_fn);
		//stop if file will use more than 10 blocks
		if(file.length() > 5100) {
			System.out.println("File is too large");
			return;
		}
		/* get number of blocks file will use
		 * each block will store 510 bytes of data instead of 512
		 * 		since next block will be stored in last two bytes
		 * */
		int blocks = (int)Math.ceil(file.length()/510.0);
		//array to store open blocks for file
		int[] open_blocks = new int[blocks];
		int open_blocks_index = 0;
		//search through bitmap for open blocks
		for(int i = 0; i < 256; i++) {
			//if block is empty add it to open blocks
			if(disk.read(1, i) == 0) {
				open_blocks[open_blocks_index] = i;
				open_blocks_index++;
			}
			//stop searching if enough blocks found
			if(open_blocks_index == blocks)
				break;
			//display to user if not enough blocks available
			else if(i == 255) {
				System.out.println("Not enough storage available");
				return;
			}
		}
		
		//starting block is first open block in open_block array
		int start = open_blocks[0];
		
		//create temp byte array with all bytes of file
		byte[] temp = new byte[(int)file.length()];
		FileInputStream fs = new FileInputStream(file);
		fs.read(temp);
		
		//copy bytes from temp array to blocks
		int block_num = start;
		int index = 0;
		open_blocks_index = 1;
		for(int i = 0; i < temp.length; i++) {
			//write byte from temp to disk
			disk.write(block_num, index, temp[i]);
			//end of block is reached
			if(index == 509) {
				index++;
				//write next block to last two bytes
				if(open_blocks_index != blocks) {
					//if next block is greater than 127 split it between two bytes
					if(open_blocks[open_blocks_index] > 127) {
						disk.write(block_num, index, (byte)127);
						index++;
						disk.write(block_num, index, (byte)(open_blocks[open_blocks_index] - 129));
					}
					//otherwise, 0 is stored in the latter index
					else {
						disk.write(block_num, index, (byte)(open_blocks[open_blocks_index] - 2));
						index++;
						disk.write(block_num, index, (byte)0);
					}
				}
				
				//move to next block
				index = 0;
				if(open_blocks_index != blocks)
					block_num = open_blocks[open_blocks_index];
				open_blocks_index++;
			}
			//increment index
			else
				index++;
		}
		//mark blocks in bitmap as occupied
		for(int i = 0; i < open_blocks.length; i++)
			disk.write(1, open_blocks[i], (byte)1);
		
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
	
	//outputs a file from the simulation to the real system
	public void generateFile(String simulation_fn, String real_fn) throws IOException {
		//create new file if not created
		File file = new File(real_fn);
		file.createNewFile();
		//create file output stream
		FileOutputStream fos = new FileOutputStream(real_fn);
		//find entry of file in table
		int entry = findTableEntry(simulation_fn);
		//get start block
		int curr = (int)disk.read(0, (entry + 8)) + (int)disk.read(0, (entry + 9)) + 2;
		boolean file_completed = false;
		//write to new file until chain is completed
		while(!file_completed) {
			//write data bytes in blocks to full 
			for(int i = 0; i < 510; i ++) {
				fos.write(disk.read(curr, i));
			}
			//file is complete if chain is completed
			if(disk.read(curr, 510) == 0 && disk.read(curr, 511) == 0) {
				file_completed = true;
			}
			//jump to next block in chain
			else
				curr = disk.read(curr, 510) + disk.read(curr, 511) + 2;
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
		int curr = (int)disk.read(0, (entry + 8)) + (int)disk.read(0, (entry + 9)) + 2;
		//erase entry from block table 
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
		
		//create array to store blocks occupied by the file
		int[] file_blocks = new int[length];
		int file_blocks_index = 0;
		boolean file_completed = false;
		//go through all blocks in chain
		while(!file_completed) {
			//add current block to file_blocks array
			file_blocks[file_blocks_index] = curr;
			file_blocks_index++;
			//reset all file bytes in block
			for(int i = 0; i < 510; i ++) {
				disk.write(curr, i, (byte)0);
			}
			//stop if chain is complete
			if(disk.read(curr, 510) == 0 && disk.read(curr, 511) == 0) {
				file_completed = true;
			}
			//jump to next chain and reset chain bytes
			else {
				int temp = curr;
				curr = disk.read(curr, 510) + disk.read(curr, 511) + 2;
				disk.write(temp, 510, (byte)0);
				disk.write(temp, 511, (byte)0);
			}
				
		}
		
		//reset bitmap bits
		for(int i = 0; i < file_blocks.length; i++)
			disk.write(1,  file_blocks[i], (byte)0);
	}
	
	//used by other functions to find an entry in the file table
	public int findTableEntry(String filename) {
		//search through file table for file names
		for(int i = 0; i < filetable_index; i += 11) {
			int curr = i;
			String temp = "";
			//get file name of current entry
			for(int j = curr; j < curr + 8; j++) {
				if(disk.read(0, j) != 0)
					temp += (char)disk.read(0, j);
			}
			//if filenames are the same, return current entry
			if(temp.equals(filename))
				return curr;
		}
		//file not in system
		return -1;
	}
}

