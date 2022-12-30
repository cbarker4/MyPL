/*
 * File: CodeGenerator.java
 * Date: Spring 2022
 * Auth:
 * Desc:
 */

import java.awt.*;
import java.util.*;
import java.util.List;


public class CodeGenerator implements Visitor {

    // the user-defined type and function type information
    private TypeInfo typeInfo = null;

    // the virtual machine to add the code to
    private VM vm = null;

    // the current frame
    private VMFrame currFrame = null;

    // mapping from variables to their indices (in the frame)
    private Map<String,Integer> varMap = null;

    // the current variable index (in the frame)
    private int currVarIndex = 0;

    // to keep track of the typedecl objects for initialization
    Map<String,TypeDecl> typeDecls = new HashMap<>();
    // For representing "nil" as a value
    public static String NIL_OBJ = new String("nil");


    //----------------------------------------------------------------------
    // HELPER FUNCTIONS
    //----------------------------------------------------------------------

    // helper function to clean up uneeded NOP instructions
    private void fixNoOp() {
        int nextIndex = currFrame.instructions.size();
        // check if there are any instructions
        if (nextIndex == 0)
            return;
        // get the last instuction added
        VMInstr instr = currFrame.instructions.get(nextIndex - 1);
        // check if it is a NOP
        if (instr.opcode() == OpCode.NOP)
            currFrame.instructions.remove(nextIndex - 1);
    }

    private void fixCallStmt(Stmt s) {
        // get the last instuction added
        if (s instanceof CallExpr) {
            VMInstr instr = VMInstr.POP();
            instr.addComment("clean up call return value");
            currFrame.instructions.add(instr);
        }

    }

    //----------------------------------------------------------------------
    // Constructor
    //----------------------------------------------------------------------

    public CodeGenerator(TypeInfo typeInfo, VM vm) {
        this.typeInfo = typeInfo;
        this.vm = vm;
    }


    //----------------------------------------------------------------------
    // VISITOR FUNCTIONS
    //----------------------------------------------------------------------

    public void visit(Program node) throws MyPLException {

        // store UDTs for later
        for (TypeDecl tdecl : node.tdecls) {
            // add a mapping from type name to the TypeDecl
            typeDecls.put(tdecl.typeName.lexeme(), tdecl);
        }
        // only need to translate the function declarations
        for (FunDecl fdecl : node.fdecls)
            fdecl.accept(this);
    }

    public void visit(TypeDecl node) throws MyPLException {
        // Intentionally left blank -- nothing to do here
    }

    public void visit(FunDecl node) throws MyPLException {
        // TODO:

        VMFrame temp = new VMFrame(node.funName.lexeme(),node.params.size());
        vm.add(temp);
        currFrame = temp;
        // 1. create a new frame for the function

        varMap = new HashMap<String,Integer>();
        currVarIndex =0;
        // 2. create a variable mapping for the frame

        // 3. store args
        for (FunParam f : node.params){
            // TODO: might be the wrong order of stack operations
            varMap.put(f.paramName.lexeme(),currVarIndex);
            currFrame.instructions.add(VMInstr.STORE(currVarIndex));
            currVarIndex+=1;
        }
        // 4. visit statement nodes
        for (Stmt s : node.stmts) {
            s.accept(this);
        }
        // 5. check to see if the last statement was a return (if not, add
            if (currFrame.instructions.size()== 0){
                currFrame.instructions.add(VMInstr.PUSH(vm.NIL_OBJ));
                currFrame.instructions.add(VMInstr.VRET());
            }
            else if(currFrame.instructions.get(currFrame.instructions.size()-1) != VMInstr.VRET()){
                currFrame.instructions.add(VMInstr.PUSH(vm.NIL_OBJ));
                currFrame.instructions.add(VMInstr.VRET());
            }
        //    return nil)
    }

    public void visit(VarDeclStmt node) throws MyPLException {

        node.expr.accept(this);
        currFrame.instructions.add(VMInstr.STORE(currVarIndex));
        varMap.put(node.varName.lexeme(),currVarIndex);
        currVarIndex +=1;
    }

