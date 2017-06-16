# TestTinker
测试集成Tinker热修复框架
### 1. 添加依赖
在Project的`build.gradle中`配置`classpath ('com.tencent.tinker:tinker-patch-gradle-plugin:1.7.11')`
```
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:2.3.2'
        classpath ('com.tencent.tinker:tinker-patch-gradle-plugin:1.7.11')
    }
}
```
`app -> build.gradle -> dependencies`中添加
```
    provided('com.tencent.tinker:tinker-android-anno:1.7.11')
    compile('com.tencent.tinker:tinker-android-lib:1.7.11')
    compile "com.android.support:multidex:1.0.1"
```
以及在 `build.gradle`最上方添加
```
apply plugin: 'com.tencent.tinker.patch'
```
这个时候需要同步一下，下面需要使用到Tinker去初始化
然而你会发现编译失败，出现如下信息

  ######  tinkerId is not set!!!
这个暂时不用理会，后边我们会处理这个`tinkerId `

### 2. 通过注解生成Application
引入Tinker的项目中需要创建一个类继承`DefaultApplicationLike`，然后为该类添加如下注解
```
@DefaultLifeCycle(application = "com.kwan.testtinker2.MyApplication", 
         flags = ShareConstants.TINKER_ENABLE_ALL)
```
然后重写`onBaseContextAttached()`方法，在其中实现原本Application中需要实现的操作
```
@DefaultLifeCycle(
        application = "com.kwan.testtinker.MyApplication",//此处换成你自己的包名以及想要命名的Application
        flags = ShareConstants.TINKER_ENABLE_ALL)
public class MyApplicationLike extends DefaultApplicationLike {
    public MyApplicationLike(Application application, int tinkerFlags, boolean tinkerLoadVerifyFlag, long applicationStartElapsedTime, long applicationStartMillisTime, Intent tinkerResultIntent) {
        super(application, tinkerFlags, tinkerLoadVerifyFlag, applicationStartElapsedTime, applicationStartMillisTime, tinkerResultIntent);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void registerActivityLifecycleCallbacks(Application.ActivityLifecycleCallbacks callback) {
        getApplication().registerActivityLifecycleCallbacks(callback);
    }

    @Override
    public void onBaseContextAttached(Context context) {
        super.onBaseContextAttached(context);
        //you must install multiDex whatever tinker is installed!
        MultiDex.install(context);
        TinkerInstaller.install(this);
    }
}
```
然后将刚才我们命名的Application（我这里是`com.kwan.testtinker.MyApplication`）添加到`Manifest`的`application`中，这时候会你添加的`MyApplication`会是红字，因为还没有生成，也暂时不用理会
### 3. 完善app中`build.gradle`配置信息
参考Tinker官方demo中的配置信息
https://github.com/Tencent/tinker/blob/master/tinker-sample-android/app/build.gradle

