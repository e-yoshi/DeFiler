package dfs;

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void destroyDFile(DFileID dFID) {
		// TODO Auto-generated method stub

	}

	@Override
	public int read(DFileID dFID, byte[] buffer, int startOffset, int count) {
		// TODO Auto-generated method stub
		return 0;
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
		// TODO Auto-generated method stub

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
				for (int i : file.getIndirectBlocks()) {
					DBuffer indirectBlock = _cache.getBlock(i);
					indirectBlock.read(buffer, 0, Constants.BLOCK_SIZE);
					if (_usedBlocks.contains(i)) {
						return false; // Only one file should map to one
										// indirect block
					}
					for (int j = 0; j < Constants.INTS_IN_BLOCK; j++) {
						integer = Arrays.copyOfRange(buffer, 4 * j, 4 * (j + 1));
						int dataBlockId = ByteBuffer.wrap(integer).getInt();
						if (dataBlockId == 0)
							continue;
						if (_usedBlocks.contains(dataBlockId)) {
							return false;
						}
						_usedBlocks.add(dataBlockId);
						// Does datablock have a fileId in header?
					}
				}
			}

		}
		return true;
	}
	/*
	private boolean checkFileIdBlockHeader(byte[] buffer, DFile file) {
		byte[] integer = new byte[4];
		integer = Arrays.copyOfRange(buffer, 0, 4);
		int fileId = ByteBuffer.wrap(integer).getInt();
		if (fileId != file.getFileId()) {
			return false; // Indirect Block has the fileId in header
		}
		return true;
	}*/
}
