/**
 * Montana State University
 * Class: Compilers - CSCI 468
 * @author Olexandr Matveyev, Mandy Hawkins, Abdulrahman Alhitm, Michael Seeley
 */

public class VarDeclaration
{
    private boolean isString = false;
    private String id = null;
    private String type = null;
    private String value = null;

    /*
    private static int registerMaxNum = 0;
    private static int registerCount = 0;
    private int registerNum = 0;
    private String register = null;
    */

    public VarDeclaration(String id, String type, String value, boolean isString)
    {
        this.id = id;
        this.type = type;
        this.value = value;
        this.isString = isString;

        /*
        this.register = "r" + registerCount;
        this.registerNum = registerCount;
        this.registerMaxNum = registerCount;
        registerCount++;
        */
    }

    public String getId()
    {
        return this.id;
    }

    public String getType()
    {
        return this.type;
    }

    public String getValue()
    {
        return this.value;
    }

    public boolean isString()
    {
        return this.isString;
    }

    /*
    public String getRegister()
    {
        return this.register;
    }

    public static int getRegisterMaxNum()
    {
        return registerMaxNum;
    }
    */
}
