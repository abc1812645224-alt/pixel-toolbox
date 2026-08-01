package com.example.pixeltoolbox.shizuku;

interface IImsConfigService {
    /**
     * Destroy the service process.
     */
    void destroy() = 16777114;

    /**
     * Appies full IMS configurations (VoLTE, VoWiFi, VoNR, 5G dual mode) for the given subId.
     */
    boolean applyFullImsConfig(int subId) = 1;

    /**
     * Apply CarrierConfig override with toggle map, running in Shizuku process (bypasses shell UID check on Android 17+).
     * @param subId target subscription ID, -1 for auto-detect
     * @param toggleJson JSON string with 10 boolean keys: vonr,nr_5g,5g_signal,5ga_icon,volte,vowifi,vilte,lte_4g,cross_sim,ut
     * @return true on success
     */
    boolean applyCarrierConfig(int subId, String toggleJson) = 2;
}
