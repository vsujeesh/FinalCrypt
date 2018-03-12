/*
 * Copyright © 2017 Ron de Jong (ronuitzaandam@gmail.com).
 *
 * This is free software; you can redistribute it 
 * under the terms of the Creative Commons License
 * Creative Commons License: (CC BY-NC-ND 4.0) as published by
 * https://creativecommons.org/licenses/by-nc-nd/4.0/; either
 * version 4.0 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * Creative Commons Attribution-NonCommercial-NoDerivatives 4.0
 * International Public License for more details.
 *
 * You should have received a copy of the Creative Commons 
 * Public License License along with this software;
 */
package rdj;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import static rdj.FinalCrypt.verbose;

public class Validate
{
    private static Path selectedCipherPath;
    public static long bytesCount;
    
    public static void checkCipher(UI ui, FinalCrypt finalcrypt, Path cipherPath)
    {
        State.cipherSelected = State.OBJECT;
        State.cipherReady = false;
	boolean device = true; long minSize = 1024; long cipherSize = 0; boolean symlink = false; boolean writable = false; boolean report = true;

	if (cipherPath.toAbsolutePath().toString().startsWith("/dev/"))
	{
	    if (cipherPath.toAbsolutePath().toString().startsWith("/dev/sd")) // Linux Cipher Device Selection
	    {
										minSize = 0;     symlink = false; writable = false; report = true;
		if (isValidFile(ui, "Validate.checkCipher", cipherPath, device, minSize,	 symlink,	  writable,	    report))
		{
//                  Get size of partition
//		    try (final SeekableByteChannel deviceChannel = Files.newByteChannel(cipherPath, EnumSet.of(StandardOpenOption.READ)))
//		    { cipherSize = deviceChannel.size(); deviceChannel.close(); } catch (IOException ex) { ui.status(ex.getMessage(), true); }
		    cipherSize = DeviceController.getDeviceSize(ui, new Device(ui, cipherPath));
		    
		    if (Character.isDigit(cipherPath.getFileName().toString().charAt(cipherPath.getFileName().toString().length()-1))) // Partition
		    {
			State.cipherReady = true;
			selectedCipherPath = cipherPath;
			State.cipherSelected = State.PARTITION;
			ui.status("Cipher " + State.getCipherSelectedDescription() + " " + cipherPath.toAbsolutePath().toString() + " " + getHumanSize(cipherSize,1) + " selected\r\n", false);			    
		    }
		    else
		    {
			State.cipherSelected = State.DEVICE;
			if ( ! cipherPath.getFileName().endsWith("sda"))
			{
			    State.cipherReady = true;
			    selectedCipherPath = cipherPath;
			    ui.status("Cipher " + State.getCipherSelectedDescription() + " " + cipherPath.toAbsolutePath().toString() + " " + getHumanSize(cipherSize,1) + " selected\r\n", false);
			}
			else
			{
			    ui.status("Cipher " + State.getCipherSelectedDescription() + " " + cipherPath.toAbsolutePath().toString() + " " + getHumanSize(cipherSize,1) + " validated\r\n", false);			    
			}
		    }
		}
		else { /*ui.status("Probably no read permission on " + cipherPath + " Run FinalCRypt with sudo (superuser) rights or execute: \"sudo usermod -a -G disk " + System.getProperty("user.name") + "\" and re-login your desktop and try again\r\n", true);*/ }
	    }
	    else if (cipherPath.toAbsolutePath().toString().startsWith("/dev/disk")) // Apple Cipher Device Selection
	    {
										    minSize = 0; symlink = false; writable = false; report = true;
		if (isValidFile(ui, "Validate.checkCipher", cipherPath, device, minSize,	 symlink,	  writable,	    report))
		{
//		    Get size of device        
//		    try (final SeekableByteChannel deviceChannel = Files.newByteChannel(cipherPath, EnumSet.of(StandardOpenOption.READ)))
//		    { cipherSize = deviceChannel.size(); deviceChannel.close(); } catch (IOException ex) { ui.status(ex.getMessage(), true); }
		    cipherSize = DeviceController.getDeviceSize(ui, new Device(ui, cipherPath));
		    
		    if (
			    (Character.isDigit(cipherPath.getFileName().toString().charAt(cipherPath.getFileName().toString().length()-1))) &&
			    (String.valueOf(cipherPath.getFileName().toString().charAt(cipherPath.getFileName().toString().length()-2)).equalsIgnoreCase("s"))
		       )
		    {
			State.cipherReady = true;
			selectedCipherPath = cipherPath;
			State.cipherSelected = State.PARTITION;
			ui.status("Cipher " + State.getCipherSelectedDescription() + " " + cipherPath.toAbsolutePath().toString() + " " + getHumanSize(cipherSize,1) + " selected\r\n", false);
		    }
		    else
		    {
			State.cipherSelected = State.DEVICE;
			if ( ! cipherPath.getFileName().toString().endsWith("disk0"))
			{
			    State.cipherReady = true;
			    selectedCipherPath = cipherPath;
			    ui.status("Cipher " + State.getCipherSelectedDescription() + " " + cipherPath.toAbsolutePath().toString() + " " + getHumanSize(cipherSize,1) + " selected\r\n", false);
			}
			else
			{
			    ui.status("Cipher " + State.getCipherSelectedDescription() + " " + cipherPath.toAbsolutePath().toString() + " " + getHumanSize(cipherSize,1) + " validated\r\n", false);
			}
		    }
		}
		else { /*ui.status("Probably no read permission on " + cipherPath + " Run FinalCRypt with sudo (superuser) rights or execute: \"sudo dseditgroup -o edit -a " + System.getProperty("user.name") + " -t user operator; sudo chmod g+w /dev/disk*\" and re-login your desktop and try again\r\n", true);*/ }
		
		
	    }
	} // Not a Device /dev/
//						    device minSize  symlink writable  report
	else if ( isValidFile(   ui, "", cipherPath, false,  1024L,   false,   false,   true) )
	{
	    State.cipherSelected = State.FILE;
	    State.cipherReady = true;
	    selectedCipherPath = cipherPath;
	    cipherSize = 0; try { cipherSize = Files.size(cipherPath.toAbsolutePath()); } catch (IOException ex) { ui.error("Error: Validate: IOException: else if ( isValidFile(..) " + ex.getLocalizedMessage() + "\r\n");	}
	    ui.status("Cipher " + State.getCipherSelectedDescription() + " " + cipherPath.toAbsolutePath().toString() + " " + getHumanSize(cipherSize,1) + " selected\r\n", false);
	}
	else
	{
	    State.cipherSelected = State.OBJECT;
	    State.cipherReady = false;
	    selectedCipherPath = null;
	}
	if ((cipherSize > 0) && (cipherSize < finalcrypt.getBufferSize())) { finalcrypt.setBufferSize((int)cipherSize); }
    }

