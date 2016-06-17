package why;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * @title SerializeUtil
 * @desc description
 * @author weiaz
 * @date 2016年6月17日
 * @version version
 */
public class SerializeUtil {

    public static void serializeToFile(Object ob, String fileName) throws FileNotFoundException, IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName));// 输出流保存的文件名为 my.out // //
                                                                                        // ；ObjectOutputStream能把Object输出成Byte流
        oos.writeObject(ob);
        oos.flush(); // 缓冲流
        oos.close(); // 关闭流
    }

    public static Object unserializeFromFile(String fileName)
        throws FileNotFoundException, IOException, ClassNotFoundException {
        ObjectInputStream oin = new ObjectInputStream(new FileInputStream(fileName));
        Object mts = oin.readObject();// 由Object对象向下转型为MyTest对象
        return mts;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
        ArrayList<Integer> list = new ArrayList<Integer>();
        list.add(1);
        serializeToFile(list, "out.out");
        ArrayList<Integer> list2 = null;
        list2 = (ArrayList<Integer>) unserializeFromFile("out.out");
        System.out.println(list2);
    }

}
