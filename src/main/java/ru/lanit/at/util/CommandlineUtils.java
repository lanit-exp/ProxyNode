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
    public static Process executeCommand(@NotEmpty String command, String[] envp) throws IOException {
        return Runtime.getRuntime().exec(command, envp);
    }
}
