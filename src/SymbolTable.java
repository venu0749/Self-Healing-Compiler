import java.util.HashMap ;
public class SymbolTable {
    private HashMap<String,String> table = new HashMap<>();
    
    public void declare(String variable ,String type){
        table.put(variable,type);
    }
    public boolean isDeclared(String variable){
        return table.containsKey(variable);
    }
    public void printTable() {
        System.out.println("\n ------ Symbol table -----");
        for (String var : table.keySet()) {
            System.out.println("variable : " + var + " | Type : " + table.get(var));
        }
    }
}
