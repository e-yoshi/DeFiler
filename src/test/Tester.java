package test;

import dfs.DFS;
import dfs.DFSImpl;

public class Tester implements Runnable{
	
	private DFS dfs = new DFS("vla.dat", true);
	
	public void main(String args[]) {
		dfs.createDFile();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}
