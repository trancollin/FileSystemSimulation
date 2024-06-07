import java.util.*;
import java.io.*;

public class IndexedFileSystem {
	//disk drive object
	DiskDrive disk;
	//keeps track of next open entry space in file table
	int filetable_index;
	
	public IndexedFileSystem() {
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
			//get index block
			int index_block = (int)disk.read(0, (entry + 8)) + (int)disk.read(0, (entry + 9)) + 2;
			//print all blocks in index block
			int index = 0;
			while(disk.read(index_block, index) != 0) {
				int file_block = (int)disk.read(index_block, index) + 
						(int)disk.read(index_block, index + 1) + 2;
				//print all bytes in current file block
				for(int i = 0; i < 512; i++) {
					System.out.print((char)disk.read(file_block, i));
				}
				index += 2;
			}
			System.out.println();
		}
		
		//prints the file table located at block 1 
		public void printFileTable() {
			//display if file is empty to user 
			if(filetable_index == 0) {
				System.out.println("File table is empty\n");
				return;
			}
			//print header
			System.out.println("File Name\t" + "Index Block");
			//print all entries in the file table
			for(int i = 0; i < filetable_index; i += 10) {
				//relative index of entry
				int curr = i;
				//print simulation name of file
				for(int j = curr; j < curr + 8; j++)
					System.out.print((char)disk.read(0, j));
				System.out.print("\t\t");
				//print index block
				System.out.println(disk.read(0, (curr + 8)) + disk.read(0, (curr + 9)) + 2);
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
			if(file.length() > 5120) {
				System.out.println("File is too large");
				return;
			}
			
			//get number of blocks file will use 
			int blocks = (int)Math.ceil(file.length()/512.0);
			//array to store open blocks for file and index block
			int[] open_blocks = new int[blocks + 1];
			int open_blocks_index = 0;
			//search through bitmap for open blocks
			for(int i = 0; i < 256; i++) {
				//if block is empty add it to open blocks
				if(disk.read(1, i) == 0) {
					open_blocks[open_blocks_index] = i;
					open_blocks_index++;
				}
				//stop searching if enough blocks found
				if(open_blocks_index == blocks + 1)
					break;
				//display to user if not enough blocks available
				else if(i == 255) {
					System.out.println("Not enough storage available");
					return;
				}
			}
			
			//fill index block
			int index_block = open_blocks[0];
			int index_block_index = 0;
			for(int i = 1; i < open_blocks.length; i++) {
				int file_block = open_blocks[i];
				if(file_block > 127) {
					disk.write(index_block, index_block_index, (byte)127);
					index_block_index++;
					disk.write(index_block, index_block_index, (byte)(file_block - 129));
				}
				//otherwise, 0 is stored in the latter index
				else {
					disk.write(index_block, index_block_index, (byte)(file_block - 2));
					index_block_index++;
					disk.write(index_block, index_block_index, (byte)0);
				}
				index_block_index++;	
			}
			
			//create temp byte array with all bytes of file
			byte[] temp = new byte[(int)file.length()];
			FileInputStream fs = new FileInputStream(file);
			fs.read(temp);
			
			open_blocks_index = 1;
			int block_num = open_blocks[open_blocks_index];
			int index = 0;
			for(int i = 0; i < temp.length; i++) {
				//write byte from temp to disk
				disk.write(block_num, index, temp[i]);
				//end of block is reached
				if(index == 511) {
					index = 0;
					open_blocks_index++;
					block_num = open_blocks[open_blocks_index];
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
			 * each entry is 10 bytes in length:
			 * 		8 for name
			 * 		2 for index block
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
			 * write index block to file table
			 * index blocks are stored as two separate bytes since 
			 *		total amount of blocks > 127 (range of byte)
			 * 2 is subtracted from the stored value to make the range of possible blocks 2 - 256
			 */
			//if the index block is greater than 127, the difference of index_block - 127 - 2 is stored
				//in the latter index
			if(index_block > 127) {
				disk.write(0, filetable_index, (byte)127);
				filetable_index++;
				disk.write(0, filetable_index, (byte)(index_block - 129));
			}
			//otherwise, 0 is stored in the latter index
			else {
				disk.write(0, filetable_index, (byte)(index_block - 2));
				filetable_index++;
				disk.write(0, filetable_index, (byte)0);
			}
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
			//get index block
			int index_block = (int)disk.read(0, (entry + 8)) + (int)disk.read(0, (entry + 9)) + 2;
			//print all blocks in index block
			int index = 0;
			while(disk.read(index_block, index) != 0) {
				int file_block = (int)disk.read(index_block, index) + 
						(int)disk.read(index_block, index + 1) + 2;
				//print all bytes in current file block
				for(int i = 0; i < 512; i++) {
					fos.write(disk.read(file_block, i));
				}
				index += 2;
			}
			fos.close();
		}
		//removes a file from the simulation
		public void deleteFile(String filename) {
			//get file entry in table
			int entry = findTableEntry(filename);
			//get index block
			int index_block = (int)disk.read(0, (entry + 8)) + (int)disk.read(0, (entry + 9)) + 2;
			//erase entry from block table 
			for(int i = 0; i < 10; i++)
				disk.write(0, (entry + i), (byte)0);
			//shift down entries
			int table_index = entry + 10;
			int shift = 10;
			while(disk.read(0, table_index) != 0) {
				for(int i = 0; i < 10; i++) {
					disk.write(0, table_index - 10, disk.read(0, table_index));
					table_index++;
				}
			}
			//decrement disk variables
			filetable_index -= 10;
			disk.in_table--;
			
			//reset index block in bitmap
			disk.write(1, index_block, (byte)0);
			
			//reset all file_blocks
			int index = 0;
			while(disk.read(index_block, index) != 0) {
				int file_block = (int)disk.read(index_block, index) + 
						(int)disk.read(index_block, index + 1) + 2;
				//reset all bytes in current file block
				for(int i = 0; i < 512; i++) {
					disk.write(file_block, i, (byte)0);
				}
				//reset block in bitmap
				disk.write(1, file_block, (byte)0);
				//reset file block in index block
				disk.write(index_block, index, (byte)0);
				disk.write(index_block, index + 1, (byte)0);
				index += 2;
			}
		}
		
		//used by other functions to find an entry in the file table
		public int findTableEntry(String filename) {
			//search through file table for file names
			for(int i = 0; i < filetable_index; i += 10) {
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
