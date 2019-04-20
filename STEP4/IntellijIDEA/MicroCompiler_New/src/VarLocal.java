public class VarLocal
{
    private String varName = null;
    private String register = null;
    private int registerNum = -1;

    private static int maxRegNum = -1;

    public VarLocal(String varName, String register, int registerNum)
    {
        this.varName = varName;
        this.register = register;
        this.registerNum = registerNum;

        if (registerNum > maxRegNum)
        {
            maxRegNum = registerNum;
        }
    }

    public String getVarName()
    {
        return this.varName;
    }

    public String getRegister()
    {
        return this.varName;
    }

    public int getRegisterNum()
    {
        return this.registerNum;
    }

    public static int getMaxRegNum()
    {
        return maxRegNum;
    }
}
