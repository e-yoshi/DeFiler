package test;

import java.util.Arrays;
import java.util.Random;


import common.Constants;
import common.DFileID;
import dfs.DFS;
import dfs.DFSImpl;

public class Tester implements Runnable {

	private static final String text = "DeFiler clients can create, destroy, read, and write dfiles.";
	private static DFS dfs = new DFSImpl(false);

	public static void main(String args[]) {
		dfs.init();
		// Testing Create DFile
		// testCreateDFile();
		// TestDeleteFile
		// testDeleteDFiles();
		// Testing max byte array
		//
		
		int result = 0;
		for (int i = 0; i < 100; i++) {
			Random rand = new Random();
			System.out.println("Test #"+i);
			boolean success = testMaxByteArray(rand.nextInt(100) + 1000);
			if(success)
				result++;
		}
		System.out.println(result);
		
		
		
		// Testing Read and Write
		//testReadWriteText();
		//dfs.sync();
		// Test concurrent read write.
		/*
		byte[] buffer = text.getBytes();
		byte[] result1 = new byte[buffer.length];
		dfs.read(new DFileID(1), result1, 0, buffer.length);
		String resultString = new String(result1);
		if (resultString.equals(text))
			System.out.println(resultString);
*/
	}

	private static boolean testMaxByteArray(int i) {
	    System.out.println("test for length "+i);
		byte[] max = new byte[i];
		Random rand = new Random();
		rand.nextBytes(max);
		DFileID fileID = dfs.createDFile();
		dfs.write(fileID, max, 0, max.length);
		byte[] result = new byte[max.length];
		dfs.read(fileID, result, 0, max.length);
		if (Arrays.equals(max, result)) {
			System.out.println("Test case passed!");
			return true;
		} else {
			/*
			 * for(int i=0; i<max.length; i++) { if(max[i]!=result[i]) {
			 * System.out.println("Problem at index "+ i +"   "+ max[i] + "   "+
			 * result[i]); } }
			 */
			System.out.println("Test case failed!");
			return false;

		}
	}

	private static void testReadWriteText() {
		byte[] buffer = text.getBytes();
		DFileID fileID = dfs.createDFile();
		dfs.write(fileID, buffer, 0, buffer.length);
		byte[] result = new byte[buffer.length];
		dfs.read(fileID, result, 0, buffer.length);
		String resultString = new String(result);
		if (resultString.equals(text))
			System.out.println(resultString);
	}

	private static void testDeleteDFiles() {
		for (int i = 1; i <= 512; i++) {
			dfs.destroyDFile(new DFileID(i));
		}
	}

	private static void testCreateDFile() {
		for (int i = 0; i < 514; i++) {
			DFileID fileId = dfs.createDFile();
			if (fileId == null) {
				System.out.println("Should fail at 512. FileId = " + i);
			} else
				System.out.println("fileId = " + fileId.getDFileID());
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}
}
