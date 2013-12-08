package common;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import dblockcache.DBuffer;
import dfs.Inode;

public class DFile {
	private ReadWriteLock _lock = new ReentrantReadWriteLock();
	private int _file = -1;
	private List<Integer> _indirectBlocks = null;
	private List<Integer> _dataBlocks = null;
	private int _size = -1;
	private int _iNodeBlock = -1;
	private int _iNodePosition = -1;
	private Inode _inode;

	public DFile(int fileId) {
		_file = fileId;
	}

	public DFile(int fileId, int size, int iNodeBlock, int iNodePosition) {
		_file = fileId;
		_size = size;
		setINodeBlock(iNodeBlock);
		setINodePosition(iNodePosition);
		_inode = new Inode(fileId, size);
	}
	
	public void mapFile(List<DBuffer> indirect, List<Integer> direct) {
	    _inode.mapFile(indirect, direct);
	    _indirectBlocks = _inode.getIndirectBlocks();
	}

	public ReadWriteLock getLock() {
		return _lock;
	}

	public Integer getFileId() {
		return _file;
	}

	public List<Integer> getIndirectBlocks() {
		return _inode.getIndirectBlocks();
	}

	public void setIndirectBlocks(List<Integer> indirectBlocks) {
		_inode.setIndirectBlocks(indirectBlocks);
	}

	public int getSize() {
		return _inode.getSize();
	}

	public void setSize(int size) {
	    _inode.setSize(size);
	}

	public int getINodeBlock() {
		return _iNodeBlock;
	}

	public void setINodeBlock(int iNodeBlock) {
		this._iNodeBlock = iNodeBlock;
	}

	public int getINodePosition() {
		return _iNodePosition;
	}

	public void setINodePosition(int iNodePosition) {
		this._iNodePosition = iNodePosition;
	}

	public List<Integer> getDataBlocks() {
		return _dataBlocks;
	}

	public void setDataBlocks(List<Integer> dataBlocks) {
		this._dataBlocks = dataBlocks;
	}
	
	public int deltaBlocks(int newSize) {
	    return (int) (Math.ceil((double) newSize/(double) Constants.BLOCK_SIZE)
	            - Math.ceil((double) _size/(double) Constants.BLOCK_SIZE));
	}
	
	public boolean isMapped() {
	    return _inode.isMapped();
	}
	
	public int getNumIndirectBlocks() {
	    return _inode.getNumberOfIndirectBlocks();
	}
}
