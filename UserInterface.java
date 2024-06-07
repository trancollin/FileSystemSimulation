import java.io.IOException;
import java.util.*;

public class UserInterface {
	public static void main(String[] args) throws IOException {
		String allocation_method = args[0];
		Scanner input = new Scanner(System.in);
		boolean exit = false;
		switch(allocation_method) {
		//argument specifies contiguous allocation
		case "contiguous":
			System.out.println("Contiguous mode selected");
			ContiguousFileSystem contiguous = new ContiguousFileSystem();
			//loop until user enters exit integer
			while(!exit) {
				//print menu choices
				System.out.println("1) Display a file\n"
						+ "2) Display the file table\n"
						+ "3) Display the free space bitmap\n"
						+ "4) Display a disk block\n"
						+ "5) Copy a file from the simulation to a file on the real system\n"
						+ "6) Copy a file from the real system to a file in the simulation\n"
						+ "7) Delete a file\n"
						+ "8) Exit\n");
				System.out.print("Enter an integer to perform an operation: ");
				
				//get user input and perform specified operation
				int user_choice = input.nextInt();
				switch(user_choice) {
				//display file
				case 1:
					System.out.print("File to display: ");
					String filename = input.next();
					input.nextLine();
					contiguous.printFile(filename);
					break;
				
				//display file table
				case 2:
					contiguous.printFileTable();
					break;

				//display free space bitmap
				case 3:
					contiguous.printBitMap();
					break;
					
				//display disk block
				case 4:
					System.out.print("Enter the block to print: ");
					int block = input.nextInt();
					contiguous.printBlock(block);
					break;
					
				//copy file from simulation to real system
				case 5:
					System.out.print("File being copied from: ");
					String s_fn = input.next();
					input.nextLine();
					System.out.print("File being copied to: ");
					String r_fn = input.next();
					input.nextLine();
					
					contiguous.generateFile(s_fn, r_fn);
					break;
					
				//copy file from real system to simulation
				case 6:
					if(contiguous.disk.in_table == 46) {
						System.out.println("File cannot be copied, file table full\n");
						break;
					}
					//prompt user for name of file being copied from
					System.out.print("File being copied from: ");
					String real_fn = input.next();
					input.nextLine();
					//prompt user for name of file being written to 
					System.out.print("File being copied to: ");
					String simulation_fn = input.next();
					input.nextLine();
					//file name cannot be larger than 8 characters
					if(simulation_fn.length() > 8)
						System.out.println("File name is too long, try again.");
					else
						contiguous.writeFile(real_fn, simulation_fn);
					System.out.println();
					break;
					
				//delete file
				case 7:
					System.out.print("File to be deleted: ");
					String d_file = input.next();
					input.nextLine();
					contiguous.deleteFile(d_file);
					break;
				
				//exit
				case 8:
					exit = true;
					break;
					
				default:
					System.out.println("Invalid integer entered");
					break;
				}
				
			}
			
			break;
			
		//argument specifies chained allocation
		case "chained":
			System.out.println("Chained mode selected");
			ChainedFileSystem chained = new ChainedFileSystem();
			while(!exit) {
				//print menu choices
				System.out.println("1) Display a file\n"
						+ "2) Display the file table\n"
						+ "3) Display the free space bitmap\n"
						+ "4) Display a disk block\n"
						+ "5) Copy a file from the simulation to a file on the real system\n"
						+ "6) Copy a file from the real system to a file in the simulation\n"
						+ "7) Delete a file\n"
						+ "8) Exit\n");
				System.out.print("Enter an integer to perform an operation: ");
				
				//get user input and perform specified operation
				int user_choice = input.nextInt();
				switch(user_choice) {
				//display file
				case 1:
					System.out.print("File to display: ");
					String filename = input.next();
					input.nextLine();
					chained.printFile(filename);
					break;
				
				//display file table
				case 2:
					chained.printFileTable();
					break;

				//display free space bitmap
				case 3:
					chained.printBitMap();
					break;
					
				//display disk block
				case 4:
					System.out.print("Enter the block to print: ");
					int block = input.nextInt();
					chained.printBlock(block);
					break;
					
				//copy file from simulation to real system
				case 5:
					System.out.print("File being copied from: ");
					String s_fn = input.next();
					input.nextLine();
					System.out.print("File being copied to: ");
					String r_fn = input.next();
					input.nextLine();
					
					chained.generateFile(s_fn, r_fn);
					break;
					
				//copy file from real system to simulation
				case 6:
					if(chained.disk.in_table == 46) {
						System.out.println("File cannot be copied, file table full\n");
						break;
					}
					//prompt user for name of file being copied from
					System.out.print("File being copied from: ");
					String real_fn = input.next();
					input.nextLine();
					//prompt user for name of file being written to 
					System.out.print("File being copied to: ");
					String simulation_fn = input.next();
					input.nextLine();
					//file name cannot be larger than 8 characters
					if(simulation_fn.length() > 8)
						System.out.println("File name is too long, try again.");
					else
						chained.writeFile(real_fn, simulation_fn);
					System.out.println();
					break;
					
				//delete file
				case 7:
					System.out.print("File to be deleted: ");
					String d_file = input.next();
					input.nextLine();
					chained.deleteFile(d_file);
					break;
				
				//exit
				case 8:
					exit = true;
					break;
					
				default:
					System.out.println("Invalid integer entered");
					break;
				}
			}	
			break;
			
		//argument specifies indexed allocation
		case "indexed":
			System.out.println("Indexed mode selected");
			IndexedFileSystem indexed = new IndexedFileSystem();
			while(!exit) {
				//print menu choices
				System.out.println("1) Display a file\n"
						+ "2) Display the file table\n"
						+ "3) Display the free space bitmap\n"
						+ "4) Display a disk block\n"
						+ "5) Copy a file from the simulation to a file on the real system\n"
						+ "6) Copy a file from the real system to a file in the simulation\n"
						+ "7) Delete a file\n"
						+ "8) Exit\n");
				System.out.print("Enter an integer to perform an operation: ");
				
				//get user input and perform specified operation
				int user_choice = input.nextInt();
				switch(user_choice) {
				//display file
				case 1:
					System.out.print("File to display: ");
					String filename = input.next();
					input.nextLine();
					indexed.printFile(filename);
					break;
				
				//display file table
				case 2:
					indexed.printFileTable();
					break;

				//display free space bitmap
				case 3:
					indexed.printBitMap();
					break;
					
				//display disk block
				case 4:
					System.out.print("Enter the block to print: ");
					int block = input.nextInt();
					indexed.printBlock(block);
					break;
					
				//copy file from simulation to real system
				case 5:
					System.out.print("File being copied from: ");
					String s_fn = input.next();
					input.nextLine();
					System.out.print("File being copied to: ");
					String r_fn = input.next();
					input.nextLine();
					
					indexed.generateFile(s_fn, r_fn);
					break;
					
				//copy file from real system to simulation
				case 6:
					if(indexed.disk.in_table == 51) {
						System.out.println("File cannot be copied, file table full\n");
						break;
					}
					//prompt user for name of file being copied from
					System.out.print("File being copied from: ");
					String real_fn = input.next();
					input.nextLine();
					//prompt user for name of file being written to 
					System.out.print("File being copied to: ");
					String simulation_fn = input.next();
					input.nextLine();
					//file name cannot be larger than 8 characters
					if(simulation_fn.length() > 8)
						System.out.println("File name is too long, try again.");
					else
						indexed.writeFile(real_fn, simulation_fn);
					System.out.println();
					break;
					
				//delete file
				case 7:
					System.out.print("File to be deleted: ");
					String d_file = input.next();
					input.nextLine();
					indexed.deleteFile(d_file);
					break;
				
				//exit
				case 8:
					exit = true;
					break;
					
				default:
					System.out.println("Invalid integer entered");
					break;
				}
			}	
			break;
			
		default:
			System.out.println("Invalid mode entered");
		}
		System.out.println("Exited");
	}
}
