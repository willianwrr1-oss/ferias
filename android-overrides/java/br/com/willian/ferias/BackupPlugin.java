package br.com.willian.ferias;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.activity.result.ActivityResult;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.concurrent.TimeUnit;

/** Ponte entre a tela Opções (JavaScript) e o backup nativo do Android. */
@CapacitorPlugin(name = "Backup")
public class BackupPlugin extends Plugin {

    private static final String WORK_NAME = "ferias-backup-automatico";
    private static final int URI_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

    private JSObject statusObject() {
        SharedPreferences p = BackupCore.prefs(getContext());
        JSObject o = new JSObject();
        o.put("hasFolder", !p.getString("tree_uri", "").isEmpty());
        o.put("folder", p.getString("folder_name", ""));
        o.put("enabled", p.getBoolean("enabled", false));
        o.put("hours", p.getInt("hours", 12));
        o.put("lastTime", p.getLong("last_time", 0L));
        o.put("lastOk", p.getBoolean("last_ok", true));
        o.put("lastMsg", p.getString("last_msg", ""));
        return o;
    }

    @PluginMethod
    public void status(PluginCall call) {
        call.resolve(statusObject());
    }

    @PluginMethod
    public void pickFolder(PluginCall call) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(URI_FLAGS | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(call, intent, "onFolderPicked");
    }

    @ActivityCallback
    private void onFolderPicked(PluginCall call, ActivityResult result) {
        if (call == null) {
            return;
        }
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) {
            call.reject("cancelled");
            return;
        }
        Uri tree = result.getData().getData();
        try {
            Context ctx = getContext();
            ctx.getContentResolver().takePersistableUriPermission(tree, URI_FLAGS);
            SharedPreferences p = BackupCore.prefs(ctx);
            String old = p.getString("tree_uri", "");
            if (old != null && !old.isEmpty() && !old.equals(tree.toString())) {
                try {
                    ctx.getContentResolver().releasePersistableUriPermission(Uri.parse(old), URI_FLAGS);
                } catch (Exception ignored) {
                    // a pasta antiga pode já ter perdido a permissão
                }
            }
            p.edit()
                .putString("tree_uri", tree.toString())
                .putString("folder_name", BackupCore.folderLabel(tree))
                .apply();
            call.resolve(statusObject());
        } catch (Exception e) {
            call.reject("Não foi possível usar essa pasta: " + e.getMessage());
        }
    }

    @PluginMethod
    public void configure(PluginCall call) {
        boolean enabled = Boolean.TRUE.equals(call.getBoolean("enabled", false));
        Integer h = call.getInt("hours", 12);
        int hours = (h == null || h < 1) ? 12 : h;
        Context ctx = getContext();
        BackupCore.prefs(ctx).edit().putBoolean("enabled", enabled).putInt("hours", hours).apply();
        if (enabled) {
            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(BackupWorker.class, hours, TimeUnit.HOURS).build();
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
        } else {
            WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME);
        }
        call.resolve(statusObject());
    }

    @PluginMethod
    public void syncData(PluginCall call) {
        String json = call.getString("json");
        if (json == null || json.isEmpty()) {
            call.reject("Sem dados para guardar.");
            return;
        }
        try {
            BackupCore.saveData(getContext(), json);
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage() != null ? e.getMessage() : "Falha ao guardar os dados.");
        }
    }

    @PluginMethod
    public void backupNow(PluginCall call) {
        String json = call.getString("json");
        if (json != null && !json.isEmpty()) {
            try {
                BackupCore.saveData(getContext(), json);
            } catch (Exception ignored) {
                // o backup abaixo ainda usa os dados recebidos
            }
        }
        BackupCore.run(getContext(), json);
        call.resolve(statusObject());
    }
}
