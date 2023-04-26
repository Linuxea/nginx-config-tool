package com.linuxea.nginx.script;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptUtil {

  /**
   * 生成混淆配置
   *
   * @param pkg          包名
   * @param configDomain 配置实体
   * @param dest
   * @throws IOException io exception
   */
  private void generateUrlMapFile(String pkg, ConfigDomain configDomain, String dest)
      throws IOException {
    Map<String, String> methodMappingMap = configDomain.getMethodMappingMap();
    Path path = Paths.get(dest + pkg + "映射关系.txt");
    StringBuilder stringBuilder = new StringBuilder();
    Set<Entry<String, String>> entries = methodMappingMap.entrySet();
    entries.forEach(entry -> {
      stringBuilder.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
    });
    Files.writeString(path, stringBuilder.toString());
  }

  public void generate(String pkg, String domain, String dest)
      throws IOException, InterruptedException {
    // Create a new HttpClient
    HttpClient client = HttpClient.newHttpClient();
    // Set the request URI and body
    String uri = "http://18.236.87.145:2030/common/methodMapping?pkgName=" + pkg;
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("{}", StandardCharsets.UTF_8)).build();

    // Send the request and get the response
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    String bodyString = response.body();
    //gson parse
    Gson gson = new Gson();
    ConfigDomain configDomain = gson.fromJson(bodyString, ConfigDomain.class);

    //generate task
    //generate url map file
    CompletableFuture<Void> generateUrlMapFileFuture = CompletableFuture.runAsync(() -> {
      try {
        generateUrlMapFile(pkg, configDomain, dest);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
    //generate ssl content
    String sslFile = generateSslFile(domain);
    //generate app file
    CompletableFuture<Void> generateAppAndLogFileFuture = CompletableFuture.runAsync(() -> {
      try {
        String logProxyUrl = generateAppFile(sslFile, domain, configDomain, dest);
        // generate log file
        generateLogProxyFile(sslFile, domain, logProxyUrl);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    //generate h5 file
    CompletableFuture<Void> generateH5FileFuture = CompletableFuture.runAsync(() -> {
      try {
        generateH5File(sslFile, domain, dest);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    //generate im file
    CompletableFuture<Void> generateImFileFuture = CompletableFuture.runAsync(() -> {
      try {
        generateImFile(sslFile, domain, dest);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    //generate test-app file
    CompletableFuture<Void> generateTestAppFileFuture = CompletableFuture.runAsync(() -> {
      try {
        generateTestAppFile(domain, configDomain, dest);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    //generate test-im file
    CompletableFuture<Void> generateTestImFileFuture = CompletableFuture.runAsync(() -> {
      try {
        generateTestImFile(domain, dest);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    //wait for all the futures to complete
    CompletableFuture.allOf(generateUrlMapFileFuture, generateAppAndLogFileFuture,
        generateH5FileFuture, generateImFileFuture, generateTestAppFileFuture,
        generateTestImFileFuture).join();
  }

  private void generateTestImFile(String domain, String dest)
      throws IOException {
    String content = """
        map $http_upgrade $connection_upgrade {
            default upgrade;
            '' close;
        }
        server {
            server_name test-im.ppjoy.xyz;
            listen 80;
            location /socket.io {
              proxy_pass http://172.21.28.244:8914;
              #proxy_set_header Host $host:$server_port;
              #proxy_set_header X-Real-IP $remote_addr;
              #proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
              #proxy_set_header X-Forwarded-Proto $scheme;
               proxy_set_header Host $host:$server_port;
               proxy_set_header X-Real-IP $remote_addr;
               proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
               proxy_set_header X-Forwarded-Proto $scheme;
               proxy_http_version 1.1;
               proxy_set_header Upgrade $http_upgrade;
               proxy_set_header Connection $connection_upgrade;
            }
          }
                
        """;

    //replace server_name
    String regex = "server_name\\s+(\\S+);";
    String serverName = "test-im." + domain;
    String replace = content.replaceAll(regex, "server_name " + serverName + ";");
    Path path = Paths.get(dest + serverName);
    Files.writeString(path, replace);
  }

  private void generateTestAppFile(String domain, ConfigDomain configDomain, String dest)
      throws IOException {

    String serverName = "test-app." + domain;
    Path path = Paths.get(dest + serverName);
    //replace server_name
    String config = configDomain.getConfig();
    String regex = "server_name\\s+(\\S+);";
    config = config.replaceAll(regex, "server_name " + serverName + ";");

    StringBuilder stringBuilder = new StringBuilder();
    for (String line : config.split("\n")) {
      //if line contains {shortLinkId} then replace xxxx
      if (line.contains("{shortLinkId}")) {
        String replace = line.replace("{shortLinkId}", "");
        stringBuilder.append(replace).append("\n");
      } else if (line.contains("{type}")) {
        String replace = line.replace("{type}", "");
        stringBuilder.append(replace).append("\n");
      } else {
        stringBuilder.append(line).append("\n");
      }
    }
    Files.writeString(path, stringBuilder.toString());
  }

  private void generateImFile(String sslFile, String domain, String dest) throws IOException {
    String content = """
        map $http_upgrade $connection_upgrade {
            default upgrade;
            '' close;
        }
        server {
          server_name im.ppjoy.xyz;
         ${sslFile}
          listen 80;
        location /socket.io {
            proxy_pass http://socket;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection $connection_upgrade;
          }
                
        """;
    //replace server_name
    String regex = "server_name\\s+(\\S+);";
    String serverName = "im." + domain;
    String replace = content.replaceAll(regex, "server_name " + serverName + ";")
        .replace("${sslFile}", sslFile);
    ;
    Path path = Paths.get(dest + serverName);
    Files.writeString(path, replace);
  }

  private void generateH5File(String sslFile, String domain, String dest) throws IOException {
    String content = """
        server {
          server_name h5.ppjoy.xyz;
           ${sslFile}
          listen 80;
          location / {
            return https://www.baidu.com;
          }
        }
        """;
    //replace server_name
    String regex = "server_name\\s+(\\S+);";
    String serverName = "h5." + domain;
    String replace = content.replaceAll(regex, "server_name " + serverName + ";")
        .replace("${sslFile}", sslFile);
    Path path = Paths.get(dest + serverName);
    Files.writeString(path, replace);
  }

  private String generateSslFile(String domain) {
    String secondDomain = extractDomain(domain);
    String ssl = """
             listen 443 ssl;
                 ssl_certificate /usr/local/nginx/ssl_acme/${secondDomain}.fullchain.pem;
                 ssl_certificate_key /usr/local/nginx/ssl_acme/${secondDomain}.pem;
                 ssl_protocols TLSv1 TLSv1.1 TLSv1.2 TLSv1.3;
        """;
    assert secondDomain != null;
    ssl = ssl.replace("${secondDomain}", secondDomain);
    return ssl;
  }

  private void generateLogProxyFile(String sslFile, String domain, String logProxyUrl)
      throws IOException {
    String content = """
        server {
          server_name log.ppjoy.xyz;
          ${sslFile}
          listen 80;
            location / {
            proxy_pass http://logserver;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
          }
                
           location /${logProxyUrl} {
                  proxy_pass http://logserver/log/live-chat;
                  proxy_set_header Host $host:$server_port;
                  proxy_set_header X-Real-IP $remote_addr;
                  proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                  proxy_set_header X-Forwarded-Proto $scheme;
                }
        }
        """;
    //replace server_name
    String regex = "server_name\\s+(\\S+);";
    String serverName = "log." + domain;
    String replace = content.replaceAll(regex, "server_name " + serverName + ";")
        .replace("${sslFile}", sslFile).replace("${logProxyUrl}", logProxyUrl);
    Path path = Paths.get(serverName);
    Files.writeString(path, replace);
  }

  private String generateAppFile(String sslFile, String domain,
      ConfigDomain configDomain, String dest)
      throws IOException {
    String serverName = "app." + domain;
    Path path = Paths.get(dest + serverName);
    //replace server_name
    String config = configDomain.getConfig();
    String regex = "server_name\\s+(\\S+);";
    config = config.replaceAll(regex, "server_name " + serverName + ";");

    String logUrl = "";
    StringBuilder stringBuilder = new StringBuilder();
    for (String line : config.split("\n")) {
      //if line contains {shortLinkId} then replace xxxx
      if (line.contains("{shortLinkId}")) {
        String replace = line.replace("{shortLinkId}", "");
        stringBuilder.append(replace).append("\n");
      } else if (line.contains("{type}")) {
        String replace = line.replace("{type}", "");
        stringBuilder.append(replace).append("\n");
      } else {
        stringBuilder.append(line).append("\n");
        //if line contains server_name
        if (line.contains("server_name")) {
          //replace server_name
          stringBuilder.append(sslFile).append("\n");
        }
      }

      //extract log random proxy url
      String logProxyUrl = "\\s+}\\s+location\\s+/([a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12})\\{$";
      Pattern pattern = Pattern.compile(logProxyUrl);
      Matcher matcher = pattern.matcher(line);
      if (matcher.find()) {
        logUrl = matcher.group(1);
      }
    }
    Files.writeString(path, stringBuilder.toString());

    return logUrl;
  }

  private String extractDomain(String domain) {
    String regex = "^([^.]+)\\..+$";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(domain);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }


}
