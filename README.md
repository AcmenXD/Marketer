# Marketer
渠道打包工具,可视化打包工具 -> (100个渠道包只需要10秒钟 )由作者:https://github.com/mcxiaoke 的 https://github.com/mcxiaoke/packer-ng-plugin 项目拓展而来,并解决渠道文件由于BOM编码导致的渠道信息乱码问题

### 依赖
---
- AndroidStudio
```
	allprojects {
			repositories {
				...
				maven { url 'https://jitpack.io' }
		    }
	}
```
```
	 compile 'com.github.AcmenXD:Marketer:1.0'
```
### 功能
---
- 100个渠道包只需要10秒钟
- 提供可视化工具,使用超方便
### 使用 -> 以下代码 注释很详细、很重要很重要很重要!!!
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
* markets.txt文件在项目根目录common中有示例
```
---
**第四步**
```java
打开Marketer.jar,填写 1.*原始包apk绝对路径 2.*markets.txt绝对路径 3.*apks输出目录绝对路径
* Marketer.jar在项目根目录common中
```
---
**第五步**
```java
等待打包完毕,所有渠道包的文件名规则为:原始包文件名-渠道信息.apk
完成后请关注apk渠道信息是否存在乱码情况(即渠道包的文件名)! 有问题请于作者联系<a href="AcmenXD@163.com" target="_email"> ^_^!
```
---
### 打个小广告^_^
**gitHub** : https://github.com/AcmenXD   如对您有帮助,欢迎点Star支持,谢谢~

**技术博客** : http://blog.csdn.net/wxd_beijing
# END