/**
 * Montana State University
 * Class: Compilers - CSCI 468
 * @author Olexandr Matveyev, Mandy Hawkins, Abdulrahman Alhitm, Michael Seeley
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

/**
 * This class is used to generate tiny code
 */
public class CodeGeneration
{
    // Assign operation
    private final String ASSIGN = ":=";

    // Symbol table
    private Map<Integer, MicroSymbolTable> mst = null;

    // This map will be used to identify variable types
    private Map<String, VarDeclaration> varMap = new HashMap<String, VarDeclaration>();

    // Is used to generate labels for tiny language
    private int labelCount = 1;

    // Stack of labels for WHILE BLOCK
    private Stack<String> labelsWhile = new Stack<String>();

    // Stack of labels for IF BLOCK
    private Stack<String> labelsIf = new Stack<String>();

    // Stack of labels for ELSE BLOCK, you can treat it as IF-ELSE BLOCK
    private Stack<String> labelsIfElse = new Stack<String>();

    private GenerateStatement gs = new GenerateStatement();

    // Is used to count inner IF BLOCKS
    private int numOf_inner_IFs = 0;

    /**
     * Constructor accepts MicroSymbolTable as argument
     * @param mst
     */
    public CodeGeneration(Map<Integer, MicroSymbolTable> mst)
    {
        this.mst = mst;
    }

