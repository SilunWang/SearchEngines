import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

/**
 * This file deals with File IO functions
 * Created by Silun Wang on 15/9/20.
 */
public class FileIO {

    static String filename = "";

    public static void setFilename(String str) {
        filename = str;
    }

    public static void write2File(String str) {
        try {
            // append to file
            FileWriter writer = new FileWriter(filename, true);
            // buffered writer more efficient
            BufferedWriter out = new BufferedWriter(writer);
            out.write(str);
            // remember to close it
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteFile(String str) {
        try {
            File file = new File(str);
            Files.deleteIfExists(file.toPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
