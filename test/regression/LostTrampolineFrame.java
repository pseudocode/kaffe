
import java.util.Vector;

import java.io.File;
import java.io.FileInputStream;

/**
 * This class will check for a bug in the x86 trampoline (and possibly others)
 * that caused kaffe to ignore the stack frame of a routine that jumped through
 * a trampoline.  By "ignore", we mean that an exception handler in the calling
 * routine wasn't found and executed.
 */
class LostTrampolineFrame
    extends ClassLoader
{
    /**
     * We use a bad class to get the jitter to throw an exception through the
     * trampoline.  The following byte code was generated by disassembling the
     * DamagedClass class below, tweaking some values, and then reassembling
     * it.
     */
    public static byte damagedByteCode[] = {
        (byte)0xca, (byte)0xfe, (byte)0xba, (byte)0xbe,
        (byte)0x0, (byte)0x3, (byte)0x0, (byte)0x2d,
        (byte)0x0, (byte)0xd, (byte)0xa, (byte)0x0,
        (byte)0x4, (byte)0x0, (byte)0xa, (byte)0x1,
        (byte)0x0, (byte)0x4, (byte)0x43, (byte)0x6f,
        (byte)0x64, (byte)0x65, (byte)0x7, (byte)0x0,
        (byte)0x8, (byte)0x7, (byte)0x0, (byte)0x9,
        (byte)0x1, (byte)0x0, (byte)0x6, (byte)0x3c,
        (byte)0x69, (byte)0x6e, (byte)0x69, (byte)0x74,
        (byte)0x3e, (byte)0x1, (byte)0x0, (byte)0xa,
        (byte)0x53, (byte)0x6f, (byte)0x75, (byte)0x72,
        (byte)0x63, (byte)0x65, (byte)0x46, (byte)0x69,
        (byte)0x6c, (byte)0x65, (byte)0x1, (byte)0x0,
        (byte)0xf, (byte)0x4c, (byte)0x69, (byte)0x6e,
        (byte)0x65, (byte)0x4e, (byte)0x75, (byte)0x6d,
        (byte)0x62, (byte)0x65, (byte)0x72, (byte)0x54,
        (byte)0x61, (byte)0x62, (byte)0x6c, (byte)0x65,
        (byte)0x1, (byte)0x0, (byte)0x20, (byte)0x4c,
        (byte)0x6f, (byte)0x73, (byte)0x74, (byte)0x54,
        (byte)0x72, (byte)0x61, (byte)0x6d, (byte)0x70,
        (byte)0x6f, (byte)0x6c, (byte)0x69, (byte)0x6e,
        (byte)0x65, (byte)0x46, (byte)0x72, (byte)0x61,
        (byte)0x6d, (byte)0x65, (byte)0x24, (byte)0x44,
        (byte)0x61, (byte)0x6d, (byte)0x61, (byte)0x67,
        (byte)0x65, (byte)0x64, (byte)0x43, (byte)0x6c,
        (byte)0x61, (byte)0x73, (byte)0x73, (byte)0x1,
        (byte)0x0, (byte)0x10, (byte)0x6a, (byte)0x61,
        (byte)0x76, (byte)0x61, (byte)0x2f, (byte)0x6c,
        (byte)0x61, (byte)0x6e, (byte)0x67, (byte)0x2f,
        (byte)0x4f, (byte)0x62, (byte)0x6a, (byte)0x65,
        (byte)0x63, (byte)0x74, (byte)0xc, (byte)0x0,
        (byte)0x5, (byte)0x0, (byte)0xc, (byte)0x1,
        (byte)0x0, (byte)0x18, (byte)0x4c, (byte)0x6f,
        (byte)0x73, (byte)0x74, (byte)0x54, (byte)0x72,
        (byte)0x61, (byte)0x6d, (byte)0x70, (byte)0x6f,
        (byte)0x6c, (byte)0x69, (byte)0x6e, (byte)0x65,
        (byte)0x46, (byte)0x72, (byte)0x61, (byte)0x6d,
        (byte)0x65, (byte)0x2e, (byte)0x6a, (byte)0x61,
        (byte)0x76, (byte)0x61, (byte)0x1, (byte)0x0,
        (byte)0x3, (byte)0x28, (byte)0x29, (byte)0x56,
        (byte)0x0, (byte)0x21, (byte)0x0, (byte)0x3,
        (byte)0x0, (byte)0x4, (byte)0x0, (byte)0x0,
        (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x1,
        (byte)0x0, (byte)0x1, (byte)0x0, (byte)0x5,
        (byte)0x0, (byte)0xc, (byte)0x0, (byte)0x1,
        (byte)0x0, (byte)0x2, (byte)0x0, (byte)0x0,
        (byte)0x0, (byte)0x21, (byte)0x0, (byte)0x0,
        (byte)0x0, (byte)0x1, (byte)0x0, (byte)0x0,
        (byte)0x0, (byte)0x5, (byte)0x2a, (byte)0xb7,
        (byte)0x0, (byte)0x1, (byte)0xb1, (byte)0x0,
        (byte)0x0, (byte)0x0, (byte)0x1, (byte)0x0,
        (byte)0x7, (byte)0x0, (byte)0x0, (byte)0x0,
        (byte)0xa, (byte)0x0, (byte)0x2, (byte)0x0,
        (byte)0x0, (byte)0x0, (byte)0x53, (byte)0x0,
        (byte)0x4, (byte)0x0, (byte)0x54, (byte)0x0,
        (byte)0x1, (byte)0x0, (byte)0x6, (byte)0x0,
        (byte)0x0, (byte)0x0, (byte)0x2, (byte)0x0,
        (byte)0xb
    };

    /**
     * Read in the byte code for the given class.
     *
     * @param name The name of the class to read in.
     * @return The byte code for the class.
     */
    static byte [] readin(String name) throws Exception
    {
        File cf = new File(name);
        FileInputStream cfi = new FileInputStream(cf);

        int len = (int)cf.length();
        byte [] cb = new byte[len];
        if (cfi.read(cb) != len)
            throw new Exception("short read for " + name);
        return cb;
    }
    
    public synchronized Class loadClass(String name, boolean resolve)
	throws ClassNotFoundException
    {
	Class retval = null;

	if( !name.startsWith("LostTrampolineFrame") )
	{
	    /* System class... */
	    retval = super.findSystemClass(name);
	}
	else if( name.equals("LostTrampolineFrame$DamagedClass") )
	{
	    /* Load our damaged class file. */
	    retval = defineClass(name,
				 damagedByteCode,
				 0,
				 damagedByteCode.length);
	}
	else
	{
	    /* One of our class' and not the DamagedClass. */
	    try
	    {
		byte []b = readin(name + ".class");
		retval = defineClass(name, b, 0, b.length);
	    }
	    catch(Exception e)
	    {
		throw new ClassNotFoundException(name);
	    }
	}
	if( resolve )
	    super.resolveClass(retval);
	return retval;
    }

    /**
     * The original class that was used to generate damagedByteCode.  NOTE:
     * This is not actually used.
     */
    public static class DamagedClass
    {
	public DamagedClass()
	{
	}
    }

    /**
     * An intermediate class that will try to construct a DamagedClass object.
     */
    public static class IntermediateClass
	implements Runnable
    {
	public IntermediateClass()
	{
	}

	public void run()
	{
	    try
	    {
		/* The constructor shouldn't verify ... */
		new DamagedClass();
	    }
	    catch(VerifyError e)
	    {
		/*
		 * If the VerifyError isn't caught here, that means the
		 * trampoline is hiding the stack frame.
		 */
		System.out.println("Success");
	    }
	}
    }

    public static void main(String args[])
	throws Throwable
    {
	LostTrampolineFrame ltf;
	Runnable run;
	Class cl;
	
	ltf = new LostTrampolineFrame();
	cl = ltf.loadClass("LostTrampolineFrame$IntermediateClass");
	try
	{
	    run = (Runnable)cl.newInstance();
	    run.run();
	}
	catch(VerifyError e)
	{
	    /*
	     * If the VerifyError is caught here, that means the trampoline is
	     * hiding the stack frame.
	     */
	    System.out.println("Bad trampoline");
	}
    }
}

/* Expected Output:
Success
*/