    public static ArrayList<Path> getTargetList(UI ui, FinalCrypt finalcrypt, ArrayList<Path> targetPathList, Path cipherPath, String pattern, boolean negatePattern, boolean symlink, boolean status, boolean printgpt, boolean deletegpt)
    {
//	Test for Cipher Device Target
	ArrayList<Path> extendedPathList = new ArrayList<>(); extendedPathList.clear();
	boolean device = true; long minSize = 1024; long targetSize = 0;
	if ((targetPathList.size() == 1))
	{
	    if (targetPathList.get(0).toAbsolutePath().toString().startsWith("/dev/sd")) // Linux Cipher Device Device
	    {
		minSize = 0; boolean writable = false;
		if (isValidFile(ui, "Validate.checkTarget startsWith(\"/dev/sd\")", targetPathList.get(0), device, minSize, symlink, writable, true))
		{
//		    try (final SeekableByteChannel deviceChannel = Files.newByteChannel(targetPathList.get(0), EnumSet.of(StandardOpenOption.READ)))
//		    { targetSize = deviceChannel.size(); deviceChannel.close(); } catch (IOException ex) { ui.status(ex.getMessage(), true); }			
		    targetSize = DeviceController.getDeviceSize(ui, new Device(ui, targetPathList.get(0)));

		    if (Character.isLetter(targetPathList.get(0).getFileName().toString().charAt(targetPathList.get(0).getFileName().toString().length()-1))) // Device selected
		    {
			State.targetSelected = State.DEVICE;

			if  ( ! targetPathList.get(0).getFileName().toString().endsWith("sda")) // Not main disk
			{
			    State.targetReady = true;
			    extendedPathList.add(targetPathList.get(0));
			    ui.status("Target " + State.getTargetSelectedDescription() + " " + targetPathList.get(0).toAbsolutePath().toString() + " " + getHumanSize(targetSize,1) + " selected\r\n", true);
			    if (printgpt)   { DeviceManager deviceManager = new DeviceManager(ui); deviceManager.start(); deviceManager.printGPT(new Device(ui,targetPathList.get(0))); return extendedPathList; }
			    if (deletegpt)  { DeviceManager deviceManager = new DeviceManager(ui); deviceManager.start(); deviceManager.deleteGPT(new Device(ui,targetPathList.get(0))); return extendedPathList; }
			}
			else
			{
			    ui.status("Target " + State.getTargetSelectedDescription() + " " + targetPathList.get(0).toAbsolutePath().toString() + " " + getHumanSize(targetSize,1) + " validated\r\n", true);
			    if (printgpt)   { DeviceManager deviceManager = new DeviceManager(ui); deviceManager.start(); deviceManager.printGPT(new Device(ui,targetPathList.get(0))); return extendedPathList; }
			}
		    }
		    else
		    {
			State.targetSelected = State.PARTITION;
			ui.status("Target " + State.getTargetSelectedDescription() + " " + targetPathList.get(0).toAbsolutePath().toString() + " " + getHumanSize(targetSize,1) + " validated\r\n", true);
		    }
		}
		else { /*ui.status("Probably no read & write permission on " + targetPathList.get(0).toString() + " Run FinalCRypt with sudo (superuser) rights or execute: \"sudo usermod -a -G disk " + System.getProperty("user.name") + "\" and re-login your desktop and try again\r\n", true);*/ }
	    }
	    else if (targetPathList.get(0).toAbsolutePath().toString().startsWith("/dev/disk")) // Apple Cipher Device Device
	    {
		minSize = 0; boolean writable = true;
		if (isValidFile(ui, "Validate.checkTarget", targetPathList.get(0), device, minSize, symlink, writable, status))
		{
//		    try (final SeekableByteChannel deviceChannel = Files.newByteChannel(targetPathList.get(0), EnumSet.of(StandardOpenOption.READ)))
//		    { targetSize = deviceChannel.size(); deviceChannel.close(); } catch (IOException ex) { ui.status(ex.getMessage(), true); }
		    targetSize = DeviceController.getDeviceSize(ui, new Device(ui, targetPathList.get(0)));
		    
		    if (
			    (Character.isDigit(targetPathList.get(0).getFileName().toString().charAt(targetPathList.get(0).getFileName().toString().length()-1))) && // last char = digit
			    ( ! String.valueOf(targetPathList.get(0).getFileName().toString().charAt(targetPathList.get(0).getFileName().toString().length()-2)).equalsIgnoreCase("s")) // ! slice
		       ) 
		    {
			State.targetSelected = State.DEVICE;			
			if ( ! targetPathList.get(0).getFileName().toString().endsWith("disk0")) // not primary disk
			{
			    State.targetReady = true;
			    extendedPathList.add(targetPathList.get(0));
			    ui.status("Target " + State.getTargetSelectedDescription() + " " + targetPathList.get(0).toAbsolutePath().toString() + " " + getHumanSize(targetSize,1) + " selected\r\n", true);
			    if (printgpt)   { DeviceManager deviceManager = new DeviceManager(ui); deviceManager.start(); deviceManager.printGPT(new Device(ui,targetPathList.get(0))); return extendedPathList; }
			    if (deletegpt)  { DeviceManager deviceManager = new DeviceManager(ui); deviceManager.start(); deviceManager.deleteGPT(new Device(ui,targetPathList.get(0)));  return extendedPathList; }
			}
			else
			{
			    ui.status("Target " + State.getTargetSelectedDescription() + " " + targetPathList.get(0).toAbsolutePath().toString() + " " + getHumanSize(targetSize,1) + " validated\r\n", true);
			    if (printgpt)   { DeviceManager deviceManager = new DeviceManager(ui); deviceManager.start(); deviceManager.printGPT(new Device(ui,targetPathList.get(0))); return extendedPathList; }
			}
		    }
		    else
		    {
			State.targetSelected = State.PARTITION;
			ui.status("Target " + State.getTargetSelectedDescription() + " " + targetPathList.get(0).toAbsolutePath().toString() + " " + getHumanSize(targetSize,1) + " validated\r\n", true);
		    }
		}
		else { /*ui.status("Probably no read & write permission on " + targetPathList.get(0) + " Run FinalCRypt with sudo (superuser) rights or execute: \"sudo dseditgroup -o edit -a " + System.getProperty("user.name") + " -t user operator; sudo chmod g+w /dev/disk*\" and re-login your desktop and try again\r\n", true);*/ }
	    }
	    else // No Cipher Device Device Target selected
	    {
		State.targetSelected = State.OBJECT;
		State.targetReady = false;                
	    }
	}

//      En/Disable hasEncryptableItems
	if (( State.targetSelected != State.DEVICE ) && ( State.targetSelected != State.PARTITION ) && ( State.cipherReady )) // No need to scan for encryptable items without selected cipher for better performance
	{
	    minSize = 1;
	    boolean writable = true;
	    boolean report = false;
	    if (( targetPathList.size() == 1 ) )
	    {
		if  ( (Files.isDirectory(targetPathList.get(0))) && (isValidDir(ui, targetPathList.get(0), symlink, true)) )
		{
		    ui.status("Scanning selection... \r\n", true);
//				       getExtendedPathList(UI ui, ArrayList<Path> userSelectedItemsPathList,    Path cipherPath, long minSize, boolean symlink, boolean writable, String pattern, boolean negatePattern, boolean status)
		    extendedPathList = getExtendedPathList(   ui,                            targetPathList, selectedCipherPath,      minSize,         symlink,             true,        pattern,         negatePattern,          false);
		    if ( extendedPathList.size() > 0 )
		    {
			State.targetReady = true;
			State.targetSelected = State.FILE;
			ui.status("Targets selected: " + extendedPathList.size() + " (" + getHumanSize(bytesCount,1) +")"+ "\r\n", true);
		    }
		    else
		    {
			State.targetSelected = State.OBJECT;
			State.targetReady = false;
			ui.status("Nothing selected\r\n", true);
		    }
		} //                     "Validate.checkTarget singlefile"
		else if ( (! Files.isDirectory(targetPathList.get(0))) && (isValidFile(ui, "vcsf", targetPathList.get(0), selectedCipherPath, false, minSize, symlink, writable, true)) )
		{
		    State.targetReady = true;
		    State.targetSelected = State.FILE;
		    extendedPathList.add(targetPathList.get(0));

		    targetSize = 0; try { targetSize = Files.size(targetPathList.get(0).toAbsolutePath()); bytesCount += targetSize; } catch (IOException ex) { ui.error("Error: Validate: IOException: Files.size(targetPathList.get(0) " + ex.getLocalizedMessage() + "\r\n");	}
		    ui.status("Target " + State.getTargetSelectedDescription() + " " + targetPathList.get(0).toAbsolutePath().toString() + " " + getHumanSize(targetSize,1) + " selected\r\n", true);
		}
		else
		{
		    State.targetSelected = State.OBJECT;
		    State.targetReady = false;
		    ui.status("Nothing selected\r\n", true);
		}
	    }
	    else if ( targetPathList.size() > 1 )
	    {
		ui.status("Scanning selections... \r\n", true);
//				   getExtendedPathList(UI ui, ArrayList<Path> userSelectedItemsPathList,     Path cipherPath, long minSize, boolean symlink, boolean writable, String pattern, boolean negatePattern, boolean status)
		extendedPathList = getExtendedPathList(   ui,                            targetPathList,  selectedCipherPath,      minSize,         symlink,             true,        pattern,         negatePattern,           true);
		if ( extendedPathList.size() > 0 )
		{
		    State.targetReady = true;
		    State.targetSelected = State.FILE;
		    ui.status("Targets selected: " + extendedPathList.size() + " (" + getHumanSize(bytesCount,1) +")"+ "\r\n", true);
		}
		else
		{
		    State.targetReady = false;
		    State.targetSelected = State.OBJECT;
		    ui.status("Nothing selected\r\n", true);
		}
	    }
	    else
	    {
		State.targetReady = false;
		State.targetSelected = State.OBJECT;
		ui.status("Nothing selected\r\n", true);
	    }
	}
	return extendedPathList;
    }