    /**
     * This method is used to print each block of the source code from the symbol table,
     * also is used to partially generate tiny code.
     * Currently in the demo stage.
     */
    public void demo()
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
            for (Map.Entry<Integer, MicroSymbolTable> entry : mst.entrySet())
            {
                // Symbol-Table
                // ------------------------------------------------------------------------ //
                int symbolTableID = entry.getValue().getSymbolTableId();
                String symbolTableName = entry.getValue().getSymbolTableName();
                String label = entry.getValue().getLabel();
                // ------------------------------------------------------------------------ //

                // Variable declaration
                // ------------------------------------------------------------------------ //
                String name = entry.getValue().getName();
                String type = entry.getValue().getType();
                String value = entry.getValue().getValue();
                // ------------------------------------------------------------------------ //

                // PRINT GLOBAL DECLARATIONS
                // =================================================================================== //
                // Printing variables declarations
                // is for STRING, INT, and FLOAT types
                // ---------------------------------------------------------------------------------- //
                if (name != null && type != null)
                {
                    generateGlobalDeclarations(name, type, value);
                }
                // ---------------------------------------------------------------------------------- //
                // =================================================================================== //

                // PRINT SYMBOL TABLE NAME OR LABEL
                // =================================================================================== //
                if (label != null)
                {
                    generateLabel(null, label, false, null);
                    System.out.printf("\n");

                    // Print label stack of WHILE labels
                    // Visit this if statement just ones to print end of WHILE
                    if (label.equals("EXIT-WHILE"))
                    {
                        if (labelsWhile != null)
                        {
                            for (String s : labelsWhile)
                            {
                                System.out.printf("%s\n", s.toLowerCase());
                            }
                            labelsWhile = null;
                            System.out.printf("\n");
                        }
                    }

                }
                // =================================================================================== //

                // PRINT LOCAL DECLARATIONS
                // =================================================================================== //
                // Printing statements and sys-statements, such as READ or WRITE
                // ---------------------------------------------------------------------------------- //
                if(entry.getValue().getStatementObj() != null)
                {
                    generateLocalDeclarations(entry.getValue().getStatementObj(), symbolTableID, symbolTableName, label);
                    System.out.printf("\n");
                }

                // ---------------------------------------------------------------------------------- //
                // =================================================================================== //

                System.out.printf("\n");
            }
        }

        System.out.printf("sys halt\n");
    }

    /**
     * Generating GLOBAL STRING, INT, FLOAT declarations
     * @param id
     * @param type
     * @param value
     */
    private void generateGlobalDeclarations(String id, String type, String value)
    {
        varMap.put(id, new VarDeclaration(id, type, null, false));

        String str = null;
        if (type.equals("STRING"))
        {
            str = "str " + id + " " + value;
        }
        else
        {
            str = "var " + id;
        }
        System.out.printf("%s", str);
    }

    /**
     *  Partially generating local statements, such as expressions and conditions
     * @param statementObj
     * @param symbolTableID
     * @param symbolTableName
     * @param blockLabel
     */
    public void generateLocalDeclarations(Statement statementObj, int symbolTableID, String symbolTableName, String blockLabel)
    {
        // ------------------------------------------------------------------------------- //
        String labelTMP = statementObj.getLabelName();
        String statement = statementObj.getStatement();
        boolean isBeginningOfBlock = statementObj.isBeginningOfBlock();
        boolean isCondition = statementObj.isCondition();
        boolean isRead = statementObj.isRead();
        boolean isWrite = statementObj.isWrite();

        String statementType = null;
        // ------------------------------------------------------------------------------- //


        // Currently used just for testing purpose
        // ------------------------------------------------------------------------------- //
        if(isCondition)
        {
            statementType = "condition";
        }
        else
        {
            statementType = "assignment";
        }
        // ------------------------------------------------------------------------------- //

        // Used for READ or WRITE, or to print statements: conditions and assignments
        // ------------------------------------------------------------------------------- //
        if (isRead || isWrite)
        {
            // Used to print sys-input and sys-output, such as READ or WRITE

            if (isRead) { labelTMP = "READ"; }
            else if (isWrite) { labelTMP = "WRITE"; }
            generateSys(labelTMP, statement, isBeginningOfBlock, isRead, isWrite);
        }
        else
        {
            // Used for statements, such as conditions and assignments
            buildStatement(labelTMP, isCondition, statement, statementType, isBeginningOfBlock);
        }
        // ------------------------------------------------------------------------------- //
    }

    /**
     * Generating Label
     * @param symbolTableName
     * @param lable
     * @param isCondition
     * @param statement
     */
    private void generateLabel(String symbolTableName, String lable, boolean isCondition, String statement)
    {
        // Print labels or block names
        // ---------------------------------------------------------------------------- //
        if (!lable.equals("GLOBAL") && !isCondition)
        {
            if (!lable.equals("EXIT-IF") && !lable.equals("EXIT-ELSE"))
            {
                String str = "label " + lable;
                System.out.printf("%s", str);
            }
        }
        // ---------------------------------------------------------------------------- //

        // IF
        // ---------------------------------------------------------------------------- //
        if (lable.equals("IF") && isCondition)
        {
            String condStnt = statement;

            // GENERATE LABELS
            // ----------------------------------------------------------- //
            String lblElse = "label" + labelCount;
            labelCount++;

            String lblExit = "label" + labelCount;
            labelCount++;
            // ----------------------------------------------------------- //

            // push to the stack
            // This stack is used when we exit from ELSE BLOCK
            // ----------------------------------------------------------- //
            if (labelsIfElse != null)
            {
                labelsIfElse.push(lblExit);
                labelsIfElse.push(lblElse);
            }
            else
            {
                labelsIfElse = new Stack<String>();
                labelsIfElse.push(lblExit);
                labelsIfElse.push(lblElse);
            }
            // ----------------------------------------------------------- //


            // push to the stack
            // This stack is used when we exit from IF BLOCK
            // ----------------------------------------------------------- //
            if (labelsIf != null)
            {
                labelsIf.push(lblElse);
            }
            else
            {
                labelsIf = new Stack<String>();
                labelsIf.push(lblElse);
            }
            // ----------------------------------------------------------- //

            // -------------------------------------------------------------------------------- //
            // Here we should call "gs.buildCondition()" function to generate condition
            // and after we have to generate labels based on current IF, ELSE or WHILE BLOCK,
            // and based on current Condition
            // -------------------------------------------------------------------------------- //

            // DEMO
            // -------------------------------------------------------------------------------- //
            gs.updateData(condStnt, varMap);
            gs.buildCondition();
            condStnt = gs.getCondBody();
            // -------------------------------------------------------------------------------- //

            String output = condStnt + "\n" + gs.getJumpName() + " " + lblElse + "\nif-body";
            System.out.printf("%s\n", output);

            // At some point this wearable used to identify multiple IF BLOCKS
            numOf_inner_IFs++;
        }
        // ---------------------------------------------------------------------------- //

        // EXIT-IF
        // ---------------------------------------------------------------------------- //
        if (lable.equals("EXIT-IF") && !isCondition)
        {
            System.out.printf("%s\n", "EXIT-IF");

            // ----------------------------------------------------------- //
            // If no inner IFs "numOf_inner_IFs" should be less than 2
            // and we have to zero this variable
            if (numOf_inner_IFs < 2)
            {
                numOf_inner_IFs = 0;
            }

            // If no inner IFs than "numOf_inner_IFs" should be zero
            // and we have to rebuild "labelsIfElse" after in next IF BLOCK
            if (numOf_inner_IFs == 0)
            {
                labelsIfElse = null;
            }
            // ----------------------------------------------------------- //


            // print from stack
            // -------------------------------------------------------------------- //
            if (labelsIf != null)
            {
                String str[] = new String[labelsIf.size()];
                String lblExit = null;
                int count = 0;

                // Getting values from stack for if-labels
                for (String s : labelsIf)
                {
                    str[count] = s;
                    count++;
                }
                labelsIf = null;

                // If we have more than one IF BLOCK
                // we have to use value from stack under specific index;
                // Such approach will work only with one inner IF BLOCK
                if (numOf_inner_IFs >= 1)
                {
                    // Sometimes labelsIf can have only one element even if we have more
                    // than one IF BLOCK it depends in which order we have IF-ELSE and IF BLOCKS
                    // , so we have to test it out
                    if (count > 1)
                    {
                        lblExit = str[1];
                    }
                    else
                    {
                        lblExit = str[0];
                    }
                }
                else if (numOf_inner_IFs < 1)
                {
                    lblExit = str[0];
                }


                String output = "label " + lblExit + "\n";
                System.out.printf("\n%s\n", output);
            }
            // -------------------------------------------------------------------- //
        }
        // ---------------------------------------------------------------------------- //

        // ELSE
        // ---------------------------------------------------------------------------- //
        if (lable.equals("ELSE") && !isCondition)
        {
            // print from stack
            if (labelsIfElse != null)
            {
                String str[] = new String[labelsIfElse.size()];
                String lblElse = null;
                String lblExit = null;
                int count = 0;

                // Get values from stack
                // Tha labels generated in the IF BLOCK should be at the beginning of stack,
                // so we taking only first two elements
                // ------------------------------------------------------- //
                for (String s : labelsIfElse)
                {
                    str[count] = s;
                    if (count >= 1)
                    {
                        break;
                    }
                    count++;
                }
                labelsIfElse = null;
                // ------------------------------------------------------- //

                // If in the Micro source code we had inner IF BLOCK
                // it will change order of labels in the stack of else-labels
                // and we have to retrieve it in specific way
                // ------------------------------------------------------- //
                if (numOf_inner_IFs >= 1)
                {
                    lblExit = str[0];
                    lblElse = str[1];
                }
                else if (numOf_inner_IFs < 1)
                {
                    lblElse = str[1];
                    lblExit = str[0];
                }
                // ------------------------------------------------------- //

                // Build again label for if to exit
                // It will be used after we exit from IF BLOCK
                // ------------------------------------------------------- //
                labelsIf = null;
                labelsIf = new Stack<String>();
                labelsIf.push(lblExit);
                // ------------------------------------------------------- //

                String output = "jmp " + lblExit + "\nlabel " + lblElse + "\n" + "else-body" + "\n";
                System.out.printf("\n%s\n", output);
            }
        }
        // ---------------------------------------------------------------------------- //

        // EXIT-ELSE
        // ---------------------------------------------------------------------------- //
        if (lable.equals("EXIT-ELSE") && !isCondition)
        {
            //System.out.printf("%s\n", "EXIT-ELSE");
        }
        // ---------------------------------------------------------------------------- //

        // WHILE
        // ---------------------------------------------------------------------------- //
        if (lable.equals("WHILE") && isCondition)
        {
            String condStnt = statement;

            String lblLoop = "label" + labelCount;
            labelCount++;

            String lblExit = "label" + labelCount;
            labelCount++;


            // Push to the stack "continue" and "exit" labels
            String lblContinue = "jmp " + lblLoop;
            labelsWhile.push(lblContinue);
            labelsWhile.push( ("label " + lblExit) );

            // -------------------------------------------------------------------------------- //
            // Here we should call "generateCondition" function to generate condition
            // and after we have to generate labels based on current IF, ELSE or WHILE BLOCK,
            // and based on current Condition

            gs.updateData(condStnt, varMap);
            gs.buildCondition();
            condStnt = gs.getCondBody();
            // -------------------------------------------------------------------------------- //

            String output = "label " + lblLoop + "\n" + condStnt + "\n" + gs.getJumpName() + " " + lblExit;
            System.out.printf("%s\n", output);
        }
        // ---------------------------------------------------------------------------- //
    }

    /**
     * Generating Condition or Expression
     * @param label
     * @param isCondition
     * @param statement
     * @param statementType
     * @param isBeginningOfBlock
     */
    private void buildStatement(String label, boolean isCondition, String statement, String statementType, boolean isBeginningOfBlock)
    {
        if (isCondition)
        {
            // Moved to generate labels
            // -------------------------------------------------------------------------------- //
            // Here we should call "generateCondition" function to generate condition
            // and after we have to generate labels based on current IF, ELSE or WHILE BLOCK,
            // and based on current Condition
            // -------------------------------------------------------------------------------- //

            generateLabel(null, label, isCondition, statement);
        }
        else
        {
            // -------------------------------------------------------------------------------- //
            // Here we should call "gs.buildAssignmentExpression()" function
            // to generate assignment statement
            // and after just print it out

            String assignmentBody = null;
            gs.updateData(statement, varMap);
            gs.buildAssignmentExpression();
            if (gs.getAssignmentBody() == null)
            {
                assignmentBody = statement;
            }
            else
            {
                assignmentBody = gs.getAssignmentBody();
            }
            // -------------------------------------------------------------------------------- //

            //String str = statementType + " ::: " + statement;
            System.out.printf("%s", assignmentBody);
        }

        // Currently used for testing purpose
        // If end of function
        if (label.equals("MAIN-WRITE") && !isBeginningOfBlock)
        {
            System.out.printf("END OF FUNCTION\n");
        }
    }

    /**
     * Generating sys: READ or WRITE
     * @param lable
     * @param statement
     * @param isBeginningOfBlock
     * @param isRead
     * @param isWrite
     */
    private void generateSys(String lable, String statement, boolean isBeginningOfBlock, boolean isRead, boolean isWrite)
    {

        // Splitting up READ or WRITE statement
        // ------------------------------------------------------------ //
        String ids[] = null;
        try
        {
            // Split
            if (statement.length() > 1)
            {
                ids = statement.split(",");
            }
            else
            {
                ids = new String[1];
                ids[0] = statement;
            }
        }
        catch (Exception e)
        {
            System.out.printf("BUILD-SYS" + e.getMessage());
        }
        // ------------------------------------------------------------ //

        if (isRead)
        {
            // Loop via ids and generate tiny code for READ or WRITE
            for (int i = 0; i < ids.length; i++)
            {
                // Loop via varMap to identify type of each id
                for (Map.Entry<String, VarDeclaration> entry : varMap.entrySet())
                {
                    if (ids[i].equals(entry.getKey()))
                    {
                        String sys = null;
                        if (entry.getValue().getType().equals("INT"))
                        {
                            sys = "sys readi";
                        }
                        else if (entry.getValue().getType().equals("FLOAT"))
                        {
                            sys = "sys readr";
                        }

                        if (sys != null)
                        {
                            String output = sys + " " + ids[i];
                            System.out.printf("%s\n", output);
                        }
                    }
                }
            }
        }
        if (isWrite)
        {
            // Loop via ids and generate tiny code for READ or WRITE
            for (int i = 0; i < ids.length; i++)
            {
                // Loop via varMap to identify type of each id
                for (Map.Entry<String, VarDeclaration> entry : varMap.entrySet())
                {
                    if (ids[i].equals(entry.getKey()))
                    {
                        String sys = null;
                        if (entry.getValue().getType().equals("INT"))
                        {
                            sys = "sys writei";
                        }
                        else if (entry.getValue().getType().equals("FLOAT"))
                        {
                            sys = "sys writer";
                        }
                        else if (entry.getValue().getType().equals("STRING"))
                        {
                            sys = "sys writes";
                        }

                        if (sys != null)
                        {
                            String output = sys + " " + ids[i];
                            System.out.printf("%s\n", output);
                        }
                    }
                }
            }
        }
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
