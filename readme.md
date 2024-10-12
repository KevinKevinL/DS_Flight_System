## 消息传输格式
#
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

牛

巨佬
