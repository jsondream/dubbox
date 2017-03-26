/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.dubbo.rpc.filter.tps;

import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;

/**
 * 类似令牌桶的限流方式
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
class StatItem {

    private String name;

    // 下一次重置的时间
    private long lastResetTime;

    // 每次重置的间隔时间
    private long interval;

    // 令牌容器:每个数值都代表一个令牌
    private AtomicInteger token;

    // 默认令牌的平均个数
    private int rate;

    StatItem(String name, int rate, long interval) {
        this.name = name;
        this.rate = rate;
        this.interval = interval;
        this.lastResetTime = System.currentTimeMillis();
        this.token = new AtomicInteger(rate);
    }

    public boolean isAllowable(URL url, Invocation invocation) {
        long now = System.currentTimeMillis();
        // 判断是否到重置的时间了
        if (now > lastResetTime + interval) {
            token.set(rate);
            lastResetTime = now;
        }

        int value = token.get();
        boolean flag = false;
        // 利用cas的方式去桶中拿令牌
        while (value > 0 && !flag) {
            flag = token.compareAndSet(value, value - 1);
            value = token.get();
        }

        return flag;
    }

    long getLastResetTime() {
        return lastResetTime;
    }
    
    int getToken() {
        return token.get();
    }
    
    public String toString() {
        return new StringBuilder(32).append("StatItem ")
            .append("[name=").append(name).append(", ")
            .append("rate = ").append(rate).append(", ")
            .append("interval = ").append(interval).append("]")
            .toString();
    }

}
