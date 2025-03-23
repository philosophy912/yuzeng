package com.philosophy.translate;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FileSelectorHelper {
    private static final String TAG = "FileSelectorHelper";
    private final AppCompatActivity activity;
    private final ActivityResultLauncher<Intent> filePickerLauncher;

    private File selectFile;
    public File getSelectFile() {
        return selectFile;
    }

    public FileSelectorHelper(AppCompatActivity activity, final TextView selectedFileNameTextView) {
        this.activity = activity;
        this.filePickerLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri selectedFileUri = result.getData().getData();
                            if (selectedFileUri != null) {
                                if (isValidFileType(selectedFileUri) && isValidFileSize(selectedFileUri)) {
                                    selectFile = copyFileToAppStorage(selectedFileUri);
                                    String fileName = getFileName(selectedFileUri);
                                    selectedFileNameTextView.setText(fileName);
                                    if (selectFile != null) {
                                        Log.d(TAG, "File selected: " + selectFile.getAbsolutePath());
                                    }

                                } else {
                                    Toast.makeText(activity, "Invalid file type or size", Toast.LENGTH_SHORT).show();
                                    Log.w(TAG, "Invalid file type or size");
                                }
                            }
                        }
                    }
                }
        );
    }

    public void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain", "text/markdown"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        filePickerLauncher.launch(Intent.createChooser(intent, "Select a File"));
    }

    private boolean isValidFileType(Uri uri) {
        String mimeType = activity.getContentResolver().getType(uri);
        return mimeType != null && (mimeType.equals("application/msword") ||
                mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                mimeType.equals("text/plain") ||
                mimeType.equals("text/markdown"));
    }

    private boolean isValidFileSize(Uri uri) {
        try (android.content.res.AssetFileDescriptor assetFileDescriptor = activity.getContentResolver().openAssetFileDescriptor(uri, "r")) {
            if (assetFileDescriptor != null) {
                long fileSize = assetFileDescriptor.getLength();
                return fileSize <= 100 * 1024 * 1024; // 100 MB
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking file size", e);
        }
        return false;
    }

    private File copyFileToAppStorage(Uri uri) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        File file = null;
        try {
            inputStream = activity.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "InputStream is null");
                return null;
            }
            String fileName = getFileName(uri);
            file = new File(activity.getFilesDir(), fileName);
            outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            Log.d(TAG, "File copied to app storage: " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error copying file to app storage", e);
            file = null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
        return file;
    }

    private String getFileName(Uri uri) {
        String fileName = null;
        String scheme = uri.getScheme();
        if (scheme != null && scheme.equals("content")) {
            try (Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }
        return fileName;
    }

}
