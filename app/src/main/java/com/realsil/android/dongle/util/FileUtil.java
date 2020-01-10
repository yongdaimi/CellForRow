package com.realsil.android.dongle.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;

/**
 * @author xp.chen
 */
public class FileUtil {

    private static final String FILE_SCHEME_FILE    = "file";
    private static final String FILE_SCHEME_CONTENT = "content";

    private static final String FILE_DB_COLUMN_FIELD_DATA = "_data";

    /**
     * Return the true path of a file based on its uri.
     *
     * @param context Application context
     * @param uri     File uri
     * @return The true path of a file. Note: The return value may be empty.
     */
    public static String getFilePath(Context context, Uri uri) {
        if (context == null || uri == null) return null;
        if (FILE_SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {FILE_DB_COLUMN_FIELD_DATA};
            try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor == null) return null;
                int data_column_index = cursor.getColumnIndexOrThrow(FILE_DB_COLUMN_FIELD_DATA);
                if (cursor.moveToFirst()) {
                    return cursor.getString(data_column_index);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (FILE_SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }


    /**
     * This method is used to read the content of a binary file, it will return
     * a byte array to hold the contents of the file.
     * <p>
     * Note: It can not be used to read large files
     * </p>
     *
     * @param filePath Path to the binary file
     * @return The contents of the binary file specifying the path. The returned
     * byte array may be empty if the file at the specified path does
     * not exist.
     */
    public static byte[] readBinaryFileContent(String filePath) {
        File binaryFile = new File(filePath);
        if (!binaryFile.exists())
            return null;

        long fileLength = binaryFile.length();
        if (fileLength > Integer.MAX_VALUE)
            return null;

        try {
            byte[] fileContent = new byte[(int) fileLength];
            FileInputStream fos = new FileInputStream(binaryFile);
            byte[] buffer = new byte[1024];
            int len = 0, pos = 0;
            while ((len = fos.read(buffer)) != -1) {
                System.arraycopy(buffer, 0, fileContent, pos, len);
                pos += len;
            }
            fos.close();
            return fileContent;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
