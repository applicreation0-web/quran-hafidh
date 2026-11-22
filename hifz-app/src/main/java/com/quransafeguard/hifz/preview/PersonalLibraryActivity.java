package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Personal, read-only reference library for BOOX.
 *
 * No copyrighted book text, login secret or private reading URL is packaged, downloaded,
 * cached or copied into Quran Haafidh. An external browser owns online authentication.
 * Local documents are selected by the user through Android's Storage Access Framework
 * and remain on their chosen storage provider (no file-byte copy into the app).
 *
 * Intentionally independent of all Hifz progress, receipts, schedules and backup formats.
 */
public final class PersonalLibraryActivity extends Activity {
    private static final int REQUEST_REFERENCE_DOCUMENT = 4816;
    private static final String PREF_NAME = "personal_reference_library_v1";
    private static final String KEY_URI = "authorized_document_uri";
    private static final String ONLINE_LIBRARY = "https://ebooks.albayyinah.fr/library";

    private TextView selectedDocument;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.PAPER);
        LinearLayout column = Ui.column(this);
        scroll.addView(column);

        LinearLayout top = Ui.row(this);
        TextView back = Ui.text(this, "‹  Retour", 17f, false);
        back.setPadding(Ui.dp(this, 6), Ui.dp(this, 10),
            Ui.dp(this, 16), Ui.dp(this, 10));
        back.setOnClickListener(v -> finish());
        back.setClickable(true);
        top.addView(back);
        column.addView(top);
        column.addView(Ui.divider(this));

        TextView title = Ui.bookText(this, "Bibliothèque personnelle", 22f, true);
        title.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 8));
        column.addView(title);

        TextView note = Ui.text(this,
            "Références personnelles uniquement. Cette bibliothèque n'ajoute aucun reçu Hifz "
                + "et ne modifie ni les séances ni la progression.", 16f, false);
        note.setTextColor(Ui.MUTED);
        column.addView(note);
        column.addView(Ui.divider(this));

        TextView bookTitle = Ui.bookText(this,
            "Mille et une significations coraniques", 19f, true);
        bookTitle.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 3));
        column.addView(bookTitle);
        column.addView(Ui.text(this,
            "Michel Issa Petit · Éditions Al Bayyinah · ISBN 9782385551759",
            16f, false));
        TextView access = Ui.text(this,
            "Ouvrir le livre acheté dans le navigateur : connexion à votre propre compte "
                + "Al Bayyinah requise. Lecture en ligne, selon les droits de l'éditeur.",
            15f, false);
        access.setTextColor(Ui.MUTED);
        access.setPadding(0, Ui.dp(this, 7), 0, Ui.dp(this, 7));
        column.addView(access);
        column.addView(Ui.settingRow(this, "Ouvrir Al Bayyinah",
            "Bibliothèque en ligne · navigateur externe",
            v -> openOnlineLibrary()));
        column.addView(Ui.divider(this));

        TextView localTitle = Ui.bookText(this,
            "Document personnel hors connexion", 18f, true);
        localTitle.setPadding(0, Ui.dp(this, 20), 0, Ui.dp(this, 8));
        column.addView(localTitle);
        column.addView(Ui.text(this,
            "Importez uniquement un fichier PDF/EPUB que vous êtes autorisé à conserver. "
                + "Le fichier reste sur l'appareil, sans copie dans l'APK.",
            15f, false));
        selectedDocument = Ui.menuSubline(this, "");
        selectedDocument.setPadding(0, Ui.dp(this, 9), 0, Ui.dp(this, 9));
        selectedDocument.setMaxLines(3);
        column.addView(selectedDocument);
        column.addView(Ui.settingRow(this, "Choisir un PDF / EPUB",
            "Sélectionner un document déjà disponible sur BOOX",
            v -> chooseDocument()));
        column.addView(Ui.divider(this));
        column.addView(Ui.settingRow(this, "Lire le document local",
            "Ouvrir avec une application compatible, par exemple NeoReader",
            v -> openLocalDocument()));
        column.addView(Ui.divider(this));
        column.addView(Ui.settingRow(this, "Retirer la référence locale",
            "Ne supprime pas le fichier original",
            v -> clearDocument()));

        Ui.respectSystemBars(this, scroll, 0, 0, 0, 0);
        setContentView(scroll);
        refreshSelection();
    }

    private SharedPreferences store() {
        return getSharedPreferences(PREF_NAME, MODE_PRIVATE);
    }

    private Uri chosenUri() {
        String value = store().getString(KEY_URI, null);
        if (value == null || value.isEmpty()) return null;
        try { return Uri.parse(value); } catch (Exception ignored) { return null; }
    }

    private void refreshSelection() {
        Uri uri = chosenUri();
        if (uri == null) {
            selectedDocument.setText("Aucun document local sélectionné.");
            return;
        }
        String displayName = "Document local sélectionné";
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0))
                displayName = cursor.getString(0);
        } catch (Exception ignored) {
            // Revoked storage permissions are handled when opening the document.
        }
        selectedDocument.setText(displayName);
    }

    private void chooseDocument() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,
            new String[]{"application/pdf", "application/epub+zip"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_REFERENCE_DOCUMENT);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "Sélecteur de documents indisponible sur cet appareil.",
                Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode, resultCode, result);
        if (requestCode != REQUEST_REFERENCE_DOCUMENT || resultCode != RESULT_OK
                || result == null || result.getData() == null) return;
        Uri uri = result.getData();
        try {
            getContentResolver().takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException error) {
            Toast.makeText(this, "Accès durable refusé par le fournisseur de fichiers.",
                Toast.LENGTH_LONG).show();
            return;
        }
        store().edit().putString(KEY_URI, uri.toString()).apply();
        refreshSelection();
    }

    private void openLocalDocument() {
        Uri uri = chosenUri();
        if (uri == null) {
            Toast.makeText(this, "Sélectionnez d'abord un PDF ou EPUB autorisé.",
                Toast.LENGTH_SHORT).show();
            return;
        }
        String type;
        try { type = getContentResolver().getType(uri); }
        catch (Exception error) { type = null; }
        if (type == null || type.isEmpty() || type.equals("application/octet-stream"))
            type = "*/*";
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setDataAndType(uri, type);
        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(view, "Lire avec"));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "Aucun lecteur compatible. Installez ou activez NeoReader.",
                Toast.LENGTH_LONG).show();
        } catch (SecurityException error) {
            Toast.makeText(this, "Accès au fichier expiré : sélectionnez-le à nouveau.",
                Toast.LENGTH_LONG).show();
        }
    }

    private void openOnlineLibrary() {
        Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse(ONLINE_LIBRARY));
        browse.addCategory(Intent.CATEGORY_BROWSABLE);
        try {
            startActivity(browse);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "Aucun navigateur disponible sur cet appareil.",
                Toast.LENGTH_LONG).show();
        }
    }

    private void clearDocument() {
        Uri uri = chosenUri();
        store().edit().remove(KEY_URI).apply();
        if (uri != null) {
            try {
                getContentResolver().releasePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) { }
        }
        refreshSelection();
    }
}
