import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class GenerateStatement
{
    private static int registerCount = 0;

    private String statement = null;

    // This map will be used to identify variable types
    private Map<String, VarDeclaration> varMap = null;

    private static Map<String, VarLocal> varLocalMap = new HashMap<String, VarLocal>();

    // FINAL Conditions map
    // Is used to build specific label which depends on condition
    private final Map<String, String> conditionsMap = new HashMap<String, String>()
    {
        {
            // Generating conditions
            put("<", "jge");    // jgt target: jump if greater:          A < B
            put(">", "jle");    // jle target: jump if less or equal:    A > B
            put("=", "jne");    // jne target: jump if not equal:        A = B
            put("!=", "jeq");   // jeq target: jump if equal:            A != B
            put("<=", "jgt");   // jgt target: jump if (op1 of the preceding cmp was) greater (than op2)
            put(">=", "jlt");   // jlt target: jump if less than:        A > B
        }
    };

    // FINAL Arithmetic operations map
    private final Map<String, String> arithmeticMap = new HashMap<String, String>()
    {
        {
            // Generate arithmetic operations
            put("int#+", "addi"); // addi opmrl reg:    integer addition, reg = reg + op1
            put("int#-", "subi"); // subi opmrl reg:    computes reg = reg - op1
            put("int#*", "muli"); // muli opmrl reg:    computes reg = reg * op1
            put("int#/", "divi"); // divi opmrl reg:    computes reg = reg /  op1

            put("real#+", "addr"); // addr opmrl reg:   real (i.e. floatingpoint) addition
            put("real#-", "subr"); // subr opmrl reg:   computes reg = reg - op1
            put("real#*", "mulr"); // mulr opmrl reg:   computes reg = reg * op1
            put("real#/", "divr"); // divr opmrl reg:   computes reg = reg /  op1
        }
    };

    private String jumpName = null;
    private String leftExpr = null;
    private String rightExpr = null;
    private String compStmt = null;

    private String condBody = null;

    // FINAL Assign operation map
    private final Map<String, String> assignMap = new HashMap<String, String>()
    {
        {
            // Generate assign operation
            put(":=", "move"); // move opmrl opmr: only one operand can be a memory id or stack variable
        }
    };

    /**
     * Constructor
     */
    public GenerateStatement()
    {

    }

    public void buildComparison(String statement, Map<String, VarDeclaration> varMap)
    {
        this.varMap = varMap;
        this.statement = statement;

        if (registerCount < VarLocal.getMaxRegNum())
        {
            registerCount = VarLocal.getMaxRegNum();
            registerCount++;
        }

        // Split-up statement
        // 1: Identifying comparison symbol
        char smt[] = statement.toCharArray();
        boolean isFound = false;
        String comp = null;
        String tmpJump = null;

        // Identifying comparison symbol
        // ------------------------------------------------------------------------------------------ //
        for (int i = 0; i < smt.length; i++)
        {
            String s1 = Character.toString(smt[i]);

            // Loop via all conditions symbols
            for (Map.Entry<String, String> entry : conditionsMap.entrySet())
            {
                if (s1.equals(entry.getKey()))
                {
                    comp = entry.getKey();
                    tmpJump = entry.getValue();
                    isFound = true;
                    break;
                }
                else
                {
                    comp = null;
                    tmpJump = null;
                }

                // Some comparison statement can have two chars: <=, =>, !=
                // so we have to test it out
                String s2 = "";
                if ( (i + 1) <=  (smt.length - 1) )
                {
                    s2 = Character.toString(smt[i + 1]);
                    comp = s1 + "" + s2;

                    if (comp.equals(entry.getKey()))
                    {
                        comp = entry.getKey();
                        tmpJump = entry.getValue();
                        isFound = true;
                        break;
                    }
                    else
                    {
                        comp = null;
                        tmpJump = null;
                    }
                }
            }

            if (isFound)
            {
                break;
            }
        }
        // ------------------------------------------------------------------------------------------ //

        if (comp != null && tmpJump != null)
        {
            this.jumpName = tmpJump;

            generate3AC(smt, comp);
        }
    }

    private void generate3AC(char smt[], String comp)
    {
        String lType = null;
        String left = null;
        String regLeft = null;

        String rType = null;
        String right = null;
        String regRight = null;

        // Split condition expression by its comparison symbol
        String tmp[] = statement.split(comp);

        left = tmp[0];
        right = tmp[1];

        // Testing
        isLiteral(left);
        isLiteral(right);

        // Testing left side of the comparison expression
        // ------------------------------------------------------------------------------------------- //
        if (isINT(left))
        {
            lType = "INT";
        }
        else if (isFLOAT(left))
        {
            lType = "FLOAT";
        }
        else if (isLiteral(left))
        {
            lType = findType(left);

            // Get pre-generated register
            regLeft = findReg(left);
        }
        //System.out.printf("*** %s: %s\n", left, lType);
        // ------------------------------------------------------------------------------------------- //

        // Testing right side of the comparison expression
        // ------------------------------------------------------------------------------------------- //
        if (isINT(right))
        {
            rType = "INT";
        }
        else if (isFLOAT(right))
        {
            rType = "FLOAT";
        }
        else if (isLiteral(right))
        {
            rType = findType(right);

            // Get pre-generated register
            regRight = findReg(right);

        }
        //System.out.printf("*** %s: %s\n", right, rType);
        // ------------------------------------------------------------------------------------------- //

        // Generating left-side of the condition
        // But before we have to make sure that map of all variables does not include
        // pre-generated register
        // ------------------------------------------------------------------------------------------ //
        if (regLeft == null)
        {
            regLeft = "r" + registerCount;
            leftExpr = "move " + left + " " + regLeft;
            registerCount++;
        }
        else
        {
            //leftExpr = "move " + left + " " + regLeft;
            regLeft = left;
        }
        // ------------------------------------------------------------------------------------------ //

        // Generating right-side of the condition
        // But before we have to make sure that map of all variables does not include
        // pre-generated register
        // ------------------------------------------------------------------------------------------ //
        if (regRight == null)
        {
            regRight = "r" + registerCount;
            rightExpr  = "move " + right + " " + regRight;
            registerCount++;
        }
        else
        {
            //rightExpr  = "move " + right + " " + regRight;
            regRight = right;
        }
        // ------------------------------------------------------------------------------------------ //

        if (lType.equals("INT") && rType.equals("INT"))
        {
            compStmt = "cmpi";
        }
        else if (lType.equals("FLOAT") && rType.equals("FLOAT"))
        {
            compStmt = "cmpr";
        }

        String tmp1 = "";
        if (leftExpr != null)
        {
            tmp1 = leftExpr;
        }
        else
        {
            tmp1 = "";
        }

        String tmp2 = "";
        if (rightExpr != null)
        {
            tmp2 = rightExpr;
        }
        else
        {
            tmp2 = "";
        }

        condBody = tmp1 + "\n" + tmp2;
        compStmt = compStmt + " " + regLeft + " " + regRight;
        condBody = condBody + "\n" + compStmt;

    }

    public String getCondBody()
    {
        return this.condBody;
    }

    /**
     * If we have numeric value we have to see if it is INT
     * @param tmp
     * @return
     */
    private boolean isINT(String tmp)
    {
        try
        {
            Integer.parseInt(tmp);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * If we have numeric value we have to see if it is FLOAT
     * @param tmp
     * @return
     */
    private boolean isFLOAT(String tmp)
    {
        try
        {
            Float.parseFloat(tmp);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * This method is used to identify if we have literal or numeric value
     * @param obj
     * @return
     */
    private boolean isLiteral(Object obj)
    {
        if (obj instanceof String)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * This method is used to identify the type of a variable
     * @param tmp
     * @return
     */
    private String findType(String tmp)
    {
        String type = null;

        // Loop via varMap to identify type of each var
        for (Map.Entry<String, VarDeclaration> entry : varMap.entrySet())
        {
            if (tmp.equals(entry.getKey()))
            {
                type = entry.getValue().getType();
                break;
            }
        }

        return type;
    }

    /**
     * After we generate local statements we also generated register,
     * so we have to find these registers and maximum register number
     * @param var
     * @return
     */
    private String findReg(String var)
    {
        String reg = null;

        // Loop via varMap to identify type of each var
        for (Map.Entry<String, VarLocal> entry : varLocalMap.entrySet())
        {
            if (var.equals(entry.getKey()))
            {
                reg = entry.getValue().getRegister();
                break;
            }
        }

        return reg;
    }



    /** Get prefix for a label that is based on comparison expression
     * Only for conditions
     * @return
     */
    public String getJumpName()
    {
        return this.jumpName;
    }

    /** Get left side of comparison expression in TINY language
     * Only for conditions
     * @return
     */
    public String getLeftExpr()
    {
        return this.leftExpr;
    }

    /** Get right side of comparison expression in TINY language
     * Only for conditions
     * @return
     */
    public String getRightExpr()
    {
        return this.rightExpr;
    }

    /** Get comparison expression in TINY language
     * Only for conditions
     * @return
     */
    public String getCompStmt()
    {
        return this.compStmt;
    }
}