    synchronized public static boolean isValidDir(UI ui, Path targetDirPath, boolean symlink, boolean report)
    {
        boolean validdir = true; String conditions = "";				    String exist = ""; String read = ""; String write = ""; String symbolic = "";
        if ( ! Files.exists(targetDirPath))						    { validdir = false; exist = "[not found] "; conditions += exist; }
        if ( ! Files.isReadable(targetDirPath) )					    { validdir = false; read = "[not readable] "; conditions += read;  }
        if ( ! Files.isWritable(targetDirPath) )					    { validdir = false; write = "[not writable] "; conditions += write;  }
        if ( (! symlink) && (Files.isSymbolicLink(targetDirPath)) )			    { validdir = false; symbolic = "[symlink]"; conditions += symbolic;  }
//        if ( validdir ) {  } else { if ( report )					    { ui.error("Warning: Validate.isValidDir: " + targetDirPath.toString() + ": " + conditions + "\r\n"); } }
        if ( ! validdir )								    { if ( report ) { ui.status("Warning: " + targetDirPath.toString() + ": " + conditions + "\r\n", true); } }
        return validdir;
    }

    synchronized public static boolean isValidFile(UI ui, String caller, Path targetpath, Path cipherPath, boolean device, long minSize, boolean symlink, boolean writable, boolean report) // fileValidation Wrapper (including target==cipherSource comparison)
    {
	
        boolean validfile = true; String conditions = "";				    String cipher = "";
	validfile = isValidFile(ui, caller, targetpath, device, minSize, symlink, writable, report);
	if ((cipherPath != null) && validfile) { if (targetpath.compareTo(cipherPath) == 0) { validfile = false; cipher = "[is cipher] "; conditions += cipher; }}	
        if ( ! validfile ) { if ( report )						    { ui.status("Warning: " + targetpath.toString() + ": " + conditions + "\r\n", true); } }                    
        return validfile;
    }

