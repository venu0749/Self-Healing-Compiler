public class FunctionNode extends ASTNode {
    public String name;
    public String returnType;

    public FunctionNode(String returnType, String name) {
        this.returnType = returnType;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Function: " + returnType + " " + name + "()";
    }

    @Override
    public boolean isValid() {
        return name != null;
    }
}
