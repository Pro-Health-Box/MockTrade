/*
 * Author: Balch
 * Created: 8/23/16 5:06 PM
 *
 * This file is part of MockTrade.
 *
 * MockTrade is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MockTrade is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MockTrade.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2016
 *
 */

package com.balch.android.app.framework;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.android.volley.ExecutorDelivery;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.ResponseDelivery;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;

import java.io.File;
import java.util.concurrent.Executors;

public class VolleyBackground {
    /** Default on-disk cache directory. */
    private static final String DEFAULT_CACHE_DIR = "volley";

    public static RequestQueue newRequestQueue(Context context, int threadPoolSize) {
        File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);

        String userAgent = "volley/0";
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            userAgent = packageName + "/" + info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }

        HttpStack stack = new HurlStack();

        Network network = new BasicNetwork(stack);

        ResponseDelivery delivery = new ExecutorDelivery(Executors.newFixedThreadPool(threadPoolSize));
        // pass ResponseDelivery object as a 4th parameter for RequestQueue constructor
        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir), network, threadPoolSize, delivery);
        queue.start();

        return queue;
    }

}