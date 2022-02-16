package gitlet;

import java.io.File;
import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Xidong Wu
 */
public class Main {

    /** Current Working Directory. */
    static final File CWD = new File(".");

    /** Main metadata folder. */
    static final File GIT_FOLDER = new File(".gitlet");
    /** Main folder. */
    private static File dir = Utils.join(GIT_FOLDER, "gitlet");


    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String[] args) throws IOException {
        Command repo = dir.exists() ? Utils.readObject(dir, Command.class)
                : new Command();
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        switch (args[0]) {
        case "init":
            if (args.length > 1) {
                System.out.println("Incorrect operands."); System.exit(0);
            } else {
                repo.init();
            }
            break;
        case "add":
            repo.add(args[1]);
            break;
        case "commit":
            repo.commit(args); break;
        case "rm":
            repo.remove(args[1]);
            break;
        case "log":
            repo.log();
            break;
        case "global-log":
            repo.globalLog();
            break;
        case "find":
            repo.find(args[1]);
            break;
        case "status":
            if (repo.gethead() == null) {
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            } else {
                repo.status();
            }
            break;
        case "checkout":
            repo.checkout(args);
            break;
        case "branch":
            repo.branch(args);
            break;
        case "rm-branch":
            repo.rmbranch(args[1]);
            break;
        case "reset":
            repo.reset(args[1]);
            break;
        case "merge":
            repo.merge(args[1]); break;
        default:
            System.out.println("No command with that name exists.");
            break;
        }
        Utils.writeObject(dir, repo);
    }
}

