package gitlet;

import java.io.Serializable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.TreeMap;

/**
 * Gitlet class.
 * @author Xidong Wu.
 */
public class Command implements Serializable {

    /** Current Working Directory. */
    static final File CWD = new File(".");
    /** Git Directory. */
    static final File GIT_FOLDER = new File(".gitlet");
    /** Current staging Directory. */
    static final File STAGE_FOLDER = new File(".gitlet/staging");
    /** Current blobs Directory. */
    static final File BLOB_FOLDER = new File(".gitlet/blobs");

    /** Constructor.*/
    public Command() {
        _branch = null;
        _head = null;
        _branchCommits = new HashMap<String, LinkedList<Commit>>();
        _allCommits = new HashMap<>();
        _remove = new HashSet<>();
        _staging = new HashSet<>();
        _rmTmp = new HashSet<>();
    }

    /** init command.*/
    public void init() throws IOException {
        if (!Files.exists(Paths.get(".gitlet"))) {
            GIT_FOLDER.mkdir();
            STAGE_FOLDER.mkdir();
            BLOB_FOLDER.mkdir();
            Commit commit = new Commit();
            _head = commit;
            _branch = "master";

            if (_branchCommits.get(_branch) == null) {
                LinkedList<Commit> commitlist = new LinkedList<>();
                commitlist.add(commit);
                _branchCommits.put(_branch, commitlist);
            } else {
                _branchCommits.get(_branch).add(commit);
            }
            _allCommits.put(commit.getindex(), commit);

        } else {
            System.out.println("A Gitlet version-control system"
                    + " already exists in the current directory.");
            System.exit(0);
        }
    }

    /** add method without targetfile.
     *  @param filename String */
    public void add(String filename) {
        add(filename, null);
    }

    /** add method with targetfile.
     * @param filename String
     * @param targetFiles TreeMap<String, String>*/
    public void add(String filename, TreeMap<String, String> targetFiles) {
        String[] namelst = filename.split("/");
        String name = namelst[namelst.length - 1];

        File newFile = null;
        if (targetFiles == null) {
            if (!(new File(filename)).exists()) {
                System.out.println("File does not exist.");
                System.exit(0);
                return;
            }
            newFile = new File(filename);
        } else {
            newFile = Utils.join(BLOB_FOLDER, targetFiles.get(filename));
            _head.getTrackedFiles().add(name);
        }
        String version1 = Utils.readContentsAsString(newFile);
        File oldFile = Utils.join(STAGE_FOLDER, name);

        if (_remove.contains(name)) {
            _remove.remove(name);
        }

        _staging.add(name);
        Utils.writeContents(oldFile, Utils.readContents(newFile));

        if (_head.getBlob().size() > 0) {
            String file = _head.getBlob().get(name);
            if (file != null) {
                File temp = Utils.join(BLOB_FOLDER, file);
                String version2 = Utils.readContentsAsString(temp);
                if (version1.equals(version2)) {
                    _staging.remove(name);
                    oldFile.delete();
                }
            }
        }
    }

    /** commit command.
     * @param msg String[]*/
    public void commit(String[] msg) {
        if (msg.length != 2 || msg[1].equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        } else {
            commit(msg[1]);
        }
    }

    /** commit command.
     * @param msg String*/
    public void commit(String msg) {
        if (_staging.size() == 0 && _remove.size() == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
            return;
        }
        commit(new Commit(_head, msg));
    }

    /** commit command.
     * @param commit Commit*/
    @SuppressWarnings("unchecked")
    public void commit(Commit commit) {
        HashSet<String> tempStage = (HashSet<String>) _staging.clone();
        for (String iter : tempStage) {
            commit.getTrackedFiles().add(iter);
            File from = Utils.join(STAGE_FOLDER, iter);
            String content = Utils.readContentsAsString(from);
            String code = Utils.sha1(content);
            commit.getBlob().put(iter, code);
            File to = Utils.join(BLOB_FOLDER, code);
            Utils.writeContents(to, Utils.readContents(from));
            _staging.remove(iter);
            from.delete();
        }
        for (String s : _rmTmp) {
            commit.getTrackedFiles().remove(s);
            commit.getBlob().remove(s);
        }
        _rmTmp.clear();

        for (String s : _remove) {
            commit.getBlob().remove(s);
            _remove.remove(s);
        }
        commit.setindex();
        if (_branchCommits.get(_branch) == null) {
            LinkedList<Commit> com = new LinkedList<>();
            com.add(commit);
            _branchCommits.put(_branch, com);
        } else {
            _branchCommits.get(_branch).add(commit);
        }
        _allCommits.put(commit.getindex(), commit);
        _head = commit;
    }

