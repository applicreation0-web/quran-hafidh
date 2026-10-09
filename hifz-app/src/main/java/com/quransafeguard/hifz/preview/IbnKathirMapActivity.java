package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.quransafeguard.hifz.core.VerseRef;
import java.util.ArrayList;
import java.util.List;

/**
 * Documentary Ibn Kathir map. Source-verified numeric group boundaries only.
 * No generated exegesis, legacy segmentation, progress writes or network calls.
 * The source has no rights-cleared collection of printed English titles yet.
 */
public final class IbnKathirMapActivity extends Activity {
    static final String EXTRA_SURAH = "mapSurah";
    static final String EXTRA_AYAH = "mapAyah";
    private IbnKathirGroupIndex index;
    private int surah = 1;
    private int highlightedAyah = 1;
    private ListView groupsList;
    private TextView header;
    private int scrollIndex;
    private int scrollTop;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        try {
            index = IbnKathirGroupIndex.shared();
        } catch (RuntimeException error) {
            Ui.showFatal(this, "L'index Ibn Kathīr vérifié est indisponible.");
            return;
        }
        surah = saved != null ? saved.getInt("surah",1) : getIntent().getIntExtra(EXTRA_SURAH,1);
        highlightedAyah = saved != null ? saved.getInt("ayah",1) : getIntent().getIntExtra(EXTRA_AYAH,1);
        scrollIndex = saved == null ? -1 : saved.getInt("firstVisible",-1);
        scrollTop = saved == null ? 0 : saved.getInt("topOffset",0);
        surah = Math.max(1,Math.min(114,surah));

        LinearLayout root = Ui.column(this);
        root.setPadding(Ui.dp(this,10),Ui.dp(this,4),Ui.dp(this,10),Ui.dp(this,4));
        LinearLayout top = Ui.row(this);
        Button back = Ui.iconButton(this,"","Retour",v -> finish());
        top.addView(back);
        header = Ui.bookText(this,"",16,true);
        header.setGravity(Gravity.CENTER);
        top.addView(header,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f));
        Button picker = Ui.iconButton(this,"","Choisir une sourate",v -> chooseSurah());
        top.addView(picker);
        Button info = Ui.iconButton(this,"","Référence",v -> showSource());
        top.addView(info);
        root.addView(top);
        root.addView(Ui.divider(this));
        groupsList = new ListView(this);
        groupsList.setCacheColorHint(Ui.PAPER);
        groupsList.setBackgroundColor(Ui.PAPER);
        groupsList.setDividerHeight(Math.max(1,Ui.dp(this,1)));
        groupsList.setFastScrollEnabled(true);
        root.addView(groupsList,new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,0,1f));
        setContentView(root);
        Ui.respectSystemBars(this,root,0,0,0,0);
        showSurah();
    }

    private void chooseSurah() {
        String[] names = new String[114];
        for (int i=0;i<114;i++) names[i] = QuranSurahNames.labelFor(i+1);
        new AlertDialog.Builder(this).setTitle("Sourate")
            .setItems(names,(dialog,which) -> {
                surah = which+1;
                highlightedAyah = 1;
                scrollIndex = 0;
                scrollTop = 0;
                showSurah();
            }).show();
    }

    private void showSurah() {
        header.setText(QuranSurahNames.labelFor(surah));
        List<IbnKathirGroupIndex.Group> groups =
            new ArrayList<>(index.groupsForSurah(surah));
        ArrayAdapter<IbnKathirGroupIndex.Group> adapter =
            new ArrayAdapter<IbnKathirGroupIndex.Group>(
                this,android.R.layout.simple_list_item_1,groups) {
            @Override public View getView(int position,View convert,ViewGroup parent) {
                TextView row=(TextView) super.getView(position,convert,parent);
                IbnKathirGroupIndex.Group group=getItem(position);
                row.setText(QuranSurahNames.range(
                    new VerseRef(group.surah,group.startAyah),
                    new VerseRef(group.surah,group.endAyah)));
                row.setTextSize(17f);
                row.setTextColor(Ui.INK);
                row.setTypeface(Typeface.SERIF);
                row.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
                row.setPadding(Ui.dp(IbnKathirMapActivity.this,18),Ui.dp(IbnKathirMapActivity.this,12),
                    Ui.dp(IbnKathirMapActivity.this,18),Ui.dp(IbnKathirMapActivity.this,12));
                row.setMinHeight(Ui.dp(IbnKathirMapActivity.this,56));
                row.setBackgroundColor(Ui.PAPER);
                row.setContentDescription("Ibn Kathīr, versets "+group.navigationRange()+" : ouvrir dans le Mushaf");
                return row;
            }
        };
        groupsList.setAdapter(adapter);
        groupsList.setOnItemClickListener((parent,view,position,id) ->
            openMushaf(groups.get(position)));
        int initial=0;
        if(scrollIndex>=0) initial=Math.min(groups.size()-1,scrollIndex);
        else for (int i=0;i<groups.size();i++) {
            IbnKathirGroupIndex.Group g=groups.get(i);
            if(highlightedAyah>=g.startAyah&&highlightedAyah<=g.endAyah){ initial=i;break; }
        }
        final int target=initial,offset=scrollTop;
        groupsList.post(() -> groupsList.setSelectionFromTop(target,offset));
    }

    private void openMushaf(IbnKathirGroupIndex.Group group) {
        try {
            VerseRef begin = new VerseRef(group.surah,group.startAyah);
            int page = GeometryRepository.get(this).pageForVerse(begin);
            // Open a temporary reader: no Hifz cursor or persisted Lecture bookmark changes.
            Intent intent = new Intent(this,StudyReaderActivity.class);
            intent.putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,page);
            intent.putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE,begin.toString());
            intent.putExtra(StudyReaderActivity.EXTRA_MAP_PREVIEW,true);
            startActivity(intent);
        } catch (RuntimeException invalidGeometry) {
            Toast.makeText(this,"Le verset demandé est absent du Mushaf.",Toast.LENGTH_LONG).show();
        }
    }

    private void showSource() {
        new AlertDialog.Builder(this)
            .setTitle("Ibn Kathīr · source")
            .setMessage("Index documentaire : 1 903 groupes de commentaires numériques identiques couvrant les 114 sourates. "
                +"Source : Darussalam, édition anglaise abrégée, corpus spa5k/tafsir_api. "
                +"Révision : "+IbnKathirGroupIndex.SOURCE_COMMIT+". "
                +"Titres et préambules anglais non publiés : authentification et droits non établis. "
                +"La Carte ne produit aucun commentaire religieux.")
            .setPositiveButton("Fermer",(d,w)->d.dismiss()).show();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        if(groupsList!=null) {
            out.putInt("firstVisible",groupsList.getFirstVisiblePosition());
            View first=groupsList.getChildAt(0);
            out.putInt("topOffset",first==null?0:first.getTop());
        }
        out.putInt("surah",surah);
        out.putInt("ayah",highlightedAyah);
        super.onSaveInstanceState(out);
    }
}
