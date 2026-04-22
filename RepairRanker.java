public class RepairRanker {

    public static String getBestRepair(String errorType) {

        int maxScore = -1;
        String bestRepair = null;

        for (RepairRecord record :
                RepairHistory.getHistory()) {

            if (record.errorType.equals(errorType)) {

                int score = record.timesUsed +
                        (record.success ? 5 : 0);

                if (score > maxScore) {
                    maxScore = score;
                    bestRepair = record.repairApplied;
                }
            }
        }

        return bestRepair;
    }
}