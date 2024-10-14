## 消息传输格式
1. 长度字段总是1字节，可以表示0-255的长度。
2. 对于String类型，如果长度超过255字节，会抛出异常。
3. 对于Integer，Boolean和Float类型，长度总是4，我们仍然写入这个长度以保持一致性。
4. 在unmarshall方法中，我们使用`& 0xFF`来确保将字节正确解释为无符号值。

现在，我们的消息格式看起来像这样：

```
[1字节键] [1字节类型] [1字节长度] [变长值] [1字节键] [1字节类型] [1字节长度] [变长值] ... [0字节结束标记]
```

例如，之前的示例消息现在会是这样的格式（用十六进制表示）：

```
01 53 01 31                    // OPTION: "1"
02 49 04 00 00 30 39           // REQUEST_ID: 12345
03 53 08 4E 65 77 20 59 6F 72 6B // SOURCE: "New York"
04 53 06 4C 6F 6E 64 6F 6E      // DESTINATION: "London"
00                              // 结束标记
```

需要注意的限制：

字符串长度不能超过255字节。对于大多数用例来说，这应该足够了。



## 最多一次和最少一次模式





## 回调函数

为了以回调的方式实现monitorSeatAvailability，我们需要在客户端和服务端做一些改进

### 客户端

monitorSeatAvailability函数中在发送了request后，启动一个新的线程来监听从服务端传来的更新，这样可以在进行其他操作的同时监听航班的改动。

```java
executorService.submit(() -> {
    try {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < monitorInterval * 1000) {
            try {
                socket.setSoTimeout(1000);
                Message update = receiveResponse();
                if (update.getInt(MessageKey.FLIGHT_ID) != null && update.getInt(MessageKey.SEAT_AVAILABILITY) != null) {
                    System.out.println("Update for Flight ID: " + update.getInt(MessageKey.FLIGHT_ID));
                    System.out.println("New Seat Availability: " + update.getInt(MessageKey.SEAT_AVAILABILITY));
                }
            } catch (SocketTimeoutException e) {
                // 超时，继续循环
            } catch (IOException e) {
                System.err.println("Error receiving update: " + e.getMessage());
            }
        }
        System.out.println("Monitoring ended for Flight ID: " + flightId);
        socket.setSoTimeout(0);
    } catch (SocketException e) {
        System.err.println("Error setting socket timeout: " + e.getMessage());
    }
});
```


Google Doc:https://docs.google.com/document/d/1qjfCh8XJKJ2dl5XqjVfmLh0FX28_MzVnayBB3A4lBOM/edit?usp=sharing

