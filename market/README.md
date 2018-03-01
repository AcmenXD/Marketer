**第一步**
```java
/**
 * java代码中获取渠道信息,存储到market变量中使用
 */
String market = Marketer.getMarket(context, default_market);
```
---
**第二步**
```java
Android Studio生成包(debug或release包)
* 注意 : 此包为原始包,无需带有任何渠道信息
```
---
**第三步**
```java
配置markets.txt文件,规则:每行一个渠道名,注释用#号
* markets.txt文件在项目根目录market中有示例
```
---
**第四步**
```java
下列任意方式执行即可
	a)将apk、markets.txt放到程序Marketer.jar同目录下,双击打开Marketer.jar,点击开始
	b)双击打开Marketer.jar,点击自定义目录,填写 1.*原始包apk绝对路径 2.*markets.txt绝对路径 3.*apks输出目录绝对路径,点击开始
	c)打开命令窗口,跳转到Marketer.jar所在目录,执行java -jar Marketer.jar apk路径 markets.txt路径 apks输出目录(可不填,默认为程序根目录apks文件夹) 点击回车执行命令
* Marketer.jar在项目根目录market文件夹中
```
---
**第五步**
```java
等待打包完毕,所有渠道包的文件名规则为:原始包文件名-渠道信息.apk
完成后请关注apk渠道信息是否存在乱码情况(即渠道包的文件名)!
```
有问题请于作者联系AcmenXD@163.com ^_^!