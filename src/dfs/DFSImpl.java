package dfs;

import java.util.List;

import common.DFileID;

public class DFSImpl extends DFS {
	
	@Override
	public void init() {
		// TODO Auto-generated method stub
		synchronized (this) {
			
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
