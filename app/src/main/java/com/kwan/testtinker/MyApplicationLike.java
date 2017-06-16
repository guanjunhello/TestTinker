package com.kwan.testtinker;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.support.multidex.MultiDex;

import com.kwan.testtinker.Log.MyLogImp;
import com.kwan.testtinker.util.SampleApplicationContext;
import com.kwan.testtinker.util.TinkerManager;
import com.tencent.tinker.anno.DefaultLifeCycle;
import com.tencent.tinker.lib.tinker.TinkerInstaller;
import com.tencent.tinker.loader.app.DefaultApplicationLike;
import com.tencent.tinker.loader.shareutil.ShareConstants;

/**
 * author: $USER_NAME
 * created on: 2017/6/16 0016 上午 9:54
 * description:
 */
@DefaultLifeCycle(
        application = "com.kwan.testtinker.MyApplication",
        flags = ShareConstants.TINKER_ENABLE_ALL)
public class MyApplicationLike extends DefaultApplicationLike {
    public MyApplicationLike(Application application, int tinkerFlags, boolean tinkerLoadVerifyFlag, long applicationStartElapsedTime, long applicationStartMillisTime, Intent tinkerResultIntent) {
        super(application, tinkerFlags, tinkerLoadVerifyFlag, applicationStartElapsedTime, applicationStartMillisTime, tinkerResultIntent);
    }

    @Override
    public void onBaseContextAttached(Context context) {
        super.onBaseContextAttached(context);
        //you must install multiDex whatever tinker is installed!
        MultiDex.install(context);
        SampleApplicationContext.application = getApplication();
        SampleApplicationContext.context = getApplication();
        TinkerManager.setTinkerApplicationLike(this);

        TinkerManager.initFastCrashProtect();
        TinkerManager.setUpgradeRetryEnable(true);

        TinkerInstaller.setLogIml(new MyLogImp());
        TinkerManager.installTinker(this);
    }
}
