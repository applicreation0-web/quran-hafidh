package com.quransafeguard.hifz.preview;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

/** Pre-write schema validation; Android store preservation is tested separately. */
public final class HifzBackupSchemaValidationTest {
    private void check(String type, Object value) throws Exception {
        JSONObject values=new JSONObject().put("schema",new JSONObject().put("t",type).put("v",value));
        Method method=HifzBackup.class.getDeclaredMethod("checkValues",JSONObject.class);
        method.setAccessible(true);
        try { method.invoke(null,values); }
        catch(InvocationTargetException e) { throw (Exception)e.getCause(); }
    }
    @Test public void rejectsStringSchemaInsteadOfCommittingAnUnreadablePreference() throws Exception {
        assertThrows(JSONException.class,()->check("s","6"));
    }
    @Test public void rejectsFutureSchemaBeforeReplacingAcquisitions() throws Exception {
        assertThrows(JSONException.class,()->check("i",999));
    }
    @Test public void rejectsZeroSchemaWhichWouldInitializeOverRestoredProgress() throws Exception {
        assertThrows(JSONException.class,()->check("i",0));
    }
    @Test public void rejectsCoercedAndFractionalSchemaNumbers() throws Exception {
        assertThrows(JSONException.class,()->check("i","6"));
        assertThrows(JSONException.class,()->check("i",5.5));
        assertThrows(JSONException.class,()->check("l",6));
    }
    @Test public void acceptsEverySupportedHistoricalSchema() throws Exception {
        for(int schema=1;schema<=PreviewConfig.SCHEMA_VERSION;schema++)check("i",schema);
    }
}