    /** remove command.
     * @param filename String*/
    public void remove(String filename) {
        String[] namelst = filename.split("/");
        String name = namelst[namelst.length - 1];
        _remove.add(name);
        if (!_staging.contains(name)
                && !_head.getTrackedFiles().contains(name)) {
            System.out.println("No reason to remove the file.");
            return;
        }
        if (_staging.contains(name)) {
            _staging.remove(name);
            File file = Utils.join(STAGE_FOLDER, name);
            file.delete();
        }
        if (_head.getTrackedFiles().contains(name)) {
            _rmTmp.add(name);
            File f = new File(filename);
            if (f.exists()) {
                f.delete();
            }
        }
    }

    /** display information about each commit backwards. */
    public void log() {
        String output = "";
        Commit pointer = _head;

        while (pointer != null) {
            output += "=== \n";
            output += "commit " + pointer.getindex() + "\n";
            if (pointer.getParent() != null
                    && pointer.getParent().size() == 2) {
                output += "Merge: ";
                output += pointer.getParent().get(0).
                        getindex().substring(0, 7);
                output += " ";
                output += pointer.getParent().get(1).
                        getindex().substring(0, 7);
                output += "\n";
            }

            output += "Date: " + pointer.getTime() + "\n";
            output += pointer.getMsg() + "\n" + "\n";
            if (pointer.getParent() != null) {
                pointer = pointer.getParent().getFirst();
            } else {
                pointer = null;
            }
        }
        System.out.println(output);
    }

    /** Show all the log from all commits. */
    public void globalLog() {
        String output = "";

        for (String key : _allCommits.keySet()) {
            Commit pointer = _allCommits.get(key);
            output += "===\n";
            output += "commit " + pointer.getindex() + "\n";

            if (pointer.getParent() != null
                    && pointer.getParent().size() == 2) {
                output += "Merge: ";
                output += pointer.getParent().get(0).
                        getindex().substring(0, 7);
                output += " ";
                output += pointer.getParent().get(1).
                        getindex().substring(0, 7);
                output += "\n";
            }
            output += "Date: " + pointer.getTime() + "\n";
            output += pointer.getMsg() + "\n" + "\n";
        }
        System.out.println(output);
    }

