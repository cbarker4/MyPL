/*
        * File: VM.java
        * Date: Spring 2022
        * Auth:
        * Desc: A bare-bones MyPL Virtual Machine. The architecture is based
        *       loosely on the architecture of the Java Virtual Machine
        *       (JVM).  Minimal error checking is done except for runtime
        *       program errors, which include: out of bound indexes,
        *       dereferencing a nil reference, and invalid value conversion
        *       (to int and double).
        */


import java.util.*;
import java.util.concurrent.Semaphore;


/*----------------------------------------------------------------------
  TODO: Your main job for HW-6 is to finish the VM implementation
        below by finishing the handling of each instruction.
        Note that PUSH, NOT, JMP, READ, FREE, and NOP (trivially) are
        completed already to help get you started. 
        Be sure to look through OpCode.java to get a basic idea of
        what each instruction should do as well as the unit tests for
        additional details regarding the instructions.
        Note that you only need to perform error checking if the
        result would lead to a MyPL runtime error (where all
        compile-time errors are assumed to be found already). This
        includes things like bad indexes (in GETCHR), dereferencing
        and/or using a NIL_OBJ (see the ensureNotNil() helper
        function), and converting from strings to ints and doubles. An
        error() function is provided to help generate a MyPLException
        for such cases.
----------------------------------------------------------------------*/


class VM extends Thread{
    private Thread t;
    private String t_name = "main";
    private  Integer t_count = 0;
    static Map<Integer, Object> thrRET = new HashMap<Integer, Object>();
    // set to true to print debugging information
    private boolean DEBUG = false;
    private boolean ThreadDEBUG = false;
    static Semaphore s = new Semaphore(2);
    // the VM's heap (free store) accessible via object-id
    private Map<Integer,Map<String,Object>> heap = new HashMap<>();
    static List<Boolean> done= new ArrayList<>();
    // next available object-id
    private int objectId = 1111;

    // the frames for the program (one frame per function)
    private Map<String,VMFrame> frames = new HashMap<>();

    // the VM call stack
    private Deque<VMFrame> frameStack = new ArrayDeque<>();

    private String funname = "main";
    private VMFrame frame = null;

    /**
     * For representing "nil" as a value
     */
    public static String NIL_OBJ = new String("nil");


    public VM() {

    }

    public VM(Map<String,VMFrame> newframes,String newname) {
        frames = newframes;
        funname = newname;
    }

    public VM(VMFrame frame2) {
        frame = frame2;
    }

    public VM(VMFrame frame2, String newname) {
        frame = frame2;
        funname = newname;
    }

    public VM(VMFrame frame2, String newname, Integer t_count2) {
        frame = frame2;
        funname = newname;
        t_count = t_count2;
    }

    public VM(Map<String,VMFrame> map, String newname, Integer t_count2) {
        funname = newname;
        t_count = t_count2;
        frames= map;

    }

    public VM(Map<String,VMFrame> map, String newname, Integer t_count2, VMFrame temp) {
        funname = newname;
        t_count = t_count2;
        frames= map;
        frame = temp;
        done.add(false);
        t_name = newname + t_count2;

    }


    /**
     * Add a frame to the VM's list of known frames
     * @param frame the frame to add
     */
    public void add(VMFrame frame) {
        frames.put(frame.functionName(), frame);
    }

    /**
     * Turn on/off debugging, which prints out the state of the VM prior
     * to each instruction.
     * @param debug set to true to turn on debugging (by default false)
     */
    public void setDebug(boolean debug) {
        DEBUG = debug;
    }