    public void visit(AssignStmt node) throws MyPLException {
        int var = -1;
        if (node.lvalue.size() == 1){
            var = varMap.get(node.lvalue.get(0).lexeme());
            node.expr.accept(this);
            currFrame.instructions.add(VMInstr.STORE(var));
        }
        else {
            int i =0;

            var = varMap.get(node.lvalue.get(0).lexeme());
            currFrame.instructions.add(VMInstr.LOAD(var));


            for (Token t :node.lvalue){
                if (! (i < 1)){
                currFrame.instructions.add(VMInstr.DUP());
                    if (i< node.lvalue.size()-1) {

                        currFrame.instructions.add(VMInstr.GETFLD(t.lexeme));
                    }
                    else{
                        node.expr.accept(this);
                        currFrame.instructions.add(VMInstr.SETFLD( t.lexeme));
                    }
                }
                i+=1;

            }
        }
//        node.expr.accept(this);
//        currFrame.instructions.add(VMInstr.STORE(var));
    }

    public void visit(CondStmt node) throws MyPLException {
        currFrame.instructions.add(VMInstr.PUSH(true));
        currFrame.instructions.add(VMInstr.STORE(currVarIndex));
        int my_Bool = currVarIndex;
        currVarIndex+=1;
        int Else = 0;
        int start =0;
        if (node.ifPart != null){
            node.ifPart.cond.accept(this);
            start = currFrame.instructions.size()-1;
            currFrame.instructions.add(VMInstr.JMPF(-1));
            for (Stmt s :node.ifPart.stmts){
                s.accept(this);
            }
            currFrame.instructions.add(VMInstr.PUSH(false));
            currFrame.instructions.add(VMInstr.STORE(my_Bool));
            currFrame.instructions.add(VMInstr.NOP());
            currFrame.instructions.set(start+1,VMInstr.JMPF(currFrame.instructions.size()-1));
        }
        for  (BasicIf B:node.elifs){
            currFrame.instructions.add(VMInstr.LOAD(my_Bool));
            B.cond.accept(this);
            currFrame.instructions.add(VMInstr.AND());
            start = currFrame.instructions.size()-1;
            currFrame.instructions.add(VMInstr.JMPF(-1));
            for (Stmt s :B.stmts){
                s.accept(this);
            }
            currFrame.instructions.add(VMInstr.PUSH(false));
            currFrame.instructions.add(VMInstr.STORE(my_Bool));
            currFrame.instructions.add(VMInstr.NOP());
            currFrame.instructions.set(start+1,VMInstr.JMPF(currFrame.instructions.size()-1));
        }
        if (node.elseStmts != null){
            currFrame.instructions.add(VMInstr.LOAD(my_Bool));
            currFrame.instructions.add(VMInstr.PUSH(true));
            currFrame.instructions.add(VMInstr.CMPEQ());
            start = currFrame.instructions.size()-1;
            currFrame.instructions.add(VMInstr.JMPF(-1));
            for (Stmt s : node.elseStmts){
                s.accept(this);
            }
            currFrame.instructions.add(VMInstr.NOP());
            currFrame.instructions.set(start+1,VMInstr.JMPF(currFrame.instructions.size()-1));
        }
    }

    public void visit(WhileStmt node) throws MyPLException {
        int start = currFrame.instructions.size();
        node.cond.accept(this);
        int first = currFrame.instructions.size();
        currFrame.instructions.add(VMInstr.JMPF(-1));
        for (Stmt s :node.stmts){
            s.accept(this);
        }
        currFrame.instructions.add(VMInstr.JMP(start));
        currFrame.instructions.add(VMInstr.NOP());
        currFrame.instructions.set(first,VMInstr.JMPF(currFrame.instructions.size()-1));
    }

