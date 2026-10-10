package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.net.Uri;
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
    private TextView previewCaption;
    private MushafView pagePreview;
    private GeometryRepository geometry;
    private IbnKathirGroupIndex.Group previewGroup;
    private int previewPage;
    private int selectedGroupIndex=-1;
    private boolean previewReady;
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
        selectedGroupIndex=saved==null?-1:saved.getInt("selectedGroupIndex",-1);
        previewPage=saved==null?0:saved.getInt("previewPage",0);
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
        Button info = Ui.iconButton(this,"","Référence",v -> showBlockDetails());
        top.addView(info);
        root.addView(top);
        root.addView(Ui.divider(this));
        TextView guidance=Ui.text(this,
            "Choisir un bloc · glisser ou toucher le numéro de page · toucher la miniature pour lire",11f,false);
        guidance.setTextColor(Ui.MUTED);
        guidance.setGravity(Gravity.CENTER);
        guidance.setPadding(Ui.dp(this,4),Ui.dp(this,5),Ui.dp(this,4),Ui.dp(this,5));
        root.addView(guidance);
        groupsList = new ListView(this);
        groupsList.setCacheColorHint(Ui.PAPER);
        groupsList.setBackgroundColor(Ui.PAPER);
        groupsList.setDividerHeight(Math.max(1,Ui.dp(this,1)));
        groupsList.setFastScrollEnabled(true);
        // One true KFQC renderer, instead of a costly WebView for every map card.
        LinearLayout body=Ui.row(this);
        boolean spacious=getResources().getConfiguration().smallestScreenWidthDp>=600;
        if(!spacious)body.setOrientation(LinearLayout.VERTICAL);
        body.addView(groupsList,spacious
            ? new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,0.47f)
            : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,0.48f));
        LinearLayout miniature=Ui.column(this);
        miniature.setPadding(Ui.dp(this,3),Ui.dp(this,4),Ui.dp(this,3),Ui.dp(this,3));
        previewCaption=Ui.bookText(this,"",13,false);
        previewCaption.setGravity(Gravity.CENTER);
        miniature.addView(previewCaption,new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        // BOOX-friendly alternative to swipe, with no new toolbar pictogram.
        previewCaption.setOnClickListener(v -> advancePreviewFromCaption());
        pagePreview=new MushafView(this);
        pagePreview.setContentDescription("Miniature authentique du Mushaf · toucher pour lire");
        miniature.addView(pagePreview,new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,0,1f));
        body.addView(miniature,spacious
            ? new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,0.53f)
            : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,0.52f));
        root.addView(body,new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,0,1f));
        setContentView(root);
        Ui.respectSystemBars(this,root,0,0,0,0);
        pagePreview.setListener(new MushafView.Listener() {
            @Override public void onReady() {
                previewReady=true;
                if(previewGroup!=null)renderPreview();
            }
            @Override public void onError(String message) {
                previewCaption.setText("Miniature indisponible · ouvrir depuis la liste");
            }
            @Override public void onPageShown(int page) {}
            @Override public void onVerseTap(VerseRef verse) {openPreviewInMushaf(verse);}
            @Override public void onSurfaceTap() {openPreviewInMushaf();}
            @Override public void onPageSwipe(int delta) {turnPreviewPage(delta);}
        });
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
                selectedGroupIndex = -1;
                previewPage = 0;
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
                row.setText((position+1)+" / "+groups.size()+"   ·   "+QuranSurahNames.range(
                    new VerseRef(group.surah,group.startAyah),
                    new VerseRef(group.surah,group.endAyah)));
                row.setTextSize(17f);
                row.setTextColor(Ui.INK);
                row.setTypeface(Typeface.SERIF,
                    position==selectedGroupIndex?Typeface.BOLD:Typeface.NORMAL);
                row.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
                row.setPadding(Ui.dp(IbnKathirMapActivity.this,18),Ui.dp(IbnKathirMapActivity.this,12),
                    Ui.dp(IbnKathirMapActivity.this,18),Ui.dp(IbnKathirMapActivity.this,12));
                row.setMinHeight(Ui.dp(IbnKathirMapActivity.this,56));
                row.setBackgroundColor(position==selectedGroupIndex?Ui.SURFACE:Ui.PAPER);
                row.setContentDescription("Ibn Kathīr, versets "+group.navigationRange()
                    +" : sélectionner la miniature");
                return row;
            }
        };
        groupsList.setAdapter(adapter);
        groupsList.setOnItemClickListener((parent,view,position,id) -> {
            selectedGroupIndex=position;
            setPreviewGroup(groups.get(position),0);
            // Tap a group to inspect its verified Mushaf page, not to launch a new reader.
            // The miniature itself is the only "open full Mushaf" touch target.
            ((ArrayAdapter<?>)groupsList.getAdapter()).notifyDataSetChanged();
        });
        groupsList.setOnItemLongClickListener((parent,view,position,id) -> {
            selectedGroupIndex=position;
            setPreviewGroup(groups.get(position),0);
            ((ArrayAdapter<?>)groupsList.getAdapter()).notifyDataSetChanged();
            showBlockDetails();
            return true;
        });
        int initial=0;
        if(scrollIndex>=0) initial=Math.min(groups.size()-1,scrollIndex);
        else for (int i=0;i<groups.size();i++) {
            IbnKathirGroupIndex.Group g=groups.get(i);
            if(highlightedAyah>=g.startAyah&&highlightedAyah<=g.endAyah){ initial=i;break; }
        }
        final int target=initial,offset=scrollTop;
        if(selectedGroupIndex<0 || selectedGroupIndex>=groups.size())selectedGroupIndex=initial;
        setPreviewGroup(groups.get(selectedGroupIndex),previewPage);
        groupsList.post(() -> groupsList.setSelectionFromTop(target,offset));
    }

    private void setPreviewGroup(IbnKathirGroupIndex.Group group,int preferredPage) {
        if(group==null)return;
        previewGroup=group;
        try {
            if(geometry==null)geometry=GeometryRepository.get(this);
            int first=geometry.pageForVerse(new VerseRef(group.surah,group.startAyah));
            int last=geometry.pageForVerse(new VerseRef(group.surah,group.endAyah));
            previewPage=preferredPage>=first&&preferredPage<=last?preferredPage:first;
            renderPreview();
        } catch(RuntimeException error) {
            previewCaption.setText(group.navigationRange()+" · aperçu indisponible");
        }
    }

    private void renderPreview() {
        if(!previewReady||previewGroup==null||geometry==null)return;
        int first=geometry.pageForVerse(new VerseRef(previewGroup.surah,previewGroup.startAyah));
        int last=geometry.pageForVerse(new VerseRef(previewGroup.surah,previewGroup.endAyah));
        int count=index.groupsForSurah(surah).size();
        int ordinal=selectedGroupIndex+1;
        previewCaption.setText("Bloc "+ordinal+"/"+count+" · "+previewGroup.navigationRange()
            +" · p. "+previewPage+(first==last?"":" ("+(previewPage-first+1)+"/"+(last-first+1)+")"));
        previewCaption.setContentDescription("Ibn Kathīr, bloc "+ordinal+" sur "+count
            +", "+previewGroup.navigationRange()+", page "+previewPage
            +(first==last ? ". Toucher pour la référence du bloc."
                : ", page "+(previewPage-first+1)+" sur "+(last-first+1)
                    +". Toucher pour "+(previewPage==last
                        ? "revenir à la première page." : "voir la page suivante.")));
        List<VerseRef> exact=new ArrayList<>();
        for(VerseRef v:geometry.versesOnLines(geometry.lineIdsOnPage(previewPage))) {
            if(v.getSurah()==previewGroup.surah
                && v.getAyah()>=previewGroup.startAyah
                && v.getAyah()<=previewGroup.endAyah
                && !exact.contains(v))exact.add(v);
        }
        pagePreview.setHighlightVerses(exact);
        pagePreview.show(previewPage,java.util.Collections.emptyList(),
            java.util.Collections.emptyList(),0);
    }

    private void advancePreviewFromCaption() {
        if(previewGroup==null||geometry==null)return;
        try {
            int first=geometry.pageForVerse(
                new VerseRef(previewGroup.surah,previewGroup.startAyah));
            int last=geometry.pageForVerse(
                new VerseRef(previewGroup.surah,previewGroup.endAyah));
            if(first==last) { showBlockDetails(); return; }
            previewPage = previewPage>=last ? first : previewPage+1;
            renderPreview();
        } catch(RuntimeException missingGeometry) {
            Toast.makeText(this,"Miniature indisponible.",Toast.LENGTH_SHORT).show();
        }
    }

    private void turnPreviewPage(int delta) {
        if(previewGroup==null||geometry==null)return;
        int first=geometry.pageForVerse(new VerseRef(previewGroup.surah,previewGroup.startAyah));
        int last=geometry.pageForVerse(new VerseRef(previewGroup.surah,previewGroup.endAyah));
        int next=Math.max(first,Math.min(last,previewPage+delta));
        if(next!=previewPage) {
            previewPage=next;
            renderPreview();
        }
    }

    private void openPreviewInMushaf() { openPreviewInMushaf(null); }

    private void openPreviewInMushaf(VerseRef tappedVerse) {
        if(previewGroup!=null)openMushaf(previewGroup,previewPage,tappedVerse);
    }

    private void openMushaf(IbnKathirGroupIndex.Group group) {
        openMushaf(group,0);
    }

    private void openMushaf(IbnKathirGroupIndex.Group group,int preferredPage) {
        openMushaf(group,preferredPage,null);
    }

    private void openMushaf(IbnKathirGroupIndex.Group group,int preferredPage,VerseRef tappedVerse) {
        try {
            VerseRef begin = new VerseRef(group.surah,group.startAyah);
            GeometryRepository g=GeometryRepository.get(this);
            int page=g.pageForVerse(begin);
            int last=g.pageForVerse(new VerseRef(group.surah,group.endAyah));
            if(preferredPage>=page&&preferredPage<=last)page=preferredPage;
            VerseRef selectedVerse=begin;
            if(page!=g.pageForVerse(begin)) {
                for(VerseRef v:g.versesOnLines(g.lineIdsOnPage(page))) {
                    if(v.getSurah()==group.surah&&v.getAyah()>=group.startAyah
                        &&v.getAyah()<=group.endAyah) {selectedVerse=v;break;}
                }
            }
            // Open the actual touched ayah only if it belongs to the verified block/page.
            // A surface tap retains the first visible verse of the current block.
            if(tappedVerse!=null && tappedVerse.getSurah()==group.surah
                    && tappedVerse.getAyah()>=group.startAyah
                    && tappedVerse.getAyah()<=group.endAyah
                    && g.versesOnLines(g.lineIdsOnPage(page)).contains(tappedVerse)) {
                selectedVerse=tappedVerse;
            }
            // Open a temporary reader: no Hifz cursor or persisted Lecture bookmark changes.
            Intent intent = new Intent(this,StudyReaderActivity.class);
            intent.putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,page);
            intent.putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE,selectedVerse.toString());
            intent.putExtra(StudyReaderActivity.EXTRA_MAP_PREVIEW,true);
            startActivity(intent);
        } catch (RuntimeException invalidGeometry) {
            Toast.makeText(this,"Le verset demandé est absent du Mushaf.",Toast.LENGTH_LONG).show();
        }
    }

    /** Rights-safe documentary sheet: only numeric references and the pinned index. */
    private void showBlockDetails() {
        IbnKathirGroupIndex.Group group=previewGroup;
        if(group==null) { showSource(); return; }
        try {
            GeometryRepository g=geometry==null?GeometryRepository.get(this):geometry;
            int first=g.pageForVerse(new VerseRef(group.surah,group.startAyah));
            int last=g.pageForVerse(new VerseRef(group.surah,group.endAyah));
            int total=index.groupsForSurah(group.surah).size();
            String[] headings=IbnKathirQuranComSource.verifiedQafHeadings(group);
            StringBuilder details=new StringBuilder("Référence : "+group.navigationRange()
                +"\nIdentifiant : "+group.id
                +"\nPages du Mushaf : "+first+(last==first?"":" à "+last)
                +"\nPage affichée : "+previewPage);
            if(headings.length>0) {
                details.append("\n\nTitres originaux anglais vérifiés (Quran.com) :");
                for(String title:headings) details.append("\n• ").append(title);
            } else {
                details.append("\n\nTitres originaux : consulter le commentaire sur Quran.com.");
            }
            details.append("\n\nLe commentaire complet est consultable sur Quran.com. ")
                   .append("La Carte ne copie pas le texte du Tafsir dans l'application.")
                   .append("\n\nAucune progression de mémorisation n’est modifiée depuis la Carte.");
            new AlertDialog.Builder(this)
                .setTitle("Ibn Kathīr · Bloc "+(selectedGroupIndex+1)+"/"+total)
                .setMessage(details.toString())
                .setPositiveButton("Lire Ibn Kathīr",(d,w)->openIbnKathirOnQuranCom(group))
                .setNeutralButton("Ouvrir le Mushaf",(d,w)->openMushaf(group,previewPage))
                .setNegativeButton("Fermer",(d,w)->d.dismiss())
                .show();
        } catch(RuntimeException missingGeometry) {
            Toast.makeText(this,"Référence du bloc indisponible.",Toast.LENGTH_LONG).show();
        }
    }

    /** External consultation on an explicit tap only, never during map navigation.
     * No Android network permission, persisted preference, or Hifz cursor mutation.
     */
    private void openIbnKathirOnQuranCom(IbnKathirGroupIndex.Group group) {
        if(group==null) return;
        final String url;
        try {
            url=IbnKathirQuranComSource.urlForGroup(group);
        } catch(IllegalArgumentException invalidSource) {
            Toast.makeText(this,"Référence Ibn Kathīr invalide.",Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Intent view=new Intent(Intent.ACTION_VIEW,Uri.parse(url));
            view.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(view);
        } catch(ActivityNotFoundException unavailable) {
            new AlertDialog.Builder(this)
                .setTitle("Quran.com")
                .setMessage("Aucun navigateur disponible pour ouvrir le commentaire.\n"+url)
                .setPositiveButton("Fermer",(d,w)->d.dismiss()).show();
        }
    }

    private void showSource() {
        if(previewGroup!=null) { showBlockDetails(); return; }
        new AlertDialog.Builder(this)
            .setTitle("Ibn Kathīr · source")
            .setMessage("La Carte référence 1 903 groupes authentifiés du Tafsir Ibn Kathīr anglais abrégé. "
                +"Sélectionner un groupe puis « Lire Ibn Kathīr » ouvre son commentaire exact sur Quran.com. "
                +"Aucune donnée de mémorisation n’est modifiée.")
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
        out.putInt("selectedGroupIndex",selectedGroupIndex);
        out.putInt("previewPage",previewPage);
        super.onSaveInstanceState(out);
    }

    @Override protected void onDestroy() {
        if(pagePreview!=null)pagePreview.destroySafely();
        super.onDestroy();
    }
}
