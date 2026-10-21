package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageButton;
import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRef;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Authentic QCF comparison for historical references, without falsely certifying a literary form. */
public final class CorrespondenceReferenceActivity extends Activity {
    private static final String ASSET="correspondences/historical_110_v1.json";
    private static final String HASH="b46c0e7d7800cbb1cfe34c72a39328339608d9b4e0ede5397dcd780de32aa8a2";
    private JSONObject record;
    private JSONArray ranges;
    private GeometryRepository geometry;
    private CorrespondenceQcfSurface[] views;
    private LinearLayout pages;
    private TextView description;
    private int position,expanded=-1;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        try{
            String id=getIntent().getStringExtra("relationId");
            if(id==null||!id.matches("R[0-9]{3}"))throw new IllegalArgumentException("Unrecognised dossier");
            record=verifiedRecord(id);
            ranges=record.getJSONArray("refs");
            if(ranges.length()<1||ranges.length()>40)throw new IllegalArgumentException("Invalid original references");
            geometry=GeometryRepository.get(this);
            position=state==null?0:Math.max(0,Math.min(ranges.length()-1,state.getInt("position")));
            display();
        }catch(Exception unavailable){Ui.showFatal(this,"Références QCF indisponibles pour ce dossier.");}
    }
    private JSONObject verifiedRecord(String id)throws Exception{
        byte[] data;
        try(InputStream input=getAssets().open(ASSET);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[8192];int n;
            while((n=input.read(buf))!=-1){if(out.size()+n>300000)throw new IllegalStateException("Oversized reference file");out.write(buf,0,n);}
            data=out.toByteArray();
        }
        StringBuilder hex=new StringBuilder();
        for(byte b:MessageDigest.getInstance("SHA-256").digest(data))hex.append(String.format(Locale.ROOT,"%02x",b&255));
        if(!HASH.equals(hex.toString()))throw new IllegalStateException("Wrong private historical reference");
        JSONObject root=new JSONObject(new String(data,StandardCharsets.UTF_8));
        if(!"HAAFIDH_CORRESPONDENCE_HISTORICAL_110_DIRECTORY_V1".equals(root.getString("schema"))||root.getInt("count")!=110)
            throw new IllegalArgumentException("Invalid catalogue version");
        JSONArray rows=root.getJSONArray("entries");if(rows.length()!=110)throw new IllegalArgumentException("Incomplete references");
        for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);if(id.equals(row.optString("id")))return row;}
        throw new IllegalArgumentException("Reference not found");
    }
    private static String ref(JSONObject j){return j.optInt("surah")+":"+j.optInt("start")+(j.optInt("end")==j.optInt("start")?"":"–"+j.optInt("end"));}
    private void display(){
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        LinearLayout root=Ui.column(this);root.setBackgroundColor(Ui.PAPER);
        LinearLayout toolbar=Ui.row(this);toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.addView(CorrespondenceQcfSurface.action(this,R.drawable.ic_ui_back,"Retour aux dossiers",v->finish()));
        TextView title=Ui.text(this,record.optString("id")+" · "+record.optString("title"),14,true);
        title.setMaxLines(2);toolbar.addView(title,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));
        toolbar.addView(CorrespondenceQcfSurface.action(this,R.drawable.ic_ui_info,"Justification et sources",v->details()));
        root.addView(toolbar,new LinearLayout.LayoutParams(-1,Ui.dp(this,56)));
        description=Ui.text(this,"",12,false);
        description.setGravity(Gravity.CENTER);
        description.setPadding(Ui.dp(this,6),Ui.dp(this,5),Ui.dp(this,6),Ui.dp(this,5));
        root.addView(description,new LinearLayout.LayoutParams(-1,Ui.dp(this,50)));
        pages=new LinearLayout(this);pages.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(pages,new LinearLayout.LayoutParams(-1,0,1));
        if(ranges.length()>2){
            LinearLayout rail=Ui.row(this);rail.setGravity(Gravity.CENTER);
            rail.addView(CorrespondenceQcfSurface.action(this,R.drawable.ic_ui_back,"Références précédentes",v->shift(-1)));
            TextView positionText=Ui.text(this,"Faire défiler les références du dossier",11,false);
            positionText.setGravity(Gravity.CENTER);rail.addView(positionText,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));
            rail.addView(CorrespondenceQcfSurface.action(this,R.drawable.ic_ui_next,"Références suivantes",v->shift(1)));
            root.addView(rail,new LinearLayout.LayoutParams(-1,Ui.dp(this,48)));
        }
        setContentView(root);
        showReferences();
    }
    private void shift(int delta){if(expanded>=0)expanded=-1;position=Math.max(0,Math.min(ranges.length()-1,position+delta));showReferences();}
    private void showReferences(){
        if(views!=null)for(CorrespondenceQcfSurface old:views)if(old!=null)old.destroy();
        pages.removeAllViews();
        int count=Math.min(2,ranges.length()-position);
        views=new CorrespondenceQcfSurface[count];
        for(int i=0;i<count;i++){
            JSONObject r=ranges.optJSONObject(position+i);if(r==null)continue;
            int surah=r.optInt("surah"),start=r.optInt("start"),end=r.optInt("end");
            LinearLayout panel=Ui.column(this);
            LinearLayout panelBar=Ui.row(this);panelBar.setGravity(Gravity.CENTER_VERTICAL);
            TextView label=Ui.text(this,ref(r),12,true);label.setGravity(Gravity.CENTER);
            panelBar.addView(label,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));
            final int pane=i;
            panelBar.addView(CorrespondenceQcfSurface.action(this,R.drawable.ic_ui_reading,
                "Agrandir ou réduire la page",v->{expanded=expanded==pane?-1:pane;layoutViews();}));
            panel.addView(panelBar,new LinearLayout.LayoutParams(-1,Ui.dp(this,48)));
            CorrespondenceQcfSurface surface=new CorrespondenceQcfSurface(this);
            views[i]=surface;
            panel.addView(surface,new LinearLayout.LayoutParams(-1,0,1));
            pages.addView(panel,new LinearLayout.LayoutParams(0,-1,1));
            // Original file reference, NOT an attestation of a center, alternance or chiasm.
            int first=geometry.pageForVerse(new VerseRef(surah,start));
            int last=MawdudiMapModel.lastPageForVerse(new VerseRef(surah,end),geometry);
            int before=start>1?start-1:0;
            int after=end<QuranCanon.INSTANCE.ayahCount(surah)?end+1:0;
            if(before>0)first=Math.min(first,geometry.pageForVerse(new VerseRef(surah,before)));
            if(after>0)last=Math.max(last,MawdudiMapModel.lastPageForVerse(new VerseRef(surah,after),geometry));
            CorrespondenceDocument.Focus focus=new CorrespondenceDocument.Focus(surah,start,end,after,before,first,last,geometry.pageForVerse(new VerseRef(surah,start)),true);
            surface.show(focus,null);
        }
        layoutViews();
        String type=record.optString("type","");
        if(type.isEmpty()||"null".equals(type))type="Non établi";
        description.setText("Références "+(position+1)+"–"+(position+count)+"/"+ranges.length()+" · "+type+" (proposé)\n"+
            "Versets originaux sur QCF · classification non confirmée");
    }
    private void layoutViews(){
        for(int i=0;i<pages.getChildCount();i++){
            View panel=pages.getChildAt(i);
            panel.setVisibility(expanded<0||expanded==i?View.VISIBLE:View.GONE);
        }
    }
    private void details(){
        String type=record.optString("type","");
        if(type.isEmpty()||"null".equals(type))type="Non établi";
        new AlertDialog.Builder(this).setTitle(record.optString("id")+" · "+type)
            .setMessage(record.optString("reason")+"\n\n"+record.optString("source")+
                "\n\nComparaison documentaire des versets originaux. Aucune structure non démontrée n'est affirmée par le rendu.")
            .setPositiveButton("Fermer",null).show();
    }
    @Override protected void onSaveInstanceState(Bundle state){state.putInt("position",position);super.onSaveInstanceState(state);}
    @Override protected void onDestroy(){if(views!=null)for(CorrespondenceQcfSurface x:views)if(x!=null)x.destroy();super.onDestroy();}
}
