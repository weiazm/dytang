package why;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

/**
 * @title ResultWriter
 * @desc 按行输出内容到指定文件
 * @author weiaz
 * @date 2016年6月7日
 * @version version
 */
public class ResultWriter implements Serializable {

    private String filePath = null;
    private BufferedWriter writer = null;

    public ResultWriter(String filePath) {
        this.setFilePath(filePath);
        try {
            this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8"));
        } catch (UnsupportedEncodingException | FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void write(String str) {
        try {
            writer.write(str);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.out.println("写入输出文件失败！");
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param test
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        ResultWriter rw = new ResultWriter("txt.txt");
        rw.write("fuck");
        rw.write("够");
        rw.close();
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

}
