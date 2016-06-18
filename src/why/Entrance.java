package why;

import org.python.core.Py;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

/**
 * @title Entrance
 * @desc run all the things
 * @author weiaz
 * @date 2016Äê6ÔÂ18ÈÕ
 * @version version
 */
public class Entrance {

    /**
     * @param args
     */
    public static void main(String[] args) {
        PySystemState sys = Py.getSystemState();
        sys.path.add("D:/workSpace/PycharmSpace/crawler/assemble");

        PythonInterpreter pythonInterpreter = new PythonInterpreter();
        pythonInterpreter.execfile("D:/workSpace/PycharmSpace/crawler/assemble/RunAfterEmotion.py");

    }

}