    synchronized public static boolean isValidFile(UI ui, String caller, Path path, boolean device, long minSize, boolean symlink, boolean writable, boolean report)
    {
        boolean validfile = true; String conditions = "";				    String size = ""; String exist = ""; String dir = ""; String read = ""; String write = ""; String symbolic = ""; String cipher = "";

        if ( ! Files.exists(path))							    { validfile = false; exist = "[not found] "; conditions += exist; }
        else
        {
            if ( Files.isDirectory(path))						    { validfile = false; dir = "[is directory] "; conditions += dir; }
	    long fileSize = 0; if ( device )						    { fileSize = 0; fileSize = DeviceController.getDeviceSize(ui, new Device(ui, path)); }
	    else									    { fileSize = 0; try { fileSize = Files.size(path); } catch (IOException ex)  { ui.error("Error: Validate: IOException: Files.size(" + path.toString() + ") Size: " + fileSize + "<" + minSize + " "+ ex.getMessage() + "\r\n"); } }
            if ( fileSize < minSize )							    { validfile = false; size = path.toString() + " smaller than " + minSize + " byte "; conditions += size; }
            if ( ! Files.isReadable(path) )						    { validfile = false; read = "[not readable] "; conditions += read; }
            if ((writable) && ( ! Files.isWritable(path)))				    { validfile = false; write = "[not writable] "; conditions += write; }
            if ( (! symlink) && (Files.isSymbolicLink(path)) )				    { validfile = false; symbolic = "[symlink] "; conditions += symbolic; }
        }
        
	if ( ! validfile )
	{ 
	    if ( report )
//	    { ui.error("Warning: Validate.isValidFile(...): " + caller + " Invalid File: " + targetSourcePath.toAbsolutePath().toString() + ": " + conditions + "\r\n"); } 
//	    { ui.status("Warning: " + caller + " " + path.toString() + ": " + conditions + "\r\n", true); } 
	    { ui.status("Warning: " + path.toString() + ": " + conditions + "\r\n", true); } 
	}                    
        return validfile;
    }
    
