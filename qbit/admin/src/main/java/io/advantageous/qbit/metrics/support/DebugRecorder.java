/*
 * Copyright (c) 2015. Rick Hightower, Geoff Chandler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * QBit - The Microservice lib for Java : JSON, WebSocket, REST. Be The Web!
 */

package io.advantageous.qbit.metrics.support;


import io.advantageous.qbit.metrics.StatRecorder;

import java.util.List;


/**
 * created by rhightower on 1/28/15.
 */
public class DebugRecorder implements StatRecorder {
    public volatile int count;
    public boolean out = false;

    public DebugRecorder(boolean out) {
        this.out = out;
    }

    public DebugRecorder() {
    }


    @Override
    public void record(List<MinuteStat> records) {
        count += records.size();
        if (out) System.out.println("DEBUG RECORDER " + records);
    }
}
