package cn.archeo.rihen;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class ShareFileProvider extends ContentProvider {
    private static final String SHARED_CARD_DIRECTORY = "shared_cards";
    private static final String[] DEFAULT_PROJECTION = {
            OpenableColumns.DISPLAY_NAME,
            OpenableColumns.SIZE
    };

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        resolveReadableFile(uri);
        return "image/png";
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {
        File file = resolveReadableFile(uri);
        String[] columns = projection == null ? DEFAULT_PROJECTION : projection;
        MatrixCursor cursor = new MatrixCursor(columns, 1);
        Object[] row = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) {
                row[i] = file.getName();
            } else if (OpenableColumns.SIZE.equals(columns[i])) {
                row[i] = file.length();
            }
        }
        cursor.addRow(row);
        return cursor;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new FileNotFoundException("Shared cards are read-only");
        }
        File file = resolveReadableFile(uri);
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        ParcelFileDescriptor descriptor = openFile(uri, mode);
        return new AssetFileDescriptor(descriptor, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Read-only provider");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read-only provider");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read-only provider");
    }

    private File resolveReadableFile(Uri uri) {
        List<String> segments = uri.getPathSegments();
        if (segments.size() != 2 || !SHARED_CARD_DIRECTORY.equals(segments.get(0))) {
            throw new SecurityException("Invalid shared card path");
        }

        String fileName = segments.get(1);
        if (!fileName.endsWith(".png") || fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0) {
            throw new SecurityException("Invalid shared card name");
        }

        File directory = new File(getContext().getCacheDir(), SHARED_CARD_DIRECTORY);
        File file = new File(directory, fileName);
        try {
            String directoryPath = directory.getCanonicalPath();
            String filePath = file.getCanonicalPath();
            if (!filePath.startsWith(directoryPath + File.separator) || !file.isFile()) {
                throw new SecurityException("Shared card is unavailable");
            }
            return file;
        } catch (IOException exception) {
            throw new SecurityException("Unable to resolve shared card", exception);
        }
    }
}
