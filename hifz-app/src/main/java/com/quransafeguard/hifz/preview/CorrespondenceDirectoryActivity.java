package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.HorizontalScrollView;
import android.graphics.drawable.GradientDrawable;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.quransafeguard.hifz.core.VerseRef;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * New read-only Correspondances destination; Atlas and the Hifz engines are not touched.
 * A historically proposed type is NOT a runtime-approved relation.
 * Exact references and editorial caveats are kept in private Android assets.
 */
public final class CorrespondenceDirectoryActivity extends Activity {
    private static final String REFERENCE_ASSET="correspondences/historical_110_v1.json";
    private static final String REFERENCE_SHA256="b46c0e7d7800cbb1cfe34c72a39328339608d9b4e0ede5397dcd780de32aa8a2";
    private final List<JSONObject> historic=new ArrayList<>();
    private CorrespondenceRuntime runtime;
    private LinearLayout results;
    private EditText search;
    private LinearLayout typeRail;
    // An empty selection opens the families overview; 'TOUS' explicitly browses 110 dossiers.
    private String selectedFamily="ACCUEIL";
    private static final String[] FAMILIES={"REPETITION","RESSEMBLANCE","OPPOSITION","PROGRESSION",
        "ALTERNANCE","INVERSION","CENTRE","DEBUT_FIN_INCLUSIO"};

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        runtime=CorrespondenceRuntime.load(this);
        loadHistoricalIndex();

