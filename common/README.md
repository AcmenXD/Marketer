1.MarketsUtils.java 和 UnicodeReader.java 放到src代码路径下(java类文件,放到任意位置)
2.代码中获取渠道信息:
    String market = MarketUtils.getMarket(Context, defaultMarket);
3.Android Studio生成最终包(debug或release包)
4.配置markets.txt文件,规则:每行一个渠道名,注释用#号
5.打开MarketUtils.jar,填写 *最终包apk绝对路径 *markets.txt绝对路径 *apks输出目录绝对路径
6.等待打包完毕! 完成后请关注apk渠道信息是否存在乱码情况! 有问题找开发^_^!