    public void visit(ForStmt node) throws MyPLException {
        varMap.put(node.varName.lexeme(),currVarIndex);
        int counter = currVarIndex;
        currVarIndex +=1;

        node.start.accept(this);
        currFrame.instructions.add(VMInstr.STORE(counter));
        int strt_line = currFrame.instructions.size();
        node.end.accept(this);
        //currFrame.instructions.add(VMInstr.STORE(currVarIndex));


        currFrame.instructions.add(VMInstr.LOAD(counter));
        node.end.accept(this);
        if (node.upto){
            currFrame.instructions.add(VMInstr.CMPLT());
        }
        else {
            currFrame.instructions.add(VMInstr.CMPGT());
        }
        currFrame.instructions.add(VMInstr.NOT());
        int first = currFrame.instructions.size();
        currFrame.instructions.add(VMInstr.JMPF(-1));
        for (Stmt s :node.stmts){
            s.accept(this);
        }
        if (node.upto == false) {
            varMap.put(node.varName.lexeme(), currVarIndex);
            currFrame.instructions.add(VMInstr.PUSH(1));
            currFrame.instructions.add(VMInstr.LOAD(counter));
            currFrame.instructions.add(VMInstr.ADD());
            currFrame.instructions.add(VMInstr.STORE(counter));
        }
        else {
            varMap.put(node.varName.lexeme(), currVarIndex);
            currFrame.instructions.add(VMInstr.LOAD(counter));
            currFrame.instructions.add(VMInstr.PUSH(1));
            currFrame.instructions.add(VMInstr.SUB());
            currFrame.instructions.add(VMInstr.STORE(counter));

        }
        currFrame.instructions.add(VMInstr.JMP(strt_line));
        currFrame.instructions.add(VMInstr.NOP());
        currFrame.instructions.set(first,VMInstr.JMPF(currFrame.instructions.size()-1));





    }

    public void visit(ReturnStmt node) throws MyPLException {
        node.expr.accept(this);
        currFrame.instructions.add(VMInstr.VRET());
    }


    public void visit(DeleteStmt node) throws MyPLException {
        //varMap.get(node.varName.lexeme);
        currFrame.instructions.add(VMInstr.LOAD(varMap.get(node.varName.lexeme)));
        currFrame.instructions.add(VMInstr.FREE());
    }

    public void visit(CallExpr node) throws MyPLException {


        // push args (in order)
        Boolean needwait = false;
        int i =0;
        while (i < node.args.size()) {
            Boolean callthread = false;
            if (node.canTread.get(i) == true){

                //currFrame.instructions.add(VMInstr.THREADCALL(node.funName.lexeme()));
                callthread = true;
            }

                node.args.get(i).accept(this);
                if (callthread == true){
                    VMInstr name = currFrame.instructions.get(currFrame.instructions.size() - 1);
                    currFrame.instructions.set(currFrame.instructions.size()-1,VMInstr.THREADCALL((String) name.operand()));
                    needwait =true;
                }



            i = i + 1 ;
        }
        if (needwait){
            currFrame.instructions.add(VMInstr.WAIT());
        }
        // built-in functions:
        if (node.funName.lexeme().equals("print")) {
            currFrame.instructions.add(VMInstr.WRITE());
            currFrame.instructions.add(VMInstr.PUSH(VM.NIL_OBJ));
        }
        else if (node.funName.lexeme().equals("read"))
            currFrame.instructions.add(VMInstr.READ());
        else if (node.funName.lexeme().equals("stod"))
            currFrame.instructions.add(VMInstr.TODBL());
        else if (node.funName.lexeme().equals("stoi"))
            currFrame.instructions.add(VMInstr.TOINT());
        else if (node.funName.lexeme().equals("get"))
            currFrame.instructions.add(VMInstr.GETCHR());
        else if (node.funName.lexeme().equals("length"))
            currFrame.instructions.add(VMInstr.LEN());
        else if (node.funName.lexeme().equals("itos"))
            currFrame.instructions.add(VMInstr.TOSTR());
        else if (node.funName.lexeme().equals("dtos"))
            currFrame.instructions.add(VMInstr.TOSTR());

            // TODO: add remaining built in functions

            // user-defined functions

        else
            currFrame.instructions.add(VMInstr.CALL(node.funName.lexeme()));
        //currFrame.instructions.add(VMInstr.POP());
    }

