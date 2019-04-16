public class VarDeclaration
{
    private boolean isString = false;
    private String id = null;
    private String type = null;
    private String value = null;

    public VarDeclaration(String id, String type, String value, boolean isString)
    {
        this.id = id;
        this.type = type;
        this.value = value;
        this.isString = isString;
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
}
