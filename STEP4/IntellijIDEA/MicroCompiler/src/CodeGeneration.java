import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to generate tiny code
 */
public class CodeGeneration
{
    // Symbol table
    private Map<Integer, MicroSymbolTable> mst = null;

    /**
     * Constructor
     * @param mst
     */
    public CodeGeneration(Map<Integer, MicroSymbolTable> mst)
    {
        this.mst = mst;
    }

    /**
     * This method is used to print each block of the source code from the symbol table,
     * also is used to generate tiny code.
     * Currently in the demo stage.
     */
    public void demoPrint()
    {
        // Check if symbol table get duplicate identifiers
        // if there are duplicate identifiers, then print and error message
        Map<String, MicroSymbolTable> duplicates = duplicateCheck(mst);
        if (duplicates != null)
        {
            for (Map.Entry<String, MicroSymbolTable> entry : duplicates.entrySet())
            {
                System.out.printf("DECLARATION ERROR %s\n", entry.getKey());
            }
        }

        // If no duplicate identifiers print symbol table
        if (duplicates == null)
        {
            // This counter used to specify where we should print extra new-line
            int countSymTable = 0;

            for (Map.Entry<Integer, MicroSymbolTable> entry : mst.entrySet())
            {
                int symbolTableID = entry.getValue().getSymbolTableId();
                String symbolTable = entry.getValue().getSymbolTableName();

                String name = entry.getValue().getName();
                String type = entry.getValue().getType();
                String value = entry.getValue().getValue();

                String nl = "";

                // Checking if we need to print extra newline
                // ================================================================================= //
                if (symbolTable != null)
                {
                    // Printing extra new-line or not
                    if(countSymTable == 0)
                    {
                        nl = "";
                    }
                    else if(countSymTable > 0)
                    {
                        nl = "\n";
                    }
                    countSymTable++;

                    // Printing symbol-table
                    System.out.printf("%s[%d]: %s\n", nl, symbolTableID, symbolTable);

                    // Printing statements
                    // ---------------------------------------------------------------------------------- //
                    if(entry.getValue().getStatementObj() != null)
                    {
                        String lable = entry.getValue().getStatementObj().getLableName();
                        String statement = null;
                        if(entry.getValue().getStatementObj().isCondition())
                        {
                            statement = "condition";
                        }
                        else
                        {
                            statement = "assignment";
                        }
                        String strOut = entry.getValue().getStatementObj().getStatement();

                        boolean isBeginningOfBlock = entry.getValue().getStatementObj().isBeginningOfBlock();
                        System.out.printf("\t\tBLOCK BEGINNING: %b\n", isBeginningOfBlock);

                        System.out.printf("\t\t%s --- %s: %s\n", lable, statement, strOut);
                    }
                    // ---------------------------------------------------------------------------------- //
                }
                // ================================================================================= //

                // Printing strings declarations
                // This IF block, is for STRING type because usually STRING go with some value
                // ---------------------------------------------------------------------------------- //
                if (name != null && type != null && value != null)
                {
                    String str = "name " + name + " type " + type + " value " + value;
                    //System.out.printf("[%d]: %s\n", symbolTableID, str);

                    buildGlobalString(name, type, value);
                }
                // ---------------------------------------------------------------------------------- //

                // Printing variables declarations
                // This IF block, is for INT and FLOAT types
                // ---------------------------------------------------------------------------------- //
                if (name != null && type != null && value == null)
                {
                    String str = "name " + name + " type " + type;
                    //System.out.printf("[%d]: %s\n", symbolTableID, str);

                    buildGlobalVar(name, type);
                }
                // ---------------------------------------------------------------------------------- //
                // ================================================================================= //
            }
        }
    }

    private void buildGlobalString(String id, String type, String value)
    {
        String str = "str " + id + " " + value;
        System.out.printf("%s\n", str);
    }

    private void buildGlobalVar(String id, String type)
    {
        String str = "var " + id;
        System.out.printf("%s\n", str);
    }

    private void buildLabel(String symbolTableName, boolean isBeginningOfBlock)
    {

    }

    private void buildExpression(String expression, boolean isCondition, String blockName, boolean blockBeginning)
    {

    }

















    /**
     * Check for duplicate identifiers
     * @param tmp: Map<Integer, MicroSymbolTable>
     * @return Map<String, MicroSymbolTable>
     */
    private Map<String, MicroSymbolTable> duplicateCheck(Map<Integer, MicroSymbolTable> tmp)
    {
        Map<String, MicroSymbolTable> duplicates = null;

        int count = 0;

        for (Map.Entry<Integer, MicroSymbolTable> entry1 : tmp.entrySet())
        {
            String stn1 = entry1.getValue().getCurrentSymbolTableName();
            String n1 = entry1.getValue().getName();

            for (Map.Entry<Integer, MicroSymbolTable> entry2 : tmp.entrySet())
            {
                String stn2 = entry2.getValue().getCurrentSymbolTableName();
                String n2 = entry2.getValue().getName();

                if (n2 != null)
                {
                    if (stn2.equals(stn1) && n2.equals(n1))
                    {
                        count++;
                    }
                }
            }

            if (count >= 2)
            {
                if (duplicates == null)
                {
                    duplicates = new HashMap<String, MicroSymbolTable>();
                    duplicates.put(entry1.getValue().getName(), entry1.getValue());
                }
                else
                {
                    duplicates.put(entry1.getValue().getName(), entry1.getValue());
                }
            }
            count = 0;
        }

        return duplicates;
    }
}
