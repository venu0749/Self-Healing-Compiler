import java.util.ArrayList;
import java.util.List;

public class RepairHistory {

    private static List<RepairRecord> history =
            new ArrayList<>();

    public static void logRepair(String errorType,
                                 String repairApplied,
                                 boolean success) {

        for (RepairRecord record : history) {

            if (record.errorType.equals(errorType)
                    && record.repairApplied.equals(repairApplied)) {

                record.incrementUsage();
                record.success = success;
                return;
            }
        }

        history.add(new RepairRecord(
                errorType,
                repairApplied,
                success));
    }

    public static List<RepairRecord> getHistory() {
        return history;
    }
}