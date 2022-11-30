# ProxyNode

## Общее описание

Сервис предназначен для проксирования внешних запросов на драйверы различного назначения.
<br/>
Соединением по умолчанию драйвером является FlaNium-драйвер, располагающийся на той же машине, что и контроллер (В случае локального 
запуска необходимо проверить application.yaml для корректировки параметров подключения к драйверу).
Однако, драйвер может располагаться на удаленной машине. Однако, драйверов может быть и несколько. Количество их может быть сколько угодно. Для этого необходимые параметры для подключения нужно указать в дополнительном конфигурационном файле (по умолчанию connections.yaml (формат файла обязательно .yaml), однако название файла можно поменять, используя параметр connection.list.file). 
В нем нужно указать название соединения, url, тип драйвера и признак локальности драйвера (если драйвер локальный,
то нужно указать путь до файла, иначе данное соединение не будет иcпользоваться при работе).

## Параметры конфигурации соединения

* url - url драйвера (например localhost:9999)
* driver: - тип драйвера;
* isLocal - признак нахождения драйвера на том же хосте, что и сам сервис;
* driverPath - путь до драйвера, для локальных обязательное указание, в противном случае соединение будет игнорироваться.

При запуске сервис запускает локальные драйверы, при возникновении ошибок или завершении работы сервера драйверы завершают 
работу автоматически. С определенной периодичностью локальные драйверы обновляются.

## Параметры конфигурации работы драйверов при запуске сервиса

* connection.list.file - путь до файла с доп. драйверами (по умолчанию ./connections.yaml)
* connection.default.url - url драйвера по умолчанию (localhost:9999)
* connection.default.driver - имя драйвера (по умолчанию Flaium)
* connection.default.isLocal - признак локальности драйвера (по умолчанию true)
* connection.default.path - (опционально) путь до драйвера (по умолчанию C:\FlaNium.Desktop.Driver-v1.6.0\FlaNium.Driver.exe)

Примеры запуска сервиса
```shell
java -Dconnection.list.file=./hosts.yaml -Dconnection.default.url=host:9999 -Dconnection.default.driver=FlaNium -Dconnection.default.isLocal=false -jar ProxyRemoteController-1.0-SNAPSHOT.jar
```
```shell
java -Dconnection.list.file=./connections.yaml -Dconnection.default.url=localhost:9999 -Dconnection.default.driver=FlaNium -Dconnection.default.isLocal=true connection.default.path=C:\FlaNium.Desktop.Driver-v1.6.0\FlaNium.Driver.exe -jar ProxyRemoteController-1.0-SNAPSHOT.jar
```

## API

* ```POST: /{sessionId}/change-driver?driver={driverName}``` - изменить текущий драйвер, где driverName - имя нового драйвера, sessionId - id текущей сессии
* ```POST: /rest/api/v1/release-connections``` - высвободить все занятые соединения
* ```POST: /rest/api/v1/timeout/set/{value}``` - задать значение таймаута в секундах для освобождения занятого соединения (в случае некорректного завершения работы со стороны фреймворка)
По умолчанию 3 секунды.