    public static ArrayList<Path> getExtendedPathList(UI ui, ArrayList<Path> userSelectedItemsPathList, Path cipherPath, long minSize, boolean symlink, boolean writable, String pattern, boolean negatePattern, boolean status)
    {
        // Converts from File[] to ArraayList<Path> where as every dir is converted into additional PathLists
        ArrayList<Path> recursivePathList = new ArrayList<>();
        if ( cipherPath == null )
        {
            for (Path outerpath:userSelectedItemsPathList)
            {
                if ( Files.isDirectory(outerpath) )
                {
                    for (Path path:getDirectoryPathList(ui, outerpath.toFile(), minSize, symlink, writable, pattern, negatePattern))
                    {
                        recursivePathList.add(path);
                    }
                }
                else
                {
                    recursivePathList.add(outerpath);
                }
            }
        }
        else
        {
            for (Path path:userSelectedItemsPathList)
            {
                if ( Files.isDirectory(path) )
                {
                    for (Path subItemPath:getDirectoryPathList(ui, path.toFile(), minSize, symlink, writable, pattern, negatePattern))
                    {
                        // cipherdetection not shown?
                        if ( ((subItemPath.toAbsolutePath().compareTo(cipherPath.toAbsolutePath()) != 0)) ) { recursivePathList.add(subItemPath); } else { if (status) { ui.status("Warning: cipher-file: " + cipherPath.toAbsolutePath() + " will be excluded!\r\n", true); }}
                    }
                }
                else
                {
                    if ( ((path.compareTo(cipherPath) != 0)) )
		    {
			recursivePathList.add(path);
			try { bytesCount += Files.size(path); } catch (IOException ex) { ui.error("Error: IOException: bytesCount += Files.size(path) " + ex.getMessage() + "\r\n"); }
		    }
		    else { if (status) { ui.status("Warning: cipher-file: " + cipherPath.toAbsolutePath() + " will be excluded!\r\n", true); }}
                }
            }
        }
	
        return recursivePathList;
    }

