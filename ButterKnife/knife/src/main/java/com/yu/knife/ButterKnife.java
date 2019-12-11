package com.yu.knife;

import android.app.Activity;

import java.lang.reflect.Constructor;

/**
 * @author luyanjun
 * @description
 */
public class ButterKnife {


    public static void bind(Activity activity) {
        try {
            //要用getName 拿到全名
            Class bindClass = Class.forName(activity.getClass().getName() + "_ViewBinding");
            Constructor constructor = bindClass.getDeclaredConstructor(activity.getClass());
            constructor.newInstance(activity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