    /**
     * Run the virtual machine
     */
    public void run() {
        try {
            if (ThreadDEBUG)
                System.out.println("Grabbing"+ t.getName());
            s.acquire(1);

            // grab the main stack frame
            if (frame == null)
                frame = frames.get(funname).instantiate();
            if (frameStack.size()==1){
                frame = frameStack.peek();
                frameStack.pop();}

//            if (frames.containsKey(funname) ) {
//                frame = frames.get(funname).instantiate();
//            }
//            else
//
//               frameStack.push(frame);
//               funname = "main";
//
//            else {
//                System.out.println("IM HERERE");
//               frame = frames.get(funname).instantiate();
//                frameStack.push(frame);
//            }


            // run loop (keep going until we run out of frames or
            // instructions) note that we assume each function returns a
            // value, and so the second check below should never occur (but is
            // useful for testing, etc).
            while (frame != null && frame.pc < frame.instructions.size()) {
                //System.out.println(funname + t_count.toString());
                // get next instruction
                VMInstr instr = frame.instructions.get(frame.pc);
                // increment instruction pointer
                ++frame.pc;
                //System.out.println(instr.opcode().toString());

                // For debugging: to turn on the following, call setDebug(true)
                // on the VM.
                if (DEBUG) {
                    System.out.println();
                    System.out.println("\t Thread.......: " + t.getName());
                    System.out.println("\t FRAME........: " + frame.functionName());
                    System.out.println("\t PC...........: " + (frame.pc - 1));
                    System.out.println("\t INSTRUCTION..: " + instr);
                    System.out.println("\t OPERAND STACK: " + frame.operandStack);
                    System.out.println("\t HEAP ........: " + heap);
                }


                //------------------------------------------------------------
                // Consts/Vars
                //------------------------------------------------------------

                if (instr.opcode() == OpCode.PUSH) {
                    frame.operandStack.push(instr.operand());
                } else if (instr.opcode() == OpCode.POP) {
                    frame.operandStack.pop();
                } else if (instr.opcode() == OpCode.LOAD) {
                    frame.operandStack.push(frame.variables.get((Integer) instr.operand()));
                } else if (instr.opcode() == OpCode.STORE) {
                    Object x = frame.operandStack.pop();
                    int temp = Integer.parseInt(instr.operand().toString());
                    if (temp >= frame.variables.size()) {
                        //frame.variables.add(x);
                        frame.variables.put(temp, x);
                    } else {
                        frame.variables.replace(temp, x);
                    }

                }


                //------------------------------------------------------------
                // Ops
                //------------------------------------------------------------

                else if (instr.opcode() == OpCode.ADD) {
                    Object x = frame.operandStack.pop();
                    Object y = frame.operandStack.pop();
                    ensureNotNil(frame, y);
                    ensureNotNil(frame, x);

                    if (x instanceof Integer) {
                        int X = ((Integer) x);
                        int Y = ((Integer) y);
                        frame.operandStack.push(X + Y);
                    }

                    if (x instanceof Double) {
                        double X = ((Double) x);
                        double Y = ((Double) y);
                        frame.operandStack.push(X + Y);
                    }

                    if (x instanceof String) {
                        String X = ((String) x);
                        String Y = ((String) y);
                        frame.operandStack.push(Y + X);
                    }
                    // frame.operandStack.push(frame.operandStack.pop() + frame.operandStack.pop());
                } else if (instr.opcode() == OpCode.SUB) {
                    Object x = frame.operandStack.pop();
                    Object y = frame.operandStack.pop();

                    if (x instanceof Integer) {
                        int X = ((Integer) x);
                        int Y = ((Integer) y);
                        frame.operandStack.push(Y - X);
                    }

                    if (x instanceof Double) {
                        double X = ((Double) x);
                        double Y = ((Double) y);
                        frame.operandStack.push(Y - X);
                    }

                } else if (instr.opcode() == OpCode.MUL) {
                    Object x = frame.operandStack.pop();
                    Object y = frame.operandStack.pop();

                    if (x instanceof Integer) {
                        int X = ((Integer) x);
                        int Y = ((Integer) y);
                        frame.operandStack.push(X * Y);
                    }

                    if (x instanceof Double) {
                        double X = ((Double) x);
                        double Y = ((Double) y);
                        frame.operandStack.push(X * Y);
                    }
                } else if (instr.opcode() == OpCode.DIV) {
                    Object x = frame.operandStack.pop();
                    Object y = frame.operandStack.pop();

                    if (x instanceof Integer) {
                        int X = ((Integer) x);
                        int Y = ((Integer) y);
                        frame.operandStack.push(Y / X);
                    } else if (x instanceof Double) {
                        double X = ((Double) x);
                        double Y = ((Double) y);
                        frame.operandStack.push(Y / X);
                    }
                } else if (instr.opcode() == OpCode.MOD) {
                    Object x = frame.operandStack.pop();
                    Object y = frame.operandStack.pop();
                    int X = ((Integer) x);
                    int Y = ((Integer) y);
                    frame.operandStack.push(Y % X);
                } else if (instr.opcode() == OpCode.AND) {
                    Object y = frame.operandStack.pop();
                    Object x = frame.operandStack.pop();
                    ensureNotNil(frame, y);
                    ensureNotNil(frame, x);
                    Boolean Y = ((Boolean) y);
                    Boolean X = ((Boolean) x);
                    frame.operandStack.push(X && Y);

                } else if (instr.opcode() == OpCode.OR) {
                    Object y = frame.operandStack.pop();
                    Object x = frame.operandStack.pop();
                    Boolean Y = ((Boolean) y);
                    Boolean X = ((Boolean) x);
                    frame.operandStack.push(X || Y);
                } else if (instr.opcode() == OpCode.NOT) {
                    Object operand = frame.operandStack.pop();
                    if (operand instanceof Boolean) {
                        ensureNotNil(frame, operand);
                        frame.operandStack.push(!(boolean) operand);
                    } else if (operand instanceof Integer) {
                        ensureNotNil(frame, operand);
                        frame.operandStack.push((Integer) 0 - (Integer) operand);
                    } else if (operand instanceof Double) {
                        ensureNotNil(frame, operand);
                        frame.operandStack.push((Double) 0.0 - (Double) operand);
                    }
                } else if (instr.opcode() == OpCode.CMPLT) {
                    Object y = frame.operandStack.pop();
                    Object x = frame.operandStack.pop();
                    if (x instanceof Integer) {
                        int X = ((Integer) x);
                        int Y = ((Integer) y);
                        frame.operandStack.push(X < Y);
                    }

                    if (x instanceof Double) {
                        double X = ((Double) x);
                        double Y = ((Double) y);
                        frame.operandStack.push(X < Y);
                    } else if (x instanceof String) {
                        String X = ((String) x);
                        String Y = ((String) y);
                        int i = X.compareTo(Y);
                        if (i < 0) {
                            frame.operandStack.push(true);
                        } else {
                            frame.operandStack.push(false);
                        }
                    }
                } else if (instr.opcode() == OpCode.CMPLE) {
                    Object y = frame.operandStack.pop();
                    Object x = frame.operandStack.pop();
                    if (x instanceof Integer) {
                        int X = ((Integer) x);
                        int Y = ((Integer) y);
                        frame.operandStack.push(X <= Y);
                    }

                    if (x instanceof Double) {
                        double X = ((Double) x);
                        double Y = ((Double) y);
                        frame.operandStack.push(X <= Y);
                    } else if (x instanceof String) {
                        String X = ((String) x);
                        String Y = ((String) y);
                        int i = X.compareTo(Y);
                        if (i <= 0) {
                            frame.operandStack.push(true);
                        } else {
                            frame.operandStack.push(false);
                        }
                    }
                } else if (instr.opcode() == OpCode.CMPGT) {
                    Object y = frame.operandStack.pop();
                    Object x = frame.operandStack.pop();
                    if (x instanceof Integer) {
                        int X = ((Integer) x);
                        int Y = ((Integer) y);
                        frame.operandStack.push(X > Y);
                    } else if (x instanceof Double) {
                        double X = ((Double) x);
                        double Y = ((Double) y);
                        frame.operandStack.push(X > Y);
                    } else if (x instanceof String) {
                        String X = ((String) x);
                        String Y = ((String) y);
                        int i = X.compareTo(Y);
                        if (i >= 1) {
                            frame.operandStack.push(true);
                        } else {
                            frame.operandStack.push(false);
                        }
                    }
                } else if (instr.opcode() == OpCode.CMPGE) {
                    Object y = frame.operandStack.pop();
                    Object x = frame.operandStack.pop();
                    if (x instanceof Integer) {
                        int X = ((Integer) x);
                        int Y = ((Integer) y);
                        frame.operandStack.push(X >= Y);
                    } else if (x instanceof Double) {
                        double X = ((Double) x);
                        double Y = ((Double) y);
                        frame.operandStack.push(X >= Y);
                    } else if (x instanceof String) {
                        String X = ((String) x);
                        String Y = ((String) y);
                        int i = X.compareTo(Y);
                        if (i >= 0) {
                            frame.operandStack.push(true);
                        } else {
                            frame.operandStack.push(false);
                        }
                    }
                } else if (instr.opcode() == OpCode.CMPEQ) {
                    Object y = frame.operandStack.pop();
                    Object x = frame.operandStack.pop();
                    frame.operandStack.push(x.equals(y));

                } else if (instr.opcode() == OpCode.CMPNE) {
                    Object y = frame.operandStack.pop();
                    Object x = frame.operandStack.pop();
                    frame.operandStack.push(!x.equals(y));
//
                } else if (instr.opcode() == OpCode.NEG) {
                    Object x = frame.operandStack.pop();

                    if (x instanceof Integer) {
                        int X = ((Integer) x);

                        frame.operandStack.push(-1 * X);
                    }

                    if (x instanceof Double) {
                        Double X = ((Double) x);
                        frame.operandStack.push(-1 * X);
                    }
                }


                //------------------------------------------------------------
                // Jumps
                //------------------------------------------------------------

                else if (instr.opcode() == OpCode.JMP) {
                    frame.pc = (int) instr.operand();
                } else if (instr.opcode() == OpCode.JMPF) {
                    Boolean x = (Boolean) frame.operandStack.pop();
                    if (x == false)
                        frame.pc = (int) instr.operand();

                }

                //------------------------------------------------------------
                // Functions
                //------------------------------------------------------------

                else if (instr.opcode() == OpCode.CALL) {

                    VMFrame f2 = frames.get(instr.operand().toString());
                    VMFrame temp = f2.instantiate();
                    int i = 0;
                    while (i < temp.argCount()) {
                        temp.operandStack.push(frame.operandStack.pop());
                        i = i + 1;
                    }
                    frameStack.push(temp);
                    frame = temp;

                } else if (instr.opcode() == OpCode.VRET) {

                    Object x = frame.operandStack.pop();

                    if (frameStack.size()==1 && t.getName().toString().equals("main0")){
                        frame = frameStack.peek();
                        frame.operandStack.push(x);


                        //return;
                    }
                    else {
                        try {


                        frameStack.pop();

                        if (frameStack.size() == 0 && !(t.getName().toString().equals("main0"))) {//t_count>0){
                            thrRET.put(t_count, x);
                            if(ThreadDEBUG)
                                System.out.println("Relesing " + t_name);
                            s.release();
                            done.set(t_count - 1, true);
                            t.interrupt();
                            //return;
                        }} catch (Exception e )
                        { t.interrupt();}


                        //            frameStack.peek();
                        if (!frameStack.isEmpty()) {
                            frame = frameStack.peek();
                            frame.operandStack.push(x);
                        }
                    }


                }

                //------------------------------------------------------------
                // Built-ins
                //------------------------------------------------------------

                else if (instr.opcode() == OpCode.WRITE) {
                    System.out.print(frame.operandStack.pop());
                } else if (instr.opcode() == OpCode.READ) {
                    Scanner s = new Scanner(System.in);
                    frame.operandStack.push(s.nextLine());
                } else if (instr.opcode() == OpCode.LEN) {
                    Object i = frame.operandStack.pop();
                    String s = ((String) i);
                    frame.operandStack.push(s.length());
                } else if (instr.opcode() == OpCode.GETCHR) {
                    try {
                        Object x = frame.operandStack.pop();
                        Object y = frame.operandStack.pop();
                        String X = (String) x;
                        int Y = (int) y;
                        frame.operandStack.push(X.substring(Y, Y + 1));
                    } catch (Exception e) {
                        error("bad indexing of string", frame);
                    }

                } else if (instr.opcode() == OpCode.TOINT) {
                    try {
                        Object x = frame.operandStack.pop();
                        String temp = x.toString();
                        int i = 0;
                        boolean doub = false;
                        int bad = 0;
                        for (char in : temp.toCharArray()) {
                            if (in == '.') {
                                doub = true;
                                bad = i;
                            }
                            i = i + 1;
                        }
                        if (doub == false) {
                            bad = temp.length();
                        }
                        frame.operandStack.push(Integer.parseInt(temp.substring(0, bad)));
                    } catch (Exception e) {
                        error("Cannot turn into an int", frame);
                    }
                } else if (instr.opcode() == OpCode.TODBL) {
                    try {
                        Object x = frame.operandStack.pop();
                        String temp = x.toString();
                        frame.operandStack.push(Double.parseDouble(temp));
                    } catch (Exception e) {
                        error("Cannot convert to double", frame);
                    }
                } else if (instr.opcode() == OpCode.TOSTR) {
                    Object x = frame.operandStack.pop();
                    frame.operandStack.push((x.toString()));
                }

                //------------------------------------------------------------
                // Heap related
                //------------------------------------------------------------

                else if (instr.opcode() == OpCode.ALLOC) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    List<String> f = (List<String>) instr.operand();
                    for (String s : f) {
                        map.put(s, NIL_OBJ);
                    }
                    heap.put(objectId, map);
                    frame.operandStack.push(objectId);
                    objectId++;
                } else if (instr.opcode() == OpCode.FREE) {
                    // pop the oid to
                    Object oid = frame.operandStack.pop();
                    ensureNotNil(frame, oid);
                    // remove the object with oid from the heap
                    heap.remove((int) oid);
                } else if (instr.opcode() == OpCode.SETFLD) {
                    Object x = frame.operandStack.pop();
                    Object y = frame.operandStack.pop();
                    heap.get(y).replace(instr.operand().toString(), x);
                } else if (instr.opcode() == OpCode.GETFLD) {
                    Object x = frame.operandStack.pop();
                    try {
                        frame.operandStack.push(heap.get(x).get(instr.operand()));
                    } catch (Exception e) {
                        error("Null pointer Exception", frame);
                    }
                }

                //------------------------------------------------------------
                // Special instructions
                //------------------------------------------------------------

                else if (instr.opcode() == OpCode.DUP) {
                    Object o = frame.operandStack.pop();
                    frame.operandStack.push(o);
                    frame.operandStack.push(o);
                } else if (instr.opcode() == OpCode.SWAP) {
                    Object x = frame.operandStack.pop();
                    Object y = frame.operandStack.pop();
                    frame.operandStack.push(x);
                    frame.operandStack.push(y);

                } else if (instr.opcode() == OpCode.NOP) {
                    // do nothing
                }

                else if (instr.opcode() == OpCode.THREADCALL) {
                 //   System.out.println("f stack "+frameStack.size());
                    VMFrame f2 = frames.get(instr.operand().toString());
                    VMFrame temp = f2.instantiate();
                    int i = 0;
                    while (i < temp.argCount()) {
                        temp.operandStack.push(frame.operandStack.pop());
                        i = i + 1;
                    }
                    //frameStack.push(temp);
                    //TODO remove slows
                    t_count = t_count +1;
                    t_name = t_count.toString() + t.getName();

                    frameStack.push(frame);
                    Map<String, VMFrame> map = new HashMap<>(frames);
                    //VMFrame temp2 = new VMFrame(temp);
                    VM pls = new VM(map,instr.operand().toString(),t_count,temp);
                    if(ThreadDEBUG)
                        System.out.println("Relesing " + t.getName());
 //                   if (s.)
                    s.release();
                    pls.start();

                }

                else if (instr.opcode() == OpCode.WAIT) {
                   // System.out.println("Before"+frameStack.size());
                  // while (done.get(0)!=true&&done.get(1)!=true){
                     //  t.wait();
  //                      System.out.println(done.get(0));
  //                      System.out.println(done.get(1));
                    //}
                   // System.out.println(done.get(0)!=true&&done.get(1)!=true);
                   // System.out.println(done.get(0));
                   // System.out.println(done.get(1));
                    //    t.wait(100);
//                        System.out.println(t_count);

                        s.acquire(3);
                    if(ThreadDEBUG)
                        System.out.println("Aquired" + t.getName());
                    //System.out.println(frameStack.size());
                        int i =1;
                        while (i < t_count + 1){
//                            System.out.print(" what index");
                         //  System.out.println(thrRET.get(i));
                            frame.operandStack.push(thrRET.get(i));

                            i= i+1;
                        }
                      //  System.out.println("HI");
                    for( Object x : frame.operandStack){
                      //  System.out.println(x);
                    }
                  //  System.out.println(frame.pc);

                    }

                    //frameStack.pop();


            }
        }catch (MyPLException e){
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    // to print the lists of instructions for each VM Frame
    @Override
    public String toString() {
        String s = "";
        for (Map.Entry<String,VMFrame> e : frames.entrySet()) {
            String funName = e.getKey();
            s += "Frame '" + funName + "'\n";
            List<VMInstr> instructions = e.getValue().instructions;
            for (int i = 0; i < instructions.size(); ++i) {
                VMInstr instr = instructions.get(i);
                s += "  " + i + ": " + instr + "\n";
            }
            // s += "\n";
        }
        return s;
    }


    //----------------------------------------------------------------------
    // HELPER FUNCTIONS
    //----------------------------------------------------------------------

    // error
    private void error(String m, VMFrame f) throws MyPLException {
        int pc = f.pc - 1;
        VMInstr i = f.instructions.get(pc);
        String name = f.functionName();
        m += " (in " + name + " at " + pc + ": " + i + ")";
        throw MyPLException.VMError(m);
    }

    // error if given value is nil
    private void ensureNotNil(VMFrame f, Object v) throws MyPLException {
        if (v == NIL_OBJ)
            error("Nil reference", f);
    }
    public void start(){
        //'if(ThreadDEBUG)
        //System.out.println("Starting " +  funname +t_count.toString());
        //int idk = 1;
        System.out.print(" ");       // uh idk why i have to have this
        t = new Thread (this, funname+t_count.toString());
        t.start();
    }


}