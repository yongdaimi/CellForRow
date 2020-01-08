package com.realsil.android.dongle.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

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

}
