public class MicroExpression
{
    private String varLeft = null;
    private String varRight = null;
    private String register = null; // this object will be stored under this register
    private String tinyCode = null;
    private String arithmeticSymbol = null;

    private int registerNum = -1;
    private static int maxRegNum = -1;

    public MicroExpression(String varLeft, String varRight, String register, String tinyCode, String arithmeticSymbol, int registerNum)
    {
        this.varLeft = varLeft;
        this.varRight = varRight;
        this.register = register;
        this.tinyCode = tinyCode;
        this.registerNum = registerNum;

        if (registerNum > maxRegNum)
        {
            maxRegNum = registerNum;
        }
    }

    public String getVarLeft()
    {
        return this.varLeft;
    }

    public String getVarRight()
    {
        return this.varRight;
    }

    public String getRegister()
    {
        return this.register;
    }

    public int getRegisterNum()
    {
        return this.registerNum;
    }

    public static int getMaxRegNum()
    {
        return maxRegNum;
    }

    public String getTinyCode()
    {
        return this.tinyCode;
    }

    public String getArithmeticSymbol()
    {
        return this.arithmeticSymbol;
    }
}
