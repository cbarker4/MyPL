///*
//    File: SpeedTest.java
// */
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.fail;
//import org.junit.Test;
//import org.junit.Ignore;
//import org.junit.Before;
//import org.junit.After;
//
//import java.io.*;
//
//public class speedTest {
//    private PrintStream stdout = System.out;
//    private ByteArrayOutputStream output = new ByteArrayOutputStream();
////    @Before
////    public void changeSystemOut() {
////        // redirect System.out to output
////        System.setOut(new PrintStream(output));
////    }
////
////    @After
////    public void restoreSystemOut() {
////        // reset System.out to standard out
////        System.setOut(stdout);
////    }
//    @Test
//    public void runtest() throws IOException {
//        String s = buildString
//                ("fun void main() {",
//                        "}");
//        long start = System.currentTimeMillis();
//        String homeDirectory = System.getProperty("user.home");
//        Process process;
//
//            process = Runtime.getRuntime()
//                    .exec(String.format("cmd.exe /c dir %s", homeDirectory));
//
//
//    }
//}
