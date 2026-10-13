package br.com.willian.ferias;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Roda em segundo plano na frequência escolhida, mesmo com o app fechado. */
public class BackupWorker extends Worker {

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        if (BackupCore.prefs(ctx).getBoolean("enabled", false)) {
            BackupCore.run(ctx, null);
        }
        return Result.success();
    }
}
