DFiler
======

Virtual file system implementation

*******Class breakdown******

Inode
This class is used to hold metadata of a file. It represents the top level of 
indirection as it points to indirect blocks that will contain the id of blocks to a file
The amount of space required is calculated dynamically, so if the basic size parameters
are altered, inode will still be able to map a file properly
The inode has a method that maps its buffer and the indirect blocks it points to. 
It contains the ids of the debuffers in its metadata that act as indirect blocks.
To map the blocks, it takes the debuffers corresponding to the indirect blocks and an integer
list of the block ids that actually make the data of the file

DFile
This class holds the basic informa
