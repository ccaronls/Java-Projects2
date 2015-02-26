package cc.fantasy.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handy utility to perform command line parsing similar to stdarg
 * 
 * Does type checking, allows for complex param specification and argument specification
 * AND generates formatted usage text based on user defined formats.
 * 
 * @author Chris Caron
 *
 */
public class CommandLineParser {

    private enum Type {
        STRING('s', "<String>"),
        INT('i', "<Int>"),
        FILE('f', "<File>"),
        DIR('d', "<Dir>");        
        
        final char c;
        final String desc;
        
        Type(char c, String desc) {
            this.c = c;
            this.desc = desc;
        }
        
        void validate(String s) throws Exception {
            switch (this) {
            case STRING: break;
            case INT: Integer.parseInt(s); break;
            case FILE: if (!new File(s).isFile()) throw new FileNotFoundException(s); break;
            case DIR: if (!new File(s).isDirectory()) throw new FileNotFoundException("Not a directory: s"); break;
            }
        }
        
        static String getAllParams() {
            String params = "";
            for (int i=0; i<values().length; i++) {
                params += values()[i].c;
            }
            return params;
        }
        
        static Type getParam(char c) {
        	c = Character.toLowerCase(c);
            for (int i=0; i<values().length; i++) {
                if (values()[i].c == c)
                    return values()[i];
            }
            throw new RuntimeException("Invalid Type specification [" + c + "], must be one of [" + getAllParams() + "]");
        }
    };
    
    
    private class Param {
        char c;
        Type type;
        String msg = "";
        String value;
        boolean specified;
        
        public String toString() {
        	return "c=" + c + ", type=" + type + ", msg=" + msg + ", value=" + value + ", specified=" + specified;
        }
    };
    
    private class Arg {
    	boolean required;
    	String defaultValue = "";
    	String value;
    	String desc = "";
    	Type type;
    	
    	Arg() {}
    	
    	Arg(String value) {
    		this.value = value;
    	}
    	
    	public String toString() {
    		return "required=" + required + ", defaultValue=" + defaultValue + ", value=" + value + ", desc=" + desc + ", type=" + type;
    	}
    };
    
    private ArrayList<Arg> args = new ArrayList();    
    private HashMap<Character, Param> params = new LinkedHashMap();
    private int formatIndex = 0;
    private boolean hasArgsFormat = false;
    private boolean argRequired = true;

    
    /**
     * Create a parser that isolates OPTIONS parameters as specified by params format
     * (kind of like stdarg)
     * 
     * Example paramsFormat:
     *    "a$ihrv$sF$fd$d"
     *    
     * Means that following are accepted:
     * -a <int>
     * -h
     * -r
     * -v <string>
     * -F <file>
     * -d <dir>
     * 
     * you can also add info to you params by including a quoted string after the param
     * Example:
     * 
     *    "a"Enable alpha"b$i"Enable <num> betas"
     *    
     * Produces this message from getParamUsage()
     * 
     * -a          Enable apha
     * -b <int>    Enable <num> betas
     * 
     * @param argv
     * @param params
     */
    public CommandLineParser(String paramsFormat) {
        while (paramsFormat.length() > 0) {
            paramsFormat = parseParam(paramsFormat);
        }
    }

    /**
     * Create a command line parser that takes no OPTIONS paramaters
     *
     */
    public CommandLineParser() {
    }
    
    private final Pattern typePattern = Pattern.compile("^[$][" + Type.getAllParams() + "]");
    private final Pattern stringPattern = Pattern.compile("^\"[^\"]*\"");
    private final Pattern defaultValuePattern = Pattern.compile("^\\([^)]*\\)");
    
    private String parseParam(String format) {
        char c = format.charAt(0);
        format = format.substring(1);
        formatIndex++;
        if (!Character.isLetter(c))
            throw new RuntimeException("Invalid params format at index [" + formatIndex + "], expected letter");
        Param newParam = new Param();
        newParam.c = c;
        Matcher matcher = typePattern.matcher(format);
        if (matcher.find()) {
            Type type = Type.getParam(matcher.group().charAt(1));
            newParam.type = type;
            formatIndex += 2;
            format = format.substring(2);
        } 
        matcher = stringPattern.matcher(format);
        if (matcher.find()) {
            String msg = matcher.group();
            formatIndex += msg.length();
            newParam.msg = msg.substring(1, msg.length()-1);
            format = format.substring(msg.length());
        }
        params.put(c, newParam);
        return format;
    }
    
