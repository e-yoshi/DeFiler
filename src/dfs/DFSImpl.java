package dfs;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import virtualdisk.VirtualDisk;
import common.Constants;
import common.DFile;
import common.DFileID;
import dblockcache.DBuffer;
import dblockcache.DBufferCache;

/**
 * 
 * This class is the entry point of the program and the main component that
 * manages the files. It can be initiated with a format parameter that specifies
 * if the disk should be formatted or not. If so, the disk attempts to zero the
 * data from every block, if not, it will scan the inode region and load the
 * existent data
 * 
 * The create file method simply creates a file and assings an id to it. Destroy
 * file erases the data in the file blocks and in its inode. It overwrites the
 * metadata and frees the indirect and direct blocks of the file.
 * 
 * readFile retrieves the file with the correct id and locks it for reading,
 * gets the mapped block ids from the file, and then loops for the appropriate
 * amount of iterations to read from the debuff requested to the cache. Finally,
 * it releases the lock
 * 
 * write file gets the file and the lock, and first calculates the discrepancy
 * between write size and file size. It allocates or deallocates the required
 * number of blocks, and initializes and maps the metadata in case it is the
 * first write to the file. Finally, it feteches the appropriate blocks from the
 * cache and overwrite them as necessary
 * 
 * getMappedBlockIDs is an interesting method to retrieve the ids of the data
 * blocks in the file. Interestingly, we could have used a list contained in the
 * object file to return the ids of the data If it was implemented this way, we
 * would be able to quickly access our data with a minimum number of lines of
 * code. But we chose to access the data in the indirect blocks mapped by the
 * inode, to scan each of them, to painfully devise an algorithm to extract
 * bytes and convert them to ints, and to retrieve the list of mapped block ids.
 * Now, several lines of code are iterated multiple times hindering the
 * performance that could let the file manager using our code to have a good
 * night of sleep. But we chose it this way to not defy the purposes of the
 * assignment
 * 
 * 
 * 
 * 
 * @author henriquemoraes, elderyoshida
 * 
 */
public class DFSImpl extends DFS {

	DBufferCache _cache;
	Map<Integer, DFile> _fileMap = new HashMap<Integer, DFile>();

	public DFSImpl() {
		super();
	}

	public DFSImpl(boolean format) {
		super(format);
	}

	public DFSImpl(String volName, boolean format) {
		super(volName, format);
	}

