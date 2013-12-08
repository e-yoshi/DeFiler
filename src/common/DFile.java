package common;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import dblockcache.DBuffer;
import dfs.Inode;

public class DFile {
	private ReadWriteLock _lock = new ReentrantReadWriteLock();
	private int _file = -1;
	private int _iNodeBlock = -1;
	private int _iNodePosition = -1;
	private Inode _inode;

	public DFile(int fileId) {
		_file = fileId;
		_inode = new Inode(fileId, 0);
	}

	public DFile(int fileId, int size, int iNodeBlock, int iNodePosition) {
		_file = fileId;
		setINodeBlock(iNodeBlock);
		setINodePosition(iNodePosition);
		_inode = new Inode(fileId, size);
	}
	
	public void mapFile(List<DBuffer> indirect, List<Integer> direct) {
	    _inode.mapFile(indirect, direct);
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

	public byte[] getINodeMetadata() {
		return _inode.getMetadata();
	}
	
	public int deltaBlocks(int newSize) {
	    return (int) (Math.ceil((double) newSize/(double) Constants.BLOCK_SIZE)
	            - Math.ceil((double) _inode.getSize()/(double) Constants.BLOCK_SIZE));
	}
	
	public boolean isMapped() {
	    return _inode.isMapped();
	}
	
	public int getNumIndirectBlocks() {
	    return _inode.getNumberOfIndirectBlocks();
	}
	
	public void setMapped() {
	    _inode.setMapped();
	}
}
