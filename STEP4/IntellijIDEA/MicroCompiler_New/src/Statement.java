/**
 * This class is used to store conditions and statement under specific symbol table.
 */
public class Statement
{
    private String currentSymbolTableName = null;
    private String labelName = null;

    private boolean isCondition = false;
    private String statement = null;

    private boolean isBeginningOfBlock = false;

    private boolean isWrite = false;
    private boolean isRead = false;

    /**
     * Constructor
     * @param currentSymbolTableName
     * @param labelName
     * @param isCondition
     * @param statement
     * @param isBeginningOfBlock
     */
    public Statement(String currentSymbolTableName, String labelName, boolean isCondition, String statement, boolean isBeginningOfBlock)
    {
        this.currentSymbolTableName = currentSymbolTableName;
        this.labelName = labelName;
        this.isCondition = isCondition;
        this.statement = statement;
        this.isBeginningOfBlock = isBeginningOfBlock;
    }

    public String getCurrentSymbolTableName()
    {
        return this.currentSymbolTableName;
    }

    public String getLabelName()
    {
        return this.labelName;
    }

    public boolean isCondition()
    {
        return this.isCondition;
    }

    public String getStatement()
    {
        return this.statement;
    }

    public boolean isBeginningOfBlock()
    {
        return this.isBeginningOfBlock;
    }

    public void setIsWrite(boolean isWrite)
    {
        this.isWrite = isWrite;
    }

    public void setIsRead(boolean isRead)
    {
        this.isRead = isRead;
    }

    public boolean isWrite()
    {
        return this.isWrite;
    }

    public boolean isRead()
    {
        return this.isRead;
    }
}
