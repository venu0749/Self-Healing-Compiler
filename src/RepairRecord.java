public class RepairRecord {

    public String errorType;
    public String repairApplied;
    public boolean success;
    public int timesUsed;

    public RepairRecord(String errorType,
                        String repairApplied,
                        boolean success) {

        this.errorType = errorType;
        this.repairApplied = repairApplied;
        this.success = success;
        this.timesUsed = 1;
    }

    public void incrementUsage() {
        timesUsed++;
    }

    @Override
    public String toString() {
        return errorType + " -> " + repairApplied +
                " | Success: " + success +
                " | Used: " + timesUsed;
    }
}