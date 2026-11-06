import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.runner.*;
import org.junit.runner.notification.*;

/** Executes actual compiled JUnit suites and emits per-case XML/CSV, including failures. */
public final class Audit11711JvmRunner {
    static String xml(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    static String csv(String s){return "\""+s.replace("\"","\"\"")+"\"";}
    public static void main(String[] args)throws Exception {
        Path out=Paths.get(args[0]);Files.createDirectories(out);
        List<String[]> rows=new ArrayList<>();Map<Description,Long> times=new HashMap<>();Map<Description,Failure> failed=new HashMap<>();
        JUnitCore core=new JUnitCore();
        core.addListener(new RunListener(){
            public void testStarted(Description d){times.put(d,System.nanoTime());}
            public void testFailure(Failure f){failed.put(f.getDescription(),f);}
            public void testFinished(Description d){
                Failure f=failed.get(d);rows.add(new String[]{d.getClassName(),d.getMethodName(),f==null?"PASS":"FAIL",
                    String.valueOf((System.nanoTime()-times.getOrDefault(d,System.nanoTime()))/1000000),f==null?"":f.getTrace()});
            }
        });
        Class<?>[] classes=new Class<?>[args.length-1];for(int i=1;i<args.length;i++)classes[i-1]=Class.forName(args[i]);
        Result r=core.run(classes);
        for(Failure f:r.getFailures())if(rows.stream().noneMatch(row->row[0].equals(f.getDescription().getClassName())&&Objects.equals(row[1],f.getDescription().getMethodName())))
            rows.add(new String[]{String.valueOf(f.getDescription().getClassName()),String.valueOf(f.getDescription().getMethodName()),"FAIL","0",f.getTrace()});
        StringBuilder report=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<testsuite name=\"haafidh-11711-final-audit\" tests=\""+r.getRunCount()+"\" failures=\""+r.getFailureCount()+"\" time=\""+r.getRunTime()/1000.0+"\">\n");
        StringBuilder table=new StringBuilder("class,test,status,duration_ms,failure\n");
        for(String[] row:rows){
            report.append("<testcase classname=\"").append(xml(row[0])).append("\" name=\"").append(xml(String.valueOf(row[1]))).append("\" time=\"").append(Long.parseLong(row[3])/1000.0).append("\">");
            if(row[2].equals("FAIL"))report.append("<failure>").append(xml(row[4])).append("</failure>");
            report.append("</testcase>\n");
            for(int i=0;i<row.length;i++){if(i>0)table.append(',');table.append(csv(String.valueOf(row[i])));}table.append('\n');
        }
        report.append("</testsuite>\n");Files.writeString(out.resolve("junit.xml"),report,StandardCharsets.UTF_8);Files.writeString(out.resolve("cases.csv"),table,StandardCharsets.UTF_8);
        for(Failure f:r.getFailures())System.out.println("FAIL "+f.toString());
        System.out.println("AUDIT_RESULT tests="+r.getRunCount()+" failures="+r.getFailureCount()+" ignored="+r.getIgnoreCount()+" duration_ms="+r.getRunTime());
        if(!r.wasSuccessful())System.exit(1);
    }
}
