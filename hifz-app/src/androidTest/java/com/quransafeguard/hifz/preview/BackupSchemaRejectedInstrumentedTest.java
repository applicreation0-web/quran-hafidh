package com.quransafeguard.hifz.preview;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.json.JSONObject;
import org.json.JSONException;
import org.junit.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

/** Isolated Android test installation only. */
public final class BackupSchemaRejectedInstrumentedTest {
 @Test public void malformedSchemasPreserveAllStoresAndJournal() throws Exception {
  Context context=ApplicationProvider.getApplicationContext();new HifzPrefs(context);
  JSONObject backup=HifzBackup.export(context);
  String[] names={"quran_hifz_preview_v1","quran_hifz_dashboard_ledger_v1","quran_hifz_j10_v1",
    "hifz_preview_session_gates","quran_hifz_restore_journal_v1"};
  Map<String,Map<String,?>> before=new LinkedHashMap<>();
  for(String name:names)before.put(name,context.getSharedPreferences(name,0).getAll());
  for(int version:new int[]{1,2})for(Object[] schema:new Object[][]{
    {"s","6"},{"i",999},{"i",0},{"i",5.5},{"i","6"},{null,null}}){
   JSONObject corrupt=new JSONObject(backup.toString()).put("version",version);
   if(schema[0]==null)corrupt.getJSONObject("values").remove("schema");
   else corrupt.getJSONObject("values").put("schema",new JSONObject().put("t",schema[0]).put("v",schema[1]));
   assertThrows(JSONException.class,()->HifzBackup.restore(context,corrupt));
   for(String name:names)assertEquals(name,before.get(name),context.getSharedPreferences(name,0).getAll());
  }
  assertEquals(PreviewConfig.SCHEMA_VERSION,context.getSharedPreferences(names[0],0).getInt("schema",0));
 }
}
