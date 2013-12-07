package dfs;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import common.Constants;
import common.DFileID;

/**
 * This class is used to hold metadata of a file. It represents the top level of 
 * indirection as it points to indirect blocks that will contain the id of blocks to a file
 * The amount of space required is calculate dynamically, so if the basic size parameters
 * are altered, inode will still be able to map a file properly
 * 
 * @author henriquemoraes
 *
 */
public class Inode {
    private DFileID _FID;
    private int _fileSize;
    private byte[] _buffer;
    
    private int _numOfIndirectBlocks;
    
    Inode(DFileID fileID, int size) {
        _FID = fileID;
        _fileSize = size;
        _buffer = new byte[Constants.INODE_SIZE];

        try{
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeInt(_FID.getDFileID());
            dos.writeInt(_fileSize);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Assuming size is given in bytes instead of blocks
        int numOfBlocks = (int) Math.ceil(_fileSize/Constants.BLOCK_SIZE);
        _numOfIndirectBlocks = (int) Math.ceil(numOfBlocks/Constants.INTS_IN_BLOCK);
    }
    
    /**
     * 
     * @return _buffer containing information of this inode
     */
    public byte[] getMetadata() {
        return _buffer;
    }
    
    /**
     * 
     * @return the number of indirect blocks this inode maps to
     */
    public int getNumberOfIndirectBlocks() {
        return _numOfIndirectBlocks;
    }
}
