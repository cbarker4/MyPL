/*
 * File: HW6.java
 * Date: Spring 2022
 * Auth:
 * Desc: Example program to test the MyPL VM
 */


/*----------------------------------------------------------------------
   Your job for this part of the assignment is to imlement the
   following as a set of MyPL VM instructions and the VM. Note that
   you must implement the is_prime function and generally folow the
   approach laid out below. You can view the following as pseudocode
   (which could have been written in any procedural programming
   language). Note that since we don't have a square root function in
   MyPL, our naive primality tester is not very efficient.

    fun bool is_prime(int n) {
      var m = n / 2
      var v = 2
      while v <= m {
        var r = n / v
        var p = r * v
        if p == n {
          return false
        }
        v = v + 1
      }
      return true
    }

    fun void main() {
      print("Please enter integer values to sum (prime number to quit)\n")
      var sum = 0
      while true {
        print(">> Enter an int: ")
        var val = stoi(read())
        if is_prime(val) {
          print("The sum is: " + itos(sum) + "\n")
          print("Goodbye!\n")
          return
        }
        sum = sum + val
      }
    }
----------------------------------------------------------------------*/

public class HW6 {

    public static void main(String[] args) throws Exception {
        VM vm = new VM();
        VMFrame f = new VMFrame("is_prime", 1);
        vm.add(f);
        //n:0 m:1 v:2 r:3
        f.instructions.add(VMInstr.DUP());
        f.instructions.add(VMInstr.STORE(0));   // stores input n
        f.instructions.add(VMInstr.PUSH(2));
        f.instructions.add(VMInstr.DIV());      // n/2
        f.instructions.add(VMInstr.STORE(1));   //m =
        f.instructions.add(VMInstr.PUSH(2));
        f.instructions.add(VMInstr.STORE(2));  // v = 2


        //f.instructions.add(VMInstr.STORE(1));

        f.instructions.add(VMInstr.LOAD(2)); //m

        f.instructions.add(VMInstr.LOAD(1)); // v
        f.instructions.add(VMInstr.CMPLE());    // start of while v<m here
        f.instructions.add(VMInstr.JMPF(31));
        f.instructions.add(VMInstr.LOAD(0)); // 11  n
        f.instructions.add(VMInstr.LOAD(2)); // v
        f.instructions.add(VMInstr.DIV()); // n/v
        f.instructions.add(VMInstr.DUP());
        f.instructions.add(VMInstr.STORE(3)); //r
        f.instructions.add(VMInstr.LOAD(2));
        f.instructions.add(VMInstr.MUL());  //r*v

        f.instructions.add(VMInstr.LOAD(0));//
        f.instructions.add(VMInstr.CMPEQ()); // p ==n
        f.instructions.add(VMInstr.NOT());
        f.instructions.add(VMInstr.JMPF(33));   //
        f.instructions.add(VMInstr.PUSH(1));
        f.instructions.add(VMInstr.LOAD(2));
        f.instructions.add(VMInstr.ADD());
        f.instructions.add(VMInstr.STORE(2));
        f.instructions.add(VMInstr.LOAD(1));
        f.instructions.add(VMInstr.LOAD(2));
        f.instructions.add(VMInstr.CMPLE());
        f.instructions.add(VMInstr.NOT());
        f.instructions.add(VMInstr.JMPF(7));
        f.instructions.add(VMInstr.PUSH(true));
        f.instructions.add(VMInstr.VRET()); // 32
        f.instructions.add(VMInstr.PUSH(false));
        f.instructions.add(VMInstr.VRET());  // 35








        VMFrame main = new VMFrame("main", 0);
        vm.add(main);
        main.instructions.add(VMInstr.PUSH("Please enter integer values to sum (prime number to quit) \n"));
        main.instructions.add(VMInstr.WRITE());                     //1
        main.instructions.add(VMInstr.PUSH(0));                     //2
        main.instructions.add(VMInstr.STORE(0));                    //3
        main.instructions.add(VMInstr.PUSH(">> Enter an int: "));   //4
        main.instructions.add(VMInstr.WRITE());
        main.instructions.add(VMInstr.READ());                      //5
        main.instructions.add(VMInstr.TOINT());
        main.instructions.add(VMInstr.DUP());
        main.instructions.add(VMInstr.STORE(1));
        main.instructions.add(VMInstr.CALL("is_prime"));
        main.instructions.add(VMInstr.JMPF(19));
        main.instructions.add(VMInstr.PUSH("The sum is: "));   //4
        main.instructions.add(VMInstr.WRITE());
        main.instructions.add(VMInstr.LOAD(0));
        main.instructions.add(VMInstr.WRITE());
        main.instructions.add(VMInstr.PUSH("\n Goodbye! \n"));
        main.instructions.add(VMInstr.WRITE());
        main.instructions.add(VMInstr.JMP(24));
        //main.instructions.add(VMInstr.PUSH(VM.NIL_OBJ));
        //main.instructions.add(VMInstr.VRET()); // 19
        main.instructions.add(VMInstr.LOAD(0));
        main.instructions.add(VMInstr.LOAD(1));
        main.instructions.add(VMInstr.ADD());
        main.instructions.add(VMInstr.STORE(0));
        main.instructions.add(VMInstr.JMP(4));
        main.instructions.add(VMInstr.PUSH(0));
        vm.run();
    }
}