    /**
     * Parse a command line with no formating support
     * 
     * @param argv
     */
    public void parse(String [] argv) {
    	parse(argv, null);	
    }
    
    /**
     * Parse a comamnd line specified by argv with formatting support.
     * 
     * @param argv
     * @param format
     * 
     * Example Format: i"num widgets"f"input file"D(/tmp)S(string)"the name"
     * 
     * Means:
     * 
     * 2 required params: 1 integer, 1 file
     * 2 optional params: 1 directory (default '/tmp'), 1 string (default 'string')
     * 
     * Usage outputs:
     * 
     * PARAMS:
     * 
     * <Int>						num widgets   
     * <File>						input file
     * [Dir]			/tmp
     * [String]			'string'	the name
     */
    public void parse(String [] argv, String format) {
        int i=0;
        Param param = null;
        argv = resolveQuotes(argv);
        try {
            for (i=0; i<argv.length; i++) {
                if (argv[i].startsWith("-")) {
                    param = params.get(argv[i].charAt(1));
                    if (param == null)
                        throw new RuntimeException("Invalid option [" + argv[i] + "]");
                    param.specified = true;
                    if (param.type != null) {
                        if (argv[i].length()>2) {
                            param.value = argv[i].substring(2);
                        } else {
                            param.value = argv[++i];
                        }
                        if (stringPattern.matcher(param.value).matches()) {
                            param.value = param.value.substring(1, param.value.length()-1);
                        }
                        param.type.validate(param.value);
                    }
                } else {
                    break;
                }
            }

            
            if (format == null)
            	parseArgs(argv, i);
            else            
            	parseArgs(argv, i, format);
            
        } catch (FileNotFoundException e) {
    		throw new RuntimeException("Not a file [" + e.getMessage() + "]");
    	} catch (NumberFormatException e) {
    		throw new RuntimeException("Not a number [" + e.getMessage() + "]");
    	} catch (Exception e) {
    		throw new RuntimeException(e.getMessage());
    	}

    }
    
    private String [] resolveQuotes(String [] argv) {
    	StringBuffer buf = new StringBuffer();
    	ArrayList<String> newArgs = new ArrayList();
        boolean quoted = false;
        for (int i=0; i<argv.length; i++) {
            
            if (quoted) {
                buf.append(argv[i]);
                if (argv[i].indexOf("\"") >= 0) {
                    quoted= false;
                   	newArgs.add(buf.substring(1, buf.length()-1).toString());
                    buf.setLength(0);
                }
            } else {
                if (argv[i].startsWith("\"")) {
                    quoted = true;
                    buf.append(argv[i]);
                } else {
                    newArgs.add(argv[i]);
                }
                
            }
        }
        return newArgs.toArray(new String[newArgs.size()]);
    }
    
    // package access for so Junit can test this func individually
    void parseArgsFormat(String format) {
   		while (format.length() > 0)
   			format = this.parseArgFormat(format);
    }
    
    /*
     * 
     */
    private String parseArgFormat(String format) {
    	char c = format.charAt(0);
    	if (!Character.isLetter(c))
    		throw new RuntimeException("Invalid format at index " + formatIndex + ", expected letter, found [" + c + "]");

    	formatIndex ++;
    	Arg arg = new Arg();
    	args.add(arg);
    	arg.type = Type.getParam(c);
    	format = format.substring(1);

    	Matcher matcher = null;
    	if (Character.isUpperCase(c)) {
    		argRequired = false;
        	matcher = defaultValuePattern.matcher(format);
        	if (matcher.find()) {
        		 arg.required = false;
        		 String value = matcher.group();
        		 arg.defaultValue = value.substring(1, value.length()-1);
        		 format = format.substring(value.length());
        		 formatIndex += value.length();
        	}
    	} else if (!argRequired) {
    		throw new RuntimeException("Invalid format at index " + formatIndex + ", cannot specify required parms after non-required");
    	}
    	arg.required = argRequired;
    	matcher = stringPattern.matcher(format);
    	if (matcher.find()) {
    		String value = matcher.group();
    		arg.desc = value.substring(1, value.length()-1);
    		formatIndex += value.length();
    		format = format.substring(value.length());
    	}
    	return format;
    }
    
    private void parseArgs(String [] argv, int index) {
    	for (int i=index; i<argv.length; i++) {
    		args.add(new Arg(argv[i]));
    	}
    }
    
