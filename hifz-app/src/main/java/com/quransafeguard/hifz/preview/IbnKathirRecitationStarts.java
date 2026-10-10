package com.quransafeguard.hifz.preview;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * QCF-exact locators of the 1,903 VERIFIED Ibn Kathir documentary group starts.
 * These one-word locations are NOT approved mnemonic recall cues. Never use
 * these in Hifz/Quiz/Itqan engines or alter Mushaf word geometry.
 */
final class IbnKathirRecitationStarts {
    static final String STATUS = "SOURCE_START_ONLY_NOT_EDITORIALLY_APPROVED";
    private final IbnKathirGroupIndex index;
    private final WordGeometryRepository words;

    IbnKathirRecitationStarts(IbnKathirGroupIndex index, WordGeometryRepository words) {
        if(index==null||words==null||index.count()!=1903)
            throw new IllegalStateException("Unverified Ibn Kathir or QCF source");
        this.index=index;
        this.words=words;
    }

    boolean isAvailable(){return index.count()==1903;}
    JSONArray forPage(int page){
        return forPageWords(index,words.wordsForPage(page),page,null);
    }
    JSONArray forSelectedGroup(int page,String id) {
        if(groupForId(id)==null)return new JSONArray();
        return forPageWords(index,words.wordsForPage(page),page,id);
    }

    IbnKathirGroupIndex.Group groupForId(String id) {
        if(id==null||!id.matches("IKEN[0-9]{3}_[0-9]{3}_[0-9]{3}"))return null;
        try {
            int surah=Integer.parseInt(id.substring(4,7));
            int first=Integer.parseInt(id.substring(8,11));
            if(surah<1||surah>114)return null;
            IbnKathirGroupIndex.Group group=index.containing(surah,first);
            return group!=null&&group.id.equals(id)?group:null;
        } catch(RuntimeException malformed){return null;}
    }

    static JSONArray forPageWords(IbnKathirGroupIndex index,
            List<WordGeometryRepository.WordBox> boxes,int page,String filterId) {
        JSONArray out=new JSONArray();
        if(index==null||boxes==null||boxes.isEmpty()||page<1||page>604)return out;
        Set<String> seen=new HashSet<>();
        for(WordGeometryRepository.WordBox word:boxes){
            if(word.word!=1)continue;
            IbnKathirGroupIndex.Group group;
            try {group=index.containing(word.surah,word.ayah);}
            catch(RuntimeException invalid){return new JSONArray();}
            if(group==null||group.startAyah!=word.ayah)continue;
            if(filterId!=null&&!filterId.equals(group.id))continue;
            if(!seen.add(group.id))return new JSONArray();
            try {
                JSONObject cue=new JSONObject();
                cue.put("id",group.id);
                cue.put("index",out.length()+1);
                cue.put("title","Départ · "+group.navigationRange());
                cue.put("anchor","");
                cue.put("anchorWordCount",1);
                cue.put("boxes",new JSONArray().put(word.boxJson()));
                cue.put("ranges",new JSONArray());
                cue.put("status",STATUS);
                out.put(cue);
            } catch(JSONException invalid){return new JSONArray();}
        }
        return out;
    }
}
