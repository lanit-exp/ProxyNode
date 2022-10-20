package ru.lanit.at.util;

import javax.validation.constraints.NotEmpty;
import java.io.IOException;

public class CommandlineUtils {
    /**
     * Execute native command
     * @param command - command
     * @param envp - environment properties (can be null)
     * @throws IOException
     */
    public static void executeCommand(@NotEmpty String command, String[] envp) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        runtime.exec(command, envp);
    }
}
