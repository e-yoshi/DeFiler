package common;

/*
 * This class contains the global constants used in DFS
 */

public class Constants {

	/* The below constants indicate that we have approximately 268 MB of
	 * disk space with 67 MB of memory cache; a block can hold upto 32 inodes and
	 * the maximum file size is constrained to be 500 blocks. These are compile
	 * time constants and can be changed during evaluation.  Your implementation
	 * should be free of any hard-coded constants.  
	 */

	public static final int NUM_OF_BLOCKS = 262144; // 2^18
	public static final int BLOCK_SIZE = 1024; // 1kB
	public static final int INODE_SIZE = 32; //32 Bytes
	public static final int NUM_OF_CACHE_BLOCKS = 65536; // 2^16
	public static final int MAX_FILE_SIZE = BLOCK_SIZE*500; // Constraint on the max file size

	public static final int MAX_DFILES = 512; // For recylcing DFileIDs`
	
	public static final int INODE_FID = 0;
	public static final int INODE_FILE_SIZE = 1;
	
	// Represents an error on a reading or writing operation on the dbuffer
	public static final int DBUFFER_ERROR = -1;
	
	public static final int INTS_IN_BLOCK = BLOCK_SIZE/4;
	
	public static byte EOF = (byte) 0x1ceb00da;
	
	/* DStore Operation types */
	public enum DiskOperationType {
		READ, WRITE
	};

	/* Virtual disk file/store name */
	public static final String vdiskName = "DSTORE.dat";
}
