package dfs;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import virtualdisk.VirtualDisk;
import common.Constants;
import common.DFile;
import common.DFileID;
import dblockcache.DBuffer;
import dblockcache.DBufferCache;

public class DFSImpl extends DFS {

	DBufferCache _cache;
	SortedSet<Integer> _usedBlocks = new TreeSet<Integer>();
	SortedSet<Integer> _freeBlocks = new TreeSet<Integer>();
	Map<Integer, DFile> _fileMap = new HashMap<Integer, DFile>();
	List<Integer> _lockedBlocks = new ArrayList<>();

	@Override
	public void init() {
		synchronized (_cache) {
			if (_cache == null) {
				try {
					_cache = new DBufferCache(Constants.NUM_OF_CACHE_BLOCKS, new VirtualDisk(super._volName,
							super._format));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		// Scan Inode Region for files and check file consistency
		for (int i = 1; i <= Constants.INODE_REGION_END; i++) {
			DBuffer block = _cache.getBlock(i);
			readInodes(block);
		}
		// Reserve INode Region
		for (int i = 0; i < Constants.INODE_REGION_END; i++) {
			_usedBlocks.add(i);
		}
		checkFileConsistency();

		// Find freeBlocks
		for (int i = 1; i < _usedBlocks.last(); i++) {
			if (!_usedBlocks.contains(i))
				_freeBlocks.add(i);
		}

	}

	@Override
	public DFileID createDFile() {
		synchronized (_fileMap) {
			for (int i = 0; i < Constants.MAX_DFILES; i++) {
				if (_fileMap.containsKey(i)) {
					continue;
				}
				return new DFileID(i);
			}
		}
		return null;
	}

	@Override
	public void destroyDFile(DFileID dFID) {
		synchronized (_fileMap) {
			DFile file = _fileMap.get(dFID.getDFileID());
			// lock file writer, avoid readers from reading this file
			file.getLock().writeLock().lock();
			_lockedBlocks.add(file.getINodeBlock());
			DBuffer dbuf = _cache.getBlock(file.getINodeBlock());
			int position = file.getINodePosition();
			byte[] buffer = new byte[Constants.INODE_SIZE];
			dbuf.write(buffer, position, position + Constants.INODE_SIZE);
			dbuf.startPush();
			synchronized (_usedBlocks) {
				synchronized (_freeBlocks) {
					// remove data blocks
					for (int indBlocks : file.getIndirectBlocks()) {
						_usedBlocks.remove(indBlocks);
						_freeBlocks.add(indBlocks);
					}
					for (int dataBlocks : file.getDataBlocks()) {
						_usedBlocks.remove(dataBlocks);
						_freeBlocks.add(dataBlocks);
					}
				}
			}
		}
	}

	@Override
	public int read(DFileID dFID, byte[] buffer, int startOffset, int count) {
		DFile file = _fileMap.get(dFID.getDFileID());
		if (file == null) {
			System.out.println("Error: bad file request");
			return Constants.DBUFFER_ERROR;
		}
		List<Integer> blockIDs = getMappedBlockIDs(file);
		file.getLock().readLock().lock();
		int size = blockIDs.size();
		int start = startOffset;
		int howMany = count;

		for (int i = 0; i < size; i++) {
			DBuffer dbuffer = _cache.getBlock(blockIDs.get(i));

			if (!dbuffer.checkValid()) {
				dbuffer.startFetch();
				dbuffer.waitValid();
			}

			int read = dbuffer.read(buffer, start, howMany);
			howMany -= read;
			start += read;
		}
		file.getLock().readLock().unlock();
		return count;
	}

	@Override
	public int write(DFileID dFID, byte[] buffer, int startOffset, int count) {
		// TODO Auto-generated method stub
		return 0;
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
	}

	/**
	 * Maps the blocks from a file
	 * 
	 * @param file
	 *            the file to have its blocks mapped
	 * @return list with indexes of blocks with data
	 */
	private List<Integer> getMappedBlockIDs(DFile file) {
		List<Integer> indirectBlocks = file.getIndirectBlocks();
		List<Integer> blockIDs = new ArrayList<>();
		for (int i : indirectBlocks) {
			DBuffer buffer = _cache.getBlock(i);
			byte[] bytes = buffer.getBuffer();
			try {
				ByteArrayInputStream bos = new ByteArrayInputStream(bytes);
				DataInputStream dos = new DataInputStream(bos);
				while (dos.available() >= Constants.BYTES_PER_INT) {
					blockIDs.add(dos.readInt());
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
				integer = Arrays.copyOfRange(buffer, 4 * Constants.INODE_FID, 4 * (Constants.INODE_FID + 1));
				int fileId = ByteBuffer.wrap(integer).getInt();
				integer = Arrays
						.copyOfRange(buffer, 4 * Constants.INODE_FILE_SIZE, 4 * (Constants.INODE_FILE_SIZE + 1));
				int fileSize = ByteBuffer.wrap(integer).getInt();
				List<Integer> indirectBlocks = new ArrayList<>();
				for (int j = Constants.POSITION_INDIRECT_BLOCK_REGION; j < (Constants.INODE_SIZE / 4); j++) {
					integer = Arrays.copyOfRange(buffer, 4 * j, 4 * (j + 1));
					int indBlock = ByteBuffer.wrap(integer).getInt();
					if (indBlock > 0) {
						indirectBlocks.add(indBlock);
					}
				}
				DFile file = new DFile(fileId, fileSize, indirectBlocks, buf.getBlockID(), i);
				_fileMap.put(fileId, file);
			}
		}
	}

	/**
	 * Checks consistency of a DFile. Finds the used blocks.
	 * 
	 * @param file
	 */
	private boolean checkFileConsistency() {
		synchronized (_fileMap) {
			byte[] buffer = new byte[32];
			byte[] integer = new byte[4];

			if (_fileMap.isEmpty())
				return true;
			for (DFile file : _fileMap.values()) {
				file.getLock().readLock().lock();
				for (int i : file.getIndirectBlocks()) {
					DBuffer indirectBlock = _cache.getBlock(i);
					indirectBlock.read(buffer, 0, Constants.BLOCK_SIZE);
					if (_usedBlocks.contains(i)) {
						return false; // Only one file should map to one
										// indirect block
					}
					List<Integer> dataBlocks = new ArrayList<>();
					for (int j = 0; j < Constants.INTS_IN_BLOCK; j++) {
						integer = Arrays.copyOfRange(buffer, 4 * j, 4 * (j + 1));
						int dataBlockId = ByteBuffer.wrap(integer).getInt();
						if (dataBlockId == 0)
							continue;
						if (_usedBlocks.contains(dataBlockId)) {
							return false;
						}
						_usedBlocks.add(dataBlockId);
						dataBlocks.add(dataBlockId);
						// Does datablock have a fileId in header?
					}
					file.setDataBlocks(dataBlocks);
				}
				file.getLock().readLock().unlock();
			}

		}
		return true;
	}
	/*
	 * private boolean checkFileIdBlockHeader(byte[] buffer, DFile file) {
	 * byte[] integer = new byte[4]; integer = Arrays.copyOfRange(buffer, 0, 4);
	 * int fileId = ByteBuffer.wrap(integer).getInt(); if (fileId !=
	 * file.getFileId()) { return false; // Indirect Block has the fileId in
	 * header } return true; }
	 */
}