        LinearLayout root=Ui.column(this);
        root.setBackgroundColor(Ui.PAPER);
        LinearLayout header=Ui.row(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(Ui.iconButton(this,"","Retour",v->finish()));
        TextView title=Ui.bookText(this,"Correspondances",18,true);
        title.setGravity(Gravity.CENTER);
        header.addView(title,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));
        header.addView(new View(this),new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        root.addView(header);
        root.addView(Ui.divider(this));

        String status=historic.size()==110
            ? "110 dossiers historiques · "+runtime.catalog.relations.size()+" relations consultables"
            : runtime.catalog.relations.size()+" relations consultables";
        TextView info=Ui.text(this,status,12,false);
        info.setPadding(Ui.dp(this,10),Ui.dp(this,8),Ui.dp(this,10),Ui.dp(this,8));
        root.addView(info);
        HorizontalScrollView navigator=new HorizontalScrollView(this);
        navigator.setHorizontalScrollBarEnabled(false);navigator.setFillViewport(false);
        typeRail=Ui.row(this);typeRail.setGravity(Gravity.CENTER_VERTICAL);
        navigator.addView(typeRail,new android.widget.FrameLayout.LayoutParams(-2,Ui.dp(this,88)));
        root.addView(navigator,new LinearLayout.LayoutParams(-1,Ui.dp(this,88)));
        renderTypeRail();
        search=new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setHint("Chercher parmi les 110 dossiers");
        search.setContentDescription("Rechercher une correspondance");
        search.setPadding(Ui.dp(this,12),Ui.dp(this,8),Ui.dp(this,12),Ui.dp(this,8));
        root.addView(search,new LinearLayout.LayoutParams(-1,Ui.dp(this,52)));
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(false);
        results=Ui.column(this);
        results.setPadding(Ui.dp(this,6),0,Ui.dp(this,6),0);
        scroll.addView(results);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        search.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){render(s.toString());}
            @Override public void afterTextChanged(Editable e){}
        });
        render("");
    }

    /** Atlas-like eight-family pictogram rail. Icons identify navigation categories,
     * not documentary approval of each historical proposal. */
    private void renderTypeRail(){
        typeRail.removeAllViews();
        addFamily("ACCUEIL","Départ",null);
        addFamily("TOUS","110",null);
        for(String code:FAMILIES) {
            CorrespondenceModel.Type type=CorrespondenceModel.Type.valueOf(code);
            addFamily(code,CorrespondenceScreen.label(type),type);
        }
        addFamily("NON_ETABLI","Sans type",null);
    }
    private void addFamily(String family,String label,CorrespondenceModel.Type type){
        boolean selected=family.equals(selectedFamily);
        LinearLayout node=Ui.column(this);node.setGravity(Gravity.CENTER);
        node.setPadding(Ui.dp(this,6),Ui.dp(this,3),Ui.dp(this,6),Ui.dp(this,2));
        GradientDrawable outline=new GradientDrawable();
        outline.setColor(Ui.PAPER);
        if(selected){outline.setCornerRadius(Ui.dp(this,8));outline.setStroke(Ui.dp(this,2),Ui.INK);}
        node.setBackground(outline);
        if(type!=null){
            ImageView icon=new ImageView(this);
            icon.setImageResource(CorrespondenceScreen.icon(type));
            icon.setImageTintList(null);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            node.addView(icon,new LinearLayout.LayoutParams(Ui.dp(this,38),Ui.dp(this,42)));
        }else{
            TextView marker=Ui.text(this,family.equals("ACCUEIL")?"⌂":family.equals("TOUS")?"110":"?",14,true);
            marker.setGravity(Gravity.CENTER);
            node.addView(marker,new LinearLayout.LayoutParams(Ui.dp(this,38),Ui.dp(this,42)));
        }
        TextView name=Ui.text(this,label,10,false);name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        node.addView(name,new LinearLayout.LayoutParams(-1,Ui.dp(this,24)));
        node.setContentDescription(label+" · famille de relations");
        node.setClickable(true);node.setFocusable(true);
        node.setOnClickListener(v->{
            selectedFamily=family;renderTypeRail();render(search.getText().toString());
        });
        typeRail.addView(node,new LinearLayout.LayoutParams(Ui.dp(this,92),Ui.dp(this,77)));
    }

    private void loadHistoricalIndex(){
        try(InputStream input=getAssets().open(REFERENCE_ASSET);
            ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192]; int n;
            while((n=input.read(buffer))!=-1){
                if(out.size()+n>300000)throw new IllegalStateException("Oversized historical index");
                out.write(buffer,0,n);
            }
            byte[] raw=out.toByteArray();
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(raw);
            StringBuilder hash=new StringBuilder();
            for(byte b:digest)hash.append(String.format(Locale.ROOT,"%02x",b&255));
            if(!REFERENCE_SHA256.equals(hash.toString()))throw new IllegalStateException("Historical index integrity mismatch");
            JSONObject document=new JSONObject(new String(raw,StandardCharsets.UTF_8));
            JSONArray entries=document.getJSONArray("entries");
            if(!"HAAFIDH_CORRESPONDENCE_HISTORICAL_110_DIRECTORY_V1".equals(document.getString("schema"))
                ||document.getInt("count")!=110||entries.length()!=110)throw new IllegalStateException("Invalid historical registry");
            for(int i=0;i<entries.length();i++){
                JSONObject entry=entries.getJSONObject(i);
                if(!String.format(Locale.ROOT,"R%03d",i+1).equals(entry.getString("id")))throw new IllegalStateException("Noncanonical relation ID");
                historic.add(entry);
            }
        }catch(Exception missing){historic.clear();}
    }

    private static String reference(JSONObject r){
        int s=r.optInt("surah"),a=r.optInt("start"),b=r.optInt("end");
        return s+":"+a+(b==a?"":"–"+b);
    }
    private static String references(JSONArray arr){
        StringBuilder b=new StringBuilder();
        for(int i=0;i<arr.length();i++){
            if(i>0)b.append(" ↔ ");
            b.append(reference(arr.optJSONObject(i)));
        }
        return b.toString();
    }
    private void render(String query){
        results.removeAllViews();
        String needle=query.toLowerCase(Locale.ROOT).trim();
        int shown=0;
        if(needle.isEmpty() && "ACCUEIL".equals(selectedFamily)){
            TextView lead=Ui.text(this,"Relations vérifiées · "+runtime.catalog.relations.size()+
                "\nChoisir une famille ci-dessus pour parcourir les propositions.",13,false);
            lead.setPadding(Ui.dp(this,12),Ui.dp(this,14),Ui.dp(this,12),Ui.dp(this,16));
            results.addView(lead);
            for(CorrespondenceModel.Relation relation:runtime.catalog.relations){
                addApproved(relation);shown++;
            }
            return;
        }
        if(historic.size()==110){
            for(JSONObject entry:historic){
                String id=entry.optString("id");
                String label=entry.optString("title");
                String type=entry.optString("type");
                String refs=references(entry.optJSONArray("refs"));
                String searchable=(id+" "+label+" "+type+" "+refs).toLowerCase(Locale.ROOT);
                if(!searchable.contains(needle))continue;
                if(needle.isEmpty() && !"TOUS".equals(selectedFamily)
                   && !"ACCUEIL".equals(selectedFamily)
                   && !( "NON_ETABLI".equals(selectedFamily)
                        ? !isKnownFamily(type) : selectedFamily.equals(type)))continue;
                addEntry(entry,id,label,type,refs);
                shown++;
            }
        }else{
            // Still useful when private historical asset is absent: approved relations only.
            for(CorrespondenceModel.Relation relation:runtime.catalog.relations){
                String label=relation.presentation.shortTitle;
                String type=relation.type.name();
                if(!(relation.id+" "+label+" "+type).toLowerCase(Locale.ROOT).contains(needle))continue;
                addApproved(relation);
                shown++;
            }
        }
        if(shown==0){
            TextView empty=Ui.text(this,historic.size()==110?"Aucun résultat":"Aucune correspondance disponible",14,false);
            empty.setPadding(Ui.dp(this,12),Ui.dp(this,24),Ui.dp(this,12),Ui.dp(this,12));
            results.addView(empty);
        }
    }
    private static boolean isKnownFamily(String type){
        for(String known:FAMILIES)if(known.equals(type))return true;
        return false;
    }
    private void addApproved(CorrespondenceModel.Relation relation){
        String label=relation.id+" · "+relation.presentation.shortTitle;
        appendRow(label,CorrespondenceScreen.label(relation.type)+" · Vérifiée",relation.type,relation.id,()->openApproved(relation));
    }
    private void addEntry(JSONObject entry,String id,String title,String type,String refs){
        CorrespondenceModel.Relation approved=runtime.find(id);
        // Only a verified, runtime-admitted relation earns its contractual icon.
        // A historical proposal keeps the neutral information pictogram.
        CorrespondenceModel.Type iconType=approved==null?null:approved.type;
        String status=approved!=null?"CONSULTABLE":entry.optString("status").equals("NON_RÉSOLU")?"TYPE NON ÉTABLI":"TYPE PROPOSÉ";
        appendRow(id+" · "+title,refs+" · "+status,iconType,id,()->{
            if(approved!=null)openApproved(approved);
            else if(entry.optJSONArray("refs")!=null&&entry.optJSONArray("refs").length()>0
                    &&entry.optJSONArray("refs").length()<=40)
                startActivity(new Intent(this,CorrespondenceReferenceActivity.class)
                    .putExtra("relationId",id));
            else details(entry);
        });
    }
    private void appendRow(String title,String subtitle,CorrespondenceModel.Type type,String id,Runnable open){
        LinearLayout row=Ui.row(this);
        row.setPadding(Ui.dp(this,2),Ui.dp(this,8),Ui.dp(this,2),Ui.dp(this,8));
        row.setMinimumHeight(Ui.dp(this,68));
        row.setGravity(Gravity.CENTER_VERTICAL);
        // The eight relation icons indicate *verified* relationships only.
        // A proposed historical dossier receives a neutral ordinal dot, not "i"
        // repeated 110 times or a pictogram falsely claiming certification.
        if(type!=null){
            ImageView icon=new ImageView(this);
            icon.setImageResource(CorrespondenceScreen.icon(type));
            icon.setImageTintList(null);
            icon.setPadding(Ui.dp(this,8),Ui.dp(this,8),Ui.dp(this,8),Ui.dp(this,8));
            row.addView(icon,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        }else{
            TextView dot=Ui.text(this,id.substring(1),12,false);
            dot.setGravity(Gravity.CENTER);
            GradientDrawable ring=new GradientDrawable();
            ring.setColor(Ui.PAPER);ring.setShape(GradientDrawable.OVAL);
            ring.setStroke(Ui.dp(this,1),Ui.INK);
            dot.setBackground(ring);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(Ui.dp(this,38),Ui.dp(this,38));
            params.leftMargin=Ui.dp(this,5);params.rightMargin=Ui.dp(this,5);
            row.addView(dot,params);
        }
        LinearLayout text=Ui.column(this);
        TextView heading=Ui.text(this,title,14,true);
        TextView description=Ui.text(this,subtitle,11,false);
        description.setTextColor(Ui.MUTED);
        text.addView(heading);text.addView(description);
        row.addView(text,new LinearLayout.LayoutParams(0,-2,1));
        row.setContentDescription(title+" · "+subtitle);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v->open.run());
        results.addView(row);
        results.addView(Ui.divider(this));
    }
    private void openApproved(CorrespondenceModel.Relation relation){
        if(runtime.geometry==null||relation.anchors.isEmpty())return;
        CorrespondenceModel.Anchor a=relation.anchors.get(0);
        CorrespondenceModel.VerseRange r=a.range;
        CorrespondenceAccess.Origin origin=CorrespondenceEntryPoints.origin(
            this,CorrespondenceAccess.Surface.LECTURE,"correspondances-directory",
            r.surah,r.start,r.firstPage,"","","","catalogue",r.start,r.end);
        Intent intent=new Intent(this,CorrespondenceActivity.class);
        intent.putExtra("relationId",relation.id);
        intent.putExtra("origin",CorrespondenceEntryPoints.encode(origin));
        startActivity(intent);
    }
    private void details(JSONObject entry){
        String type=entry.optString("type","");
        if(type.isEmpty()||"null".equals(type))type="Non établi";
        JSONArray refs=entry.optJSONArray("refs");
        String message=references(refs)+"\n\nType proposé : "+type+
            "\n\n"+entry.optString("reason","")+
            "\n\nSource : "+entry.optString("source","Registre documentaire")+
            "\n\nDossier historique : relation non activée dans le lecteur.";
        AlertDialog.Builder dialog=new AlertDialog.Builder(this)
            .setTitle(entry.optString("id")+" · "+entry.optString("title"))
            .setMessage(message)
            .setNegativeButton("Fermer",null);
        // Some historical dossiers have no displayable original range.
        // Never present an action that would lead to an empty/fatal QCF reader.
        if(refs!=null&&refs.length()>0&&refs.length()<=40)
            dialog.setPositiveButton("Comparer les pages QCF",(ignored,which)->startActivity(
                new Intent(this,CorrespondenceReferenceActivity.class).putExtra("relationId",entry.optString("id"))));
        dialog.show();
    }
    private void chooseReference(JSONArray refs){
        if(refs==null||refs.length()==0)return;
        String[] titles=new String[refs.length()];
        for(int i=0;i<titles.length;i++)titles[i]=reference(refs.optJSONObject(i));
        new AlertDialog.Builder(this).setTitle("Consulter dans le mushaf")
            .setItems(titles,(dialog,which)->{
                JSONObject verse=refs.optJSONObject(which);
                if(verse==null)return;
                try{
                    int p=GeometryRepository.get(this).pageForVerse(
                        new VerseRef(verse.getInt("surah"),verse.getInt("start")));
                    startActivity(new Intent(this,StudyReaderActivity.class)
                        .putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,p));
                }catch(Exception badLocation){Ui.showFatal(this,"Position du mushaf indisponible.");}
            }).setNegativeButton("Fermer",null).show();
    }
}
