package ru.lanit.at.driver;

import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Driver {
    private String url;
    private String name;
    private boolean isLocal;
    private String driverPath;
    private String process;

    public Driver(String url, String name, boolean isLocal, String driverPath) {
        this.url = url;
        this.name = name;
        this.isLocal = isLocal;
        this.driverPath = driverPath;
    }
}