	@Override
	public void init() {
		if (_cache == null) {
			try {
				_cache = new DBufferCache(Constants.NUM_OF_CACHE_BLOCKS, new VirtualDisk(super._volName, super._format));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Scan Inode Region for files and check file consistency
		for (int i = 1; i <= Constants.INODE_REGION_SIZE; i++) {
			DBuffer block = _cache.getBlock(i);
			readInodes(block);
		}

		checkFileConsistency();

	}

	@Override
	public DFileID createDFile() {
		synchronized (_fileMap) {
			for (int i = 1; i <= Constants.MAX_DFILES; i++) {
				if (_fileMap.containsKey(i)) {
					continue;
				}
				_fileMap.put(i, writeInode(new DFile(i)));
				return new DFileID(i);
			}
		}
		return null;
	}

	@Override
	public void destroyDFile(DFileID dFID) {
		synchronized (_fileMap) {
			DFile file = _fileMap.get(dFID.getDFileID());
			if (file == null) {
				System.out.println("The Dfile was not found!");
				return;
			}
			// lock file writer, avoid readers from reading this file
			file.getLock().writeLock().lock();
			DBuffer dbuffer = _cache.getBlock(file.getINodeBlock());
			if (!dbuffer.checkValid()) {
				dbuffer.startFetch();
				dbuffer.waitValid();
			}
			int position = file.getINodePosition();
			byte[] buffer = new byte[Constants.INODE_SIZE];
			dbuffer.write(buffer, position, position + Constants.INODE_SIZE);
			dbuffer.startPush();
			if (file.getIndirectBlocks() != null && !file.getIndirectBlocks().isEmpty()) {
				for (int indBlocks : file.getIndirectBlocks()) {
					_cache.newFreeBlock(indBlocks);
				}
			}
			List<Integer> mappedBlocks = getMappedBlockIDs(file);
			if (mappedBlocks != null && !mappedBlocks.isEmpty()) {
				for (Integer dataBlocks : getMappedBlockIDs(file)) {
					_cache.newFreeBlock(dataBlocks);
				}
			}
			_fileMap.remove(file.getFileId());
		}
	}

	@Override
	public int read(DFileID dFID, byte[] buffer, int startOffset, int count) {
	    System.out.println("Reading...");
		DFile file = _fileMap.get(dFID.getDFileID());
		if (file == null) {
			System.out.println("Error: bad file request");
			return Constants.DBUFFER_ERROR;
		}
		file.getLock().readLock().lock();
		List<Integer> blockIDs = getMappedBlockIDs(file);
		System.out.println("Size of block ids is "+blockIDs.size()+"with numbers\n"+blockIDs.toString());
		int size = blockIDs.size();
		int start = startOffset;
		int howMany = count;
		if (file.getSize() < count)
			howMany = file.getSize();

		for (int i = 0; i < size; i++) {
			DBuffer dbuffer = _cache.getBlock(blockIDs.get(i));

			if (!dbuffer.checkValid()) {
				dbuffer.startFetch();
				dbuffer.waitValid();
			}
			ByteBuffer bytes = ByteBuffer.wrap(dbuffer.getBuffer());
                      IntBuffer ints = bytes.asIntBuffer();
                      System.out.println("elements in dbuffer: ");
                      while(ints.hasRemaining()){
                          System.out.print(" "+ints.get());
                      }
                      System.out.println("");
	
			int read = dbuffer.read(buffer, start, howMany);
			howMany -= read;
			start += read;
		}
		file.getLock().readLock().unlock();
		return count;
	}

	@Override
	public int write(DFileID dFID, byte[] buffer, int startOffset, int count) {
		DFile file = _fileMap.get(dFID.getDFileID());
		if (file == null) {
			System.out.println("Error: bad file request");
			return Constants.DBUFFER_ERROR;
		}
		file.getLock().writeLock().lock();

		List<Integer> blockIDs = getMappedBlockIDs(file);
		int deltaBlocks = file.deltaBlocks(count + startOffset);
		System.out.println("Delta blocks is "+deltaBlocks);
		if (deltaBlocks < 0) {
			deltaBlocks *= -1;
			for (int i = blockIDs.size(); i > blockIDs.size() - deltaBlocks; i--) {
				_cache.newFreeBlock(blockIDs.get(i - 1));
			}
		} else {
			// Adding blocks
			int intialSize = blockIDs.size();
			for (int i = intialSize; i < intialSize + deltaBlocks; i++) {
				if (_cache.numOfFreeBlocks() > 0) {
					int newBlock = _cache.getNextFreeBlock();
					_cache.newUsedBlock(newBlock);
					blockIDs.add(newBlock);
				}
			}
		}
		System.out.println("Size of block ids is "+blockIDs.size()+"with numbers\n"+blockIDs.toString());

		file.setSize(count);
		List<DBuffer> indirect = new ArrayList<>();
		if (!file.isMapped()) {
			for (int i = 0; i < file.getNumIndirectBlocks(); i++) {
			    
				int newBlock = _cache.getNextFreeBlock();
				_cache.newUsedBlock(newBlock);
				DBuffer dbuffer = _cache.getBlock(newBlock);
				System.out.println("Got indirect id "+newBlock);
				indirect.add(dbuffer);
				if (!dbuffer.checkValid()) {
					dbuffer.startFetch();
					dbuffer.waitValid();
				}
			}
		} else {
			while (file.getIndirectBlocks().size() < file.getNumIndirectBlocks()) {
				int newBlock = _cache.getNextFreeBlock();
				DBuffer dbuffer = _cache.getBlock(newBlock);
				indirect.add(dbuffer);
				if (!dbuffer.checkValid()) {
					dbuffer.startFetch();
					dbuffer.waitValid();
				}
			}
			for (Integer i : file.getIndirectBlocks()) {
				DBuffer dbuffer = _cache.getBlock(i);
				indirect.add(dbuffer);
				if (!dbuffer.checkValid()) {
					dbuffer.startFetch();
					dbuffer.waitValid();
				}
			}
		}
		file.mapFile(indirect, blockIDs);

		blockIDs = getMappedBlockIDs(file);
		System.out.println("New mapped block IDs has size "+blockIDs.size()+" and ids:\n"+blockIDs.toString());
		int startBlock = (int) Math.floor((double) startOffset / (double) Constants.BLOCK_SIZE);
		int startInStartBlock = startOffset % Constants.BLOCK_SIZE;
		int start = startOffset;
		int howMany = count;
		System.out.println("How many is "+howMany);
		int written = 0;
		// Actually write now
		for (int i = startBlock; i < blockIDs.size(); i++) {
			System.out.println("Requesting direct block "+blockIDs.get(i));
			DBuffer d = _cache.getBlock(blockIDs.get(i));
			if (!d.checkValid()) {
				d.startFetch();
				d.waitValid();
			}

			written = d.write(buffer, start, howMany);
			howMany -= written;
			start += written;
		}
		System.out.println("Written bytes "+(start-startOffset));
		file.getLock().writeLock().unlock();
		return count;
	}

	@Override
	public int sizeDFile(DFileID dFID) {
		synchronized (_fileMap) {
			DFile file = _fileMap.get(dFID.getDFileID());
			return file.getSize();
		}
	}

	@Override
	public List<DFileID> listAllDFiles() {
		List<DFileID> list = new ArrayList<>();
		synchronized (_fileMap) {
			if (_fileMap.keySet().isEmpty())
				return list;
			for (int file : _fileMap.keySet()) {
				list.add(new DFileID(file));
			}
		}
		return list;
	}

	@Override
	public void sync() {
		_cache.sync();
		System.out.println("Sync completed");
	}

	/**
	 * Maps the blocks from a file
	 * 
	 * @param file
	 *            the file to have its blocks mapped
	 * @return list with indexes of blocks with data
	 */
	private List<Integer> getMappedBlockIDs(DFile file) {
		List<Integer> blockIDs = new ArrayList<>();
		if (!file.isMapped())
			return blockIDs;
		List<Integer> indirectBlocks = file.getIndirectBlocks();
		for (int i : indirectBlocks) {
			DBuffer dbuffer = _cache.getBlock(i);
			if (!dbuffer.checkValid()) {
				dbuffer.startFetch();
				dbuffer.waitValid();
			}
			byte[] bytes = dbuffer.getBuffer();
			try {
				ByteArrayInputStream bos = new ByteArrayInputStream(bytes);
				DataInputStream dos = new DataInputStream(bos);
				while (dos.available() >= Constants.BYTES_PER_INT) {
					int j = 0;
					if ((j = dos.readInt()) != 0) {
						blockIDs.add(j);
					}
				}
				dos.close();
				// Create a new byte array of the correct size
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return blockIDs;
	}

	/**
	 * Reads the Inodes from a DBuffer from the Inode region during
	 * initialization.
	 * 
	 * @param buf
	 */
	private void readInodes(DBuffer buf) {
		for (int i = 0; i < Constants.INODES_IN_BLOCK; i++) {
			byte[] buffer = new byte[Constants.INODE_SIZE];
			byte[] integer = new byte[4];
			if (buf.read(buffer, i * Constants.INODE_SIZE, Constants.INODE_SIZE) == -1)
				continue;
			else {
				integer = Arrays.copyOfRange(buffer, Constants.BYTES_PER_INT * Constants.INODE_FID,
						Constants.BYTES_PER_INT * (Constants.INODE_FID + 1));
				int fileId = ByteBuffer.wrap(integer).getInt();
				if (fileId == 0) {
					continue;
				}

				if (fileId < 1 || fileId > 512) {
					throw new IllegalStateException("Invalid DFileId value of: " + fileId);
				}
				integer = Arrays.copyOfRange(buffer, Constants.BYTES_PER_INT * Constants.INODE_FILE_SIZE,
						Constants.BYTES_PER_INT * (Constants.INODE_FILE_SIZE + 1));
				int fileSize = ByteBuffer.wrap(integer).getInt();
				List<Integer> indirectBlocks = new ArrayList<>();
				for (int j = Constants.POSITION_INDIRECT_BLOCK_REGION; j < (Constants.INODE_SIZE / Constants.BYTES_PER_INT); j++) {
					integer = Arrays
							.copyOfRange(buffer, Constants.BYTES_PER_INT * j, Constants.BYTES_PER_INT * (j + 1));
					int indBlock = ByteBuffer.wrap(integer).getInt();
					if (indBlock > 0) {
						indirectBlocks.add(indBlock);
					}
				}
				if (fileSize > Constants.MAX_FILE_SIZE) {
					throw new IllegalStateException("Invalid File Size");
				}
				DFile file = new DFile(fileId, fileSize, buf.getBlockID(), i);
				file.setIndirectBlocks(indirectBlocks);
				file.setMapped();
				if (_fileMap.containsKey(fileId)) {
					throw new IllegalStateException("One Inode should only map to one file");
				}
				_fileMap.put(fileId, file);
			}
		}
	}

	/**
	 * Checks consistency of a DFile. Finds the used blocks.
	 * 
	 * @param file
	 */
	private void checkFileConsistency() {
		synchronized (_fileMap) {
			byte[] buffer = new byte[32];
			byte[] integer = new byte[4];

			if (_fileMap.isEmpty())
				return;
			for (DFile file : _fileMap.values()) {
				file.getLock().readLock().lock();
				for (int i : file.getIndirectBlocks()) {
					DBuffer indirectBlock = _cache.getBlock(i);
					if (!indirectBlock.checkValid()) {
						indirectBlock.startFetch();
						indirectBlock.waitValid();
					}
					if (i > Constants.NUM_OF_BLOCKS || i < Constants.INODE_REGION_SIZE)
						throw new IllegalStateException("Invalid block index.");
					indirectBlock.read(buffer, 0, Constants.BLOCK_SIZE);
					if (_cache.containsUsedBlock(i)) {
						throw new IllegalStateException("One block should only be mapped by one file.");
					}
					// read datablocks
					List<Integer> dataBlocks = new ArrayList<>();
					for (int j = 0; j < Constants.INTS_IN_BLOCK; j++) {
						integer = Arrays.copyOfRange(buffer, Constants.BYTES_PER_INT * j, Constants.BYTES_PER_INT
								* (j + 1));
						int dataBlockId = ByteBuffer.wrap(integer).getInt();
						if (dataBlockId == 0)
							continue;
						if (dataBlockId > Constants.NUM_OF_BLOCKS || dataBlockId < Constants.INODE_REGION_SIZE)
							throw new IllegalStateException("Invalid block index.");
						if (_cache.containsUsedBlock(dataBlockId)) {
							throw new IllegalStateException("One block should only be mapped by one file.");
						}
						_cache.newUsedBlock(dataBlockId);
						dataBlocks.add(dataBlockId);
					}

				}
				file.getLock().readLock().unlock();
			}

		}
	}

	private DFile writeInode(DFile file) {
		for (int i = 1; i <= Constants.INODE_REGION_SIZE; i++) {
			DBuffer dbuffer = _cache.getBlock(i);
			if (!dbuffer.checkValid()) {
				dbuffer.startFetch();
				dbuffer.waitValid();
			}
			byte[] block = new byte[Constants.BLOCK_SIZE];
			dbuffer.read(block, 0, Constants.BLOCK_SIZE);
			byte[] integer = new byte[Constants.BYTES_PER_INT];
			for (int j = 0; j < Constants.INODES_IN_BLOCK; j++) {
				int position = j * Constants.INODE_SIZE;
				integer = Arrays.copyOfRange(block, position, position + Constants.BYTES_PER_INT);
				
				int dfileId = ByteBuffer.wrap(integer).getInt();
				System.out.println("For position "+position+" extracted id "+dfileId);
				if (dfileId == 0) {
				    System.out.println("Found id 0, Creating file");
					dbuffer.write(file.getINodeMetadata(), position, Constants.INODE_SIZE);
					file.setINodeBlock(i);
					file.setINodePosition(j);
					return file;
				}
			}
		}
		throw new IllegalStateException("File could not be written. Exceeded Inode Space");
	}
}
