package br.com.willian.ferias;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Lógica de backup compartilhada entre o plugin (botão "Fazer backup agora")
 * e o agendamento em segundo plano (BackupWorker).
 */
final class BackupCore {
    static final String PREFS = "ferias_backup";
    static final String DATA_FILE = "ferias-data.json";
    static final String PREFIX = "ferias-backup-";
    static final int KEEP = 10;

    private BackupCore() {}

    static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Guarda a cópia mais recente dos dados do app para o agendamento usar com o app fechado. */
    static void saveData(Context ctx, String json) throws Exception {
        File tmp = new File(ctx.getFilesDir(), DATA_FILE + ".tmp");
        FileOutputStream os = new FileOutputStream(tmp);
        try {
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.getFD().sync();
        } finally {
            os.close();
        }
        File dest = new File(ctx.getFilesDir(), DATA_FILE);
        if (!tmp.renameTo(dest)) {
            if (dest.exists()) {
                dest.delete();
            }
            if (!tmp.renameTo(dest)) {
                throw new Exception("Não foi possível guardar os dados para o backup.");
            }
        }
    }

    static String readData(Context ctx) throws Exception {
        File f = new File(ctx.getFilesDir(), DATA_FILE);
        if (!f.exists()) {
            return null;
        }
        InputStream is = new FileInputStream(f);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            is.close();
        }
    }

    /** Nome legível da pasta escolhida, por exemplo "Armazenamento interno/Documents/Backup Férias". */
    static String folderLabel(Uri tree) {
        String id = DocumentsContract.getTreeDocumentId(tree);
        int i = id.indexOf(':');
        if (i < 0) {
            return id;
        }
        String volume = id.substring(0, i);
        String path = id.substring(i + 1);
        String base = "primary".equals(volume) ? "Armazenamento interno" : "Cartão de memória";
        return path.isEmpty() ? base : base + "/" + path;
    }

    static boolean hasWritePermission(Context ctx, Uri tree) {
        for (UriPermission p : ctx.getContentResolver().getPersistedUriPermissions()) {
            if (p.getUri().equals(tree) && p.isWritePermission()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Grava um arquivo ferias-backup-AAAA-MM-DD_HH-mm-ss.json na pasta escolhida.
     * Usa os dados recebidos ou, se vierem vazios, a última cópia guardada pelo app.
     * O resultado fica nas preferências (última data, sucesso ou erro).
     */
    static synchronized boolean run(Context ctx, String jsonOrNull) {
        SharedPreferences p = prefs(ctx);
        boolean ok = false;
        String msg = "";
        try {
            String json = (jsonOrNull != null && !jsonOrNull.isEmpty()) ? jsonOrNull : readData(ctx);
            if (json == null || json.isEmpty()) {
                throw new Exception("Ainda não há dados guardados. Abra o app e tente de novo.");
            }
            String treeStr = p.getString("tree_uri", "");
            if (treeStr == null || treeStr.isEmpty()) {
                throw new Exception("Nenhuma pasta escolhida.");
            }
            Uri tree = Uri.parse(treeStr);
            if (!hasWritePermission(ctx, tree)) {
                throw new Exception("O Android removeu a permissão da pasta. Escolha a pasta de novo.");
            }
            ContentResolver cr = ctx.getContentResolver();
            Uri parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
            String name = PREFIX + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date()) + ".json";
            Uri doc = DocumentsContract.createDocument(cr, parent, "application/json", name);
            if (doc == null) {
                throw new Exception("Não foi possível criar o arquivo na pasta.");
            }
            OutputStream os = cr.openOutputStream(doc, "w");
            if (os == null) {
                throw new Exception("Não foi possível gravar na pasta.");
            }
            try {
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.flush();
            } finally {
                os.close();
            }
            trim(cr, tree);
            ok = true;
        } catch (Exception e) {
            msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
        p.edit()
            .putLong("last_time", System.currentTimeMillis())
            .putBoolean("last_ok", ok)
            .putString("last_msg", msg)
            .apply();
        return ok;
    }

    /** Mantém só as KEEP cópias mais recentes (as mais antigas são apagadas). */
    private static void trim(ContentResolver cr, Uri tree) {
        try {
            String treeId = DocumentsContract.getTreeDocumentId(tree);
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, treeId);
            List<String[]> files = new ArrayList<String[]>();
            Cursor c = cr.query(
                children,
                new String[] {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                },
                null,
                null,
                null
            );
            if (c == null) {
                return;
            }
            try {
                while (c.moveToNext()) {
                    String id = c.getString(0);
                    String n = c.getString(1);
                    if (id != null && n != null && n.startsWith(PREFIX) && n.endsWith(".json")) {
                        files.add(new String[] { id, n });
                    }
                }
            } finally {
                c.close();
            }
            Collections.sort(
                files,
                new Comparator<String[]>() {
                    @Override
                    public int compare(String[] a, String[] b) {
                        return b[1].compareTo(a[1]);
                    }
                }
            );
            for (int i = KEEP; i < files.size(); i++) {
                try {
                    DocumentsContract.deleteDocument(cr, DocumentsContract.buildDocumentUriUsingTree(tree, files.get(i)[0]));
                } catch (Exception ignored) {
                    // se não der para apagar uma cópia antiga, segue com as outras
                }
            }
        } catch (Exception ignored) {
            // limpeza é opcional: a cópia nova já foi gravada
        }
    }
}
