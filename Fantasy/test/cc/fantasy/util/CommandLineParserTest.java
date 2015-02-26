package cc.fantasy.util;

import org.apache.log4j.Category;

import junit.framework.TestCase;

public class CommandLineParserTest extends TestCase {

    static Category log = Category.getInstance(CommandLineParserTest.class);
    
    public void test() {
        CommandLineParser p = new CommandLineParser();        
        log.info(p.getUsage("Program"));
        
        p = new CommandLineParser("x");
        log.info(p.getUsage("Program"));
        p.parse(new String [] { "-x", "x" }, "s");
        
        p = new CommandLineParser("x$i");
        log.info(p.getUsage("Program"));
        
        p = new CommandLineParser("x$i\"set <num> exes\"");
        log.info(p.getUsage("Program"));
        
        p = new CommandLineParser("xy$sz$fq$d\"set source dir\"t$i\"set <num> exes\"");
        log.info(p.getUsage("Program"));
        
        p.parse(new String [] {
           "-x", "-y", "hello there", "-z", "lib/cypher.jar", "-q", "test", "hello", "goodbye" 
        });

        assertTrue(p.getParamSpecified('x'));
        assertTrue(p.getParamSpecified('y'));
        assertEquals(p.getParamValue('y'), "hello there");
        assertTrue(p.getParamSpecified('z'));
        assertEquals(p.getParamValue('z'), "lib/cypher.jar");
        assertTrue(p.getParamSpecified('q'));
        assertEquals(p.getParamValue('q'), "test");
        assertEquals(p.getNumArgs(), 2);
        assertEquals(p.getArg(0), "hello");
        assertEquals(p.getArg(1), "goodbye");
        
        p.parse(new String [] {
            "-y\"smoopy smoo\""
        });
        
        assertTrue(p.getParamSpecified('y'));
        assertEquals(p.getParamValue('y'), "smoopy smoo");
        
    }
    
    public void test2() {
    	CommandLineParser p = new CommandLineParser("xy$sz$fq$d\"set source dir\"t$i\"set <num> exes\"");
        log.info(p.getUsage("Program"));
    	
    	p.parse(new String [] {
        	"Hello", "3", "lib/cypher.jar", "test"	
        }, "s\"Some String\"i\"Some int\"F(none)\"some file\"D(none)\"some dir\"");
        
        log.info(p.getUsage("Program"));
        
        assertEquals(p.getNumArgs(), 4);
        assertEquals(p.getArg(0), "Hello");
        assertEquals(p.getArg(1), "3");
        assertEquals(p.getArg(2), "lib/cypher.jar");
        assertEquals(p.getArg(3), "test");
    }
    
    public void test3() {
    	negParamFormatTest("x$q");
    	negParamFormatTest("x$3");
    	negParamFormatTest("x$3\"hello");
    	
    	negArgFormatTest("i\"some int\"fFd");
    }
    
    private void negParamFormatTest(String paramFormat) {
    	log.info("Negative test bad paramFormat [" + paramFormat + "]");
    	try {
    		new CommandLineParser(paramFormat);
    		fail("Failed to catch malformed paramFormat [" + paramFormat + "]");
    	} catch (Exception e) {
    		log.info(e.getMessage());
    	}
    	log.info("PASSED");
    }

    private void negArgFormatTest(String argFormat) {
    	log.info("Negative test bad argFormat [" + argFormat + "]");
    	try {
    		new CommandLineParser().parseArgsFormat(argFormat);
    		fail("Failed to catch malformed paramFormat [" + argFormat + "]");
    	} catch (Exception e) {
    		log.info(e.getMessage());
    	}
    	log.info("PASSED");
    }

    
}