    /** Find the commit index with particular commit msg.
     * @param msg msg.
     */
    public void find(String msg) {
        String output = "";
        for (String key : _allCommits.keySet()) {
            Commit pointer = _allCommits.get(key);

            if (pointer.getMsg().equals(msg)) {
                output += pointer.getindex() + "\n";
            }
        }

        if (output.length() == 0) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        } else {
            System.out.println(output);
        }
    }

    /** Print out information about status.*/
    public void status() {
        if (_head == null) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        String output = "=== Branches ===\n";
        List<String> brachname = new ArrayList<>(_branchCommits.keySet());
        Collections.sort(brachname);
        for (String entry : brachname) {
            if (entry.equals(_branch)) {
                output += "*" + _branch + "\n";
            } else {
                output += entry + "\n";
            }
        }
        output += "\n" + "=== Staged Files ===" + "\n";
        List<String> stagename = new ArrayList<>(_staging);
        Collections.sort(stagename);
        for (String entry: stagename) {
            output += entry + "\n";
        }
        output += "\n" + "=== Removed Files ===" + "\n";
        List<String> removename = new ArrayList<>(_remove);
        Collections.sort(removename);
        for (String entry: removename) {
            if (!(new File(entry)).exists()) {
                output += entry + "\n";
            }
        }
        if (mergedcommand) {
            output += "\n" + "=== Modifications Not Staged For Commit ==="
                    + "\n";
            output += "\n" + "=== Untracked Files ===" + "\n";
        } else {
            output = statushelper(output);
        }
        output += "\n";

        System.out.println(output);
    }

    /**Helper to Print out information about status.
     * @param  output String
     * @return output String*/
    public String statushelper(String output) {
        List<String> wkd = new ArrayList<>();
        if (CWD.list() != null) {
            for (String name: CWD.list()) {
                int len = name.length();
                if (len > 4 && name.substring(len - 4, len).equals(".txt")) {
                    wkd.add(name);
                }
            }
        }
        List<String> total = new ArrayList<>(); total.addAll(wkd);
        total.addAll(_staging); total.addAll(_head.getTrackedFiles());
        HashSet<String> h = new HashSet<String>(total);
        total.clear(); total.addAll(h); Collections.sort(total);
        String modify = "";
        String untrack = "";
        for (String entry: total) {
            if (wkd.contains(entry)) {
                String content = Utils.readContentsAsString(new File(entry));
                if ((_head.getBlob().containsKey(entry)
                        && !_staging.contains(entry)
                        && !_head.getBlob().get(entry).
                        equals(Utils.sha1(content)))
                        || (_staging.contains(entry) && !content.
                        equals(Utils.readContentsAsString(
                        Utils.join(STAGE_FOLDER, entry))))) {
                    modify += entry + " (modified)" +  "\n";
                } else if (!_head.getBlob().containsKey(entry)
                        && !_staging.contains(entry)) {
                    untrack += entry + "\n";
                }
            } else if (_staging.contains(entry)
                    || (!_remove.contains(entry)
                    && _head.getTrackedFiles().contains(entry))) {
                modify += entry +  " (deleted)" + "\n";
            }
        }
        output += "\n" + "=== Modifications Not Staged For Commit ===" + "\n";
        output += modify;
        output += "\n" + "=== Untracked Files ===" + "\n";
        output += untrack;
        return output;
    }

    /**
     * 1. checkout [branch name]:
     * Create/overwrite files, update _currentBranch & _head.
     * 2. checkout -- [file name]: Write out file.
     * 3. checkout [commit id] -- [file name]: Create/overwrite files.
     *
     * @param args args.
     */
    public void checkout(String[] args) {
        if (args.length == 2) {
            if (args[1].equals(_branch)) {
                System.out.println("No need to checkout the current branch.");
                return;
            } else if (!_branchCommits.keySet().contains(args[1])) {
                System.out.println("No such branch exists.");
                return;
            }
            Commit commit = _branchCommits.get(args[1]).getLast();
            for (String s : commit.getBlob().keySet()) {
                if (!_head.getTrackedFiles().contains(s)
                        && new File(s).exists()) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    return;
                }
            }
            TreeMap<String, String> tempBlob = commit.getBlob();
            for (String s : _head.getTrackedFiles()) {
                if (!tempBlob.keySet().contains(s)) {
                    new File(s).delete();
                }
            }
            _branch = args[1];
            for (String s : tempBlob.keySet()) {
                File temp = Utils.join(BLOB_FOLDER, tempBlob.get(s));
                Utils.writeContents(new File(s), Utils.readContents(temp));
            }
            _head = commit;
        } else if (args.length == 3) {
            if (!args[1].equals("--")) {
                System.out.println("Incorrect operands.");
                return;
            }
            Commit commit = _head;
            if (!commit.getBlob().containsKey(args[2])) {
                System.out.println("File does not exist in that commit.");
                return;
            }
            String code = _head.getBlob().get(args[2]);
            byte[] content = Utils.readContents(Utils.join(BLOB_FOLDER, code));
            Utils.writeContents(new File(args[2]), content);
        } else if (args.length == 4) {
            checkouthelper(args);
        } else {
            System.out.println("Incorrect operands.");
            System.exit(0);
            return;
        }
    }

    /**
     * 3. checkout [commit id] -- [file name]: Create/overwrite files.
     *
     * @param args args.
     */
    public void checkouthelper(String[] args) {
        if (!args[2].equals("--")) {
            System.out.println("Incorrect operands.");
            System.exit(0); return;
        }
        String code = args[1]; Commit commit;
        boolean notexist = true;
        for (String key : _allCommits.keySet()) {
            if (code.equals(key.substring(0, code.length()))) {
                commit = _allCommits.get(key);
                notexist = false;
                if (commit.getBlob().containsKey(args[3])) {
                    File temp = Utils.join(BLOB_FOLDER,
                            commit.getBlob().get(args[3]));
                    Utils.writeContents(new File(args[3]),
                            Utils.readContents(temp));
                } else {
                    System.out.println("File does not exist in that commit.");
                    return;
                }
                break;
            }
        }
        if (notexist) {
            System.out.println("No commit with that id exists.");
            return;
        }
    }



    /**Creat new branch, add _head to the branch head.
     * @param args args.
     */
    public void branch(String[] args) {
        String branchName = args[1];

        if (_branchCommits.keySet().contains(branchName)) {
            System.out.println("A branch with that name already exsists.");
            return;
        }

        LinkedList<Commit> branchlist = new LinkedList<>();
        branchlist.add(_head);
        Commit temp = _head;
        while (temp.getParent() != null) {
            temp = temp.getParent().getFirst();
            branchlist.addFirst(temp);
        }
        _branchCommits.put(branchName, branchlist);
    }

    /**
     * Remove branch, should not remove its commits.
     * @param branch branch.
     */
    public void rmbranch(String branch) {
        if (!_branchCommits.keySet().contains(branch)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (_branch.equals(branch)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        _branchCommits.remove(branch);
    }

    /** Reset.
     * @param args id.
     */
    public void reset(String args) {
        Commit commit = null;
        for (String key : _allCommits.keySet()) {
            if (args.equals(key.substring(0, args.length()))) {
                commit = _allCommits.get(key);
                break;
            }
        }
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        for (String s : commit.getBlob().keySet()) {
            if (!_head.getTrackedFiles().contains(s)
                    && new File(s).exists()) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
                return;
            }
        }

        for (String fileName : commit.getTrackedFiles()) {
            String code = commit.getBlob().get(fileName);
            if (code != null) {
                byte[] content = Utils.readContents(
                        Utils.join(BLOB_FOLDER, code));
                Utils.writeContents(new File(fileName), content);
            } else {
                File file = new File(fileName);
                file.delete();
            }
        }

        _head = commit;
        for (String branch : _branchCommits.keySet()) {
            LinkedList<Commit> commitList = _branchCommits.get(branch);
            if (commitList.contains(_head)) {
                _branch = branch;
                break;
            }
        }
    }

    /** Megre.
     * @param args branchname.
     */
    public void merge(String args) {
        mergedcommand = true;
        if (args.equals(_branch)) {
            System.out.println("Cannot merge a branch with itself");
            return;
        } else if (!_staging.isEmpty() || !_remove.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        } else if (!_branchCommits.keySet().contains(args)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }

        Commit splitPoint = new Commit();
        LinkedList<Commit> tbranch = _branchCommits.get(args);
        LinkedList<Commit> mbranch = _branchCommits.get(_branch);
        for (Commit temp : tbranch) {
            if (!mbranch.contains(temp)) {
                int index = tbranch.indexOf(temp);
                if (index > 0) {
                    splitPoint = tbranch.get(index - 1);
                } else {
                    splitPoint = tbranch.get(0);
                }
                break;
            }
        }
        TreeMap<String, String> tFiles =
                _branchCommits.get(args).getLast().getBlob();
        TreeMap<String, String> curFiles =
                _branchCommits.get(_branch).getLast().getBlob();
        TreeMap<String, String> spFiles = splitPoint.getBlob();

        boolean conflict1 = targetcheck(args, tFiles, curFiles, spFiles);
        boolean conflict2  = spplitcheck(tFiles, curFiles, spFiles);
        for (String currFile : curFiles.keySet()) {
            if (!spFiles.containsKey(currFile)
                    && !tFiles.containsKey(currFile)) {
                if (currFile.equals("f.txt")) {
                    new File(currFile).delete();
                }
            }
        }

        if (_staging.size() == 0 && _remove.size() == 0) {
            System.out.println("No changes added to the commit.");
            return;
        }

        commit(new Commit(_branchCommits.get(_branch).getLast(),
                _branchCommits.get(args).getLast(),
                "Merged " + args + " into " + _branch + "."));
        if (conflict1 || conflict2) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /**
     * Check Target.
     *
     * @param branch      branch.
     * @param tgtFiles targetFiles.
     * @param curFiles   currFiles.
     * @param spFiles     spFiles.
     * @return Conflicted.
     */
    private boolean targetcheck(String branch,
                                TreeMap<String, String> tgtFiles,
                                TreeMap<String, String> curFiles,
                                TreeMap<String, String> spFiles) {
        for (String targetFile : tgtFiles.keySet()) {
            if (spFiles.containsKey(targetFile)
                    && !tgtFiles.get(targetFile).
                    equals(spFiles.get(targetFile))
                    && !curFiles.containsKey(targetFile)) {
                add(targetFile, tgtFiles);
            } else if (!spFiles.containsKey(targetFile)
                    && !curFiles.containsKey(targetFile)) {
                _head = _branchCommits.get(branch).getLast();
                checkout(new String[]{"checkout", "--", targetFile});
                _head = _branchCommits.get(_branch).getLast();
                add(targetFile, tgtFiles);
            } else if (!spFiles.containsKey(targetFile)
                    && !curFiles.get(targetFile).
                    equals(tgtFiles.get(targetFile))) {
                String currContent = Utils.readContentsAsString
                        (Utils.join(BLOB_FOLDER, curFiles.get(targetFile)));
                String givenContent = Utils.readContentsAsString
                        (Utils.join(BLOB_FOLDER, tgtFiles.get(targetFile)));
                Utils.writeContents(new File(targetFile),
                        conflictPrint(currContent, givenContent));
                return true;
            }
        }
        return false;
    }

    /**
     * Check Target.
     * @param tgtFiles targetFiles.
     * @param curFiles   currFiles.
     * @param spFiles     spFiles.
     * @return Conflicted.
     */
    private boolean spplitcheck(TreeMap<String, String> tgtFiles,
                            TreeMap<String, String> curFiles,
                            TreeMap<String, String> spFiles) {
        for (String spFile : spFiles.keySet()) {
            if (!tgtFiles.containsKey(spFile)
                    && curFiles.containsKey(spFile)
                    && curFiles.get(spFile).equals(spFiles.get(spFile))) {
                new File(spFile).delete();
            } else if (tgtFiles.containsKey(spFile)
                    && curFiles.containsKey(spFile)
                    && !curFiles.get(spFile).equals(tgtFiles.get(spFile))) {
                String currContent = Utils.readContentsAsString
                        (Utils.join(BLOB_FOLDER, curFiles.get(spFile)));
                String givenContent = Utils.readContentsAsString
                        (Utils.join(BLOB_FOLDER, tgtFiles.get(spFile)));
                Utils.writeContents(new File(spFile),
                        conflictPrint(currContent, givenContent));
                return true;
            } else if (!tgtFiles.containsKey(spFile)
                    && curFiles.containsKey(spFile)
                    && !curFiles.get(spFile).equals(spFiles.get(spFile))) {
                String currContent = Utils.readContentsAsString
                        (Utils.join(BLOB_FOLDER, curFiles.get(spFile)));
                String givenContent = "";
                Utils.writeContents(new File(spFile),
                        conflictPrint(currContent, givenContent));
                return true;
            } else if (tgtFiles.containsKey(spFile)
                    && !curFiles.containsKey(spFile)
                    && !tgtFiles.get(spFile).equals(spFiles.get(spFile))) {
                String currContent = "";
                String givenContent = Utils.readContentsAsString
                        (Utils.join(BLOB_FOLDER, tgtFiles.get(spFile)));
                Utils.writeContents(new File(spFile),
                        conflictPrint(currContent, givenContent));
                return true;
            }
        }
        return false;
    }


    /**
     * Return merge conflict file.
     *
     * @param curr  curr.
     * @param given given.
     * @return String.
     */
    private String conflictPrint(String curr, String given) {
        return "<<<<<<< HEAD\n" + curr + "=======\n" + given + ">>>>>>>\n";
    }
    /**
     * Return head.*/
    Commit gethead() {
        return _head;
    }



    /** Record current branch. */
    private String _branch;
    /** Current Commit. */
    private Commit _head;
    /** All of the commits paired with Branch.*/
    private HashMap<String, LinkedList<Commit>> _branchCommits;
    /** All commits. */
    private HashMap<String, Commit> _allCommits;
    /** File names to be removed. */
    private HashSet<String> _remove;
    /** Staging file names. */
    private HashSet<String> _staging;
    /** Remove tmp. */
    private HashSet<String> _rmTmp;
    /** Remove tmp. */
    private boolean mergedcommand;

}
