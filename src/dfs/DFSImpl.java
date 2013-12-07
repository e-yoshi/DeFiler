package dfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import virtualdisk.VirtualDisk;
import common.Constants;
import common.DFileID;
import dblockcache.DBufferCache;

public class DFSImpl extends DFS {
	
	DBufferCache _cache;
	
	@Override
	public void init() {
		synchronized (_cache) {
			try {
				_cache = new DBufferCache(Constants.NUM_OF_CACHE_BLOCKS, new VirtualDisk(super._volName, super._format));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<DFileID> listAllDFiles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sync() {
		// TODO Auto-generated method stub

	}

}
