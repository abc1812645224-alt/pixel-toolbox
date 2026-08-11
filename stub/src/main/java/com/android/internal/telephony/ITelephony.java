/*
 * Copyright (C) 2026 Pixel Toolbox Project
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
 *
 * This file is a stub mirroring AOSP interfaces for hidden API access.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.telephony;

import android.os.Binder;
import android.os.IBinder;

public interface ITelephony extends android.os.IInterface {
    void setCarrierTestOverride(
            int subId,
            String mccmnc,
            String imsi,
            String iccid,
            String gid1,
            String gid2,
            String plmn,
            String spn,
            String carrierPriviledgeRules,
            String apn);

    int setImsProvisioningInt(int subId, int key, int value);

    int getImsProvisioningInt(int subId, int key);

    void resetIms(int slotIndex);

    boolean isImsRegistered(int subId);

    abstract class Stub extends Binder implements ITelephony {
        public native static ITelephony asInterface(IBinder binder);
    }
}
