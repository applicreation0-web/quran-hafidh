package com.quransafeguard.hifz.preview;
import java.nio.file.*;
import java.util.regex.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class HifzActionRoleCoverageTest {
 @Test public void everyActualSessionActionHasAnApprovedDrawable() throws Exception {
   Path file=Paths.get("src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
   if(!Files.exists(file))file=Paths.get("hifz-app").resolve(file);
   String source=Files.readString(file);
   Pattern action=Pattern.compile("addRoundAction\\s*\\(\\s*\"[^\"]*\"\\s*,\\s*\"([^\"]+)\"");
   Matcher actions=action.matcher(source);
   int count=0;
   while(actions.find()) {
     String role=actions.group(1);
     assertTrue("Session action would crash due to missing icon: "+role,Ui.iconFor(role,"")!=0);
     count++;
   }
   assertTrue("Session action inventory must remain covered",count>=8);
   Matcher resume=action.matcher(source.substring(source.indexOf("private void renderLearningFinalReview()"),
       source.indexOf("private void validateLearningFinalReview()")));
   assertTrue("Blocked Sabqi must offer a real resume action",resume.find());
   assertEquals("Saturday resume must use the contractual resume pictogram",
       R.drawable.ic_ui_resume,Ui.iconFor(resume.group(1),""));
 }
}
