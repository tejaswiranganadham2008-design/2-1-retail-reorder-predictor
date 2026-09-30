package service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * =============================================================================
 * Class: PythonForecastRunner
 * Concept: System Integration / Inter-Process Communication via ProcessBuilder
 * Executes Python demand forecasting script (forecast.py) from Java.
 * =============================================================================
 */
public class PythonForecastRunner {

    public static class RunResult {
        public boolean success;
        public int exitCode;
        public long durationMs;
        public String outputLog;
        public String errorMessage;

        public RunResult(boolean success, int exitCode, long durationMs, String outputLog, String errorMessage) {
            this.success = success;
            this.exitCode = exitCode;
            this.durationMs = durationMs;
            this.outputLog = outputLog;
            this.errorMessage = errorMessage;
        }

        public String toJson() {
            return "{" +
                    "\"success\":" + success + "," +
                    "\"exitCode\":" + exitCode + "," +
                    "\"durationMs\":" + durationMs + "," +
                    "\"outputLog\":\"" + JsonHelper.escape(outputLog) + "\"," +
                    "\"errorMessage\":\"" + JsonHelper.escape(errorMessage) + "\"" +
                    "}";
        }
    }

    /**
     * Executes forecast.py using ProcessBuilder
     */
    public static RunResult runForecast(String salesCsvPath, String forecastCsvPath) {
        long startTime = System.currentTimeMillis();
        StringBuilder logBuilder = new StringBuilder();
        StringBuilder errBuilder = new StringBuilder();

        // Detect available Python command
        String[] candidateCommands = {"python", "python3", "py"};
        String workingCommand = null;

        for (String cmd : candidateCommands) {
            try {
                Process checkProc = new ProcessBuilder(cmd, "--version").start();
                int code = checkProc.waitFor();
                if (code == 0) {
                    workingCommand = cmd;
                    break;
                }
            } catch (Exception ignored) {
            }
        }

        if (workingCommand == null) {
            long duration = System.currentTimeMillis() - startTime;
            return new RunResult(
                    false,
                    -1,
                    duration,
                    "",
                    "Python executable ('python', 'python3', or 'py') was not found in system PATH. Please ensure Python 3 is installed."
            );
        }

        File scriptFile = new File("forecast.py");
        if (!scriptFile.exists()) {
            long duration = System.currentTimeMillis() - startTime;
            return new RunResult(
                    false,
                    -2,
                    duration,
                    "",
                    "Forecast script 'forecast.py' not found at workspace root: " + scriptFile.getAbsolutePath()
            );
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    workingCommand,
                    "forecast.py",
                    salesCsvPath,
                    forecastCsvPath
            );
            pb.directory(new File(".")); // Set working directory to project root
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // Read standard output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logBuilder.append(line).append("\n");
                }
            }

            // Read standard error
            try (BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = errReader.readLine()) != null) {
                    errBuilder.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            long duration = System.currentTimeMillis() - startTime;

            if (exitCode == 0) {
                return new RunResult(true, exitCode, duration, logBuilder.toString(), "");
            } else {
                return new RunResult(false, exitCode, duration, logBuilder.toString(),
                        errBuilder.length() > 0 ? errBuilder.toString() : "Python exited with error code " + exitCode);
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return new RunResult(false, -3, duration, logBuilder.toString(), "Execution failed: " + e.getMessage());
        }
    }
}
