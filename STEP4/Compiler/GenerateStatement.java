/**
 * Montana State University
 * Class: Compilers - CSCI 468
 * @author Olexandr Matveyev
 */

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 *  This class is used to generate statements
 */
public class GenerateStatement
{
    private static int registerCount = 0;

    private String statement = null;

    // This map will be used to identify variable types
    private Map<String, VarDeclaration> varMap = null;

    // This map is used to hold generated Tiny code for each pair of variables,
    // but it mostly used for complex expressions
    private Map<String, MicroExpression> microExpressionMap = new HashMap<String, MicroExpression>();
    private Stack<MicroExpression> stackOFMicroExpr = new Stack<MicroExpression>();

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
            put("INT#+", "addi"); // addi opmrl reg:    integer addition, reg = reg + op1
            put("INT#-", "subi"); // subi opmrl reg:    computes reg = reg - op1
            put("INT#*", "muli"); // muli opmrl reg:    computes reg = reg * op1
            put("INT#/", "divi"); // divi opmrl reg:    computes reg = reg /  op1

            put("FLOAT#+", "addr"); // addr opmrl reg:   real (i.e. floatingpoint) addition
            put("FLOAT#-", "subr"); // subr opmrl reg:   computes reg = reg - op1
            put("FLOAT#*", "mulr"); // mulr opmrl reg:   computes reg = reg * op1
            put("FLOAT#/", "divr"); // divr opmrl reg:   computes reg = reg /  op1
        }
    };

    private String jumpName = null;
    private String leftExpr = null;
    private String rightExpr = null;
    private String compStmt = null;

    private String condBody = null;
    private String assignmentBody = null;

    /**
     * Constructor
     */
    public GenerateStatement()
    {

    }

    /**
     * This function must be called each time before building/generating "Condition Expression"
     * or before building/generating "Assignment Expression".
     * @param statement
     * @param varMap
     */
    public void updateData(String statement, Map<String, VarDeclaration> varMap)
    {
        // It is redundant to pass again and again the varMap to this class,
        // but currently this solution is working with the currently
        // structure of the CodeGeneration class
        this.varMap = varMap;

        // We have to update statement every time
        this.statement = statement;

        // The registers were generated for each variable that were declared globally.
        // And here I am getting number of the last generated register,
        // and updating current register counter.
        if (registerCount == 0)
        {
            if (registerCount < VarDeclaration.getRegisterMaxNum())
            {
                registerCount = VarDeclaration.getRegisterMaxNum();
                registerCount++;
            }
        }
    }

    /**
     * Is used to build condition 3AC: PART 1
     */
    public void buildCondition()
    {
        // JUST TESTING
        //System.out.printf("[ %s ]\n", statement);

        String comp = identifyComparisonSymbol();

        char smt[] = statement.toCharArray();
        String tmpJump = null;

        // Identifying comparison label-name
        // ------------------------------------------------------------------------------------------ //
        for (Map.Entry<String, String> entry : conditionsMap.entrySet())
        {
            if (comp.equals(entry.getKey()))
            {
                tmpJump = entry.getValue();
            }
        }
        // ------------------------------------------------------------------------------------------ //

        // Continue generating comparison code
        if (comp != null && tmpJump != null)
        {
            this.jumpName = tmpJump;
            generateCondition(smt, comp);
        }
    }

    /**
     * Is used to build condition 3AC: PART 2
     * @param smt
     * @param comp
     */
    private void generateCondition(char smt[], String comp)
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

        boolean isLeftComplex = true;
        boolean isRightComplex = true;

        // Testing left side of the comparison expression
        // ------------------------------------------------------------------------------------------- //
        if (isINT(left))
        {
            lType = "INT";
            isLeftComplex = false;
        }
        else if (isFLOAT(left))
        {
            lType = "FLOAT";
            isLeftComplex = false;
        }
        else if (isLiteral(left))
        {
            lType = findTypeByID(left);
            if (lType == null)
            {
                isLeftComplex = true;
            }
            else
            {
                isLeftComplex = false;
            }
        }
        // ------------------------------------------------------------------------------------------- //

        // Testing right side of the assignment expression
        // ------------------------------------------------------------------------------------------- //
        if (isINT(right))
        {
            rType = "INT";
            isRightComplex = false;
        }
        else if (isFLOAT(right))
        {
            rType = "FLOAT";
            isRightComplex = false;
        }
        else if (isLiteral(right))
        {
            rType = findTypeByID(right);
            if (rType == null)
            {
                isRightComplex = true;
            }
            else
            {
                isRightComplex = false;
            }
        }
        // ------------------------------------------------------------------------------------------- //

        // Simple assignments must be in form: a := DIGIT
        if (isLeftComplex || isRightComplex)
        {
            // Building complex assignment
            buildComplexCondition(left, right, comp);
        }
        else if (!isLeftComplex && !isRightComplex)
        {
            // Building simple assignment
            buildSimpleCondition(left, right, comp, lType, rType);
        }
    }

    /**
     * This function is used to handle simple conditions, such as "a > b" or
     * "b >= 10"
     * @param left
     * @param right
     * @param comp
     * @param lType
     * @param rType
     */
    private void buildSimpleCondition(String left, String right, String comp, String lType, String rType)
    {
        String regLeft = null;
        String regRight = null;

        regLeft = findID(left);
        regRight = findID(right);

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

        if ("INT".equals(lType) && "INT".equals(rType))
        {
            compStmt = "cmpi";
        }
        else if ("FLOAT".equals(lType) && "FLOAT".equals(rType))
        {
            compStmt = "cmpr";
        }

        String tmp1 = "";
        if (leftExpr != null)
        {
            tmp1 = leftExpr + "\n";
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

        // Generated condition body
        condBody = tmp1 + tmp2;
        compStmt = compStmt + " " + regLeft + " " + regRight;
        condBody = condBody + "\n" + compStmt;
    }

    /**
     * This function is used to handle complex conditions, such as "a > (b - c)" or
     * z != (b * a)
     * @param left
     * @param right
     * @param comp
     */
    private void buildComplexCondition(String left, String right, String comp)
    {
        String stmt[] = modifyExprInput(right);

        String newExpression = "";
        String exprTmp[] = new String[stmt.length];
        int exprTmpCount = 0;
        int indexStart = 0;
        int indexEnd = 0;
        boolean subExpr = false;
        for (int i = 0; i < stmt.length; i++)
        {
            // We have to in the first place compute expression between brackets
            if ("(".equals(stmt[i]) || ")".equals(stmt[i]))
            {
                String tmp = "";
                // We have to identify beginning of the expression
                // between brackets
                if ("(".equals(stmt[i]))
                {
                    indexStart = i + 1;
                    subExpr = true;
                }
                if (")".equals(stmt[i]))
                {
                    indexEnd = i;
                    subExpr = false;

                    // Build new sub-expression
                    // ---------------------------------------------- //
                    int arrSize = indexEnd - indexStart;
                    int countTmp = 0;
                    String subExprTMP[] = new String[arrSize];
                    for (int j = indexStart; j < indexEnd; j++)
                    {
                        subExprTMP[countTmp] = stmt[j];
                        countTmp++;
                    }
                    // ---------------------------------------------- //

                    // Computing sub-expression
                    // ------------------------------------------------------------------------- //
                    // Working on [*] and [/]
                    // will return modified array of strings
                    subExprTMP = multiplyDivide(subExprTMP);

                    // Working on [+] and [-]
                    // will return void because eliminating [+] and [-] should be final stage
                    subExprTMP = addSubtract(subExprTMP);
                    // ------------------------------------------------------------------------- //

                    // Replace closing bracket of the current sub-expressions with
                    // register which will store sub-expression computation result
                    stmt[i] = subExprTMP[0];
                    i--;
                }
            }
            else
            {
                if (!subExpr)
                {
                    exprTmp[exprTmpCount] = stmt[i];
                    exprTmpCount++;
                }
            }
        }

        // Re-build exprTmp array, and remove all empty and null records
        exprTmp = cleaningUpArray(exprTmp);

        // Final stage, the exprTmp should not contain any brackets
        // Working on [*] and [/]
        // will return modified array of strings
        exprTmp = multiplyDivide(exprTmp);

        // Working on [+] and [-]
        // will return void because eliminating [+] and [-] should be final stage
        exprTmp = addSubtract(exprTmp);

        // Store the least generated register in the right side of expression
        // basically store in in the final variable

        assignmentBody = finalCondition(left, exprTmp, comp);

    }

    /**
     * This function is used to identifying comparison symbol
     * @return
     */
    private String identifyComparisonSymbol()
    {
        String stmt[] = statement.split("");
        String compTmp = null;
        String comp = null;
        int index = -1;
        boolean found = false;

        for (int i = 0; i < stmt.length; i++)
        {
            for (Map.Entry<String, String> entry : conditionsMap.entrySet())
            {
                String str = Character.toString(entry.getKey().charAt(0));
                if (stmt[i].equals(str))
                {
                    comp = stmt[i];
                    index = i;
                    found = true;
                    break;
                }
                else
                {
                    comp = null;
                    found = false;
                }
            }

            if (found)
            {
                break;
            }
        }
        found = false;

        for (Map.Entry<String, String> entry : conditionsMap.entrySet())
        {
            String tmp = (comp + stmt[index + 1]);
            if (tmp.equals(entry.getKey()))
            {
                compTmp = entry.getKey();
                found = true;
                break;
            }
            else
            {
                compTmp = null;
                found = false;
            }
        }

        if (found)
        {
            comp = compTmp;
        }

        return comp;
    }

    /**
     * This function used in first place to identify if given assignment expression is
     * simple or complex, and after it will utilize function "buildSimpleAssignment"
     * or function "buildComplexAssignment" to generate appropriate assignment expression
     * in the format of Tiny language.
     */
    public void buildAssignmentExpression()
    {
        // Remove ';' from the end of the statement
        // ----------------------------------------------------------------------- //
        String statementTMP = null;
        if ( statement.charAt(statement.length() - 1) == ';' )
        {
            statementTMP = statement.replaceAll(";", "");
        }

        if (statementTMP != null)
        {
            statement = statementTMP;
        }
        // ----------------------------------------------------------------------- //

        String lType = null;
        String left = null;
        String regLeft = null;

        String rType = null;
        String right = null;
        String regRight = null;

        String str[] = statement.split(":=");
        left = str[0];
        right = str[1];

        boolean isComplex = true;

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
            lType = findTypeByID(left);

            // Get pre-generated register
            regLeft = findID(left);
        }
        // ------------------------------------------------------------------------------------------- //

        // Testing right side of the assignment expression
        // ------------------------------------------------------------------------------------------- //
        if (isINT(right))
        {
            rType = "INT";
            isComplex = false;
        }
        else if (isFLOAT(right))
        {
            rType = "FLOAT";
            isComplex = false;
        }
        else if (isLiteral(right))
        {
            isComplex = true;
        }
        // ------------------------------------------------------------------------------------------- //

        // Simple assignments must be in form: a := DIGIT
        if (isComplex)
        {
            // Building complex assignment
            buildComplexAssignment(lType, left, right);
        }
        else
        {
            // Building simple assignment
            buildSimpleAssignment(left, right);
        }

    }

    /**
     * This function is used to generate the simplest assignment expressions,
     * such as [a := DIGIT]
     * @param left
     * @param right
     */
    public void buildSimpleAssignment(String left, String right)
    {
        String rightExpr = null;
        String regRight = null;

        regRight = "r" + registerCount;
        rightExpr  = "move " + right + " " + regRight;
        registerCount++;

        // Generated assignment body
        assignmentBody = rightExpr + "\n";
        assignmentBody = assignmentBody + "move " + regRight + " " + left;
    }


    /**
     * This function is used to generate complex assignment expressions,
     * such as [a := a + b + 1] or [b := (b * 2) - 5]
     * @param left
     * @param right
     */
    public void buildComplexAssignment(String lType, String left, String right)
    {
        String stmt[] = modifyExprInput(right);

        String newExpression = "";
        String exprTmp[] = new String[stmt.length];
        int exprTmpCount = 0;
        int indexStart = 0;
        int indexEnd = 0;
        boolean subExpr = false;
        for (int i = 0; i < stmt.length; i++)
        {
            // We have to in the first place compute expression between brackets
            if ("(".equals(stmt[i]) || ")".equals(stmt[i]))
            {
                String tmp = "";
                // We have to identify beginning of the expression
                // between brackets
                if ("(".equals(stmt[i]))
                {
                    indexStart = i + 1;
                    subExpr = true;
                }
                if (")".equals(stmt[i]))
                {
                    indexEnd = i;
                    subExpr = false;

                    // Build new sub-expression
                    // ---------------------------------------------- //
                    int arrSize = indexEnd - indexStart;
                    int countTmp = 0;
                    String subExprTMP[] = new String[arrSize];
                    for (int j = indexStart; j < indexEnd; j++)
                    {
                        subExprTMP[countTmp] = stmt[j];
                        countTmp++;
                    }
                    // ---------------------------------------------- //

                    // Computing sub-expression
                    // ------------------------------------------------------------------------- //
                    // Working on [*] and [/]
                    // will return modified array of strings
                    subExprTMP = multiplyDivide(subExprTMP);

                    // Working on [+] and [-]
                    // will return void because eliminating [+] and [-] should be final stage
                    subExprTMP = addSubtract(subExprTMP);
                    // ------------------------------------------------------------------------- //

                    // Replace closing bracket of the current sub-expressions with
                    // register which will store sub-expression computation result
                    stmt[i] = subExprTMP[0];
                    i--;
                }
            }
            else
            {
                if (!subExpr)
                {
                    exprTmp[exprTmpCount] = stmt[i];
                    exprTmpCount++;
                }
            }
        }

        // Re-build exprTmp array, and remove all empty and null records
        exprTmp = cleaningUpArray(exprTmp);

        // Final stage, the exprTmp should not contain any brackets
        // Working on [*] and [/]
        // will return modified array of strings
        exprTmp = multiplyDivide(exprTmp);

        // Working on [+] and [-]
        // will return void because eliminating [+] and [-] should be final stage
        exprTmp = addSubtract(exprTmp);

        // Store the least generated register in the right side of expression
        // basically store in in the final variable

        assignmentBody = finalAssignment(left, exprTmp);

    }

    /**
     * This function is used to handle MULTIPLICATION and DIVISION operations.
     * @param stmt
     * @return
     */
    private String[] multiplyDivide(String stmt[])
    {
        MicroExpression microExpression = null;

        for (int i = 0; i < stmt.length; i++)
        {
            String arithmeticSymbol = null;
            if ("*".equals(stmt[i]) || "/".equals(stmt[i]))
            {
                // Just local variables
                // ------------------------------------------------------------------------ //
                String id1 = null;
                String id2 = null;

                String id1Type = null;
                String id2Type = null;

                String command = null;
                // ------------------------------------------------------------------------ //

                // Test for milt or div
                // I am looking one-symbol behind and one-symbol ahead for [*] and [/]
                // in order to get its children variables
                // ------------------------------------------------------------------------ //
                if ("*".equals(stmt[i]))
                {
                    id1 = stmt[i - 1];
                    id2 = stmt[i + 1];
                    arithmeticSymbol = "*";
                }
                else if ("/".equals(stmt[i]))
                {
                    id1 = stmt[i - 1];
                    id2 = stmt[i + 1];
                    arithmeticSymbol = "/";
                }
                // ------------------------------------------------------------------------ //

                // Checking type of current variables
                // ------------------------------------------------------------------------ //
                id1Type = typeTest(id1);
                id2Type = typeTest(id2);
                String type = null;
                // ------------------------------------------------------------------------ //

                // Find appropriate Tiny command for the specific arithmetic operation
                // ------------------------------------------------------------------------ //
                if (id1Type.equals(id2Type))
                {
                    for (Map.Entry<String, String> entry : arithmeticMap.entrySet())
                    {
                        String tmp1[] = entry.getKey().split("#");
                        if (id1Type.equals(tmp1[0]) && arithmeticSymbol.equals(tmp1[1]))
                        {
                            command = entry.getValue();
                            type = id1Type;
                            break;
                        }
                    }
                }
                // ------------------------------------------------------------------------ //

                // Generating Tiny code for this pair of variables
                // ------------------------------------------------------------------------ //;

                String tinyCode = null;

                int regNum1 = registerCount;
                String reg1 = "r" + registerCount;
                registerCount++;

                int regNum2 = registerCount;
                String reg2 = "r" + registerCount;
                registerCount++;

                //muli opmrl reg: computes reg = op1 * reg
                //divi opmrl reg: computes reg = op1 / reg

                tinyCode = "move " + id1 + " " + reg1 + "\n";
                tinyCode = tinyCode + "move " + id2 + " " + reg2 + "\n";
                //tinyCode = tinyCode + command + " " + reg1 + " " + reg2 + "\n";
                tinyCode = tinyCode + command + " " + reg2 + " " + reg1 + "\n";

                microExpression = new MicroExpression(id1, id2, reg1, tinyCode, arithmeticSymbol, type, regNum1);
                microExpressionMap.put(reg1, microExpression);

                stackOFMicroExpr.push(microExpression);

                // Remove user variables and its arithmetic value,
                // and replace last var with register were we did store result
                stmt[i - 1] = "";
                stmt[i] = "";
                stmt[i + 1] = reg1;

                // ------------------------------------------------------------------------ //
            }
        }

        // Cleaning up stmt from empty strings
        // and returning it
        return cleaningUpArray(stmt);
    }

    /**
     * This function is used to handle ADD and SUBTRACT arithmetic.
     * @param stmt
     * @return
     */
    private String[] addSubtract(String stmt[])
    {
        MicroExpression microExpression = null;

        for (int i = 0; i < stmt.length; i++)
        {
            String arithmeticSymbol = null;
            if ("+".equals(stmt[i]) || "-".equals(stmt[i]))
            {
                // Just local variables
                // ------------------------------------------------------------------------ //
                String id1 = null;
                String id2 = null;

                String id1Type = null;
                String id2Type = null;

                String command = null;
                // ------------------------------------------------------------------------ //

                // Test for milt or div
                // I am looking one-symbol behind and one-symbol ahead for [*] and [/]
                // in order to get its children variables
                // ------------------------------------------------------------------------ //
                if ("+".equals(stmt[i]))
                {
                    id1 = stmt[i - 1];
                    id2 = stmt[i + 1];
                    arithmeticSymbol = "+";
                }
                else if ("-".equals(stmt[i]))
                {
                    id1 = stmt[i - 1];
                    id2 = stmt[i + 1];
                    arithmeticSymbol = "-";
                }
                // ------------------------------------------------------------------------ //

                // Checking type of current variables
                // ------------------------------------------------------------------------ //
                id1Type = typeTest(id1);
                id2Type = typeTest(id2);
                String type = null;
                // ------------------------------------------------------------------------ //

                // Find appropriate Tiny command for the specific arithmetic operation
                // ------------------------------------------------------------------------ //
                if (id1Type.equals(id2Type))
                {
                    for (Map.Entry<String, String> entry : arithmeticMap.entrySet())
                    {
                        String tmp1[] = entry.getKey().split("#");
                        if (id1Type.equals(tmp1[0]) && arithmeticSymbol.equals(tmp1[1]))
                        {
                            command = entry.getValue();
                            type = id1Type;
                            break;
                        }
                    }
                }
                // ------------------------------------------------------------------------ //

                // Generating Tiny code for this pair of variables
                // ------------------------------------------------------------------------ //

                String tinyCode = null;

                int regNum1 = registerCount;
                String reg1 = "r" + registerCount;
                registerCount++;

                int regNum2 = registerCount;
                String reg2 = "r" + registerCount;
                registerCount++;

                //muli opmrl reg: computes reg = reg * op1
                //divi opmrl reg: computes reg = reg / op1

                tinyCode = "move " + id1 + " " + reg1 + "\n";
                tinyCode = tinyCode + "move " + id2 + " " + reg2 + "\n";
                //tinyCode = tinyCode + command + " " + reg1 + " " + reg2 + "\n";
                tinyCode = tinyCode + command + " " + reg2 + " " + reg1 + "\n";

                microExpression = new MicroExpression(id1, id2, reg1, tinyCode, arithmeticSymbol, type, regNum1);
                microExpressionMap.put(reg1, microExpression);

                stackOFMicroExpr.push(microExpression);

                // Remove user variables and its arithmetic value,
                // and replace last var with register were we did store result
                stmt[i - 1] = "";
                stmt[i] = "";
                stmt[i + 1] = reg1;

                // ------------------------------------------------------------------------ //
            }
        }

        // Cleaning up stmt from empty strings
        // and returning it
        return cleaningUpArray(stmt);

    }

    /**
     * This function is used to restructure assignment expression.
     * Because we have to properly identify an entire digit or a literal.
     * @param right
     * @return
     */
    private String[] modifyExprInput(String right)
    {
        // Adding new-line.
        // Later it will help us to rebuild digits in this input-line
        right = right + "\n";

        // Splitting up string-line into individual characters
        String stmt[] = right.split("");

        // This string will store digit
        String tmpDigit = "";
        int startIndex = 0;
        int endIndex = 0;
        int range = 0;
        String symbols[] = {"+", "-", "*", "/", "(", ")"};

        // When converting into string array,
        // extra check must be developed to capture entire number
        // currently program missing full length of digit
        for (int i = 0; i < stmt.length; i++)
        {
            if (stmt[i].matches("[0-9]"))
            {
                // Storing beginning of a digit
                startIndex = i;
                boolean stop = false;

                // Looking for the end of a digit
                for (int j = i; j < stmt.length; j++)
                {
                    // Loop via all symbols to find match
                    for (int k = 0; k < symbols.length; k++)
                    {
                        if (stmt[j].equals(symbols[k]) || "\n".equals(stmt[j]))
                        {
                            endIndex = j - 1;
                            stop = true;
                            break;
                        }
                    }

                    if (stop)
                    {
                        break;
                    }
                }

                for (i = startIndex; i <= endIndex; i++)
                {
                    tmpDigit = tmpDigit + stmt[i];
                    stmt[i] = "";
                }
                stmt[endIndex] = tmpDigit;
            }

            tmpDigit = "";
            startIndex = 0;
            endIndex = 0;
        }

        stmt = cleaningUpArray(stmt);


        // Some IDs can be larger that single character,
        // so we have to properly identify such Ids
        for (int i = 0; i < stmt.length; i++)
        {
            String newID = "";
            while ( i < stmt.length )
            {
                if (
                        "+".equals(stmt[i]) || "-".equals(stmt[i]) ||
                                "*".equals(stmt[i]) || "/".equals(stmt[i]) ||
                                "(".equals(stmt[i]) || ")".equals(stmt[i])
                )
                {
                    break;
                }
                else
                {
                    if (newID.equals(""))
                    {
                        newID = stmt[i];
                        stmt[i] = "";
                    }
                    else
                    {
                        newID = newID + stmt[i];
                        stmt[i] = "";
                    }
                }
                i++;
            }

            if (!newID.equals(""))
            {
                stmt[i-1] = newID;
            }
        }


        stmt = cleaningUpArray(stmt);

        return stmt;
    }

    /**
     * This function is used to re-build and array, basically we just removing empty string records.
     * @param stmt
     * @return
     */
    private String[] cleaningUpArray(String stmt[])
    {
        // Cleaning up stmt from empty strings
        // --------------------------------------------------------------------------------- //
        int newSize = 0;
        for (int i = 0; i < stmt.length; i++)
        {
            if (stmt[i] != null)
            {
                if (!"".equals(stmt[i]) && !"\n".equals(stmt[i]))
                {
                    newSize++;
                }
            }
        }

        String output[] = new String[newSize];
        int count = 0;
        for (int i = 0; i < stmt.length; i++)
        {
            if (stmt[i] != null)
            {
                if (!"".equals(stmt[i]) && !"\n".equals(stmt[i]))
                {
                    output[count] = stmt[i];
                    count++;
                }
            }
        }
        // --------------------------------------------------------------------------------- //

        return output;
    }

    /**
     * This function is used to finalize condition generation
     * @param left
     * @param stmt
     * @param comp
     * @return
     */
    private String finalCondition(String left, String stmt[], String comp)
    {
        String tmp1 = "";
        String tmp2 = "";

        String regLeft = null;
        String lType = null;

        String regRight = null;
        String rType = null;

        regLeft = findID(left);
        lType = findTypeByReg(regLeft);

        regRight = stmt[0];

        // Getting Tiny code from stack, and type for the left side of expression
        for (MicroExpression me : stackOFMicroExpr)
        {
            // We have to get type of newly generated register from stack
            rType = me.getType();
            tmp2 = tmp2 + me.getTinyCode();
        }

        // Getting compare label name in the form of Tiny code
        if ("INT".equals(lType) && "INT".equals(rType))
        {
            compStmt = "cmpi";
        }
        else if ("FLOAT".equals(lType) && "FLOAT".equals(rType))
        {
            compStmt = "cmpr";
        }

        // Final output for a specific condition expression
        tmp1 = compStmt + " " + left + " " + stmt[0];
        tmp2 = tmp2 + tmp1;

        return tmp2;
    }

    /**
     * This function generates final output for the assignment expression
     * @param left
     * @param stmt
     * @return
     */
    private String finalAssignment(String left, String stmt[])
    {
        String tmp1 = "";
        String tmp2 = "";

        tmp1 = "move " + stmt[0] + " " + left;

        //assignmentBody
        //Test print stack
        for (MicroExpression me : stackOFMicroExpr)
        {
            tmp2 = tmp2 + me.getTinyCode();
        }


        // Here is one bug. Sometimes Micro source code will have such assignment id1 := id2
        // where id1 is multi-character literal as well as id2.
        // So to handle it properly a register generated for id2 must be stored in id1,
        // but currently in multi-character literal id1 will be stored
        // multi-character literal id2 which is wrong.
        // If output of the stack is empty then we got such situation described above.
        if (tmp2.equals(""))
        {
            buildSimpleAssignment(left, stmt[0]);
            tmp2 = this.assignmentBody;
        }
        else
        {
            tmp2 = tmp2 + tmp1;
        }

        return tmp2;
    }

    /**
     * Getting type of an ID
     * @param id
     * @return
     */
    private String typeTest(String id)
    {
        String type = null;

        if (isINT(id))
        {
            type = "INT";
        }
        else if (isFLOAT(id))
        {
            type = "FLOAT";
        }
        else if (isLiteral(id))
        {
            type = findTypeByID(id);

            // id can be in the form of register, so we have to use
            // another way to find out data-type of register
            if (type == null)
            {
                for (Map.Entry<String, MicroExpression> entry : microExpressionMap.entrySet())
                {
                    if (id.equals(entry.getKey()))
                    {
                        type = entry.getValue().getType();
                    }
                }
            }
        }

        return type;
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
    private String findTypeByID(String tmp)
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
     * This method is used to identify the type of a variable
     * @param tmp
     * @return
     */
    private String findTypeByReg(String tmp)
    {
        String type = null;

        // Loop via varMap to identify type of each var
        for (Map.Entry<String, VarDeclaration> entry : varMap.entrySet())
        {
            if (tmp.equals(entry.getValue().getRegister()))
            {
                type = entry.getValue().getType();
                break;
            }
        }

        return type;
    }

    /**
     * Is used to test existing IDs
     * @param var
     * @return
     */
    private String findID(String var)
    {
        String id = null;

        // Loop via varMap to identify type of each var
        for (Map.Entry<String, VarDeclaration> entry : varMap.entrySet())
        {
            if (var.equals(entry.getKey()))
            {
                id = entry.getValue().getRegister();
                break;
            }
        }

        return id;
    }

    /**
     * After we generate local statements we also generated registers,
     * so we have to find these registers and maximum register number
     * @param var
     * @return
     */
    private String findReg(String var)
    {
        String reg = null;

        // Loop via varMap to identify type of each var
        for (Map.Entry<String, VarDeclaration> entry : varMap.entrySet())
        {
            if (var.equals(entry.getValue().getRegister()))
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

    /**
     * Get 3AC code for the condition
     * @return
     */
    public String getCondBody()
    {
        return this.condBody;
    }

    /**
     * Get generated assignment body in a Tiny language format
     * @return
     */
    public String getAssignmentBody()
    {
        return this.assignmentBody;
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

    /**
     * This function is used to reset [assignmentBody] and [stackOFMicroExpr]
     */
    public void resetData()
    {
        this.assignmentBody = null;
        stackOFMicroExpr = new Stack<MicroExpression>();
    }
}
