import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.HashMap;

public class Processor {
    static String code = "";
    static int PC = 0;
    static char icode, ifun = '0';
    static int rA, rB, valC, valP = 0;
    static int valA, valB = 0;
    static int cnd, valE = 0;
    static int valM = 0;

    //consider each element of the stack array to be one line in the stack (8 bytes, so that it's in sync with the length of valC and pointers)
    static int[] stack = new int[300];

    public static void main(String[] args) {
        createAndShowGUI();
    }

    public static void createAndShowGUI() {
        JFrame frame = new JFrame("Y86 Processor");
        frame.setSize(1600, 900);
        frame.setLayout(new GridLayout(3, 1));

        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.PAGE_AXIS));

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent){
                System.exit(0);
            }        
        });   

        JLabel PCLabel = new JLabel("",JLabel.CENTER);   
        PCLabel.setFont(new Font("Calibri", Font.BOLD, 20));
        PCLabel.setSize(100,200);
        JLabel MemoryLabel = new JLabel("",JLabel.CENTER);  
        MemoryLabel.setFont(new Font("Calibri", Font.BOLD, 20));      
        MemoryLabel.setSize(100,200);
        JLabel ExecuteLabel = new JLabel("",JLabel.CENTER);
        ExecuteLabel.setFont(new Font("Calibri", Font.BOLD, 20));        
        ExecuteLabel.setSize(100,200);
        JLabel DecodeLabel = new JLabel("",JLabel.CENTER);  
        DecodeLabel.setFont(new Font("Calibri", Font.BOLD, 20));      
        DecodeLabel.setSize(100,200);
        JLabel FetchLabel = new JLabel("",JLabel.CENTER);   
        FetchLabel.setFont(new Font("Calibri", Font.BOLD, 20));     
        FetchLabel.setSize(100,200);
        JLabel ConditionLabel = new JLabel("",JLabel.CENTER);
        ConditionLabel.setFont(new Font("Calibri", Font.BOLD, 20));        
        ConditionLabel.setSize(100,200);
        JLabel RegisterLabel = new JLabel("",JLabel.CENTER);
        RegisterLabel.setFont(new Font("Calibri", Font.BOLD, 20));        
        RegisterLabel.setSize(100,200);

        
        labelPanel.add(FetchLabel);
        labelPanel.add(Box.createRigidArea(new Dimension(0,5)));
        labelPanel.add(DecodeLabel);
        labelPanel.add(Box.createRigidArea(new Dimension(0,5)));
        labelPanel.add(ExecuteLabel);
        labelPanel.add(Box.createRigidArea(new Dimension(0,5)));
        labelPanel.add(MemoryLabel);
        labelPanel.add(Box.createRigidArea(new Dimension(0,5)));
        labelPanel.add(PCLabel);
        labelPanel.add(Box.createRigidArea(new Dimension(0,5)));

        labelPanel.add(ConditionLabel);
        labelPanel.add(Box.createRigidArea(new Dimension(0,5)));
        labelPanel.add(RegisterLabel);
        labelPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));


        JButton runButton = new JButton("Run Instruction");

        //Make the GUI element for memory
        int n = stack.length;
        DefaultTableModel model = new DefaultTableModel();
        //Col
        for (int i = 0; i < n; i++) {
            model.addColumn(String.format("0x%03x", i));
        }
        //Row
        Object[] row = new Object[n];
        for (int i = 0; i < n; i++) {
            row[i] = stack[i];
        }
        model.addRow(row);

        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane scrollPane = new JScrollPane(table);



        runButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    code = Files.lines(Paths.get("file.txt")).collect(Collectors.joining());
                    int code_bytelen = code.length()/2;
                    for (int i=0; i<code_bytelen; i++){
                        String sub = code.substring(2*i,2*i+2);
                        stack[i] = Integer.parseInt(sub, 16);
                    }
                    model.fireTableDataChanged();


                    fetch(code);
                    PCLabel.setText("PC: " + String.format("0x%03x", PC) + "     valP:" + String.format("0x%03x", valP));
                    MemoryLabel.setText("valM: " + valM);
                    ExecuteLabel.setText("ValE: " + String.format("0x%03x", valE));
                    DecodeLabel.setText("valA: " + String.format("0x%03x", valA) + "     valB: " + String.format("0x%03x", valB));
                    FetchLabel.setText("icode,ifun: 0x"+ icode +ifun + "     rA,rB: " + String.format("0x%01x", rA*16+rB) + "     valC: " + String.format("0x%03x", valC));
                    ConditionLabel.setText("Condition: " + String.valueOf(cnd) + "     ZF: " + conditionCodes.get("ZF") + "     SF: " + conditionCodes.get("SF")+ "     OF: " + conditionCodes.get("OF")); 
                    RegisterLabel.setText(  "%eax: "+ String.format("0x%03x", registers.get(0)) + "     " +
                                            "%ecx: "+ String.format("0x%03x", registers.get(1)) + "     " +
                                            "%edx: "+ String.format("0x%03x", registers.get(2)) + "     " +
                                            "%ebx: "+ String.format("0x%03x", registers.get(3)) + "     " +
                                            "%esi: "+ String.format("0x%03x", registers.get(6)) + "     " +
                                            "%edi: "+ String.format("0x%03x", registers.get(7)) + "     " +
                                            "%esp: "+ String.format("0x%03x", registers.get(4)) + "     " +
                                            "%ebp: "+ String.format("0x%03x", registers.get(5)) + "     " );

                    for (int i = 0; i < n; i++) {
                        model.setValueAt(String.format("%02x", stack[i]), 0, i); // Update row 0, column i
                    }
                    model.fireTableDataChanged();

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });


        frame.add(runButton);
        frame.add(labelPanel);
        frame.add(scrollPane);
        frame.setVisible(true);
    }

    public static void fetch(String code){
        icode = code.charAt(2*PC);
        ifun = code.charAt(2*PC+1);
        switch(icode){
            case '0': //halt
                System.exit(0);
            //how to deal with a 0? is it padding or halt? padding in between instructions probably doesn't happen
                break;
            case '1': //nop
                valP = PC + 1;
                break;
            case '2': //rrmovl + cmovXX
                rA = Integer.parseInt(String.valueOf(code.charAt(2*(PC+1))), 16);
                rB = Integer.parseInt(String.valueOf(code.charAt(2*(PC+1)+1)), 16);
                valP = PC + 2;
                break;
            //valC is now an integer, not hex - no need to worry about conversion from here
            case '3': //irmovl
                rA = 15;
                rB = Integer.parseInt(String.valueOf(code.charAt(2*(PC+1)+1)), 16);
                valC = (int) Long.parseUnsignedLong(code.substring(2*(PC+2), 2*(PC+10)), 16);
                valP = PC + 10;
                break;
            case '4': //rmmovl
                rA = Integer.parseInt(String.valueOf(code.charAt(2*(PC+1))), 16);
                rB = Integer.parseInt(String.valueOf(code.charAt(2*(PC+1)+1)), 16);
                valC = Integer.parseInt(code.substring(2*(PC+2), 2*(PC+10)), 16);
                valP = PC + 10;
                break;
            case '5': //mrmovl
                rA = Integer.parseInt(String.valueOf(code.charAt(2*(PC+1))), 16);
                rB = Integer.parseInt(String.valueOf(code.charAt(2*(PC+1)+1)), 16);
                valC = Integer.parseInt(code.substring(2*(PC+2), 2*(PC+10)), 16);
                valP = PC + 10;
                break;
            case '6': //Opl
                rA = Integer.parseInt(String.valueOf(code.charAt(2*(PC+1))), 16);
                rB = Integer.parseInt(String.valueOf(code.charAt(2*(PC+1)+1)), 16);
                valP = PC + 2;
                break;
            case '7': //jXX
                valC = Integer.parseInt(code.substring(2*(PC+1), 2*(PC+9)), 16);
                valP = PC + 9;
                rA=0; rB = 0;
                break;
            case '8': //call
                valC = Integer.parseInt(code.substring(2*(PC+1), 2*(PC+9)), 16);
                valP = PC + 5; //shouldn't valP be PC+5
                rA=0; rB = 0;
                break;
            case '9': //ret
                valP = PC + 1;
                rA=0; rB = 0;
                break;
            case 'A': //pushl
                rA = Integer.parseInt(String.valueOf(code.charAt(2*(PC+1))), 16);
                rB = 15;
                valP = PC + 2;
                break;
            case 'B': //popl
                rA = Integer.parseInt(String.valueOf(code.charAt(2*(PC+1))), 16);
                rB = 15;
                valP = PC + 2;
                break;
        }
        System.out.println("rA: " + rA + " rB: " + rB + " valC: " + valC + " valP: " + valP);
        decode(rA, rB, valC, valP);
    }
    
    public static void decode(int rA, int rB, int valC, int valP){
        switch(icode){
            case '2':
                valA = registers.get(rA);
                break;
            case '4':
                valA = registers.get(rA);
                valB = registers.get(rB);
                break;
            case '5':
                valB = registers.get(rB);
                break;
            case '6':
                valA = registers.get(rA);
                valB = registers.get(rB);
                break;
            case '8': 
                //%esp
                valB = registers.get(4);
                break;
            case '9':
                valA = registers.get(4);
                valB = registers.get(4);
                break;
            case 'A':
                valA = registers.get(rA);
                valB = registers.get(4);
                break;
            case 'B':
                valA = registers.get(4);
                valB = registers.get(4);
                break;
        }
        System.out.println("valA: " + valA + " valB: " + valB);
        execute(rA, rB, valA, valB, valC, valP);
    }

    public static void execute(int rA, int rB, int valA, int valB, int valC, int valP){
        //cnd: 0 if condition is not satisfied, 1 if satisfied
        switch(icode){
            //cmov - do the switch case thing
            case '2':
                valE = 0 + valA;
                switch(ifun){
                    case '0': //rrmovl
                        cnd = 1;
                        break;
                    case '1': //cmovle
                        cnd = (conditionCodes.get("SF") ^ conditionCodes.get("OF")) | conditionCodes.get("ZF");
                        break;
                    case '2': //cmovl
                        cnd = conditionCodes.get("SF") ^ conditionCodes.get("OF");
                        break;
                    case '3': //cmove
                        cnd = conditionCodes.get("ZF");
                        break;
                    case '4': //cmovne
                        cnd = conditionCodes.get("ZF") == 0 ? 1 : 0;
                        break;
                    //needs rechecking
                    case '5': //cmovge
                        cnd = ((conditionCodes.get("SF") ^ conditionCodes.get("OF")) == 0 && conditionCodes.get("ZF") == 0) ? 1 : 0;
                        break;
                    case '6': //cmovg
                        cnd = (conditionCodes.get("SF") ^ conditionCodes.get("OF")) == 0 ? 1 : 0;
                        break;
                }
                break;
            case '3':
                valE = 0 + valC;
                break;
            case '4':
                valE = valB + valC;
                break;
            case '5':
                valE = valB + valC;
                break;
            //operation - switch case based on ifun
            case '6':
                //reset condition codes
                conditionCodes.replace("ZF", 0);
                conditionCodes.replace("SF", 0);
                conditionCodes.replace("OF", 0);
                switch(ifun){
                    case '0':
                        valE = valB + valA;
                        break;
                    case '1':
                        valE = valB - valA;
                        break;
                    case '2':
                        valE = valB & valA;
                        break;
                    case '3':
                        valE = valB ^ valA;
                        break;
                }
                //set condition codes
                if(valE == 0){
                    conditionCodes.replace("ZF", 1);
                }
                if(valE < 0){
                    conditionCodes.replace("SF", 1);
                }
                if((valA > 0 && valB > 0 && valE < 0) || (valA < 0 && valB < 0 && valE > 0)){
                    conditionCodes.replace("OF", 1);
                }
                System.out.println("ZF: " + conditionCodes.get("ZF") + " SF: " + conditionCodes.get("SF") + " OF: " + conditionCodes.get("OF"));
                break;
            //jump - switch case based on which condition
            //use valE to check condition (true or false)
            case '7':
                switch(ifun){
                    case '0': //jump unconditional
                        cnd = 1;
                        break;
                    case '1': //less or equal
                        cnd = (conditionCodes.get("SF") ^ conditionCodes.get("OF")) | conditionCodes.get("ZF");
                        break;
                    case '2': //less
                        cnd = conditionCodes.get("SF") ^ conditionCodes.get("OF");
                        break;
                    case '3': //equal
                        cnd = conditionCodes.get("ZF");
                        break;
                    case '4': //not equal
                        cnd = conditionCodes.get("ZF") == 0 ? 1 : 0;
                        break;
                    case '5': //greater or equal
                        cnd = (conditionCodes.get("SF") ^ conditionCodes.get("OF")) == 0 ? 1 : 0;
                        break;
                    case '6': //greater -- needs checking
                        cnd = ((conditionCodes.get("SF") ^ conditionCodes.get("OF")) == 0 && conditionCodes.get("ZF") == 0) ? 1 : 0;
                        break;
                }
                break;
            //move the stack pointer down 8 bytes (= one element in the stack array) to store valP on the top of the stack
            case '8':
                valE = valB - 4;
                break;
            case '9':
                valE = valB + 4;
                break;
            case 'A':
                valE = valB - 4;
                break;
            case 'B':
                valE = valB + 4;
                break;
        }
        System.out.println("valE: " + valE);
        memory(rA, rB, valA, valB, valC, valE, valP, cnd);
    }

    //this needs some editing - memory should be organized in a way where each element is one digit of a hex number
    public static void memory(int rA, int rB, int valA, int valB, int valC, int valE, int valP, int cnd){
        switch(icode){
            case '4':
                stack[valE] = valA;
                System.out.println("stack[valE]: " + stack[valE]);
                break;
            case '5':
                valM = stack[valE];
                break;
            case '8':
                stack[valE] = valP; //shouldn't it be stack[valE]? Also why is valE=valB-1? i thought it was valB-4
                break;
            case '9':
                valM = stack[valA];
                break;
            case 'A':
                stack[valE] = valA;
                System.out.println("stack[valE]: " + stack[valE]);
                break;
            case 'B':
                valM = stack[valA];
                break;
        }
        writeBack(rA, rB, valA, valB, valC, valE, valP, valM, cnd);
    }

    public static void writeBack(int rA, int rB, int valA, int valB, int valC, int valE, int valP, int valM, int cnd){
        switch(icode){
            //case 2 is conditional move - should depend on condition
            case '2':
                if(cnd == 1){
                    registers.replace(rB, valE);
                }
                break;
            case '5':
                registers.replace(rA, valM);
                break;
            case '3':
                registers.replace(rB, valE);
                break;
            case '6':
                registers.replace(rB, valE);
                break;
            case '8':
                registers.replace(4, valE);
                System.out.println("stack[%esp]: " + stack[registers.get(4)]);
            case '9':
                registers.replace(4, valE);
                System.out.println("stack[%esp]: " + stack[registers.get(4)]);
            case 'A':
                registers.replace(4, valE);
                break;
            case 'B':
                registers.replace(4, valE);
                registers.replace(rA, valM);
                break;
        }
        registers.forEach((key, value) -> System.out.print(key + ": " + value + " "));
        PCupdate(rA, rB, valA, valB, valC, valE, valP, valM, cnd);
    }

    public static void PCupdate(int rA, int rB, int valA, int valB, int valC, int valE, int valP, int valM, int cnd){
        switch(icode){
            case '7':
                if(cnd == 1){
                    PC = valC;
                }
                else{
                    PC = valP;
                }
                break;
            case '8':
                PC = valC;
                break;
            case '9':
                PC = valM;
                break;
            default:
                PC = valP;
        }
        System.out.println("PC: " + PC);
        for (int asdf=0; asdf<stack.length; asdf++){
            System.out.print(stack[asdf]);
        }
        //printResult(rA, rB, valA, valB, valC, valE, valP, valM, cnd);
    }

    /*public static void printResult(int rA, int rB, int valA, int valB, int valC, int valE, int valP, int valM, int cnd){
        
        System.out.println(PC);
    }*/

    static HashMap<Integer, Integer> registers = new HashMap<Integer, Integer>(){{
        put(0, 0);
        put(1, 0);
        put(2, 0);
        put(3, 0);
        //%esp - stack pointer
        put(4, 299);
        put(5, 0);
        put(6, 0);
        put(7, 0);
        put(8, 0);
        put(9, 0);
        put(10, 0);
        put(11, 0);
        put(12, 0);
        put(13, 0);
        put(14, 0);
    }};

    static HashMap<String, Integer> conditionCodes = new HashMap<String, Integer>(){{
        //we probably don't need the carry flag? since we're not doing unsigned arithmetic
        put("SF", 0); //Sign flag
        put("ZF", 0); //Zero flag
        put("OF", 0); //Overflow flag
    }};
}