    // Used by getExtendedPathList(File[] files)
    public static ArrayList<Path> getDirectoryPathList(UI ui,File file, long minSize, boolean symlink, boolean writable, String pattern, boolean negatePattern)
    {
        // Converts from File[] to ArraayList<Path> where as every dir is converted into additional PathLists
        ArrayList<Path> recursivePathList = new ArrayList<>();
        
        EnumSet opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS); //follow links
//						      MySimpleFileVisitor(UI ui, boolean verbose, boolean delete, long minSize, boolean symlink, boolean writable, boolean returnpathlist, String pattern, boolean negatePattern)
        MySimpleFileVisitor mySimpleFileVisitor = new MySimpleFileVisitor(   ui,	 verbose,         false,       minSize,         symlink,             true,                   true,        pattern,         negatePattern);
        try{Files.walkFileTree(file.toPath(), opts, Integer.MAX_VALUE, mySimpleFileVisitor);} catch(IOException e){System.err.println(e);}
        recursivePathList = mySimpleFileVisitor.getPathList();
	
	bytesCount += mySimpleFileVisitor.bytesCount;

        return recursivePathList;
    }
    
    synchronized public static String getHumanSize(double value,int decimals)
    {
        int x = 0;
        long factor;
        double newValue = value;
        String returnString = new String("");
        ArrayList<String> magnitude = new ArrayList<String>(); magnitude.addAll(Arrays.asList("ZiB","EiB","PiB","TiB","GiB","MiB","KiB","Bytes"));
        for (factor = 70; factor > 0; factor -= 10)
        {
            if ((value / Math.pow(2, factor)) >= 1) { newValue = (value / Math.pow(2, factor)); returnString = String.format("%.1f", (newValue)) + " " + magnitude.get(x); break; } x++;
        }
        if (factor == 0) { newValue = (value / Math.pow(2, factor)); returnString = String.format("%." + decimals + "f", (newValue)) + " " + magnitude.get(x); }
        return returnString;
    }
    
}

// override only methods of our need (SimpleFileVisitor is a full blown class)
class MySimpleFileVisitor extends SimpleFileVisitor<Path>
{
    private final UI ui;
    private final PathMatcher pathMatcher;
    private final boolean verbose; 
    private final boolean delete; 
    private final long minSize; 
    private final boolean symlink; 
    private final boolean writable; 
    private final boolean returnpathlist; 
    private final ArrayList<Path> pathList;
    private boolean negatePattern;
    public long bytesCount = 0;

//  Default CONSTRUCTOR

//  regex pattern
//  all *.bit   =   'regex:^.*\.bit$'
//  all but *.bit   'regex:(?!.*\.bit$)^.*$'
    
