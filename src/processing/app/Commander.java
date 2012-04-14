/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2008 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package processing.app;


import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import processing.core.PApplet;

import processing.app.debug.*;


/**
 * Class to handle running Processing from the command line.
 * <PRE>
 * --help               Show the help text.
 * 
 * --sketch=&lt;name&rt;      Specify the sketch folder (required)
 * --output=&lt;name&rt;      Specify the output folder (required and
 *                      cannot be the same as the sketch folder.)
 * 
 * --preprocess         Preprocess a sketch into .java files.
 * --build              Preprocess and compile a sketch into .class files.
 * --run                Preprocess, compile, and run a sketch.
 * --present            Preprocess, compile, and run a sketch full screen.
 * 
 * --export-applet      Export an applet.
 * --export-application Export an application.
 * --platform           Specify the platform (export to application only).
 *                      Should be one of 'windows', 'macosx', or 'linux'.
 * 
 * --preferences=&lt;file&rt; Specify a preferences file to use (optional).
 * </PRE>
 * 
 * To build the command line version, first build for your platform, 
 * then cd to processing/build/cmd and type 'dist.sh'. This will create a 
 * usable installation plus a zip file of the same. 
 * 
 * @author fry
 */
public class Commander implements RunnerListener {
  static final String helpArg = "--help";
  static final String preprocArg = "--preprocess";
  static final String buildArg = "--build";
  static final String uploadArg = "--upload";
  static final String sketchArg = "--sketch=";
  static final String outputArg = "--output=";
  static final String portArg = "--port=";
  static final String targetArg = "--target=";
  static final String preferencesArg = "--preferences=";

  static final int HELP = -1;
  static final int PREPROCESS = 0;
  static final int BUILD = 1;
  static final int UPLOAD = 2;
  
  Sketch sketch;

  static public void main(String args[]) {
	  Commander commander = new Commander(args);
  }