    public void visit(SimpleRValue node) throws MyPLException {


        if (node.value.type() == TokenType.INT_VAL) {
            int val = Integer.parseInt(node.value.lexeme());
            currFrame.instructions.add(VMInstr.PUSH(val));
        }
        else if (node.value.type() == TokenType.DOUBLE_VAL) {
            double val = Double.parseDouble(node.value.lexeme());
            currFrame.instructions.add(VMInstr.PUSH(val));
        }
        else if (node.value.type() == TokenType.BOOL_VAL) {
            if (node.value.lexeme().equals("true"))
                currFrame.instructions.add(VMInstr.PUSH(true));
            else
                currFrame.instructions.add(VMInstr.PUSH(false));
        }
        else if (node.value.type() == TokenType.CHAR_VAL) {
            String s = node.value.lexeme();
            s = s.replace("\\n", "\n");
            s = s.replace("\\t", "\t");
            s = s.replace("\\r", "\r");
            s = s.replace("\\\\", "\\");
            currFrame.instructions.add(VMInstr.PUSH(s));
        }
        else if (node.value.type() == TokenType.STRING_VAL) {
            String s = node.value.lexeme();
            s = s.replace("\\n", "\n");
            s = s.replace("\\t", "\t");
            s = s.replace("\\r", "\r");
            s = s.replace("\\\\", "\\");
            currFrame.instructions.add(VMInstr.PUSH(s));
        }
        else if (node.value.type() == TokenType.NIL) {
            currFrame.instructions.add(VMInstr.PUSH(VM.NIL_OBJ));
        }
    }

    public void visit(NewRValue node) throws MyPLException {
       List <String> names= new ArrayList<>();
       //names.add("20");
    for (VarDeclStmt v:typeDecls.get(node.typeName.lexeme).vdecls){

        names.add(v.varName.lexeme());
    }

        currFrame.instructions.add(VMInstr.ALLOC(names));
        for (String s: names)
            currFrame.instructions.add(VMInstr.DUP());

        for(VarDeclStmt v  : typeDecls.get(node.typeName.lexeme).vdecls){
            v.expr.accept(this);
            currFrame.instructions.add(VMInstr.SETFLD(v.varName.lexeme));
        }

    }

    public void visit(IDRValue node) throws MyPLException {
       if (node.path.size() == 1)
        currFrame.instructions.add(VMInstr.LOAD(varMap.get(node.path.get(0).lexeme())));
       else {
           int i =0;
           for (Token T :node.path){
               if (i ==0){
                   currFrame.instructions.add(VMInstr.LOAD(varMap.get(node.path.get(0).lexeme())));
               }
               else if (i < node.path.size()){
                   currFrame.instructions.add(VMInstr.GETFLD(T.lexeme()));
               }
               i+=1;


           }

       }
    }

    public void visit(NegatedRValue node) throws MyPLException {

        node.expr.accept(this);
        currFrame.instructions.add(VMInstr.NEG());


    }

    public void visit(Expr node) throws MyPLException {
        if (node.first!= null){
        node.first.accept(this);}
        if (node.logicallyNegated){
            currFrame.instructions.add(VMInstr.NOT());
        }
        if (node.rest!=null) {
            node.rest.accept(this);
            if (node.op!= null) {
                String op = node.op.lexeme();
                if (op.equals("+")) {
                    currFrame.instructions.add(VMInstr.ADD());
                } else if (op.equals("-")) {
                    currFrame.instructions.add(VMInstr.SUB());
                } else if (op.equals("*")) {
                    currFrame.instructions.add(VMInstr.MUL());
                } else if (op.equals("/")) {
                    currFrame.instructions.add(VMInstr.DIV());
                } else if (op.equals(">")) {
                    currFrame.instructions.add(VMInstr.CMPGT());
                } else if (op.equals(">=")) {
                    currFrame.instructions.add(VMInstr.CMPGE());
                } else if (op.equals("<")) {
                    currFrame.instructions.add(VMInstr.CMPLT());
                } else if (op.equals("<=")) {
                    currFrame.instructions.add(VMInstr.CMPLE());
                } else if (op.equals("%")) {
                    currFrame.instructions.add(VMInstr.MOD());
                } else if (op.equals("==")) {
                    currFrame.instructions.add(VMInstr.CMPEQ());
                } else if (op.equals("!=")) {
                    currFrame.instructions.add(VMInstr.CMPNE());
                } else if (op.equals("and")) {
                    currFrame.instructions.add(VMInstr.AND());
                } else if (op.equals("or")) {
                    currFrame.instructions.add(VMInstr.OR());
                }
            }

        }

    }

    public void visit(SimpleTerm node) throws MyPLException {
        if (node.rvalue!= null){
        node.rvalue.accept(this);}
    }

    public void visit(ComplexTerm node) throws MyPLException {
        // defer to contained expression
        node.expr.accept(this);
    }

}