    public MySimpleFileVisitor(UI ui, boolean verbose, boolean delete, long minSize, boolean symlink, boolean writable, boolean returnpathlist, String pattern, boolean negatePattern)
    {
        this.ui = ui;
        pathMatcher = FileSystems.getDefault().getPathMatcher(pattern); // "glob:" or "regex:" included in pattern
        this.verbose = verbose;
        this.delete = delete;
        this.minSize = minSize;
        this.symlink = symlink;
        this.writable = writable;
        this.returnpathlist = returnpathlist;
        pathList = new ArrayList<Path>();
        this.negatePattern = negatePattern;
	bytesCount = 0;
    }
   
    @Override public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs)
    {
        if ( Validate.isValidDir(ui, path, symlink, true) ) { return FileVisitResult.CONTINUE; } else { return FileVisitResult.SKIP_SUBTREE; }
    }
    
    @Override public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
    {
        long fileSize = 0; try { fileSize = Files.size(path); } catch (IOException ex) { }
        if (!negatePattern)
        {
            if ( (path.getFileName() != null ) && ( pathMatcher.matches(path.getFileName())) )
            {            
                if (delete)                 { try { Files.delete(path); } catch (IOException ex) { ui.error("Error: visitFile(.. ) Failed file: " + path.toString() + " due to: " + ex.getMessage() + "\r\n"); } }
                else if (returnpathlist)    
                {
//				  isValidFile(UI ui,                   String caller,	Path targetSourcePath, boolean device, long minSize, boolean symlink, boolean writable, boolean report)
                    if ( Validate.isValidFile(   ui, "MySimpleFileVisitor.visitFile",                    path,          false, this.minSize,   this.symlink,     this.writable,   this.verbose) )
		    {
			pathList.add(path);
			try { bytesCount += Files.size(path); } catch (IOException ex) { ui.error("Error: FileVisitResult IOException: Files.size() " + ex.getMessage() + "\r\n"); }
		    } 
                }
                else { ui.status("Huh? this shouldn't have happened. Neither booleans: delete & returnpathlist are present?\r\n", true); }
            }   
        }
        else
        {
            if ( (path.getFileName() != null ) && ( ! pathMatcher.matches(path.getFileName())) ) // Negate Pattern; Does NOT match pattern
            {
                if (delete)                 { try { Files.delete(path); } catch (IOException ex) { ui.error("Error: visitFile(.. ) Failed file: " + path.toString() + " due to: " + ex.getMessage() + "\r\n"); } }
                else if (returnpathlist)
                {
//				    isValidFile(UI ui,                   String caller,	Path targetSourcePath, boolean device, long minSize, boolean symlink, boolean writable, boolean report)
                    if ( Validate.isValidFile(     ui, "MySimpleFileVisitor.visitFile",                  path,          false, this.minSize,    this.symlink,    this.writable, this.verbose))
		    {
			pathList.add(path);
			try { bytesCount += Files.size(path); } catch (IOException ex) { ui.error("Error: FileVisitResult IOException: Files.size() " + ex.getMessage() + "\r\n"); }
		    }
                }
                else  { ui.status("Huh? this shouldn't have happened. Neither booleans: delete & returnpathlist are present?\r\n", true); }
            }   
        }
        return FileVisitResult.CONTINUE;
    }
    
    @Override public FileVisitResult visitFileFailed(Path path, IOException exc)
    {
        ui.error("Warning: Skip File: " + path.toAbsolutePath().toString() + ": " + exc + "\r\n");
        return FileVisitResult.SKIP_SIBLINGS;
    }
    
    @Override public FileVisitResult postVisitDirectory(Path path, IOException exc)
    {
        if      (delete)            { try { Files.delete(path); } catch (IOException ex) { ui.error("Error: postVisitDirectory: " + path.toString() + " due to: " + ex.getMessage() + "\r\n"); } }
        else if (returnpathlist)    {        }
        else                        {     }
        return FileVisitResult.CONTINUE;
    }
    
    public ArrayList<Path> getPathList() { return pathList; }
}