package ru.lanit.at.driver;

import lombok.extern.slf4j.Slf4j;
import ru.lanit.at.util.CommandlineUtils;

import java.io.IOException;

@Slf4j
public class DriverUtils {

    public static void startDriver(String path) throws IOException {
        log.info(String.format("Command - cmd /c start /W %s -v", path));
        CommandlineUtils.executeCommand(String.format("cmd /c start %s -v", path), null);
    }

    public static void stopDriver(String process) throws IOException {
        log.info(String.format("Command - cmd /c taskkill /F /IM %s", process));
        CommandlineUtils.executeCommand(String.format("cmd /c taskkill /F /IM %s", process), null);
    }

    public static void restartDriver(String process, String path) throws IOException {
        log.info(String.format("Command - cmd /c taskkill /F /IM %s & cmd /c start %s -v", process, path));
        CommandlineUtils.executeCommand(String.format("cmd /c taskkill /F /IM %s & cmd /c start %s -v", process, path), null);
    }
}
