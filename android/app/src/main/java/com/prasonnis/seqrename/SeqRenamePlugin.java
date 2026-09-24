package com.prasonnis.seqrename;

import android.app.Activity;
import android.app.RecoverableActionException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import androidx.activity.result.ActivityResult;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.ArrayList;
import java.util.List;

@CapacitorPlugin(name = "SeqRename")
public class SeqRenamePlugin extends Plugin {

    public static final int REQUEST_CODE_WRITE = 9101;

    // Held across the write-permission round trip on Android 11+
    private PluginCall pendingRenameCall;
    private List<Uri> pendingUris;
    private List<String> pendingNames;

    @PluginMethod
    public void pickPhotos(PluginCall call) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(call, intent, "pickPhotosResult");
    }

    @ActivityCallback
    private void pickPhotosResult(PluginCall call, ActivityResult result) {
        if (call == null) return;
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
            call.reject("No photos selected");
            return;
        }
        Intent data = result.getData();
        List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                uris.add(data.getClipData().getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }

        JSArray photos = new JSArray();
        for (Uri uri : uris) {
            try {
                getContext().getContentResolver()
                    .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
                // Some providers don't support persistable permissions; safe to ignore.
            }
            JSObject obj = new JSObject();
            obj.put("uri", uri.toString());
            obj.put("name", queryDisplayName(uri));
            photos.put(obj);
        }
        JSObject ret = new JSObject();
        ret.put("photos", photos);
        call.resolve(ret);
    }

    private String queryDisplayName(Uri uri) {
        String name = "unknown";
        try (Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
            }
        } catch (Exception ignored) {}
        return name;
    }

    // items: [{ uri: string, newName: string }]
    @PluginMethod
    public void renamePhotos(PluginCall call) {
        JSArray items = call.getArray("items");
        if (items == null) {
            call.reject("items array required");
            return;
        }

        List<Uri> uris = new ArrayList<>();
        List<String> names = new ArrayList<>();
        try {
            for (int i = 0; i < items.length(); i++) {
                JSObject o = JSObject.fromJSONObject(items.getJSONObject(i));
                uris.add(Uri.parse(o.getString("uri")));
                names.add(o.getString("newName"));
            }
        } catch (Exception e) {
            call.reject("Invalid items payload", e);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: must get one-time write consent for these URIs first.
            this.pendingRenameCall = call;
            this.pendingUris = uris;
            this.pendingNames = names;
            call.setKeepAlive(true);
            try {
                IntentSender sender = MediaStore.createWriteRequest(
                        getContext().getContentResolver(), uris).getIntentSender();
                getActivity().startIntentSenderForResult(
                        sender, REQUEST_CODE_WRITE, null, 0, 0, 0);
            } catch (Exception e) {
                call.reject("Failed to request write access", e);
                clearPending();
            }
        } else {
            doRename(uris, names, call);
        }
    }

    // Called from MainActivity.onActivityResult once the system write-consent dialog returns.
    public void handleWriteRequestResult(int resultCode) {
        if (pendingRenameCall == null) return;
        if (resultCode == Activity.RESULT_OK) {
            doRename(pendingUris, pendingNames, pendingRenameCall);
        } else {
            pendingRenameCall.reject("Write permission denied by user");
        }
        clearPending();
    }

    private void clearPending() {
        pendingRenameCall = null;
        pendingUris = null;
        pendingNames = null;
    }

    private void doRename(List<Uri> uris, List<String> names, PluginCall call) {
        int success = 0;
        JSArray failed = new JSArray();
        for (int i = 0; i < uris.size(); i++) {
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, names.get(i));
                int rows = getContext().getContentResolver().update(uris.get(i), values, null, null);
                if (rows > 0) {
                    success++;
                } else {
                    failed.put(uris.get(i).toString());
                }
            } catch (Exception e) {
                // On API 29 this can throw RecoverableSecurityException for files this
                // app doesn't own. Caught generically here; see README for the fallback.
                failed.put(uris.get(i).toString());
            }
        }
        JSObject ret = new JSObject();
        ret.put("successCount", success);
        ret.put("failed", failed);
        call.resolve(ret);
    }
}
