# Gitlet Design Document

**Name**:Yuqiong Ji 3035272657; 
----------
# Classes and Data Structures


## Main

This class initializes the gitlet program.


## Blob

This class stores the content of files. 

**Fields**

1. String blobHash: Hashcode calculated based on SHA-1 values.
2. File content: Actual content stored in the Blob. 


## Commit

This class combines a log message, a timestamp, a reference to a tree, and references to parent commits (two for merges only). 

**Fields**

1. String commitHash: Hashcode calculated based on SHA-1 values.
2. String msg: Log message saved along with the commit. 
3. String time: Timestamp of the commit. 
4. HashMap<Blob> blobs: Mapping of file names to Blobs stored in this commit. 
5. List<Commit> parent: Previous commit(s) that precedes this commit. Max size 2. 


## Repository

This class is the repository that stores the gitlet object that contains the commit tree. Maintains a mapping from *branch head* to references to commits. 

**Fields**

1. File .gitlet: Directory that stores all files for the gitlet. 
2. LinkedList<Commit> commits: Linked list of all the commits made in the repository. 
3. Commit head: Pointer to the current active commit.


## Command

This class performs accordingly when main method receives various command-line arguments. 

**Fields**

1. Repository name: the name of repository 
2. String[] args: String of command arguments that we input.

**Error Handling**

1. empty arguments
2. nonexistent command
3. wrong number/format of operands
4. not initialized



----------
# Algorithm


## Blob Class
1. addFile(String[] args): add sets of file and store content in Blob. 
2. getFile(String args): get the content from Blob according to its name and version. 


## Repository Class
1. init(): open a new repository if it does not exist.
2. checkout(String[] args): Checks out the given commit.


## Commit Class
1. commit(): Creates a commit with the commit message, commit time, parent commit, and corresponding blobs. 
2. getParent(): Get previous commit. 


## Command Class
1. run(Repository repo, String name): Operate as command name under Repository repo.
2. checkFailure(): Checks if it meets the failure case.
3. dangerous():  Checks whether the command is dangerous or not. 



----------
# Persistence

In order to save the state of the repository, the state of the Blobs, Trees, and Commits need to be saved after every valid command input. To do this: 


1. Write the Commit HashMap<Blob> and LinkedList<Commit> to disk. Serialize them into bytes that we can eventually write to a specially named file on disk. This can be done with writeObject method from the Utils class.
2. Write all the Blob file objects to disk. We can serialize the Files objects and write them to files on disk (for example, “file1v1” file, “file1v2” file, “file2v1” file, etc.). This can be done with the writeObject method from the Utils class. 

In order to retrieve the state, before executing any code, use the readObject method from the Utils class to read the data of files as and deserialize the objects we previously wrote to these files.

**Cases:**

- java gitlet.Main add [Filename]

Search in the HashMap<Blob> and if it does not exist, create a new Blob for it and store it. If it exists, create a new version about it. 


- java gitlet.Main commit [message]

Saves files in the current commit. And store the commit in the LinkedList<Commit>, which ponts to the previous added files.


- java gitlet.Main rm [file name]

Find and deletle the Bloc contained the file. And modify corresponding commit.


- java gitlet.Main branch [branch name]

Creates a new branch with the given name, and points it at the current head node.


- java gitlet.Main rm-branch [branch name]

Deletes the branch with the given name. Keep all commits that were created under the branch.


-  java gitlet.Main reset [commit id]

Go through all the files tracked by the given commit. Removes tracked files with its Blob that are not present in that commit. Also moves the current branch's head to that commit node. 


- java gitlet.Main merge [branch name]
    - Find the split point, which  is a latest common ancestor of the current and given branch heads. and remove the given branch. 
    - Files in current branch that have been *modified* in the given branch since the split point, but not modified in the current branch should be changed to the version in the given branch.
    - Files that have been modified in the current branch but not in the given branch should stay as they are.
    - Files that have been modified in both branch,  in the same way (i.e., both to files with the same content or both removed) shold be left with both version.
    - Files that were not present at the split point and are present only in the current branch should remain as they are.
    - Files that were not present at the split point and are present only in the given branch should be added to the current branch.
    - Files present at the split point, unmodified in the current branch, and absent in the given branch should be removed (and untracked). We delete corresponding Blob and mofify corresponding commits.
    - Files present at the split point, unmodified in the given branch, and absent in the current branch should remain absent.

**.gitlet Directory:**
Repository object instance
HashMap<Blob> blobs
LinkedList<Commit> commits


