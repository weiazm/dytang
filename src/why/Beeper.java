package why;

import java.awt.Frame;
import java.awt.Toolkit;

/**
 * @title Beeper
 * @desc description
 * @author weiaz
 * @date 2016Äê6ÔÂ7ÈÕ
 * @version version
 */
public class Beeper {

    /**
     * @param args
     * @throws InterruptedException
     */
    public static void beep() throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            Frame f = new Frame();
            Toolkit kit = f.getToolkit();
            kit.beep();
            Thread.sleep(1000);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        beep();
    }

}
