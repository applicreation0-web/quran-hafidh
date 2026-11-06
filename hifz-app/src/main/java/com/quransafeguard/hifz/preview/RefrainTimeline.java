package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Offline, read-only editorial transitions inside the existing Sources dialog. */
final class RefrainTimeline {
    private RefrainTimeline() {}
    static final class State {
        boolean open;
        final Set<String> stations=new HashSet<>(),nodes=new HashSet<>();
        State(Bundle saved){
            if(saved==null)return;
            open=saved.getBoolean("friseOpen",false);
            ArrayList<String> s=saved.getStringArrayList("friseStations");
            ArrayList<String> n=saved.getStringArrayList("friseNodes");
            if(s!=null)stations.addAll(s);if(n!=null)nodes.addAll(n);
        }
        void save(Bundle out){
            out.putBoolean("friseOpen",open);
            out.putStringArrayList("friseStations",new ArrayList<>(stations));
            out.putStringArrayList("friseNodes",new ArrayList<>(nodes));
        }
    }
    private static TextView text(Activity a,String value,boolean bold){
        TextView t=Ui.text(a,value,16,bold);t.setTextColor(Ui.INK);return t;
    }
    private static Button toggle(Activity a,String title,Runnable click){
        Button b=Ui.button(a,title,v->click.run());b.setAllCaps(false);
        b.setMinHeight(Ui.dp(a,44));return b;
    }
    private static void reading(Activity a,String ref,String page){
        a.startActivity(new Intent(a,StudyReaderActivity.class)
            .putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,Integer.parseInt(page))
            .putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE,ref));
    }
    static void append(Activity a,LinearLayout column,String id,State state){
        ArrayList<String[]> nodes=new ArrayList<>();
        for(String[] n:RefrainTimelineData.NODES)if(id.equals(n[0]))nodes.add(n);
        if(nodes.isEmpty())return;
        LinearLayout body=Ui.column(a);
        body.setVisibility(state.open?View.VISIBLE:View.GONE);
        Button[] opener=new Button[1];
        opener[0]=toggle(a,(state.open?"⌃ ":"⌄ ")+"Frise des refrains",()->{
            state.open=!state.open;body.setVisibility(state.open?View.VISIBLE:View.GONE);
            opener[0].setText((state.open?"⌃ ":"⌄ ")+"Frise des refrains");
        });
        opener[0].setContentDescription("Frise des refrains");
        column.addView(opener[0]);column.addView(body);
        body.addView(text(a,nodes.get(0)[1]+" · "+nodes.size()+" occurrences",true));
        body.addView(text(a,"Repères pédagogiques éditoriaux. Les sources expliquent le refrain ; elles ne proposent pas cette frise. Avant et après désignent les versets immédiatement voisins.",false));
        String previous="";LinearLayout stationBody=null;
        for(String[] n:nodes){
            if(!previous.equals(n[7])){
                previous=n[7];String key=id+":"+previous;
                LinearLayout section=Ui.column(a);section.setPadding(Ui.dp(a,8),0,0,0);
                section.setVisibility(state.stations.contains(key)?View.VISIBLE:View.GONE);
                Button heading=toggle(a,n[7],()->{
                    boolean expanded=section.getVisibility()!=View.VISIBLE;
                    section.setVisibility(expanded?View.VISIBLE:View.GONE);
                    if(expanded)state.stations.add(key);else state.stations.remove(key);
                });
                heading.setContentDescription("Station "+n[7]);
                body.addView(heading);body.addView(section);stationBody=section;
            }
            LinearLayout details=Ui.column(a);details.setPadding(Ui.dp(a,8),0,0,Ui.dp(a,8));
            details.setVisibility(state.nodes.contains(n[2])?View.VISIBLE:View.GONE);
            Button occurrence=toggle(a,n[3]+"/"+n[4]+" · "+n[5]+" · p."+n[6],()->{
                boolean expanded=details.getVisibility()!=View.VISIBLE;
                details.setVisibility(expanded?View.VISIBLE:View.GONE);
                if(expanded)state.nodes.add(n[2]);else state.nodes.remove(n[2]);
            });
            occurrence.setContentDescription("Occurrence "+n[3]+" sur "+n[4]+" · "+n[5]);
            stationBody.addView(occurrence);stationBody.addView(details);
            details.addView(text(a,"Avant · "+n[8]+"\n"+n[9],false));
            details.addView(text(a,"Refrain · "+n[5],true));
            details.addView(text(a,"Après · "+n[11]+"\n"+n[12],false));
            details.addView(toggle(a,"Lire "+n[8],()->reading(a,n[8],n[10])));
            details.addView(toggle(a,"Lire "+n[5],()->reading(a,n[5],n[6])));
            details.addView(toggle(a,"Lire "+n[11],()->reading(a,n[11],n[13])));
            stationBody.addView(Ui.divider(a));
        }
    }
}