android中添加签名配置，其中的路径、签名文件名密码等需要根据自己项目实际情况修改，此处我的签名文件放在`app/keystore/`中
```
//recommend
    dexOptions {
        jumboMode = true
    }
    signingConfigs {
        release {
            try {
                storeFile file("./keystore/release.jks")
                storePassword "123456"
                keyAlias "release"
                keyPassword "123456"
            } catch (ex) {
                throw new InvalidUserDataException(ex.toString())
            }
        }
    }
```
然后`buildTypes`中修改为
```
release {
    minifyEnabled true
    signingConfig signingConfigs.release
    proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
}
```
`defaultConfig`中添加
```
multiDexEnabled true
buildConfigField "String", "MESSAGE", "\"I am the base apk\""
buildConfigField "String", "TINKER_ID", "\"${getTinkerIdValue()}\""
buildConfigField "String", "PLATFORM",  "\"all\""
```
最后，在下方添加剩余的代码（具体哪些有用哪些是没用的我也没有去研究过，直接从官方的demo中整个丢了进去）
```
def gitSha() {
    try {
        String gitRev = 'git rev-parse --short HEAD'.execute(null, project.rootDir).text.trim()
        if (gitRev == null) {
            throw new GradleException("can't get git rev, you should add git to system path or just input test value, such as 'testTinkerId'")
        }
        return gitRev
    } catch (Exception e) {
        throw new GradleException("can't get git rev, you should add git to system path or just input test value, such as 'testTinkerId'")
    }
}

def javaVersion = JavaVersion.VERSION_1_7

def bakPath = file("${buildDir}/bakApk/")

ext {
    tinkerEnabled = true
    tinkerOldApkPath = "${bakPath}/app-release-0615-11-09-30.apk"
    tinkerApplyMappingPath = "${bakPath}/app-release-0615-11-09-30-mapping.txt"
    tinkerApplyResourcePath = "${bakPath}/app-release-0615-11-09-30-R.txt"
    tinkerBuildFlavorDirectory = "${bakPath}/app-release-0615-11-09-30"
}


def getOldApkPath() {
    return hasProperty("OLD_APK") ? OLD_APK : ext.tinkerOldApkPath
}

def getApplyMappingPath() {
    return hasProperty("APPLY_MAPPING") ? APPLY_MAPPING : ext.tinkerApplyMappingPath
}

def getApplyResourceMappingPath() {
    return hasProperty("APPLY_RESOURCE") ? APPLY_RESOURCE : ext.tinkerApplyResourcePath
}

def getTinkerIdValue() {
    return hasProperty("TINKER_ID") ? TINKER_ID : gitSha()
}

def buildWithTinker() {
    return hasProperty("TINKER_ENABLE") ? TINKER_ENABLE : ext.tinkerEnabled
}

def getTinkerBuildFlavorDirectory() {
    return ext.tinkerBuildFlavorDirectory
}

if (buildWithTinker()) {
    apply plugin: 'com.tencent.tinker.patch'

    tinkerPatch {
        oldApk = getOldApkPath()
        ignoreWarning = true

        useSign = true
        tinkerEnable = buildWithTinker()
        buildConfig {
            applyMapping = getApplyMappingPath()
            applyResourceMapping = getApplyResourceMappingPath()
            tinkerId = "TestTinkerId"
            keepDexApply = false
            isProtectedApp = false
        }

        dex {
            dexMode = "jar"
            pattern = ["classes*.dex",
                       "assets/secondary-dex-?.jar"]
            loader = [
                    //use sample, let BaseBuildInfo unchangeable with tinker
                    "tinker.sample.android.app.BaseBuildInfo"
            ]
        }

        lib {
            pattern = ["lib/*/*.so"]
        }

        res {
            pattern = ["res/*", "assets/*", "resources.arsc", "AndroidManifest.xml"]

            ignoreChange = ["assets/sample_meta.txt"]
            largeModSize = 100
        }

        packageConfig {
            configField("patchMessage", "tinker is sample to use")
            configField("platform", "all")
            configField("patchVersion", "1.0")
        }
        sevenZip {
            zipArtifact = "com.tencent.mm:SevenZip:1.1.10"
        }
    }

    List<String> flavors = new ArrayList<>();
    project.android.productFlavors.each {flavor ->
        flavors.add(flavor.name)
    }
    boolean hasFlavors = flavors.size() > 0
    def date = new Date().format("MMdd-HH-mm-ss")
    android.applicationVariants.all { variant ->
        def taskName = variant.name

        tasks.all {
            if ("assemble${taskName.capitalize()}".equalsIgnoreCase(it.name)) {

                it.doLast {
                    copy {
                        def fileNamePrefix = "${project.name}-${variant.baseName}"
                        def newFileNamePrefix = hasFlavors ? "${fileNamePrefix}" : "${fileNamePrefix}-${date}"

                        def destPath = hasFlavors ? file("${bakPath}/${project.name}-${date}/${variant.flavorName}") : bakPath
                        from variant.outputs.outputFile
                        into destPath
                        rename { String fileName ->
                            fileName.replace("${fileNamePrefix}.apk", "${newFileNamePrefix}.apk")
                        }

                        from "${buildDir}/outputs/mapping/${variant.dirName}/mapping.txt"
                        into destPath
                        rename { String fileName ->
                            fileName.replace("mapping.txt", "${newFileNamePrefix}-mapping.txt")
                        }

                        from "${buildDir}/intermediates/symbols/${variant.dirName}/R.txt"
                        into destPath
                        rename { String fileName ->
                            fileName.replace("R.txt", "${newFileNamePrefix}-R.txt")
                        }
                    }
                }
            }
        }
    }
    project.afterEvaluate {
        //sample use for build all flavor for one time
        if (hasFlavors) {
            task(tinkerPatchAllFlavorRelease) {
                group = 'tinker'
                def originOldPath = getTinkerBuildFlavorDirectory()
                for (String flavor : flavors) {
                    def tinkerTask = tasks.getByName("tinkerPatch${flavor.capitalize()}Release")
                    dependsOn tinkerTask
                    def preAssembleTask = tasks.getByName("process${flavor.capitalize()}ReleaseManifest")
                    preAssembleTask.doFirst {
                        String flavorName = preAssembleTask.name.substring(7, 8).toLowerCase() + preAssembleTask.name.substring(8, preAssembleTask.name.length() - 15)
                        project.tinkerPatch.oldApk = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-release.apk"
                        project.tinkerPatch.buildConfig.applyMapping = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-release-mapping.txt"
                        project.tinkerPatch.buildConfig.applyResourceMapping = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-release-R.txt"

                    }

                }
            }

            task(tinkerPatchAllFlavorDebug) {
                group = 'tinker'
                def originOldPath = getTinkerBuildFlavorDirectory()
                for (String flavor : flavors) {
                    def tinkerTask = tasks.getByName("tinkerPatch${flavor.capitalize()}Debug")
                    dependsOn tinkerTask
                    def preAssembleTask = tasks.getByName("process${flavor.capitalize()}DebugManifest")
                    preAssembleTask.doFirst {
                        String flavorName = preAssembleTask.name.substring(7, 8).toLowerCase() + preAssembleTask.name.substring(8, preAssembleTask.name.length() - 13)
                        project.tinkerPatch.oldApk = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-debug.apk"
                        project.tinkerPatch.buildConfig.applyMapping = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-debug-mapping.txt"
                        project.tinkerPatch.buildConfig.applyResourceMapping = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-debug-R.txt"
                    }

                }
            }
        }
    }
}
```
其中在`if (buildWithTinker()) { -> tinkerPatch -> buildConfig` 中有一个`tinkerId` ，给为你自己想用的id（命名好像没有什么特别的规则，随便取）