  public Commander(String[] args) {
    String sketchFolder = null;
    String pdePath = null;  // path to the .pde file
    String outputPath = null;
	String portPath = null;
	String target = null;
    String preferencesPath = null;
    int platformIndex = PApplet.platform; // default to this platform
    int mode = HELP;

    for (String arg : args) {
      if (arg.length() == 0) {
        // ignore it, just the crappy shell script
        
      } else if (arg.equals(helpArg)) {
        // mode already set to HELP

      } else if (arg.equals(buildArg)) {
        mode = BUILD;

      } else if (arg.equals(uploadArg)) {
        mode = UPLOAD;

      } else if (arg.equals(preprocArg)) {
        mode = PREPROCESS;

      } else if (arg.startsWith(sketchArg)) {
        sketchFolder = arg.substring(sketchArg.length());
		File sketchy = new File(sketchFolder);
        File pdeFile = new File(sketchy, sketchy.getName() + ".pde");
        pdePath = pdeFile.getAbsolutePath();

      } else if (arg.startsWith(outputArg)) {
        outputPath = arg.substring(outputArg.length());
        
      } else if (arg.startsWith(portArg)) {
		portPath = arg.substring(portArg.length());
		  
      } else if (arg.startsWith(targetArg)) {
		target = arg.substring(targetArg.length());

      } else if (arg.startsWith(preferencesArg)) {
        preferencesPath = arg.substring(preferencesArg.length());
 
      } else {
		printCommandLine(System.err);
        complainAndQuit("I don't know anything about " + arg + ".");
      }
    }
    
	// Spit out the help and quit.
	if (mode == HELP) {
      printCommandLine(System.out);
      System.exit(0);
    }
	  
	  // Check that an output path is specified if necessary
	  // TODO:If doing upload, default to some made-up tmp directory,
	  // notify the user, and proceed.
	  if ((outputPath == null) &&
		  (mode == PREPROCESS || mode == BUILD || 
		   mode == UPLOAD)) {
			  complainAndQuit("An output path must be specified when using " + 
							  preprocArg + ", " + buildArg + ", or " + 
							  uploadArg + ".");
		  }

	// Verify that the output folder is writable
	// before actuallly trying to do anything.
    File outputFolder = new File(outputPath);
    if (!outputFolder.exists()) {
      if (!outputFolder.mkdirs()) {
        complainAndQuit("Could not create the output folder.");
      }
    }
	  
	  // Run static initialization that grabs all the prefs
	  // (If a specified preferences file was given, we use
	  // that instead)
	  // NOTE: This is done again after it was already done in the Base()
	  // constructor. It shouldn't cause any problems but is worth noting.
	  Preferences.init(preferencesPath);
	  
	  // Check that a port was specified for uploading
	  if (portPath == null && mode == UPLOAD) {
		  portPath = Preferences.get("serial.port");
		  if (portPath == null) {
			complainAndQuit("No serial port specified and no defaults to fall back on. Aborting.");
		  } else {
			System.out.println("WARNING: No serial port specified, defaulting to " + portPath);
		  }
	  }
	  Preferences.set("serial.port", portPath);
	  
	  // Check that a board target was specified for uploading
	  if (target == null) {
		  target = Preferences.get("serial.port");
		  if (target == null) {
			  complainAndQuit("No serial port specified and no defaults to fall back on. Aborting.");
		  } else {
			  System.out.println("WARNING: No serial port specified, defaulting to " + target);
		  }
	  }
	  Preferences.set("target", target);

    if (sketchFolder == null) {
      complainAndQuit("No sketch path specified.");
      
    } else if (outputPath.equals(pdePath)) {
      complainAndQuit("The sketch path and output path cannot be identical.");
      
    } else if (!pdePath.toLowerCase().endsWith(".pde")) {
      complainAndQuit("Sketch path must point to the main .pde file.");
      
    } else {
      boolean success = false;

      try {
		sketch = new Sketch(null, pdePath);
        if (mode == PREPROCESS) {
          success = sketch.preprocess(outputPath) != null;
			if (success) {
				statusNotice("Preprocessing complete.");
			} else {
				statusError("Preprocessing failed.");
			}
        } else if (mode == BUILD) {
          success = sketch.build(outputPath, true) != null;

        } else if (mode == UPLOAD) {
		  String className = sketch.build(outputPath, true);
          if (className != null) {
			  success = sketch.upload(outputPath, className, true) != null;

          }
        }
        System.exit(success ? 0 : 1);

      } catch (RunnerException re) {
        statusError(re);

      } catch (SerialException e) {
        statusError(e);
        System.exit(1);
        
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
  }


  public void statusError(String msg) {
    System.err.println(msg);
  }
	
	
  public void statusNotice(String msg) {
	System.out.println(msg);
  }


  public void statusError(Exception exception) {
    if (exception instanceof RunnerException) {
      RunnerException re = (RunnerException) exception;

      // format the runner exception like emacs
      //blah.java:2:10:2:13: Syntax Error: This is a big error message
      String filename = sketch.getCode(re.getCodeIndex()).getFileName();
      int line = re.getCodeLine();
      int column = re.getCodeColumn();
      if (column == -1) column = 0;
      // TODO if column not specified, should just select the whole line. 
      System.err.println(filename + ":" + 
                         line + ":" + column + ":" + 
                         line + ":" + column + ":" + " " + re.getMessage());
    } else {
      exception.printStackTrace();
    }
  }

  
  static void complainAndQuit(String lastWords) {
    System.err.println(lastWords);
    System.exit(1);
  }


  static void printCommandLine(PrintStream out) {
    out.println("Arduino " + Base.VERSION_NAME);
    out.println();
    out.println("--help               Show this help text.");
    out.println();
    out.println("--sketch=<name>      Specify the sketch folder (required)");
    out.println("--output=<name>      Specify the output folder (required and");
	out.println("                     cannot be the same as the sketch folder.)");
	out.println("--port=<name>        Specify the serial port to use for programming");
	out.println("                     (required for uploading)");
	out.println("--target=<name>      Specify the target board to use for programming");
	out.println("                     (required for uploading)");
    out.println();
    out.println("--preprocess         Preprocess a sketch into .c files.");
    out.println("--build              Preprocess and compile a sketch into a binary.");
    out.println("--upload             Preprocess, compile, and upload a sketch.");
    out.println("--preferences=<file> Specify a preferences file to use (optional).");
    out.println();
  }
}
