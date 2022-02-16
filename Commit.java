package gitlet;


import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeMap;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Commit Class.
 * @author Yuqiong Ji.
 */

public class Commit implements Serializable {

    /** Default Constructor. */
    public Commit() {
        _msg = "initial commit";
        _time = new SimpleDateFormat("EEE MMM dd"
                + " HH:mm:ss yyyy Z").format(new Date(0));
        _parent = null;
        _Blob = new TreeMap<>();
        _trackedFiles = new HashSet<>();
        _index = hashcode();
    }

    /** Constructor.
     * @param parent commit.
     * @param msg message.
     */
    @SuppressWarnings("unchecked")
    public Commit(Commit parent, String msg) {
        _msg = msg;
        _time = new SimpleDateFormat("EEE MMM dd"
                + " HH:mm:ss yyyy Z").format(new Date());
        _parent = new LinkedList<Commit>();
        _parent.add(parent);
        _Blob = (TreeMap<String, String>) parent.getBlob().clone();
        _trackedFiles = (HashSet<String>) parent.getTrackedFiles().clone();
        _index = hashcode();
    }

    /** Constructor.
     * @param parent1 commit.
     * @param parent2 commit.
     * @param msg String.
     */
    @SuppressWarnings("unchecked")
    public Commit(Commit parent1, Commit parent2, String msg) {
        _msg = msg;
        _time = new SimpleDateFormat("EEE MMM dd "
                + "HH:mm:ss yyyy Z").format(new Date());
        _parent = new LinkedList<Commit>();
        _parent.add(parent1);
        _parent.add(parent2);
        _Blob = (TreeMap<String, String>) parent1.getBlob().clone();
        for (String iter: parent2.getBlob().keySet()) {
            _Blob.put(iter, parent2.getBlob().get(iter));
        }
        _trackedFiles = (HashSet<String>) parent1.getTrackedFiles().clone();
        for (String iter : parent2.getTrackedFiles()) {
            _trackedFiles.add(iter);
        }
        _index = hashcode();
    }

    /** Set id. */
    public void setindex() {
        _index = hashcode();
    }

    /** get index.
     * @return _index*/
    public String getindex() {
        return _index;
    }
    /** get message.
     *  @return _msg*/
    public String getMsg() {
        return _msg;
    }
    /** get time.
     *  @return _time*/
    public String getTime() {
        return _time;
    }
    /** get parent commit.
     * @return _parent*/
    public LinkedList<Commit> getParent() {
        return _parent;
    }
    /** get blob.
     * @return _Blob*/
    public TreeMap<String, String> getBlob() {
        return _Blob;
    }
    /** get tracked fileds.
     * @return _trackedFiles*/
    public HashSet<String> getTrackedFiles() {
        return _trackedFiles;
    }

    /** get hashcode.
     * @return String*/
    public String hashcode() {
        String item = _time + _msg + _parent;
        if (_Blob != null) {
            for (String iter : _Blob.keySet()) {
                File temp = Utils.join(Command.BLOB_FOLDER, _Blob.get(iter));
                String result = Utils.readContentsAsString(temp);
                item += result;
            }
        }
        return Utils.sha1(item);
    }


    /** SHA-1 strings.  */
    private String _index;
    /** Commit message. */
    private String _msg;
    /** Commit time.  */
    private String _time;
    /** Commit Parents. */
    private LinkedList<Commit> _parent;
    /** Tracked Files.  */
    private HashSet<String> _trackedFiles;
    /** File name and blob ID. */
    private TreeMap<String, String> _Blob;


}