还有就是`ext` 中的几个属性需要留意一下，后边会用到

`Sync Now`
一切就绪
### 4. 自留bug

![1.png](http://upload-images.jianshu.io/upload_images/2249474-c801e3c1bc7ad846.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

我写了一个简单的界面，第一个Button用来弹出上面TextView中的文字，第二个Button用来添加热修复补丁，此时上方的TextView我是没有进行初始化的
```
public class MainActivity extends AppCompatActivity {

    private TextView tv_test;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_show_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = tv_test.getText().toString();
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_load_patch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TinkerInstaller.onReceiveUpgradePatch(getApplicationContext(), Environment.getExternalStorageDirectory().getAbsolutePath() + "/patch_signed_7zip.apk");
            }
        });
    }
}
```
还有就是记得加权限，为了简单，我没有动态申请，自己去权限设置里打开
```
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
```

这个时候点击`Show Text Above`，程序崩溃

### 5. 生成补丁
刚才运行之后会在`\app\build\bakApk`下生成以时间命名的一个xxx.apk和一个xxx-R.txt，将这个名字xxx复制，粘贴到上面我让留意过的`build.gradle`的`ext` 中
```
ext {
    tinkerEnabled = true
    tinkerOldApkPath = "${bakPath}/app-debug-0616-10-48-43.apk"
    tinkerApplyMappingPath = "${bakPath}/app-debug-0616-10-48-43-mapping.txt"
    tinkerApplyResourcePath = "${bakPath}/app-debug-0616-10-48-43-R.txt"
    tinkerBuildFlavorDirectory = "${bakPath}/app-debug-0616-10-48-43"
}
```
其中`app-debug-0616-10-48-43`就是刚才我生成的apk
`Sync Now`
然后现在去修复自留的bug
```
setContentView(R.layout.activity_main);

tv_test = (TextView) findViewById(R.id.tv_test);
```

这个时候使用Tinker生成补丁，有几种生成的方式，下面讲述我觉得最简单的方式
as的工作区域右上角有一个侧着的Gradle

![2.png](http://upload-images.jianshu.io/upload_images/2249474-ba2aee79a338dcab.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
此时我们使用的是debug包，所以双击tinkerPatchDebug，等到它操作完成，我看到有人说这一步会有成功的提示，可是我一直没看到过，等它状态处转圈结束，在`app\build\outputs\tinkerPatch\debug`下会生成一堆文件，我们只需要`patch_signed_7zip.apk`
由于代码中我们指定的补丁路径就是根目录，所以直接将这个文件复制到手机根目录
重新打开程序，点击`Load Patch`
出现如下信息，并且没有反应
```
 I/Tinker.DefaultLoadReporter: patch loadReporter onLoadPatchListenerReceiveFail: patch receive fail: /storage/emulated/0/patch_signed_7zip.apk, code: -2
```
这个是因为权限没给，去权限管理中允许权限，重新点击，几秒钟之后程序自己关闭了，重新打开，点击`Show Text Above`

![3.png](http://upload-images.jianshu.io/upload_images/2249474-f3818ff032d43a1d.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

bug被修复

###### `签名包也是按上述操作，因为我们已经在build.gradle中配置了release信息，所以只需要如上双击tinkerPatchRelease即可，不过生成的patch文件路径有些许差别`

### 6. 需要注意的几点
I. 最后加载patch的时候程序自己结束了，这样在实际项目中肯定是不允许的，我们可以把官方的demo中的一个工具类拿过来，demo中在加载patch之后弹出了success的Toast，这个我们也不需要，找出来去掉即可

![4.png](http://upload-images.jianshu.io/upload_images/2249474-d09b8e160cfc15b7.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

II. 生成的补丁文件建议不要使用apk作为后缀，并且设置合理路径


*如有错误，请不吝指正且轻喷，谢谢阅读*
