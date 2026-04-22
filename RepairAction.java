public class RepairAction {

    private String actionType;
    private String variableName;

    public RepairAction(String actionType,
                        String variableName) {

        this.actionType = actionType;
        this.variableName = variableName;
    }

    public String getActionType() {
        return actionType;
    }

    public String getVariableName() {
        return variableName;
    }
}