    private void parseArgs(String [] argv, int index, String format) throws Exception {
    	
   		formatIndex = 0;
   		hasArgsFormat = true;
        int argsIndex = 0;
   		
   		parseArgsFormat(format);
    	
    	while (index < argv.length) {
    		if (argsIndex >= args.size())
    			throw new RuntimeException("Too may arguments");
    		Arg arg = args.get(argsIndex);
    		arg.value = argv[index];
    		arg.type.validate(arg.value);
    		index ++;
            argsIndex ++;
    	}
    	
    	if (index<args.size()) {
    		if (args.get(index).required)
    			throw new RuntimeException("Not enough arguments specified.  Expected " + args.get(index).type + " [" + args.get(index).desc + "]");	        }
        
    }
    
    
    
    /**
     * Return number of arguments parsed, including default args. 
     * 
     * @return
     */
    public int getNumArgs() {
        return args.size();
    }
    
    /**
     * 
     * @param index
     * @return
     */
    public String getArg(int index) {
    	Arg arg = args.get(index);
    	if (arg.value == null)
    		return arg.defaultValue;
    	return arg.value;
    }
    
    /**
     * 
     * @param c
     * @return
     */
    public String getParamValue(char c) {
        return params.get(c).value;
    }
    
    /**
     * 
     * @param c
     * @return
     */
    public boolean getParamSpecified(char c) {
        Param p = params.get(c);
        if (p == null)
            throw new RuntimeException("'" + c + "' is not a valid param, is one of " + params.keySet());
        return params.get(c).specified;
    }
    
    /**
     * 
     * @param appName
     * @return
     */
    public String getUsage(String appName) {
    	StringBuffer buf = new StringBuffer();
    	buf.append("USAGE: ").append(appName).append(" ");
    	if (params.size() > 0) {
    		buf.append("[OPTIONS] ");
    	}
    	if (hasArgsFormat) {
	    	for (int i=0; i<args.size(); i++) {
	    		Arg arg = args.get(i);
                String desc = arg.desc;
                if (desc == null)
                    desc = arg.type.name();
	    		if (arg.required) {
	    			buf.append("<").append(desc).append(">");
	    		} else {
	    			buf.append("[").append(desc).append("]");
	    		}
	    		buf.append(" ");
	    	}
    	}
    	buf.append("\n\n");
    	if (params.size() > 0) {
    		buf.append("[OPTIONS]\n").append(getParamUsage()).append("\n");
    	}
    	
    	if (args.size() > 0 && hasArgsFormat) {
    		buf.append("[ARGS]\n").append(getArgsUsage()).append("\n");
    	}
    	
    	return buf.toString();
    }
    
    /**
     * 
     * @return
     */
    public String getArgsUsage() {
    	final int spacing1 = 6;
    	final int spacing2 = 15;
        final int spacing3 = 15;
    	StringBuffer buf = new StringBuffer();
    	for (int i=0; i<args.size(); i++) {
        	int s = 0;
    		Arg arg = args.get(i);
    		String desc = "arg" + i;
            buf.append(desc);
            s+=desc.length();
            while (s++ < spacing1)
                buf.append(" ");
            String req = "required";
            if (!arg.required)
                req = "optional (" + arg.defaultValue + ")  ";
            buf.append(req);
            s = req.length();
            while (s++ < spacing2)
                buf.append(" ");
            buf.append(arg.type.name());
    		s = arg.type.name().length();
    		while (s++ < spacing3)
    			buf.append(" ");
    		buf.append(arg.desc).append("\n");    		
    	}        
    	return buf.toString();
        
    }
    
    /**
     * 
     * @return
     */
    public String getParamUsage() {
        StringBuffer buf = new StringBuffer();
        
        final int spacing = 15;
        Iterator it = params.keySet().iterator();
        while (it.hasNext()) {
            Param param = params.get(it.next());
            buf.append("-").append(param.c).append(" ");
            int s = 0;
            if (param.type != null) {
                String desc = param.type.desc;
                buf.append(desc);
                s = desc.length();
            }
            while (s++ < spacing) {
                buf.append(" ");
            }
            buf.append(param.msg).append("\n");
        }
        
        
        return buf.toString();
    }
    
    /*
     *  (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
    	return "PARAMS=" + params + ", ARGS=" + args + ", formatIndex=" + formatIndex;
    }
}
